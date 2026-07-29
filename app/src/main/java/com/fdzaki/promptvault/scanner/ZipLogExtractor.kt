package com.fdzaki.promptvault.scanner

import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.util.zip.ZipInputStream

/**
 * Result of extracting the first readable log/text entry from a ZIP stream.
 */
data class ExtractedLog(
    val entryName: String,
    val content: String,
    val lineCount: Int,
    val truncated: Boolean
)

/**
 * Decompresses a ZIP archive directly from an [InputStream] (no extraction to disk)
 * and returns the raw text content of the first entry matching .log/.txt.
 *
 * Defensive by design: caps read size to avoid OOM on malformed/huge archives,
 * and never throws — callers get null on any failure.
 */
object ZipLogExtractor {

    private const val MAX_CHARS = 200_000 // ~200 KB of text, plenty for a log prompt
    private val LOG_EXTENSIONS = setOf("log", "txt")

    /**
     * @param zipStream raw input stream of the ZIP archive (e.g. FileInputStream, not pre-extracted)
     * @return the first matching log/text entry's content, or null if none found or on error
     */
    fun extractFirstLog(zipStream: InputStream): ExtractedLog? {
        return try {
            ZipInputStream(zipStream).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    val name = entry.name
                    val ext = name.substringAfterLast('.', missingDelimiterValue = "").lowercase()

                    if (!entry.isDirectory && ext in LOG_EXTENSIONS) {
                        return readEntryCapped(zis)?.let { (text, lines, truncated) ->
                            ExtractedLog(
                                entryName = name,
                                content = text,
                                lineCount = lines,
                                truncated = truncated
                            )
                        }
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
                null
            }
        } catch (e: IOException) {
            null
        } catch (e: IllegalArgumentException) {
            // Thrown by ZipInputStream on corrupted/malformed archives
            null
        }
    }

    /** Reads the current ZIP entry's stream, capped at [MAX_CHARS] to stay memory-safe. */
    private fun readEntryCapped(input: InputStream): Triple<String, Int, Boolean>? {
        return try {
            val reader = BufferedReader(InputStreamReader(input, Charsets.UTF_8))
            val sb = StringBuilder()
            var lineCount = 0
            var truncated = false
            var line: String?

            while (reader.readLine().also { line = it } != null) {
                lineCount++
                if (sb.length < MAX_CHARS) {
                    sb.append(line).append('\n')
                } else {
                    truncated = true
                    // keep counting lines for accurate summary_metrics, but stop appending text
                }
            }
            Triple(sb.toString(), lineCount, truncated)
        } catch (e: IOException) {
            null
        }
    }
}
