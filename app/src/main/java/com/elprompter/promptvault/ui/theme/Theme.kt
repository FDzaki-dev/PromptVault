package com.elprompter.promptvault.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * v3.0.0 — Dark mode adalah SATU-SATUNYA skema yang ada (AMOLED Glassmorphism
 * Hybrid). Skema terang lama DIHAPUS TOTAL -- bukan disembunyikan, tapi
 * benar-benar tidak ada lagi jalur kode yang bisa membuat komponen manapun
 * jatuh ke tampilan terang/neumorphic cerah.
 *
 * v2.16.0 -- parameter `darkTheme: Boolean` yang SENGAJA diabaikan dihapus
 * total (technical debt closure, bukan cuma dibiarkan mati): sebelumnya
 * dipertahankan "supaya pemanggil lama tidak perlu diubah", tapi itu
 * sendiri jadi bug-in-waiting -- signature fungsi berbohong soal
 * mengizinkan tema terang padahal tidak. `ThemeMode`/opsi "Terang"/"Ikuti
 * Sistem" juga sudah dihapus dari `SettingsRepository` & `SettingsScreen`
 * di batch yang sama; lihat CHANGELOG v2.16.0.
 *
 * v4.0.0 -- gradient tint "Midnight Blue" diganti "Transformative Teal"
 * (biru-hijau gelap), token `MidnightBlueAccent*` -> `TealAccent*` di
 * Color.kt (rename + re-hex, bukan cuma rename kosong). Lihat CHANGELOG.
 *
 * v6.0.0 -- Re-palette "Transformative Teal" -> "Platinum + Ruby" (lihat
 * javadoc lengkap di Color.kt). `onSecondary`/`onSecondaryContainer`
 * SENGAJA TIDAK reuse `PlatinumAccentOn` lagi (beda dari pola lama yang
 * reuse `TealAccentOn` utk semua *On) -- `RubyOn` baru (terang) dipasang
 * krn `RubyGlow` cukup jenuh/gelap-value shg teks terang kontrasnya lebih
 * baik drpd teks gelap, lihat javadoc Color.kt.
 */
private val VaultDarkColors = darkColorScheme(
    primary = PlatinumAccent,
    onPrimary = PlatinumAccentOn,
    primaryContainer = PlatinumAccentContainer,
    onPrimaryContainer = PlatinumAccent,
    secondary = RubyGlow,
    onSecondary = RubyOn,
    secondaryContainer = RubyGlowContainer,
    onSecondaryContainer = RubyOn,
    tertiary = AmberGlow,
    onTertiary = PlatinumAccentOn,
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
    inversePrimary = PlatinumAccent,
    error = RustGlow,
    onError = PlatinumAccentOn,
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
