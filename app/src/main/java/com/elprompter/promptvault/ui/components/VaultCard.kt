package com.elprompter.promptvault.ui.components

import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.elprompter.promptvault.ui.theme.EmeraldAmbientAlpha
import com.elprompter.promptvault.ui.theme.EmeraldAmbientTint
import com.elprompter.promptvault.ui.theme.NeuHighlight
import com.elprompter.promptvault.ui.theme.TitaniumSurface
import com.elprompter.promptvault.ui.theme.TitaniumSurfaceRaised

/**
 * v4.0.0 — Kartu tactile utama, ganti total dari "AMOLED glass + Midnight
 * Blue tint" ke "Dark Titanium Neumorphism": shadow elevasi asli (ambient/
 * spotColor bertone titanium gelap, bukan hitam generik) + fill brushed-metal
 * diagonal (TitaniumSurfaceRaised -> wash Zamrud alpha-rendah -> TitaniumSurface)
 * + border highlight rambut kiri-atas. Wash Zamrud sengaja SANGAT tipis
 * (EmeraldAmbientAlpha) -- "sedikit sentuhan" sesuai instruksi user, Titanium
 * tetap warna yang dominan terlihat. Struktur wrap-content (tanpa
 * fillMaxSize di dalam Box) dipertahankan dari fix regresi v2.3.1.
 */
@Composable
fun VaultCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(EmeraldAmbientTint.copy(alpha = EmeraldAmbientAlpha), Color.Transparent)
                ),
                shape = MaterialTheme.shapes.medium
            )
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(TitaniumSurfaceRaised, TitaniumSurface, TitaniumSurface)
                ),
                shape = MaterialTheme.shapes.medium
            )
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(NeuHighlight.copy(alpha = 0.10f), Color.Transparent, Color.Transparent)
                ),
                shape = MaterialTheme.shapes.medium
            ),
        shape = MaterialTheme.shapes.medium,
        color = Color.Transparent,
        border = androidx.compose.foundation.BorderStroke(1.dp, NeuHighlight.copy(alpha = 0.10f)),
        tonalElevation = 0.dp,
        shadowElevation = 6.dp
    ) {
        content()
    }
}
