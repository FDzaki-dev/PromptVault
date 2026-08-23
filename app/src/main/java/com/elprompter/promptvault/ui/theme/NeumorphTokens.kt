package com.elprompter.promptvault.ui.theme

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
 * 3 syarat user (sama seperti [GlassTokens], ditelusuri utk gaya ini juga):
 * 1. **"murni, tanpa keluar dari gaya desain itu sendiri"** -- HANYA
 *    shadow ganda + fill opaque yang jadi treatment visual, TIDAK ada
 *    border/translucency/sheen (itu ciri Glassmorphism, BUKAN
 *    Neumorphism -- mencampur keduanya justru keluar dari "murni").
 * 2. **"calm, bukan warm"** -- shadow terang & gelap SAMA-SAMA netral
 *    (hitam/putih alpha rendah, bukan hue kustom), warna fill dasar
 *    tetap dari [Color.kt] (H222 biru, TIDAK disentuh). Tidak ada
 *    penambahan hue baru sama sekali di file ini.
 * 3. **"100% WCAG"** -- fill 100% OPAQUE (alpha 1.0, BEDA dari
 *    Glassmorphism yang translucent) -- kontras teks di atasnya PERSIS
 *    angka worst-case yang SUDAH diverifikasi manual di [Color.kt]
 *    (11.64:1/7.54:1 dst), 0 ketidakpastian tambahan dari blending.
 *    Shadow ganda 100% dekoratif (di LUAR shape konten, bukan di atas
 *    teks) -- tidak relevan dinilai WCAG teks/1.4.11 sama sekali.
 */
object NeumorphTokens {
    /** Jarak offset shadow dari tepi permukaan (kedua arah, simetris). */
    val ShadowOffset: Dp = 7.dp

    /** Radius blur shadow -- lebih besar dari offset supaya terasa "lembut" (ciri khas neumorphism, bukan shadow tegas M3 biasa). */
    val ShadowBlurRadius: Dp = 18.dp

    // [fix bug nyata, 2026-08-23 -- laporan user "Neumorphism sama Material3
    // gak ada bedanya sama sekali"] ROOT CAUSE: shadow SECARA TEKNIS memang
    // render (kode sudah benar, dua Box beroffset + Modifier.shadow), TAPI
    // alpha lama (putih 0.06f / hitam 0.45f) jauh terlalu tipis utk terlihat
    // -- yang kelihatan cuma "bleed" tipis di LUAR bentuk konten (fill di
    // atasnya menutupi sisanya, lihat TactileSurface.kt), jadi di alpha
    // serendah itu efeknya nyaris 0% kebaca di layar HP nyata, apalagi warna
    // terang di atas latar app yang sudah gelap total. Dinaikkan jauh lebih
    // kuat di sini -- BUKAN restrukturisasi teknik (offset+shadow tetap sama
    // persis), murni kontras dinaikkan supaya "efek timbul" benar2 terlihat.

    /** Shadow terang (kiri-atas) -- putih alpha rendah, netral, bukan warm. */
    val LightShadowColor: Color = Color.White.copy(alpha = 0.35f)

    /** Shadow gelap (kanan-bawah) -- hitam alpha lebih tinggi (latar app sudah gelap, perlu sedikit lebih kuat drpd shadow terang supaya kedua arah sama-sama kebaca). */
    val DarkShadowColor: Color = Color.Black.copy(alpha = 0.70f)
}
