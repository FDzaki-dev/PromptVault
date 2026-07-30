package com.elprompter.promptvault.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Pine,
    onPrimary = CardPaper,
    secondary = Stamp,
    onSecondary = CardPaper,
    tertiary = Amber,
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

private val DarkColors = darkColorScheme(
    primary = PineLight,
    onPrimary = Color(0xFF10160F),
    secondary = Stamp,
    background = Color(0xFF171D19),
    onBackground = Kraft,
    surface = Color(0xFF1E251F),
    onSurface = Kraft,
    error = Rust
)

@Composable
fun PromptVaultTheme(darkTheme: Boolean = false, content: @Composable () -> Unit) {
    val colors = if (darkTheme) DarkColors else LightColors
    MaterialTheme(colorScheme = colors, typography = PromptVaultTypography, content = content)
}
