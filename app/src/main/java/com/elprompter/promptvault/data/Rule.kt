package com.elprompter.promptvault.data

import kotlinx.serialization.Serializable

@Serializable
data class Rule(
    val id: String,
    val folderName: String,
    val pattern: String,       // glob pattern, BOLEH lebih dari satu dipisah koma, mis: "invoice_*.zip, receipt_*.txt"
    val excludePattern: String = "", // opsional; kosong = tidak ada pengecualian; boleh juga dipisah koma
    val minSizeKb: Long? = null, // opsional; null = tidak ada batas minimum
    val maxSizeKb: Long? = null, // opsional; null = tidak ada batas maksimum
    val enabled: Boolean = true
)
