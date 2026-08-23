package com.elprompter.promptvault.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * v8.23.2 — Neumorphism, gaya KEDUA di toggle "Tampilan". Teknik "soft UI":
 * fill OPAQUE (bukan translucent spt Glassmorphism di [GlassTokens])
 * sewarna surface aslinya.
 *
 * v8.25.3 — GANTI TEKNIK TOTAL (ke-3 kalinya, root cause finalnya
 * ditemukan sekarang): user laporkan v8.25.2 (fill tint linear-gradient
 * DI DALAM bentuk, dibatasi alpha 0.16 demi WCAG) hasilnya "masih flat
 * macam tema lain, gak ada unsur cekung+timbul yang realistis" -- BENAR,
 * krn tint yang nutup SELURUH badan kartu (termasuk area teks) TERPAKSA
 * dibatasi alpha sangat rendah (0.16 = batas WCAG mutlak utk teks di
 * ATASnya) supaya tidak melanggar kontras -- alpha serendah itu, di
 * bidang seluas kartu, SECARA VISUAL nyaris tidak kebaca sbg "cekung/
 * timbul" sama sekali, apalagi di layar kecil + kompresi screenshot.
 * **Root cause SEBENARNYA**: neumorphism asli (referensi user) TIDAK
 * PERNAH mewarnai SELURUH badan elemen -- kesan timbulnya dari BINGKAI/
 * TEPI (rim light+shadow) yang SEMPIT, bukan wash penuh. Area tepi
 * sesempit ini TIDAK PERNAH ditempati teks (semua card/row pemanggil
 * `TactileSurface` punya padding konten >= 16dp dari tepi, diverifikasi
 * di seluruh call site) -- artinya BEBAS dari batas WCAG teks sama
 * sekali (WCAG cuma berlaku utk teks/ikon, bukan garis dekoratif), alpha
 * bisa jauh lebih tinggi & tetap 100% aman.
 * **Fix**: fill tint linear-gradient DI DALAM (v8.24.0-v8.25.2) DIHAPUS
 * TOTAL, diganti `Modifier.border` dgn `Brush` diagonal (terang di
 * kiri-atas -> gelap di kanan-bawah) SEBAGAI GARIS TEPI SAJA (lihat
 * `BevelWidth` -- setipis 2dp), bukan fill. Ini BUKAN "glow" (tidak ada
 * blur/scale/offset keluar bentuk spt v8.25.0 yang bikin "uncanny", TIDAK
 * ada fill wash spt v8.24.0-v8.25.2 yang "flat") -- murni garis bingkai
 * dual-tone TEPAT DI TEPI bentuk, teknik neumorphism/soft-UI yang paling
 * umum & 100% cocok definisi "cekung+timbul" (rim highlight = cembung,
 * rim shadow = sisi berlawanan).
 *
 * 3 syarat user (TETAP berlaku identik):
 * 1. **"murni, tanpa keluar dari gaya desain itu sendiri"** -- garis
 *    bingkai TEPAT di tepi shape (via `Modifier.border`, ikut clip ke
 *    `shape` otomatis), TIDAK ada elemen yang menonjol/blur/meleber keluar
 *    batas bentuk sama sekali (beda total dari "glow" v8.25.0).
 * 2. **"calm, bukan warm"** -- hitam/putih alpha, 0 hue kustom.
 * 3. **"100% WCAG"** -- garis bingkai TIDAK PERNAH ditempati teks (lihat
 *    root cause di atas), jadi 0 kewajiban rasio kontras teks WCAG sama
 *    sekali utk token warna di file ini -- beda dari v8.24.0-v8.25.2 yang
 *    justru DIBATASI oleh itu.
 */
object NeumorphTokens {

    /** Tebal garis bingkai (bevel) -- cukup tipis utk kesan "presisi/
     * halus" khas soft-UI, cukup tebal utk KELIHATAN di layar HP normal
     * (referensi user: garis tepi jelas kebaca, bukan samar 1px). */
    val BevelWidth: Dp = 2.dp

    /** Warna terang bingkai (sisi cembung/menonjol -- kiri-atas saat
     * `raised`, kanan-bawah saat `recessed`). Alpha JAUH lebih tinggi drpd
     * era fill (0.16) krn TIDAK PERNAH menimpa teks -- lihat root cause di
     * atas, 0 kewajiban WCAG teks di sini. */
    val BevelLightColor: Color = Color.White.copy(alpha = 0.65f)

    /** Warna gelap bingkai (sisi cekung -- kanan-bawah saat `raised`,
     * kiri-atas saat `recessed`). Sama seperti [BevelLightColor], 0 batas
     * WCAG (bahkan kalaupun ada teks, gelap tidak pernah mengurangi
     * kontras teks terang di atas surface gelap). */
    val BevelDarkColor: Color = Color.Black.copy(alpha = 0.65f)

    /** Brush garis bingkai, diagonal (default 45°, Compose otomatis
     * menyesuaikan ke bounds shape) dari [lightCorner] ke [darkCorner].
     * Dipanggil 2 arah beda dari `TactileSurface.kt` (`raised` vs
     * `recessed`) via parameter, bukan 2 fungsi terpisah -- 1 sumber
     * kebenaran teknik gradiennya. */
    fun bevelBrush(lightCorner: Color, darkCorner: Color): Brush = Brush.linearGradient(
        colors = listOf(lightCorner, darkCorner)
    )
}
