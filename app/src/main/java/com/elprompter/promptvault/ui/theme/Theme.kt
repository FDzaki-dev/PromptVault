package com.elprompter.promptvault.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * v3.0.0 — Dark mode adalah SATU-SATUNYA skema yang ada (AMOLED Glassmorphism
 * Hybrid + Midnight Blue gradient tint). Skema terang lama DIHAPUS TOTAL --
 * bukan disembunyikan, tapi benar-benar tidak ada lagi jalur kode yang bisa
 * membuat komponen manapun jatuh ke tampilan terang/neumorphic cerah.
 *
 * `darkTheme` tetap diterima sebagai parameter supaya pemanggil lama
 * (MainActivity, yang membaca preferensi ThemeMode user) tidak perlu diubah,
 * tapi nilainya SENGAJA diabaikan -- lihat DoD spesifikasi: "Dark Mode is
 * the mandatory default visual system" & "No component may silently fall
 * back to a bright/light neumorphic appearance". Opsi "Terang"/"Ikuti
 * Sistem" di menu Pengaturan saat ini tidak lagi mengubah tampilan; ini
 * dicatat sebagai known-limitation di PROJECT_STATE.md untuk pembersihan UI
 * lanjutan (bukan bagian dari batch tema ini).
 */
private val VaultDarkColors = darkColorScheme(
    primary = MidnightBlueAccent,
    onPrimary = MidnightBlueAccentOn,
    primaryContainer = MidnightBlueAccentContainer,
    onPrimaryContainer = MidnightBlueAccent,
    secondary = StampGlow,
    onSecondary = MidnightBlueAccentOn,
    secondaryContainer = StampGlowContainer,
    onSecondaryContainer = StampGlow,
    tertiary = AmberGlow,
    onTertiary = MidnightBlueAccentOn,
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
    inversePrimary = MidnightBlueAccent,
    error = RustGlow,
    onError = MidnightBlueAccentOn,
    errorContainer = RustGlowContainer,
    onErrorContainer = RustGlow,
    outline = HairlineGlass,
    outlineVariant = HairlineGlass,
    scrim = GlassShadow
)

/**
 * Aksen ke-4 di luar peran Material3 baku, dipakai khusus untuk "Pengaturan"
 * supaya grouped-list tetap punya identitas warna berbeda per menu. Tidak
 * lagi punya varian terang -- theme-aware lewat CompositionLocal yang sama,
 * tapi isinya konstan mengikuti satu-satunya skema gelap yang ada.
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
fun PromptVaultTheme(darkTheme: Boolean = true, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalVaultExtraColors provides VaultExtra) {
        MaterialTheme(
            colorScheme = VaultDarkColors,
            typography = PromptVaultTypography,
            shapes = PromptVaultShapes,
            content = content
        )
    }
}
