package com.elprompter.promptvault.ui.components

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import com.elprompter.promptvault.ui.theme.TactileTokens

/**
 * Satu baris menu ala grouped list Settings: ikon berwarna di kotak
 * membulat, label, chevron di kanan. Dipakai berkelompok di dalam GroupedList.
 * tint = null berarti pakai warna primary tema secara otomatis (theme-aware);
 * boleh dioverride eksplisit (mis. tertiary) lewat MaterialTheme.colorScheme.
 *
 * v8.0.0 -- Glassmorphism -> Material 3 murni: kotak ikon sekarang
 * `TactileSurface` (Surface M3 baku, tonal+shadow elevation, TANPA border
 * hairline kaca), warna dasar `colorScheme.surfaceContainerHigh` (peran M3
 * baku). Overlay dua-stop gradient tint diganti fill SOLID alpha rendah --
 * gradient bevel adalah bahasa Glassmorphism, bukan M3; identitas warna
 * per-menu (bab "menu tidak monoton satu warna") tetap ada lewat fill tint
 * + ikon, hanya teknik overlay-nya yang disederhanakan.
 */
@Composable
fun GroupedListRow(icon: ImageVector, label: String, tint: Color? = null, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val resolvedTint = tint ?: colors.primary
    val interactionSource = remember { MutableInteractionSource() }
    // UI-05 fix: sebelumnya `selectable(..., indication = null)` -- tidak
    // ada ripple/visual pressed state sama sekali, row terlihat statis
    // walau clickable. Sekarang pakai `clickable` + `LocalIndication.current`
    // (indication default platform, otomatis theme-aware) supaya tap selalu
    // punya sinyal visual jelas selain berpindah layar.
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(interactionSource = interactionSource, indication = LocalIndication.current, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TactileSurface(
            modifier = Modifier.size(30.dp),
            shape = MaterialTheme.shapes.small,
            color = colors.surfaceContainerHigh,
            elevation = TactileTokens.TactileElevationControl
        ) {
            Box(
                modifier = Modifier.background(resolvedTint.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = resolvedTint, modifier = Modifier.size(16.dp))
            }
        }
        Text(
            label,
            style = MaterialTheme.typography.bodyLarge,
            color = colors.onSurface,
            modifier = Modifier.weight(1f).padding(start = 12.dp)
        )
        Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = colors.onSurfaceVariant, modifier = Modifier.size(20.dp))
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
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outline,
                        thickness = 1.dp,
                        modifier = Modifier.padding(start = 58.dp)
                    )
                }
            }
        }
    }
}
