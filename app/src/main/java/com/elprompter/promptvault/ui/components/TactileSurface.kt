package com.elprompter.promptvault.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
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
import com.elprompter.promptvault.ui.theme.GlassTokens
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
 * pembeda dari Material3 Murni cuma border, bukan shadow/gradient custom.
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
        if (onClick != null) {
            Surface(
                onClick = onClick,
                enabled = enabled,
                modifier = modifier,
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
                modifier = modifier,
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
        // (v8.23.1): `Surface` M3 baku, `color`/`border` caller APA ADANYA
        // (0 alpha, 0 override border, 0 sheen). Kedalaman murni dari
        // tonal+shadow elevation M3 resmi.
        if (onClick != null) {
            Surface(
                onClick = onClick,
                enabled = enabled,
                modifier = modifier,
                shape = shape,
                color = color,
                border = border,
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
                border = border,
                tonalElevation = effectiveElevation,
                shadowElevation = effectiveElevation,
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
