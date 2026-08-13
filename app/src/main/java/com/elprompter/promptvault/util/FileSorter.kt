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
 * dukungan ekstensi lain sudah didapat dari [listCandidateFiles] yang tidak
 * lagi memfilter ekstensi sama sekali (lihat catatan di situ). TIDAK memakai
 * `android.webkit.MimeTypeMap` di sini
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
 * Logika inti: scan folder Downloads (SELALU, tidak pernah folder lain --
 * lihat catatan arsitektur di [FileSorter.scanAndSort]), cocokkan tiap file
 * terhadap rule aktif (berurutan sesuai PRIORITAS, mendukung multi-pattern &
 * filter ukuran), lalu pindahkan ke Downloads/PromptVault/<folderName>/ ATAU,
 * kalau user sudah memilih folder tujuan kustom lewat SAF, ke <folder
 * kustom>/PromptVault/<folderName>/ -- dan catat riwayat untuk undo.
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
     *
     * [Fix bug nyata, 2026-08-13, laporan user: file/apk bernama persis
     * "PromptVault" (atau apa pun yang DIAWALI teks itu, mis. "PromptVault.apk")
     * tidak pernah terdeteksi] Root cause: pengecualian folder output sendiri
     * di bawah memakai `absolutePath.startsWith(vaultRootDir.absolutePath)`
     * TANPA separator -- itu string-prefix match, bukan path-containment
     * check. "Downloads/PromptVault.apk".startsWith("Downloads/PromptVault")
     * bernilai true walau file itu SIBLING folder PromptVault, bukan isinya --
     * jadi ikut ter-exclude dari kandidat scan. Fix: tambah `File.separator`
     * di akhir prefix pembanding, supaya hanya path yang BENAR-BENAR di dalam
     * folder (mis. "Downloads/PromptVault/x.txt") yang cocok.
     */
    private fun listCandidateFiles(): Array<File> {
        return downloadsDir.listFiles { f ->
            f.isFile &&
                !isTempOrPartialFile(f) &&
                !f.absolutePath.startsWith(vaultRootDir.absolutePath + File.separator)
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
     * Versi berbasis nama-saja dari [isTempOrPartialFile]. [SAF v2,
     * restrukturisasi 2026-08-13] Dulu diekstrak spesifik supaya jalur scanner
     * SAF (`listCandidateFilesSaf`, kini dihapus -- lihat catatan arsitektur
     * di [scanAndSort]) bisa reuse logika yang sama; sumber scan sekarang
     * SELALU Downloads jadi alasan itu tidak lagi relevan, tapi fungsi ini
     * tetap dipertahankan sebagai versi nama-saja yang lebih generik.
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
     *
     * [SAF v2 -- restrukturisasi arsitektur, 2026-08-13, SAF_FINAL_VERDICT_FIX.txt]
     * ROOT CAUSE ditemukan: seluruh implementasi SAF v2.17.0-v2.18.1 salah
     * menafsirkan requirement -- SAF diperlakukan sebagai SUMBER SCAN
     * alternatif (folder kustom dipindai SENDIRI, terpisah dari Downloads),
     * padahal makna SAF yang BENAR untuk app ini adalah TUJUAN penyimpanan
     * kustom yang dipilih user, bukan sumber scan. `scanAndSortSafLocked()`
     * dan `listCandidateFilesSaf()` sebagai SCANNER dihapus total.
     *
     * ARSITEKTUR BARU (tidak lagi bercabang "Downloads ATAU folder kustom"
     * sebagai DUA sumber scan independen):
     *   SUMBER SCAN = SELALU Downloads ([listCandidateFiles], tidak berubah).
     *   TUJUAN = Downloads/PromptVault/<rule>/ (java.io.File) KALAU folder
     *   kustom belum diset, ATAU <folder kustom>/PromptVault/<rule>/ (SAF/
     *   DocumentFile) kalau sudah. [resolveSafDestinationRoot] HANYA dipakai
     *   untuk resolusi TUJUAN sekarang -- titik cabang pindah dari "sumber
     *   scan mana" ke "tulis hasil ke mana", di [processCandidate].
     */
    suspend fun scanAndSort(): ScanResult = scanMutex.withLock {
        // Titik cabang TUNGGAL untuk resolusi folder TUJUAN kustom -- AMAN
        // dipanggil dari MainViewModel MAUPUN AutoSortWorker tanpa perubahan
        // apa pun di kedua caller itu, karena signature scanAndSort() TIDAK
        // berubah. scanMutex yang sama tetap menaungi (race-fix lama, lihat
        // komentar di companion object).
        //
        // [fix audit P0 #2, 2026-08-12, TETAP BERLAKU di arsitektur baru]
        // resolveSafDestinationRoot() TIDAK PERNAH collapse "belum diset" DAN
        // "sudah diset tapi rusak/akses hilang" jadi satu `null` yang sama --
        // hanya NotConfigured yang boleh fallback ke tujuan Downloads biasa;
        // AccessLost WAJIB berhenti + lapor error, TIDAK PERNAH diam-diam
        // pindah ke Downloads sebagai tujuan pengganti (rule #8 spesifikasi:
        // "Jangan silent fallback ke Downloads ketika custom SAF destination
        // gagal").
        when (val resolution = resolveSafDestinationRoot()) {
            is SafDestinationResolution.Active -> scanAndSortToDestination(resolution.root)
            SafDestinationResolution.NotConfigured -> scanAndSortToDestination(null)
            is SafDestinationResolution.AccessLost -> {
                activityLogRepository.add(
                    LogLevel.ERROR,
                    "Folder tujuan kustom tidak bisa diakses (${resolution.reason}). Scan DIHENTIKAN, " +
                        "TIDAK fallback ke Downloads/PromptVault supaya file tidak salah tersortir ke tempat " +
                        "yang tidak kamu duga. Pilih ulang folder tujuan atau kembali ke Downloads lewat Pengaturan."
                )
                ScanResult(0, 0, foldersUnreadable = false, safAccessLost = true, overlapWarnings = emptyList())
            }
        }
    }

    /**
     * [SAF v2, fix audit P0 #2 -- SAF_FINAL_LOGIC_AUDIT.md 2026-08-12] State
     * eksplisit hasil resolusi folder TUJUAN kustom -- BUKAN `DocumentFile?`
     * polos (null berarti dua hal berbeda sekaligus: "belum diset" DAN
     * "rusak/akses hilang", audit menandai ini P0 fatal). [Rename 2026-08-13,
     * SAF_FINAL_VERDICT_FIX.txt] Nama lama `SafRootResolution` diganti
     * `SafDestinationResolution` -- bukan cuma kosmetik: root cause seluruh
     * insiden SAF di batch ini adalah SALAH MENAFSIRKAN peran SAF (sumber vs
     * tujuan), nama tipe yang jelas adalah bagian dari fix, bukan detail.
     */
    private sealed class SafDestinationResolution {
        data class Active(val root: DocumentFile) : SafDestinationResolution()
        data object NotConfigured : SafDestinationResolution()
        data class AccessLost(val reason: String) : SafDestinationResolution()
    }

    /**
     * [SAF v2] Resolusi folder TUJUAN kustom dari URI tersimpan di
     * [SettingsRepository] -- BUKAN lagi resolusi "sumber scan alternatif"
     * (lihat catatan arsitektur di [scanAndSort]). Tiga hasil eksplisit
     * (lihat [SafDestinationResolution]) supaya caller ([scanAndSort] &
     * [checkSafAccessLost]) tidak pernah salah memperlakukan "akses hilang"
     * sebagai "memang belum diset". Logika internal TIDAK berubah dari
     * `resolveSafRoot()` lama -- cuma nama & dokumentasi peran yang
     * diperjelas, karena logika validasi URI/permission-nya sendiri sudah
     * benar (lihat "YANG TETAP VALID" di SAF_FINAL_VERDICT_FIX.txt).
     */
    private suspend fun resolveSafDestinationRoot(): SafDestinationResolution {
        val uriString = settingsRepository.getSafTreeUri() ?: return SafDestinationResolution.NotConfigured
        return try {
            val doc = DocumentFile.fromTreeUri(context, Uri.parse(uriString))
            when {
                doc == null -> SafDestinationResolution.AccessLost("tree URI tidak valid")
                !doc.exists() -> SafDestinationResolution.AccessLost("folder tidak ditemukan -- mungkin dihapus/dipindah")
                !doc.isDirectory -> SafDestinationResolution.AccessLost("target bukan folder")
                else -> SafDestinationResolution.Active(doc)
            }
        } catch (e: SecurityException) {
            // [fix audit P0 #1, bagian "validasi permission"] Ini persis kasus
            // izin persistable dicabut dari luar app (mis. user cabut manual
            // lewat Pengaturan Android, atau OS reclaim saat limit provider
            // tercapai) -- SEBELUMNYA ditelan jadi `null`/fallback diam-diam.
            SafDestinationResolution.AccessLost("izin akses dicabut")
        } catch (e: Exception) {
            SafDestinationResolution.AccessLost("error tak terduga: ${e.message ?: e::class.simpleName}")
        }
    }

    /**
     * [SAF v2, fix audit P0 #1 -- "validasi permission saat startup"] Cek
     * status akses folder TUJUAN kustom TANPA menjalankan scan apa pun --
     * dipanggil [MainViewModel] saat startup & setiap kali URI folder kustom
     * berubah, supaya user diberi tahu akses sudah hilang SEBELUM scan
     * berikutnya (manual atau AutoSortWorker latar belakang) diam-diam
     * gagal/fallback. Return `false` kalau belum diset SAMA SEKALI (bukan
     * error, memang pakai Downloads sebagai tujuan) ATAU kalau folder aktif
     * & sehat.
     */
    suspend fun checkSafAccessLost(): Boolean = withContext(Dispatchers.IO) {
        resolveSafDestinationRoot() is SafDestinationResolution.AccessLost
    }

    /**
     * [perf-overhaul v2.4.0, tetap berlaku] Tiga masalah performa yang
     * menyebabkan app "kewalahan" bahkan di ratusan file, sekarang
     * diperbaiki sekaligus:
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
     * [SAF v2, restrukturisasi 2026-08-13] GANTI TOTAL `scanAndSortLocked()` +
     * `scanAndSortSafLocked()` (dua scanner independen, salah arsitektur --
     * lihat catatan di [scanAndSort]) jadi SATU fungsi ini: sumber scan
     * SELALU [listCandidateFiles] (Downloads, tidak pernah SAF); [destinationRoot]
     * HANYA menentukan KE MANA hasil match ditulis (diteruskan ke
     * [processCandidate] per-file). `null` = tujuan Downloads/PromptVault/
     * biasa (java.io.File, [moveFile]); non-null = tujuan folder kustom SAF
     * (DocumentFile, [moveFileToSafDestination]).
     *
     * Hasil per-file dikumpulkan lewat `awaitAll()` lalu digabung SEKUENSIAL
     * di luar coroutine paralel (bukan mutable var dibagi lintas coroutine),
     * supaya `moved`/`skipped`/`overlapWarnings` tetap aman tanpa race
     * condition maupun butuh Mutex tambahan.
     */
    private suspend fun scanAndSortToDestination(destinationRoot: DocumentFile?): ScanResult = withContext(Dispatchers.IO) {
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

        // [SAF, race-fix 2026-08-13 -- lihat dokumentasi lengkap di
        // resolveSafRuleDestinations()] Folder tujuan SAF (root "PromptVault" +
        // subfolder tiap rule) di-resolve SEKALI DI SINI, SERIAL, SEBELUM file
        // diproses paralel di bawah -- BUKAN lagi per-file di dalam
        // moveFileToSafDestination() seperti sebelumnya (sumber duplikat folder).
        val safRuleDestinations: Map<String, DocumentFile?> =
            if (destinationRoot != null) resolveSafRuleDestinations(destinationRoot, rules) else emptyMap()

        val semaphore = Semaphore(SCAN_CONCURRENCY)
        val results = candidateFiles.map { file ->
            async { semaphore.withPermit { processCandidate(file, rules, conflictStrategy, destinationRoot, safRuleDestinations) } }
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

        // §2 roadmap backend -- bersihkan entri MediaStore "hantu". HANYA
        // relevan kalau tujuan adalah filesystem lokal (java.io.File) --
        // kalau tujuan folder kustom SAF, file tidak pernah lewat jalur
        // penulisan lokal yang menyebabkan entri "hantu" ini muncul.
        if (destinationRoot == null) {
            cleanupGhostMediaStoreEntries()
        }

        ScanResult(moved, skipped, foldersUnreadable = false, overlapWarnings = overlapWarnings, skippedDetails = skippedDetails)
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
     * [SAF, race-fix -- 2026-08-13, laporan user: screenshot 4x folder
     * "PromptVault"/"PromptVault (1)"/"(2)"/"(3)" masing-masing isi 1 item,
     * tanggal sama] Resolusi folder tujuan SAF (root "PromptVault" + subfolder
     * TIAP rule aktif) SEKALI, SERIAL, DI SINI -- SEBELUM file kandidat
     * diproses paralel di [scanAndSortToDestination]. BUKAN lagi per-file DI
     * DALAM [moveFileToSafDestination] seperti sebelumnya.
     *
     * **ROOT CAUSE bug**: [findOrCreateChildDirSaf] sebelumnya dipanggil
     * terpisah per-file, DI DALAM tiap coroutine paralel (s/d [SCAN_CONCURRENCY]
     * = 6 file bersamaan, lihat Keputusan Arsitektur #6 di PROJECT_STATE.md).
     * `DocumentFile.createDirectory()` TIDAK atomik/tidak idempoten seperti
     * `File.mkdirs()` -- kalau 2+ coroutine SAMA-SAMA memanggil
     * `parent.findFile("PromptVault")` SEBELUM salah satu sempat selesai
     * `createDirectory("PromptVault")`, KEDUANYA melihat "belum ada" -> KEDUANYA
     * memanggil createDirectory() -> provider SAF tidak menolak/gagal, malah
     * auto-suffix nama biar tetap unik -> hasilnya 2+ folder terpisah bernama
     * "PromptVault", "PromptVault (1)", dst, masing-masing cuma kebagian file
     * dari coroutine yang kebetulan menciptakannya duluan (persis gejala
     * "1 item" di tiap folder pada laporan user). Classic TOCTOU race --
     * [scanMutex] di [scanAndSort] TIDAK mencegah ini: mutex itu cuma
     * menyerialkan ANTAR scan (manual vs AutoSortWorker), bukan antar file
     * DALAM satu scan yang SENGAJA diparalelkan sejak v2.4.0.
     *
     * **Fix STRUKTURAL** (bukan tambal Mutex tepat di titik race): folder
     * dibuat/ditemukan SEKALI di sini secara serial, SEBELUM `async{}` mana pun
     * dimulai. Hasil (`Map<namaFolderRule, DocumentFile?>`) dibagikan ke semua
     * coroutine paralel sebagai data BACA-SAJA setelah fungsi ini selesai --
     * secara struktural tidak mungkin lagi 2 coroutine saling balapan
     * menciptakan folder yang sama, bukan cuma "lebih jarang" kena race.
     * Beberapa rule bisa berbagi `folderName` yang sama -- `distinctBy` supaya
     * folder itu cuma di-resolve sekali, bukan sekali per rule.
     */
    private suspend fun resolveSafRuleDestinations(destinationRoot: DocumentFile, rules: List<Rule>): Map<String, DocumentFile?> {
        val vaultRootDoc = findOrCreateChildDirSaf(destinationRoot, "PromptVault")
        if (vaultRootDoc == null) {
            activityLogRepository.add(LogLevel.ERROR, "Gagal membuat/membuka folder \"PromptVault\" di folder tujuan kustom.")
            return rules.associate { it.folderName to null }
        }
        val resolved = mutableMapOf<String, DocumentFile?>()
        for (rule in rules.distinctBy { it.folderName }) {
            val dir = findOrCreateChildDirSaf(vaultRootDoc, rule.folderName)
            if (dir == null) {
                activityLogRepository.add(LogLevel.ERROR, "Gagal membuat/membuka folder tujuan \"${rule.folderName}\" di folder kustom.")
            }
            resolved[rule.folderName] = dir
        }
        return resolved
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
     * [SAF v2] Analog [moveFile], tapi TUJUAN folder kustom SAF -- SUMBER
     * TETAP java.io.File lokal (Downloads), BUKAN DocumentFile. [Restrukturisasi
     * 2026-08-13, SAF_FINAL_VERDICT_FIX.txt] Menggantikan `moveFileSaf()` +
     * `processCandidateSaf()` lama (yang dulu menerima `doc: DocumentFile`
     * sebagai SUMBER, sisa dari arsitektur "SAF sebagai scanner" yang salah).
     * Copy byte lewat ContentResolver langsung dari `FileInputStream` lokal ke
     * `OutputStream` DocumentFile tujuan -- `copyDocumentBytes()` (DocumentFile
     * -> DocumentFile) TIDAK dipakai di sini karena sumbernya bukan DocumentFile
     * sama sekali. Verifikasi nama aktual pasca-`createFile()` tetap
     * dipertahankan -- pelajaran langsung Bug #2 (v2.10.0): provider TIDAK
     * SELALU memakai nama persis yang diminta.
     *
     * [SAF, race-fix 2026-08-13] `destDir` SEKARANG parameter yang SUDAH
     * di-resolve (folder `<tujuan kustom>/PromptVault/<rule.folderName>/`),
     * BUKAN lagi `destinationRoot` mentah yang di-resolve ULANG per-file di
     * sini -- lihat [resolveSafRuleDestinations] untuk root cause & fix
     * lengkap kelas bug "folder PromptVault terduplikat (1)/(2)/(3)".
     */
    private suspend fun moveFileToSafDestination(
        file: File,
        rule: Rule,
        conflictStrategy: ConflictStrategy,
        destDir: DocumentFile
    ): MoveOutcome {
        return try {
            var targetName = file.name
            val existingAtTarget = destDir.findFile(targetName)
            if (existingAtTarget != null) {
                when (conflictStrategy) {
                    ConflictStrategy.SKIP -> return MoveOutcome.SKIPPED_CONFLICT
                    ConflictStrategy.OVERWRITE -> {
                        // [SAF debug/polish 2026-08-13] `delete()` TIDAK diverifikasi
                        // sebelumnya -- kalau provider SAF diam-diam gagal hapus (lebih
                        // umum di jalur SAF drpd java.io.File biasa, konsisten dengan
                        // seluruh riwayat provider SAF tidak reliable di project ini),
                        // `createFile()` di bawah tetap jalan dengan nama yang SAMA ->
                        // provider sering auto-suffix jadi nama baru ("target (1).ext")
                        // alih-alih benar-benar menimpa -- user pikir sudah overwrite,
                        // padahal file lama MASIH ADA + file baru bernama beda. Sekarang
                        // gagal eksplisit + log, TIDAK lanjut dengan asumsi overwrite berhasil.
                        if (!existingAtTarget.delete()) {
                            activityLogRepository.add(
                                LogLevel.ERROR,
                                "Gagal menimpa \"$targetName\" di folder tujuan kustom (hapus file lama gagal, provider menolak)."
                            )
                            return MoveOutcome.FAILED
                        }
                    }
                    ConflictStrategy.RENAME -> {
                        val base = file.nameWithoutExtension
                        val ext = file.extension
                        var counter = 1
                        while (destDir.findFile(targetName) != null) {
                            targetName = if (ext.isNotEmpty()) "${base}_$counter.$ext" else "${base}_$counter"
                            counter++
                        }
                    }
                }
            }

            val createdDoc = destDir.createFile(mimeTypeForFileName(file.name), targetName)
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

            val copyOk = try {
                file.inputStream().use { input ->
                    val output = context.contentResolver.openOutputStream(createdDoc.uri) ?: return@use false
                    output.use { streamOut -> input.copyTo(streamOut, bufferSize = 8 * 1024) }
                    true
                }
            } catch (e: Exception) {
                false
            }

            if (!copyOk) {
                runCatching { createdDoc.delete() } // bersihkan file tujuan setengah-jadi
                activityLogRepository.add(LogLevel.ERROR, "Gagal menyalin isi \"${file.name}\" ke folder tujuan kustom.")
                return MoveOutcome.FAILED
            }

            val originalParent = file.parentFile?.absolutePath ?: downloadsDir.absolutePath
            val deleteOk = file.delete()
            if (!deleteOk) {
                // [rule #15 spesifikasi] Salinan ke tujuan SUDAH sukses & lengkap
                // -- state TIDAK boleh dilaporkan sebagai gagal total (menyesatkan:
                // file sebenarnya AMAN di tujuan). Dicatat WARNING (COPIED_SOURCE_
                // REMAINING secara efektif -- file asli di Downloads masih ada,
                // potensi duplikat), tapi tetap lanjut sebagai MOVED supaya
                // MoveHistory konsisten dengan apa yang benar-benar ada di tujuan.
                activityLogRepository.add(LogLevel.WARNING, "\"${file.name}\" tersalin ke folder tujuan kustom, tapi berkas asli di Downloads gagal dihapus.")
            }

            moveHistoryRepository.record(
                MoveHistoryEntry(
                    id = UUID.randomUUID().toString(),
                    timestampMillis = System.currentTimeMillis(),
                    fileName = actualName,
                    originalParentUri = originalParent,
                    destUri = createdDoc.uri.toString(),
                    ruleFolderName = rule.folderName
                )
            )
            activityLogRepository.add(LogLevel.SUCCESS, "\"${file.name}\" -> folder tujuan kustom/PromptVault/${rule.folderName}/")
            MoveOutcome.MOVED
        } catch (e: Exception) {
            activityLogRepository.add(LogLevel.ERROR, "Error memindahkan \"${file.name}\" (folder tujuan kustom): ${e.message}")
            MoveOutcome.FAILED
        }
    }

    /**
     * [SAF, LEGACY -- lihat [undoSafDestination] untuk entri format BARU]
     * Analog [undo] untuk entri lama (v2.17.0-v2.18.1) yang dipindahkan lewat
     * arsitektur "SAF sebagai scanner": SUMBER *dan* TUJUAN sama-sama URI
     * `content://` (folder kustom dipindai+ditulis sendiri). [Restrukturisasi
     * 2026-08-13] TETAP DIPERTAHANKAN UTUH (logika TIDAK diubah) supaya
     * riwayat pemindahan yang SUDAH terlanjur tercatat di Room sebelum update
     * ini tetap bisa di-undo -- lihat dispatcher di [undo] yang membedakan
     * lewat `originalParentUri`: kalau juga `content://`, ini LEGACY (fungsi
     * ini); kalau path lokal biasa, itu format BARU ([undoSafDestination]).
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
     * [SAF v2, format BARU] Analog [undo] utk entri yang dibuat SETELAH
     * restrukturisasi 2026-08-13: SUMBER ASLI selalu lokal (Downloads,
     * java.io.File) -- SAF cuma jadi TUJUAN. Kebalikan persis dari
     * [moveFileToSafDestination]: baca isi dari DocumentFile tujuan
     * (`destUri`), tulis balik ke path lokal asal (`originalParentUri`, BUKAN
     * `content://`), lalu hapus DocumentFile tujuan.
     */
    private suspend fun undoSafDestination(entry: MoveHistoryEntry): Boolean {
        return try {
            val current = DocumentFile.fromSingleUri(context, Uri.parse(entry.destUri))
            if (current == null || !current.exists()) {
                activityLogRepository.add(LogLevel.ERROR, "Undo gagal: \"${entry.fileName}\" sudah tidak ada di folder tujuan kustom.")
                return false
            }

            val originalDir = File(entry.originalParentUri)
            if (!originalDir.exists()) originalDir.mkdirs()

            var restoreTarget = File(originalDir, entry.fileName)
            var counter = 1
            while (restoreTarget.exists()) {
                val base = entry.fileName.substringBeforeLast('.', entry.fileName)
                val ext = entry.fileName.substringAfterLast('.', "")
                restoreTarget = File(originalDir, if (ext.isNotEmpty()) "${base}_restored_$counter.$ext" else "${base}_restored_$counter")
                counter++
            }

            val copyOk = try {
                context.contentResolver.openInputStream(current.uri)?.use { input ->
                    restoreTarget.outputStream().use { output -> input.copyTo(output, bufferSize = 8 * 1024) }
                    true
                } ?: false
            } catch (e: Exception) {
                false
            }

            if (!copyOk) {
                runCatching { restoreTarget.delete() }
                activityLogRepository.add(LogLevel.ERROR, "Undo gagal: tidak bisa menyalin isi \"${entry.fileName}\" kembali dari folder tujuan kustom.")
                return false
            }

            val deleteOk = current.delete()
            moveHistoryRepository.markUndone(entry.id)
            try {
                MediaScannerConnection.scanFile(context, arrayOf(restoreTarget.absolutePath), null, null)
            } catch (_: Exception) { /* non-fatal, sama seperti di moveFile() */ }
            activityLogRepository.add(LogLevel.SUCCESS, "Undo berhasil: \"${entry.fileName}\" dikembalikan ke Downloads.")
            if (!deleteOk) {
                activityLogRepository.add(LogLevel.WARNING, "Undo \"${entry.fileName}\": salinan balik sukses, tapi file di folder tujuan kustom gagal dihapus otomatis.")
            }
            true
        } catch (e: Exception) {
            activityLogRepository.add(LogLevel.ERROR, "Error saat undo \"${entry.fileName}\" (folder tujuan kustom): ${e.message}")
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
            val selectionArgs = arrayOf("${vaultRootDir.absolutePath}${File.separator}%")
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

    /** Hasil pemrosesan satu file kandidat, dikumpulkan lewat awaitAll() lalu digabung sekuensial di [scanAndSortToDestination]. */
    private sealed class CandidateOutcome(val overlapWarning: String?) {
        class Moved(overlapWarning: String?) : CandidateOutcome(overlapWarning)
        class Skipped(val info: SkippedFileInfo, overlapWarning: String? = null) : CandidateOutcome(overlapWarning)
    }

    /**
     * Cek rule match (murah) SEBELUM stability check (mahal: delay + buka file
     * handle) -- lihat penjelasan §2 di [scanAndSortToDestination]. Dipanggil
     * paralel lewat Semaphore, aman karena tidak menyentuh state yang dibagi
     * lintas pemanggilan (moveFile/activityLogRepository/moveHistoryRepository
     * sudah masing-masing aman dipanggil concurrent).
     *
     * [SAF v2, restrukturisasi 2026-08-13] `destinationRoot` BARU -- SATU-
     * SATUNYA titik cabang tersisa antara tujuan lokal vs tujuan folder
     * kustom SAF (`file`, sumbernya, SELALU java.io.File dari Downloads,
     * tidak pernah lagi DocumentFile -- lihat catatan arsitektur di
     * [scanAndSort]).
     *
     * [SAF, race-fix 2026-08-13] `safRuleDestinations` BARU -- folder tujuan
     * SAF per-rule yang SUDAH di-resolve SEKALI, SERIAL, sebelum pemrosesan
     * paralel ini dimulai (lihat [resolveSafRuleDestinations]). Fungsi ini
     * TIDAK LAGI memanggil resolusi folder apa pun sendiri -- cuma baca dari
     * Map yang sudah jadi, aman dipanggil concurrent tanpa race.
     */
    private suspend fun processCandidate(
        file: File,
        rules: List<Rule>,
        conflictStrategy: ConflictStrategy,
        destinationRoot: DocumentFile?,
        safRuleDestinations: Map<String, DocumentFile?>
    ): CandidateOutcome {
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
        val outcome = if (destinationRoot != null) {
            val ruleDestDir = safRuleDestinations[rule.folderName]
            if (ruleDestDir == null) {
                // Resolusi folder GAGAL saat pre-resolve di awal scan (lihat
                // resolveSafRuleDestinations) -- sudah dilog SEKALI di sana,
                // di sini cukup skip file ini tanpa log error duplikat.
                return CandidateOutcome.Skipped(
                    SkippedFileInfo(file.name, "Folder tujuan \"${rule.folderName}\" di folder kustom gagal dibuat/dibuka (lihat Log)."),
                    overlapWarning
                )
            }
            moveFileToSafDestination(file, rule, conflictStrategy, ruleDestDir)
        } else {
            moveFile(file, rule, conflictStrategy)
        }
        val destLabel = if (destinationRoot != null) "folder tujuan kustom/PromptVault" else "PromptVault"
        return when (outcome) {
            MoveOutcome.MOVED -> CandidateOutcome.Moved(overlapWarning)
            MoveOutcome.SKIPPED_CONFLICT -> CandidateOutcome.Skipped(
                SkippedFileInfo(file.name, "Sudah ada file dengan nama sama di $destLabel/${rule.folderName}/ (strategi konflik: Lewati)"),
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
     * isi Downloads AKTIF SAAT INI. Dipakai di layar Tambah/Edit Rule supaya
     * user lihat langsung dampak pattern-nya SEBELUM menyimpan rule. Mendukung
     * multi-pattern CSV.
     *
     * [SAF v2, restrukturisasi 2026-08-13, SAF_FINAL_VERDICT_FIX.txt] SEBELUMNYA
     * fungsi ini bercabang ke [resolveSafRoot] (folder kustom SEBAGAI SUMBER),
     * sisa dari fix bug "preview vs scan lihat folder beda" (2026-08-13) yang
     * dulu menyamakan preview dengan scan asli -- KEDUANYA menuju root cause
     * yang sama: SAF salah ditafsirkan sebagai sumber scan. Sekarang [scanAndSort]
     * SELALU scan Downloads (lihat [listCandidateFiles] & catatan arsitektur di
     * situ), jadi preview di sini otomatis SATU-SATUNYA sumber yang mungkin --
     * TIDAK ADA LAGI cabang SAF sama sekali. Ini bukan cuma revert ke versi
     * lama sebelum fix 2026-08-13 -- kelas bug "preview vs scan beda folder"
     * jadi STRUKTURAL TIDAK MUNGKIN terjadi lagi (bukan lagi soal "dua cabang
     * logika harus disinkronkan", karena sekarang cuma ada SATU cabang).
     */
    suspend fun previewPatternMatches(pattern: String, excludePattern: String = ""): PatternPreviewResult =
        withContext(Dispatchers.IO) {
            if (pattern.isBlank()) return@withContext PatternPreviewResult(0, emptyList())
            if (!downloadsDir.exists() || !downloadsDir.canRead()) {
                return@withContext PatternPreviewResult(0, emptyList())
            }
            val names = listCandidateFiles().map { it.name }
            buildPreviewResult(names, pattern, excludePattern)
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
     * ActivityLogScreen).
     *
     * [SAF v2, restrukturisasi 2026-08-13] SEBELUMNYA cabang ke [undoSaf]
     * cukup dicek dari `destUri` doang ("content://..." -> selalu SAF-ke-SAF).
     * Sekarang ADA DUA format riwayat yang mungkin tersimpan di Room:
     *  - LEGACY (dibuat SEBELUM restrukturisasi ini, arsitektur "SAF sebagai
     *    scanner"): `originalParentUri` MAUPUN `destUri` sama-sama
     *    "content://..." -> [undoSaf] (logika lama, TIDAK diubah, supaya
     *    riwayat lama tetap bisa di-undo).
     *  - BARU (dibuat SETELAH restrukturisasi ini): `destUri` "content://..."
     *    TAPI `originalParentUri` path lokal biasa (sumber SELALU Downloads
     *    sekarang) -> [undoSafDestination].
     *  - Bukan keduanya -> jalur lokal-ke-lokal biasa (di bawah, tidak berubah).
     * Karakteristik dispatcher fungsi ini SENGAJA tidak diubah (masih tanpa
     * `withContext` sendiri, sama seperti sebelumnya) -- lihat catatan di
     * PROJECT_STATE.md soal potensi I/O di thread pemanggil.
     */
    suspend fun undo(entry: MoveHistoryEntry): Boolean {
        if (entry.destUri.startsWith("content://")) {
            return if (entry.originalParentUri.startsWith("content://")) {
                undoSaf(entry) // legacy: sumber & tujuan dulu sama-sama SAF
            } else {
                undoSafDestination(entry) // baru: sumber lokal, tujuan SAF
            }
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
