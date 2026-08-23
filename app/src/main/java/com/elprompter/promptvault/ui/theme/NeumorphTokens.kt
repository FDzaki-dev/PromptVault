package com.elprompter.promptvault.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * v8.23.2 — Neumorphism, gaya KEDUA di toggle "Tampilan". Teknik "soft UI":
 * fill OPAQUE (bukan translucent spt Glassmorphism di [GlassTokens])
 * sewarna surface aslinya, kedalaman murni dari tint gradient diagonal
 * LANGSUNG DI ATAS fill (bukan shadow/glow terpisah) -- terang di
 * kiri-atas, gelap di kanan-bawah, PERSIS di dalam bentuk kartu.
 *
 * v8.25.2 — REVERT total teknik "glow blob DI LUAR kartu" (v8.25.0,
 * `Brush.radialGradient` di-scale 1.7x + offset 16dp). User bandingkan
 * screenshot app vs referensi desain asli (soft-UI neumorphism genuine):
 * blob radial besar yang meleber jauh di luar tepi kartu terlihat seperti
 * halo/glow acak ("uncanny"), BUKAN kesan cekung/timbul yang genuine.
 * Referensi asli TIDAK PERNAH punya efek "meleber keluar bentuk" -- kesan
 * timbul/cekungnya 100% dari tint gradient TEPAT DI DALAM batas elemen.
 * **Balik ke default**: SEMUA token glow-luar (`GlowScale`/`GlowOffset`/
 * `LightGlowColor`/`DarkGlowColor`/`glowBrush()`) DIHAPUS -- tidak lagi
 * dipakai `TactileSurface.kt` sama sekali. Fokus SEKARANG murni pada tint
 * gradient DALAM (di bawah) -- "cekung+timbul only" sesuai instruksi user.
 * Diperkuat 2 cara SEKALIGUS drpd cuma naikkan alpha mentah-mentah:
 * (a) alpha puncak highlight dinaikkan ke batas AMAN MAKSIMAL yang SUDAH
 * pernah diverifikasi presis di riwayat v8.23.6 (0.16 -> 4.53:1, TIDAK
 * dinaikkan lebih -- dihitung ulang barusan, 0.18 turun ke 4.25:1, DI
 * BAWAH AA 4.5:1, jadi 0.16 tetap batas atas mutlak), (b) area gradient
 * DILEBARKAN (stop transparan digeser lebih jauh) supaya tint kelihatan
 * menutupi bidang lebih luas -- ini menaikkan kesan "timbul" TANPA
 * menaikkan alpha PUNCAK sama sekali, jadi WCAG worst-case TIDAK berubah.
 *
 * 3 syarat user (sama seperti [GlassTokens], TETAP berlaku identik):
 * 1. **"murni, tanpa keluar dari gaya desain itu sendiri"** -- HANYA tint
 *    gradient (fill) yang jadi treatment visual, TIDAK ada border/
 *    translucency/sheen/glow di luar bentuk (itu ciri Glassmorphism,
 *    BUKAN Neumorphism -- dan persis apa yang bikin v8.25.0 "uncanny").
 * 2. **"calm, bukan warm"** -- tint terang & gelap SAMA-SAMA netral
 *    (hitam/putih alpha, bukan hue kustom), warna fill dasar tetap dari
 *    [Color.kt] (H222 biru, TIDAK disentuh).
 * 3. **"100% WCAG"** -- fill 100% OPAQUE (alpha 1.0), kontras teks di
 *    atasnya diverifikasi PRESIS via formula WCAG relative-luminance
 *    (bukan estimasi) tiap kali alpha tint berubah, lihat angka di bawah.
 */
object NeumorphTokens {

    /** Tint gradient terang di FILL -- sudut kiri-atas.
     * (v8.25.2) Dinaikkan dari 0.14 -> 0.16 -- batas atas MUTLAK yang
     * WCAG-aman. Dihitung PRESIS (formula relative-luminance WCAG,
     * bg [SurfaceContainerHighest] `0xFF2D3139` diblend [Color.White] pada
     * alpha ini, dibandingkan [TextSecondary] `0xFFC1C5CD` -- worst-case
     * teks on-surface, base 7.54:1 tanpa blend): **4.53:1**, masih lulus
     * AA normal text (>=4.5:1) tapi MEPET -- 0.18 sudah turun ke 4.25:1
     * (GAGAL AA), jadi 0.16 TIDAK BOLEH dinaikkan lagi ke depan tanpa
     * hitung ulang presisi yang sama.
     */
    val SurfaceHighlightTint: Color = Color.White.copy(alpha = 0.16f)

    /** Tint gradient gelap di FILL -- sudut kanan-bawah. Menggelapkan bg
     * TIDAK PERNAH mengurangi kontras teks terang (base [Color.kt] semua
     * teks lebih terang dari surface) -- 0 batas atas WCAG, aman dinaikkan
     * bebas. (v8.25.2) 0.22 -> 0.42 (selaras kenaikan sisi terang, rasio
     * gelap:terang dipertahankan lebih kuat di sisi gelap seperti riwayat
     * lama -- latar app sudah gelap total, sisi gelap boleh lebih pekat). */
    val SurfaceShadeTint: Color = Color.Black.copy(alpha = 0.42f)

    /** Brush fill lapis terang (kiri-atas -> transparan). (v8.25.2) Stop
     * kedua dilebarkan dari 1.0 (nutup seluruh bidang, gradient linear
     * biasa) ke 0.65 -- bidang yang KELIHATAN tertint jadi lebih luas
     * drpd 2-stop polos, TANPA menaikkan alpha puncak [SurfaceHighlightTint]
     * (0.0 di titik awal tetap sama) -- cara menaikkan kesan "timbul" yang
     * TIDAK mengubah angka WCAG worst-case di atas sama sekali. */
    fun surfaceHighlightBrush(): Brush = Brush.linearGradient(
        colorStops = arrayOf(
            0.0f to SurfaceHighlightTint,
            0.65f to Color.Transparent
        )
    )

    /** Brush fill lapis gelap (transparan -> kanan-bawah). Simetris
     * dgn [surfaceHighlightBrush] -- 0.35 supaya kedua gradient
     * bertemu proporsional di tengah tanpa tumpang tindih. */
    fun surfaceShadeBrush(): Brush = Brush.linearGradient(
        colorStops = arrayOf(
            0.35f to Color.Transparent,
            1.0f to SurfaceShadeTint
        )
    )
}
