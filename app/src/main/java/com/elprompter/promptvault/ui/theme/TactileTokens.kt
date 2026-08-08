package com.elprompter.promptvault.ui.theme

import androidx.compose.ui.unit.dp

/**
 * v3.0.0 — Konstanta tactile terpusat (bab 12 spesifikasi: "Do not duplicate
 * tactile constants throughout screen files"). Satu arah cahaya simulasi
 * untuk seluruh app: kiri-atas terang -> kanan-bawah gelap (bab 3).
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
}
