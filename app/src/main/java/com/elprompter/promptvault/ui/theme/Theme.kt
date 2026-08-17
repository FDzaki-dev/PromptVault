package com.elprompter.promptvault.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * v8.0.0 — ROMBAK TOTAL tema (lihat javadoc lengkap di Color.kt). Toggle
 * `useAltTheme` (v7.1.0, 2 preset kustom Navy+Brass/Charcoal+Copper)
 * DIHAPUS TOTAL bersama `VaultDarkColorsAlt`/`resolveBackgroundColor` --
 * "default Material 3 murni" berarti SATU ColorScheme baku, bukan toggle
 * antar 2 identitas kustom. `PromptVaultTheme` sekarang TIDAK punya
 * parameter lagi (dulu `useAltTheme: Boolean`) -- call site cukup
 * `PromptVaultTheme { content() }`.
 *
 * Dark mode TETAP satu-satunya mode (keputusan v3.0.0 tidak diubah -- user
 * minta rombak TEMA/warna, bukan minta Light mode baru; lihat
 * PROJECT_STATE.md utk histori & app/src/main/res/values/themes.xml utk
 * native theme non-Light yang selaras).
 */
private val PromptVaultColors: ColorScheme = darkColorScheme(
    primary = Primary,
    onPrimary = OnPrimary,
    primaryContainer = PrimaryContainer,
    onPrimaryContainer = OnPrimaryContainer,
    secondary = Secondary,
    onSecondary = OnSecondary,
    secondaryContainer = SecondaryContainer,
    onSecondaryContainer = OnSecondaryContainer,
    tertiary = Tertiary,
    onTertiary = OnTertiary,
    tertiaryContainer = TertiaryContainer,
    onTertiaryContainer = OnTertiaryContainer,
    background = AppBackground,
    onBackground = TextPrimary,
    surface = SurfaceDefault,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceContainerHigh,
    onSurfaceVariant = TextSecondary,
    surfaceContainer = SurfaceContainer,
    surfaceContainerHigh = SurfaceContainerHigh,
    surfaceContainerHighest = SurfaceContainerHighest,
    surfaceContainerLow = SurfaceContainerLow,
    surfaceContainerLowest = SurfaceContainerLowest,
    inverseSurface = TextPrimary,
    inverseOnSurface = AppBackground,
    inversePrimary = Primary,
    error = ErrorRed,
    onError = OnErrorRed,
    errorContainer = ErrorContainer,
    onErrorContainer = OnErrorContainer,
    outline = Outline,
    outlineVariant = OutlineVariant,
    scrim = Color.Black
)

/**
 * Aksen ke-4 di luar peran M3 baku, khusus menu "Pengaturan" (pola
 * "sistem 4-aksen" dipertahankan, lihat Color.kt). Nama field `slate`/
 * `slateContainer` SENGAJA TIDAK di-rename (walau sumber warnanya sekarang
 * SettingsAccent, bukan lagi SlateGlow) -- satu-satunya call site
 * (`HomeScreen.kt`) tetap valid tanpa perlu disentuh, satu titik saja yang
 * berubah (di sini).
 */
data class VaultExtraColors(
    val slate: Color,
    val slateContainer: Color
)

private val VaultExtra = VaultExtraColors(slate = SettingsAccent, slateContainer = SettingsAccentContainer)

val LocalVaultExtraColors = staticCompositionLocalOf { VaultExtra }

object VaultTheme {
    val extraColors: VaultExtraColors
        @Composable get() = LocalVaultExtraColors.current
}

@Composable
fun PromptVaultTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalVaultExtraColors provides VaultExtra) {
        MaterialTheme(
            colorScheme = PromptVaultColors,
            typography = PromptVaultTypography,
            shapes = PromptVaultShapes,
            content = content
        )
    }
}
