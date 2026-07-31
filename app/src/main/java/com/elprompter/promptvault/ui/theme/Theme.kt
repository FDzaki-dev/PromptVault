package com.elprompter.promptvault.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
    primary = Pine,
    onPrimary = CardPaper,
    secondary = Stamp,
    onSecondary = CardPaper,
    tertiary = Amber,
    onTertiary = CardPaper,
    background = Kraft,
    onBackground = Ink,
    surface = CardPaper,
    onSurface = Ink,
    surfaceVariant = Kraft,
    onSurfaceVariant = InkFaint,
    error = Rust,
    onError = CardPaper,
    outline = HairlineInk
)

/**
 * Skema gelap "ultra premium": tiga lapisan permukaan (background < surface <
 * surfaceVariant sebagai "raised") supaya kartu/sheet terasa mengambang, bukan
 * menyatu jadi hitam polos. Semua aksen dicerahkan (PineGlow/StampGlow/AmberGlow)
 * biar tetap hidup & kontras di atas dasar nyaris-hitam, alih-alih warna terang
 * yang justru pudar/kusam kalau ditaruh apa adanya di dark mode.
 */
private val DarkColors = darkColorScheme(
    primary = PineGlow,
    onPrimary = ObsidianBase,
    secondary = StampGlow,
    onSecondary = ObsidianBase,
    tertiary = AmberGlow,
    onTertiary = ObsidianBase,
    background = ObsidianBase,
    onBackground = IvoryText,
    surface = ObsidianSurface,
    onSurface = IvoryText,
    surfaceVariant = ObsidianSurfaceRaised,
    onSurfaceVariant = IvoryTextFaint,
    error = RustGlow,
    onError = ObsidianBase,
    outline = HairlineIvory
)

@Composable
fun PromptVaultTheme(darkTheme: Boolean = false, content: @Composable () -> Unit) {
    val colors = if (darkTheme) DarkColors else LightColors
    MaterialTheme(colorScheme = colors, typography = PromptVaultTypography, shapes = PromptVaultShapes, content = content)
}
