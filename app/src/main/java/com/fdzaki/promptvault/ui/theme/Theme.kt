package com.fdzaki.promptvault.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val VaultTeal = Color(0xFF1E7D6B)
private val VaultTealDark = Color(0xFF57C4AB)
private val VaultAmber = Color(0xFFE0A93E)
private val VaultBackgroundDark = Color(0xFF12181A)
private val VaultBackgroundLight = Color(0xFFF6F8F7)

private val DarkColors = darkColorScheme(
    primary = VaultTealDark,
    secondary = VaultAmber,
    background = VaultBackgroundDark,
    surface = Color(0xFF1B2224)
)

private val LightColors = lightColorScheme(
    primary = VaultTeal,
    secondary = VaultAmber,
    background = VaultBackgroundLight,
    surface = Color.White
)

@Composable
fun PromptVaultTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkColors else LightColors
    MaterialTheme(
        colorScheme = colors,
        typography = MaterialTheme.typography,
        content = content
    )
}
