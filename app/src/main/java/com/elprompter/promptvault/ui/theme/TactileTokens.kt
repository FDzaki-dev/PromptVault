package com.elprompter.promptvault.ui.theme

import androidx.compose.ui.unit.dp

/**
 * v8.0.0 — "Premium Tactile" REBASE ke elevasi M3 murni: token `Glass*`
 * (elevasi custom 2/3/6/10dp, dipasangkan warna shadow kustom
 * [ambientColor]/[spotColor] non-standar di `GlassPanel.kt` lama) DIHAPUS,
 * diganti `Tactile*` di bawah -- nilainya PERSIS `Elevation Level` baku M3
 * (1dp/3dp/6dp = Level1/Level2/Level3 spec resmi; 6dp KHUSUS juga elevasi
 * default FAB M3 asli, dipakai utk CTA supaya benar-benar "M3 murni", bukan
 * angka custom). Primitif konsumen sekarang `TactileSurface.kt`
 * (menggantikan `GlassPanel.kt`) -- pakai `Surface(tonalElevation=,
 * shadowElevation=)` BAKU Compose M3 (efek "surface tint naik seiring
 * elevasi" otomatis dari M3 sendiri, bukan overlay gradient/border kustom)
 * -- inilah wujud "Premium Tactile" versi M3 murni: kedalaman & feedback
 * tekan (press->scale & elevasi turun, TIDAK diubah dari v7.x, sudah
 * teruji) tetap ada, tapi lewat mekanisme M3 baku, bukan primitif custom.
 *
 * `PressScale`/`PressAnimationMillis`/`ControlCornerRadius`/
 * `ElevationRaised`/`ElevationPressed` TIDAK diubah -- masih dipakai
 * `PressScale.kt`, di luar cakupan permintaan "tema/warna" sesi ini
 * (motion sudah tuned & terdokumentasi, bukan bagian dari "base warna").
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

    /** Radius bevel standar untuk kontrol tactile (tombol, chip ikon, dll) -- == shapes.medium M3. */
    val ControlCornerRadius = 12.dp

    // ---- v8.0.0: Elevasi M3 murni (tonalElevation == shadowElevation) ----
    /** VaultCard -- permukaan paling besar/dominan di app. M3 Elevation Level2. */
    val TactileElevationCard = 3.dp

    /** CTA "Scan Sekarang" -- titik fokus utama. == elevasi default FAB M3 baku. */
    val TactileElevationCta = 6.dp

    /** Kotak ikon GroupedListRow & lingkaran ikon EmptyState. M3 Elevation Level1. */
    val TactileElevationControl = 1.dp

    /** Thumb TactileSwitch saat ON. M3 Elevation Level1. */
    val TactileElevationThumb = 1.dp
}
