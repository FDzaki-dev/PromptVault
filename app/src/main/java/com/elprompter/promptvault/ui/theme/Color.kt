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

/**
 * (2026-08-28) Warna sistem iOS -- tutup item TERAKHIR dari 3 pending
 * restyling Cupertino murni (lihat [CupertinoTokens]). DIPAKAI KHUSUS oleh
 * `CupertinoColors` (`Theme.kt`, kondisional per `themeStyle`, pola identik
 * `CupertinoTypography`/`CupertinoShapes`) -- [PromptVaultColors] di atas
 * (dipakai 3 gaya lain: Glass/Neumorphism/M3) 0 disentuh/0 berubah.
 *
 * Hue diambil PERSIS dari nilai publik resmi Apple HIG (dark appearance,
 * bukan reverse-engineer aset berlisensi): systemBlue #0A84FF (H210),
 * systemTeal #64D2FF (H197), systemOrange #FF9F0A (H37), systemRed
 * #FF453A (H3), systemIndigo #5E5CE6 (H241). TAPI tone (S/L) TIDAK dipakai
 * mentah -- nilai dark-appearance Apple dikalibrasi utk `systemBackground`
 * iOS asli yang jauh lebih gelap dari [SurfaceContainerHighest] app ini;
 * dipakai apa adanya sbg teks/ikon langsung, systemBlue mentah cuma
 * 3.58:1 (gagal AA 4.5:1), systemIndigo mentah cuma 2.58:1. Tone (S/L)
 * di-re-derive per hue Apple di atas supaya lulus standar WCAG project ini
 * (syarat #3 javadoc atas) -- pola IDENTIK cara [Primary]/[Tertiary]/dst
 * diturunkan (tone dark-scheme M3 pastel, bukan warna solid vivid), cuma
 * hue-nya sekarang genuinely dari Apple, bukan hue custom lama.
 *
 * Kontras (worst-case vs [SurfaceContainerHighest], sama metodologi audit
 * di atas): Blue 6.17:1, Teal 7.24:1, Orange 7.33:1, Red 5.42:1, Indigo
 * 5.24:1 -- semua lulus AA teks (>=4.5:1). On* vs base fill masing-masing
 * (dipakai saat base jadi containerColor tombol/snackbar, mis.
 * `VaultActionSheet.kt`): Blue 7.64:1, Teal 7.80:1, Orange 8.03:1, Red
 * 6.75:1. OnXContainer vs XContainer (kotak ikon/chip): 7.53-8.37:1.
 * XContainer vs [AppBackground] (info saja, konteks sama spt catatan audit
 * 1.4.11 di atas -- container SELALU dipakai berbentuk jelas dgn elevasi
 * sendiri): 1.36-2.14:1, sejalan dgn precedent container lain.
 *
 * Audit titik pemakaian (alasan item ini dulu ditunda, lihat
 * `PROJECT_STATE.md`): grep `Color(0x...)` menyeluruh di luar package
 * `ui/theme` = 0 hasil -- SELURUH app 100% konsumsi warna lewat
 * `MaterialTheme.colorScheme.*`/`VaultTheme.extraColors`, 0 hardcode
 * lepas yang bisa bypass swap kondisional ini. Aman full-swap tanpa
 * sentuh satu-satu call site (sama spt precedent shapes/typography).
 */
val CupertinoBlue = Color(0xFF80B7EF)                  // primary -- systemBlue
val CupertinoOnBlue = Color(0xFF0F2235)
val CupertinoBlueContainer = Color(0xFF264564)
val CupertinoOnBlueContainer = Color(0xFFDEEBF7)

val CupertinoTeal = Color(0xFF77CCEE)                  // secondary -- systemTeal
val CupertinoOnTeal = Color(0xFF112F3B)
val CupertinoTealContainer = Color(0xFF264A59)
val CupertinoOnTealContainer = Color(0xFFD9EBF2)

