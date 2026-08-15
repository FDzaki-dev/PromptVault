package com.elprompter.promptvault.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * v3.0.0 — Dark mode adalah SATU-SATUNYA skema yang ada. TIDAK diubah oleh
 * batch ini -- KEDUA preset di bawah (default & alternatif) sama-sama gelap,
 * toggle-nya soal PALET WARNA, bukan terang/gelap.
 *
 * v7.0.0 — Neumorphism -> Glassmorphism (lihat javadoc lengkap di Color.kt).
 *
 * v7.1.0 — Toggle tema (fitur baru, `SettingsRepository.useAltThemeFlow`).
 * `PromptVaultTheme` SEKARANG punya parameter `useAltTheme: Boolean` yang
 * BENAR-BENAR dibaca tiap recomposition utk memilih 1 dari 2 `ColorScheme`
 * ([VaultDarkColorsDefault]/[VaultDarkColorsAlt]) -- PENTING: ini BEDA dari
 * `ThemeMode` lama (v2.16.0, DIHAPUS krn jadi switch UI kosong yang tidak
 * pernah benar-benar mengubah apa pun, lihat catatan di
 * `SettingsRepository.DEFAULT_USE_ALT_THEME`). Struktur PERAN M3
 * (primary/secondary/dst) SAMA PERSIS di kedua skema, HANYA hex tokennya
 * beda -- jadi kedua preset otomatis konsisten scoping-nya (semantik
 * Amber/Rust/Slate TETAP sama di kedua preset, lihat Color.kt).
 */
private val VaultDarkColorsDefault: ColorScheme = darkColorScheme(
    primary = BrassAccent,
    onPrimary = BrassAccentOn,
    primaryContainer = BrassAccentContainer,
    onPrimaryContainer = BrassAccent,
    secondary = BrassAccent,
    onSecondary = BrassAccentOn,
    secondaryContainer = BrassAccentContainer,
    onSecondaryContainer = BrassAccent,
    tertiary = AmberGlow,
    onTertiary = BrassAccentOn,
    tertiaryContainer = AmberGlowContainer,
    onTertiaryContainer = AmberGlow,
    background = AmoledBackground,
    onBackground = TextPrimary,
    surface = GlassSurface,
    onSurface = TextPrimary,
    surfaceVariant = GlassSurfaceElevated,
    onSurfaceVariant = TextSecondary,
    surfaceContainer = GlassSurface,
    surfaceContainerHigh = GlassSurfaceElevated,
    surfaceContainerHighest = GlassSurfaceSheet,
    surfaceContainerLow = AmoledBackground,
    surfaceContainerLowest = AmoledBackground,
    inverseSurface = TextPrimary,
    inverseOnSurface = AmoledBackground,
    inversePrimary = BrassAccent,
    error = RustGlow,
    onError = BrassAccentOn,
    errorContainer = RustGlowContainer,
    onErrorContainer = RustGlow,
    outline = HairlineGlass,
    outlineVariant = HairlineGlass,
    scrim = GlassShadow
)

/**
 * Preset alternatif "Charcoal + Copper" -- lihat javadoc lengkap sumber
 * warna & perhitungan WCAG di `Color.kt`. Peran M3 di sini SENGAJA
 * struktur-identik dgn [VaultDarkColorsDefault] di atas (baris demi baris
 * peran yang sama), supaya perbedaan HANYA di token warna, bukan di
 * pemetaan peran -- lebih mudah diverifikasi/diaudit berdampingan.
 */
private val VaultDarkColorsAlt: ColorScheme = darkColorScheme(
    primary = CopperAccent,
    onPrimary = CopperAccentOn,
    primaryContainer = CopperAccentContainer,
    onPrimaryContainer = CopperAccent,
    secondary = CopperAccent,
    onSecondary = CopperAccentOn,
    secondaryContainer = CopperAccentContainer,
    onSecondaryContainer = CopperAccent,
    tertiary = AmberGlow,
    onTertiary = CopperAccentOn,
    tertiaryContainer = AmberGlowContainer,
    onTertiaryContainer = AmberGlow,
    background = CharcoalBackground,
    onBackground = TextPrimary,
    surface = CharcoalSurface,
    onSurface = TextPrimary,
    surfaceVariant = CharcoalSurfaceElevated,
    onSurfaceVariant = TextSecondary,
    surfaceContainer = CharcoalSurface,
    surfaceContainerHigh = CharcoalSurfaceElevated,
    surfaceContainerHighest = CharcoalSurfaceSheet,
    surfaceContainerLow = CharcoalBackground,
    surfaceContainerLowest = CharcoalBackground,
    inverseSurface = TextPrimary,
    inverseOnSurface = CharcoalBackground,
    inversePrimary = CopperAccent,
    error = RustGlow,
    onError = CopperAccentOn,
    errorContainer = RustGlowContainer,
    onErrorContainer = RustGlow,
    outline = GlassBorder,
    outlineVariant = GlassBorder,
    scrim = GlassShadow
)

/**
 * Resolusi warna status/nav bar sistem (dipakai `MainActivity.onCreate` +
 * `SideEffect` reaktif) -- SATU sumber kebenaran, supaya chrome sistem &
 * konten Compose TIDAK PERNAH beda preset (root cause class bug baru yang
 * dihindari: window decor disetel sekali di `onCreate` sebelum state
 * DataStore termuat, TIDAK reaktif ke toggle tanpa titik pusat ini).
 */
fun resolveBackgroundColor(useAltTheme: Boolean) = if (useAltTheme) CharcoalBackground else AmoledBackground

/**
 * Aksen ke-4 di luar peran Material3 baku, dipakai khusus untuk "Pengaturan".
 * TIDAK diubah dari v6.0.0 -- lihat Color.kt. SAMA di kedua preset tema
 * (semantik "Pengaturan" TIDAK bagian dari toggle, konsisten dgn Amber/Rust
 * yang juga TIDAK berubah antar preset).
 */
data class VaultExtraColors(
    val slate: androidx.compose.ui.graphics.Color,
    val slateContainer: androidx.compose.ui.graphics.Color
)

private val VaultExtra = VaultExtraColors(slate = SlateGlow, slateContainer = SlateGlowContainer)

val LocalVaultExtraColors = staticCompositionLocalOf { VaultExtra }

object VaultTheme {
    val extraColors: VaultExtraColors
        @Composable get() = LocalVaultExtraColors.current
}

@Composable
fun PromptVaultTheme(useAltTheme: Boolean = false, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalVaultExtraColors provides VaultExtra) {
        MaterialTheme(
            colorScheme = if (useAltTheme) VaultDarkColorsAlt else VaultDarkColorsDefault,
            typography = PromptVaultTypography,
            shapes = PromptVaultShapes,
            content = content
        )
    }
}
