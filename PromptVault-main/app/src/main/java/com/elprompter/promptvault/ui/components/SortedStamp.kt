package com.elprompter.promptvault.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.unit.dp

/**
 * Satu-satunya "kejutan" visual di app ini: badge stempel untuk entri log
 * SUKSES, meniru stempel tinta di kartu arsip. Dipakai sengaja hanya di satu
 * tempat (bukan di mana-mana) supaya tetap terasa istimewa, bukan dekorasi.
 */
@Composable
fun SortedStamp(text: String = "SORTED") {
    val accent = MaterialTheme.colorScheme.secondary
    Box(
        modifier = Modifier
            .rotate(-6f)
            .border(BorderStroke(1.5.dp, accent), RoundedCornerShape(4.dp))
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Text(
            text,
            color = accent,
            style = MaterialTheme.typography.labelSmall
        )
    }
}
