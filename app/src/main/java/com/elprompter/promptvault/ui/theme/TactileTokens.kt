package com.elprompter.promptvault.ui.theme

import androidx.compose.ui.unit.dp

/**
 * v3.0.0 — Konstanta tactile terpusat (satu titik, bukan duplikasi di tiap
 * layar). `ElevationRaised`/`ElevationPressed`/`PressScale`/
 * `PressAnimationMillis`/`ControlCornerRadius` TIDAK diubah -- masih dipakai
 * `PressScale.kt` (`pressScale()`/`tactilePress()`), lepas dari sistem
 * shadow permukaan (Neu*/Glass*).
 *
 * v7.0.0 — Neumorphism -> Glassmorphism: token `Neu*` (elevasi+offset shadow
 * ganda, `NeuPressedDarkAlpha`/`NeuPressedLightAlpha`) DIHAPUS TOTAL bersama
 * `Neumorphic.kt` (permintaan eksplisit user, "ultra buggy"). Digantikan set
 * `Glass*` di bawah -- SATU nilai elevasi standar per komponen (dipakai
 * langsung lewat `Modifier.shadow` biasa di `GlassPanel.kt`, TIDAK butuh
 * pasangan offset seperti sistem lama), jauh lebih sederhana & tanpa kelas
 * bug shadow-caster/baseColor-matching yang tercatat di PROJECT_STATE.md
 * (Insiden #3, #8, #9, #10).
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

    // ---- v7.0.0: Glassmorphism -- elevasi tunggal per komponen ----
    /** VaultCard -- permukaan kaca paling besar/dominan di app. */
    val GlassElevationCard = 6.dp

    /** CTA "Scan Sekarang" -- titik fokus utama. */
    val GlassElevationCta = 10.dp

    /** Kotak ikon GroupedListRow & lingkaran ikon EmptyState -- kontrol kecil. */
    val GlassElevationControl = 3.dp

    /** Thumb TactileSwitch saat ON. */
    val GlassElevationThumb = 2.dp
}
