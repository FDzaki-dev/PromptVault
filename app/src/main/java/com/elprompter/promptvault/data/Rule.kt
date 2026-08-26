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
    val enabled: Boolean = true,
    // [Fitur baru: Keep Latest Version Only, 2026-08-26] Opsional, default
    // false (0 regresi rule lama -- kolom baru di data class @Serializable
    // otomatis backward-compatible saat decode JSON lama yang belum punya
    // field ini). true = folder tujuan rule ini HANYA menyisakan 1 file
    // (versi terbaru yang barusan discan/dipindah) -- file lain di folder
    // itu (versi lama) DIHAPUS OTOMATIS tiap kali file baru yang cocok
    // rule ini berhasil dipindahkan, baik lewat scan manual maupun
    // otomatis. Lihat FileSorter.kt (enforceKeepLatestVersionOnly*) untuk
    // implementasi penghapusannya.
    val keepLatestVersionOnly: Boolean = false
)
