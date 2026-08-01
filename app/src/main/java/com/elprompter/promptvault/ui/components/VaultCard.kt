package com.elprompter.promptvault.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Kartu bersih ala iOS grouped list: radius besar, border rambut tipis.
 *
 * v2.3.0: kartu diberi gradient vertikal halus supaya terasa berlapis, bukan
 * datar (keluhan "monoton").
 * v2.3.1 (FIX REGRESI): implementasi v2.3.0 salah taruh `Modifier.fillMaxSize()`
 * di Box pembungkus gradient. Karena Surface aslinya membungkus tinggi sesuai
 * konten (wrap-content), `fillMaxSize()` di dalamnya justru memaksa kartu ini
 * merebut SISA SELURUH tinggi layar yang tersedia di Column induknya --
 * akibatnya tombol "Scan Sekarang" & menu di bawahnya kedorong keluar area
 * yang terlihat. Sekarang gradient digambar langsung lewat `Modifier.background`
 * pada Surface itu sendiri (tanpa Box/fillMaxSize tambahan), sehingga ukuran
 * kartu kembali mengikuti kontennya seperti semula.
 */
@Composable
fun VaultCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        modifier = modifier.background(
            brush = Brush.verticalGradient(colors = listOf(colors.surfaceVariant, colors.surface)),
            shape = MaterialTheme.shapes.medium
        ),
        shape = MaterialTheme.shapes.medium,
        color = Color.Transparent,
        border = BorderStroke(1.dp, colors.outline),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        content()
    }
}
