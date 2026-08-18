package com.elprompter.promptvault.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.elprompter.promptvault.ui.theme.TactileTokens

/**
 * Segmented control (pil berisi, bukan garis bawah tab Material biasa) --
 * lebih jelas mana yang aktif, dan terasa lebih "sentuh" di layar sempit.
 * Semua warna theme-aware supaya kontrasnya tetap benar.
 *
 * v8.0.0 -- Glassmorphism -> Material 3 murni: wadah track sekarang
 * [TactileSurface] `recessed = true` (Surface M3 baku, tonal+shadow
 * elevation, TANPA border kaca) supaya terbaca sebagai "slot" -- pilihan
 * aktif digambar sebagai pil [TactileSurface] TIMBUL kecil di atasnya
 * ([TactileTokens.TactileElevationControl]), pilihan tidak-aktif rata tanpa
 * elevasi. Warna `colors.primary`/`colors.surfaceVariant` TIDAK berubah.
 *
 * [Roadmap Fase 1.2, 2026-08-18] 2 fix TalkBack/touch-target, KEDUANYA
 * ADITIF (tidak menyentuh `onClick`/`interactionSource`/`pressScale` yang
 * sudah teruji, nol risiko regresi interaksi):
 * 1. Dulu TIDAK ADA semantics `selected`/`Role.Tab` sama sekali -- screen
 *    reader cuma baca label polos ("Log", "Undo Pemindahan") tanpa pernah
 *    bilang mana yang aktif atau bahwa ini grup pilihan tunggal. Ditambah
 *    `Modifier.semantics { role = Role.Tab; selected = ... }` per segmen +
 *    `Modifier.selectableGroup()` di Row induk.
 * 2. Padding vertikal tiap segmen 9dp -> 14dp (tinggi total ~38dp -> 48dp,
 *    titleSmall lineHeight 20dp + 14+14 = 48dp) -- di bawah standar target
 *    sentuh minimum Android (48dp) sebelumnya.
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
    TactileSurface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = colors.surfaceVariant,
        recessed = true
    ) {
        Row(modifier = Modifier.padding(3.dp).selectableGroup()) {
            options.forEachIndexed { index, label ->
                val selected = index == effectiveIndex
                val interactionSource = remember { MutableInteractionSource() }
                if (selected) {
                    TactileSurface(
                        modifier = Modifier
                            .weight(1f)
                            .semantics { role = Role.Tab; this.selected = true },
                        shape = RoundedCornerShape(10.dp),
                        color = colors.primary,
                        elevation = TactileTokens.TactileElevationControl,
                        onClick = { onSelect(index) },
                        interactionSource = interactionSource
                    ) {
                        Text(
                            label,
                            color = colors.onPrimary,
                            style = MaterialTheme.typography.titleSmall,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp)
                        )
                    }
                } else {
                    // [Fix audit P2 #UI-19, 2026-08-15] Sebelumnya segment TIDAK
                    // terpilih benar-benar NOL feedback tekan (indication=null,
                    // tanpa scale) -- beda dari segment terpilih yang otomatis
                    // dapat ripple bawaan `TactileSurface(onClick=...)`. Bukan sekadar
                    // "beda gaya" (ripple di list biasa vs scale di kontrol
                    // tactile itu memang pola desain sengaja, lihat dokumentasi
                    // `PressScale.kt`) -- ini gap NYATA: segment ini sama sekali
                    // tidak dapat KEDUANYA. `pressScale()` dipakai (bukan ripple)
                    // supaya konsisten dgn keluarga kontrol tactile lain (CTA
                    // Home, TactileSwitch), bukan menambah pola feedback ketiga.
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .semantics { role = Role.Tab; this.selected = false }
                            .pressScale(interactionSource)
                            .clickable(
                                interactionSource = interactionSource,
                                indication = null
                            ) { onSelect(index) }
                            .padding(vertical = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(label, color = colors.primary, style = MaterialTheme.typography.titleSmall)
                    }
                }
            }
        }
    }
}
