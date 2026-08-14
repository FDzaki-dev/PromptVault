package com.elprompter.promptvault.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * v4.0.0 — GANTI TOTAL palet "AMOLED Glassmorphism Hybrid + Midnight Blue
 * Gradient" (v3.0.0) dengan identitas baru: "Transformative Teal" (biru-hijau
 * gelap), atas permintaan eksplisit user. Struktur arsitektur v3.0.0 TETAP
 * dipertahankan (dark-only, AMOLED near-black + frosted glass sebagai fondasi,
 * aksen warna HANYA sebagai tint/ambient + kontrol interaktif, bukan warna
 * layar penuh) -- yang berubah adalah HUE seluruh keluarga "Midnight Blue"
 * (indigo dingin) menjadi "Transformative Teal" (biru-hijau, lebih hidup),
 * DITAMBAH sistem token elevasi baru di TactileTokens.kt untuk efek depth/3D
 * immersive (dipakai nyata di VaultCard.kt, HomeScreen.kt CTA, GroupedListRow.kt,
 * TactileSwitch.kt -- token warna di file ini cuma bahan baku).
 *
 * v5.0.0 — Redesign TEKNIK render Glassmorphism -> Neumorphism ultra
 * immersive, atas permintaan eksplisit user "tanpa harus ganti palet warna".
 * SEMUA token di atas (Amoled*, Glass*, Teal*, Text*, Stamp/Amber/Rust/Slate
 * *Glow) TIDAK DIUBAH NILAINYA SAMA SEKALI di batch ini -- hue/hex brand
 * 100% identik dengan v4.0.0. Yang berubah adalah CARA token-token itu
 * digambar (lihat `ui/components/Neumorphic.kt`): permukaan glass+border+
 * gradient tipis (glassmorphism, "kaca berlapis") diganti permukaan solid
 * timbul/tenggelam dgn shadow ganda arah cahaya kiri-atas terang -> kanan-
 * bawah gelap (neumorphism, "soft UI dark"), tetap di atas fondasi AMOLED +
 * tint Teal ambient yang sama persis. Satu token BARU ditambah di bawah
 * (`NeuHighlight`) -- basisnya TETAP `Color.White` (sama seperti
 * `GlassHighlight`), HANYA alpha yang lebih pekat supaya terbaca sebagai
 * "cahaya" shadow-cast (bukan hairline border seperti `GlassHighlight`).
 * Ini BUKAN hue baru, murni derivasi alpha dari warna netral yang sudah ada.
 *
 * Aturan wajib (diwarisi dari v3.0.0, TIDAK berubah saat re-palette/redesign):
 * 1. Dark mode adalah SATU-SATUNYA mode. Tidak ada fallback terang.
 * 2. AMOLED near-black = identitas DOMINAN (fondasi permukaan neumorphic).
 * 3. Teal HANYA tint/gradient ambient tipis di dalam permukaan + warna
 *    kontrol interaktif utama -- BUKAN warna latar layar penuh.
 * 4. Tidak ada tekstur bitmap berat -- kedalaman lewat shadow ganda nyata
 *    (lihat TactileTokens.NeuElevation* dkk & `Neumorphic.kt`).
 */

// ---- Fondasi AMOLED + permukaan glass (hirarki elevasi), tint teal tipis ----
val AmoledBackground = Color(0xFF030A09)      // root background, near-black OLED-safe, hint teal
val GlassSurface = Color(0xFF071A17)          // panel utama (VaultCard, dsb)
val GlassSurfaceElevated = Color(0xFF0D2622)  // panel terangkat / bagian atas gradient kartu
val GlassSurfaceSheet = Color(0xFF123330)     // lapisan sheet/dialog, satu tingkat lebih terang
val GlassSurfacePressed = Color(0xFF051211)   // kontrol recessed / pressed