val CupertinoOrange = Color(0xFFF2B85F)                // tertiary/warning -- systemOrange
val CupertinoOnOrange = Color(0xFF39270C)
val CupertinoOrangeContainer = Color(0xFF5F441C)
val CupertinoOnOrangeContainer = Color(0xFFF6E9D5)

val CupertinoRed = Color(0xFFF08B84)                   // error -- systemRed
val CupertinoOnRed = Color(0xFF3B1411)
val CupertinoRedContainer = Color(0xFF622925)
val CupertinoOnRedContainer = Color(0xFFF6D7D5)

val CupertinoIndigo = Color(0xFF9E9DE7)                // aksen ke-4 "slate" -- systemIndigo
val CupertinoIndigoContainer = Color(0xFF27264F)

/**
 * (2026-08-29) Warna khusus gaya NEUMORPHISM -- kombinasi "Teal & Amber
 * (Blade Runner)", permintaan eksplisit user. DIPAKAI KHUSUS oleh
 * `NeumorphismColors` (`Theme.kt`, kondisional per `themeStyle`, pola
 * IDENTIK `CupertinoColors` di atas) -- [PromptVaultColors] (dipakai 2
 * gaya lain: Glass/M3) & [CupertinoColors] 0 disentuh/0 berubah. Cakupan
 * SENGAJA dibatasi cuma 3 slot AKSEN (primary/secondary/tertiary) --
 * neutral/background/surface/error/outline & aksen ke-4 "Pengaturan"
 * ([VaultExtraColors]) SENGAJA 100% REUSE token lama (0 token baru), sama
 * persis alasan [CupertinoColors]: identitas 2-warna cukup dibawa lewat
 * slot aksen, background terpisah/aksen ke-4 baru tidak diminta.
 *
 * Hue: primary = TEAL H187 (cyan-teal terang, cahaya "hologram" khas
 * poster Blade Runner), secondary = TEAL lebih hijau H172 (varian teal
 * lebih teduh, SENGAJA beda hue tipis dari primary spy pola M3 baku
 * primary/secondary tetap 2 hue berbeda -- lihat javadoc [Secondary] di
 * atas), tertiary = AMBER/ORANGE H32 (lebih hangat & lebih jenuh dari
 * amber warning M3 baku H42 di atas -- disengaja, supaya baca sbg identik
 * "amber neon" duotone klasik Blade Runner, bukan amber redup semantik
 * warning). Saturasi SEDIKIT lebih tinggi dari resep [Primary]/[Tertiary]
 * M3 baku (~0.55) di atas -- 0.34-0.72 tergantung tone -- utk kesan "neon"
 * yg diminta, TAPI tetap 1 keluarga metodologi kontras yg sama (bukan
 * warna solid vivid mentah, tone tetap dikalibrasi WCAG spt biasa).
 *
 * Kontras (worst-case vs [SurfaceContainerHighest], metodologi identik
 * seluruh file ini): Teal 6.20:1, TealDeep 6.60:1, Amber 7.30:1 -- semua
 * lulus AA teks (>=4.5:1) dgn margin nyaman. On* vs base fill masing2:
 * OnTeal 7.41:1, OnTealDeep 7.31:1, OnAmber 8.00:1 (semua AAA). Container
 * vs [AppBackground] (info saja, konteks sama spt catatan audit 1.4.11 di
 * atas -- container SELALU dipakai berbentuk jelas dgn elevasi sendiri):
 * 1.90:1 utk ketiganya (disamakan presisi ke tingkat existing
 * PrimaryContainer 1.82:1/dst, bukan kebetulan -- dicari eksplisit lewat
 * script kalkulasi kontras spy "kegelapan" container konsisten lintas
 * hue meski hue teal secara persepsi jauh lebih terang dari hue biru pada
 * L yang sama, krn bobot channel hijau formula WCAG jauh lebih besar dari
 * biru). OnXContainer vs XContainer: 7.79-7.80:1 (AAA).
 *
 * Audit titik pemakaian: sama seperti [CupertinoBlue] dkk di atas -- grep
 * `Color(0x...)` di luar package `ui/theme` = 0 hasil, SELURUH app 100%
 * konsumsi warna lewat `MaterialTheme.colorScheme.*` -- aman full-swap
 * kondisional tanpa sentuh call site manapun.
 */
