package com.elprompter.promptvault.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
 * ## Neumorphism (v8.23.2, GANTI TEKNIK ke-4 kalinya di v8.25.4 -- lihat
 * javadoc lengkap & root cause final di `NeumorphTokens.kt`)
 * Berlaku saat `VaultTheme.style == NEUMORPHISM` -- treatment BEDA TOTAL
 * (bukan varian dari Glass ATAU M3 `Surface`): `Box` polos + gradient
 * fill diagonal (`NeumorphTokens.convexBrush`/`concaveBrush`) + SHADOW
 * GANDA genuine (`NeumorphTokens.convexShadow`/`concaveShadow`, teknik
 * `Paint.setShadowLayer` dual-offset dual-warna, PERSIS terjemahan CSS
 * `box-shadow` yg dikasih user) -- BUKAN lagi `Modifier.border` bevel
 * (v8.25.3, garis tegas terbukti "belum sempurna" krn beda teknik dari
 * soft-shadow neumorphism genuine). `recessed=true` pakai versi
 * concave (cekung, shadow inset via clip+stroke) alih-alih convex
 * (timbul, shadow di luar bentuk).
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
        // (v8.25.4) `Surface` M3 DILEPAS utk gaya ini -- `color`-nya cuma
        // terima solid `Color`, tidak bisa gradient `Brush`. Shadow GANDA
        // (di `NeumorphTokens`) WAJIB digambar di layer TERPISAH & DI
        // BAWAH fill gradient (lihat urutan modifier: shadow dulu baru
        // `.background(brush, shape)`), supaya cuma bagian shadow yg
        // "mengintip" di tepi luar/dalam yg tampak -- PERSIS efek CSS
        // box-shadow ganda, bukan shadow nutup seluruh badan.
        val shadowModifier = with(NeumorphTokens) {
            if (recessed) Modifier.concaveShadow(shape) else Modifier.convexShadow(shape)
        }
        val brush = if (recessed) NeumorphTokens.concaveBrush() else NeumorphTokens.convexBrush()
        val clickModifier = if (onClick != null) {
            Modifier.clickable(
                interactionSource = interactionSource,
                indication = rememberRipple(),
                enabled = enabled,
                onClick = onClick
            )
        } else Modifier
        Box(
            modifier = modifier
                .then(shadowModifier)
                .then(clickModifier)
        ) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clip(shape)
                    .background(brush)
            )
            content()
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
