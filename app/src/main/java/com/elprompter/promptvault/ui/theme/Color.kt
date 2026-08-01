package com.elprompter.promptvault.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Palet TERANG "Manifest Arsip": kertas kraft + tinta arsip + stempel manifest.
 * v2.3.0: ditambah aksen ke-4 (Slate) supaya menu list tidak monoton hijau,
 * plus warna "container" lembut untuk tiap aksen (dipakai di chip ikon &
 * gradient kartu) supaya ada variasi kedalaman visual, bukan blok warna rata.
 */
val Kraft = Color(0xFFEADFC5)
val KraftWash = Color(0xFFF2E9D2)     // sedikit lebih terang, dipakai untuk gradient wash atas layar
val CardPaper = Color(0xFFF7F1E2)
val CardPaperWash = Color(0xFFFDFAF1) // lapisan atas kartu untuk gradient halus
val Ink = Color(0xFF26302A)
val InkFaint = Color(0xFF5B685F)
val Pine = Color(0xFF34523C)
val PineLight = Color(0xFF4C7057)
val PineContainer = Color(0xFFD8E6DA)
val Stamp = Color(0xFFB1432E)
val StampContainer = Color(0xFFF3D9CF)
val Amber = Color(0xFFC6862F)
val AmberContainer = Color(0xFFF3E1C0)
val Rust = Color(0xFF8C3626)
val Slate = Color(0xFF3B5E74)          // aksen ke-4: biru batu tenang, untuk "Pengaturan"
val SlateContainer = Color(0xFFD6E3EA)
val HairlineInk = Color(0x2626302A)

/**
 * Palet GELAP "ultra premium" -- bukan sekadar invert warna terang. Prinsipnya
 * sama seperti dark mode iOS: lapisan permukaan (elevation) dibedakan lewat
 * tingkat kecerahan abu-abu gelap yang halus (bukan hitam pekat rata semua),
 * aksen dicerahkan secukupnya supaya tetap enak dipandang & kontras di layar
 * OLED, teks utama nyaris putih hangat (bukan putih murni yang menyilaukan).
 *
 * v2.3.0: menu & kartu sebelumnya nyaris monokrom (hijau di mana-mana di atas
 * hitam polos). Sekarang tiap aksen benar-benar dipakai bergantian (hijau,
 * amber, biru batu, merah stempel) + ditambah warna "wash" tipis untuk
 * gradient latar & kartu supaya permukaan gelap terasa berlapis, bukan datar.
 */
val ObsidianBase = Color(0xFF0B0C0B)      // background dasar, nyaris hitam OLED
val ObsidianWash = Color(0xFF141C17)      // wash gradient atas layar, sedikit rona hijau gelap
val ObsidianSurface = Color(0xFF171816)   // permukaan kartu, satu "lapisan" lebih terang
val ObsidianSurfaceWash = Color(0xFF1E211D) // lapisan atas kartu untuk gradient halus
val ObsidianSurfaceRaised = Color(0xFF212220) // permukaan sheet/dialog, lapisan lebih terang lagi
val IvoryText = Color(0xFFF3EFE4)         // teks utama, putih hangat (bukan putih pekat)
val IvoryTextFaint = Color(0xFF9C9A90)    // teks sekunder
val PineGlow = Color(0xFF5FCB8B)          // aksen hijau dicerahkan biar hidup di gelap
val PineGlowContainer = Color(0xFF1E2E22)
val StampGlow = Color(0xFFFF6E52)         // aksen merah stempel dicerahkan
val StampGlowContainer = Color(0xFF33201A)
val AmberGlow = Color(0xFFE8AC4E)         // aksen amber dicerahkan
val AmberGlowContainer = Color(0xFF302315)
val RustGlow = Color(0xFFFF6B5C)          // error dicerahkan
val SlateGlow = Color(0xFF7CB3D4)         // aksen ke-4: biru batu dicerahkan, untuk "Pengaturan"
val SlateGlowContainer = Color(0xFF1A262E)
val HairlineIvory = Color(0x1FF3EFE4)     // border tipis translusen di atas gelap
