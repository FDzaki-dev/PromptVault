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
import com.elprompter.promptvault.shizuku.IFileOpsService
import com.elprompter.promptvault.shizuku.ShizukuManager
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
    val safAccessLost: Boolean = false,
    /**
     * [Fitur baru 2026-08-17, integrasi Shizuku] `true` HANYA kalau mode
     * Shizuku aktif tapi service belum ter-bind (Shizuku belum jalan/izin
     * belum diberikan/masih proses binding). Sengaja field TERPISAH dari
     * [shizukuRootMissing] -- dua kegagalan ini butuh tindakan pemulihan
     * beda buat user (buka Shizuku Manager & beri izin vs buat folder
     * secara manual), pola yang SAMA dengan pemisahan [safAccessLost] dari
     * "NotConfigured" di [FileSorter.resolveSafDestinationRoot].
     */
    val shizukuNotReady: Boolean = false,
    /**
     * [Fitur baru 2026-08-17, integrasi Shizuku] `true` kalau mode Shizuku
     * aktif, service SIAP, TAPI folder root yang diisi user di Pengaturan
     * belum ada secara fisik di storage. App SENGAJA TIDAK PERNAH membuat
     * folder ini sendiri -- lihat KDoc [FileSorter.resolveShizukuRuleDestinations]
     * & peringatan eksplisit di SettingsScreen.
     */
    val shizukuRootMissing: Boolean = false
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
 * Pure, top-level (bukan method [FileSorter]) -- pola sama persis dengan
 * [mimeTypeForFileName] di atas, alasan sama: unit-testable tanpa Context
 * Android. [Fase 1.1 roadmap, 2026-08-18] Diekstrak MURNI dari isi lama
 * `FileSorter.isTempOrPartialName` (private instance method) -- perilaku
 * 100% identik, cuma dipindah keluar class + [TEMP_FILE_MARKERS] ikut jadi
 * top-level supaya bisa diakses tanpa instance.
 */
internal val TEMP_FILE_MARKERS = listOf(
    ".crdownload", ".tmp", ".part", ".download", ".downloading"
)

fun isTempOrPartialName(name: String): Boolean {
    val lowerName = name.lowercase()
    return TEMP_FILE_MARKERS.any { lowerName.endsWith(it) }
}

/**
 * Pure, top-level -- diekstrak MURNI dari `FileSorter.explainNoMatchByName`
 * (private instance method lama), perilaku 100% identik. [Fase 1.1 roadmap,
 * 2026-08-18]
 */
