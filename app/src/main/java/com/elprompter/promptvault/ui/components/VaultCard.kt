package com.elprompter.promptvault.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.elprompter.promptvault.ui.theme.GlassBorder
import com.elprompter.promptvault.ui.theme.GlassSurface
import com.elprompter.promptvault.ui.theme.GlassSurfaceElevated
import com.elprompter.promptvault.ui.theme.MidnightBlueGradientAlpha
import com.elprompter.promptvault.ui.theme.MidnightBlueTint

/**
 * Permukaan glass tactile utama (bab 4 & 2.5 spesifikasi): gradient dari
 * GlassSurfaceElevated -> tint Midnight Blue tipis (alpha rendah, HANYA
 * atmosfer, bukan warna dominan) -> GlassSurface, dengan border rambut
 * translusen. Ini menggantikan gradient "kertas kraft" versi lama secara
 * total; struktur (wrap-content, tanpa fillMaxSize di dalam) dipertahankan
 * dari fix regresi v2.3.1 supaya kartu tidak merebut sisa tinggi layar.
 */
@Composable
fun VaultCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier.background(
            brush = Brush.linearGradient(
                colors = listOf(
                    GlassSurfaceElevated,
                    MidnightBlueTint.copy(alpha = MidnightBlueGradientAlpha),
                    GlassSurface
                )
            ),
            shape = MaterialTheme.shapes.medium
        ),
        shape = MaterialTheme.shapes.medium,
        color = Color.Transparent,
        border = BorderStroke(1.dp, GlassBorder),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        content()
    }
}
