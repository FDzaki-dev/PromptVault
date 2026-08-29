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
 * (2026-08-28) Skema warna khusus gaya CUPERTINO -- tutup item TERAKHIR
 * dari 3 pending restyling Cupertino murni (lihat javadoc lengkap hue di
 * `Color.kt` & progres di `CupertinoTokens.kt`). Neutral/background/surface
 * SENGAJA 100% REUSE dari [PromptVaultColors] (0 token baru) -- "warna
 * sistem" cuma soal 4 slot AKSEN (primary/secondary/tertiary/error),
 * bukan rombak background; shape ([CupertinoShapes]) & typography
 * ([CupertinoTypography]) yang sudah ada di batch sebelumnya SUDAH cukup
 * membedakan identitas visual Cupertino tanpa perlu background terpisah.
 */
private val CupertinoColors: ColorScheme = darkColorScheme(
    primary = CupertinoBlue,
    onPrimary = CupertinoOnBlue,
    primaryContainer = CupertinoBlueContainer,
    onPrimaryContainer = CupertinoOnBlueContainer,
    secondary = CupertinoTeal,
    onSecondary = CupertinoOnTeal,
    secondaryContainer = CupertinoTealContainer,
    onSecondaryContainer = CupertinoOnTealContainer,
    tertiary = CupertinoOrange,
    onTertiary = CupertinoOnOrange,
    tertiaryContainer = CupertinoOrangeContainer,
    onTertiaryContainer = CupertinoOnOrangeContainer,
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
    inversePrimary = CupertinoBlue,
    error = CupertinoRed,
    onError = CupertinoOnRed,
    errorContainer = CupertinoRedContainer,
    onErrorContainer = CupertinoOnRedContainer,
    outline = Outline,
    outlineVariant = OutlineVariant,
    scrim = Color.Black
)

/**
 * (2026-08-29) Skema warna khusus gaya NEUMORPHISM -- "Teal & Amber (Blade
 * Runner)", permintaan eksplisit user (lihat javadoc lengkap hue/kontras di
 * `Color.kt`). Pola IDENTIK [CupertinoColors] di atas: neutral/background/
 * surface/error/outline 100% REUSE dari [PromptVaultColors] (0 token baru)
 * -- cuma 3 slot aksen (primary/secondary/tertiary) yang diganti.
 */
