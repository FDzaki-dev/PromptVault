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
    // [Fix audit P2 #UI-18, 2026-08-15] Sebelumnya `selected = index ==
    // selectedIndex` polos -- kalau caller kirim index di luar range
    // options (mis. 0-based vs 1-based ketukar, atau options berubah tapi
    // state index belum di-reset), TIDAK ADA segment yang keliatan
    // terpilih, bukan crash tapi state visual jadi hilang tanpa jejak.
    // Diklem ke range valid di sini (boundary component, bukan di tiap
    // caller) supaya selalu ada 1 segment terpilih selama `options` tidak
    // kosong -- hardening, belum ada laporan bug aktif dari ini.
    val effectiveIndex = if (options.isEmpty()) -1 else selectedIndex.coerceIn(0, options.lastIndex)
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
                val selected = index == effectiveIndex
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
                    // [Fix audit P2 #UI-19, 2026-08-15] Sebelumnya segment TIDAK
                    // terpilih benar-benar NOL feedback tekan (indication=null,
                    // tanpa scale) -- beda dari segment terpilih yang otomatis
                    // dapat ripple bawaan `NeumorphicSurface(onClick=...)`. Bukan
                    // sekadar "beda gaya" (ripple di list biasa vs scale di
                    // kontrol neumorphic itu memang pola desain sengaja, lihat
                    // dokumentasi `PressScale.kt`/`Neumorphic.kt`) -- ini gap
                    // NYATA: segment ini sama sekali tidak dapat KEDUANYA.
                    // `pressScale()` dipakai (bukan ripple) supaya konsisten dgn
                    // keluarga kontrol neumorphic lain (CTA Home, TactileSwitch),
                    // bukan menambah pola feedback ketiga.
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .pressScale(interactionSource)
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
