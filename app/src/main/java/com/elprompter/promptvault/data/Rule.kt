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
    // [Fitur baru 2026-08-26 batch 2, permintaan eksplisit user] Opt-in
    // PER RULE utk "tahan versi .zip terbaru di Downloads" (lihat
    // computeLatestZipHeldBack() di FileSorter.kt). SEBELUMNYA (v8.32.0)
    // perilaku ini otomatis berlaku ke SEMUA rule yang cocok scope .zip+SAF
    // tanpa kontrol user sama sekali -- sekarang HANYA rule yang togglenya
    // AKTIF di sini yang boleh menyisakan file. Default false: rule LAMA
    // (hasil decode JSON tanpa field ini, backward-compatible) DAN rule
    // BARU SAMA-SAMA mulai OFF, user harus nyalakan eksplisit per rule
    // lewat AddEditRuleScreen.
    val holdBackLatestZip: Boolean = false
)
