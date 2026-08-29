package com.elprompter.promptvault.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * v8.23.0 — Glassmorphism DIHIDUPKAN KEMBALI (permintaan eksplisit user),
 * TAPI cuma lapisan VISUAL (translucency + glass-edge glint + sheen
 * highlight) di atas primitif [com.elprompter.promptvault.ui.components.TactileSurface]
 * yang SUDAH ADA -- 3 syarat eksplisit user, ditelusuri semua:
 *
 * 1. **"Glassmorphism murni, tanpa utak-atik diluar gaya desain itu
 *    sendiri"** -- HANYA `color`/`border`/overlay highlight di
 *    `TactileSurface` yang berubah. Layout, spacing, typography,
 *    interaksi/press-scale ([TactileTokens], `PressScale.kt`), shape
 *    radius -- SEMUA TIDAK DISENTUH. Warna dasar (hue) JUGA tidak
 *    disentuh -- token di [Color.kt] (Primary/Secondary/Tertiary/dst,
 *    H222 biru) dipakai APA ADANYA, cuma alpha-nya yang berubah di sini.
 * 2. **"base color wajib calm, gak boleh warm"** -- tidak relevan diubah
 *    di file ini (base hue tetap H222 biru cool, lihat [Color.kt] v8.0.0
 *    yang TIDAK disentuh sesi ini). Alpha putih (`Color.White.copy(alpha=)`)
 *    dipakai HANYA utk glint/sheen dekoratif (bukan base warna dominan),
 *    identik prinsipnya dgn `OutlineVariant`/`GlassHighlight` era
 *    sebelumnya yg TIDAK dianggap melanggar syarat "calm" krn porsinya
 *    kecil & dekoratif, bukan fill dominan.
 * 3. **"100% WCAG"** -- translucency mengubah warna EFEKTIF yg terlihat
 *    (campuran fill + apapun di belakangnya). Backdrop app ini SELALU
 *    tema gelap sendiri (AppBackground/SurfaceContainer*, tidak pernah
 *    foto/konten terang) dan SELALU sama-gelap-atau-LEBIH-gelap dari
 *    tingkat surface container manapun (5-tingkat naik dari
 *    [SurfaceContainerLowest] ke [SurfaceContainerHighest]). Jadi
 *    fill translucent akan tercampur MENUJU LEBIH GELAP dari nilai
 *    nominal opaque-nya, TIDAK PERNAH lebih terang -- artinya kontras
 *    teks terang (TextPrimary/TextSecondary) di atasnya SELALU >= angka
 *    worst-case opaque yang sudah diverifikasi di [Color.kt] (11.64:1 /
 *    7.54:1 vs [SurfaceContainerHighest]), tidak pernah turun di bawah
 *    itu -- termasuk saat teks berwarna aksen (mis. `Primary` di label
 *    `SegmentedControl` yang duduk LANGSUNG di atas fill `recessed`,
 *    kasus paling ketat: worst-case opaque-nya 5.89:1 vs
 *    `SurfaceContainerHighest`, sementara fill nyata di sana
 *    `SurfaceContainerHigh` yang SUDAH lebih gelap dari itu -- makin
 *    aman, bukan makin mepet). Border glint & sheen highlight
 *    SEPENUHNYA dekoratif (bukan batas grafis fungsional, prinsip sama
 *    dgn `OutlineVariant` di [Color.kt]) -- TIDAK tunduk ambang 3:1
 *    WCAG 1.4.11.
 *
 * **(2026-08-29) SUPERSEDE syarat #1 di atas ("typography ... SEMUA TIDAK
 * DISENTUH")**: instruksi eksplisit user sesi ini, "perkuat typography
 * Glassmorphism murni", membalik syarat itu KHUSUS utk sumbu typography --
 * sesuai hirarki resmi (User Inst TERBARU > Core Protocol > catatan lama
 * di komentar/`PROJECT_STATE.md`). Paragraf syarat #1 di atas SENGAJA
 * TIDAK dihapus (arsip riwayat kenapa aturan lama ada), TAPI sudah tidak
 * berlaku lagi persis apa adanya -- lihat [GlassTypography] (`Type.kt`)
 * utk isi & alasan lengkap "perkuat"-nya. Syarat #2/#3 (base color calm,
 * 100% WCAG) TIDAK terdampak/TETAP berlaku penuh -- yg disupersede HANYA
 * larangan sentuh typography.
 */
object GlassTokens {
    /** Fill permukaan "terangkat" (VaultCard, dialog, sheet, CTA, kotak ikon/chip -- semua level non-recessed pakai token yang sama, `TactileSurface` tidak punya parameter tingkat ukuran terpisah). */
    const val FillAlphaRaised = 0.82f

    /** Fill permukaan "tenggelam" (track switch OFF, grabber pill) -- lebih transparan, kesan cekung/menyerap cahaya. */
    const val FillAlphaRecessed = 0.60f

    /** Lebar hairline glass-edge, konsisten di semua tingkat. */
    val BorderWidthDefault: Dp = 1.dp

    /** Alpha glass-edge utk permukaan terangkat/kontrol -- glint tipis, bukan outline tegas. */
    const val BorderAlphaRaised = 0.16f

    /** Alpha glass-edge utk permukaan tenggelam -- lebih redup, cocok kesan cekung. */
    const val BorderAlphaRecessed = 0.07f

    fun borderColor(recessed: Boolean): Color =
        Color.White.copy(alpha = if (recessed) BorderAlphaRecessed else BorderAlphaRaised)

    /**
     * Sheen highlight khas glassmorphism -- gradien vertikal, terang tipis
     * di atas meluruh ke transparan. `Brush.verticalGradient(colors)` TANPA
     * `startY`/`endY` eksplisit otomatis menyesuaikan tinggi elemen saat
     * digambar (bukan angka piksel tetap) -- aman dipakai lintas ukuran
     * kartu/kontrol tanpa perlu ukur manual per pemanggil.
     */
    fun highlightBrush(): Brush = Brush.verticalGradient(
        colors = listOf(Color.White.copy(alpha = 0.10f), Color.Transparent)
    )
}
