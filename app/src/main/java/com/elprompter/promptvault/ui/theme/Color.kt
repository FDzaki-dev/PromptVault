package com.elprompter.promptvault.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * v8.0.0 — ROMBAK TOTAL (permintaan eksplisit user, sesi ini): seluruh
 * palet Glassmorphism kustom (Deep Navy+Brass / Charcoal+Copper, v7.x)
 * DIHAPUS TOTAL, diganti SATU skema tonal Material 3 murni. 3 syarat
 * eksplisit user, semua ditelusuri di bawah:
 * 1. "default Material 3 murni" -- peran warna & tingkat elevasi permukaan
 *    di bawah adalah peran BAKU M3 (primary/secondary/tertiary/error +
 *    5-tingkat surfaceContainer), BUKAN token kustom ber-merek ("Glass*",
 *    "Brass*") seperti sebelumnya. Toggle preset ganda (useAltTheme, 2
 *    hue eksotis) ikut DIHAPUS (lihat Theme.kt) -- "pure default" berarti
 *    SATU identitas warna, bukan 2 preset kustom untuk dipilih.
 * 2. "base warna calm bukan warm" -- seed hue dasar (neutral+primary) =
 *    H222 (BIRU, cool). Preset lama v7.1.0 "Charcoal+Copper" (H30, EKSPLISIT
 *    hangat, lihat riwayat git) adalah pelanggaran langsung syarat ini --
 *    salah satu alasan kenapa dihapus, bukan direvisi.
 * 3. "tetap sesuai standar WCAG" -- SEMUA pasangan teks/ikon dihitung manual
 *    (formula relative luminance W3C, sama seperti fix 2026-08-16
 *    sebelumnya) sebelum di-commit, lihat comment kontras di tiap grup di
 *    bawah. Teks/UI SELALU >=4.5:1 (teks) / >=3:1 (batas grafis non-teks,
 *    1.4.11), diverifikasi worst-case di TINGKAT PERMUKAAN PALING TERANG
 *    (margin kontras paling kecil), pola yang sama dipertahankan dari
 *    audit WCAG sebelumnya.
 *
 * Warna semantik status (tertiary=warning, error) SENGAJA TIDAK ikut hue
 * calm murni -- amber utk warning & merah utk error adalah konvensi
 * universal, dan porsinya kecil/aksen-saja (bukan "base warna dominan"
 * yang jadi syarat #2). Base/dominan (background, surface 5-tingkat,
 * primary CTA) 100% cool/calm.
 */

// ---- Neutral: root + 5-tingkat surfaceContainer (M3 baku), hue ditarik
// dari primary (H222) supaya "surface tint" kohesif & calm, saturasi
// SANGAT rendah (16%) -- bukan abu netral polos, bukan juga berwarna. ----
val AppBackground = Color(0xFF0D0E12)             // root; splash & status/nav bar (lihat MainActivity)
val SurfaceContainerLowest = Color(0xFF090A0C)    // tingkat paling redup (recessed/track dasar)
val SurfaceContainerLow = Color(0xFF111317)       // == surface, tingkat dasar konten
val SurfaceDefault = Color(0xFF111317)
val SurfaceContainer = Color(0xFF181A20)          // panel kartu (VaultCard)
val SurfaceContainerHigh = Color(0xFF21242B)      // "naik" 1 tingkat (kotak ikon)
val SurfaceContainerHighest = Color(0xFF2D3139)   // sheet/dialog, tingkat PALING TERANG
val SurfaceRecessed = Color(0xFF060709)           // kontrol tenggelam (track switch OFF)

// on-neutral. Worst-case dihitung vs SurfaceContainerHighest (tingkat paling
// terang, margin kontras paling kecil):
// TextPrimary: 11.64:1 (lulus AAA). TextSecondary: 7.54:1 (lulus AAA).
val TextPrimary = Color(0xFFF1F2F4)
val TextSecondary = Color(0xFFC1C5CD)

