package com.elprompter.promptvault.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.elprompter.promptvault.ui.theme.GlassBorder
import com.elprompter.promptvault.ui.theme.GlassSurface
import com.elprompter.promptvault.ui.theme.GlassSurfaceElevated
import com.elprompter.promptvault.ui.theme.TactileTokens
import com.elprompter.promptvault.ui.theme.TealGradientAlpha
import com.elprompter.promptvault.ui.theme.TealTint

/**
 * Permukaan glass tactile utama (bab 4 & 2.5 spesifikasi v3.0.0): gradient dari
 * GlassSurfaceElevated -> tint Teal tipis (alpha rendah, HANYA atmosfer, bukan
 * warna dominan) -> GlassSurface, dengan border rambut translusen. Struktur
 * wrap-content (tanpa fillMaxSize di dalam) dipertahankan dari fix regresi
 * v2.3.1 supaya kartu tidak merebut sisa tinggi layar.
 *
 * v4.0.0 -- "ultra immersive depth/3D" (permintaan eksplisit user): kartu
 * sekarang punya elevasi NYATA ([TactileTokens.ElevationCard]), bukan flat
 * (`shadowElevation = 0.dp` di v3.0.0). Cara amannya SENGAJA BUKAN
 * `Modifier.shadow(...).background(brush)` yang dirantai langsung -- itu
 * PERSIS kombinasi yang menyebabkan regresi CTA Home v2.14.0 (kotak pucat/
 * glitch di banyak GPU/skin, di-fix v2.14.1 dengan melepas shadow total,
 * lihat CHANGELOG). Pola aman di sini: `Surface(shadowElevation=...)` dengan
 * `color` SOLID (`GlassSurface`, bukan `Color.Transparent`) supaya RenderNode
 * shadow Material3 (jalur resmi, sudah teruji luas) yang menggambar bayangan
 * -- lalu gradient teal ditumpuk sebagai LAYER TERPISAH (`Box.background`)
 * DI DALAM konten Surface, bukan di-chain ke node yang sama dengan shadow.
 */
@Composable
fun VaultCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = GlassSurface,
        border = BorderStroke(1.dp, GlassBorder),
        tonalElevation = 0.dp,
        shadowElevation = TactileTokens.ElevationCard
    ) {
        Box(
            modifier = Modifier.background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        GlassSurfaceElevated,
                        TealTint.copy(alpha = TealGradientAlpha),
                        GlassSurface
                    )
                )
            )
        ) {
            content()
        }
    }
}
