package com.elprompter.promptvault.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.elprompter.promptvault.data.ThemeStyleOption
import com.elprompter.promptvault.ui.theme.CupertinoTokens
import com.elprompter.promptvault.ui.theme.GlassTokens
import com.elprompter.promptvault.ui.theme.Material3Tokens
import com.elprompter.promptvault.ui.theme.NeumorphTokens
import com.elprompter.promptvault.ui.theme.TactileTokens
import com.elprompter.promptvault.ui.theme.VaultTheme

/**
 * v8.23.0 — Glassmorphism DIHIDUPKAN KEMBALI (lihat javadoc lengkap alasan
 * & audit WCAG di `ui/theme/GlassTokens.kt`). Signature publik primitif
 * ini TIDAK BERUBAH SAMA SEKALI dari v8.0.0 -- semua pemanggil
 * (`VaultCard`, `RuleCard`, `GroupedListRow`, dst) otomatis dapat wajah
 * glass baru TANPA perlu disentuh satu pun, sesuai permintaan "bertahap,
 * mulai dari sini" (primitif tunggal, efek menjalar ke seluruh app).
 *
 * 3 lapisan glass, SEMUA murni dekoratif (`GlassTokens`), TIDAK mengubah
 * mekanisme elevasi M3 asli (`tonalElevation`/`shadowElevation`, tetap
 * dipertahankan dari v8.0.0 -- "premium tactile" via elevasi RESMI M3 +
 * glass finish, bukan salah satu doang):
 * 1. **Fill translucent**: `color` pemanggil di-alpha (bukan diganti hue-nya)
 *    -- `recessed` pakai [GlassTokens.FillAlphaRecessed] (paling transparan,
 *    kesan cekung), selainnya [GlassTokens.FillAlphaRaised].
 * 2. **Glass-edge border**: kalau pemanggil TIDAK kirim `border` eksplisit
 *    (semua call site saat ini memang begitu, diverifikasi sebelum
 *    menulis file ini), otomatis pakai hairline putih-alpha
 *    [GlassTokens.borderColor]. Kalau pemanggil suatu saat kirim `border`
 *    sendiri (mis. state error/fungsional), itu DIHORMATI apa adanya,
 *    bukan ditimpa.
 * 3. **Sheen highlight**: gradien vertikal tipis di lapisan PALING ATAS
 *    konten (dalam `Box`, di belakang `content()` asli) -- ikut ter-clip
 *    ke `shape` otomatis krn `Surface` M3 sudah clip slot kontennya.
 *    DILEWATI saat `recessed=true` (cekung tidak boleh "berkilau" seperti
 *    mengambang, insting glassmorphism standar: raised vs sunken beda
 *    treatment cahaya, BUKAN keluar dari gaya desain itu sendiri).
 *
 * ## Neumorphism (v8.23.2, REVERT DARURAT ke Surface+border polos di
 * v8.26.0 -- lihat komentar lengkap di titik cabang NEUMORPHISM di bawah)
 * Teknik shadow-ganda custom (drawBehind/nativeCanvas, v8.23.2-v8.25.4)
 * DIHAPUS TOTAL setelah terbukti bikin seluruh UI washed-out di device
 * nyata. Sekarang: `Surface` M3 baku + `BorderStroke` solid lebih tebal --
 * pembeda dari Material3 Murni SAAT ITU (v8.26.0) cuma border, bukan
 * shadow/gradient custom. ([2026-09-12] M3 sekarang JUGA punya border
 * sendiri -- facet glint gemstone, lihat cabang MATERIAL3 & `Material3Tokens.kt`
 * -- pembeda dgn Neumorphism sekarang di motif border-nya: solid tebal
 * netral vs gradient tipis warna Primary, bukan lagi "ada border/tidak".)
 *
 * @param recessed permukaan "tenggelam" (track switch/segmented control
 *   OFF, grabber pill sheet) -- tonal & shadow elevation SAMA-SAMA
 *   dipaksa 0dp, `color` pemanggil (biasanya `colorScheme.surfaceContainerLowest`,
 *   lebih gelap) yang membawa kesan cekung; fill lebih transparan & TANPA
 *   sheen menguatkan kesan itu di gaya glass. Di gaya Neumorphism, arah
 *   shadow dibalik (lihat di atas) alih-alih transparansi.
 */