// outline (batas grafis non-teks, ambang WCAG 1.4.11 = 3:1). Worst-case vs
// SurfaceContainerHighest: 3.25:1, lulus dengan margin wajar.
val Outline = Color(0xFF767F93)
val OutlineVariant = Color(0xFF3D4351)            // divider dekoratif, bukan batas fungsional -- tidak wajib 3:1

// ---- Primary: BIRU calm (H222), CTA & kontrol interaktif utama. Pola dark-
// scheme M3 baku: `primary` tone TERANG (dipakai lgs sbg teks/ikon di atas
// surface gelap), `onPrimary` tone GELAP (teks di atas primary saat jadi
// fill tombol). Kontras: primary vs SurfaceContainerHighest 5.89:1 (teks,
// lulus AA). onPrimary vs primary 7.43:1 (lulus AAA). ----
val Primary = Color(0xFF98AEE1)
val OnPrimary = Color(0xFF171F30)
val PrimaryContainer = Color(0xFF313E5E)
val OnPrimaryContainer = Color(0xFFDFE6F6)        // vs PrimaryContainer: 8.47:1

// ---- Secondary: biru-sian teredam (H200), SENGAJA beda hue dari primary
// (pemisahan peran M3 murni -- v7.x lama reuse primary=secondary, bukan
// pola M3 baku). Kontras: secondary vs SurfaceContainerHighest 6.69:1.
// onSecondary vs secondary 7.33:1. ----
val Secondary = Color(0xFFA8BDC7)
val OnSecondary = Color(0xFF212C31)
val SecondaryContainer = Color(0xFF38464D)
val OnSecondaryContainer = Color(0xFFE0E7EB)      // vs SecondaryContainer: 7.81:1

// ---- Tertiary: amber (H42) -- SATU-SATUNYA hue non-cool di app, dipakai
// KHUSUS semantik warning (porsi kecil, bukan base warna), lihat javadoc
// atas. Kontras: tertiary vs SurfaceContainerHighest 7.30:1. onTertiary vs
// tertiary 8.03:1. ----
val Tertiary = Color(0xFFDABF81)
val OnTertiary = Color(0xFF322915)
val TertiaryContainer = Color(0xFF534628)
val OnTertiaryContainer = Color(0xFFF4EBD7)       // vs TertiaryContainer: 7.79:1

// ---- Error: merah (H8) standar M3. Kontras: error vs
// SurfaceContainerHighest 5.65:1. onError vs error 6.68:1. ----
val ErrorRed = Color(0xFFE4978B)
val OnErrorRed = Color(0xFF391D18)
val ErrorContainer = Color(0xFF59322C)
val OnErrorContainer = Color(0xFFF5DAD6)          // vs ErrorContainer: 8.28:1

// ---- Aksen ke-4 di luar peran M3 baku (khusus menu "Pengaturan", pola
// "sistem 4-aksen" dipertahankan dari versi sebelumnya) -- indigo calm
// (H258), TETAP cool/tidak warm. Kontras vs SurfaceContainerHighest: 5.59:1.
// ----
val SettingsAccent = Color(0xFFB2A1D9)
val SettingsAccentContainer = Color(0xFF332B46)

/**
 * Catatan audit 1.4.11 (container fill vs root background, BUKAN vs
 * surface tempat container itu sendiri dipakai): PrimaryContainer/
 * SecondaryContainer/TertiaryContainer/ErrorContainer/SettingsAccentContainer
 * hanya ~1.8-2.1:1 vs [AppBackground] kalau diukur TANPA konteks. Ini SAMA
 * seperti perilaku skema dark M3 baku manapun (tone container ~30 vs
 * background tone ~6 memang rendah by design) -- TIDAK melanggar 1.4.11
 * krn container di app ini SELALU dipakai sbg fill kecil BERBENTUK JELAS
 * (kotak ikon bulat, chip) di dalam TactileSurface yang SUDAH punya
 * shadow+tonal elevation sendiri sbg penanda batas -- bukan blok warna
 * mengambang tanpa bentuk di atas background polos. Sama dgn precedent
 * GlassHighlight (dekoratif, bukan pembatas fungsional) di audit
 * sebelumnya.
 */
