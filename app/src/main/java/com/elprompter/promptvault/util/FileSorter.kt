package com.elprompter.promptvault.util

import android.content.ContentUris
import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import androidx.documentfile.provider.DocumentFile
import com.elprompter.promptvault.data.ActivityLogRepository
import com.elprompter.promptvault.data.ConflictStrategy
import com.elprompter.promptvault.data.LogLevel
import com.elprompter.promptvault.data.MoveHistoryEntry
import com.elprompter.promptvault.data.MoveHistoryRepository
import com.elprompter.promptvault.data.Rule
import com.elprompter.promptvault.data.RuleRepository
import com.elprompter.promptvault.data.SettingsRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.io.File
import java.io.RandomAccessFile
import java.util.UUID

data class SkippedFileInfo(
    val fileName: String,
    val reason: String
)

data class ScanResult(
    val filesMoved: Int,
    val filesSkippedNoMatch: Int,
    val foldersUnreadable: Boolean,
    val overlapWarnings: List<String>,
    val skippedDetails: List<SkippedFileInfo> = emptyList(),
    /**
     * [SAF, fix audit P0 #2 -- SAF_FINAL_LOGIC_AUDIT.md 2026-08-12] `true` HANYA
     * kalau folder kustom SUDAH DIKONFIGURASI tapi tidak bisa diakses lagi
     * (dihapus, dipindah, izin dicabut dari luar app, dst). Sengaja field
     * TERPISAH dari [foldersUnreadable] (yang berarti Downloads legacy tidak
     * terbaca) -- dua kegagalan ini butuh pesan & tindakan pemulihan yang
     * beda buat user (fix izin storage vs pilih ulang folder kustom).
     */
    val safAccessLost: Boolean = false
)

/** Hasil uji-coba pattern terhadap isi Downloads saat ini, dipakai di layar Tambah/Edit Rule. */
data class PatternPreviewResult(
    val totalCandidateFiles: Int,
    val matchedFileNames: List<String>
)

/**
 * [SAF, syarat (c) Insiden #7] Mime type file tujuan HANYA PERNAH diturunkan
 * dari ekstensi nama file di sini -- TIDAK PERNAH dipercaya dari metadata
 * provider SAF sumber (`DocumentFile.getType()`). Ini persis Bug #2 yang
 * ditemukan di v2.10.0 dulu (lihat PROJECT_STATE.md, Insiden #7): mime type
 * dari provider sumber terbukti tidak konsisten antar OEM/app sumber, dan
 * memicu provider TUJUAN menambah ekstensi ganda/salah saat `createFile()`.
 * Fungsi murni & top-level (bukan method [FileSorter]) SUPAYA unit-testable
 * tanpa Context Android -- lihat MimeTypeForFileNameTest.
 */
/**
 * [Feature, dukung SEMUA ekstensi -- 2026-08-13, permintaan user] Sebelumnya
 * hanya zip/txt terdaftar eksplisit, `else` genap balik `application/octet-
 * stream`. `octet-stream` tetap fallback AMAN untuk ekstensi apa pun yang
 * tidak ada di tabel ini -- SAF `createFile()` tetap sukses, cuma tanpa
 * asosiasi MIME spesifik (nama+ekstensi file tetap utuh, aplikasi lain
 * biasanya tetap kenali dari ekstensi). Tabel di bawah cuma memperkaya
 * fidelity untuk tipe umum, BUKAN syarat supaya ekstensi lain "didukung" --
 * dukungan ekstensi lain sudah didapat dari [listCandidateFiles] &
 * [listCandidateFilesSaf] tidak lagi memfilter ekstensi sama sekali (lihat
 * catatan di situ). TIDAK memakai `android.webkit.MimeTypeMap` di sini
 * SENGAJA -- fungsi ini top-level pure Kotlin biar tetap unit-testable
 * tanpa Context/Robolectric (lihat MimeTypeForFileNameTest), sedangkan
 * MimeTypeMap butuh runtime Android asli.
 */
fun mimeTypeForFileName(name: String): String = when (name.substringAfterLast('.', "").lowercase()) {
    "zip" -> "application/zip"
    "txt" -> "text/plain"
    "pdf" -> "application/pdf"
    "jpg", "jpeg" -> "image/jpeg"
    "png" -> "image/png"
    "gif" -> "image/gif"
    "webp" -> "image/webp"
    "mp4" -> "video/mp4"
    "mp3" -> "audio/mpeg"
    "json" -> "application/json"
    "xml" -> "application/xml"
    "md" -> "text/markdown"
    "csv" -> "text/csv"
    "apk" -> "application/vnd.android.package-archive"
    "doc" -> "application/msword"
    "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    "kt", "java", "gradle", "kts", "py", "js", "html", "css" -> "text/plain"
    else -> "application/octet-stream"
}

/**
 * Logika inti: scan folder Downloads, cocokkan tiap file terhadap rule aktif
 * (berurutan sesuai PRIORITAS, mendukung multi-pattern & filter ukuran),
 * pindahkan ke Downloads/PromptVault/<folderName>/, dan catat riwayat untuk undo.
 *
 * Prinsip "expert-level file organizer": setiap file yang TIDAK dipindahkan harus
 * bisa dijelaskan alasannya secara spesifik ke user, bukan cuma angka "dilewati".
 */
