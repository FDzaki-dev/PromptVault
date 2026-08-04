package com.elprompter.promptvault.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Baris tabel Room untuk riwayat pemindahan file (dasar fitur UNDO).
 * Bentuknya sengaja identik dengan domain model [com.elprompter.promptvault.data.MoveHistoryEntry].
 */
@Entity(tableName = "move_history")
data class MoveHistoryEntity(
    @PrimaryKey val id: String,
    val timestampMillis: Long,
    val fileName: String,
    val originalParentUri: String,
    val destUri: String,
    val ruleFolderName: String,
    val undone: Boolean = false
)
