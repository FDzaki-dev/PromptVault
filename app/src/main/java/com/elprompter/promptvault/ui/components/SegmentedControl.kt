package com.elprompter.promptvault.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Segmented control ala iOS (pil berisi, bukan garis bawah Material) --
 * lebih jelas mana yang aktif, dan terasa lebih "sentuh" di layar sempit.
 * Semua warna theme-aware supaya kontrasnya tetap benar di dark mode.
 * v4.0.0: track pembungkus jadi neumorphic inset (tenggelam) supaya pil
 * yang terpilih terbaca "duduk di dalam sumur" -- prinsip Titanium
 * Neumorphism yang sama dengan track TactileSwitch.
 */
@Composable
fun SegmentedControl(options: List<String>, selectedIndex: Int, onSelect: (Int) -> Unit) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .neuInset(shape = RoundedCornerShape(12.dp))
            .padding(3.dp)
    ) {
        options.forEachIndexed { index, label ->
            val selected = index == selectedIndex
            val bg by animateColorAsState(if (selected) colors.primary else colors.surfaceVariant, label = "segmentBg")
            val fg = if (selected) colors.onPrimary else colors.primary
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onSelect(index) }
                    .background(bg, RoundedCornerShape(10.dp))
                    .padding(vertical = 9.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(label, color = fg, style = MaterialTheme.typography.titleSmall)
            }
        }
    }
}
