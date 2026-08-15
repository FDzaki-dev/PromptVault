package com.elprompter.promptvault.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.elprompter.promptvault.ui.theme.AmoledBackground
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
 * (shadow ganda terarah, bukan lagi gradient+hairline). `PlatinumTint`/
 * `PlatinumGradientAlpha`/`GlassBorder` (dulu dipakai di sini, era Teal
 * disebut `TealTint`/`TealGradientAlpha`) SEKARANG TIDAK
 * dipakai lagi oleh `VaultCard` -- tapi TIDAK dihapus dari `Color.kt`, masih
 * dipakai komponen lain (lihat FILE_MANIFEST/grep sebelum audit hapus).
 *
 * [Fix Insiden #9, v2.24.3] `baseColor` sekarang bisa dioper pemanggil
 * (default TETAP [AmoledBackground], 0 perubahan utk 12 dari 13 call site --
 * lihat PROJECT_STATE.md). Dibutuhkan krn `NeumorphicSurface` cuma
 * "menyamarkan" badan shadow-caster-nya kalau `baseColor` PERSIS/dekat sama
 * warna LATAR SESUNGGUHNYA di belakang kartu (lihat javadoc `Neumorphic.kt`)
 * -- default [AmoledBackground] cocok utk layar berlatar solid, TAPI
 * `HomeScreen` (satu-satunya pemanggil non-default) punya wash gradient di
 * latarnya (bukan solid), jadi perlu baseColor efektif hasil composite,
 * bukan default mentah.
 */
@Composable
fun VaultCard(
    modifier: Modifier = Modifier,
    baseColor: Color = AmoledBackground,
    content: @Composable () -> Unit
) {
    NeumorphicSurface(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = GlassSurface,
        baseColor = baseColor,
        elevation = TactileTokens.NeuElevationCard,
        shadowOffset = TactileTokens.NeuOffsetCard,
        content = content
    )
}
