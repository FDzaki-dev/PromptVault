package com.elprompter.promptvault.data

import kotlinx.serialization.Serializable

@Serializable
data class Rule(
    val id: String,
    val folderName: String,
    val pattern: String,       // glob pattern, mis: "invoice_*.zip" atau "*.txt"
    val enabled: Boolean = true
)
