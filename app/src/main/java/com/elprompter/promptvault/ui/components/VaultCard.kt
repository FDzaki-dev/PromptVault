package com.elprompter.promptvault.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.elprompter.promptvault.ui.theme.TactileTokens

/**
 * Permukaan kartu utama app. Struktur wrap-content dipertahankan (kartu
 * TIDAK BOLEH merebut sisa tinggi layar, lihat Insiden #3 lama di
 * PROJECT_STATE.md) -- tidak berubah oleh batch ini.
 *
 * v8.0.0 — Glassmorphism -> Material 3 murni: `GlassPanel` diganti
 * `TactileSurface` (lihat `TactileSurface.kt`). `color` sekarang
 * `colorScheme.surfaceContainer` (peran M3 BAKU utk permukaan kartu
 * "naik" 1 tingkat dari root), menggantikan token literal `GlassSurface`.
 */
@Composable
fun VaultCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    TactileSurface(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainer,
        elevation = TactileTokens.TactileElevationCard,
        content = content
    )
}
