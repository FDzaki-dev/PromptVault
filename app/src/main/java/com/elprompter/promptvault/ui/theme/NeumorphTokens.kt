package com.elprompter.promptvault.ui.theme

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
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
     *
     * v8.28.3 — Warna diganti dari `Tertiary` (emas/tan) ke `IceCyan`
     * (0xFF7DD3E0), user pilih dari 5 opsi yang diusulkan Claude.
     *
     * v8.28.4 — User: "lebih cocok pakai tone warna yang nyaru" -- ganti
     * lagi ke `Platinum` (0xFFC8CDD6, opsi lain yang sama-sama diusulkan
     * Claude: "netral abu-terang, klasik neumorphism, MONOKROM bukan
     * warna aksen"). Beda filosofi dari `IceCyan`: bukan cari kontras
     * mencolok, tapi BLEND ke palet gelap netral app -- gradient diagonal
     * (v8.28.2) tetap 0 berubah.
     */
    val Platinum: Color = Color(0xFFC8CDD6)
    fun borderBrush(): Brush = Brush.linearGradient(
        colors = listOf(Platinum, Color.Transparent)
    )
    val BorderWidth: Dp = 1.5.dp

    /**
     * v8.30.0 — "Stacked Cards Effect", permintaan eksplisit user: "tanpa
     * utak-atik pencahayaan dan icon menu sama sekali". Ditelusuri ketat:
     * 1. **Pencahayaan 0 disentuh**: `ShadowElevation`/`FillHighlightTint`/
     *    `FillShadeTint`/`fillHighlightBrush()`/`fillShadeBrush()`/border
     *    di atas SEMUA 0 baris berubah. Lapis stacked-cards di bawah FLAT
     *    SOLID murni (`drawRoundRect(color=...)`, TANPA shadow/gradient/
     *    alpha-blend apa pun) -- mekanisme YANG SAMA SEKALI TERPISAH dari
     *    sistem pencahayaan existing, bukan modifikasi atasnya.
     * 2. **Icon menu 0 disentuh**: token & fungsi di bawah HANYA dipakai
     *    lewat parameter opt-in BARU `TactileSurface(stackedCards=...)`
     *    default `false` -- 0 dampak ke caller manapun yang tidak eksplisit
     *    mengaktifkan. Diaktifkan HANYA di `VaultCard.kt` (kartu utama).
     *    `GroupedListRow` (kotak ikon menu), `EmptyState` (lingkaran ikon),
     *    `TactileSwitch`, `SegmentedControl`, dll TIDAK disentuh -- tetap
     *    default `false`, 0 baris kode-nya berubah.
     *
     * Teknik: `Modifier.drawBehind{}` (BUKAN `Box` pembungkus baru --
     * lihat histori regresi v8.28.0 di PROJECT_STATE.md soal `Box`
     * pembungkus merusak `Modifier.weight()`/`align()` caller, TIDAK BOLEH
     * terulang) ditempel LANGSUNG ke modifier chain yang SAMA dgn `Surface`
     * -- gambar rounded-rect solid, offset kumulatif kanan-bawah (arah
     * SAMA dgn shadow existing, 1 sumber "kedalaman" konsisten). Karena
     * `drawBehind` ini berada SEBELUM `clip(shape)` internal `Surface`
     * dalam urutan chain (modifier pemanggil selalu di-prepend, `Surface`
     * menambahkan clip/background-nya SENDIRI setelahnya), bagian yang
     * ke-gambar DI DALAM bentuk kartu tertutup fill utama Surface (Lapis
     * 1, tidak berubah) -- HANYA bagian yang offset MELEBIHI tepi kartu
     * yang terlihat mengintip, PERSIS efek "tumpukan kartu" klasik, tanpa
     * clip Surface memotongnya (clip cuma berlaku ke draw SETELAHNYA di
     * chain, bukan retroaktif ke draw SEBELUMNYA -- prinsip yang sama
     * persis knp `Modifier.shadow()` bisa "bleed" di luar shape).
     */
    val StackedCardOffset: Dp = 9.dp

    /** Radius sudut lapis stacked-card -- SAMA dgn shape kartu utama
     * (`shapes.medium`/[TactileTokens.ControlCornerRadius]) supaya
     * terlihat sbg tumpukan kartu SEBENTUK, bukan kotak acak. */
    val StackedCardCornerRadius: Dp = TactileTokens.ControlCornerRadius

    /**
     * v8.30.0 — Warna reuse `SurfaceContainerLow`/`SurfaceContainerLowest`
     * (LEBIH GELAP dari `SurfaceContainer`, warna kartu itu sendiri).
     *
     * v8.30.1 — FIX: user lapor "mana efeknya" -- efek TIDAK terlihat sama
     * sekali di HP. Root cause: `SurfaceContainerLowest` (0x090A0C) MALAH
     * LEBIH GELAP dari `AppBackground` (0x0D0E12, latar app), dan
     * `SurfaceContainerLow` (0x111317) cuma beda ~4-5 unit per channel dari
     * situ -- bagian "mengintip" tenggelam total ke background gelap,
     * bukan bug teknik drawBehind/clip (itu SUDAH benar, diverifikasi
     * ulang), murni pilihan warna nyaris tidak kontras. Ganti ke
     * `SurfaceContainerHigh`/`SurfaceContainerHighest` -- keduanya LEBIH
     * TERANG dari `SurfaceContainer` (kartu) & jelas kontras vs
     * `AppBackground`, jadi lapis mengintip benar-benar kebaca sbg
     * "tumpukan kartu", bukan menghilang ke gelap. TETAP reuse token
     * existing (0 hue baru), TETAP FLAT solid (0 shadow/gradient/alpha).
     */
    val StackedCardColors: List<Color> = listOf(SurfaceContainerHigh, SurfaceContainerHighest)

    /** Modifier stacked-cards -- ditempel LANGSUNG ke chain yang sama dgn
     * `Surface` (bukan `Box` baru), lihat javadoc lengkap di atas.
     *
     * v8.30.2 — Poles: tiap lapis sekarang dapat GARIS TEPI tipis (flat
     * solid, `Stroke` -- BUKAN alpha/gradient, tetap patuh prinsip "0
     * pencahayaan") supaya kebaca sbg kartu TERPISAH yang berbatas jelas,
     * bukan cuma blok warna nempel. Warna garis = `StackedOutline`
     * (reuse `OutlineVariant`, sudah ada -- 0 hue baru), dekoratif murni
     * (bukan teks) sama prinsipnya dgn border utama.
     *
     * v8.30.3 — Poles lagi ("lebih wah"): tambah ROTASI kipas (`rotate()`
     * DrawScope, pivot pojok kiri-atas -- geometris murni, BUKAN
     * shadow/gradient, tetap patuh "0 pencahayaan") supaya lapis
     * belakang sedikit MEMUTAR, bukan cuma geser sejajar -- kesan
     * "kartu dikocok/terfan" klasik, jauh lebih hidup drpd offset lurus
     * doang. Offset dasar juga dinaikkan 7dp/14dp -> 9dp/18dp biar lebih
     * kebaca. Pivot di pojok yang SAMA dgn arah offset (kiri-atas ->
     * kartu memutar & bergeser konsisten ke kanan-bawah, 1 arah gerak).
     */
    val StackedFanAngle: Float = 3.5f
    val StackedOutline: Color = OutlineVariant
    val StackedOutlineWidth: Dp = 1.dp

    fun Modifier.stackedCards(): Modifier = this.drawBehind {
        val radius = CornerRadius(StackedCardCornerRadius.toPx())
        val outlinePx = StackedOutlineWidth.toPx()
        // Digambar dari PALING JAUH -> PALING DEKAT (offset besar dulu)
        // supaya lapis lebih dekat menimpa sebagian lapis lebih jauh --
        // efek "kartu terfan" yang benar, bukan tumpang tindih acak.
        for (i in StackedCardColors.indices.reversed()) {
            val layerIndex = i + 1
            val shift = StackedCardOffset.toPx() * layerIndex
            val topLeft = Offset(shift, shift)
            rotate(degrees = StackedFanAngle * layerIndex, pivot = Offset.Zero) {
                drawRoundRect(
                    color = StackedCardColors[i],
                    topLeft = topLeft,
                    size = size,
                    cornerRadius = radius
                )
                drawRoundRect(
                    color = StackedOutline,
                    topLeft = topLeft,
                    size = size,
                    cornerRadius = radius,
                    style = Stroke(width = outlinePx)
                )
            }
        }
    }
}