private val NeumorphismColors: ColorScheme = darkColorScheme(
    primary = NeoTeal,
    onPrimary = NeoOnTeal,
    primaryContainer = NeoTealContainer,
    onPrimaryContainer = NeoOnTealContainer,
    secondary = NeoTealDeep,
    onSecondary = NeoOnTealDeep,
    secondaryContainer = NeoTealDeepContainer,
    onSecondaryContainer = NeoOnTealDeepContainer,
    tertiary = NeoAmber,
    onTertiary = NeoOnAmber,
    tertiaryContainer = NeoAmberContainer,
    onTertiaryContainer = NeoOnAmberContainer,
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
    inversePrimary = NeoTeal,
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
 *
 * (2026-08-28) `CupertinoExtra` -- varian kedua, `slate` diganti
 * [CupertinoIndigo] (systemIndigo) khusus gaya Cupertino, pola identik
 * `CupertinoColors` di atas. 3 gaya lain (Glass/Neumorphism/M3) tetap
 * pakai `VaultExtra` lama, 0 berubah.
 *
 * (2026-08-29, lanjutan sesi Blade Runner) `NeumorphismExtra` -- varian
 * ketiga, `slate` diganti [NeoMagenta] (neon magenta/pink BR2049, dipilih
 * user dari 4 opsi) khusus gaya Neumorphism, pola identik 2 varian di
 * atas. Javadoc lama [PromptVaultTheme] yg bilang aksen ke-4 "TETAP TIDAK
 * berubah utk NEUMORPHISM" SEKARANG usang -- diperbarui di sana, bukan
 * dihapus.
 */
data class VaultExtraColors(
    val slate: Color,
    val slateContainer: Color
)

private val VaultExtra = VaultExtraColors(slate = SettingsAccent, slateContainer = SettingsAccentContainer)
private val CupertinoExtra = VaultExtraColors(slate = CupertinoIndigo, slateContainer = CupertinoIndigoContainer)
private val NeumorphismExtra = VaultExtraColors(slate = NeoMagenta, slateContainer = NeoMagentaContainer)

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
 *
 * (fase terakhir restyling Cupertino murni, 2026-08-28) `colorScheme` &
 * `LocalVaultExtraColors` KINI juga KONDISIONAL, pola identik persis 2
 * baris di atas -- Cupertino pakai [CupertinoColors]/`CupertinoExtra`
 * (warna sistem iOS: systemBlue/Teal/Orange/Red/Indigo), 3 gaya lain TETAP
 * [PromptVaultColors]/`VaultExtra` (M3 calm biru lama, 0 berubah). Ini
 * MENUTUP SEMUA 3 item pending restyling Cupertino murni (typography,
 * custom dialog, warna sistem) -- lihat [CupertinoTokens] utk daftar
 * lengkap progres.
 *
 * (2026-08-29, permintaan eksplisit user) `colorScheme` sekarang 3-cabang
 * (bukan cuma cabang boolean `isCupertino`) -- NEUMORPHISM dapat skema
 * warna sendiri, [NeumorphismColors] ("Teal & Amber (Blade Runner)", lihat
 * javadoc lengkap di [NeumorphismColors] & `Color.kt`). `shapes`/
 * `typography`/`LocalVaultExtraColors` TIDAK ikut berubah utk NEUMORPHISM
 * (TETAP [PromptVaultShapes]/[PromptVaultTypography]/`VaultExtra`, sama
 * persis GLASSMORPHISM/MATERIAL3) -- user cuma minta "kombinasi warna",
 * bukan rombak shape/tipografi/aksen ke-4 spt restyling Cupertino murni di
 * atas; scope sengaja dijaga sempit sesuai yang diminta.
 *
 * (2026-08-29, lanjutan sesi sama) SUSUL: `shapes`/`typography` KINI *juga*
 * 3-cabang, pola PERSIS `colorScheme` di atas -- NEUMORPHISM dapat
 * [NeumorphismShapes] (`Shapes.kt`, keluarga `CutCornerShape` -- sudut
 * potong tegas, kebalikan arah bulat/lembut Cupertino) & [NeumorphismTypography]
 * (`Type.kt`, [CodeFont] monospace+tebal+tracking lebar utk role
 * display/headline/label -- kesan "signage/HUD terminal" khas Blade
 * Runner). Paragraf di atas ("shapes/typography TIDAK ikut berubah")
 * SEKARANG USANG -- diperbarui di sini alih-alih dihapus, konsisten pola
 * arsip riwayat di seluruh file ini. Trigger: user eksplisit minta
 * "shape/typography ala Blade Runner, seperti tema warnanya" (menyusul
 * `NeumorphismColors` yang sudah lebih dulu ada) -- scope sekarang
 * melengkapi 2 sumbu identitas visual yang tadinya sengaja ditunda.
 * `LocalVaultExtraColors` (aksen ke-4 "Pengaturan") TETAP TIDAK berubah --
 * di luar cakupan permintaan sesi ini (cuma shape+tipografi), tetap reuse
 * `VaultExtra` sama seperti GLASSMORPHISM/MATERIAL3.
 *
 * (2026-08-29, sesi lanjutan lagi) SUSUL LAGI, MENUTUP 4/4: aksen ke-4
 * (`LocalVaultExtraColors`) SEKARANG *juga* 3-cabang, pola PERSIS 3 baris
 * di atas -- NEUMORPHISM dapat `NeumorphismExtra` (slot "Pengaturan" ->
 * [NeoMagenta], neon magenta/pink BR2049, lihat javadoc lengkap di
 * `Color.kt`). Paragraf tepat di atas ("LocalVaultExtraColors TETAP TIDAK
 * berubah") SEKARANG jg usang -- diperbarui di sini. Dengan ini SEMUA 4
 * sumbu identitas visual Neumorphism (warna, shape, tipografi, aksen ke-4)
 * sudah lengkap kondisional per `themeStyle`, menutup permintaan
 * bertahap sepanjang sesi hari ini. Var lokal `isCupertino` (dipakai
 * ternary boolean lama) DIHAPUS -- sudah 0 pemakai lagi stlh baris
 * terakhir ini ikut pindah ke `when`.
 *
 * (2026-08-29, sesi baru) `typography` SEKARANG bercabang 4-arah (bukan 3)
 * -- GLASSMORPHISM dapat asetnya sendiri, [GlassTypography] (`Type.kt`,
 * javadoc lengkap di sana termasuk catatan SUPERSEDE thd syarat lama
 * `GlassTokens.kt` v8.23.0 yg pernah bilang typography Glass tidak boleh
 * disentuh). MATERIAL3 sekarang SATU-SATUNYA gaya yg pakai
 * [PromptVaultTypography] via `else` (sebelumnya GLASSMORPHISM ikut
 * numpang cabang `else` yg sama, sekarang sudah dipisah).
 * `colorScheme`/`shapes`/`extraColors` GLASSMORPHISM TIDAK ikut berubah
 * sesi ini -- scope sengaja cuma typography sesuai yg diminta.
 */
@Composable
fun PromptVaultTheme(themeStyle: ThemeStyleOption = ThemeStyleOption.GLASSMORPHISM, content: @Composable () -> Unit) {
    val colorScheme = when (themeStyle) {
        ThemeStyleOption.CUPERTINO -> CupertinoColors
        ThemeStyleOption.NEUMORPHISM -> NeumorphismColors
        else -> PromptVaultColors
    }
    val typography = when (themeStyle) {
        ThemeStyleOption.CUPERTINO -> CupertinoTypography
        ThemeStyleOption.NEUMORPHISM -> NeumorphismTypography
        ThemeStyleOption.GLASSMORPHISM -> GlassTypography
        else -> PromptVaultTypography
    }
    val shapes = when (themeStyle) {
        ThemeStyleOption.CUPERTINO -> CupertinoShapes
        ThemeStyleOption.NEUMORPHISM -> NeumorphismShapes
        else -> PromptVaultShapes
    }
    val extraColors = when (themeStyle) {
        ThemeStyleOption.CUPERTINO -> CupertinoExtra
        ThemeStyleOption.NEUMORPHISM -> NeumorphismExtra
        else -> VaultExtra
    }
    CompositionLocalProvider(
        LocalVaultExtraColors provides extraColors,
        LocalThemeStyle provides themeStyle
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = typography,
            shapes = shapes,
            content = content
        )
    }
}
