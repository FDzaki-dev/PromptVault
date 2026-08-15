package com.elprompter.promptvault.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * v3.0.0 — Dark mode adalah SATU-SATUNYA skema yang ada. TIDAK diubah oleh
 * batch ini.
 *
 * v7.0.0 — Neumorphism -> Glassmorphism (lihat javadoc lengkap di
 * Color.kt). `primary`/`secondary` SEKARANG SAMA-SAMA `BrassAccent` --
 * CTA tidak lagi blend 2 aksen (Platinum+Ruby v6.0.0 DIHAPUS), cukup 1
 * aksen tombol utama sesuai instruksi eksplisit user. `SortedStamp`
 * (badge sukses, pakai `colors.secondary`) otomatis ikut jadi stempel
 * Brass -- cocok secara tematik ("stempel emas/kuningan"), bukan
 * penambahan warna baru.
 */
private val VaultDarkColors = darkColorScheme(
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
 * Aksen ke-4 di luar peran Material3 baku, dipakai khusus untuk "Pengaturan".
 * TIDAK diubah dari v6.0.0 -- lihat Color.kt.
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
