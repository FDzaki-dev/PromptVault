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
