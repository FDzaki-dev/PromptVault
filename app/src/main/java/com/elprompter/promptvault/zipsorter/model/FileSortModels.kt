package com.elprompter.promptvault.zipsorter.model

/**
 * Kategori file berdasarkan ekstensi, dipakai khusus oleh modul Zip Sorter
 * (terpisah dari engine Rule berbasis pattern di util/FileSorter.kt).
 */
enum class FileCategory(val folderName: String, val extensions: List<String>) {
    DOCUMENTS("Documents", listOf("pdf", "doc", "docx", "txt", "xlsx", "pptx")),
    IMAGES("Images", listOf("jpg", "jpeg", "png", "gif", "webp", "svg")),
    VIDEOS("Videos", listOf("mp4", "mkv", "avi", "mov", "webm")),
    AUDIO("Audio", listOf("mp3", "wav", "flac", "aac", "m4a")),
    ARCHIVES("Archives", listOf("zip", "rar", "7z", "tar", "gz")),
    OTHERS("Others", emptyList());

    companion object {
        fun fromExtension(extension: String): FileCategory {
            val ext = extension.lowercase()
            return entries.firstOrNull { it.extensions.contains(ext) } ?: OTHERS
        }
    }
}

/** State untuk pemantauan proses asinkron via Flow. */
sealed class SortState {
    data object Idle : SortState()
    data class Scanning(val currentFolder: String) : SortState()
    data class Processing(val fileName: String, val progressPercent: Int) : SortState()
    data class Success(val processedCount: Int, val extractedZipCount: Int) : SortState()
    data class Error(val message: String, val throwable: Throwable? = null) : SortState()
}

/** Konfigurasi aturan proses sortir. */
data class SortConfig(
    val autoExtractZip: Boolean = true,
    val deleteZipAfterExtract: Boolean = false,
    val overwriteDuplicates: Boolean = false
)
