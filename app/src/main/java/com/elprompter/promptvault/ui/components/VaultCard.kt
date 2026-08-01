package com.elprompter.promptvault.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp

/**
 * Kartu bersih ala iOS grouped list: radius besar, border rambut tipis.
 *
 * v2.3.0: sebelumnya warna kartu rata satu warna solid dari atas sampai bawah
 * (terasa datar/monoton, apalagi di dark mode yang serba gelap). Sekarang diberi
 * gradient vertikal SANGAT halus (lapisan atas sedikit lebih terang) supaya kartu
 * terasa punya kedalaman/"napas", tanpa jadi berlebihan seperti neumorphism atau
 * bayangan Material default yang generik.
 */
@Composable
fun VaultCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = androidx.compose.ui.graphics.Color.Transparent,
        border = BorderStroke(1.dp, colors.outline),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        androidx.compose.foundation.layout.Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(colors.surfaceVariant, colors.surface)
                    )
                )
        ) {
            content()
        }
    }
}
