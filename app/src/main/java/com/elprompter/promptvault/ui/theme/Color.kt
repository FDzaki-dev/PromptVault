package com.elprompter.promptvault.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * v4.0.0 — GANTI TOTAL tema visual: "AMOLED Glassmorphism Hybrid + Midnight
 * Blue" (v3.x) DIHAPUS, diganti "Dark Titanium Neumorphism + Zamrud Accent".
 *
 * Prinsip wajib (jangan dilanggar sesi berikutnya):
 * 1. Dark mode tetap SATU-SATUNYA mode (keputusan arsitektur lama TIDAK
 *    berubah) -- tapi base-nya sekarang logam titanium matte gelap
 *    (netral, sedikit dingin), BUKAN AMOLED near-black biru.
 * 2. Titanium = warna DOMINAN di seluruh app: root background, permukaan
 *    kartu, ikon well, track kontrol -- semuanya varian abu-abu titanium.
 * 3. Zamrud (Emerald) HANYA "sedikit sentuhan" (per instruksi user) --
 *    dipakai TERBATAS di: CTA utama, switch ON, item terpilih, dan wash
 *    ambient alpha-rendah di kartu. TIDAK dominan seperti Midnight Blue
 *    dulu -- kalau ragu, defaultkan ke titanium netral.
 * 4. Depth "ultra realistic" dicapai lewat KOMBINASI 3 lapis proven-API
 *    Compose (bukan hack shadow custom yang belum pernah dikompilasi):
 *    (a) Modifier.shadow beneran dgn ambient/spotColor = NeuShadowDark
 *    (elevasi asli, bukan dekorasi), (b) gradient brushed-metal diagonal
 *    terang kiri-atas->gelap kanan-bawah pada fill permukaan, (c) border
 *    gradient highlight rambut di tepi kiri-atas (reflected light). Lihat
 *    `ui/components/Neumorphic.kt` untuk implementasi terpusat.
 *    Elemen recessed/pressed (inset) membalik arah gradient (gelap
 *    kiri-atas->terang kanan-bawah) TANPA shadow elevasi -- itulah yang
 *    membuatnya terbaca "tenggelam" bukan "terangkat", inti neumorphism.
 */

// ---- Fondasi Titanium (hirarki elevasi, brushed-metal dark) ----
val TitaniumBase = Color(0xFF1E2023)            // root background
val TitaniumSurface = Color(0xFF2A2D31)         // panel utama (VaultCard, dsb)
val TitaniumSurfaceRaised = Color(0xFF34373C)   // titik paling terang gradient permukaan terangkat
val TitaniumSurfaceSheet = Color(0xFF2E3237)    // lapisan sheet/dialog, 1 tingkat lebih terang dari base
val TitaniumSurfaceRecessed = Color(0xFF17191B) // titik paling gelap gradient permukaan tenggelam/pressed

// ---- Ambient wash Zamrud: HANYA tint tipis, "sedikit sentuhan" sesuai instruksi ----
val EmeraldAmbientTint = Color(0xFF14B889)
val EmeraldAmbientAlpha = 0.05f

// ---- Aksen interaksi utama (dipakai TERBATAS: CTA, switch ON, selected) ----
val EmeraldAccent = Color(0xFF2ED9A0)
val EmeraldAccentDeep = Color(0xFF17A374)       // varian ditekan/border aktif
val EmeraldAccentContainer = Color(0xFF15332A)
val EmeraldAccentOn = Color(0xFF04140F)         // teks/ikon di atas aksen terang

// ---- Teks ----
val TextPrimary = Color(0xFFEEF1F3)
val TextSecondary = Color(0xFFA6ACB3)
val TextMuted = Color(0xFF6C7178)

// ---- Lapisan bevel/neu (arah cahaya: kiri-atas terang, kanan-bawah gelap) ----
val NeuHighlight = Color.White.copy(alpha = 0.16f)   // highlight rambut tepi terangkat
val NeuBorder = Color.White.copy(alpha = 0.06f)      // hairline netral dasar
val NeuShadowDark = Color(0xFF000000).copy(alpha = 0.55f) // shadow elevasi asli (ambient/spot)

/**
 * Aksen semantik ke-4 (stamp/sukses, amber/peringatan, rust/error, slate/
 * pengaturan) dipertahankan strukturnya dari sistem lama, tapi ditata ulang
 * jadi keluarga "logam & permata" senada dgn Titanium+Zamrud: stamp SUKSES
 * memakai Zamrud (paling logis -- "berhasil disortir" = permata hijau yang
 * sama dgn aksen utama), amber jadi kuningan/brass hangat, error jadi
 * tembaga (copper), dan aksen Pengaturan jadi abu-biru titanium dingin.
 * Glow tetap lokal/terbatas (bab Glow Rules lama) -- bukan dipakai di mana-mana.
 */
val StampGlow = EmeraldAccent                    // badge "SORTED" -- satu-satunya tempat
val StampGlowContainer = EmeraldAccentContainer
val AmberGlow = Color(0xFFD9A452)                // kuningan/brass -- auto-scan / peringatan
val AmberGlowContainer = Color(0xFF322A16)
val RustGlow = Color(0xFFE0684F)                 // tembaga/copper -- error
val RustGlowContainer = Color(0xFF33211A)
val SlateGlow = Color(0xFF93A6B3)                // abu-biru titanium dingin -- aksen "Pengaturan"
val SlateGlowContainer = Color(0xFF20272B)

val HairlineNeu = NeuBorder
