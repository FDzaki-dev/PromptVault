package com.elprompter.promptvault.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.elprompter.promptvault.ui.theme.HairlineInk

/**
 * Kartu bergaya "index card arsip": strip tab kecil di kiri-atas seperti map
 * gantung, border tipis tinta, tanpa bayangan Material default yang generik.
 * Ini elemen signature yang dipakai konsisten di semua layar -- satu motif,
 * bukan dekorasi ganda-ganda di banyak tempat.
 */
@Composable
fun VaultCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(6.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, HairlineInk),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Box {
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .width(44.dp)
                    .height(5.dp)
                    .clip(RoundedCornerShape(bottomEnd = 4.dp))
                    .background(MaterialTheme.colorScheme.primary)
            )
            content()
        }
    }
}
