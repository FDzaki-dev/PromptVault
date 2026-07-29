package com.fdzaki.promptvault.scanner

import android.os.Environment
import com.fdzaki.promptvault.data.SortLogEntry
import com.fdzaki.promptvault.data.SortRule
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.util.regex.Pattern

/** Explicit outcome of a scan so the UI can give real feedback instead of silent no-ops. */
sealed class ScanResult {
    data class Success(val movedEntries: List<SortLogEntry>) : ScanResult()
    object DownloadsDirUnavailable : ScanResult()
    object NoMatchingFiles : ScanResult()
}

/**
 * Scans the public Downloads directory for .zip/.txt files whose name matches
 * a user-defined glob pattern (e.g. "AudioPlayer*") and moves them into a
 * dedicated project subfolder under Downloads/PromptVault/<folderName>/.
 *
 * Requires MANAGE_EXTERNAL_STORAGE (Android 11+) to move files outside the app's
 * own sandbox, granted via the in-app permission screen.
 */
class DownloadsSorter {

    private val supportedExtensions = setOf("zip", "txt")

    fun scanAndSort(rules: List<SortRule>): ScanResult {
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        if (!downloadsDir.exists() || !downloadsDir.isDirectory) {
            return ScanResult.DownloadsDirUnavailable
        }

        val vaultRoot = File(downloadsDir, "PromptVault").apply { mkdirs() }
        val logs = mutableListOf<SortLogEntry>()

        val candidateFiles = downloadsDir.listFiles { file ->
            file.isFile && file.extension.lowercase() in supportedExtensions
        } ?: emptyArray()

        for (file in candidateFiles) {
            val matchedRule = rules.firstOrNull { rule ->
                rule.enabled && globToRegex(rule.pattern).matcher(file.name).matches()
            } ?: continue

            val destinationDir = File(vaultRoot, matchedRule.folderName).apply { mkdirs() }
            val destinationFile = uniqueDestination(destinationDir, file.name)

            val moved = file.renameTo(destinationFile)
            if (moved) {
                logs.add(
                    SortLogEntry(
                        fileName = file.name,
                        matchedPattern = matchedRule.pattern,
                        destinationFolder = matchedRule.folderName,
                        timestampMillis = System.currentTimeMillis()
                    )
                )
            }
        }
        return if (logs.isEmpty()) ScanResult.NoMatchingFiles else ScanResult.Success(logs)
    }

    /** Avoids overwriting an existing file by appending "(1)", "(2)", etc. */
    private fun uniqueDestination(dir: File, fileName: String): File {
        var candidate = File(dir, fileName)
        if (!candidate.exists()) return candidate

        val dot = fileName.lastIndexOf('.')
        val base = if (dot >= 0) fileName.substring(0, dot) else fileName
        val ext = if (dot >= 0) fileName.substring(dot) else ""

        var counter = 1
        while (candidate.exists()) {
            candidate = File(dir, "$base ($counter)$ext")
            counter++
        }
        return candidate
    }

    /**
     * Opens [zipFile] as a stream (no disk extraction), pulls the first .log/.txt entry,
     * and wraps it in the injection-resistant Universal Log Parsing system prompt.
     * Returns null if the file isn't a valid ZIP or contains no log/text entry.
     */
    fun buildLogPromptFromZip(zipFile: File): String? {
        if (!zipFile.exists() || !zipFile.isFile) return null
        val extracted = try {
            FileInputStream(zipFile).use { ZipLogExtractor.extractFirstLog(it) }
        } catch (e: IOException) {
            null
        } ?: return null

        return LogPromptBuilder.build(extracted)
    }

    /** Converts a simple glob (supports '*' and '?') into a compiled, case-insensitive regex. */
    private fun globToRegex(glob: String): Pattern {
        val sb = StringBuilder("^")
        for (c in glob) {
            when (c) {
                '*' -> sb.append(".*")
                '?' -> sb.append(".")
                '.', '(', ')', '+', '|', '^', '$', '@', '%' -> sb.append("\\").append(c)
                else -> sb.append(c)
            }
        }
        sb.append("$")
        return Pattern.compile(sb.toString(), Pattern.CASE_INSENSITIVE)
    }
}