class FileSorter(
    private val context: Context,
    private val ruleRepository: RuleRepository,
    private val activityLogRepository: ActivityLogRepository,
    private val moveHistoryRepository: MoveHistoryRepository,
    private val settingsRepository: SettingsRepository
) {

    private val downloadsDir: File
        get() = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)

    private val vaultRootDir: File
        get() = File(downloadsDir, "PromptVault")

    /**
     * [Feature, dukung SEMUA ekstensi -- 2026-08-13, permintaan user] SEBELUMNYA
     * hanya file `.zip`/`.txt` pernah jadi kandidat -- filter ekstensi itu
     * DIHAPUS TOTAL di sini. Rule/[GlobMatcher] (pattern glob, mis. `*.kt`,
     * `*` untuk semua) sekarang SATU-SATUNYA penentu file mana yang cocok --
     * bukan lagi whitelist ekstensi hardcode duluan sebelum pattern sempat
     * dicek. `isTempOrPartialFile` & pengecualian folder `PromptVault` sendiri
     * TETAP jalan (aturan itu tidak terkait ekstensi, tetap relevan untuk
     * ekstensi apa pun).
     */
    private fun listCandidateFiles(): Array<File> {
        return downloadsDir.listFiles { f ->
            f.isFile &&
                !isTempOrPartialFile(f) &&
                !f.absolutePath.startsWith(vaultRootDir.absolutePath)
        } ?: emptyArray()
    }

    /**
     * File sementara dari browser/downloader (belum selesai diunduh) tidak boleh
     * pernah masuk sebagai kandidat sama sekali -- bukan cuma "ditunda" seperti
     * [isLikelyStillWriting], tapi memang belum selesai ditulis/diunduh sepenuhnya.
     * Daftar ini sengaja dicek terhadap NAMA LENGKAP (bukan cuma `.extension`
     * Kotlin) karena marker sering muncul sebagai akhiran ganda, mis.
     * "prompt.zip.crdownload".
     */
    private fun isTempOrPartialFile(file: File): Boolean = isTempOrPartialName(file.name)

    /**
     * [SAF] Versi berbasis nama-saja dari [isTempOrPartialFile], diekstrak
     * supaya jalur SAF ([listCandidateFilesSaf]) memakai PERSIS logika yang
     * sama, bukan salinan kedua yang independen -- sesuai syarat (c) Insiden
     * #7 (pelajaran satu implementasi tidak boleh menyebar tanpa disengaja).
     */
    private fun isTempOrPartialName(name: String): Boolean {
        val lowerName = name.lowercase()
        return TEMP_FILE_MARKERS.any { lowerName.endsWith(it) }
    }

    /**
     * Dual Stability Guard: sebuah file dianggap "masih ditulis" kalau salah
     * satu dari dua sinyal ini terpenuhi --
     *  1. Umurnya lebih baru dari [STABILITY_WINDOW_MS] (sinyal cepat, tanpa I/O).
     *  2. Ukurannya masih berubah dalam jeda singkat, ATAU file tersebut masih
     *     terkunci proses lain (mis. downloader belum selesai flush ke disk).
     * Guard #2 baru dijalankan kalau guard #1 lolos, supaya scan tetap murah
     * untuk mayoritas file yang memang sudah lama diam di Downloads.
     */
    private suspend fun isLikelyStillWriting(file: File): Boolean {
        val age = System.currentTimeMillis() - file.lastModified()
        if (age in 0 until STABILITY_WINDOW_MS) return true

        val sizeBefore = runCatching { file.length() }.getOrDefault(-1L)
        if (sizeBefore < 0) return true // tidak terbaca -> aman diasumsikan belum siap

        delay(SIZE_CHECK_DELAY_MS)

        val sizeAfter = runCatching { file.length() }.getOrDefault(-1L)
        if (sizeAfter < 0 || sizeAfter != sizeBefore) return true

        return try {
            RandomAccessFile(file, "rw").use { raf ->
                raf.channel.use { channel ->
                    val lock = channel.tryLock()
                    if (lock == null) {
                        true // sedang dikunci proses lain
                    } else {
                        lock.release()
                        false
                    }
                }
            }
        } catch (e: Exception) {
            // Tidak bisa membuka mode tulis (permission/OS lock) -> anggap belum
            // aman dipindah sekarang, coba lagi di scan berikutnya daripada
            // memaksa pindah file yang berisiko korup/setengah jadi.
            true
        }
    }

    private fun File.sizeKb(): Long = length() / 1024

    /**
     * Batch [race-fix]: scan manual (dari MainViewModel) dan auto-scan latar
     * belakang (AutoSortWorker) sebelumnya bisa berjalan BERSAMAAN karena
     * masing-masing membuat instance FileSorter sendiri tanpa koordinasi apa
     * pun. Akibatnya dua proses bisa mencoba memindahkan file Downloads yang
     * SAMA di saat yang sama -- proses kedua kehilangan race pada
     * `File.renameTo()` dan tercatat sebagai "Gagal dipindahkan" di Log,
     * padahal file itu sebenarnya sudah aman dipindahkan oleh proses pertama.
     * Fix: [scanMutex] ada di companion object (dibagi lintas SEMUA instance
     * FileSorter dalam proses yang sama, bukan per-instance), jadi manual
     * scan dan auto-scan otomatis mengantre, tidak pernah menyentuh Downloads
     * berbarengan. Kalau ada panggilan kedua datang saat yang pertama masih
     * jalan, ia menunggu giliran lalu scan ulang dengan kondisi folder yang
     * sudah terbaru (bukan gagal/error).
     */
    suspend fun scanAndSort(): ScanResult = scanMutex.withLock {
        // [SAF, syarat (c) Insiden #7] Titik cabang TUNGGAL antara jalur lama
        // (java.io.File/Downloads) dan jalur baru (SAF/folder kustom) -- AMAN
        // dipanggil dari MainViewModel MAUPUN AutoSortWorker tanpa perubahan
        // apa pun di kedua caller itu, karena signature scanAndSort() TIDAK
        // berubah. scanMutex yang sama tetap menaungi kedua jalur (race-fix
        // lama, lihat komentar di companion object, berlaku sama untuk SAF).
        //
        // [fix audit P0 #2, 2026-08-12] SEBELUMNYA resolveSafRoot() collapse
        // "belum diset" DAN "sudah diset tapi rusak/akses hilang" jadi satu
        // `null` yang sama-sama fallback diam-diam ke Downloads -- scan
        // TERLIHAT sukses padahal user mengira file masuk folder kustom.
        // Sekarang dua kondisi itu dibedakan eksplisit lewat
        // [SafRootResolution]: hanya NotConfigured yang boleh fallback ke
        // Downloads; AccessLost WAJIB berhenti + lapor error, TIDAK PERNAH
        // diam-diam pindah jalur.
        when (val resolution = resolveSafRoot()) {
            is SafRootResolution.Active -> scanAndSortSafLocked(resolution.root)
            SafRootResolution.NotConfigured -> scanAndSortLocked()
            is SafRootResolution.AccessLost -> {
                activityLogRepository.add(
                    LogLevel.ERROR,
                    "Folder kustom tidak bisa diakses (${resolution.reason}). Scan DIHENTIKAN, " +
                        "TIDAK fallback ke Downloads supaya file tidak salah tersortir ke tempat " +
                        "yang tidak kamu duga. Pilih ulang folder atau kembali ke Downloads lewat Pengaturan."
                )
                ScanResult(0, 0, foldersUnreadable = false, safAccessLost = true, overlapWarnings = emptyList())
            }
        }
    }

    /**
     * [SAF, fix audit P0 #2 -- SAF_FINAL_LOGIC_AUDIT.md 2026-08-12] State
     * eksplisit hasil resolusi folder kustom, GANTI TOTAL versi lama yang
     * cuma `DocumentFile?` (null berarti dua hal berbeda sekaligus: "belum
     * diset" DAN "rusak/akses hilang" -- audit menandai ini P0 fatal karena
     * scanner lalu fallback diam-diam ke Downloads di kedua kasus, jadi SAF
     * bisa gagal total tapi app terlihat seolah berhasil scan lokasi lain).
     */
    private sealed class SafRootResolution {
        data class Active(val root: DocumentFile) : SafRootResolution()
        data object NotConfigured : SafRootResolution()
        data class AccessLost(val reason: String) : SafRootResolution()
    }

    /**
     * [SAF] Resolusi root folder kustom dari URI tersimpan di
     * [SettingsRepository]. Tiga hasil eksplisit (lihat [SafRootResolution])
     * -- BUKAN lagi `DocumentFile?` polos -- supaya caller ([scanAndSort] &
     * [checkSafAccessLost]) tidak pernah salah memperlakukan "akses hilang"
     * sebagai "memang belum diset".
     */
    private suspend fun resolveSafRoot(): SafRootResolution {
        val uriString = settingsRepository.getSafTreeUri() ?: return SafRootResolution.NotConfigured
        return try {
            val doc = DocumentFile.fromTreeUri(context, Uri.parse(uriString))
            when {
                doc == null -> SafRootResolution.AccessLost("tree URI tidak valid")
                !doc.exists() -> SafRootResolution.AccessLost("folder tidak ditemukan -- mungkin dihapus/dipindah")
                !doc.isDirectory -> SafRootResolution.AccessLost("target bukan folder")
                else -> SafRootResolution.Active(doc)
            }
        } catch (e: SecurityException) {
            // [fix audit P0 #1, bagian "validasi permission"] Ini persis kasus
            // izin persistable dicabut dari luar app (mis. user cabut manual
            // lewat Pengaturan Android, atau OS reclaim saat limit provider
            // tercapai) -- SEBELUMNYA ditelan jadi `null`/fallback diam-diam.
            SafRootResolution.AccessLost("izin akses dicabut")
        } catch (e: Exception) {
            SafRootResolution.AccessLost("error tak terduga: ${e.message ?: e::class.simpleName}")
        }
    }

    /**
     * [SAF, fix audit P0 #1 -- "validasi permission saat startup"] Cek status
     * akses folder kustom TANPA menjalankan scan apa pun -- dipanggil
     * [MainViewModel] saat startup & setiap kali URI folder kustom berubah,
     * supaya user diberi tahu akses sudah hilang SEBELUM scan berikutnya
     * (manual atau AutoSortWorker latar belakang) diam-diam gagal/fallback.
     * Return `false` kalau belum diset SAMA SEKALI (bukan error, memang
     * pakai Downloads) ATAU kalau folder aktif & sehat.
     */
    suspend fun checkSafAccessLost(): Boolean = withContext(Dispatchers.IO) {
        resolveSafRoot() is SafRootResolution.AccessLost
    }

    /**
     * [perf-overhaul v2.4.0] Tiga masalah performa yang menyebabkan app
     * "kewalahan" bahkan di ratusan file, sekarang diperbaiki sekaligus:
     *
     * 1. **Semua I/O sekarang di [Dispatchers.IO]**: sebelumnya fungsi ini
     *    berjalan di dispatcher pemanggil (Main, lewat `viewModelScope.launch`
     *    di [MainViewModel]) -- setiap `File.listFiles()`, `RandomAccessFile`
     *    lock check, `renameTo()`/`copyTo()` adalah I/O blocking sinkron yang
     *    dulunya mengeksekusi LANGSUNG di UI thread -> freeze/ANR/force-close.
     * 2. **Urutan pengecekan dibalik**: [isLikelyStillWriting] (delay 1 detik +
     *    buka `RandomAccessFile` untuk cek lock) dulunya jalan untuk SEMUA file
     *    kandidat termasuk yang TIDAK PERNAH akan dipindah karena tidak cocok
     *    rule manapun. Sekarang pengecekan rule (murah, in-memory) jalan dulu;
     *    stability check hanya untuk file yang benar-benar akan dipindah.
     * 3. **Diproses paralel dengan batas [SCAN_CONCURRENCY]**: dulunya
     *    `for (file in candidateFiles)` sekuensial -- 300 file yang semuanya
     *    lolos ke stability check berarti ~300 detik (delay 1 detik/file
     *    berturutan). Sekarang tiap kandidat diproses lewat `async` dengan
     *    [Semaphore] agar wall-time mendekati (jumlah file / SCAN_CONCURRENCY)
     *    detik, bukan (jumlah file) detik, tanpa membuka terlalu banyak file
     *    handle bersamaan.
     *
     * Hasil per-file dikumpulkan lewat `awaitAll()` lalu digabung SEKUENSIAL
     * di luar coroutine paralel (bukan mutable var dibagi lintas coroutine),
     * supaya `moved`/`skipped`/`overlapWarnings` tetap aman tanpa race
     * condition maupun butuh Mutex tambahan.
     */
    private suspend fun scanAndSortLocked(): ScanResult = withContext(Dispatchers.IO) {
        val rules = ruleRepository.getRules().filter { it.enabled }
        val conflictStrategy = settingsRepository.getConflictStrategy()

        if (!downloadsDir.exists() || !downloadsDir.canRead()) {
            activityLogRepository.add(LogLevel.ERROR, "Folder Downloads tidak terbaca. Cek izin penyimpanan.")
            return@withContext ScanResult(0, 0, foldersUnreadable = true, overlapWarnings = emptyList())
        }

        if (rules.isEmpty()) {
            activityLogRepository.add(LogLevel.INFO, "Scan dijalankan, tapi belum ada rule aktif.")
            return@withContext ScanResult(0, 0, foldersUnreadable = false, overlapWarnings = emptyList())
        }

        val candidateFiles = listCandidateFiles()

        if (candidateFiles.isEmpty()) {
            activityLogRepository.add(LogLevel.INFO, "Scan selesai: tidak ada file baru yang cocok pattern rule manapun.")
            return@withContext ScanResult(0, 0, foldersUnreadable = false, overlapWarnings = emptyList())
        }

        val semaphore = Semaphore(SCAN_CONCURRENCY)
        val results = candidateFiles.map { file ->
            async { semaphore.withPermit { processCandidate(file, rules, conflictStrategy) } }
        }.awaitAll()

        var moved = 0
        var skipped = 0
        val overlapWarnings = mutableListOf<String>()
        val skippedDetails = mutableListOf<SkippedFileInfo>()
        for (result in results) {
            result.overlapWarning?.let { overlapWarnings.add(it) }
            when (result) {
                is CandidateOutcome.Moved -> moved++
                is CandidateOutcome.Skipped -> {
                    skipped++
                    skippedDetails.add(result.info)
                }
            }
        }

        val summary = if (skipped > 0) {
            "Scan selesai: $moved file dipindahkan, $skipped dilewati. Buka \"Detail File Dilewati\" untuk lihat nama filenya."
        } else {
            "Scan selesai: $moved file dipindahkan, $skipped dilewati."
        }
        activityLogRepository.add(LogLevel.SUCCESS, summary)

        // §2 roadmap backend -- bersihkan entri MediaStore "hantu" yang SUDAH
        // terlanjur nyangkut dari sebelum fix scanFile() di atas ada (mis. dari
        // versi lama app, atau file yang dihapus manual lewat file manager lain
        // tanpa lewat PromptVault). Query murah (1x per scan, bukan per file),
        // TIDAK pernah menggagalkan scan utama kalau error/permission masalah.
        cleanupGhostMediaStoreEntries()

        ScanResult(moved, skipped, foldersUnreadable = false, overlapWarnings = overlapWarnings, skippedDetails = skippedDetails)
    }

    /**
     * [SAF, syarat (c) Insiden #7] Versi folder-kustom dari [scanAndSortLocked],
     * dipanggil dari [scanAndSort] kalau [resolveSafRoot] berhasil. SENGAJA
     * MEMILIKI `withContext(Dispatchers.IO)` MILIKNYA SENDIRI di sini -- persis
     * seperti [scanAndSortLocked] -- BUKAN mengandalkan scope milik pemanggil.
     * Ini pelajaran langsung dari CI-fail v2.8.0: fungsi SAF lama pernah
     * ditulis sebagai `suspend fun` polos yang memanggil `async{}` tanpa
     * CoroutineScope receiver sama sekali. Dengan wrapper sendiri di sini,
     * struktur 1:1 sama dengan versi yang SUDAH terbukti kompil, jadi kelas
     * bug itu tidak mungkin terulang dengan cara yang sama.
     */
    private suspend fun scanAndSortSafLocked(root: DocumentFile): ScanResult = withContext(Dispatchers.IO) {
        val rules = ruleRepository.getRules().filter { it.enabled }
        val conflictStrategy = settingsRepository.getConflictStrategy()

        if (!root.exists() || !root.isDirectory) {
            activityLogRepository.add(LogLevel.ERROR, "Folder kustom (SAF) tidak terbaca. Folder mungkin sudah dipindah/dihapus, atau izin dicabut dari luar app.")
            return@withContext ScanResult(0, 0, foldersUnreadable = true, overlapWarnings = emptyList())
        }

        if (rules.isEmpty()) {
            activityLogRepository.add(LogLevel.INFO, "Scan dijalankan, tapi belum ada rule aktif.")
            return@withContext ScanResult(0, 0, foldersUnreadable = false, overlapWarnings = emptyList())
        }

        val vaultRootDoc = findOrCreateChildDirSaf(root, "PromptVault")
        if (vaultRootDoc == null) {
            activityLogRepository.add(LogLevel.ERROR, "Gagal membuat/membuka folder \"PromptVault\" di dalam folder kustom.")
            return@withContext ScanResult(0, 0, foldersUnreadable = true, overlapWarnings = emptyList())
        }

        val candidateFiles = listCandidateFilesSaf(root, vaultRootDoc)

        if (candidateFiles.isEmpty()) {
            activityLogRepository.add(LogLevel.INFO, "Scan selesai: tidak ada file baru yang cocok pattern rule manapun di folder kustom.")
            return@withContext ScanResult(0, 0, foldersUnreadable = false, overlapWarnings = emptyList())
        }

        val semaphore = Semaphore(SCAN_CONCURRENCY)
        val results = candidateFiles.map { doc ->
            async { semaphore.withPermit { processCandidateSaf(doc, rules, conflictStrategy, vaultRootDoc) } }
        }.awaitAll()

        var moved = 0
        var skipped = 0
        val overlapWarnings = mutableListOf<String>()
        val skippedDetails = mutableListOf<SkippedFileInfo>()
        for (result in results) {
            result.overlapWarning?.let { overlapWarnings.add(it) }
            when (result) {
                is CandidateOutcome.Moved -> moved++
                is CandidateOutcome.Skipped -> {
                    skipped++
                    skippedDetails.add(result.info)
                }
            }
        }

        val summary = if (skipped > 0) {
            "Scan selesai: $moved file dipindahkan, $skipped dilewati. Buka \"Detail File Dilewati\" untuk lihat nama filenya."
        } else {
            "Scan selesai: $moved file dipindahkan, $skipped dilewati."
        }
        activityLogRepository.add(LogLevel.SUCCESS, summary)

        // Catatan sengaja [batch discipline, syarat (c)]: cleanupGhostMediaStoreEntries()
        // TIDAK dipanggil di jalur ini. Fungsi itu murni untuk sinkronisasi index
        // MediaStore yang relevan HANYA waktu app menulis lewat java.io.File
        // langsung ke penyimpanan publik (§2 roadmap backend). File yang
        // dipindah lewat SAF/DocumentsContract tidak pernah masuk lewat jalur
        // penulisan itu, jadi tidak menghasilkan kelas "entri hantu" yang sama
        // -- DICATAT sebagai scope yang sengaja tidak diperluas, bukan lupa.

        ScanResult(moved, skipped, foldersUnreadable = false, overlapWarnings = overlapWarnings, skippedDetails = skippedDetails)
    }

    /**
     * [SAF] Analog [listCandidateFiles] untuk folder kustom: hanya level
     * TERATAS folder pilihan user (non-rekursif, sama seperti Downloads asli),
     * filter ekstensi zip/txt, buang file sementara/partial (reuse
     * [isTempOrPartialName] -- BUKAN salinan kedua, lihat syarat (c) Insiden
     * #7), dan buang defensif kalau kebetulan match folder "PromptVault" itu
     * sendiri (harusnya sudah otomatis kefilter oleh `isFile`, tapi dicek
     * eksplisit untuk paritas dengan pengecekan `vaultRootDir` di versi lama).
     */
    /**
     * [SAF] Analog [listCandidateFiles] untuk folder kustom: hanya level
     * TERATAS folder pilihan user (non-rekursif, sama seperti Downloads asli),
     * buang file sementara/partial (reuse [isTempOrPartialName] -- BUKAN
     * salinan kedua, lihat syarat (c) Insiden #7), dan buang defensif kalau
     * kebetulan match folder "PromptVault" itu sendiri. [Feature, dukung SEMUA
     * ekstensi -- 2026-08-13] Filter ekstensi zip/txt DIHAPUS TOTAL, sama
     * seperti [listCandidateFiles] -- lihat catatan di situ, alasan sama
     * persis berlaku di jalur SAF.
     *
     * [fix preview/scan-mismatch, 2026-08-13] `vaultRootDoc` sekarang NULLABLE
     * -- dipakai bersama dari SCAN sungguhan (dibuat lebih dulu lewat
     * `findOrCreateChildDirSaf`, tidak pernah null di jalur itu) MAUPUN
     * PREVIEW (dicari lewat `findFile()` read-only, `null` kalau folder belum
     * pernah dibuat -- tetap valid, artinya tidak ada apa pun untuk
     * dikecualikan).
     */
    private fun listCandidateFilesSaf(root: DocumentFile, vaultRootDoc: DocumentFile?): List<DocumentFile> {
        return try {
            root.listFiles().filter { doc ->
                val name = doc.name
                name != null && doc.isFile && doc.uri != vaultRootDoc?.uri &&
                    !isTempOrPartialName(name)
            }
        } catch (e: Exception) {
            // Fail-safe: provider SAF gagal/timeout query -> anggap tidak ada
            // kandidat scan INI, coba lagi scan berikutnya (bukan crash).
            emptyList()
        }
    }

    /**
     * [SAF] Dual Stability Guard versi folder kustom -- SENGAJA HANYA 2 dari 3
     * sinyal versi [isLikelyStillWriting] (dicatat eksplisit di PROJECT_STATE.md
     * sebagai known limitation, BUKAN diklaim setara): tidak ada pengecekan
     * file-lock (`RandomAccessFile.tryLock()`) karena tidak ada API resmi &
     * konsisten untuk itu pada `content://` URI lintas provider/OEM.
     *  1. `lastModified()` sebagai sinyal cepat -- TAPI banyak provider SAF
     *     melaporkan 0 kalau field ini tidak didukung; 0 SENGAJA tidak
     *     dianggap "baru saja diubah" (supaya provider yang tidak mengisi
     *     field ini tidak permanent-skip semua filenya).
     *  2. Kestabilan ukuran (`length()` dicek 2x dengan jeda) -- sinyal utama
     *     & satu-satunya yang reliable lintas provider untuk SAF.
     */
    private suspend fun isLikelyStillWritingSaf(doc: DocumentFile): Boolean {
        val lastModified = runCatching { doc.lastModified() }.getOrDefault(0L)
        if (lastModified > 0L) {
            val age = System.currentTimeMillis() - lastModified
            if (age in 0 until STABILITY_WINDOW_MS) return true
        }

        val sizeBefore = runCatching { doc.length() }.getOrDefault(-1L)
        if (sizeBefore < 0) return true

        delay(SIZE_CHECK_DELAY_MS)

        val sizeAfter = runCatching { doc.length() }.getOrDefault(-1L)
        return sizeAfter < 0 || sizeAfter != sizeBefore
    }

    /** [SAF] Cari subfolder bernama [name] di [parent]; buat baru kalau belum ada. `null` = gagal (jangan paksa lanjut). */
    private fun findOrCreateChildDirSaf(parent: DocumentFile, name: String): DocumentFile? {
        val existing = parent.findFile(name)
        if (existing != null) {
            return if (existing.isDirectory) existing else null // nama dipakai FILE, bukan folder -- konflik, jangan dipaksa
        }
        return try {
            parent.createDirectory(name)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * [SAF] Salin byte lewat ContentResolver -- SATU-SATUNYA cara yang
     * reliable lintas provider. `DocumentsContract.moveDocument()` SENGAJA
     * tidak dipakai walau lebih hemat I/O, karena dukungannya tidak konsisten
     * antar provider/OEM (alasan sama dengan kenapa Dual Stability Guard di
     * atas cuma 2/3 sinyal). Copy-lalu-delete lebih lambat tapi jauh lebih
     * predictable, konsisten dengan filosofi `copyThenDelete` di jalur
     * java.io.File yang sudah ada.
     */
    private fun copyDocumentBytes(src: DocumentFile, dest: DocumentFile): Boolean {
        return try {
            val resolver = context.contentResolver
            val input = resolver.openInputStream(src.uri) ?: return false
            input.use { streamIn ->
                val output = resolver.openOutputStream(dest.uri) ?: return false
                output.use { streamOut -> streamIn.copyTo(streamOut, bufferSize = 8 * 1024) }
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * [SAF] Cek rule match (murah) SEBELUM stability check (mahal) -- persis
     * pola [processCandidate], reuse [explainNoMatchByName] & [MoveOutcome] &
     * [CandidateOutcome] yang SAMA (bukan tipe hasil kedua yang independen).
     *
     * BEDA SENGAJA dari [processCandidate]: SELURUH badan fungsi ini dibungkus
     * try-catch, sesuatu yang TIDAK ada di versi java.io.File. Alasannya:
     * `doc.name`/`doc.length()` di sini adalah panggilan ke ContentProvider
     * LEWAT IPC (bisa gagal krn provider crash/dicabut izinnya di tengah scan),
     * beda karakteristik risiko dari `File.name`/`File.length()` yang murni
     * baca field lokal & praktis tidak pernah throw. Prinsip "expert-level
     * file organizer" di header file ini -- tiap file yang TIDAK dipindahkan
     * HARUS punya alasan spesifik, bukan cuma angka -- SENGAJA dipertahankan
     * di sini: satu file bermasalah jadi "dilewati dgn alasan", BUKAN
     * menjatuhkan seluruh batch `awaitAll()` di [scanAndSortSafLocked].
     */
    private suspend fun processCandidateSaf(
        doc: DocumentFile,
        rules: List<Rule>,
        conflictStrategy: ConflictStrategy,
        vaultRootDoc: DocumentFile
    ): CandidateOutcome {
        return try {
            val name = doc.name
            if (name == null) {
                return CandidateOutcome.Skipped(SkippedFileInfo("(nama tidak terbaca)", "Nama file tidak terbaca dari provider SAF."))
            }
            val sizeKb = runCatching { doc.length() / 1024 }.getOrDefault(0L)
            val matches = RuleOverlapChecker.matchingRules(name, sizeKb, rules)
            if (matches.isEmpty()) {
                return CandidateOutcome.Skipped(SkippedFileInfo(name, explainNoMatchByName(name, sizeKb, rules)))
            }

            if (isLikelyStillWritingSaf(doc)) {
                return CandidateOutcome.Skipped(
                    SkippedFileInfo(
                        fileName = name,
                        reason = "Ditunda: file baru saja berubah, kemungkinan masih ditulis/disalin. Akan dicoba lagi scan berikutnya."
                    )
                )
            }

            var overlapWarning: String? = null
            if (matches.size > 1) {
                overlapWarning = "\"$name\" cocok dengan ${matches.size} rule (${matches.joinToString { it.folderName }}). " +
                    "Dipindahkan memakai rule prioritas tertinggi: \"${matches.first().folderName}\"."
                activityLogRepository.add(LogLevel.WARNING, overlapWarning)
            }

            val rule = matches.first()
            when (moveFileSaf(doc, rule, conflictStrategy, vaultRootDoc)) {
                MoveOutcome.MOVED -> CandidateOutcome.Moved(overlapWarning)
                MoveOutcome.SKIPPED_CONFLICT -> CandidateOutcome.Skipped(
                    SkippedFileInfo(name, "Sudah ada file dengan nama sama di folder kustom/PromptVault/${rule.folderName}/ (strategi konflik: Lewati)"),
                    overlapWarning
                )
                MoveOutcome.FAILED -> CandidateOutcome.Skipped(
                    SkippedFileInfo(name, "Gagal dipindahkan (lihat Log untuk detail error)"),
                    overlapWarning
                )
            }
        } catch (e: CancellationException) {
            // [SAF] WAJIB diteruskan, TIDAK BOLEH ditelan oleh catch(Exception)
            // di bawah -- fungsi ini membungkus [isLikelyStillWritingSaf] yang
            // punya `delay(1 detik)` per file, jendela suspensi yang jauh lebih
            // lebar dari try-catch lain di file ini. Kalau scan dibatalkan
            // (mis. user tutup app) tepat di titik itu, cancellation HARUS
            // tetap menyebar ke `awaitAll()`/coroutine induk, bukan disalahartikan
            // sebagai "gagal proses file ini saja".
            throw e
        } catch (e: Exception) {
            val fallbackName = runCatching { doc.name }.getOrNull() ?: "(nama tidak terbaca)"
            activityLogRepository.add(LogLevel.ERROR, "Error tak terduga memproses \"$fallbackName\" (folder kustom): ${e.message}")
            CandidateOutcome.Skipped(SkippedFileInfo(fallbackName, "Error tak terduga saat memproses (lihat Log untuk detail)"))
        }
    }

    /**
     * [SAF] Analog [moveFile]. Copy-lalu-delete (lihat [copyDocumentBytes]),
     * BUKAN `DocumentsContract.moveDocument()`. Verifikasi nama aktual
     * pasca-`createFile()` -- pelajaran langsung Bug #2 (v2.10.0): provider
     * TIDAK SELALU memakai nama persis yang diminta.
     */
    private suspend fun moveFileSaf(
        doc: DocumentFile,
        rule: Rule,
        conflictStrategy: ConflictStrategy,
        vaultRootDoc: DocumentFile
    ): MoveOutcome {
        val originalName = doc.name ?: return MoveOutcome.FAILED
        return try {
            val destDir = findOrCreateChildDirSaf(vaultRootDoc, rule.folderName)
            if (destDir == null) {
                activityLogRepository.add(LogLevel.ERROR, "Gagal membuat/membuka folder tujuan \"${rule.folderName}\" di folder kustom.")
                return MoveOutcome.FAILED
            }

            var targetName = originalName
            val existingAtTarget = destDir.findFile(targetName)
            if (existingAtTarget != null) {
                when (conflictStrategy) {
                    ConflictStrategy.SKIP -> return MoveOutcome.SKIPPED_CONFLICT
                    ConflictStrategy.OVERWRITE -> existingAtTarget.delete()
                    ConflictStrategy.RENAME -> {
                        val base = originalName.substringBeforeLast('.', originalName)
                        val ext = originalName.substringAfterLast('.', "")
                        var counter = 1
                        while (destDir.findFile(targetName) != null) {
                            targetName = if (ext.isNotEmpty()) "${base}_$counter.$ext" else "${base}_$counter"
                            counter++
                        }
                    }
                }
            }

            val createdDoc = destDir.createFile(mimeTypeForFileName(originalName), targetName)
            if (createdDoc == null) {
                activityLogRepository.add(LogLevel.ERROR, "Gagal membuat file tujuan \"$targetName\" (provider SAF menolak).")
                return MoveOutcome.FAILED
            }

            // [pelajaran Bug #2, v2.10.0] Provider TIDAK SELALU memakai nama
            // persis yang diminta -- verifikasi, jangan percaya begitu saja.
            val actualName = createdDoc.name ?: targetName
            if (actualName != targetName) {
                activityLogRepository.add(LogLevel.WARNING, "Provider SAF mengubah nama \"$targetName\" menjadi \"$actualName\" saat membuat file.")
            }

            if (!copyDocumentBytes(doc, createdDoc)) {
                runCatching { createdDoc.delete() } // bersihkan file tujuan setengah-jadi
                activityLogRepository.add(LogLevel.ERROR, "Gagal menyalin isi \"$originalName\" ke folder kustom.")
                return MoveOutcome.FAILED
            }

            val originalParentDoc = doc.parentFile ?: vaultRootDoc.parentFile ?: vaultRootDoc
            val deleteOk = doc.delete()
            if (!deleteOk) {
                // Salinan ke tujuan SUDAH sukses & lengkap -- BUKAN dianggap
                // gagal total (itu akan menyesatkan: file sebenarnya AMAN di
                // tujuan). Dicatat sebagai WARNING karena file sumber masih
                // ada (potensi duplikat), tapi tetap lanjut sebagai MOVED
                // supaya MoveHistory konsisten dengan apa yang benar-benar
                // ada di tujuan.
                activityLogRepository.add(LogLevel.WARNING, "\"$originalName\" tersalin ke tujuan, tapi berkas asli di folder kustom gagal dihapus (provider menolak).")
            }

            moveHistoryRepository.record(
                MoveHistoryEntry(
                    id = UUID.randomUUID().toString(),
                    timestampMillis = System.currentTimeMillis(),
                    fileName = actualName,
                    originalParentUri = originalParentDoc.uri.toString(),
                    destUri = createdDoc.uri.toString(),
                    ruleFolderName = rule.folderName
                )
            )
            activityLogRepository.add(LogLevel.SUCCESS, "\"$originalName\" -> folder kustom/PromptVault/${rule.folderName}/")
            MoveOutcome.MOVED
        } catch (e: Exception) {
            activityLogRepository.add(LogLevel.ERROR, "Error memindahkan \"$originalName\" (folder kustom): ${e.message}")
            MoveOutcome.FAILED
        }
    }

    /**
     * [SAF] Analog [undo] untuk entri yang dipindahkan lewat folder kustom.
     * Dipanggil dari [undo] berdasarkan prefix `destUri` ("content://" vs
     * path biasa) -- SENGAJA TIDAK perlu kolom/skema DB baru sama sekali:
     * [MoveHistoryEntry.originalParentUri]/[MoveHistoryEntry.destUri] SUDAH
     * berupa `String` polos, jadi URI SAF & path File sama-sama muat tanpa
     * migrasi Room apa pun (DB Schema/DAO protected asset TIDAK disentuh).
     */
    private suspend fun undoSaf(entry: MoveHistoryEntry): Boolean {
        return try {
            val current = DocumentFile.fromSingleUri(context, Uri.parse(entry.destUri))
            if (current == null || !current.exists()) {
                activityLogRepository.add(LogLevel.ERROR, "Undo gagal: \"${entry.fileName}\" sudah tidak ada di tujuan (folder kustom).")
                return false
            }

            val originalParent = DocumentFile.fromSingleUri(context, Uri.parse(entry.originalParentUri))
            if (originalParent == null || !originalParent.exists() || !originalParent.isDirectory) {
                activityLogRepository.add(LogLevel.ERROR, "Undo gagal: folder asal \"${entry.fileName}\" (folder kustom) sudah tidak ada/tidak bisa diakses.")
                return false
            }

            var restoreName = entry.fileName
            var restoreCounter = 1
            while (originalParent.findFile(restoreName) != null) {
                val base = entry.fileName.substringBeforeLast('.', entry.fileName)
                val ext = entry.fileName.substringAfterLast('.', "")
                restoreName = if (ext.isNotEmpty()) "${base}_restored_$restoreCounter.$ext" else "${base}_restored_$restoreCounter"
                restoreCounter++
            }

            val restoredDoc = originalParent.createFile(mimeTypeForFileName(entry.fileName), restoreName)
            if (restoredDoc == null) {
                activityLogRepository.add(LogLevel.ERROR, "Undo gagal: tidak bisa membuat \"$restoreName\" di folder asal (folder kustom).")
                return false
            }

            if (!copyDocumentBytes(current, restoredDoc)) {
                runCatching { restoredDoc.delete() }
                activityLogRepository.add(LogLevel.ERROR, "Undo gagal: tidak bisa menyalin isi \"${entry.fileName}\" kembali (folder kustom).")
                return false
            }

            val deleteOk = current.delete()
            moveHistoryRepository.markUndone(entry.id)
            activityLogRepository.add(LogLevel.SUCCESS, "Undo berhasil: \"${entry.fileName}\" dikembalikan (folder kustom).")
            if (!deleteOk) {
                activityLogRepository.add(LogLevel.WARNING, "Undo \"${entry.fileName}\": salinan balik sukses, tapi file di PromptVault (folder kustom) gagal dihapus otomatis.")
            }
            true
        } catch (e: Exception) {
            activityLogRepository.add(LogLevel.ERROR, "Error saat undo \"${entry.fileName}\" (folder kustom): ${e.message}")
            false
        }
    }

    /**
     * Cari baris MediaStore yang path-nya di bawah Downloads/PromptVault/ TAPI
     * file fisiknya sudah tidak ada di disk (entri "hantu") -- lalu hapus baris
     * itu. Kenapa bisa "hantu": app ini pakai `java.io.File` langsung (bukan
     * SAF, lihat Keputusan Arsitektur #2 di PROJECT_STATE.md), jadi rename/
     * delete lewat filesystem tidak otomatis sinkron ke index MediaStore kalau
     * ada app LAIN yang sempat baca/index file itu duluan sebelum dipindah.
     * `MediaStore.Files.FileColumns.DATA` deprecated sejak API 29, tapi TETAP
     * berfungsi untuk app dengan `MANAGE_EXTERNAL_STORAGE` (app ini sudah
     * pakai izin itu).
     */
    private suspend fun cleanupGhostMediaStoreEntries() {
        try {
            val resolver = context.contentResolver
            val collection = MediaStore.Files.getContentUri("external")
            val projection = arrayOf(MediaStore.Files.FileColumns._ID, MediaStore.Files.FileColumns.DATA)
            val selection = "${MediaStore.Files.FileColumns.DATA} LIKE ?"
            val selectionArgs = arrayOf("${vaultRootDir.absolutePath}%")
            var removedCount = 0

            resolver.query(collection, projection, selection, selectionArgs, null)?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
                val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATA)
                while (cursor.moveToNext()) {
                    val path = cursor.getString(dataColumn) ?: continue
                    if (!File(path).exists()) {
                        val id = cursor.getLong(idColumn)
                        val itemUri = ContentUris.withAppendedId(collection, id)
                        if (resolver.delete(itemUri, null, null) > 0) removedCount++
                    }
                }
            }

            if (removedCount > 0) {
                activityLogRepository.add(LogLevel.INFO, "$removedCount entri MediaStore usang (file sudah tidak ada) dibersihkan.")
            }
        } catch (e: Exception) {
            // Non-fatal dengan sengaja -- kegagalan cleanup kosmetik ini TIDAK
            // BOLEH pernah menggagalkan/membatalkan hasil scan utama yang nyata.
        }
    }

    /** Hasil pemrosesan satu file kandidat, dikumpulkan lewat awaitAll() lalu digabung sekuensial di [scanAndSortLocked]. */
    private sealed class CandidateOutcome(val overlapWarning: String?) {
        class Moved(overlapWarning: String?) : CandidateOutcome(overlapWarning)
        class Skipped(val info: SkippedFileInfo, overlapWarning: String? = null) : CandidateOutcome(overlapWarning)
    }

    /**
     * Cek rule match (murah) SEBELUM stability check (mahal: delay + buka file
     * handle) -- lihat penjelasan §2 di [scanAndSortLocked]. Dipanggil paralel
     * lewat Semaphore, aman karena tidak menyentuh state yang dibagi lintas
     * pemanggilan (moveFile/activityLogRepository/moveHistoryRepository sudah
     * masing-masing aman dipanggil concurrent).
     */
    private suspend fun processCandidate(file: File, rules: List<Rule>, conflictStrategy: ConflictStrategy): CandidateOutcome {
        val sizeKb = file.sizeKb()
        val matches = RuleOverlapChecker.matchingRules(file.name, sizeKb, rules)
        if (matches.isEmpty()) {
            return CandidateOutcome.Skipped(SkippedFileInfo(file.name, explainNoMatch(file, sizeKb, rules)))
        }

        if (isLikelyStillWriting(file)) {
            return CandidateOutcome.Skipped(
                SkippedFileInfo(
                    fileName = file.name,
                    reason = "Ditunda: file baru saja berubah, kemungkinan masih ditulis/didownload. Akan dicoba lagi scan berikutnya."
                )
            )
        }

        var overlapWarning: String? = null
        if (matches.size > 1) {
            overlapWarning = "\"${file.name}\" cocok dengan ${matches.size} rule (${matches.joinToString { it.folderName }}). " +
                "Dipindahkan memakai rule prioritas tertinggi: \"${matches.first().folderName}\"."
            activityLogRepository.add(LogLevel.WARNING, overlapWarning)
        }

        val rule = matches.first()
        return when (moveFile(file, rule, conflictStrategy)) {
            MoveOutcome.MOVED -> CandidateOutcome.Moved(overlapWarning)
            MoveOutcome.SKIPPED_CONFLICT -> CandidateOutcome.Skipped(
                SkippedFileInfo(file.name, "Sudah ada file dengan nama sama di PromptVault/${rule.folderName}/ (strategi konflik: Lewati)"),
                overlapWarning
            )
            MoveOutcome.FAILED -> CandidateOutcome.Skipped(
                SkippedFileInfo(file.name, "Gagal dipindahkan (lihat Log untuk detail error)"),
                overlapWarning
            )
        }
    }

    private fun explainNoMatch(file: File, sizeKb: Long, rules: List<Rule>): String =
        explainNoMatchByName(file.name, sizeKb, rules)

    /** Versi berbasis nama-file dari [explainNoMatch], dipisah supaya generic terhadap nama saja. */
    private fun explainNoMatchByName(name: String, sizeKb: Long, rules: List<Rule>): String {
        val excludedBy = rules.firstOrNull {
            GlobMatcher.matchesAny(name, it.pattern) && RuleOverlapChecker.isExcluded(name, it)
        }
        if (excludedBy != null) {
            return "Cocok pattern \"${excludedBy.pattern}\" tapi dikecualikan oleh excludePattern \"${excludedBy.excludePattern}\" di rule \"${excludedBy.folderName}\""
        }
        val sizeMismatch = rules.firstOrNull {
            GlobMatcher.matchesAny(name, it.pattern) && !RuleOverlapChecker.matchesSizeConstraint(sizeKb, it)
        }
        if (sizeMismatch != null) {
            val range = listOfNotNull(
                sizeMismatch.minSizeKb?.let { "min ${it}KB" },
                sizeMismatch.maxSizeKb?.let { "maks ${it}KB" }
            ).joinToString(", ")
            return "Cocok pattern rule \"${sizeMismatch.folderName}\" tapi ukuran file (${sizeKb}KB) di luar batas rule ($range)"
        }
        val activePatterns = rules.joinToString(", ") { "\"${it.pattern}\"" }
        return "Tidak cocok pattern rule manapun (rule aktif: $activePatterns)"
    }

    /**
     * Uji pattern include+exclude (belum tentu tersimpan sebagai rule) terhadap
     * isi folder sumber AKTIF SAAT INI. Dipakai di layar Tambah/Edit Rule supaya
     * user lihat langsung dampak pattern-nya SEBELUM menyimpan rule. Mendukung
     * multi-pattern CSV.
     *
     * [fix bug real -- 2026-08-13, laporan user "preview cocok tapi scan bilang
     * tidak ada"] SEBELUMNYA fungsi ini HARDCODE selalu cek [downloadsDir]
     * (java.io.File biasa) TIDAK PEDULI folder kustom SAF sudah dipilih atau
     * belum -- preview & scan sesungguhnya ([scanAndSort]) bisa mengecek DUA
     * folder yang BERBEDA TOTAL. User yang taruh file di folder kustom lihat
     * preview "cocok!" (dari isi Downloads, bukan folder kustomnya), lalu scan
     * asli (yang benar mengarah ke folder kustom sejak fix P0 audit SAF)
     * melapor "tidak ada file cocok" -- keduanya BENAR menurut sumbernya
     * masing-masing, tapi user cuma lihat satu sumber yang salah.
     *
     * Sekarang preview reuse [resolveSafRoot] PERSIS sama seperti [scanAndSort]
     * -- SATU logika pemilihan sumber untuk preview & scan sungguhan, supaya
     * kelas bug "preview vs scan lihat folder beda" tidak bisa terulang lewat
     * cabang logika kedua yang independen (pelajaran sama dgn syarat (c)
     * Insiden #7: satu implementasi, bukan disalin/didekati ulang).
     *
     * Folder "PromptVault" di jalur SAF dicari lewat `findFile()` (BACA SAJA,
     * TIDAK dibuat) -- preview jalan tiap 400ms debounce ketikan, TIDAK boleh
     * bikin folder muncul sebagai efek samping ketik pattern sebelum rule
     * disimpan. Kalau folder itu belum ada, `null` aman -- artinya memang belum
     * ada apa pun untuk dikecualikan dari daftar kandidat.
     */
    suspend fun previewPatternMatches(pattern: String, excludePattern: String = ""): PatternPreviewResult =
        withContext(Dispatchers.IO) {
            if (pattern.isBlank()) return@withContext PatternPreviewResult(0, emptyList())
            when (val resolution = resolveSafRoot()) {
                is SafRootResolution.Active -> {
                    val vaultRootDoc = resolution.root.findFile("PromptVault")
                    val names = listCandidateFilesSaf(resolution.root, vaultRootDoc).mapNotNull { it.name }
                    buildPreviewResult(names, pattern, excludePattern)
                }
                SafRootResolution.NotConfigured -> {
                    if (!downloadsDir.exists() || !downloadsDir.canRead()) {
                        return@withContext PatternPreviewResult(0, emptyList())
                    }
                    val names = listCandidateFiles().map { it.name }
                    buildPreviewResult(names, pattern, excludePattern)
                }
                // Akses folder kustom hilang -- preview TIDAK boleh diam-diam
                // balik ke Downloads (itu persis P0 #2 lama), jujur kosong saja,
                // konsisten dengan [scanAndSort] yang juga berhenti di kondisi ini.
                is SafRootResolution.AccessLost -> PatternPreviewResult(0, emptyList())
            }
        }

    private fun buildPreviewResult(candidateNames: List<String>, pattern: String, excludePattern: String): PatternPreviewResult {
        val matched = candidateNames
            .filter { GlobMatcher.matchesAny(it, pattern) }
            .filterNot { excludePattern.isNotBlank() && GlobMatcher.matchesAny(it, excludePattern) }
        return PatternPreviewResult(candidateNames.size, matched)
    }

    /** Daftar nama file asli (SEMUA ekstensi, sejak fix 2026-08-13) di Downloads, dipakai layar Diagnostik agar user tahu format nama file sebenarnya. */
    fun listDownloadsCandidateFileNames(limit: Int = 100): List<String> {
        if (!downloadsDir.exists() || !downloadsDir.canRead()) return emptyList()
        return listCandidateFiles().map { it.name }.sorted().take(limit)
    }

    private enum class MoveOutcome { MOVED, SKIPPED_CONFLICT, FAILED }

    private suspend fun moveFile(file: File, rule: Rule, conflictStrategy: ConflictStrategy): MoveOutcome {
        return try {
            val destDir = File(vaultRootDir, rule.folderName)
            if (!destDir.exists()) destDir.mkdirs()

            var destFile = File(destDir, file.name)
            if (destFile.exists()) {
                when (conflictStrategy) {
                    ConflictStrategy.SKIP -> return MoveOutcome.SKIPPED_CONFLICT
                    ConflictStrategy.OVERWRITE -> destFile.delete()
                    ConflictStrategy.RENAME -> {
                        var counter = 1
                        while (destFile.exists()) {
                            destFile = File(destDir, "${file.nameWithoutExtension}_$counter.${file.extension}")
                            counter++
                        }
                    }
                }
            }

            val originalParent = file.parentFile?.absolutePath ?: downloadsDir.absolutePath
            val success = file.renameTo(destFile) || copyThenDelete(file, destFile)

            if (success) {
                // §2 roadmap backend -- Ghost/stale MediaStore entry: app pindah file
                // lewat java.io.File langsung (bukan MediaStore API), jadi index
                // MediaStore TIDAK otomatis update. Tanpa scanFile ini, file manager
                // bawaan/app lain yang baca lewat MediaStore (bukan lewat FS langsung)
                // bisa masih nunjukkin file di lokasi LAMA (sudah tidak ada / "ghost"),
                // dan file di lokasi BARU belum ke-index sampai user reboot/scan manual.
                // Non-fatal dengan sengaja: kalau scanFile gagal/exception, pemindahan
                // filenya SENDIRI sudah sukses duluan di atas -- jangan sampai indexing
                // MediaStore yang notabene kosmetik menggagalkan hasil MOVED yang nyata.
                try {
                    MediaScannerConnection.scanFile(context, arrayOf(file.absolutePath, destFile.absolutePath), null, null)
                } catch (_: Exception) { /* non-fatal, lihat komentar di atas */ }

                moveHistoryRepository.record(
                    MoveHistoryEntry(
                        id = UUID.randomUUID().toString(),
                        timestampMillis = System.currentTimeMillis(),
                        fileName = destFile.name,
                        originalParentUri = originalParent,
                        destUri = destFile.absolutePath,
                        ruleFolderName = rule.folderName
                    )
                )
                activityLogRepository.add(LogLevel.SUCCESS, "\"${file.name}\" -> PromptVault/${rule.folderName}/")
                MoveOutcome.MOVED
            } else {
                activityLogRepository.add(LogLevel.ERROR, "Gagal memindahkan \"${file.name}\".")
                MoveOutcome.FAILED
            }
        } catch (e: Exception) {
            activityLogRepository.add(LogLevel.ERROR, "Error memindahkan \"${file.name}\": ${e.message}")
            MoveOutcome.FAILED
        }
    }

    private fun copyThenDelete(src: File, dest: File): Boolean {
        return try {
            src.copyTo(dest, overwrite = false)
            src.delete()
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * UNDO satu entri riwayat pemindahan (fitur lengkap sejak v2.11.0, lihat
     * ActivityLogScreen). [SAF] Cabang ke [undoSaf] kalau `destUri` berupa URI
     * konten ("content://...") -- lihat komentar di [undoSaf] soal kenapa ini
     * tidak butuh kolom/skema DB baru. Karakteristik dispatcher fungsi ini
     * SENGAJA tidak diubah (masih tanpa `withContext` sendiri, sama seperti
     * sebelumnya) supaya jalur SAF & jalur File tetap paritas -- lihat catatan
     * di PROJECT_STATE.md soal potensi I/O di thread pemanggil.
     */
    suspend fun undo(entry: MoveHistoryEntry): Boolean {
        if (entry.destUri.startsWith("content://")) {
            return undoSaf(entry)
        }
        return try {
            val current = File(entry.destUri)
            if (!current.exists()) {
                activityLogRepository.add(LogLevel.ERROR, "Undo gagal: \"${entry.fileName}\" sudah tidak ada di tujuan.")
                return false
            }
            val originalDir = File(entry.originalParentUri)
            if (!originalDir.exists()) originalDir.mkdirs()

            var restoreTarget = File(originalDir, entry.fileName)
            var counter = 1
            while (restoreTarget.exists()) {
                restoreTarget = File(originalDir, "${current.nameWithoutExtension}_restored_$counter.${current.extension}")
                counter++
            }

            val success = current.renameTo(restoreTarget) || copyThenDelete(current, restoreTarget)
            if (success) {
                try {
                    MediaScannerConnection.scanFile(context, arrayOf(current.absolutePath, restoreTarget.absolutePath), null, null)
                } catch (_: Exception) { /* non-fatal, sama seperti di moveFile() */ }

                moveHistoryRepository.markUndone(entry.id)
                activityLogRepository.add(LogLevel.SUCCESS, "Undo berhasil: \"${entry.fileName}\" dikembalikan ke Downloads.")
            } else {
                activityLogRepository.add(LogLevel.ERROR, "Undo gagal untuk \"${entry.fileName}\".")
            }
            success
        } catch (e: Exception) {
            activityLogRepository.add(LogLevel.ERROR, "Error saat undo \"${entry.fileName}\": ${e.message}")
            false
        }
    }

    companion object {
        /**
         * Dibagi lintas SEMUA instance FileSorter dalam proses yang sama --
         * lihat penjelasan di [scanAndSort]. Sengaja di companion object
         * (bukan property instance) karena MainViewModel dan AutoSortWorker
         * masing-masing membuat instance FileSorter baru sendiri-sendiri.
         */
        private val scanMutex = Mutex()

        /** Jeda aman sebelum file dianggap "selesai ditulis" dan boleh dipindah. */
        private const val STABILITY_WINDOW_MS = 5_000L

        /** Jeda pengecekan ukuran file untuk Dual Stability Guard (§4). */
        private const val SIZE_CHECK_DELAY_MS = 1_000L

        /**
         * [perf-overhaul v2.4.0] Batas jumlah file yang diproses BERSAMAAN saat
         * scan (lihat [processCandidate]). Nilai ini ASUMSI TEKNIS AI (dicatat
         * di PROJECT_STATE.md): 6 dipilih sebagai titik tengah -- cukup tinggi
         * untuk memangkas wall-time stability-check (delay 1 detik/file) secara
         * signifikan, cukup rendah untuk tidak membuka terlalu banyak file
         * handle/RandomAccessFile bersamaan di HP kelas menengah-bawah. Kalau
         * ke depan user punya HP dengan Downloads berisi ribuan file dan masih
         * terasa berat, ini kandidat pertama untuk di-tuning (naikkan concurrency
         * atau buat konfigurable), bukan trigger untuk redesain ulang.
         */
        private const val SCAN_CONCURRENCY = 6

        /** Akhiran nama file dari browser/downloader yang menandakan unduhan belum selesai. */
        private val TEMP_FILE_MARKERS = listOf(
            ".crdownload", ".tmp", ".part", ".download", ".downloading"
        )
    }
}
