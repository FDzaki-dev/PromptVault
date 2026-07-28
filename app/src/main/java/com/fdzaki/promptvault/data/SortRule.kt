package com.fdzaki.promptvault.data

/**
 * A single sorting rule: any filename matching [pattern] (glob-style, e.g. "AudioPlayer*")
 * gets moved into a subfolder named [folderName] inside the PromptVault root.
 */
data class SortRule(
    val id: Long = System.currentTimeMillis(),
    val pattern: String,
    val folderName: String,
    val enabled: Boolean = true,
    val filesMoved: Int = 0
)

data class SortLogEntry(
    val fileName: String,
    val matchedPattern: String,
    val destinationFolder: String,
    val timestampMillis: Long
)
