package com.elprompter.promptvault.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * v8.0.0 — Skala tipografi BAKU Material 3 (15 style resmi spec M3: ukuran/
 * line-height/tracking/weight PERSIS nilai default M3), menggantikan gaya
 * kustom "Apple large title" (judul besar-tebal-rapat ala SF Pro) dari
 * versi sebelumnya -- itu gaya iOS, bukan M3 murni. `FontFamily.Default`
 * (Roboto di Android, font sistem BAKU M3 -- bukan font kustom) dipakai
 * apa adanya, tanpa override letter-spacing/weight non-standar.
 *
 * `CodeFont` (Monospace) DIPERTAHANKAN -- dipakai eksplisit hanya utk
 * elemen ala kode (pattern rule, nama file di RuleCard.kt), di luar cakupan
 * "murni M3" (M3 tidak melarang monospace utk konten teknis, hanya
 * mengatur skala type role default).
 */
private val Sans = FontFamily.Default
val CodeFont = FontFamily.Monospace

val PromptVaultTypography = Typography(
    displayLarge = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Normal, fontSize = 57.sp, lineHeight = 64.sp, letterSpacing = (-0.25).sp),
    displayMedium = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Normal, fontSize = 45.sp, lineHeight = 52.sp, letterSpacing = 0.sp),
    displaySmall = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Normal, fontSize = 36.sp, lineHeight = 44.sp, letterSpacing = 0.sp),
    headlineLarge = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Normal, fontSize = 32.sp, lineHeight = 40.sp, letterSpacing = 0.sp),
    headlineMedium = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Normal, fontSize = 28.sp, lineHeight = 36.sp, letterSpacing = 0.sp),
    headlineSmall = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Normal, fontSize = 24.sp, lineHeight = 32.sp, letterSpacing = 0.sp),
    titleLarge = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Normal, fontSize = 22.sp, lineHeight = 28.sp, letterSpacing = 0.sp),
    titleMedium = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Medium, fontSize = 16.sp, lineHeight = 24.sp, letterSpacing = 0.15.sp),
    titleSmall = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.1.sp),
    labelLarge = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.1.sp),
    labelMedium = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.5.sp),
    labelSmall = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 16.sp, letterSpacing = 0.5.sp),
    bodyLarge = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp, letterSpacing = 0.5.sp),
    bodyMedium = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.25.sp),
    bodySmall = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.4.sp)
)

/**
 * (lanjutan restyling Cupertino murni, 2026-08-27) Skala "iOS-ish" (angka
 * dari HIG Apple, size class Large -- publik, bukan hasil scraping/reverse-
 * engineer aset berlisensi) -- HANYA dipakai saat `ThemeStyleOption.CUPERTINO`
 * aktif (branch di `Theme.kt`, pola identik `CupertinoShapes`). 3 gaya lain
 * TETAP pakai `PromptVaultTypography` di atas, 0 satu baris pun diubah di sana.
 *
 * `FontFamily.Default` (Roboto) TETAP dipakai, BUKAN font SF Pro kustom --
 * di luar scope (app tidak bundling font pihak ketiga). "iOS-ish" di sini
 * murni dari METRIK (ukuran/weight/tracking), bukan glyph -- beda dari gaya
 * kustom "Apple large title" pra-v8.0.0 (lihat javadoc atas) yang cuma
 * override 1 role; di sini SELURUH 15 role dipetakan.
 *
 * Ciri HIG yang SENGAJA dipertahankan (bukan disamaratakan/linear kayak M3):
 * tracking POSITIF tipis di size besar (Large Title 34sp..Title 3 20sp,
 * +0.35..+0.38sp), lalu NEGATIF di size "workhorse" 15-17sp (Headline/Body/
 * Callout, -0.24..-0.41sp -- inilah kesan "rapat" khas SF Pro), balik
 * mendekati 0/positif lagi di size kecil (Footnote/Caption, -0.08..+0.07sp).
 * Hirarki UKURAN per role M3 (mis. headlineLarge >= headlineMedium >=
 * headlineSmall) TETAP dijaga non-menurun spy tidak ada asumsi call site
 * yang jebol, walau beberapa role sengaja reuse size HIG yang sama (HIG
 * cuma py 11 role, M3 py 15 slot).
 */
val CupertinoTypography = Typography(
    displayLarge = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Bold, fontSize = 34.sp, lineHeight = 41.sp, letterSpacing = 0.37.sp),          // Large Title
    displayMedium = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Normal, fontSize = 28.sp, lineHeight = 34.sp, letterSpacing = 0.36.sp),       // Title 1
    displaySmall = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Normal, fontSize = 22.sp, lineHeight = 28.sp, letterSpacing = 0.35.sp),        // Title 2
    headlineLarge = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Normal, fontSize = 20.sp, lineHeight = 25.sp, letterSpacing = 0.38.sp),       // Title 3
    headlineMedium = TextStyle(fontFamily = Sans, fontWeight = FontWeight.SemiBold, fontSize = 17.sp, lineHeight = 22.sp, letterSpacing = (-0.41).sp), // Headline
    headlineSmall = TextStyle(fontFamily = Sans, fontWeight = FontWeight.SemiBold, fontSize = 17.sp, lineHeight = 22.sp, letterSpacing = (-0.41).sp),  // Headline (HIG cuma 1 varian)
    titleLarge = TextStyle(fontFamily = Sans, fontWeight = FontWeight.SemiBold, fontSize = 17.sp, lineHeight = 22.sp, letterSpacing = (-0.41).sp),     // Headline, dipakai nav/top bar title
    titleMedium = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Normal, fontSize = 17.sp, lineHeight = 22.sp, letterSpacing = (-0.41).sp),      // Body
    titleSmall = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 21.sp, letterSpacing = (-0.32).sp),       // Callout
    labelLarge = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Medium, fontSize = 15.sp, lineHeight = 20.sp, letterSpacing = (-0.24).sp),       // Subheadline
    labelMedium = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Medium, fontSize = 13.sp, lineHeight = 18.sp, letterSpacing = (-0.08).sp),      // Footnote
    labelSmall = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 13.sp, letterSpacing = 0.07.sp),          // Caption 2
    bodyLarge = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Normal, fontSize = 17.sp, lineHeight = 22.sp, letterSpacing = (-0.41).sp),        // Body
    bodyMedium = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Normal, fontSize = 15.sp, lineHeight = 20.sp, letterSpacing = (-0.24).sp),       // Subheadline
    bodySmall = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.sp)                // Caption 1
)
