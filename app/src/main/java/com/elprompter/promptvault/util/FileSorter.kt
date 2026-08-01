package com.elprompter.promptvault.util

import android.content.Context
import android.os.Environment
import com.elprompter.promptvault.data.ActivityLogRepository
import com.elprompter.promptvault.data.ConflictStrategy
import com.elprompter.promptvault.data.LogLevel
import com.elprompter.promptvault.data.MoveHistoryEntry
import com.elprompter.promptvault.data.MoveHistoryRepository
import com.elprompter.promptvault.data.Rule
import com.elprompter.promptvault.data.RuleRepository
import com.elprompter.promptvault.data.SettingsRepository
import kotlinx.coroutines.delay
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

    suspend fun scanAndSort(): ScanResult {
        val rules = ruleRepository.getRules().filter { it.enabled }
        val conflictStrategy = settingsRepository.getConflictStrategy()

        if (!downloadsDir.exists() || !downloadsDir.canRead()) {
            activityLogRepository.add(LogLevel.ERROR, "Folder Downloads tidak terbaca. Cek izin penyimpanan.")
            return ScanResult(0, 0, foldersUnreadable = true, overlapWarnings = emptyList())
        }

        if (rules.isEmpty()) {
            activityLogRepository.add(LogLevel.INFO, "Scan dijalankan, tapi belum ada rule aktif.")
            return ScanResult(0, 0, foldersUnreadable = false, overlapWarnings = emptyList())
        }

        val candidateFiles = listCandidateFiles()

        if (candidateFiles.isEmpty()) {
            activityLogRepository.add(LogLevel.INFO, "Scan selesai: tidak ada file ZIP/TXT baru yang cocok.")
            return ScanResult(0, 0, foldersUnreadable = false, overlapWarnings = emptyList())
        }

        var moved = 0
        var skipped = 0
        val overlapWarnings = mutableListOf<String>()
        val skippedDetails = mutableListOf<SkippedFileInfo>()

        for (file in candidateFiles) {
            if (isLikelyStillWriting(file)) {
                skipped++
                skippedDetails.add(
                    SkippedFileInfo(
                        fileName = file.name,
                        reason = "Ditunda: file baru saja berubah, kemungkinan masih ditulis/didownload. Akan dicoba lagi scan berikutnya."
                    )
                )
                continue
            }

            val sizeKb = file.sizeKb()
            val matches = RuleOverlapChecker.matchingRules(file.name, sizeKb, rules)
            if (matches.isEmpty()) {
                skipped++
                skippedDetails.add(SkippedFileInfo(file.name, explainNoMatch(file, sizeKb, rules)))
                continue
            }
            if (matches.size > 1) {
                val msg = "\"${file.name}\" cocok dengan ${matches.size} rule (${matches.joinToString { it.folderName }}). " +
                    "Dipindahkan memakai rule prioritas tertinggi: \"${matches.first().folderName}\"."
                overlapWarnings.add(msg)
                activityLogRepository.add(LogLevel.WARNING, msg)
            }
            val rule = matches.first()
            val moveOutcome = moveFile(file, rule, conflictStrategy)
            when (moveOutcome) {
                MoveOutcome.MOVED -> moved++
                MoveOutcome.SKIPPED_CONFLICT -> {
                    skipped++
                    skippedDetails.add(SkippedFileInfo(file.name, "Sudah ada file dengan nama sama di PromptVault/${rule.folderName}/ (strategi konflik: Lewati)"))
                }
                MoveOutcome.FAILED -> {
                    skipped++
                    skippedDetails.add(SkippedFileInfo(file.name, "Gagal dipindahkan (lihat Log untuk detail error)"))
                }
            }
        }

        val summary = if (skipped > 0) {
            "Scan selesai: $moved file dipindahkan, $skipped dilewati. Buka \"Detail File Dilewati\" untuk lihat nama filenya."
        } else {
            "Scan selesai: $moved file dipindahkan, $skipped dilewati."
        }
        activityLogRepository.add(LogLevel.SUCCESS, summary)

        return ScanResult(moved, skipped, foldersUnreadable = false, overlapWarnings = overlapWarnings, skippedDetails = skippedDetails)
    }

    private fun explainNoMatch(file: File, sizeKb: Long, rules: List<Rule>): String {
        val excludedBy = rules.firstOrNull {
            GlobMatcher.matchesAny(file.name, it.pattern) && RuleOverlapChecker.isExcluded(file.name, it)
        }
        if (excludedBy != null) {
            return "Cocok pattern \"${excludedBy.pattern}\" tapi dikecualikan oleh excludePattern \"${excludedBy.excludePattern}\" di rule \"${excludedBy.folderName}\""
        }
        val sizeMismatch = rules.firstOrNull {
            GlobMatcher.matchesAny(file.name, it.pattern) && !RuleOverlapChecker.matchesSizeConstraint(sizeKb, it)
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
        /** Jeda aman sebelum file dianggap "selesai ditulis" dan boleh dipindah. */
        private const val STABILITY_WINDOW_MS = 5_000L

        /** Jeda pengecekan ukuran file untuk Dual Stability Guard (§4). */
        private const val SIZE_CHECK_DELAY_MS = 1_000L

        /** Akhiran nama file dari browser/downloader yang menandakan unduhan belum selesai. */
        private val TEMP_FILE_MARKERS = listOf(
            ".crdownload", ".tmp", ".part", ".download", ".downloading"
        )
    }
}
