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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.elprompter.promptvault.ui.theme.GlassSurfaceElevated
import com.elprompter.promptvault.ui.theme.TactileTokens

/**
 * Satu baris menu ala grouped list iOS Settings: ikon berwarna di kotak
 * membulat, label, chevron di kanan. Dipakai berkelompok di dalam GroupedList.
 * tint = null berarti pakai warna primary tema secara otomatis (theme-aware);
 * boleh dioverride eksplisit (mis. Amber/tertiary) lewat MaterialTheme.colorScheme.
 *
 * v3.0.1 -- fix pelanggaran bab 18 spesifikasi tema ("Glow forbidden: Every
 * icon"): sebelumnya SETIAP baris menu (bukan cuma yang selected/focused)
 * punya `Modifier.shadow()` berwarna tint yang terlihat sebagai glow
 * permanen -- 4 glow tampil bersamaan tiap kali Home dibuka, jelas
 * melanggar golden rule "user notice glass/AMOLED dulu, bukan glow".
 * Diganti kotak ikon glass datar (bab 4/7/14: tint hanya lewat fill alpha
 * rendah + border rambut, TANPA shadow berwarna) -- identitas warna per
 * menu tetap ada lewat isi & border, bukan lewat cahaya yang menyala.
 *
 * v4.0.0 -- "ultra immersive depth/3D" (permintaan eksplisit user, MENGGANTI
 * keputusan v3.0.1 di atas soal shadow): kotak ikon sekarang dapat elevasi
 * NYATA tapi kecil & NETRAL, shadow bawaan Material3 -- bukan `spotColor`
 * berwarna tint -- supaya tetap beda dari "glow" yang dilarang bab 18 (glow =
 * cahaya BERWARNA menyala; ini cuma bayangan abu-abu netral kecil, sinyal
 * "terangkat", bukan "menyala").
 *
 * v5.0.0 -- Redesign Glassmorphism -> Neumorphism: kotak ikon sekarang pakai
 * `NeumorphicSurface` (shadow ganda kecil, [TactileTokens.NeuElevationControl]/
 * [TactileTokens.NeuOffsetControl]) menggantikan `Surface` + border rambut +
 * shadow tunggal netral. Border [GlassBorder] DIHAPUS dari komponen ini (efek
 * "tepi kaca" bertentangan dgn neumorphism -- sisi kartu neumorphic TIDAK
 * bergaris tepi, kedalamannya murni dari shadow). Tint warna per-menu (bab
 * "menu tidak monoton satu warna", Keputusan Arsitektur #3) TETAP dipertahankan
 * PERSIS SAMA lewat overlay gradient tint [resolvedTint] di dalam Surface --
 * tidak berubah dari v4.0.0, hanya wadahnya (Surface polos -> NeumorphicSurface)
 * yang berubah.
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
        NeumorphicSurface(
            modifier = Modifier.size(30.dp),
            shape = MaterialTheme.shapes.small,
            color = GlassSurfaceElevated,
            elevation = TactileTokens.NeuElevationControl,
            shadowOffset = TactileTokens.NeuOffsetControl
        ) {
            Box(
                modifier = Modifier.background(
                    Brush.linearGradient(
                        colors = listOf(resolvedTint.copy(alpha = 0.22f), resolvedTint.copy(alpha = 0.12f))
                    )
                ),
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
