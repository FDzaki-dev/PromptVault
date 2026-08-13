package com.elprompter.promptvault.ui.theme

import androidx.compose.ui.unit.dp

/**
 * v3.0.0 — Konstanta tactile terpusat (bab 12 spesifikasi: "Do not duplicate
 * tactile constants throughout screen files"). Satu arah cahaya simulasi
 * untuk seluruh app: kiri-atas terang -> kanan-bawah gelap (bab 3).
 *
 * v4.0.0 — Ditambah token elevasi utk sistem "depth/3D ultra immersive"
 * (permintaan eksplisit user). SEMUA elevasi baru di sini dipakai LEWAT
 * `Surface(shadowElevation=...)` dengan `color` SOLID (bukan `Modifier.shadow`
 * yang dirantai langsung ke node ber-Brush gradient) -- lihat dokumentasi
 * di `VaultCard.kt` untuk alasan lengkap: kombinasi itu PERNAH menyebabkan
 * regresi nyata (kotak pucat/glitch) di CTA Home v2.14.0, di-fix v2.14.1
 * dengan MELEPAS shadow sepenuhnya. Sistem baru ini menghidupkan kembali
 * shadow BERELEVASI NYATA tapi dengan pola aman (solid base + overlay brush
 * terpisah), bukan mengulang kombinasi yang sama.
 */
object TactileTokens {
    /** Elevasi normal kontrol yang bisa ditekan (terangkat). */
    val ElevationRaised = 4.dp

    /** Elevasi saat ditekan -- kontrol "tenggelam", kehilangan elevasi. */
    val ElevationPressed = 0.dp

    /** Skala saat ditekan -- perubahan kecil, bukan bounce berlebihan. */
    const val PressScale = 0.98f

    /** Durasi animasi tekan, harus terasa langsung (immediate). */
    const val PressAnimationMillis = 120

    /** Radius bevel standar untuk kontrol tactile (tombol, chip ikon, dll). */
    val ControlCornerRadius = 12.dp

    // ---- v4.0.0: sistem depth/3D immersive ----

    /** Elevasi istirahat VaultCard -- "mengambang" nyata di atas AMOLED, bukan flat. */
    val ElevationCard = 14.dp

    /** Elevasi CTA "Scan Sekarang" saat idle -- paling menonjol (titik fokus utama layar). */
    val ElevationCta = 16.dp

    /** Elevasi CTA saat ditekan -- turun tajam, tegas terasa "ditekan". */
    val ElevationCtaPressed = 3.dp

    /** Elevasi kotak ikon GroupedListRow -- depth kecil, sengaja subtil (bukan CTA). */
    val ElevationIcon = 3.dp

    /** Elevasi glow thumb TactileSwitch saat ON. */
    val ElevationThumb = 4.dp
}
