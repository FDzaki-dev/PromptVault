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
 * v8.27.0 — [SEBAGIAN DIREVISI v8.28.0, lihat di bawah] MAKSIMALKAN efek
 * timbul/cekung, permintaan eksplisit user (dilarang teknik glow/blooming).
 * Awalnya pakai 2 primitif: drop-shadow offset SATU sisi (`Surface`
 * dibungkus `Box` tambahan) + tint gradient fill.
 *
 * v8.28.0 — REGRESI NYATA dari v8.27.0 ditemukan user via screenshot: tab
 * "Tampilan" hilang total, beberapa kartu render kosong/blank. Root cause:
 * wrapper `Box` tambahan (utk taruh shadow-caster offset DI BELAKANG
 * `Surface` konten) membuat `modifier` caller (yang kadang berisi
 * `Modifier.weight(1f)`, mis. `SegmentedControl.kt`) nempel di `Surface`
 * yang jadi CUCU dari Row/Column, BUKAN anak langsung -- `RowScope.
 * weight()`/`BoxScope.align()` HANYA dikenali di anak LANGSUNG scope itu,
 * jadi weight diabaikan & layout Row rusak (distribusi lebar ambyar, 1
 * segment "hilang"). **Fix: wrapper `Box` + shadow-caster offset DIHAPUS
 * TOTAL**, balik ke SATU `Surface(shadowElevation=)` polos tanpa offset
 * custom -- PERSIS pola cabang Glass/Material3 (1 node, modifier caller
 * nempel langsung, weight/align otomatis benar lagi). Token
 * `ShadowOffset` (v8.27.0) DIHAPUS krn sudah tidak relevan (shadow
 * sekarang shadowElevation biasa, bukan offset manual). `ShadowElevation`/
 * `FillHighlightTint`/`FillShadeTint`/2 fungsi brush TETAP dipakai, TIDAK
 * berubah nilai -- fill gradient tint (poin 2) yang jadi sumber UTAMA
 * kesan "timbul/cekung" sekarang (100% aman, terjadi di DALAM `content()`,
 * tidak pernah menyentuh struktur node di luar `Surface`).
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
    /** Elevasi drop-shadow (nilai `Surface.shadowElevation` baku, TANPA
     * offset/wrapper custom sejak v8.28.0 -- lihat javadoc atas) --
     * dinaikkan drpd kartu biasa ([TactileTokens.TactileElevationCard])
     * supaya kartu terasa "mengambang" lebih jelas, ciri khas neumorphism
     * timbul. */
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

    /**
     * v8.28.1 — Border "keemasan timbul" dikembalikan sbg identitas unik
     * Neumorphism, diminta eksplisit user (sempat hilang tanpa sengaja
     * saat emergency fix layout v8.28.0). Pakai [Tertiary] (0xFFDABF81,
     * SUDAH ada di palette -- dipakai ikon "Panduan"/"Auto-scan" dll,
     * 0 hue baru diperkenalkan) -- dekoratif (garis tepi, bukan teks),
     * sama prinsipnya dgn [OutlineVariant]/border glass-edge Glassmorphism
     * -- TIDAK tunduk ambang 3:1 WCAG 1.4.11.
     */
    /**
     * v8.28.2 — Border diubah dari SOLID jadi GRADIENT diagonal, diminta
     * eksplisit user (lapor via screenshot: solid "kek border neon", maunya
     * "muncul dari sisi kiri atas membentang lalu fade out ke sisi kanan
     * bawah"). `Brush.linearGradient(colors)` TANPA `start`/`end` eksplisit
     * default `start=Offset.Zero` (kiri-atas) & `end=Offset.Infinite` --
     * Compose resolve `Offset.Infinite` jadi diagonal PERSIS ukuran elemen
     * saat digambar (bukan piksel tetap), otomatis benar lintas ukuran
     * kartu tanpa perlu `BoxWithConstraints` manual -- arah SAMA persis dgn
     * `fillHighlightBrush()`/`fillShadeBrush()` di atas (konsisten 1 arah
     * cahaya di seluruh gaya Neumorphism).
     */
    fun goldBorderBrush(): Brush = Brush.linearGradient(
        colors = listOf(Tertiary, Color.Transparent)
    )
    val GoldBorderWidth: Dp = 1.5.dp
}
