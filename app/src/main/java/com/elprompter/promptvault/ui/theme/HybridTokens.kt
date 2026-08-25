package com.elprompter.promptvault.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * (v8.31.1, permintaan eksplisit user: "terapkan hybrid style pada theme
 * Material 3" + klarifikasi "Cupertino style yang mau di hybrid")
 *
 * Gaya ke-4 [com.elprompter.promptvault.data.ThemeStyleOption.HYBRID] --
 * BUKAN gaya baru dari nol. Kerangka warna/tonal/shadow elevation 100%
 * jalur Material 3 flat (identik cabang MATERIAL3 di `TactileSurface.kt`,
 * `color`/`border` pemanggil dihormati apa adanya, 0 alpha/gradient
 * custom). Satu aksen ditambahkan -- signature paling khas gaya Cupertino:
 * **hairline border SELALU tampil**, bahkan saat pemanggil tidak eksplisit
 * kirim `border`. iOS secara historis mengandalkan garis tipis 1px utk
 * memisahkan list/card (bukan shadow tebal ala Material) -- 1 sentuhan ini
 * sudah cukup memberi "rasa" Cupertino tanpa merombak seluruh sistem
 * elevasi/warna M3 yang sudah stabil.
 *
 * MVP batch ini SENGAJA dibatasi ke 1 primitif (`TactileSurface`, dipakai
 * semua kartu/kontrol) + toggle-nya -- corner radius lebih besar/"continuous"
 * ala iOS, cupertino-style bottom sheet/dialog, dst adalah PERLUASAN
 * terpisah (di luar scope 1 batch ini, JANGAN diasumsikan sudah termasuk).
 */
object HybridTokens {
    /** Lebar hairline -- 1px fisik, konvensi iOS (`UIView` separator
     * default juga 1 device pixel, bukan 1dp tebal). */
    val HairlineWidth: Dp = 0.75.dp

    /** Warna hairline: reuse `outlineVariant` (SUDAH ADA, 0 hue baru) --
     * cukup redup utk pemisah, tidak mendominasi spt border Neumorphism. */
    @Composable
    fun hairlineColor(): Color = MaterialTheme.colorScheme.outlineVariant
}
