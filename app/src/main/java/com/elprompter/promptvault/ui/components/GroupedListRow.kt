package com.elprompter.promptvault.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.elprompter.promptvault.ui.theme.CardPaper
import com.elprompter.promptvault.ui.theme.HairlineInk
import com.elprompter.promptvault.ui.theme.InkFaint
import com.elprompter.promptvault.ui.theme.Pine

/**
 * Satu baris menu ala grouped list iOS Settings: ikon berwarna di kotak
 * membulat, label, chevron di kanan. Dipakai berkelompok di dalam GroupedList.
 */
@Composable
fun GroupedListRow(icon: ImageVector, label: String, tint: Color = Pine, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = false, interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .background(tint, RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = CardPaper, modifier = Modifier.size(18.dp))
        }
        Text(
            label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f).padding(start = 12.dp)
        )
        Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = InkFaint, modifier = Modifier.size(20.dp))
    }
}

/** Kartu pembungkus grouped list, dengan garis pemisah tipis antar baris. */
@Composable
fun GroupedList(rows: List<@Composable () -> Unit>) {
    VaultCard(modifier = Modifier.fillMaxWidth()) {
        androidx.compose.foundation.layout.Column {
            rows.forEachIndexed { index, row ->
                row()
                if (index != rows.lastIndex) {
                    HorizontalDivider(color = HairlineInk, thickness = 1.dp, modifier = Modifier.padding(start = 58.dp))
                }
            }
        }
    }
}
