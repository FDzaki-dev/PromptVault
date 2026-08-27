package com.elprompter.promptvault.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.elprompter.promptvault.data.ThemeStyleOption

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

/** (v8.23.2) Gaya visual aktif (`TactileSurface` konsumsi ini) -- default GLASSMORPHISM, sama seperti v8.23.1 sebelum toggle nyata ada. */
val LocalThemeStyle = staticCompositionLocalOf { ThemeStyleOption.GLASSMORPHISM }

object VaultTheme {
    val extraColors: VaultExtraColors
        @Composable get() = LocalVaultExtraColors.current

    val style: ThemeStyleOption
        @Composable get() = LocalThemeStyle.current
}

/**
 * (v8.23.2) Parameter `themeStyle` baru -- default `GLASSMORPHISM` supaya
 * call site lama (`MainActivity.kt` sebelum wiring) tetap valid tanpa
 * ubah signature secara breaking. Pemanggil sekarang (`MainActivity.kt`)
 * mengirim nilai nyata dari `MainViewModel.themeStyle` (DataStore).
 *
 * (v8.31.2) `shapes` sekarang KONDISIONAL thd `themeStyle` -- gaya
 * Cupertino (rename dari HYBRID di v8.31.4) pakai [CupertinoShapes] (radius
 * lebih besar, kesan Cupertino), gaya lain TETAP [PromptVaultShapes] (skala
 * M3 baku, TIDAK berubah). Lihat javadoc lengkap alasan teknik ini (bukan
 * override per-cabang di `TactileSurface`) di [CupertinoShapes].
 *
 * (lanjutan restyling Cupertino murni, 2026-08-27) `typography` KINI juga
 * KONDISIONAL, pola identik persis baris `shapes` di atas -- Cupertino
 * pakai [CupertinoTypography] (skala HIG iOS-ish), 3 gaya lain TETAP
 * [PromptVaultTypography] (M3 baku, 0 berubah). Lihat javadoc lengkap di
 * [CupertinoTypography] (`Type.kt`).
 */
@Composable
fun PromptVaultTheme(themeStyle: ThemeStyleOption = ThemeStyleOption.GLASSMORPHISM, content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalVaultExtraColors provides VaultExtra,
        LocalThemeStyle provides themeStyle
    ) {
        MaterialTheme(
            colorScheme = PromptVaultColors,
            typography = if (themeStyle == ThemeStyleOption.CUPERTINO) CupertinoTypography else PromptVaultTypography,
            shapes = if (themeStyle == ThemeStyleOption.CUPERTINO) CupertinoShapes else PromptVaultShapes,
            content = content
        )
    }
}
