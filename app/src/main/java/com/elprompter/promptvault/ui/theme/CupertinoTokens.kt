package com.elprompter.promptvault.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * (v8.31.1-v8.31.3, MVP awal sbg "HYBRID": kerangka M3 + aksen Cupertino)
 * (v8.31.4, RESTYLING -- keputusan eksplisit user: "mending restyling dari
 * hybrid -> Cupertino style murni total") Nama gaya DIGANTI HYBRID ->
 * CUPERTINO di seluruh codebase ([ThemeStyleOption], file ini,
 * [CupertinoShapes], toggle di Pengaturan) -- bukan cuma cat ulang nama,
 * tapi arah pengembangan ke depan: bukan lagi "M3 + beberapa aksen
 * Cupertino", tujuannya sekarang PENUH identitas Cupertino (typography,
 * warna list flat, dialog/sheet, dst -- dikerjakan BERTAHAP tiap batch,
 * SAMA persis pola Neumorphism/Glassmorphism dulu yang juga puluhan
 * iterasi, BUKAN bisa selesai 1x jalan).
 *
 * Progres "murni" sejauh ini:
 * - Hairline border SELALU tampil (bukan shadow, signature list iOS).
 * - Corner radius lebih besar/"continuous" ([CupertinoShapes]).
 * - Action sheet: TextButton polos dipisah hairline (`VaultActionSheet.kt`).
 * - (v8.31.4 BARU) Elevasi DIPAKSA 0dp SELALU (lihat cabang di
 *   `TactileSurface.kt`) -- grouped list iOS FLAT total, tidak pernah pakai
 *   shadow sama sekali (beda dari cabang MATERIAL3 lama yang masih kasih
 *   elevasi saat `recessed=false`). Warna latar (bukan bayangan) yang jadi
 *   penanda "kartu" vs "background" di iOS asli.
 * - Custom dialog non-actionsheet: `VaultAlertDialog.kt` (lihat
 *   `DiagnosticsScreen.kt`) -- ganti `AlertDialog` M3 mentah yg dulu bypass
 *   total sistem tema.
 * - Typography scale iOS-ish: [CupertinoTypography] (`Type.kt`) -- kondisional
 *   di `Theme.kt` (pola identik shapes), skala HIG (tracking negatif di
 *   size 15-17sp, Semibold di Headline/nav title).
 * - (2026-08-28, FASE TERAKHIR) Warna sistem iOS: `CupertinoColors`/
 *   `CupertinoExtra` (`Theme.kt`) -- kondisional (pola identik shapes/
 *   typography), hue systemBlue/Teal/Orange/Red/Indigo (Apple HIG publik,
 *   tone di-re-derive supaya lulus WCAG app ini, lihat javadoc lengkap di
 *   `Color.kt`). Menutup SEMUA 3 item restyling Cupertino murni tahap awal
 *   (typography, custom dialog, warna sistem) -- 0 item wajib tersisa.
 *
 * Belum dikerjakan: tidak ada item wajib tersisa dari checklist restyling
 * awal. Penghalusan lanjutan (kalau ada) sifatnya iteratif ke depan --
 * sama seperti Neumorphism/Glassmorphism yg juga puluhan iterasi setelah
 * checklist awal selesai, bukan "sekali jalan lalu berhenti total".
 */
object CupertinoTokens {
    /** Lebar hairline -- 1px fisik, konvensi iOS (`UIView` separator
     * default juga 1 device pixel, bukan 1dp tebal). */
    val HairlineWidth: Dp = 0.75.dp

    /** Warna hairline: reuse `outlineVariant` (SUDAH ADA, 0 hue baru) --
     * cukup redup utk pemisah, tidak mendominasi spt border Neumorphism. */
    @Composable
    fun hairlineColor(): Color = MaterialTheme.colorScheme.outlineVariant
}
