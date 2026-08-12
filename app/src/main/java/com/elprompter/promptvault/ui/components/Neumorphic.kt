package com.elprompter.promptvault.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.elprompter.promptvault.ui.theme.NeuHighlight
import com.elprompter.promptvault.ui.theme.NeuShadowDark
import com.elprompter.promptvault.ui.theme.TitaniumSurface
import com.elprompter.promptvault.ui.theme.TitaniumSurfaceRaised
import com.elprompter.promptvault.ui.theme.TitaniumSurfaceRecessed

/**
 * v4.0.0 — Modifier terpusat untuk "Dark Titanium Neumorphism". Dipakai di
 * seluruh kartu/kontrol supaya kedalaman konsisten satu sumber (bab tactile
 * lama: "Do not duplicate tactile constants throughout screen files").
 *
 * Sengaja HANYA memakai API Compose yang sudah lama proven dipakai project
 * ini (Modifier.shadow dgn ambient/spotColor kustom, Brush.linearGradient,
 * Modifier.border dgn Brush) -- BUKAN hack Paint.setShadowLayer ganda yang
 * belum pernah dikompilasi di sandbox ini. Lihat Insiden #7 PROJECT_STATE.md
 * kenapa project ini menghindari API belum-teruji yang ditulis blind.
 *
 * Ilusi depth neumorphism (dua shadow berlawanan arah) disimulasikan lewat
 * 3 lapis: (1) shadow elevasi asli bertone titanium gelap (bukan hitam
 * generik Android), (2) gradient brushed-metal diagonal terang kiri-atas ->
 * gelap kanan-bawah di fill, (3) border gradient highlight rambut yang HANYA
 * pekat di sudut kiri-atas (reflected light, bukan outline kotak penuh).
 */

/** Permukaan "terangkat" (convex) -- kartu, chip terpilih, tombol idle. */
fun Modifier.neuRaised(
    shape: Shape = RoundedCornerShape(20.dp),
    elevation: Dp = 10.dp
): Modifier = this
    .shadow(
        elevation = elevation,
        shape = shape,
        ambientColor = NeuShadowDark,
        spotColor = NeuShadowDark,
        clip = false
    )
    .background(
        brush = Brush.linearGradient(
            colors = listOf(TitaniumSurfaceRaised, TitaniumSurface, TitaniumSurface)
        ),
        shape = shape
    )
    .border(
        width = 1.dp,
        brush = Brush.linearGradient(
            colors = listOf(NeuHighlight, Color.Transparent, Color.Transparent)
        ),
        shape = shape
    )

/** Permukaan "tenggelam" (concave/inset) -- track OFF, sumur ikon, area tekan. */
fun Modifier.neuInset(
    shape: Shape = RoundedCornerShape(14.dp)
): Modifier = this
    .background(
        brush = Brush.linearGradient(
            colors = listOf(TitaniumSurfaceRecessed, TitaniumSurface, TitaniumSurfaceRaised)
        ),
        shape = shape
    )
    .border(
        width = 1.dp,
        brush = Brush.linearGradient(
            colors = listOf(NeuShadowDark.copy(alpha = 0.5f), Color.Transparent, Color.Transparent)
        ),
        shape = shape
    )
