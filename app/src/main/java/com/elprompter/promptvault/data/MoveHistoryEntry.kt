package com.elprompter.promptvault.data

import kotlinx.serialization.Serializable

/**
 * Merekam satu pemindahan file agar bisa di-UNDO (fitur lengkap sejak v2.11.0).
 * originalParentUri & destUri disimpan sebagai path java.io.File String.
 */
@Serializable
data class MoveHistoryEntry(
    val id: String,
    val timestampMillis: Long,
    val fileName: String,
    val originalParentUri: String,
    val destUri: String,
    val ruleFolderName: String,
    val undone: Boolean = false
)
