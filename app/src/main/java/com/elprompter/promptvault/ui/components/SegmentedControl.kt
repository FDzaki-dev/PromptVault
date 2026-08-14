package com.elprompter.promptvault.ui.components

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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.elprompter.promptvault.ui.theme.TactileTokens

/**
 * Segmented control ala iOS (pil berisi, bukan garis bawah Material) --
 * lebih jelas mana yang aktif, dan terasa lebih "sentuh" di layar sempit.
 * Semua warna theme-aware supaya kontrasnya tetap benar di dark mode.
 *
 * v5.0.0 -- Redesign Glassmorphism -> Neumorphism: wadah track sekarang
 * [NeumorphicSurface] TENGGELAM (`pressed = true`, alur konsisten dengan
 * track `TactileSwitch`) supaya terbaca sebagai "slot" -- pilihan aktif
 * digambar sebagai pil [NeumorphicSurface] TIMBUL kecil di atasnya (dual-
 * shadow, [TactileTokens.NeuElevationControl]), pilihan tidak-aktif rata
 * tanpa shadow. Warna `colors.primary`/`colors.surfaceVariant` TIDAK berubah.
 */
@Composable
fun SegmentedControl(options: List<String>, selectedIndex: Int, onSelect: (Int) -> Unit) {
    val colors = MaterialTheme.colorScheme
    NeumorphicSurface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = colors.surfaceVariant,
        pressed = true,
        elevation = TactileTokens.NeuElevationControl,
        shadowOffset = TactileTokens.NeuOffsetControl
    ) {
        Row(modifier = Modifier.padding(3.dp)) {
            options.forEachIndexed { index, label ->
                val selected = index == selectedIndex
                val interactionSource = remember { MutableInteractionSource() }
                if (selected) {
                    NeumorphicSurface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        color = colors.primary,
                        baseColor = colors.surfaceVariant,
                        elevation = TactileTokens.NeuElevationControl,
                        shadowOffset = TactileTokens.NeuOffsetControl,
                        onClick = { onSelect(index) },
                        interactionSource = interactionSource
                    ) {
                        Text(
                            label,
                            color = colors.onPrimary,
                            style = MaterialTheme.typography.titleSmall,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 9.dp)
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clickable(
                                interactionSource = interactionSource,
                                indication = null
                            ) { onSelect(index) }
                            .padding(vertical = 9.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(label, color = colors.primary, style = MaterialTheme.typography.titleSmall)
                    }
                }
            }
        }
    }
}