val NeoTeal = Color(0xFF4BC2D2)                        // primary -- teal cyan terang
val NeoOnTeal = Color(0xFF12272A)
val NeoTealContainer = Color(0xFF25464B)
val NeoOnTealContainer = Color(0xFFC2E8ED)

val NeoTealDeep = Color(0xFF7FC5BC)                    // secondary -- teal lebih hijau/teduh
val NeoOnTealDeep = Color(0xFF1A2D2B)
val NeoTealDeepContainer = Color(0xFF2F4643)
val NeoOnTealDeepContainer = Color(0xFFCEE7E3)

val NeoAmber = Color(0xFFEAB980)                       // tertiary -- amber/oranye hangat, jenuh
val NeoOnAmber = Color(0xFF372715)
val NeoAmberContainer = Color(0xFF533D25)
val NeoOnAmberContainer = Color(0xFFF2DFC9)

/**
 * (2026-08-29, lanjutan sesi Blade Runner) Aksen ke-4 "Pengaturan" KHUSUS
 * gaya NEUMORPHISM -- MENUTUP catatan lama di javadoc [NeoAmber] dkk di
 * atas ("aksen ke-4 SENGAJA 100% reuse token lama, tidak diminta") yg
 * SEKARANG sudah usang: user eksplisit minta warna sendiri utk slot ini,
 * dipilih dari 4 opsi yg diajukan (Neon Magenta / Neon Violet / reuse
 * Amber / reuse Teal) -- user pilih **Neon Magenta**.
 *
 * Pola IDENTIK [CupertinoIndigo] di atas (aksen ke-4 khusus 1 gaya, HANYA
 * 2 val base+container, TANPA on-variant -- beda dari trio
 * primary/secondary/tertiary di atas krn [VaultExtraColors] cuma 2 field
 * `slate`/`slateContainer`, 0 field "onSlate" yg butuh dikonsumsi, jadi
 * on-variant di sini bakal 100% dead code kalau dibuat).
 *
 * Hue H330 (magenta-pink) -- neon pink/magenta ikonik dari palet poster &
 * kabut kota Blade Runner 2049 (Joi's billboard, dsb), SENGAJA hue baru
 * ke-4 yg beda dari trio primary/secondary/tertiary di atas (teal/
 * teal-deep/amber) supaya slot "Pengaturan" tetap kebaca sbg aksen
 * TERPISAH scr visual (bukan reuse salah satu dari 3 warna utama), sama
 * alasan [CupertinoIndigo] pakai systemIndigo yg juga beda dari
 * biru/teal/oranye Cupertino di atasnya.
 *
 * Kontras (metodologi identik trio di atas): NeoMagenta vs
 * [SurfaceContainerHighest] = 6.84:1 (lulus AA teks dgn margin nyaman,
 * sejajar NeoTeal 6.20/NeoTealDeep 6.60/NeoAmber 7.30). NeoMagentaContainer
 * vs [AppBackground] (info saja, sama catatan audit 1.4.11 -- container
 * dipakai berbentuk box ikon 30dp dgn elevasi sendiri) = 1.90:1, disamakan
 * presisi ke tingkat NeoTealContainer/NeoAmberContainer (jg 1.90:1).
 *
 * Audit titik pemakaian: satu2nya call site `extraColors.slate` ada di
 * `HomeScreen.kt` (tint ikon "Pengaturan" via `GroupedListRow`) --
 * `slateContainer` sendiri masih 0 konsumen (sama kondisi
 * [CupertinoIndigoContainer], reserved utk masa depan), tetap didefinisikan
 * demi [VaultExtraColors] butuh 2 field non-null saat konstruksi.
 */
val NeoMagenta = Color(0xFFEAA9C9)                     // aksen ke-4 "slate" -- neon magenta/pink BR2049
val NeoMagentaContainer = Color(0xFF64304A)
