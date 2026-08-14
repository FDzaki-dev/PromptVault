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
 *
 * v5.0.0 — Redesign Glassmorphism -> Neumorphism. Token `ElevationCard` /
 * `ElevationCta` / `ElevationCtaPressed` / `ElevationIcon` / `ElevationThumb`
 * DIHAPUS (bukan cuma tidak dipakai -- diganti total, dead code dibersihkan
 * di batch yang sama supaya tidak menumpuk seperti pelajaran audit v2.16.0)
 * dan digantikan set token `Neu*` di bawah: neumorphism butuh SEPASANG
 * nilai per komponen (elevasi UNTUK shadow ganda + offset jarak kedua
 * shadow-caster dari posisi asli), bukan satu nilai elevasi tunggal seperti
 * sistem glassmorphism lama. `ElevationRaised`/`ElevationPressed` di bawah
 * TETAP ADA TIDAK DIUBAH -- masih dipakai `PressScale.kt` (`tactilePress()`,
 * belum dipanggil di mana pun saat ini tapi disengaja dipertahankan sejak
 * v2.14.1 utk kontrol lain di masa depan), di luar scope batch redesign ini.
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

    // ---- v5.0.0: Neumorphism -- shadow ganda (dual-shadow) ----
    // Tiap komponen timbul (raised) butuh 2 nilai: `elevation` (dalam ke
    // dua shadow-caster, lihat `NeumorphicSurface` di `Neumorphic.kt`) dan
    // `offset` (jarak geser tiap shadow-caster dari posisi permukaan asli --
    // MAKIN BESAR offset, makin jelas kesan permukaan itu "melayang" bukan
    // cuma buram di tepi). Rasio offset:elevation dijaga ~1:2 di semua
    // komponen supaya kesan cahaya konsisten satu app, bukan tiap komponen
    // beda "kekuatan lampu".

    /** VaultCard -- permukaan neumorphic paling besar/dominan di app. */
    val NeuElevationCard = 10.dp
    val NeuOffsetCard = 5.dp

    /** CTA "Scan Sekarang" -- titik fokus utama, dual-shadow paling kuat. */
    val NeuElevationCta = 12.dp
    val NeuOffsetCta = 6.dp

    /** Kotak ikon GroupedListRow & lingkaran ikon EmptyState -- kontrol kecil. */
    val NeuElevationControl = 6.dp
    val NeuOffsetControl = 3.dp

    /** Thumb TactileSwitch saat ON -- kecil, dual-shadow paling halus. */
    val NeuElevationThumb = 5.dp
    val NeuOffsetThumb = 2.dp

    /** Alpha overlay scrim diagonal utk simulasi permukaan tenggelam (pressed/
     * inset) -- sisi gelap (mendekati sisi cahaya datang, kiri-atas, seolah
     * memblokir cahaya) & sisi terang (kanan-bawah, seolah pantulan tipis di
     * dasar cekungan). Dipakai `NeumorphicSurface(pressed = true)` & track
     * OFF `TactileSwitch`. Basis warna TETAP Color.Black/Color.White netral
     * (bukan hue brand baru), murni token alpha.
     */
    const val NeuPressedDarkAlpha = 0.30f
    const val NeuPressedLightAlpha = 0.05f
}
