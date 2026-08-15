package com.elprompter.promptvault.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * v6.0.0 — Re-palette "Transformative Teal" (v4.0.0) -> "Platinum + Ruby"
 * (permintaan eksplisit user, sesi debugging/polish neumorphism). Fondasi
 * arsitektur v3.0.0 TETAP dipertahankan (dark-only AMOLED near-black +
 * neumorphism shadow ganda dari v5.0.0) -- yang berubah HANYA hue keluarga
 * token aksen (`Teal*` -> `Platinum*`, `Stamp*` -> `Ruby*`), sama persis
 * pola rename+re-hex yang sudah dipakai aman di rebrand v4.0.0 sebelumnya.
 *
 * Konsep "nge-blend": `PlatinumAccent` (primary, silver-platinum terang,
 * dingin & netral -- kesan logam premium) dan `RubyGlow` (secondary, merah
 * ruby jenuh -- kesan batu permata) SENGAJA dipasang sbg 2 ujung satu
 * gradient horizontal di CTA "Scan Sekarang" (lihat `HomeScreen.kt`) --
 * satu-satunya tempat 2 aksen ini benar-benar "berbaur" jadi satu bidang
 * warna, bukan cuma berdampingan di komponen terpisah.
 *
 * Fix collision warna (#UI-21, ditemukan sesi ini): palet lama StampGlow
 * (`#FF6E52`, badge sukses "SORTED") vs RustGlow (`#FF6B5C`, error) HAMPIR
 * IDENTIK (selisih R/G/B < 5) -- 2 makna semantik berbeda (sukses vs error)
 * tidak terbedakan mata secara nyata. `RubyGlow` baru digeser ke hue merah-
 * jambu jenuh (~hue 350, crimson) yang jelas beda dari `RustGlow` (~hue 6,
 * oranye-koral) yang TETAP TIDAK DIUBAH -- sekarang 2 hue benar-benar
 * berbeda arah roda warna, bukan cuma beda satu digit hex.
 *
 * Aturan wajib (diwarisi v3.0.0, TIDAK berubah saat re-palette):
 * 1. Dark mode adalah SATU-SATUNYA mode. Tidak ada fallback terang.
 * 2. AMOLED near-black = identitas DOMINAN (fondasi permukaan neumorphic).
 * 3. Aksen (Platinum/Ruby) HANYA tint ambient tipis + kontrol interaktif --
 *    BUKAN warna latar layar penuh.
 * 4. Kedalaman lewat shadow ganda nyata (`TactileTokens.Neu*` &
 *    `Neumorphic.kt`), bukan tekstur bitmap.
 */

// ---- Fondasi AMOLED + permukaan neumorphic (hirarki elevasi) ----
// TIDAK DIUBAH dari v5.0.0 -- netral, bukan bagian dari hue re-palette.
val AmoledBackground = Color(0xFF030A09)      // root background, near-black OLED-safe
val GlassSurface = Color(0xFF071A17)          // panel utama (VaultCard, dsb)
val GlassSurfaceElevated = Color(0xFF0D2622)  // panel terangkat / bagian atas gradient kartu
val GlassSurfaceSheet = Color(0xFF123330)     // lapisan sheet/dialog, satu tingkat lebih terang
val GlassSurfacePressed = Color(0xFF051211)   // kontrol recessed / pressed

// ---- Ambient tint aksen (dipakai lewat alpha rendah, bukan warna dominan) ----
val PlatinumTint = Color(0xFFB9C2CC)
val PlatinumGradientAlpha = 0.10f

// ---- Aksen interaksi utama: PLATINUM (primary) ----
// Silver-platinum terang, dingin-netral -- kontras kuat di atas AMOLED tanpa
// jatuh ke putih polos (masih kerasa "logam", bukan default putih sistem).
val PlatinumAccent = Color(0xFFDCE2E9)
val PlatinumAccentContainer = Color(0xFF272B31)
val PlatinumAccentOn = Color(0xFF15181C)      // teks/ikon gelap di atas platinum terang

// ---- Aksen ke-2: RUBY (secondary) -- separuh blend CTA + badge sukses ----
val RubyGlow = Color(0xFFE23A55)
val RubyGlowContainer = Color(0xFF33141B)
val RubyOn = Color(0xFFFFF2F4)                // teks TERANG di atas ruby (bukan reuse PlatinumAccentOn
                                               // yang gelap -- ruby cukup jenuh/gelap-value shg teks
                                               // gelap kontrasnya lebih lemah dari teks terang, lihat
                                               // Theme.kt utk pemakaian onSecondary)

// ---- Teks (netral, dingin platinum-adjacent) ----
val TextPrimary = Color(0xFFE9F3F7)
val TextSecondary = Color(0xFFA6BAC7)
val TextMuted = Color(0xFF6C7E89)

// ---- Bevel / cahaya simulasi (arah: kiri-atas terang, kanan-bawah gelap) ----
// Netral (basis Color.White/Black), TIDAK bagian dari hue re-palette.
val GlassHighlight = Color.White.copy(alpha = 0.055f)
val GlassBorder = Color.White.copy(alpha = 0.035f)
val GlassShadow = Color.Black.copy(alpha = 0.72f)

// ---- Neumorphism: shadow "cahaya" sisi kiri-atas. Basis Color.White tetap
// sama, murni token alpha -- lihat javadoc lengkap di v5.0.0 lama, tidak
// diubah saat re-palette ini. ----
val NeuHighlight = Color.White.copy(alpha = 0.16f)

/**
 * Aksen semantik ke-4 (stamp/sukses kini Ruby, amber/peringatan, rust/error,
 * slate/pengaturan). AmberGlow & RustGlow TIDAK DIUBAH nilainya (masih hue
 * berbeda jauh dari Ruby baru, lihat javadoc di atas #UI-21). SlateGlow juga
 * TIDAK DIUBAH -- sudah sengaja indigo-periwinkle sejak v4.0.0 supaya beda
 * dari primary, dan indigo tetap beda jauh dari Platinum (netral) sekarang.
 */
val AmberGlow = Color(0xFFE8AC4E)             // auto-scan / peringatan
val AmberGlowContainer = Color(0xFF231B0F)
val RustGlow = Color(0xFFFF6B5C)              // error -- hue oranye-koral, sengaja beda dari RubyGlow crimson
val RustGlowContainer = Color(0xFF2A1512)
val SlateGlow = Color(0xFF8B9DFF)             // aksen "Pengaturan" -- indigo-periwinkle
val SlateGlowContainer = Color(0xFF1A1F33)

val HairlineGlass = GlassBorder
