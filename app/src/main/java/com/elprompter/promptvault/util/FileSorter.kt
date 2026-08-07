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
import kotlinx.coroutines.coroutineScope
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
    val skippedDetails: List<SkippedFileInfo> = emptyList()
)

/** Hasil uji-coba pattern terhadap isi Downloads saat ini, dipakai di layar Tambah/Edit Rule. */
data class PatternPreviewResult(
    val totalCandidateFiles: Int,
    val matchedFileNames: List<String>
)

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
     * Batch §1 Fase 2 (hybrid, opsional, 2026-08-05). Baca URI SAF tersimpan
     * (diset lewat picker di Pengaturan, Fase 1/v2.7.0) dan validasi masih
     * bisa dipakai SEKARANG (bukan cuma "tersimpan"). Return null (bukan
     * exception) untuk SEMUA kegagalan -- URI kosong, parse gagal, izin
     * sudah dicabut OS/user, folder sudah dihapus/dicabut (mis. kartu SD
     * dilepas) -- supaya pemanggil SELALU bisa fallback diam-diam ke
     * Downloads/java.io.File tanpa perlu tahu alasan spesifiknya.
     */
    private suspend fun resolveSafRoot(): DocumentFile? {
        val uriString = settingsRepository.getSafTreeUri() ?: return null
        return try {
            val doc = DocumentFile.fromTreeUri(context, Uri.parse(uriString))
            if (doc == null || !doc.isDirectory) {
                activityLogRepository.add(LogLevel.ERROR, "Folder kustom tidak valid/tidak ditemukan, fallback ke Downloads.")
                return null
            }
            // SENGAJA tidak pakai doc.exists()/doc.canRead() sebagai gerbang --
            // keduanya bergantung pada COLUMN_FLAGS yang banyak DocumentProvider
            // (kartu SD, dll) tidak isi dengan benar, jadi sering FALSE NEGATIVE
            // walau folder sebenarnya bisa diakses. listFiles() di sini dipakai
            // sebagai probe akses NYATA -- kalau memang tidak bisa dibaca, ini
            // akan lempar Exception yang ditangkap di bawah, bukan diam-diam
            // return array kosong yang salah diartikan "folder kosong".
            doc.listFiles()
            doc
        } catch (e: Exception) {
            activityLogRepository.add(LogLevel.ERROR, "Folder kustom gagal dibaca (${e.message ?: e.javaClass.simpleName}), fallback ke Downloads.")
            null
        }
    }

    private fun listCandidateFiles(): Array<File> {
        return downloadsDir.listFiles { f ->
            f.isFile && (f.extension.equals("zip", true) || f.extension.equals("txt", true)) &&
                !isTempOrPartialFile(f) &&
                !f.absolutePath.startsWith(vaultRootDir.absolutePath)
        } ?: emptyArray()
    }

    /**
     * File sementara dari browser/downloader (belum selesai diunduh) tidak boleh
     * pernah masuk sebagai kandidat sama sekali -- bukan cuma "ditunda" seperti
     * [isLikelyStillWriting], tapi memang belum jadi file ZIP/TXT yang valid.
     * Daftar ini sengaja dicek terhadap NAMA LENGKAP (bukan cuma `.extension`
     * Kotlin) karena marker sering muncul sebagai akhiran ganda, mis.
     * "prompt.zip.crdownload".
     */
    private fun isTempOrPartialFile(file: File): Boolean {
        val lowerName = file.name.lowercase()
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
    suspend fun scanAndSort(): ScanResult = scanMutex.withLock { scanAndSortLocked() }

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

        // Batch §1 Fase 2 (hybrid, 2026-08-05): kalau user sudah pilih folder
        // kustom via SAF (Pengaturan) DAN izinnya MASIH valid sekarang, scan
        // jalan di folder ITU lewat DocumentFile -- bukan Downloads. Kalau
        // tidak ada / sudah tidak valid lagi, [resolveSafRoot] return null dan
        // kode di bawah lanjut ke jalur Downloads/java.io.File TIDAK BERUBAH
        // seperti sebelum Fase 2 ada -- fallback otomatis, tidak pernah error
        // ke user hanya karena SAF gagal (mode ini murni opsional).
        val safRoot = resolveSafRoot()
        if (safRoot != null) {
            return@withContext scanAndSortSafLocked(safRoot, rules, conflictStrategy)
        }

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
            activityLogRepository.add(LogLevel.INFO, "Scan selesai: tidak ada file ZIP/TXT baru yang cocok.")
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
     * Cari baris MediaStore yang path-nya di bawah Downloads/PromptVault/ TAPI
     * file fisiknya sudah tidak ada di disk (entri "hantu") -- lalu hapus baris
     * itu. Kenapa bisa "hantu": app ini pakai `java.io.File` langsung (bukan
     * SAF, lihat Keputusan Arsitektur #2 di PROJECT_STATE.md), jadi rename/
     * delete lewat filesystem tidak otomatis sinkron ke index MediaStore kalau
     * ada app LAIN yang sempat baca/index file itu duluan sebelum dipindah.
     * `MediaStore.Files.FileColumns.DATA` deprecated sejak API 29, tapi TETAP
     * berfungsi untuk app dengan `MANAGE_EXTERNAL_STORAGE` (app ini sudah
     * pakai izin itu) -- dipilih daripada migrasi total ke SAF/DocumentFile
     * karena itu §1 yang terpisah & sengaja belum dikerjakan (lihat komentar
     * di scanAndSort soal §1).
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

    // ========================================================================
    // Batch §1 Fase 2 (hybrid SAF, opsional, 2026-08-05) -- SEMUA fungsi di
    // bawah ini ("*Saf") adalah jalur PARALEL untuk scan/move/undo lewat
    // DocumentFile, dipanggil HANYA kalau [resolveSafRoot] menemukan URI SAF
    // valid. SENGAJA duplikat struktur dari versi legacy (java.io.File) di
    // atas, bukan digabung pakai abstraksi generic -- alasannya BUKAN malas,
    // tapi supaya: (1) jalur Downloads/java.io.File yang sudah stabil sejak
    // v2.4.0 TIDAK PERNAH tersentuh sama sekali oleh perubahan ini (nol risiko
    // regresi ke user yang tidak pakai SAF), dan (2) kalau ada bug di jalur
    // SAF (yang sama sekali belum bisa diverifikasi runtime di sandbox ini),
    // gampang diisolasi & di-revert tanpa menyentuh kode yang sudah teruji.
    //
    // KETERBATASAN yang sengaja diterima untuk batch ini (bukan bug):
    // - [previewPatternMatches] & [listDownloadsCandidateFileNames] (dipakai
    //   layar Tambah/Edit Rule & Diagnostik) TETAP baca Downloads/java.io.File
    //   walau mode SAF aktif -- keduanya fungsi non-suspend yang dipanggil
    //   sinkron dari UI, sedangkan baca setting SAF butuh suspend. Digabungkan
    //   di batch terpisah kalau nanti dibutuhkan, supaya batch ini tetap fokus
    //   ke scan/move/undo (fungsi inti yang benar-benar jalan di background).
    // - Dual Stability Guard versi SAF cuma pakai 2 dari 3 sinyal legacy (age +
    //   size-delta, TANPA file-lock check) -- lihat [isLikelyStillWritingSaf].
    // - §2 (cleanup ghost MediaStore) tidak dipanggil di jalur SAF -- itu query
    //   MediaStore berbasis path java.io.File, tidak relevan untuk content://.
    // - Move & undo pakai copy-lalu-hapus (bukan DocumentsContract.moveDocument)
    //   -- lebih portable lintas document provider yang dukungannya tidak
    //   konsisten untuk moveDocument, konsisten dengan strategi
    //   [copyThenDelete] yang sudah dipakai jalur legacy sebagai fallback.
    // ========================================================================

    /** Versi SAF dari [scanAndSortLocked] (lihat blok komentar di atas). */
    private suspend fun scanAndSortSafLocked(
        safRoot: DocumentFile,
        rules: List<Rule>,
        conflictStrategy: ConflictStrategy
    ): ScanResult = coroutineScope {
        if (rules.isEmpty()) {
            activityLogRepository.add(LogLevel.INFO, "Scan dijalankan, tapi belum ada rule aktif.")
            return@coroutineScope ScanResult(0, 0, foldersUnreadable = false, overlapWarnings = emptyList())
        }

        val candidateFiles = listCandidateFilesSaf(safRoot)

        if (candidateFiles.isEmpty()) {
            activityLogRepository.add(LogLevel.INFO, "Scan selesai: tidak ada file ZIP/TXT baru yang cocok.")
            return@coroutineScope ScanResult(0, 0, foldersUnreadable = false, overlapWarnings = emptyList())
        }

        val semaphore = Semaphore(SCAN_CONCURRENCY)
        val results = candidateFiles.map { doc ->
            async { semaphore.withPermit { processCandidateSaf(doc, safRoot, rules, conflictStrategy) } }
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
            "Scan selesai (folder kustom): $moved file dipindahkan, $skipped dilewati. Buka \"Detail File Dilewati\" untuk lihat nama filenya."
        } else {
            "Scan selesai (folder kustom): $moved file dipindahkan, $skipped dilewati."
        }
        activityLogRepository.add(LogLevel.SUCCESS, summary)

        return@coroutineScope ScanResult(moved, skipped, foldersUnreadable = false, overlapWarnings = overlapWarnings, skippedDetails = skippedDetails)
    }

    /** Versi SAF dari [listCandidateFiles]. */
    private fun listCandidateFilesSaf(safRoot: DocumentFile): List<DocumentFile> {
        return safRoot.listFiles().filter { doc ->
            val name = doc.name
            // SENGAJA pakai !doc.isDirectory, BUKAN doc.isFile. isFile() balik
            // ke query MIME type provider -- kalau provider tidak isi kolom MIME
            // dengan benar (umum, tergantung cara file itu nyampe di folder: SD
            // card, sync app, dll), isFile() FALSE NEGATIF walau file valid,
            // dan lolos-tidaknya jadi acak per file (persis kelas bug yang sama
            // dgn resolveSafRoot exists()/canRead(), lihat insiden #4/#6).
            // isDirectory() cek MIME_TYPE_DIR yang jauh lebih konsisten diisi
            // provider, jadi dipakai sebagai NEGATIVE check yang bisa dipercaya.
            !name.isNullOrBlank() && !doc.isDirectory &&
                (name.substringAfterLast('.', "").equals("zip", true) ||
                    name.substringAfterLast('.', "").equals("txt", true)) &&
                !TEMP_FILE_MARKERS.any { name.lowercase().endsWith(it) }
        }
    }

    /**
     * Versi SAF dari [isLikelyStillWriting] -- lihat catatan keterbatasan
     * "Dual Stability Guard" di blok komentar atas bagian SAF ini.
     */
    private suspend fun isLikelyStillWritingSaf(doc: DocumentFile): Boolean {
        val age = System.currentTimeMillis() - doc.lastModified()
        if (age in 0 until STABILITY_WINDOW_MS) return true

        val sizeBefore = runCatching { doc.length() }.getOrDefault(-1L)
        if (sizeBefore < 0) return true

        delay(SIZE_CHECK_DELAY_MS)

        val sizeAfter = runCatching { doc.length() }.getOrDefault(-1L)
        return sizeAfter < 0 || sizeAfter != sizeBefore
    }

    /** Versi SAF dari [processCandidate]. */
    private suspend fun processCandidateSaf(
        doc: DocumentFile,
        safRoot: DocumentFile,
        rules: List<Rule>,
        conflictStrategy: ConflictStrategy
    ): CandidateOutcome {
        val name = doc.name ?: return CandidateOutcome.Skipped(
            SkippedFileInfo("(tanpa nama)", "Dokumen SAF tanpa nama, dilewati.")
        )
        val sizeKb = doc.length() / 1024
        val matches = RuleOverlapChecker.matchingRules(name, sizeKb, rules)
        if (matches.isEmpty()) {
            return CandidateOutcome.Skipped(SkippedFileInfo(name, explainNoMatchByName(name, sizeKb, rules)))
        }

        if (isLikelyStillWritingSaf(doc)) {
            return CandidateOutcome.Skipped(
                SkippedFileInfo(
                    fileName = name,
                    reason = "Ditunda: file baru saja berubah, kemungkinan masih ditulis/didownload. Akan dicoba lagi scan berikutnya."
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
        return when (moveFileSaf(doc, rule, conflictStrategy, safRoot)) {
            MoveOutcome.MOVED -> CandidateOutcome.Moved(overlapWarning)
            MoveOutcome.SKIPPED_CONFLICT -> CandidateOutcome.Skipped(
                SkippedFileInfo(name, "Sudah ada file dengan nama sama di PromptVault/${rule.folderName}/ (strategi konflik: Lewati)"),
                overlapWarning
            )
            MoveOutcome.FAILED -> CandidateOutcome.Skipped(
                SkippedFileInfo(name, "Gagal dipindahkan (lihat Log untuk detail error)"),
                overlapWarning
            )
        }
    }

    /**
     * MIME type untuk `createFile()` SAF, diturunkan dari EKSTENSI nama file --
     * BUKAN dipercaya dari `doc.type`/provider sumber. Alasan (kelas bug yang sama
     * dengan insiden #4/#6 -- kolom metadata provider SAF sering tidak akurat):
     * kalau provider sumber mengisi MIME_TYPE generik/salah (mis. "application/
     * octet-stream" untuk file .txt, atau sebaliknya provider tujuan yang justru
     * MENAMBAH ekstensi sesuai mime saat `createFile()` dipanggil), hasilnya bisa
     * nama file dobel-ekstensi ("laporan.txt.txt") atau ekstensi salah, TANPA
     * exception apapun untuk menandainya. Menurunkan dari ekstensi sendiri (yang
     * sudah kita validasi di [listCandidateFilesSaf]/[isTempOrPartialFile] cuma
     * "zip"/"txt") jauh lebih bisa diprediksi lintas provider (Google Drive,
     * SD card, dll).
     */
    private fun mimeTypeForFileName(name: String): String = when (name.substringAfterLast('.', "").lowercase()) {
        "zip" -> "application/zip"
        "txt" -> "text/plain"
        else -> "application/octet-stream"
    }

    /** Cari subfolder bernama [name] di [parent]; buat baru kalau belum ada. */
    private fun findOrCreateChildDirSaf(parent: DocumentFile, name: String): DocumentFile? {
        return parent.listFiles().firstOrNull { it.isDirectory && it.name == name } ?: parent.createDirectory(name)
    }

    /** Salin isi byte [src] -> [dest] apa adanya lewat ContentResolver stream. */
    private fun copyDocumentBytes(src: Uri, dest: Uri): Boolean {
        return try {
            context.contentResolver.openInputStream(src)?.use { input ->
                context.contentResolver.openOutputStream(dest)?.use { output ->
                    input.copyTo(output)
                } ?: return false
            } ?: return false
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Versi SAF dari [moveFile]. Copy-lalu-hapus (lihat catatan di blok
     * komentar atas), BUKAN `DocumentsContract.moveDocument`.
     */
    private suspend fun moveFileSaf(
        doc: DocumentFile,
        rule: Rule,
        conflictStrategy: ConflictStrategy,
        safRoot: DocumentFile
    ): MoveOutcome {
        val fileName = doc.name ?: return MoveOutcome.FAILED
        return try {
            val vaultDir = findOrCreateChildDirSaf(safRoot, "PromptVault") ?: return MoveOutcome.FAILED
            val destDir = findOrCreateChildDirSaf(vaultDir, rule.folderName) ?: return MoveOutcome.FAILED

            var targetName = fileName
            val existing = destDir.listFiles().firstOrNull { it.name == targetName }
            if (existing != null) {
                when (conflictStrategy) {
                    ConflictStrategy.SKIP -> return MoveOutcome.SKIPPED_CONFLICT
                    ConflictStrategy.OVERWRITE -> existing.delete()
                    ConflictStrategy.RENAME -> {
                        val baseName = fileName.substringBeforeLast('.', fileName)
                        val ext = fileName.substringAfterLast('.', "")
                        var counter = 1
                        var candidate = if (ext.isNotEmpty()) "${baseName}_$counter.$ext" else "${baseName}_$counter"
                        while (destDir.listFiles().any { it.name == candidate }) {
                            counter++
                            candidate = if (ext.isNotEmpty()) "${baseName}_$counter.$ext" else "${baseName}_$counter"
                        }
                        targetName = candidate
                    }
                }
            }

            val mimeType = mimeTypeForFileName(targetName)
            val newDoc = destDir.createFile(mimeType, targetName) ?: return MoveOutcome.FAILED
            if (newDoc.name != null && newDoc.name != targetName) {
                // Provider tujuan mengubah nama sendiri (mis. tambah/ubah ekstensi
                // sesuai mime) -- BUKAN bug di sisi kita, tapi user WAJIB tahu nama
                // aktual di penyimpanan bisa beda dari nama file asli.
                activityLogRepository.add(
                    LogLevel.WARNING,
                    "\"$fileName\" tersimpan sebagai \"${newDoc.name}\" (folder kustom mengubah nama otomatis)."
                )
            }
            val copied = copyDocumentBytes(doc.uri, newDoc.uri)
            if (!copied) {
                runCatching { newDoc.delete() }
                activityLogRepository.add(LogLevel.ERROR, "Gagal menyalin \"$fileName\" (folder kustom).")
                return MoveOutcome.FAILED
            }

            val deleted = doc.delete()

            moveHistoryRepository.record(
                MoveHistoryEntry(
                    id = UUID.randomUUID().toString(),
                    timestampMillis = System.currentTimeMillis(),
                    fileName = newDoc.name ?: targetName,
                    originalParentUri = safRoot.uri.toString(),
                    destUri = newDoc.uri.toString(),
                    ruleFolderName = rule.folderName
                )
            )

            if (!deleted) {
                // File SUDAH tersalin aman ke tujuan -- tetap dihitung MOVED
                // (data user tidak hilang), tapi user diberitahu ada
                // kemungkinan duplikat tertinggal di sumber untuk dihapus manual.
                activityLogRepository.add(
                    LogLevel.WARNING,
                    "\"$fileName\" tersalin ke PromptVault/${rule.folderName}/, tapi file asli di folder kustom gagal dihapus (mungkin ada duplikat)."
                )
            } else {
                activityLogRepository.add(LogLevel.SUCCESS, "\"$fileName\" -> PromptVault/${rule.folderName}/ (folder kustom)")
            }
            MoveOutcome.MOVED
        } catch (e: Exception) {
            activityLogRepository.add(LogLevel.ERROR, "Error memindahkan \"$fileName\" (folder kustom): ${e.message}")
            MoveOutcome.FAILED
        }
    }

    /** Versi SAF dari [undo]. Dipanggil dari [undo] kalau [MoveHistoryEntry.destUri] berupa content:// URI. */
    private suspend fun undoSaf(entry: MoveHistoryEntry): Boolean {
        return try {
            val current = DocumentFile.fromSingleUri(context, Uri.parse(entry.destUri))
            // SENGAJA tidak pakai current.exists() sbg gerbang (kelas bug sama
            // spt insiden #4/#6 -- method boolean DocumentFile false-negatif).
            // Cukup null-check; kalau memang sudah tidak ada, createFile/copy di
            // bawah akan gagal natural & ketangkap try-catch luar.
            if (current == null) {
                activityLogRepository.add(LogLevel.ERROR, "Undo gagal: \"${entry.fileName}\" sudah tidak ada di tujuan.")
                return false
            }
            val originalRoot = DocumentFile.fromTreeUri(context, Uri.parse(entry.originalParentUri))
            // Sama: canWrite() dibuang, cukup null-check + isDirectory. Izin
            // tulis nyata diuji lewat createFile() asli di bawah, bukan heuristik.
            if (originalRoot == null || !originalRoot.isDirectory) {
                activityLogRepository.add(
                    LogLevel.ERROR,
                    "Undo gagal: folder kustom asal \"${entry.fileName}\" sudah tidak bisa diakses (izin dicabut?)."
                )
                return false
            }

            val baseName = entry.fileName.substringBeforeLast('.', entry.fileName)
            val ext = entry.fileName.substringAfterLast('.', "")
            var targetName = entry.fileName
            var counter = 1
            while (originalRoot.listFiles().any { it.name == targetName }) {
                targetName = if (ext.isNotEmpty()) "${baseName}_restored_$counter.$ext" else "${baseName}_restored_$counter"
                counter++
            }

            val mimeType = mimeTypeForFileName(targetName)
            val restoreDoc = originalRoot.createFile(mimeType, targetName)
            if (restoreDoc == null) {
                activityLogRepository.add(LogLevel.ERROR, "Undo gagal untuk \"${entry.fileName}\" (folder kustom).")
                return false
            }
            val copied = copyDocumentBytes(current.uri, restoreDoc.uri)
            if (!copied) {
                runCatching { restoreDoc.delete() }
                activityLogRepository.add(LogLevel.ERROR, "Undo gagal untuk \"${entry.fileName}\" (folder kustom).")
                return false
            }
            val deleted = current.delete()
            moveHistoryRepository.markUndone(entry.id)
            if (!deleted) {
                activityLogRepository.add(
                    LogLevel.WARNING,
                    "Undo \"${entry.fileName}\" tersalin balik, tapi salinan di PromptVault gagal dihapus (mungkin ada duplikat)."
                )
            } else {
                activityLogRepository.add(LogLevel.SUCCESS, "Undo berhasil: \"${entry.fileName}\" dikembalikan ke folder kustom.")
            }
            true
        } catch (e: Exception) {
            activityLogRepository.add(LogLevel.ERROR, "Error saat undo \"${entry.fileName}\" (folder kustom): ${e.message}")
            false
        }
    }
    // ==================== akhir blok Batch §1 Fase 2 (SAF) ====================

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

    /**
     * Diekstrak dari [explainNoMatch] (Batch §1 Fase 2) supaya bisa dipakai
     * ulang oleh jalur SAF ([processCandidateSaf]) tanpa butuh `java.io.File`
     * -- logikanya sendiri sudah generic terhadap nama file, tidak pernah
     * menyentuh API File spesifik.
     */
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
     * isi Downloads saat ini. Dipakai di layar Tambah/Edit Rule supaya user lihat
     * langsung dampak pattern-nya SEBELUM menyimpan rule. Mendukung multi-pattern CSV.
     */
    fun previewPatternMatches(pattern: String, excludePattern: String = ""): PatternPreviewResult {
        if (pattern.isBlank() || !downloadsDir.exists() || !downloadsDir.canRead()) {
            return PatternPreviewResult(0, emptyList())
        }
        val candidates = listCandidateFiles()
        val matched = candidates
            .filter { GlobMatcher.matchesAny(it.name, pattern) }
            .filterNot { excludePattern.isNotBlank() && GlobMatcher.matchesAny(it.name, excludePattern) }
            .map { it.name }
        return PatternPreviewResult(candidates.size, matched)
    }

    /** Daftar nama file ZIP/TXT asli di Downloads, dipakai layar Diagnostik agar user tahu format nama file sebenarnya. */
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
                // lewat java.io.File langsung (bukan SAF/MediaStore API), jadi index
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

    /** TODO #1: UNDO satu entri riwayat pemindahan. */
    suspend fun undo(entry: MoveHistoryEntry): Boolean {
        // Batch §1 Fase 2: destUri berupa content:// URI berarti file
        // dipindah lewat jalur SAF ([moveFileSaf]) -- undo-nya juga wajib
        // lewat DocumentFile, bukan java.io.File (Uri content:// tidak bisa
        // dibuka lewat java.io.File sama sekali).
        if (entry.destUri.startsWith("content://")) {
            return undoSaf(entry)
        }
        return undoLegacy(entry)
    }

    private suspend fun undoLegacy(entry: MoveHistoryEntry): Boolean {
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