@Composable
fun TactileSurface(
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.medium,
    color: Color = MaterialTheme.colorScheme.surface,
    elevation: Dp = TactileTokens.TactileElevationCard,
    recessed: Boolean = false,
    border: BorderStroke? = null,
    onClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    // [v8.30.0] Opt-in murni -- default `false`, 0 dampak ke caller manapun
    // yang tidak eksplisit mengaktifkan (lihat javadoc lengkap "Stacked
    // Cards Effect" di `NeumorphTokens.kt`). HANYA diaktifkan di
    // `VaultCard.kt` -- `GroupedListRow`/`EmptyState`/`TactileSwitch`/dst
    // (termasuk kotak ikon menu) TIDAK disentuh sama sekali.
    stackedCards: Boolean = false,
    // [v8.36.0] Opt-in KEDUA, TERPISAH dari `stackedCards` di atas --
    // default `false`, 0 dampak ke caller manapun (termasuk `VaultCard`,
    // yang TETAP pakai `stackedCards` lama, TIDAK disentuh). Varian
    // kiri-atas/3-lapis, lihat javadoc lengkap `NeumorphTokens.
    // StackedCardOffsetTopLeft` utk alasan kenapa opt-in terpisah &
    // dipasang HANYA di 1 titik (`HomeScreen.kt`, kartu manifest/statistik).
    stackedCardsTopLeft: Boolean = false,
    content: @Composable () -> Unit
) {
    val style = VaultTheme.style
    val effectiveElevation = if (recessed) 0.dp else elevation

    if (style == ThemeStyleOption.NEUMORPHISM) {
        // [v8.28.0 -- FIX REGRESI NYATA dari v8.27.0, ditemukan user via
        // screenshot: tab "Tampilan" hilang total + kartu kosong blank di
        // beberapa layar] Root cause: v8.27.0 membungkus `Surface` konten
        // dalam `Box { shadowCasterSurface; Surface(modifier=modifier) }`
        // supaya bisa taruh shadow-caster offset DI BELAKANG-nya -- tapi
        // `modifier` (berisi `Modifier.weight(1f)` dari caller spt
        // `SegmentedControl.kt`) jadinya nempel di `Surface` yang merupakan
        // CUCU dari `Row`/`Column` (via `Box` pembungkus), BUKAN anak
        // LANGSUNG -- `RowScope.weight()`/`BoxScope.align()` HANYA
        // dikenali kalau modifier ada di anak LANGSUNG scope itu. Row jadi
        // tidak tahu elemen itu punya weight, distribusi lebar rusak total
        // (1 tab "menghilang", kartu lain kolaps/blank tak terduga).
        // FIX: buang wrapper `Box` + shadow-caster offset SEPENUHNYA,
        // balik ke SATU `Surface(modifier=modifier, shadowElevation=)` --
        // PERSIS pola cabang Glass/Material3 di bawah (1 node, `modifier`
        // caller nempel LANGSUNG di situ, weight/align caller manapun
        // otomatis benar lagi, TERBUKTI aman krn dipakai identik di 2
        // cabang lain tanpa masalah). Konsekuensi: shadow jadi SATU arah
        // native Android (bukan lagi "dual-offset" custom) -- tapi
        // stabilitas layout jauh lebih penting drpd itu, apalagi sudah 3x
        // percobaan versi "lebih canggih" berturut-turut gagal dgn cara
        // BEDA-BEDA (lemah tak terlihat -> washed-out -> layout rusak).
        // Fill gradient tint (poin 2, TIDAK diubah) TETAP jadi sumber
        // utama kesan "timbul/cekung" -- itu 100% aman krn cuma di DALAM
        // `content()`, tidak pernah menyentuh struktur node di luar Surface.
        val highlightBrush = if (recessed) NeumorphTokens.fillShadeBrush() else NeumorphTokens.fillHighlightBrush()
        val shadeBrush = if (recessed) NeumorphTokens.fillHighlightBrush() else NeumorphTokens.fillShadeBrush()

        val neumorphContent: @Composable () -> Unit = {
            // `propagateMinConstraints = true` WAJIB -- pola sama persis
            // fix regresi centering yang sudah didokumentasikan di cabang
            // Glass di bawah, dicegah terulang di sini dari awal.
            Box(propagateMinConstraints = true) {
                Box(modifier = Modifier.matchParentSize().background(highlightBrush))
                Box(modifier = Modifier.matchParentSize().background(shadeBrush))
                content()
            }
        }

        val neumorphElevation = if (recessed) 0.dp else NeumorphTokens.ShadowElevation
        val neumorphBorder = border ?: BorderStroke(NeumorphTokens.BorderWidth, NeumorphTokens.borderBrush())
        // [v8.30.0] Stacked Cards Effect -- `drawBehind` ditempel LANGSUNG
        // ke `modifier` yang SAMA dipakai `Surface` di bawah (BUKAN `Box`
        // pembungkus baru, lihat javadoc lengkap alasan di
        // `NeumorphTokens.kt`) -- 0 risiko ke `Modifier.weight()`/`align()`
        // pemanggil (kelas regresi v8.28.0 TIDAK terulang). `!recessed`
        // krn elemen cekung "tenggelam", kontradiktif dgn efek tumpukan
        // kartu yang menonjol keluar.
        //
        // [v8.36.0] `stackedCardsTopLeft` (param BARU di bawah, TERPISAH
        // dari `stackedCards`) -- varian kiri-atas/3-lapis, WAJIB
        // `.padding(top=.., start=..)` (`NeumorphTokens.
        // StackedCardInsetTopLeft`) ditempel SEBELUM `.stackedCardsTopLeft()`
        // di chain yang sama (bukan `Box` baru, pola aman identik
        // `.stackedCards()` di atas) supaya lapis yang mengintip kiri-atas
        // gambar DI DALAM ruang yang sudah dialokasikan node ini sendiri --
        // 0 bocor ke sibling/tepi layar. `stackedCards` lama (cabang
        // pertama di bawah) SENGAJA 0 disentuh -- lihat alasan lengkap
        // (kenapa tidak boleh nimpa, dipakai bareng >10 layar termasuk 2
        // `LazyColumn` rapat) di javadoc `NeumorphTokens.
        // StackedCardOffsetTopLeft`.
        val neumorphModifier = when {
            stackedCardsTopLeft && !recessed -> with(NeumorphTokens) {
                modifier
                    .padding(top = StackedCardInsetTopLeft, start = StackedCardInsetTopLeft)
                    .stackedCardsTopLeft()
            }
            stackedCards && !recessed -> with(NeumorphTokens) { modifier.stackedCards() }
            else -> modifier
        }
        if (onClick != null) {
            Surface(
                onClick = onClick,
                enabled = enabled,
                modifier = neumorphModifier,
                shape = shape,
                color = color,
                border = neumorphBorder,
                tonalElevation = 0.dp,
                shadowElevation = neumorphElevation,
                interactionSource = interactionSource,
                content = neumorphContent
            )
        } else {
            Surface(
                modifier = neumorphModifier,
                shape = shape,
                color = color,
                border = neumorphBorder,
                tonalElevation = 0.dp,
                shadowElevation = neumorphElevation,
                content = neumorphContent
            )
        }
        return
    }

    if (style == ThemeStyleOption.MATERIAL3) {
        // [v8.23.4] Gaya ke-3: "Material 3 Murni" -- PERSIS perilaku
        // `TactileSurface` v8.0.0 SEBELUM Glassmorphism dihidupkan lagi
        // (v8.23.1): `Surface` M3 baku, `color` caller APA ADANYA (0 alpha).
        // Kedalaman murni dari tonal+shadow elevation M3 resmi.
        //
        // [2026-09-12, "perluas jangkauan" rombak MATERIAL3] `border` TIDAK
        // lagi caller apa adanya total -- diisi default "facet glint"
        // ([Material3Tokens]) KALAU caller tidak kirim border sendiri, pola
        // IDENTIK cabang CUPERTINO/GLASS di bawah (border eksplisit caller,
        // mis. state error fungsional, TETAP dihormati apa adanya, tidak
        // ditimpa). Menutup kesenjangan MATERIAL3 satu2nya dari 4 gaya yg
        // 0 treatment dekoratif sendiri di primitif ini -- lihat javadoc
        // lengkap `Material3Tokens.kt`.
        val material3Border = border ?: BorderStroke(Material3Tokens.GlintWidth, Material3Tokens.glintBorderBrush())
        if (onClick != null) {
            Surface(
                onClick = onClick,
                enabled = enabled,
                modifier = modifier,
                shape = shape,
                color = color,
                border = material3Border,
                tonalElevation = effectiveElevation,
                shadowElevation = effectiveElevation,
                interactionSource = interactionSource,
                content = content
            )
        } else {
            Surface(
                modifier = modifier,
                shape = shape,
                color = color,
                border = material3Border,
                tonalElevation = effectiveElevation,
                shadowElevation = effectiveElevation,
                content = content
            )
        }
        return
    }

    if (style == ThemeStyleOption.CUPERTINO) {
        // [v8.31.1, rename v8.31.4] Kerangka warna PERSIS SAMA dgn cabang
        // MATERIAL3 di atas (`color` caller apa adanya) -- 2 beda dari
        // MATERIAL3: (1) `border` caller yang `null` diisi hairline
        // Cupertino (`CupertinoTokens`), signature list/card iOS yang
        // mengandalkan garis tipis SOLID keliling penuh -- beda MOTIF dari
        // facet glint gemstone M3 ([2026-09-12] M3 di atas jg sudah isi
        // default border sendiri, `Material3Tokens.kt`, tapi gradient tipis
        // 1 sisi, bukan hairline solid keliling; lihat javadoc lengkap di
        // situ); (2) [v8.31.4, "restyling ke Cupertino murni"] elevasi
        // DIPAKSA 0dp SELALU (bukan `effectiveElevation` spt MATERIAL3) --
        // grouped list iOS asli FLAT TOTAL, tidak pernah pakai shadow apa
        // pun, warna latar yang jadi penanda "kartu" vs "background", bukan
        // bayangan. Kalau caller SUDAH kirim border sendiri (mis. state
        // error fungsional), itu dihormati apa adanya -- tidak ditimpa
        // (pola sama persis di cabang MATERIAL3).
        val cupertinoBorder = border ?: BorderStroke(CupertinoTokens.HairlineWidth, CupertinoTokens.hairlineColor())
        if (onClick != null) {
            Surface(
                onClick = onClick,
                enabled = enabled,
                modifier = modifier,
                shape = shape,
                color = color,
                border = cupertinoBorder,
                tonalElevation = 0.dp,
                shadowElevation = 0.dp,
                interactionSource = interactionSource,
                content = content
            )
        } else {
            Surface(
                modifier = modifier,
                shape = shape,
                color = color,
                border = cupertinoBorder,
                tonalElevation = 0.dp,
                shadowElevation = 0.dp,
                content = content
            )
        }
        return
    }

    val fillAlpha = if (recessed) GlassTokens.FillAlphaRecessed else GlassTokens.FillAlphaRaised
    val glassColor = color.copy(alpha = fillAlpha)
    val glassBorder = border ?: BorderStroke(GlassTokens.BorderWidthDefault, GlassTokens.borderColor(recessed))

    val glassContent: @Composable () -> Unit = {
        if (recessed) {
            content()
        } else {
            // [Fix regresi centering, 2026-08-23] `propagateMinConstraints = true`
            // WAJIB -- tanpa ini, min-constraint (mis. fillMaxWidth/fillMaxSize
            // dari M3 Surface internal) TIDAK diteruskan ke `content()` (Box
            // default melonggarkan min ke 0 utk child non-matchParentSize),
            // bikin caller yang tadinya otomatis "fill" (CTA "Scan Sekarang",
            // kotak ikon GroupedListRow, dst -- SEMUA lewat primitif ini) balik
            // ke wrap-content & nempel kiri-atas. Bug dilaporkan user via
            // screenshot, root cause DITEMUKAN di sini (bukan di caller manapun
            // -- 0 file lain perlu diubah).
            Box(propagateMinConstraints = true) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(GlassTokens.highlightBrush())
                )
                content()
            }
        }
    }

    if (onClick != null) {
        Surface(
            onClick = onClick,
            enabled = enabled,
            modifier = modifier,
            shape = shape,
            color = glassColor,
            border = glassBorder,
            tonalElevation = effectiveElevation,
            shadowElevation = effectiveElevation,
            interactionSource = interactionSource,
            content = glassContent
        )
    } else {
        Surface(
            modifier = modifier,
            shape = shape,
            color = glassColor,
            border = glassBorder,
            tonalElevation = effectiveElevation,
            shadowElevation = effectiveElevation,
            content = glassContent
        )
    }
}
