package com.elprompter.promptvault.ui.theme

import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * v8.0.0 — Skala BAKU Material 3 (extraSmall=4, small=8, medium=12,
 * large=16, extraLarge=28), simetris di ke-4 sudut.
 *
 * (2026-09-12, SUPERSEDE, permintaan eksplisit user "rombak shape M3 Murni
 * jadi kece & underrated") Diganti "diagonal notch" -- `RoundedCornerShape`
 * per-sudut TIDAK SAMA (topStart/bottomEnd besar, topEnd/bottomStart kecil,
 * pola diagonal), signature yg 0 dipakai gaya lain (Cupertino/Glass =
 * `RoundedCornerShape` SIMETRIS, Neumorphism = `CutCornerShape` simetris --
 * asimetris genuinely underrated utk app Android, jarang dipakai app
 * arus-utama). Rasio besar:kecil SENGAJA ditipiskan di tingkat besar
 * (extraLarge 34:22 ~1.5x, bukan ~3x spt extraSmall 6:2) supaya sheet/dialog
 * besar tetap kebaca "calm" (tidak terlalu tajam/pecah scr visual), sudut
 * kecil (card/chip) yg paling kentara efek "notch"-nya. Rata-rata per
 * tingkat (~4/8/12/16/28) tetap sejalan skala M3 lama (footprint mirip, 0
 * lompatan ukuran drastis), urutan naik extraSmall->extraLarge tetap
 * dijaga di KEDUA sudut (besar: 6/11/16/22/34, kecil: 2/5/8/10/22).
 */
val PromptVaultShapes = Shapes(
    extraSmall = RoundedCornerShape(topStart = 6.dp, topEnd = 2.dp, bottomEnd = 6.dp, bottomStart = 2.dp),
    small = RoundedCornerShape(topStart = 11.dp, topEnd = 5.dp, bottomEnd = 11.dp, bottomStart = 5.dp),
    medium = RoundedCornerShape(topStart = 16.dp, topEnd = 8.dp, bottomEnd = 16.dp, bottomStart = 8.dp),
    large = RoundedCornerShape(topStart = 22.dp, topEnd = 10.dp, bottomEnd = 22.dp, bottomStart = 10.dp),
    extraLarge = RoundedCornerShape(topStart = 34.dp, topEnd = 22.dp, bottomEnd = 34.dp, bottomStart = 22.dp)
)

/**
 * (v8.31.2, "lanjut sempurnakan" gaya Cupertino) Skala sudut KHUSUS gaya
 * [com.elprompter.promptvault.data.ThemeStyleOption.CUPERTINO] (rename dari
 * HYBRID di v8.31.4) -- radius SENGAJA lebih besar dari [PromptVaultShapes]
 * di semua tingkat (bukan proporsi acak, tetap urutan naik
 * extraSmall->extraLarge yang sama), meniru kesan sudut "continuous
 * corner"/lebih membulat khas Cupertino TANPA custom `Shape` class rumit
 * (Compose stok tidak native dukung squircle iOS asli -- radius lebih
 * besar via `RoundedCornerShape` biasa sudah cukup memberi "rasa"-nya).
 *
 * Dipasang di `PromptVaultTheme` (`Theme.kt`) -- GANTI `shapes` M3 secara
 * KONDISIONAL per `themeStyle`, BUKAN nilai statis baru di `TactileSurface`
 * per-cabang (lihat alasan di javadoc `Theme.kt`). Efeknya OTOMATIS
 * menjalar ke SEMUA caller yang pakai `MaterialTheme.shapes.*` -- termasuk
 * default parameter `TactileSurface.shape` sendiri (`= MaterialTheme.shapes.medium`),
 * jadi mayoritas kartu/kontrol di app (yang tidak eksplisit override shape)
 * otomatis ikut lebih membulat saat gaya Cupertino aktif, 0 file caller
 * lain perlu disentuh satu per satu.
 *
 * Caveat jujur: ~11 titik pakai `RoundedCornerShape(Xdp)` literal LANGSUNG
 * (bukan lewat `MaterialTheme.shapes.*`) di seluruh app TIDAK ikut berubah
 * batch ini -- di luar cakupan 1 perubahan tunggal di sini, perlu disentuh
 * manual kalau diminta lanjut lebih jauh.
 */
val CupertinoShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

/**
 * (2026-08-29) Skala sudut KHUSUS gaya
 * [com.elprompter.promptvault.data.ThemeStyleOption.NEUMORPHISM]
 * ("Teal & Amber Blade Runner", lihat javadoc lengkap hue/kontras di
 * `NeumorphismColors`/`Color.kt`) -- pola IDENTIK [CupertinoShapes] di atas
 * (ganti keluarga `Shape` per `themeStyle` via `MaterialTheme.shapes.*` di
 * `Theme.kt`, BUKAN override per-cabang di `TactileSurface`). Keluarga
 * bentuknya `CutCornerShape` (sudut potong lurus/chamfer) -- KEBALIKAN arah
 * "kesan" dari [CupertinoShapes] (Cupertino = lebih bulat/lembut, Blade
 * Runner = sudut tegas/dipotong lurus, ciri panel/HUD retrofuturistik-
 * industrial khas visual film-nya, bukan sudut membulat organik).
 *
 * Nilai dp per tingkat SENGAJA disamakan PERSIS skala [PromptVaultShapes]
 * (M3 baku: 4/8/12/16/28) -- "parity footprint": komponen manapun yang
 * pakai `MaterialTheme.shapes.*` (kotak ikon, kartu, sheet/dialog) 0
 * berubah ukuran/proporsi saat pindah ke gaya Neumorphism, HANYA keluarga
 * potongannya yang beda (lurus vs bulat) -- aman full-swap tanpa sentuh
 * call site manapun, sama prinsip [CupertinoShapes].
 */
val NeumorphismShapes = Shapes(
    extraSmall = CutCornerShape(4.dp),
    small = CutCornerShape(8.dp),
    medium = CutCornerShape(12.dp),
    large = CutCornerShape(16.dp),
    extraLarge = CutCornerShape(28.dp)
)

/**
 * (2026-08-29, sesi baru "lanjut dilengkapi") Skala sudut KHUSUS gaya
 * [com.elprompter.promptvault.data.ThemeStyleOption.GLASSMORPHISM] -- pola
 * IDENTIK [CupertinoShapes]/[NeumorphismShapes] di atas. Sebelum batch ini
 * GLASSMORPHISM & MATERIAL3 100% berbagi [PromptVaultShapes] yg sama
 * persis (nebeng cabang `else` di `Theme.kt`), 0 dibedakan.
 *
 * **Supersede lanjutan**: javadoc `GlassTokens.kt` v8.23.0 jg mendaftar
 * "shape radius ... SEMUA TIDAK DISENTUH" sbg salah satu syarat lama
 * "Glassmorphism murni" (sudah di-supersede sebagian utk typography di
 * batch sebelumnya, lihat [GlassTypography] `Type.kt`). Instruksi user
 * sesi ini ("shape dulu (frosted-glass corner)") membalik syarat itu utk
 * sumbu shape jg -- catatan supersede diperluas di `GlassTokens.kt`.
 *
 * Keluarga tetap `RoundedCornerShape` (sama spt [CupertinoShapes], Compose
 * stok 0 dukung squircle asli), TAPI radius per tingkat SENGAJA dibuat
 * PALING besar/lembut dari SEMUA 4 gaya (10/14/20/28/36, lebih besar dari
 * [CupertinoShapes] 8/12/18/24/32) -- kesan "sudut kaca beku (frosted)"
 * yg diminta: bidang lebar/plush/bubble-like, bukan sekadar "agak bulat"
 * spt Cupertino. Urutan naik extraSmall->extraLarge tetap dijaga (sama
 * disiplin monoton spt semua skala shape lain di file ini).
 *
 * Sama spt [CupertinoShapes]/[NeumorphismShapes]: dipasang kondisional di
 * `PromptVaultTheme` (`Theme.kt`), otomatis menjalar ke SEMUA caller
 * `MaterialTheme.shapes.*` (termasuk default `TactileSurface.shape`), 0
 * call site lain perlu disentuh. Caveat yg sama jg berlaku: ~11 titik
 * `RoundedCornerShape(Xdp)` literal langsung (lihat javadoc
 * [CupertinoShapes]) TETAP tidak ikut berubah gaya apapun, di luar
 * cakupan batch ini.
 */
val GlassShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(36.dp)
)
