package com.elprompter.promptvault.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf

private val LightColors = lightColorScheme(
    primary = Pine,
    onPrimary = CardPaper,
    primaryContainer = PineContainer,
    onPrimaryContainer = Pine,
    secondary = Stamp,
    onSecondary = CardPaper,
    secondaryContainer = StampContainer,
    onSecondaryContainer = Stamp,
    tertiary = Amber,
    onTertiary = CardPaper,
    tertiaryContainer = AmberContainer,
    onTertiaryContainer = Amber,
    background = Kraft,
    onBackground = Ink,
    surface = CardPaper,
    onSurface = Ink,
    surfaceVariant = Kraft,
    onSurfaceVariant = InkFaint,
    error = Rust,
    onError = CardPaper,
    errorContainer = StampContainer,
    onErrorContainer = Rust,
    outline = HairlineInk,
    outlineVariant = HairlineInk,
    inverseSurface = Ink,
    inverseOnSurface = CardPaper,
    inversePrimary = PineLight,
    scrim = Ink
)

/**
 * Skema gelap "ultra premium": tiga lapisan permukaan (background < surface <
 * surfaceVariant sebagai "raised") supaya kartu/sheet terasa mengambang, bukan
 * menyatu jadi hitam polos. Semua aksen dicerahkan (PineGlow/StampGlow/AmberGlow)
 * biar tetap hidup & kontras di atas dasar nyaris-hitam, alih-alih warna terang
 * yang justru pudar/kusam kalau ditaruh apa adanya di dark mode.
 *
 * v2.3.0: primaryContainer/secondaryContainer/tertiaryContainer sebelumnya TIDAK
 * pernah diisi eksplisit -> Compose diam-diam memakai default ungu Material
 * bawaan untuk peran itu. Sekarang semua diisi dari palet brand sendiri, supaya
 * komponen manapun yang memakai peran "container" tetap konsisten dengan tema
 * arsip/premium, bukan bocor jadi ungu generik.
 */
private val DarkColors = darkColorScheme(
    primary = PineGlow,
    onPrimary = ObsidianBase,
    primaryContainer = PineGlowContainer,
    onPrimaryContainer = PineGlow,
    secondary = StampGlow,
    onSecondary = ObsidianBase,
    secondaryContainer = StampGlowContainer,
    onSecondaryContainer = StampGlow,
    tertiary = AmberGlow,
    onTertiary = ObsidianBase,
    tertiaryContainer = AmberGlowContainer,
    onTertiaryContainer = AmberGlow,
    background = ObsidianBase,
    onBackground = IvoryText,
    surface = ObsidianSurface,
    onSurface = IvoryText,
    surfaceVariant = ObsidianSurfaceRaised,
    onSurfaceVariant = IvoryTextFaint,
    error = RustGlow,
    onError = ObsidianBase,
    errorContainer = StampGlowContainer,
    onErrorContainer = RustGlow,
    outline = HairlineIvory,
    outlineVariant = HairlineIvory,
    inverseSurface = IvoryText,
    inverseOnSurface = ObsidianBase,
    inversePrimary = Pine,
    scrim = ObsidianBase
)

/**
 * Aksen ke-4 di luar peran Material3 baku (primary/secondary/tertiary/error) --
 * dipakai khusus untuk "Pengaturan" supaya menu grouped-list punya 4 warna
 * berbeda, bukan cuma 2-3 warna diulang. Theme-aware lewat CompositionLocal
 * yang sama seperti MaterialTheme.colorScheme, jadi otomatis ikut terang/gelap
 * tanpa perlu parameter tambahan di tiap composable pemanggil.
 */
data class VaultExtraColors(
    val slate: androidx.compose.ui.graphics.Color,
    val slateContainer: androidx.compose.ui.graphics.Color
)

private val LightExtraColors = VaultExtraColors(slate = Slate, slateContainer = SlateContainer)
private val DarkExtraColors = VaultExtraColors(slate = SlateGlow, slateContainer = SlateGlowContainer)

val LocalVaultExtraColors = staticCompositionLocalOf { LightExtraColors }

object VaultTheme {
    val extraColors: VaultExtraColors
        @Composable get() = LocalVaultExtraColors.current
}

@Composable
fun PromptVaultTheme(darkTheme: Boolean = false, content: @Composable () -> Unit) {
    val colors = if (darkTheme) DarkColors else LightColors
    val extraColors = if (darkTheme) DarkExtraColors else LightExtraColors
    androidx.compose.runtime.CompositionLocalProvider(LocalVaultExtraColors provides extraColors) {
        MaterialTheme(colorScheme = colors, typography = PromptVaultTypography, shapes = PromptVaultShapes, content = content)
    }
}
