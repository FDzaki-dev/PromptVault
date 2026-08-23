package com.elprompter.promptvault.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * v8.23.2 — Neumorphism, gaya KEDUA di toggle "Tampilan" (v8.23.0
 * scaffold, sekarang live). Teknik klasik "soft UI": fill OPAQUE (bukan
 * translucent spt Glassmorphism di [GlassTokens]) sewarna surface aslinya,
 * kedalaman murni dari SEPASANG shadow terarah -- terang di
 * kiri-atas (seolah cahaya datang dari sana), gelap di kanan-bawah.
 *
 * v8.25.0 — ROOT CAUSE FIX: user bandingkan langsung dgn referensi desain
 * Neumorphism asli (screenshot) -- app "gak terasa sama sekali" efek
 * timbul/cekung, walau v8.23.6 sudah menaikkan alpha & v8.24.0 sudah
 * menambah fill 3-lapis. Root cause SEBENARNYA: `Modifier.shadow(...,
 * ambientColor=, spotColor=)` (teknik v8.23.2 lama) memakai RENDERER
 * BAWAAN Android View (elevation/ambient+spot light simulation) --
 * renderer ini didesain utk shadow GELAP OPAK standar Material, BUKAN utk
 * "glow" warna TERANG custom. Hasilnya: sisi terang nyaris tidak pernah
 * benar-benar terlihat di device fisik APAPUN besar alpha-nya (bukan
 * masalah alpha kurang, tapi mekanismenya sendiri tidak didesain utk
 * kasus ini) -- PERSIS kenapa fix alpha v8.23.6 tetap tidak cukup.
 * **GANTI TOTAL teknik**: shadow bawaan Android View DIBUANG, diganti
 * `Brush.radialGradient` murni Compose (blob lembut, TIDAK bergantung
 * renderer shadow platform sama sekali -- selalu render identik di semua
 * API/GPU, ini BUKAN native-canvas/BlurMaskFilter, murni Brush biasa yg
 * SUDAH dipakai aman di seluruh app, mis. `GlassTokens.highlightBrush()`).
 * Blob digambar LEBIH BESAR dari kartu (`GlowScale`) + digeser
 * (`GlowOffset`) shg area "meleber" di luar kartu jauh lebih luas & jelas
 * kebaca -- bukan cuma beberapa dp tipis di tepi seperti teknik lama.
 *
 * 3 syarat user (sama seperti [GlassTokens], ditelusuri utk gaya ini juga,
 * TETAP berlaku persis sama di teknik baru v8.25.0):
 * 1. **"murni, tanpa keluar dari gaya desain itu sendiri"** -- HANYA
 *    shadow ganda (skrg via gradient) + fill opaque yang jadi treatment
 *    visual, TIDAK ada border/translucency/sheen (itu ciri Glassmorphism,
 *    BUKAN Neumorphism -- mencampur keduanya justru keluar dari "murni").
 * 2. **"calm, bukan warm"** -- glow terang & gelap SAMA-SAMA netral
 *    (hitam/putih alpha rendah, bukan hue kustom), warna fill dasar tetap
 *    dari [Color.kt] (H222 biru, TIDAK disentuh). Tidak ada penambahan
 *    hue baru sama sekali di file ini.
 * 3. **"100% WCAG"** -- fill 100% OPAQUE (alpha 1.0, BEDA dari
 *    Glassmorphism yang translucent) -- kontras teks di atasnya PERSIS
 *    angka worst-case yang SUDAH diverifikasi manual di [Color.kt]
 *    (11.64:1/7.54:1 dst), 0 ketidakpastian tambahan dari blending. Glow
 *    ganda 100% dekoratif, digambar DI LUAR bentuk konten (area kosong
 *    di sekitar kartu, bukan di atas teks) -- tidak relevan dinilai WCAG
 *    teks/1.4.11 sama sekali, sama seperti teknik lama.
 */
object NeumorphTokens {
    /** [v8.25.0] Seberapa besar blob glow dibanding ukuran kartu asli --
     * kunci "kerasa" -- blob HARUS jauh lebih besar dari kartu supaya
     * bagian yang meleber di luar tepi kartu cukup luas utk kebaca sbg
     * "glow lembut", bukan garis tipis di pinggir. */
    const val GlowScale: Float = 1.7f

