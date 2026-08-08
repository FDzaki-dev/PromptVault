package com.elprompter.promptvault.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * v3.0.0 — GANTI TOTAL palet lama ("Manifest Arsip" kraft/pine terang + obsidian
 * gelap terpisah) dengan SATU sistem visual sesuai spesifikasi:
 * "Skeuomorphism-lite AMOLED Glassmorphism Hybrid + Midnight Blue Gradient".
 *
 * Aturan wajib dari spesifikasi (jangan dilanggar saat edit di masa depan):
 * 1. Dark mode adalah SATU-SATUNYA mode. Tidak ada fallback terang.
 * 2. AMOLED near-black + frosted glass = identitas DOMINAN.
 * 3. Midnight Blue HANYA tint/gradient ambient tipis (alpha rendah) di dalam
 *    permukaan glass -- BUKAN warna latar utama. Jangan buat seluruh layar
 *    terasa biru rata.
 * 4. Tidak ada tekstur bitmap berat -- semua kedalaman lewat Brush/gradient/
 *    border/shadow/elevation saja.
 */

// ---- Fondasi AMOLED + permukaan glass (hirarki elevasi) ----
val AmoledBackground = Color(0xFF030508)      // root background, near-black OLED-safe
val GlassSurface = Color(0xFF0A0F16)          // panel utama (VaultCard, dsb)
val GlassSurfaceElevated = Color(0xFF101722)  // panel terangkat / bagian atas gradient kartu
val GlassSurfaceSheet = Color(0xFF141B26)     // lapisan sheet/dialog, satu tingkat lebih terang
val GlassSurfacePressed = Color(0xFF070B11)   // kontrol recessed / pressed

// ---- Lapisan Midnight Blue: HANYA tint ambient, dipakai lewat alpha rendah ----
val MidnightBlueTint = Color(0xFF191970)
val MidnightBlueGradientAlpha = 0.08f

// ---- Aksen interaksi utama (dominan dipakai untuk kontrol/primary) ----
val MidnightBlueAccent = Color(0xFF6670FF)
val MidnightBlueAccentContainer = Color(0xFF1B1E3D)
val MidnightBlueAccentOn = Color(0xFF04050C)  // teks/ikon di atas aksen terang

// ---- Teks ----
val TextPrimary = Color(0xFFEAF0F8)
val TextSecondary = Color(0xFFAAB5C4)

// ---- Bevel / cahaya simulasi (arah: kiri-atas terang, kanan-bawah gelap) ----
val GlassHighlight = Color.White.copy(alpha = 0.055f)
val GlassBorder = Color.White.copy(alpha = 0.035f)
val GlassShadow = Color.Black.copy(alpha = 0.70f)

/**
 * Aksen semantik ke-4 (stamp/sukses, amber/peringatan, rust/error, slate/
 * pengaturan) dipertahankan dari sistem lama supaya menu grouped-list &
 * badge status tetap punya identitas warna berbeda-beda (bukan monokrom
 * biru), tapi seluruhnya ditata ulang supaya "container"-nya duduk tenang
 * di atas dasar AMOLED + glass, dan glow-nya tetap lokal/terbatas sesuai
 * aturan 9 (Glow Rules) -- bukan dipakai di mana-mana.
 */
val StampGlow = Color(0xFFFF6E52)             // badge "SORTED" -- satu-satunya tempat
val StampGlowContainer = Color(0xFF241612)
val AmberGlow = Color(0xFFE8AC4E)             // auto-scan / peringatan
val AmberGlowContainer = Color(0xFF231B0F)
val RustGlow = Color(0xFFFF6B5C)              // error
val RustGlowContainer = Color(0xFF2A1512)
val SlateGlow = Color(0xFF7FA8D9)             // aksen "Pengaturan", tetap dalam keluarga biru dingin
val SlateGlowContainer = Color(0xFF121C28)

val HairlineGlass = GlassBorder
