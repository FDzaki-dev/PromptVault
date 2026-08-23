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
 * ## Neumorphism (v8.23.2, GANTI TEKNIK ke-3 kalinya di v8.25.3 -- lihat
 * javadoc lengkap & root cause final di `NeumorphTokens.kt`)
 * Berlaku saat `VaultTheme.style == NEUMORPHISM` -- treatment BEDA TOTAL
 * (bukan varian dari Glass): fill OPAQUE polos (`color` caller apa
 * adanya, TIDAK ditint), TANPA sheen glass. Kedalaman SEKARANG murni
 * dari **garis bingkai (bevel) dual-tone** -- `Modifier.border` dgn
 * `NeumorphTokens.bevelBrush()` (diagonal terang->gelap, TEPAT di tepi
 * shape, TIDAK ada fill/glow/blob apa pun lagi di dalam ATAU di luar
 * bentuk). `recessed=true` membalik arah (gelap di kiri-atas, terang di
 * kanan-bawah -- kesan "ditekan ke dalam").
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
        // [v8.25.3] Fill tint gradient (v8.24.0-v8.25.2, DI DALAM bentuk)
        // DIHAPUS TOTAL -- diganti border bevel tipis, lihat javadoc di
        // atas & NeumorphTokens.kt utk root cause lengkap kenapa fill
        // tint tidak pernah bisa cukup terlihat (dibatasi WCAG teks).
        val bevelBrush = if (recessed) {
            NeumorphTokens.bevelBrush(NeumorphTokens.BevelDarkColor, NeumorphTokens.BevelLightColor)
        } else {
            NeumorphTokens.bevelBrush(NeumorphTokens.BevelLightColor, NeumorphTokens.BevelDarkColor)
        }
        val bevelBorder = border ?: BorderStroke(NeumorphTokens.BevelWidth, bevelBrush)
        if (onClick != null) {
            Surface(
                onClick = onClick,
                enabled = enabled,
                modifier = modifier,
                shape = shape,
                color = color,
                border = bevelBorder,
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
                border = bevelBorder,
                tonalElevation = 0.dp,
                shadowElevation = 0.dp,
                content = content
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
