package com.elprompter.promptvault.data

import kotlinx.serialization.Serializable

enum class LogLevel { SUCCESS, INFO, WARNING, ERROR }

@Serializable
data class ActivityLogEntry(
    val id: String,
    val timestampMillis: Long,
    val level: LogLevel,
    val message: String
)
