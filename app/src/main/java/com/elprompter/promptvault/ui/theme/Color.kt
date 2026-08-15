package com.elprompter.promptvault.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * v7.0.0 — Neumorphism DIHAPUS TOTAL (permintaan eksplisit user: "ultra
 * buggy" -- lihat riwayat Insiden #3/#8/#9/#10 di PROJECT_STATE.md, semua
 * berasal dari teknik shadow ganda offset-Box `Neumorphic.kt` yang sekarang
 * dihapus & digantikan `GlassPanel.kt`, primitif tunggal lebih sederhana:
 * Surface biasa + border + shadow standar Compose, TANPA trik offset-Box).
 *
 * Palet dipatok EKSPLISIT ke 2 hex yang diberikan user, TIDAK ADA hue baru
 * ditambahkan di luar itu ("dilarang keras ngide sendiri"):
 * - #0B132B (Deep Navy Blue) -- 60-70% latar dominan: background root +
 *   seluruh badan panel kaca. Variasi tingkat (`GlassSurface*` di bawah)
 *   BUKAN hue baru -- murni tint/shade lebih terang dari hex yang sama,
 *   dibutuhkan supaya hierarki elevasi tetap terbaca tanpa blur asli
 *   (minSdk 26, `Modifier.blur` RenderEffect cuma nyata di API 31+).
 * - #B5A642 (Brass) -- 10-30% aksen TOMBOL UTAMA SAJA (CTA "Scan Sekarang",
 *   kontrol primer terpilih/ON: segment aktif, switch ON, ikon menu utama).
 *   TIDAK dipakai sebagai warna latar/fill besar di luar itu.
 *
 * Token semantik lama (AmberGlow/RustGlow/SlateGlow -- warning/error/menu
 * "Pengaturan") TIDAK diubah hex-nya: sudah ada SEBELUM instruksi ini & di
 * luar cakupan 2 constraint di atas ("latar dominan" + "aksen tombol
 * utama") -- dipertahankan apa adanya supaya sinyal destructive/warning
 * tetap beda dari aksen primer Brass, TANPA menambah hue baru (token lama,
 * bukan penambahan).
 *
 * `RubyGlow`/`PlatinumAccent` (blend gradient CTA v6.0.0) DIHAPUS TOTAL --
 * CTA sekarang SATU warna solid Brass saja, sesuai instruksi eksplisit
 * "aksen tombol utama" tunggal, bukan blend 2 aksen seperti sebelumnya.
 */

// ---- Fondasi Deep Navy (60-70% dominan) ----
// Nama token `AmoledBackground` DIPERTAHANKAN (bukan di-rename) supaya
// MainActivity.kt (protected asset, hanya boleh edit parsial) TIDAK perlu
// disentuh sama sekali -- import & pemakaiannya di sana tetap valid, hanya
// NILAI hex yang berubah ke Deep Navy.
val AmoledBackground = Color(0xFF0B132B)          // root background, Deep Navy solid
val DeepNavy = AmoledBackground                   // alias semantik untuk kode baru

val GlassSurface = Color(0xFF141C3A)              // panel kaca utama (VaultCard), tint navy lebih terang
val GlassSurfaceElevated = Color(0xFF1C2547)      // panel "naik" 1 tingkat (kotak ikon, dsb)
val GlassSurfaceSheet = Color(0xFF232C54)         // sheet/dialog -- tingkat paling terang
val GlassSurfacePressed = Color(0xFF080E22)       // kontrol recessed / track (lebih gelap dari root)

// ---- Aksen tombol utama: BRASS (10-30%, CTA & kontrol primer saja) ----
val BrassAccent = Color(0xFFB5A642)
val BrassAccentContainer = BrassAccent.copy(alpha = 0.18f)
val BrassAccentOn = DeepNavy                      // teks gelap di atas brass terang (reuse Navy, bukan hex baru)

// ---- Teks (netral, putih-alpha di atas Navy -- bukan hue baru) ----
val TextPrimary = Color(0xFFF2F4F8)
val TextSecondary = Color(0xFFF2F4F8).copy(alpha = 0.68f)
val TextMuted = Color(0xFFF2F4F8).copy(alpha = 0.42f)

// ---- Bevel kaca: highlight/border/shadow -- netral White/Black, teknik
// standar glassmorphism, TIDAK bagian dari constraint 2-hue di atas. ----
val GlassHighlight = Color.White.copy(alpha = 0.10f)
val GlassBorder = Color.White.copy(alpha = 0.14f)
val GlassShadow = Color.Black.copy(alpha = 0.35f)
val HairlineGlass = GlassBorder

/**
 * Aksen semantik lama (warning/error/menu "Pengaturan") -- hex TIDAK
 * diubah, lihat javadoc atas. Dipakai lewat container M3 (tertiary/error)
 * & `VaultExtraColors.slate` (Theme.kt).
 */
val AmberGlow = Color(0xFFE8AC4E)             // auto-scan / peringatan
val AmberGlowContainer = Color(0xFF231B0F)
val RustGlow = Color(0xFFFF6B5C)              // error
val RustGlowContainer = Color(0xFF2A1512)
val SlateGlow = Color(0xFF8B9DFF)             // aksen "Pengaturan"
val SlateGlowContainer = Color(0xFF1A1F33)
