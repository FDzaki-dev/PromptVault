package com.elprompter.promptvault.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * v4.0.0 — Dark mode tetap SATU-SATUNYA skema (keputusan arsitektur lama
 * tidak berubah), isinya diganti total ke "Dark Titanium Neumorphism +
 * Zamrud Accent" (lihat Color.kt untuk penjelasan lengkap tiap token).
 * `primary` sekarang Emerald (dipakai TERBATAS sesuai instruksi "sedikit
 * sentuhan zamrud"), seluruh permukaan (`surface*`, `background`) titanium.
 */
private val VaultDarkColors = darkColorScheme(
    primary = EmeraldAccent,
    onPrimary = EmeraldAccentOn,
    primaryContainer = EmeraldAccentContainer,
    onPrimaryContainer = EmeraldAccent,
    secondary = StampGlow,
    onSecondary = EmeraldAccentOn,
    secondaryContainer = StampGlowContainer,
    onSecondaryContainer = StampGlow,
    tertiary = AmberGlow,
    onTertiary = EmeraldAccentOn,
    tertiaryContainer = AmberGlowContainer,
    onTertiaryContainer = AmberGlow,
    background = TitaniumBase,
    onBackground = TextPrimary,
    surface = TitaniumSurface,
    onSurface = TextPrimary,
    surfaceVariant = TitaniumSurfaceRaised,
    onSurfaceVariant = TextSecondary,
    surfaceContainer = TitaniumSurface,
    surfaceContainerHigh = TitaniumSurfaceRaised,
    surfaceContainerHighest = TitaniumSurfaceSheet,
    surfaceContainerLow = TitaniumBase,
    surfaceContainerLowest = TitaniumBase,
    inverseSurface = TextPrimary,
    inverseOnSurface = TitaniumBase,
    inversePrimary = EmeraldAccent,
    error = RustGlow,
    onError = EmeraldAccentOn,
    errorContainer = RustGlowContainer,
    onErrorContainer = RustGlow,
    outline = HairlineNeu,
    outlineVariant = HairlineNeu,
    scrim = NeuShadowDark
)

/**
 * Aksen ke-4 di luar peran Material3 baku, dipakai khusus untuk "Pengaturan"
 * supaya grouped-list tetap punya identitas warna berbeda per menu.
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
fun PromptVaultTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalVaultExtraColors provides VaultExtra) {
        MaterialTheme(
            colorScheme = VaultDarkColors,
            typography = PromptVaultTypography,
            shapes = PromptVaultShapes,
            content = content
        )
    }
}
