package com.elprompter.promptvault.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Palet TERANG "Manifest Arsip": kertas kraft + tinta arsip + stempel manifest.
 */
val Kraft = Color(0xFFEADFC5)
val CardPaper = Color(0xFFF7F1E2)
val Ink = Color(0xFF26302A)
val InkFaint = Color(0xFF5B685F)
val Pine = Color(0xFF34523C)
val PineLight = Color(0xFF4C7057)
val Stamp = Color(0xFFB1432E)
val Amber = Color(0xFFC6862F)
val Rust = Color(0xFF8C3626)
val HairlineInk = Color(0x2626302A)

/**
 * Palet GELAP "ultra premium" -- bukan sekadar invert warna terang. Prinsipnya
 * sama seperti dark mode iOS: lapisan permukaan (elevation) dibedakan lewat
 * tingkat kecerahan abu-abu gelap yang halus (bukan hitam pekat rata semua),
 * aksen dicerahkan secukupnya supaya tetap enak dipandang & kontras di layar
 * OLED, teks utama nyaris putih hangat (bukan putih murni yang menyilaukan).
 */
val ObsidianBase = Color(0xFF0B0C0B)      // background dasar, nyaris hitam OLED
val ObsidianSurface = Color(0xFF171816)   // permukaan kartu, satu "lapisan" lebih terang
val ObsidianSurfaceRaised = Color(0xFF212220) // permukaan sheet/dialog, lapisan lebih terang lagi
val IvoryText = Color(0xFFF3EFE4)         // teks utama, putih hangat (bukan putih pekat)
val IvoryTextFaint = Color(0xFF9C9A90)    // teks sekunder
val PineGlow = Color(0xFF5FCB8B)          // aksen hijau dicerahkan biar hidup di gelap
val StampGlow = Color(0xFFFF6E52)         // aksen merah stempel dicerahkan
val AmberGlow = Color(0xFFE8AC4E)         // aksen amber dicerahkan
val RustGlow = Color(0xFFFF6B5C)          // error dicerahkan
val HairlineIvory = Color(0x1FF3EFE4)     // border tipis translusen di atas gelap
