package com.elprompter.promptvault.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * (2026-09-12, lanjutan "perluas jangkauan" rombak MATERIAL3 gemstone --
 * setelah Color.kt/Type.kt/Shapes.kt/SegmentedControl.kt) Sebelum batch ini,
 * cabang MATERIAL3 di `TactileSurface.kt` ("Material 3 Murni") adalah
 * SATU-SATUNYA dari 4 gaya TANPA treatment dekoratif sendiri di primitif
 * itu -- lihat javadoc lengkap `TactileSurface.kt`:
 * - Glassmorphism = translucency + sheen highlight ([GlassTokens]).
 * - Neumorphism = dual gradient highlight/shade + border gradient +
 *   elevated shadow ([NeumorphTokens]).
 * - Cupertino = hairline border + elevasi dipaksa 0dp ([CupertinoTokens]).
 * - Material3 = `Surface` M3 baku APA ADANYA (0 alpha, 0 border override, 0
 *   sheen) -- warisan filosofi lama "Murni" dari SEBELUM warna/tipografi/
 *   shape-nya sendiri direstyle gemstone/editorial/diagonal-notch. Identitas
 *   warna & shape sudah unik sejak 2026-09-12, tapi permukaan kartu masih
 *   generic M3 polos -- kesenjangan ini yang ditutup file ini.
 *
 * Motif: **"facet glint"** -- border tipis warna [Primary] (gemstone Emerald
 * Jade) yang paling terang di sudut topStart lalu meluruh ke transparan
 * menuju bottomEnd (arah default `Brush.linearGradient` TANPA `start`/`end`
 * eksplisit -- pola identik `NeumorphTokens.borderBrush()`/
 * `fillHighlightBrush()`, TERBUKTI aman dipakai lintas ukuran kartu).
 * SENGAJA SEARAH dgn sisi besar diagonal notch ([PromptVaultShapes],
 * `topStart`/`bottomEnd` = sudut besar) -- kesan "cahaya menangkap SATU
 * faset batu potong" persis di sudut paling menonjol dari shape-nya,
 * bentuk & cahaya kebaca sbg SATU kesatuan, bukan 2 elemen independen yang
 * kebetulan diagonal. Teknik SENGAJA beda dari 2 gaya lain yg sudah pakai
 * "cahaya" (bukan cuma beda warna dari teknik yang sama): Neumorphism =
 * gradient MENGISI SELURUH permukaan (dual, 2 arah), Glass = sheen vertikal
 * PENUH tinggi elemen; Material3 = HANYA border tipis 1 sisi, minimal &
 * presisi -- selaras kesan "faset" (garis, bukan bidang).
 *
 * Alpha [GlintAlpha] SENGAJA direduksi (bukan [Primary] mentah spt
 * `NeumorphTokens.borderBrush()` pakai `Platinum` mentah) -- pelajaran
 * histori langsung dari project ini sendiri: border warna JENUH penuh utk
 * Neumorphism awalnya pakai `Tertiary` lalu `IceCyan` (mencolok), user MINTA
 * turun ke `Platinum` netral/nyaru (v8.28.4, lihat `NeumorphTokens.kt`) --
 * krn [Primary] gemstone jauh lebih JENUH/hijau drpd `Platinum` yang nyaris
 * netral, alpha direndahkan di sini SEJAK AWAL supaya konsisten "calm/
 * underrated" (bukan neon), bukan menunggu user komplain lalu direvisi
 * ulang spt histori Neumorphism.
 *
 * WCAG: dekoratif murni (glint di border tipis, bukan fill/teks) -- prinsip
 * SAMA dgn `OutlineVariant`/glass-edge/border gradient Neumorphism di atas,
 * TIDAK tunduk ambang 3:1 WCAG 1.4.11 (bukan batas grafis fungsional).
 *
 * Dipasang lewat `border = border ?: BorderStroke(...)` di cabang MATERIAL3
 * `TactileSurface.kt` -- pola IDENTIK cabang Cupertino/Glass (hormati border
 * eksplisit caller kalau ada, mis. state error fungsional; isi default
 * HANYA kalau caller tidak kirim apa pun). 0 call site lain perlu disentuh
 * -- otomatis menjalar ke SEMUA kartu/row/dialog yg lewat primitif ini saat
 * gaya MATERIAL3 aktif.
 *
 * **[Update 2026-09-12, polish lanjutan]** Glint DILEWATI saat
 * `recessed=true` (track `SegmentedControl`/switch OFF/grabber pill) --
 * prinsip SAMA PERSIS `GlassTokens` (javadoc `TactileSurface.kt`: "DILEWATI
 * saat recessed=true, cekung tidak boleh 'berkilau' seperti mengambang,
 * insting standar: raised vs sunken beda treatment cahaya"). Batch awal
 * (facet glint pertama kali dipasang) TIDAK sengaja terapkan pengecualian
 * ini -- ditemukan & ditutup saat "poles detail lain" krn track
 * `SegmentedControl` (SELALU `recessed=true`) jadi tampil "berkilau" sama
 * persis kartu mengambang, kontradiktif scr semantik dgn kesan "slot
 * tenggelam" yang justru jadi tujuan `recessed` itu sendiri.
 */
object Material3Tokens {
    /** Lebar border facet glint -- 1dp, konsisten dgn `GlassTokens.BorderWidthDefault`. */
    val GlintWidth: Dp = 1.dp

    /** Alpha [Primary] di titik paling terang (sudut topStart) -- lihat
     * javadoc di atas kenapa direduksi (bukan warna mentah). */
    const val GlintAlpha = 0.35f

    /** Warna dasar glint -- reuse [Primary] apa adanya (0 hue baru), pola
     * identik semua token dekoratif gaya lain ([GlassTokens]/[NeumorphTokens]
     * juga reuse warna existing, cuma alpha yang berubah). */
    val GlintColor: Color = Primary.copy(alpha = GlintAlpha)

    /** Brush border diagonal topStart(terang)->bottomEnd(transparan). */
    fun glintBorderBrush(): Brush = Brush.linearGradient(
        colors = listOf(GlintColor, Color.Transparent)
    )
}
