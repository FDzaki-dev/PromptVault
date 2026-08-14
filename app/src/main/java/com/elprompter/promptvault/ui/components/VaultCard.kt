package com.elprompter.promptvault.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.elprompter.promptvault.ui.theme.GlassSurface
import com.elprompter.promptvault.ui.theme.TactileTokens

/**
 * Permukaan tactile utama app (dulu "glass": gradient tint + border rambut,
 * bab 4/2.5 spesifikasi v3.0.0). Struktur wrap-content (tanpa fillMaxSize di
 * dalam Box pembungkus) dipertahankan APA ADANYA dari fix regresi v2.3.1
 * (Insiden #3, lihat PROJECT_STATE.md) -- kartu TIDAK BOLEH merebut sisa
 * tinggi layar.
 *
 * v5.0.0 -- Redesign Glassmorphism -> Neumorphism (permintaan eksplisit
 * user, "tanpa ganti palet warna"): gradient tint Teal + border hairline
 * dihapus total, digantikan `NeumorphicSurface` (lihat `Neumorphic.kt` utk
 * detail teknik shadow ganda). Warna isi kartu TETAP [GlassSurface] --
 * TIDAK ADA hex baru di batch ini, murni ganti CARA permukaan itu digambar
 * (shadow ganda terarah, bukan lagi gradient+hairline). `TealTint`/
 * `TealGradientAlpha`/`GlassBorder` (dulu dipakai di sini) SEKARANG TIDAK
 * dipakai lagi oleh `VaultCard` -- tapi TIDAK dihapus dari `Color.kt`, masih
 * dipakai komponen lain (lihat FILE_MANIFEST/grep sebelum audit hapus).
 */
@Composable
fun VaultCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    NeumorphicSurface(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = GlassSurface,
        elevation = TactileTokens.NeuElevationCard,
        shadowOffset = TactileTokens.NeuOffsetCard,
        content = content
    )
}
