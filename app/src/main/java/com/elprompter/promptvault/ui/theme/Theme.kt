package com.elprompter.promptvault.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = PvPrimary,
    onPrimary = Color.White,
    secondary = PvPrimaryDark,
    background = PvBackground,
    surface = PvSurface,
    error = PvError
)

private val DarkColors = darkColorScheme(
    primary = PvPrimary,
    onPrimary = Color.White,
    secondary = PvPrimaryDark,
    background = Color(0xFF15131F),
    surface = Color(0xFF1E1B2C),
    error = PvError
)

@Composable
fun PromptVaultTheme(darkTheme: Boolean = false, content: @Composable () -> Unit) {
    val colors = if (darkTheme) DarkColors else LightColors
    MaterialTheme(colorScheme = colors, content = content)
}
