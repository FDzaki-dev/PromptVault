package com.elprompter.promptvault.util

import android.content.Context
import android.os.Environment
import com.elprompter.promptvault.data.ActivityLogRepository
import com.elprompter.promptvault.data.LogLevel
import com.elprompter.promptvault.data.MoveHistoryEntry
import com.elprompter.promptvault.data.MoveHistoryRepository
import com.elprompter.promptvault.data.Rule
import com.elprompter.promptvault.data.RuleRepository
import java.io.File
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
 * (berurutan sesuai PRIORITAS -- rule dengan index lebih kecil menang kalau
 * file cocok lebih dari satu rule), pindahkan ke Downloads/PromptVault/<folderName>/,
 * dan catat riwayat untuk undo.
 *
 * Prinsip "expert-level file organizer": setiap file yang TIDAK dipindahkan harus
 * bisa dijelaskan alasannya secara spesifik ke user, bukan cuma angka "dilewati".
 * Termasuk file yang sengaja DITUNDA karena kemungkinan masih ditulis/didownload.
 */
class FileSorter(
    private val context: Context,
    private val ruleRepository: RuleRepository,
    private val activityLogRepository: ActivityLogRepository,
    private val moveHistoryRepository: MoveHistoryRepository
) {

    private val downloadsDir: File
        get() = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)

    private val vaultRootDir: File
        get() = File(downloadsDir, "PromptVault")

    private fun listCandidateFiles(): Array<File> {
        return downloadsDir.listFiles { f ->
            f.isFile && (f.extension.equals("zip", true) || f.extension.equals("txt", true)) &&
                !f.absolutePath.startsWith(vaultRootDir.absolutePath)
        } ?: emptyArray()
    }

    /**
     * File yang baru diubah/ditulis dalam beberapa detik terakhir DITUNDA dulu,
     * bukan dipindahkan langsung. Ini mencegah race condition klasik: file ZIP/TXT
     * yang masih dalam proses download/ditulis ikut terpindah setengah jadi dan
     * jadi korup. File akan otomatis dicoba lagi di scan berikutnya begitu sudah
     * "diam" (tidak berubah) lebih dari STABILITY_WINDOW_MS.
     */
    private fun isLikelyStillWriting(file: File): Boolean {
        val age = System.currentTimeMillis() - file.lastModified()
        return age in 0 until STABILITY_WINDOW_MS
    }

    suspend fun scanAndSort(): ScanResult {
        val rules = ruleRepository.getRules().filter { it.enabled }

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

            // Rule diurutkan berdasarkan PRIORITAS (urutan di layar Kelola Rule).
            // Rule pertama yang cocok DAN tidak dikecualikan yang menang.
            val matches = RuleOverlapChecker.matchingRules(file.name, rules)
            if (matches.isEmpty()) {
                skipped++
                val excludedBy = rules.firstOrNull {
                    GlobMatcher.matches(file.name, it.pattern) && RuleOverlapChecker.isExcluded(file.name, it)
                }
                val reason = if (excludedBy != null) {
                    "Cocok pattern \"${excludedBy.pattern}\" tapi dikecualikan oleh excludePattern \"${excludedBy.excludePattern}\" di rule \"${excludedBy.folderName}\""
                } else {
                    val activePatterns = rules.joinToString(", ") { "\"${it.pattern}\"" }
                    "Tidak cocok pattern rule manapun (rule aktif: $activePatterns)"
                }
                skippedDetails.add(SkippedFileInfo(file.name, reason))
                continue
            }
            if (matches.size > 1) {
                val msg = "\"${file.name}\" cocok dengan ${matches.size} rule (${matches.joinToString { it.folderName }}). " +
                    "Dipindahkan memakai rule prioritas tertinggi: \"${matches.first().folderName}\"."
                overlapWarnings.add(msg)
                activityLogRepository.add(LogLevel.WARNING, msg)
            }
            val rule = matches.first()
            val moveSuccess = moveFile(file, rule)
            if (moveSuccess) {
                moved++
            } else {
                skipped++
                skippedDetails.add(SkippedFileInfo(file.name, "Gagal dipindahkan (lihat Log untuk detail error)"))
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

    /**
     * Uji pattern include + exclude (belum tentu tersimpan sebagai rule) terhadap
     * isi Downloads saat ini. Dipakai di layar Tambah/Edit Rule supaya user lihat
     * langsung dampak pattern-nya SEBELUM menyimpan rule.
     */
    fun previewPatternMatches(pattern: String, excludePattern: String = ""): PatternPreviewResult {
        if (pattern.isBlank() || !downloadsDir.exists() || !downloadsDir.canRead()) {
            return PatternPreviewResult(0, emptyList())
        }
        val candidates = listCandidateFiles()
        val matched = candidates
            .filter { GlobMatcher.matches(it.name, pattern) }
            .filterNot { excludePattern.isNotBlank() && GlobMatcher.matches(it.name, excludePattern) }
            .map { it.name }
        return PatternPreviewResult(candidates.size, matched)
    }

    /** Daftar nama file ZIP/TXT asli di Downloads, dipakai layar Diagnostik agar user tahu format nama file sebenarnya. */
    fun listDownloadsCandidateFileNames(limit: Int = 100): List<String> {
        if (!downloadsDir.exists() || !downloadsDir.canRead()) return emptyList()
        return listCandidateFiles().map { it.name }.sorted().take(limit)
    }

    private suspend fun moveFile(file: File, rule: Rule): Boolean {
        return try {
            val destDir = File(vaultRootDir, rule.folderName)
            if (!destDir.exists()) destDir.mkdirs()

            var destFile = File(destDir, file.name)
            var counter = 1
            while (destFile.exists()) {
                destFile = File(destDir, "${file.nameWithoutExtension}_$counter.${file.extension}")
                counter++
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
            } else {
                activityLogRepository.add(LogLevel.ERROR, "Gagal memindahkan \"${file.name}\".")
            }
            success
        } catch (e: Exception) {
            activityLogRepository.add(LogLevel.ERROR, "Error memindahkan \"${file.name}\": ${e.message}")
            false
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
    }
}
