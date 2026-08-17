package com.elprompter.promptvault.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.elprompter.promptvault.ui.theme.TactileTokens

/**
 * v8.0.0 — Primitif tunggal "Premium Tactile", MENGGANTIKAN `GlassPanel.kt`
 * (glassmorphism, DIHAPUS TOTAL -- permintaan eksplisit user: rombak total
 * ke "default Material 3 murni", lihat javadoc lengkap di
 * `ui/theme/Color.kt`/`TactileTokens.kt`).
 *
 * ## Beda dari `GlassPanel` lama
 * `GlassPanel` menggambar bahasa visual Glassmorphism KHUSUS (border
 * hairline putih-alpha, overlay gradient "highlight" vertikal, shadow warna
 * kustom lewat `Modifier.shadow(ambientColor=, spotColor=)`) -- semua itu
 * BUKAN bagian dari Material 3 baku, murni dekorasi tambahan era v7.x.
 * `TactileSurface` HANYA memakai `Surface` M3 BAWAAN dengan `tonalElevation`
 * + `shadowElevation` -- mekanisme kedalaman RESMI M3 (semakin tinggi
 * elevasi, M3 OTOMATIS mencampur tint `primary` ke warna permukaan lewat
 * `surfaceColorAtElevation`, ini API M3 baku, bukan hitungan manual) --
 * jadi "premium" & "tactile"-nya datang dari FISIKA M3 asli (tonal shift +
 * shadow asli), bukan lagi overlay dekoratif custom.
 *
 * @param recessed permukaan "tenggelam" (track switch/segmented control
 *   OFF, grabber pill sheet) -- tonal & shadow elevation SAMA-SAMA
 *   dipaksa 0dp, `color` pemanggil (biasanya `colorScheme.surfaceContainerLowest`,
 *   lebih gelap) yang membawa kesan cekung.
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
    val effectiveElevation = if (recessed) 0.dp else elevation

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
}
