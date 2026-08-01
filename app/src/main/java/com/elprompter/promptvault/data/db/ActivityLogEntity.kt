package com.elprompter.promptvault.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.elprompter.promptvault.data.LogLevel

/**
 * Baris tabel Room untuk riwayat aktivitas.
 * Bentuknya sengaja identik dengan domain model [com.elprompter.promptvault.data.ActivityLogEntry]
 * agar mapping di repository tetap sederhana dan tidak ada informasi yang hilang.
 */
@Entity(tableName = "activity_log")
data class ActivityLogEntity(
    @PrimaryKey val id: String,
    val timestampMillis: Long,
    val level: LogLevel,
    val message: String
)