    /** Pergeseran diagonal blob glow dari pusat kartu (kedua arah,
     * simetris) -- dinaikkan jauh dari `ShadowOffset` lama (7dp) krn
     * teknik gradient butuh jarak lebih supaya sisi terang & gelap tidak
     * saling tumpang tindih di tengah & saling menetralkan. */
    val GlowOffset: Dp = 16.dp

    /** Glow terang (kiri-atas) -- puncak alpha di titik pusat blob,
     * putih netral. Alpha lebih tinggi dari era `Modifier.shadow` lama
     * (0.35f) krn falloff radial gradient secara alami mengencerkan alpha
     * makin ke tepi -- titik pusat perlu lebih kuat drpd shadow flat lama
     * supaya hasil AKHIR yang terlihat (bukan cuma angka alpha) setara
     * atau lebih kuat. */
    val LightGlowColor: Color = Color.White.copy(alpha = 0.50f)

    /** Glow gelap (kanan-bawah) -- alasan sama seperti [LightGlowColor],
     * rasio kekuatan gelap:terang dipertahankan mirip era lama (lebih
     * kuat di sisi gelap, latar app sudah gelap total). */
    val DarkGlowColor: Color = Color.Black.copy(alpha = 0.90f)

    /** Brush blob glow, radial dgn 3 stop (bukan cuma 2) supaya falloff
     * lembut/gradual -- 2-stop polos terasa "cutoff" mendadak di tepi,
     * bukan lembut khas neumorphism. `peakColor` = [LightGlowColor] atau
     * [DarkGlowColor]/kebalikannya (utk state `recessed`, arah dibalik di
     * `TactileSurface.kt`, token warna di sini tidak berubah). */
    fun glowBrush(peakColor: Color): Brush = Brush.radialGradient(
        colorStops = arrayOf(
            0.0f to peakColor,
            0.55f to peakColor.copy(alpha = peakColor.alpha * 0.35f),
            1.0f to Color.Transparent
        )
    )

    // ---- [v8.24.0, TIDAK disentuh sama sekali di batch v8.25.0 ini] ----
    // "Background kartu dibikin berlapis 3 supaya muncul desain 3D khas
    // Neumorphism asli" -- fill Surface juga "puffy" (bukan cuma shadow di
    // LUAR bentuk) via 2 brush gradient dekoratif DI ATAS fill dasar:
    //   Lapis 1: fill solid dasar (punya `TactileSurface`, tidak berubah)
    //   Lapis 2: gradient terang, sudut kiri-atas (di bawah)
    //   Lapis 3: gradient gelap, sudut kanan-bawah (di bawah)
    // WCAG worst-case (`TextSecondary` diblend titik puncak gradient
    // terang di tier surface paling terang): alpha 0.14 -> 4.84:1 (AA,
    // margin sengaja disisakan -- 0.16 sudah mepet 4.53:1). Sisi gelap 0
    // batas atas WCAG (menggelapkan bg cuma menaikkan kontras teks
    // terang). Detail lengkap perhitungan: lihat riwayat PROJECT_STATE.md
    // v8.24.0 -- TIDAK diulang di sini supaya token INI (v8.25.0, soal
    // glow LUAR) tidak tercampur dgn token LAMA (soal fill DALAM) yang
    // memang sengaja tidak diubah.

    /** Tint gradient terang di FILL (bukan glow luar) -- sudut kiri-atas. */
    val SurfaceHighlightTint: Color = Color.White.copy(alpha = 0.14f)

    /** Tint gradient gelap di FILL (bukan glow luar) -- sudut kanan-bawah. */
    val SurfaceShadeTint: Color = Color.Black.copy(alpha = 0.22f)

    /** Brush fill lapis ke-2 (terang, kiri-atas -> transparan). */
    fun surfaceHighlightBrush(): Brush = Brush.linearGradient(
        colors = listOf(SurfaceHighlightTint, Color.Transparent)
    )

    /** Brush fill lapis ke-3 (transparan -> gelap, kanan-bawah). */
    fun surfaceShadeBrush(): Brush = Brush.linearGradient(
        colors = listOf(Color.Transparent, SurfaceShadeTint)
    )
}