fun explainNoMatchByName(name: String, sizeKb: Long, rules: List<Rule>): String {
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
 * Pure, top-level -- diekstrak MURNI dari `FileSorter.buildPreviewResult`
 * (private instance method lama), perilaku 100% identik. [Fase 1.1 roadmap,
 * 2026-08-18]
 */
fun buildPreviewResult(candidateNames: List<String>, pattern: String, excludePattern: String): PatternPreviewResult {
    val matched = candidateNames
        .filter { GlobMatcher.matchesAny(it, pattern) }
        .filterNot { excludePattern.isNotBlank() && GlobMatcher.matchesAny(it, excludePattern) }
    return PatternPreviewResult(candidateNames.size, matched)
}

/**
 * Pure, top-level -- diekstrak MURNI dari while-loop RENAME di
 * `FileSorter.moveFile` (dulu langsung baca `File(destDir, ...).exists()`
 * di tengah fungsi suspend besar, TIDAK BISA di-unit-test terisolasi).
 * Sekarang caller (`moveFile`) yang menyediakan predikat `exists` (bisa
 * `File.exists()` asli DI PRODUKSI, atau `Set<String>` palsu di test) --
 * fungsi ini sendiri 100% tidak menyentuh filesystem. [Fase 1.1 roadmap,
 * 2026-08-18]. `substringBeforeLast('.', originalName)`/
 * `substringAfterLast('.', "")` SENGAJA dipakai (bukan `File(...).nameWithoutExtension`/
 * `.extension`) supaya fungsi ini tetap Kotlin murni tanpa `java.io.File` --
 * definisi stdlib keduanya PERSIS sama, lihat `kotlin.io.FilesKt`.
 */
fun nextAvailableFileName(originalName: String, exists: (String) -> Boolean): String {
    if (!exists(originalName)) return originalName
    val base = originalName.substringBeforeLast('.', originalName)
    val ext = originalName.substringAfterLast('.', "")
    var counter = 1
    var candidate: String
    do {
        // Sengaja SELALU "_$counter.$ext" (bahkan kalau ext kosong, hasil
        // "nama_1." dgn titik trailing) -- ini bug-for-bug parity dgn
        // perilaku produksi asli (`"${file.nameWithoutExtension}_$counter.${file.extension}"`
        // tanpa pengecualian ext kosong). TIDAK diperbaiki di batch ini --
        // itu perubahan perilaku terpisah, di luar scope "ekstrak murni".
        candidate = "${base}_$counter.$ext"
        counter++
    } while (exists(candidate))
    return candidate
}

/**
 * Logika inti: scan folder Downloads (SELALU, tidak pernah folder lain --
 * lihat catatan arsitektur di [FileSorter.scanAndSort]), cocokkan tiap file
 * terhadap rule aktif (berurutan sesuai PRIORITAS, mendukung multi-pattern &
 * filter ukuran), lalu pindahkan ke Downloads/PromptVault/<folderName>/ ATAU,
 * kalau user sudah memilih folder tujuan kustom lewat SAF, ke <folder
 * kustom>/PromptVault/<folderName>/ (root "PromptVault" DIBUAT OTOMATIS lagi
 * sejak 2026-08-17 v2, dengan lapis anti-duplikat baru -- lihat KDoc
 * [FileSorter.resolveCanonicalRootDirSaf]) -- dan catat riwayat untuk undo.
 *
 * Prinsip "expert-level file organizer": setiap file yang TIDAK dipindahkan harus
 * bisa dijelaskan alasannya secara spesifik ke user, bukan cuma angka "dilewati".
 */
class FileSorter(
    private val context: Context,
    private val ruleRepository: RuleRepository,
    private val activityLogRepository: ActivityLogRepository,
    private val moveHistoryRepository: MoveHistoryRepository,
    private val settingsRepository: SettingsRepository,
    // [Fitur baru 2026-08-17, integrasi Shizuku] Lambda, bukan instance
    // langsung -- ShizukuManager.service bisa berubah (null -> ready) SETELAH
    // FileSorter dibuat, jadi setiap pemakaian WAJIB membaca ulang lewat
    // lambda ini, bukan snapshot yang di-capture sekali di constructor.
    private val shizukuServiceProvider: () -> IFileOpsService? = { ShizukuManager.service }
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
        // [Fitur baru 2026-08-17, integrasi Shizuku] Titik cabang PALING
        // AWAL -- mode Shizuku & mode SAF SALING EKSKLUSIF by design (lihat
        // SettingsRepository.useShizukuFlow). Kalau aktif, cabang SAF di
        // bawah SAMA SEKALI tidak dieksekusi -- tidak ada campur logika 2
        // mode custom-destination dalam 1 scan.
        if (settingsRepository.getUseShizuku()) {
            return@withLock scanAndSortViaShizuku()
        }
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

    // ========================================================================
    // [Fitur baru 2026-08-17, integrasi Shizuku] Jalur tujuan kustom PRIVILEGED
    // via Shizuku -- alternatif SAF, dipilih user lewat toggle "Mode Shizuku"
    // (SettingsScreen), SALING EKSKLUSIF dengan cabang SAF (lihat percabangan
    // di scanAndSort). Path SELALU filesystem absolut polos (bukan content://
    // URI), dieksekusi proses Shizuku (FileOpsUserService, UID shell/root) --
    // bisa menulis ke lokasi yang mungkin ditolak Scoped Storage kalau
    // dipanggil langsung dari proses app ini, TANPA dialog picker SAF.
    // ========================================================================

    /**
     * Analog [scanAndSortToDestination], tujuan lewat [IFileOpsService] IPC.
     * Sumber scan TETAP SELALU Downloads ([listCandidateFiles]) -- konsisten
     * dengan pelajaran permanen project ini: SAF/Shizuku HANYA tujuan, bukan
     * sumber scan alternatif (lihat Insiden SAF_FINAL_VERDICT_FIX 2026-08-13).
     */
    private suspend fun scanAndSortViaShizuku(): ScanResult = withContext(Dispatchers.IO) {
        val service = shizukuServiceProvider()
        if (service == null) {
            activityLogRepository.add(
                LogLevel.ERROR,
                "Mode Shizuku aktif tapi service belum siap (Shizuku belum jalan / izin belum diberikan / masih proses menyambung). " +
                    "Buka Pengaturan > Mode Shizuku untuk cek status & minta izin."
            )
            return@withContext ScanResult(0, 0, foldersUnreadable = false, overlapWarnings = emptyList(), shizukuNotReady = true)
        }

        val rootPath = settingsRepository.getShizukuDestPath()
        if (rootPath.isNullOrBlank()) {
            activityLogRepository.add(LogLevel.ERROR, "Mode Shizuku aktif tapi folder tujuan belum diisi di Pengaturan.")
            return@withContext ScanResult(0, 0, foldersUnreadable = false, overlapWarnings = emptyList(), shizukuNotReady = true)
        }

        // [Requirement eksplisit user, 2026-08-17] App TIDAK PERNAH membuat
        // folder ROOT ini sendiri -- HANYA memvalidasi keberadaannya lewat
        // IPC. Kalau tidak ada, scan BERHENTI dengan pesan eksplisit -- TIDAK
        // fallback diam-diam ke Downloads/PromptVault, pola sama persis
        // dengan AccessLost SAF di resolveSafDestinationRoot.
        val rootExists = try { service.exists(rootPath) } catch (e: Exception) { false }
        if (!rootExists) {
            activityLogRepository.add(
                LogLevel.ERROR,
                "Folder tujuan Shizuku \"$rootPath\" TIDAK DITEMUKAN. Aplikasi ini TIDAK PERNAH membuat folder root " +
                    "secara otomatis -- buat folder itu sendiri lewat file manager dulu (persis path yang kamu isi di " +
                    "Pengaturan), baru scan lagi. Scan DIHENTIKAN, tidak fallback ke Downloads."
            )
            return@withContext ScanResult(0, 0, foldersUnreadable = false, overlapWarnings = emptyList(), shizukuRootMissing = true)
        }
        val rootIsDir = try { service.isDirectory(rootPath) } catch (e: Exception) { false }
        if (!rootIsDir) {
            activityLogRepository.add(LogLevel.ERROR, "Folder tujuan Shizuku \"$rootPath\" ada, tapi itu FILE, bukan folder. Perbaiki path di Pengaturan.")
            return@withContext ScanResult(0, 0, foldersUnreadable = false, overlapWarnings = emptyList(), shizukuRootMissing = true)
        }

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

        // [Pelajaran WAJIB dari race-fix SAF 2026-08-13, diterapkan proaktif
        // di jalur BARU ini -- lihat resolveSafRuleDestinations untuk root
        // cause lengkap kelas bug "folder duplikat"] Subfolder RULE
        // di-resolve SEKALI, SERIAL, DI SINI -- SEBELUM file diproses
        // paralel di bawah, karena mkdirs() via IPC TIDAK dijamin idempoten
        // lintas panggilan concurrent.
        val ruleDestPaths = resolveShizukuRuleDestinations(service, rootPath, rules)

        val semaphore = Semaphore(settingsRepository.getScanConcurrency())
        val results = candidateFiles.map { file ->
            async { semaphore.withPermit { processCandidateShizuku(file, rules, conflictStrategy, service, ruleDestPaths) } }
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

        ScanResult(moved, skipped, foldersUnreadable = false, overlapWarnings = overlapWarnings, skippedDetails = skippedDetails)
    }

    /**
     * Analog [resolveSafRuleDestinations], via IPC Shizuku. HANYA membuat
     * subfolder RULE di dalam [rootPath] -- TIDAK PERNAH memanggil `mkdirs`
     * untuk [rootPath] itu sendiri (root sudah divalidasi ADA sebelum
     * fungsi ini dipanggil, lihat [scanAndSortViaShizuku]).
     */
    private suspend fun resolveShizukuRuleDestinations(
        service: IFileOpsService,
        rootPath: String,
        rules: List<Rule>
    ): Map<String, String?> {
        val resolved = mutableMapOf<String, String?>()
        for (rule in rules.distinctBy { it.folderName }) {
            val folderNameError = validateRuleFolderName(rule.folderName)
            if (folderNameError != null) {
                activityLogRepository.add(
                    LogLevel.ERROR,
                    "Rule \"${rule.folderName}\" punya nama folder tidak valid ($folderNameError) -- folder tujuan Shizuku untuk rule ini dilewati."
                )
                resolved[rule.folderName] = null
                continue
            }
            val childPath = "$rootPath${File.separator}${rule.folderName}"
            val ok = try {
                (service.exists(childPath) && service.isDirectory(childPath)) || service.mkdirs(childPath)
            } catch (e: Exception) {
                false
            }
            if (!ok) {
                activityLogRepository.add(LogLevel.ERROR, "Gagal membuat/membuka folder tujuan \"${rule.folderName}\" di folder Shizuku.")
                resolved[rule.folderName] = null
            } else {
                resolved[rule.folderName] = childPath
            }
        }
        return resolved
    }

    /** Analog [processCandidate], jalur Shizuku. */
    private suspend fun processCandidateShizuku(
        file: File,
        rules: List<Rule>,
        conflictStrategy: ConflictStrategy,
        service: IFileOpsService,
        ruleDestPaths: Map<String, String?>
    ): CandidateOutcome {
        val sizeKb = file.sizeKb()
        val matches = RuleOverlapChecker.matchingRules(file.name, sizeKb, rules)
        if (matches.isEmpty()) {
            return CandidateOutcome.Skipped(SkippedFileInfo(file.name, explainNoMatch(file, sizeKb, rules)))
        }
        if (isLikelyStillWriting(file)) {
            return CandidateOutcome.Skipped(
                SkippedFileInfo(file.name, "Ditunda: file baru saja berubah, kemungkinan masih ditulis/didownload. Akan dicoba lagi scan berikutnya.")
            )
        }
        var overlapWarning: String? = null
        if (matches.size > 1) {
            overlapWarning = "\"${file.name}\" cocok dengan ${matches.size} rule (${matches.joinToString { it.folderName }}). " +
                "Dipindahkan memakai rule prioritas tertinggi: \"${matches.first().folderName}\"."
            activityLogRepository.add(LogLevel.WARNING, overlapWarning)
        }
        val rule = matches.first()
        val destDirPath = ruleDestPaths[rule.folderName]
        if (destDirPath == null) {
            return CandidateOutcome.Skipped(
                SkippedFileInfo(file.name, "Folder tujuan \"${rule.folderName}\" di folder Shizuku gagal dibuat/dibuka (lihat Log)."),
                overlapWarning
            )
        }
        val outcome = moveFileViaShizuku(file, rule, conflictStrategy, service, destDirPath)
        return when (outcome) {
            MoveOutcome.MOVED -> CandidateOutcome.Moved(overlapWarning)
            MoveOutcome.SKIPPED_CONFLICT -> CandidateOutcome.Skipped(
                SkippedFileInfo(file.name, "Sudah ada file dengan nama sama di folder Shizuku/${rule.folderName}/ (strategi konflik: Lewati)"),
                overlapWarning
            )
            MoveOutcome.FAILED -> CandidateOutcome.Skipped(
                SkippedFileInfo(file.name, "Gagal dipindahkan (lihat Log untuk detail error)"),
                overlapWarning
            )
        }
    }

    /**
     * Analog [moveFileToSafDestination], via IPC Shizuku. `destUri` yang
     * disimpan ke [MoveHistoryEntry] diberi prefix [SHIZUKU_URI_PREFIX] --
     * BUKAN URI asli, cuma penanda supaya [undo] tahu harus lewat
     * [undoShizuku], pola identik dengan prefix `content://` untuk entri SAF.
     */
    private suspend fun moveFileViaShizuku(
        file: File,
        rule: Rule,
        conflictStrategy: ConflictStrategy,
        service: IFileOpsService,
        destDirPath: String
    ): MoveOutcome {
        return try {
            var targetName = file.name
            var targetPath = "$destDirPath${File.separator}$targetName"
            val existsAtTarget = try { service.exists(targetPath) } catch (e: Exception) { false }
            if (existsAtTarget) {
                when (conflictStrategy) {
                    ConflictStrategy.SKIP -> return MoveOutcome.SKIPPED_CONFLICT
                    ConflictStrategy.OVERWRITE -> {
                        val deleted = try { service.deleteFile(targetPath) } catch (e: Exception) { false }
                        if (!deleted) {
                            activityLogRepository.add(LogLevel.ERROR, "Gagal menimpa \"$targetName\" di folder Shizuku (hapus file lama gagal).")
                            return MoveOutcome.FAILED
                        }
                    }
                    ConflictStrategy.RENAME -> {
                        val base = file.nameWithoutExtension
                        val ext = file.extension
                        var counter = 1
                        while (try { service.exists(targetPath) } catch (e: Exception) { true }) {
                            targetName = if (ext.isNotEmpty()) "${base}_$counter.$ext" else "${base}_$counter"
                            targetPath = "$destDirPath${File.separator}$targetName"
                            counter++
                        }
                    }
                }
            }

            val originalParent = file.parentFile?.absolutePath ?: downloadsDir.absolutePath
            val moved = try { service.moveFile(file.absolutePath, targetPath) } catch (e: Exception) { false }
            if (!moved) {
                activityLogRepository.add(LogLevel.ERROR, "Gagal memindahkan \"${file.name}\" ke folder Shizuku (IPC menolak/error).")
                return MoveOutcome.FAILED
            }

            moveHistoryRepository.record(
                MoveHistoryEntry(
                    id = UUID.randomUUID().toString(),
                    timestampMillis = System.currentTimeMillis(),
                    fileName = targetName,
                    originalParentUri = originalParent,
                    destUri = "$SHIZUKU_URI_PREFIX$targetPath",
                    ruleFolderName = rule.folderName
                )
            )
            activityLogRepository.add(LogLevel.SUCCESS, "\"${file.name}\" -> folder Shizuku/${rule.folderName}/")
            MoveOutcome.MOVED
        } catch (e: Exception) {
            activityLogRepository.add(LogLevel.ERROR, "Error memindahkan \"${file.name}\" (folder Shizuku): ${e.message}")
            MoveOutcome.FAILED
        }
    }

    /**
     * Analog [undoSafDestination], via IPC Shizuku -- lihat dispatcher di
     * [undo]. [IFileOpsService.moveFile] SUDAH atomik (rename-atau-copy+delete
     * digabung di 1 panggilan IPC, lihat FileOpsUserService.kt), jadi BEDA
     * dari [undoSaf]/[undoSafDestination]: tidak ada state "Undo SEBAGIAN"
     * terpisah di sini -- kalau IPC bilang gagal, TIDAK ADA perubahan (file
     * masih di lokasi awal), aman dicoba lagi tanpa risiko duplikat.
     */
    private suspend fun undoShizuku(entry: MoveHistoryEntry): Boolean {
        val service = shizukuServiceProvider()
        if (service == null) {
            activityLogRepository.add(LogLevel.ERROR, "Undo gagal: service Shizuku belum siap. Buka Pengaturan > Mode Shizuku, pastikan status Siap, lalu coba lagi.")
            return false
        }
        return try {
            val currentPath = entry.destUri.removePrefix(SHIZUKU_URI_PREFIX)
            val currentExists = try { service.exists(currentPath) } catch (e: Exception) { false }
            if (!currentExists) {
                activityLogRepository.add(LogLevel.ERROR, "Undo gagal: \"${entry.fileName}\" sudah tidak ada di folder Shizuku.")
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

            val moved = try { service.moveFile(currentPath, restoreTarget.absolutePath) } catch (e: Exception) { false }
            if (!moved) {
                activityLogRepository.add(LogLevel.ERROR, "Undo gagal: tidak bisa memindahkan \"${entry.fileName}\" kembali dari folder Shizuku.")
                return false
            }
            try {
                MediaScannerConnection.scanFile(context, arrayOf(restoreTarget.absolutePath), null, null)
            } catch (_: Exception) { /* non-fatal, sama seperti di moveFile() */ }

            moveHistoryRepository.markUndone(entry.id)
            activityLogRepository.add(LogLevel.SUCCESS, "Undo berhasil: \"${entry.fileName}\" dikembalikan ke Downloads.")
            true
        } catch (e: Exception) {
            activityLogRepository.add(LogLevel.ERROR, "Error saat undo \"${entry.fileName}\" (folder Shizuku): ${e.message}")
            false
        }
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
     * 3. **Diproses paralel dengan batas konkurensi (default 6, configurable
     *    lewat Pengaturan sejak 2026-08-13 -- lihat [SettingsRepository.getScanConcurrency])**:
     *    dulunya `for (file in candidateFiles)` sekuensial -- 300 file yang semuanya
     *    lolos ke stability check berarti ~300 detik (delay 1 detik/file
     *    berturutan). Sekarang tiap kandidat diproses lewat `async` dengan
     *    [Semaphore] agar wall-time mendekati (jumlah file / konkurensi)
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

        // [SAF, race-fix 2026-08-13, root dikembalikan 2026-08-17 v2 -- lihat
        // dokumentasi lengkap di resolveSafRuleDestinations()/
        // resolveCanonicalRootDirSaf()] Folder tujuan SAF (root "PromptVault" +
        // subfolder tiap rule) di-resolve SEKALI DI SINI, SERIAL, SEBELUM file
        // diproses paralel di bawah -- BUKAN lagi per-file di dalam
        // moveFileToSafDestination() seperti sebelumnya (sumber duplikat folder).
        val safRuleDestinations: Map<String, DocumentFile?> =
            if (destinationRoot != null) resolveSafRuleDestinations(destinationRoot, rules) else emptyMap()

        // [Technical debt #4, dieksekusi 2026-08-13] Dulu Semaphore(SCAN_CONCURRENCY)
        // dengan konstanta hardcode 6 (v2.4.0). Sekarang dibaca dari SettingsRepository
        // (getScanConcurrency() -- default TETAP 6, lihat dokumentasi lengkap di
        // SettingsRepository.DEFAULT_SCAN_CONCURRENCY): perilaku default TIDAK
        // BERUBAH, user yang mau bisa menaikkan/menurunkan sendiri dari Pengaturan.
        val semaphore = Semaphore(settingsRepository.getScanConcurrency())
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

    /**
     * [SAF] Cari subfolder bernama [name] di [parent]; buat baru kalau belum ada.
     * [cacheKey] = path relatif thd `safTreeUri` (mis. "PromptVault" atau
     * "PromptVault/NamaRule"), dipakai [SettingsRepository] cache -- lihat KDoc
     * lengkap root-cause "duplikat folder berulang" di sana. `null` = gagal
     * (jangan paksa lanjut).
     *
     * [Instrumentasi 2026-08-16 -- user lapor duplikat MASIH terjadi meski cache
     * Uri (fix sesi sebelumnya) sudah aktif] Cache-by-Uri TERBUKTI BELUM CUKUP
     * sendirian -- kemungkinan besar karena skenario yang mendasarinya (listing
     * SAF stale) juga bisa membuat query `exists()`/`isDirectory` langsung on
     * cache HIT jadi false-negative sesaat setelah `createDirectory()`, bukan
     * cuma `findFile()`. Tanpa akses device/Logcat asli, TIDAK bisa dipastikan
     * ini vs kemungkinan lain -- jadi sesi ini fokus 2 hal: (1) retry pendek
     * dengan delay sebelum menyerah & membuat folder baru (mitigasi kalau
     * memang staleness sesaat), (2) LOG EKSPLISIT ke Activity Log tiap kali
     * folder BARU dibuat DAN tiap kali nama hasil `createDirectory()` TIDAK
     * PERSIS sama dengan yang diminta (bukti definitif provider ikut
     * auto-suffix, bukan asumsi) -- supaya lain kali kejadian, user bisa
     * buka layar Log Aktivitas & kirim baris relevan, GANTI screenshot folder
     * yang cuma nunjukkin gejala akhir, bukan penyebabnya.
     */
    private suspend fun findOrCreateChildDirSaf(parent: DocumentFile, name: String, cacheKey: String): DocumentFile? {
        // Langkah 1: coba URI hasil cache dulu -- resolusi 1 dokumen spesifik by
        // Uri, BUKAN query listing by-nama yang rentan stale (root cause fix ini).
        //
        // [FIX crash_20260817_174626, UnsupportedOperationException] SEBELUMNYA
        // pakai `DocumentFile.fromSingleUri()` di sini -- itu SELALU mengembalikan
        // SingleDocumentFile, yang `listFiles()`-nya UNCONDITIONALLY throw
        // UnsupportedOperationException (bukan cuma kalau URI-nya benar-benar
        // bukan folder -- hardcoded di androidx untuk SEMUA instance). `cached`
        // hasil cache di sini lalu dipakai lagi sebagai `parent` di pemanggilan
        // [findOrCreateChildDirSaf] berikutnya (mis. subfolder rule di bawah root
        // vault), yang manggil `parent.findFile(name)` -> internal `listFiles()`
        // -> crash persis di titik ini. `DocumentFile.fromTreeUri()` yang benar:
        // URI cache di sini SELALU berasal dari `.uri` milik TreeDocumentFile
        // (child hasil `findFile()`/`createDirectory()` dari root tree yang sama),
        // jadi sudah mengandung segmen `/tree/`. `fromTreeUri()` mengekstrak
        // document-id dari situ & membangun TreeDocumentFile yang benar --
        // listFiles()/findFile()/createDirectory() TETAP berfungsi normal.
        settingsRepository.getCachedFolderUri(cacheKey)?.let { cachedUriStr ->
            try {
                val cached = DocumentFile.fromTreeUri(context, Uri.parse(cachedUriStr))
                if (cached != null && cached.isDirectory && cached.exists()) return cached
            } catch (e: Exception) {
                // URI cache basi/tidak valid (mis. folder dihapus manual) -- lanjut jalur normal di bawah, JANGAN gagal di sini.
            }
        }
        // Langkah 2: fallback -- query listing by-nama, dengan retry+delay
        // BERTAHAP (200ms lalu 500ms, sebelumnya cuma 1x200ms) kalau hasil
        // pertama null -- mitigasi listing stale sesaat pasca-create scan
        // sebelumnya (lihat KDoc di atas). Diperkuat 2026-08-17 saat root
        // "PromptVault" dikembalikan (lihat [resolveCanonicalRootDirSaf]):
        // window race paling rawan justru saat root BELUM PERNAH ada sama
        // sekali (cache masih kosong), jadi retry lebih sabar di titik ini.
        var existing = parent.findFile(name)
        if (existing == null) {
            delay(200)
            existing = parent.findFile(name)
        }
        if (existing == null) {
            delay(500)
            existing = parent.findFile(name)
        }
        if (existing != null) {
            if (!existing.isDirectory) return null // nama dipakai FILE, bukan folder -- konflik, jangan dipaksa
            settingsRepository.setCachedFolderUri(cacheKey, existing.uri.toString())
            return existing
        }
        return try {
            val created = parent.createDirectory(name) ?: return null
            if (created.name != name) {
                // BUKTI KONKRET provider auto-suffix nama (mis. "PromptVault (1)")
                // -- ini SUMBER duplikat, dicatat APA ADANYA supaya user bisa lihat
                // di Log Aktivitas persis kapan/nama apa yang dihasilkan.
                activityLogRepository.add(
                    LogLevel.ERROR,
                    "Provider SAF mengubah nama folder \"$name\" jadi \"${created.name}\" saat dibuat " +
                        "(cacheKey=$cacheKey) -- ini penyebab folder duplikat \"(N)\". Sudah dicatat, folder ini tetap dipakai."
                )
            } else {
                activityLogRepository.add(LogLevel.INFO, "Folder SAF baru dibuat: \"$name\" (cacheKey=$cacheKey).")
            }
            settingsRepository.setCachedFolderUri(cacheKey, created.uri.toString())
            created
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
     * terpisah per-file, DI DALAM tiap coroutine paralel (s/d 6 file
     * bersamaan secara default -- kini configurable, lihat Keputusan
     * Arsitektur #6 di PROJECT_STATE.md). `DocumentFile.createDirectory()` TIDAK atomik/tidak idempoten seperti
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
     *
     * [Update 2026-08-16, duplikat MASIH berulang meski fix di atas tetap
     * berlaku] Fix serial di atas menutup race ANTAR-coroutine dalam 1 scan,
     * tapi TIDAK menutup staleness listing SAF ANTAR-scan (mis. AutoSortWorker
     * periodik, tiap scan tetap serial berkat [scanMutex] -- BUKAN race baru).
     * Lapis fix tambahan ada di [findOrCreateChildDirSaf]/[SettingsRepository]
     * (cache Uri per folder, resolusi langsung by-Uri bukan listing by-nama).
     *
     * [Keputusan Arsitektur 2026-08-17 -- PERMINTAAN LANGSUNG USER, setelah 2
     * ronde mitigasi (v7.1.5 cache-Uri, v7.1.6 retry+instrumentasi) TIDAK
     * berhasil membuktikan/menyingkirkan bug ini tuntas] App **BERHENTI
     * membuat folder root "PromptVault" sendiri di dalam folder tujuan
     * kustom**. User membuat folder root itu MANUAL lewat file manager,
     * lalu PILIH folder itu SENDIRI lewat SAF picker sbg "Folder Tujuan
     * Kustom" -- `destinationRoot` (hasil `fromTreeUri`) SEKARANG LANGSUNG
     * dipakai sbg root vault, TANPA lapisan `findOrCreateChildDirSaf(...,
     * "PromptVault", ...)` lagi. App HANYA membuat subfolder RULE (mis.
     * "Apps vault", "Markdown vault") langsung di dalamnya -- jalur ini
     * TERBUKTI 0 masalah di seluruh log yang direview sesi ini. Ini
     * menghilangkan SATU-SATUNYA titik panggilan `createDirectory("PromptVault")`
     * di seluruh codebase (grep dikonfirmasi cuma 1 titik) -- bukan cuma
     * menambal race/staleness-nya lagi, tapi menghapus PEMICUNYA sepenuhnya.
     * Konsekuensi: user yang sebelumnya sudah pakai folder tujuan kustom
     * dengan subfolder "PromptVault" di dalamnya akan lihat scan BARU
     * menulis LANGSUNG ke root (tanpa subfolder "PromptVault" lagi) --
     * kalau mau lanjutin struktur lama, tinggal arahkan SAF picker ke folder
     * "PromptVault" yang sudah ada itu sendiri (bukan parent-nya).
     *
     * [Keputusan Arsitektur 2026-08-17 v2 -- DIBALIK lagi, PERMINTAAN LANGSUNG
     * USER] Keputusan di atas (berhenti total bikin root) DIBATALKAN atas
     * permintaan eksplisit user: fitur auto-buat root dikembalikan, TAPI kali
     * ini lewat [resolveCanonicalRootDirSaf] yang menambah lapis deteksi+
     * konvergensi duplikat yang BELUM ADA di percobaan v7.1.5/v7.1.6 dulu
     * (lihat KDoc lengkap di fungsi itu). Baris `findOrCreateChildDirSaf(...,
     * "PromptVault", ...)` di bawah AKTIF LAGI, tapi dipanggil TIDAK LANGSUNG --
     * lewat `resolveCanonicalRootDirSaf` di [resolveSafRuleDestinations].
     */
    /**
     * [Keputusan Arsitektur 2026-08-17 v2 -- PERMINTAAN LANGSUNG USER: kembalikan
     * fitur auto-buat folder root "PromptVault", TAPI jangan sampai duplikat
     * "(N)" terulang] Root cause asli (KDoc [resolveSafRuleDestinations] di
     * bawah) sudah ditutup untuk race ANTAR-coroutine (resolusi serial) DAN
     * dikuatkan lagi di [findOrCreateChildDirSaf] (cache-by-Uri + retry
     * bertahap). Yang BELUM pernah dicoba sebelumnya: lapis SELF-HEALING ini --
     * bukan cuma mencegah duplikat baru, tapi mendeteksi & mengonvergensikan
     * kalau provider SAF (di luar kendali app, mis. staleness cache FUSE/
     * indexing OEM tertentu) tetap menghasilkan >1 folder cocok pola
     * "PromptVault"/"PromptVault (N)":
     * 1. Cache-by-Uri dicoba dulu (jalur cepat, sama seperti biasa).
     * 2. Kalau cache kosong/basi: LIST children [parent] sekali, cari SEMUA
     *    folder yang cocok regex `^PromptVault(\s\(\d+\))?$`.
     * 3. 0 hasil -> lanjut jalur normal [findOrCreateChildDirSaf] (buat baru).
     * 4. 1 hasil -> itu kanonik, cache & pakai.
     * 5. >1 hasil (provider SUDAH terlanjur duplikat di luar kendali app) ->
     *    JANGAN pilih random/pertama. Prioritas: (a) nama PERSIS "PromptVault"
     *    tanpa akhiran kalau ada, (b) kalau tidak ada, folder ber-`lastModified()`
     *    PALING AWAL (asumsi: yang pertama dibuat = yang paling mungkin sudah
     *    berisi riwayat file lama). Dicatat WARNING eksplisit ke Activity Log
     *    (nama semua folder yang ditemukan) supaya user tahu ada folder liar
     *    yang perlu digabung manual -- app TIDAK menghapus/memindah isi folder
     *    lain secara otomatis (aksi destruktif tanpa izin eksplisit, di luar
     *    scope batch ini). Sejak titik ini, SEMUA scan berikutnya konsisten
     *    memakai kanonik yang sama (di-cache), jadi folder tidak makin
     *    terpecah walau providernya sempat "nakal" sekali.
     */
    private suspend fun resolveCanonicalRootDirSaf(parent: DocumentFile): DocumentFile? {
        // [FIX crash_20260817_174626, sama persis dengan fix di findOrCreateChildDirSaf
        // -- lihat KDoc lengkap di sana] `fromSingleUri()` -> SingleDocumentFile ->
        // `listFiles()` unconditionally throw. Root vault yang diresolusi lewat cache
        // di sini dipakai sbg `parent` untuk `findOrCreateChildDirSaf` subfolder rule
        // berikutnya -> crash. `fromTreeUri()` mengembalikan TreeDocumentFile yang benar.
        settingsRepository.getCachedFolderUri(SAF_ROOT_CACHE_KEY)?.let { cachedUriStr ->
            try {
                val cached = DocumentFile.fromTreeUri(context, Uri.parse(cachedUriStr))
                if (cached != null && cached.isDirectory && cached.exists()) return cached
            } catch (e: Exception) {
                // Cache basi -- lanjut deteksi di bawah.
            }
        }

        val candidates = try {
            parent.listFiles().filter { it.isDirectory && it.name != null && SAF_ROOT_DUPLICATE_REGEX.matches(it.name!!) }
        } catch (e: Exception) {
            emptyList()
        }

        return when {
            candidates.size == 1 -> {
                val only = candidates.first()
                settingsRepository.setCachedFolderUri(SAF_ROOT_CACHE_KEY, only.uri.toString())
                only
            }
            candidates.size > 1 -> {
                val canonical = candidates.firstOrNull { it.name == SAF_ROOT_FOLDER_NAME }
                    ?: candidates.minByOrNull { it.lastModified() }
                    ?: candidates.first()
                activityLogRepository.add(
                    LogLevel.ERROR,
                    "Ditemukan ${candidates.size} folder \"$SAF_ROOT_FOLDER_NAME\" di folder tujuan kustom " +
                        "(${candidates.joinToString(", ") { it.name ?: "?" }}) -- kemungkinan sisa duplikat lama " +
                        "dari provider SAF. Scan ini & seterusnya HANYA memakai \"${canonical.name}\", folder lain " +
                        "TIDAK disentuh -- gabungkan isinya manual lewat file manager kalau perlu."
                )
                settingsRepository.setCachedFolderUri(SAF_ROOT_CACHE_KEY, canonical.uri.toString())
                canonical
            }
            else -> findOrCreateChildDirSaf(parent, SAF_ROOT_FOLDER_NAME, cacheKey = SAF_ROOT_CACHE_KEY)
        }
    }

    /** Riwayat lengkap "root dihapus lalu dikembalikan lagi" ada di KDoc besar di atas [resolveCanonicalRootDirSaf]. */
    private suspend fun resolveSafRuleDestinations(destinationRoot: DocumentFile, rules: List<Rule>): Map<String, DocumentFile?> {
        val vaultRootDoc = resolveCanonicalRootDirSaf(destinationRoot)
        if (vaultRootDoc == null) {
            activityLogRepository.add(LogLevel.ERROR, "Gagal membuat/membuka folder root \"$SAF_ROOT_FOLDER_NAME\" di folder tujuan kustom.")
            return rules.associate { it.folderName to null }
        }
        val resolved = mutableMapOf<String, DocumentFile?>()
        for (rule in rules.distinctBy { it.folderName }) {
            // [Fix P0-1, audit gap 2026-08-16] Invarian yang SAMA dengan
            // jalur lokal ([FileSorter.moveFile]) -- lihat KDoc lengkap di
            // RuleFolderNameValidator.kt. Provider SAF TIDAK menjamin
            // containment yang sama seperti java.io.File, jadi nama rule
            // WAJIB tervalidasi juga sebelum dipakai `createDirectory()`.
            val folderNameError = validateRuleFolderName(rule.folderName)
            if (folderNameError != null) {
                activityLogRepository.add(
                    LogLevel.ERROR,
                    "Rule \"${rule.folderName}\" punya nama folder tidak valid ($folderNameError) -- folder tujuan kustom untuk rule ini dilewati."
                )
                resolved[rule.folderName] = null
                continue
            }
            val dir = findOrCreateChildDirSaf(vaultRootDoc, rule.folderName, cacheKey = rule.folderName)
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
            activityLogRepository.add(LogLevel.SUCCESS, "\"${file.name}\" -> folder tujuan kustom/${rule.folderName}/")
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

            // [Fix P0-3, audit gap 2026-08-16 -- PromptVault_real_functional_polish_gap_audit.md]
            // SEBELUMNYA `markUndone()` dipanggil TANPA SYARAT begitu salinan
            // balik sukses, TIDAK PEDULI `current.delete()` (hapus salinan lama
            // di folder kustom) berhasil atau tidak -- kalau provider SAF
            // menolak delete, riwayat SUDAH TERLANJUR ditandai "selesai
            // di-undo" padahal DUA salinan (lama di folder kustom + baru hasil
            // restore) masih ada sekaligus, dan UI tidak lagi menawarkan cara
            // untuk menindaklanjuti salinan lama yang nyangkut itu. Fix: hanya
            // tandai `markUndone` kalau delete BENAR-BENAR sukses -- kalau
            // gagal, entri riwayat SENGAJA dibiarkan "belum selesai" (bukan
            // silent-mark-done) supaya tetap terlihat & bisa dicoba lagi.
            val deleteOk = current.delete()
            if (deleteOk) {
                moveHistoryRepository.markUndone(entry.id)
                activityLogRepository.add(LogLevel.SUCCESS, "Undo berhasil: \"${entry.fileName}\" dikembalikan (folder kustom).")
            } else {
                activityLogRepository.add(
                    LogLevel.WARNING,
                    "Undo SEBAGIAN untuk \"${entry.fileName}\": salinan berhasil dikembalikan, TAPI file lama di folder kustom gagal dihapus (duplikat, masih ada). Entri riwayat TETAP tersedia -- coba undo lagi, atau hapus manual salinan lama itu."
                )
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

            // [Fix P0-3, audit gap 2026-08-16] Pola sama persis dengan
            // [undoSaf] di atas -- lihat komentar lengkap di sana. `markUndone`
            // HANYA dipanggil kalau `current.delete()` (hapus salinan di
            // folder tujuan kustom) benar-benar sukses.
            val deleteOk = current.delete()
            try {
                MediaScannerConnection.scanFile(context, arrayOf(restoreTarget.absolutePath), null, null)
            } catch (_: Exception) { /* non-fatal, sama seperti di moveFile() */ }
            if (deleteOk) {
                moveHistoryRepository.markUndone(entry.id)
                activityLogRepository.add(LogLevel.SUCCESS, "Undo berhasil: \"${entry.fileName}\" dikembalikan ke Downloads.")
            } else {
                activityLogRepository.add(
                    LogLevel.WARNING,
                    "Undo SEBAGIAN untuk \"${entry.fileName}\": salinan berhasil dikembalikan ke Downloads, TAPI file lama di folder tujuan kustom gagal dihapus (duplikat, masih ada). Entri riwayat TETAP tersedia -- coba undo lagi, atau hapus manual salinan lama itu."
                )
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
        val destLabel = if (destinationRoot != null) "folder tujuan kustom" else "PromptVault"
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

    /** Daftar nama file asli (SEMUA ekstensi, sejak fix 2026-08-13) di Downloads, dipakai layar Diagnostik agar user tahu format nama file sebenarnya. */
    fun listDownloadsCandidateFileNames(limit: Int = 100): List<String> {
        if (!downloadsDir.exists() || !downloadsDir.canRead()) return emptyList()
        return listCandidateFiles().map { it.name }.sorted().take(limit)
    }

    private enum class MoveOutcome { MOVED, SKIPPED_CONFLICT, FAILED }

    private suspend fun moveFile(file: File, rule: Rule, conflictStrategy: ConflictStrategy): MoveOutcome {
        return try {
            // [Fix P0-1, audit gap 2026-08-16] Gerbang TERAKHIR sebelum
            // rule.folderName benar-benar dipakai membangun path filesystem
            // nyata -- lihat KDoc lengkap di RuleFolderNameValidator.kt utk
            // kenapa ini WAJIB tetap ada di sini walau AddEditRuleScreen juga
            // sudah validasi inline (rule lama yang tersimpan sebelum fix ini
            // ada tidak pernah lolos validasi UI itu).
            val folderNameError = validateRuleFolderName(rule.folderName)
            if (folderNameError != null) {
                activityLogRepository.add(
                    LogLevel.ERROR,
                    "Rule \"${rule.folderName}\" punya nama folder tidak valid ($folderNameError) -- \"${file.name}\" dilewati. Perbaiki rule ini di Kelola Rule."
                )
                return MoveOutcome.FAILED
            }

            val destDir = File(vaultRootDir, rule.folderName)
            if (!isContainedIn(destDir, vaultRootDir)) {
                activityLogRepository.add(
                    LogLevel.ERROR,
                    "Rule \"${rule.folderName}\" menghasilkan path di luar folder PromptVault -- \"${file.name}\" dilewati demi keamanan."
                )
                return MoveOutcome.FAILED
            }
            if (!destDir.exists()) destDir.mkdirs()

            var destFile = File(destDir, file.name)
            if (destFile.exists()) {
                when (conflictStrategy) {
                    ConflictStrategy.SKIP -> return MoveOutcome.SKIPPED_CONFLICT
                    ConflictStrategy.OVERWRITE -> destFile.delete()
                    ConflictStrategy.RENAME -> {
                        val newName = nextAvailableFileName(file.name) { candidateName -> File(destDir, candidateName).exists() }
                        destFile = File(destDir, newName)
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

    /**
     * [Fix P0-2, audit gap 2026-08-16 -- PromptVault_real_functional_polish_gap_audit.md]
     * SEBELUMNYA `src.copyTo(dest, overwrite=false)` menulis LANGSUNG ke
     * `dest` (nama final) -- kalau copy gagal DI TENGAH JALAN (disk penuh,
     * I/O error, proses dibunuh OS), `dest` bisa berisi file PARSIAL/korup
     * yang tertinggal di lokasi final, dan catch block cuma `return false`
     * TANPA membersihkannya. Scan berikutnya bisa menemukan file rusak itu
     * sebagai "konflik nama sudah ada" -- padahal sebenarnya bukan hasil
     * pemindahan yang valid sama sekali.
     *
     * Fix: tulis ke file SEMENTARA (nama unik, folder tujuan yang sama --
     * supaya rename akhir tetap dalam filesystem yang sama/atomik kalau
     * memungkinkan) dulu, verifikasi copy selesai, BARU rename temp -> nama
     * final. `src` (sumber) HANYA dihapus SETELAH file tujuan final
     * terkonfirmasi lengkap. Kalau copy ATAU rename gagal di titik manapun,
     * file sementara dibersihkan (`runCatching { tempDest.delete() }`) --
     * `dest` (nama final) tidak PERNAH tersentuh sampai transfer benar-benar
     * tuntas, jadi tidak ada lagi kemungkinan file parsial nyangkut di nama
     * final.
     *
     * Kegagalan `src.delete()` SENGAJA TETAP tidak menggagalkan fungsi ini
     * (perilaku lama dipertahankan, konsisten dengan filosofi
     * [moveFileToSafDestination] "salinan ke tujuan sudah sukses & lengkap,
     * jangan dilaporkan gagal total") -- tapi sekarang DILOG sebagai
     * WARNING, bukan didiamkan, supaya duplikat di sumber tetap terlihat di
     * Activity Log, bukan hilang tanpa jejak.
     */
    private suspend fun copyThenDelete(src: File, dest: File): Boolean {
        val destDir = dest.parentFile ?: return false
        val tempDest = File(destDir, "${dest.name}.tmp_${UUID.randomUUID()}")
        return try {
            src.copyTo(tempDest, overwrite = false)
            if (!tempDest.renameTo(dest)) {
                runCatching { tempDest.delete() }
                activityLogRepository.add(LogLevel.ERROR, "Gagal finalisasi salinan \"${src.name}\" ke \"${dest.name}\" (rename file sementara gagal).")
                return false
            }
            if (!src.delete()) {
                activityLogRepository.add(LogLevel.WARNING, "\"${dest.name}\" berhasil disalin, tapi berkas asli \"${src.name}\" gagal dihapus (kemungkinan duplikat tertinggal).")
            }
            true
        } catch (e: Exception) {
            runCatching { tempDest.delete() }
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
     * Karakteristik dispatcher fungsi ini SENGAJA tidak mengurus perpindahan
     * thread sendiri -- caller (`MainViewModel.undoMove()`) SUDAH membungkus
     * pemanggilan fungsi ini dengan `withContext(Dispatchers.IO)` sejak
     * v2.20.1 (2026-08-13). Membungkus lagi DI SINI cuma jadi nested
     * `withContext(Dispatchers.IO)` yang redundan (secara fungsional tidak
     * salah, tapi tidak perlu & bisa menyesatkan pembaca yang mengira ini
     * "baru pertama kali" dipindah dispatcher). [Koreksi 2026-08-16] Sempat
     * salah ditambahkan di sini pada sesi yang sama krn catatan
     * PROJECT_STATE.md v2.16.0 lama ("temuan sampingan, kandidat batch
     * terpisah") dibaca tanpa cross-check entri v2.20.1 yang lebih baru,
     * yang TERNYATA sudah menutup gap ini di caller -- langsung direvert
     * begitu ketahuan, sebelum sempat di-package.
     */
    suspend fun undo(entry: MoveHistoryEntry): Boolean {
        // [Fitur baru 2026-08-17, integrasi Shizuku] Dicek PALING AWAL --
        // prefix palsu (bukan skema URI asli, cuma penanda), lihat
        // moveFileViaShizuku()/SHIZUKU_URI_PREFIX.
        if (entry.destUri.startsWith(SHIZUKU_URI_PREFIX)) {
            return undoShizuku(entry)
        }
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
         * [Fitur baru 2026-08-17, integrasi Shizuku] Penanda non-URI (bukan
         * skema `content://` asli) yang di-prepend ke [MoveHistoryEntry.destUri]
         * untuk entri yang dipindahkan lewat jalur Shizuku -- supaya [undo]
         * bisa membedakannya dari path lokal biasa TANPA butuh kolom/skema
         * Room baru (pola sama persis dengan cara prefix `content://`
         * membedakan entri SAF, lihat KDoc [undo]).
         */
        private const val SHIZUKU_URI_PREFIX = "shizuku://"

        /**
         * [Dikembalikan 2026-08-17 v2, lihat KDoc [resolveCanonicalRootDirSaf]]
         * Nama folder root yang di-auto-create app di dalam folder tujuan
         * kustom SAF. Regex-nya SENGAJA match nama asli TANPA akhiran juga
         * (bukan cuma varian "(N)") supaya deteksi duplikat konsisten dengan
         * kasus "cuma 1 folder, nama persis benar" (candidates.size == 1).
         */
        private const val SAF_ROOT_FOLDER_NAME = "PromptVault"
        private const val SAF_ROOT_CACHE_KEY = "PromptVault"
        private val SAF_ROOT_DUPLICATE_REGEX = Regex("^PromptVault(\\s\\(\\d+\\))?$")
    }
}
