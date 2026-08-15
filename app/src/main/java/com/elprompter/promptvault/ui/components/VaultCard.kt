package com.elprompter.promptvault.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.elprompter.promptvault.ui.theme.GlassSurface
import com.elprompter.promptvault.ui.theme.TactileTokens

/**
 * Permukaan kaca utama app. Struktur wrap-content dipertahankan (kartu
 * TIDAK BOLEH merebut sisa tinggi layar, lihat Insiden #3 lama di
 * PROJECT_STATE.md) -- tidak berubah oleh batch ini.
 *
 * v7.0.0 — Neumorphism -> Glassmorphism: `NeumorphicSurface` diganti
 * `GlassPanel` (lihat `GlassPanel.kt`). Parameter `baseColor` DIHAPUS --
 * itu murni kebutuhan teknik shadow-caster neumorphic lama yang harus
 * "menyamar" dengan latar sesungguhnya (sumber Insiden #9 & #10); shadow
 * standar `GlassPanel` valid di atas latar apapun jadi parameter itu sudah
 * tidak relevan lagi (0 call site di app ini pernah override-nya, lihat
 * FILE_MANIFEST/grep sebelum audit hapus).
 */
@Composable
fun VaultCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    GlassPanel(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = GlassSurface,
        elevation = TactileTokens.GlassElevationCard,
        content = content
    )
}