// ---- Lapisan Teal ambient: tint atmosfer, dipakai lewat alpha rendah ----
// Alpha dinaikkan sedikit dari era Midnight Blue (0.06f -> 0.10f) SENGAJA --
// permintaan eksplisit "ultra immersive" butuh tint yang lebih terasa, tapi
// tetap ambient (0.10f jauh dari warna dominan layar, golden rule tetap utuh).
val TealTint = Color(0xFF0E6B5C)
val TealGradientAlpha = 0.10f

// ---- Aksen interaksi utama (dominan dipakai untuk kontrol/primary) ----
val TealAccent = Color(0xFF2EE6B8)            // teal-hijau cerah, kontras kuat di atas AMOLED
val TealAccentContainer = Color(0xFF0F3B34)
val TealAccentOn = Color(0xFF00201B)          // teks/ikon di atas aksen terang

// ---- Teks (cool-toned mengikuti keluarga teal, bukan lagi netral biru) ----
val TextPrimary = Color(0xFFE9F7F3)
val TextSecondary = Color(0xFFA6C7C0)
val TextMuted = Color(0xFF6C8A83)

// ---- Bevel / cahaya simulasi (arah: kiri-atas terang, kanan-bawah gelap) ----
val GlassHighlight = Color.White.copy(alpha = 0.055f)
val GlassBorder = Color.White.copy(alpha = 0.035f)
val GlassShadow = Color.Black.copy(alpha = 0.72f) // sedikit lebih pekat dari v3.0.0 (0.70f) utk depth 3D lebih terasa

// ---- v5.0.0: Neumorphism -- shadow "cahaya" utk sisi kiri-atas permukaan
// timbul/tenggelam. Basis warna TETAP Color.White (identik GlassHighlight),
// HANYA alpha yang dinaikkan (0.055f -> 0.16f) krn dipakai sbg spotColor/
// ambientColor Modifier.shadow (efek shadow-cast), bukan sbg fill hairline
// border tipis -- alpha lama terlalu lemah utk terbaca sbg "cahaya" nyata.
// Sisi gelap (kanan-bawah) SENGAJA TIDAK dapat token baru -- reuse default
// shadow hitam bawaan Compose (persis mekanisme yang sudah dipakai aman di
// VaultCard/CTA/icon/thumb sejak v4.0.0), supaya tidak menambah token tanpa
// perlu (bab "jangan duplikasi konstanta tactile").
val NeuHighlight = Color.White.copy(alpha = 0.16f)

/**
 * Aksen semantik ke-4 (stamp/sukses, amber/peringatan, rust/error, slate/
 * pengaturan) dipertahankan dari sistem lama supaya menu grouped-list &
 * badge status tetap punya identitas warna berbeda-beda (bukan monokrom
 * teal), tapi seluruhnya ditata ulang supaya "container"-nya duduk tenang
 * di atas dasar AMOLED + glass, dan glow-nya tetap lokal/terbatas -- bukan
 * dipakai di mana-mana.
 *
 * `SlateGlow` (aksen "Pengaturan") SENGAJA digeser dari keluarga biru dingin
 * (dulu dekat dengan Midnight Blue primary) ke indigo-periwinkle yang lebih
 * jelas berbeda hue dari TealAccent baru -- supaya baris menu "Pengaturan"
 * tetap terbaca beda identitas dari primary teal-hijau (Keputusan Arsitektur
 * #3, "menu tidak monoton satu warna"), bukan kebetulan mirip lagi seperti
 * kalau Slate dibiarkan tetap biru murni berdampingan dgn Teal (hijau-biru).
 */
val StampGlow = Color(0xFFFF6E52)             // badge "SORTED" -- satu-satunya tempat
val StampGlowContainer = Color(0xFF241612)
val AmberGlow = Color(0xFFE8AC4E)             // auto-scan / peringatan
val AmberGlowContainer = Color(0xFF231B0F)
val RustGlow = Color(0xFFFF6B5C)              // error
val RustGlowContainer = Color(0xFF2A1512)
val SlateGlow = Color(0xFF8B9DFF)             // aksen "Pengaturan" -- indigo-periwinkle, dibedakan dari TealAccent
val SlateGlowContainer = Color(0xFF1A1F33)

val HairlineGlass = GlassBorder
