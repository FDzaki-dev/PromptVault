package com.elprompter.promptvault.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * v8.23.2 — Neumorphism, gaya KEDUA di toggle "Tampilan". [Revert darurat
 * v8.26.0]: 4 percobaan teknik shadow-ganda custom (drawBehind/nativeCanvas/
 * setShadowLayer/radial-gradient blob) SEMUA gagal -- 2 lemah tak
 * terlihat, 1 bikin SELURUH UI washed-out. Direvert ke `Surface` M3 baku +
 * border polos (0 depth custom) demi stabil.
 *
 * v8.27.0 — MAKSIMALKAN efek timbul/cekung, permintaan eksplisit user
 * (dgn 1 batasan KERAS: **dilarang teknik glow/blooming/sejenisnya** --
 * radial-gradient blob v8.25.x TERMASUK yang dilarang ini, itu SEBAGIAN
 * alasan kenapa terasa "glow"/pudar, bukan shadow tegas). Teknik kali ini
 * SENGAJA hanya menggabung 2 primitif yang SUDAH terbukti stabil di
 * codebase ini, TIDAK ADA API/teknik baru sama sekali:
 * 1. **Drop-shadow asli, SATU sisi (gelap, kanan-bawah)**: `Surface(
 *    shadowElevation=...)` tanpa `ambientColor`/`spotColor` custom --
 *    warna default bawaan Android (PERSIS mekanisme yang sudah dipakai
 *    aman di cabang MATERIAL3 & sebagai fallback Neumorphism v8.26.0,
 *    0 laporan masalah). Shadow ini MENGIKUTI BENTUK kartu (rounded-rect),
 *    BUKAN blob lingkaran -- beda mendasar dari teknik glow yang dilarang.
 *    Sisi TERANG (kiri-atas) SENGAJA TIDAK dibuat via shadow custom lagi
 *    (itu akar masalah v8.23.2-v8.25.4) -- digantikan poin 2.
 * 2. **Tint gradient DI DALAM fill** (bukan di luar bentuk, jadi bukan
 *    glow): terang kiri-atas (skrg pakai [Primary] app -- lihat alasan
 *    "tone" di bawah, bukan cuma putih polos) + gelap kanan-bawah, PERSIS
 *    teknik `GlassTokens.highlightBrush()` (sheen) yang SUDAH live &
 *    terbukti stabil 100% di cabang Glassmorphism -- tinggal dipakai lagi
 *    di sini, BUKAN teknik baru.
 * Border [BorderWidth]/[BorderColor] (v8.26.0) DIHAPUS -- neumorphism
 * OTENTIK TIDAK PERNAH punya garis tepi (bentuknya didefinisikan MURNI
 * oleh shadow+fill, bukan outline) -- mempertahankan border sekarang
 * malah kontradiktif dgn tujuan "maksimalkan kesan timbul/cekung asli".
 *
 * **"Tone warna kek ada yang kurang"** (keluhan user) -- fix: tint terang
 * di poin 2 di atas PAKAI [Primary] (biru-cool brand app, `Color(0xFF98AEE1)`,
 * SUDAH ada, dipakai tombol "Scan Sekarang" dll), BUKAN putih generik --
 * kartu jadi kerasa ikatan warna sama identitas app, bukan abu-abu netral
 * kosong. TETAP calm/cool (warna itu SENDIRI sudah calm, dipakai ulang
 * apa adanya, 0 hue baru diperkenalkan) -- bukan "warm" sama sekali.
 *
 * WCAG worst-case (`TextSecondary`, kontras terkecil) diblend titik PUNCAK
 * tint terang di tier surface paling terang (metodologi sama persis
 * seluruh `Color.kt`): alpha 0.20 -> composited (66,74,91) -> 5.13:1 (AA,
 * ambang 4.5:1, margin disisakan -- 0.22 sudah 4.94:1, masih ok tapi lebih
 * mepet). Sisi gelap 0 batas atas WCAG (menggelapkan bg cuma menaikkan
 * kontras teks terang). Drop-shadow poin 1 100% di area KOSONG luar
 * kartu, 0 relevansi WCAG teks.
 */
object NeumorphTokens {
    /** Jarak geser drop-shadow gelap dari posisi asli kartu. */
    val ShadowOffset: Dp = 6.dp

    /** Elevasi drop-shadow gelap (nilai `Surface.shadowElevation` baku,
     * BUKAN parameter custom) -- dinaikkan drpd kartu biasa
     * ([TactileTokens.TactileElevationCard]) supaya kartu terasa
     * "mengambang" lebih jelas, ciri khas neumorphism timbul. */
    val ShadowElevation: Dp = 14.dp

    /** Tint fill terang, kiri-atas -- basis [Primary] app (bukan putih
     * polos, lihat alasan "tone" di javadoc atas). */
    val FillHighlightTint: Color = Primary.copy(alpha = 0.20f)

    /** Tint fill gelap, kanan-bawah -- netral (shadow gelap tidak perlu
     * ikatan warna brand, cukup hitam biasa). */
    val FillShadeTint: Color = Color.Black.copy(alpha = 0.24f)

    /** Brush fill terang (kiri-atas -> transparan). */
    fun fillHighlightBrush(): Brush = Brush.linearGradient(
        colors = listOf(FillHighlightTint, Color.Transparent)
    )

    /** Brush fill gelap (transparan -> kanan-bawah). */
    fun fillShadeBrush(): Brush = Brush.linearGradient(
        colors = listOf(Color.Transparent, FillShadeTint)
    )
}
