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
 *
 * v8.30.0 — "Stacked Cards Effect" diaktifkan di sini (`stackedCards =
 * true`), permintaan eksplisit user, khusus tema Neumorphism (opt-in di
 * `TactileSurface`, 0 dampak ke gaya Glass/Material3 Murni atau komponen
 * lain -- lihat javadoc lengkap `NeumorphTokens.kt`). VaultCard dipilih
 * krn kartu PALING besar/dominan di app -- efek tumpukan paling masuk
 * akal & terlihat di sini, bukan di kotak ikon kecil/kontrol.
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
        stackedCards = true,
        content = content
    )
}
