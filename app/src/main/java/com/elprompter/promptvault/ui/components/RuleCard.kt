package com.elprompter.promptvault.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.elprompter.promptvault.R
import com.elprompter.promptvault.data.Rule

@Composable
fun RuleCard(
    rule: Rule,
    priority: Int,
    hasOverlapWarning: Boolean,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onToggleEnabled: (Boolean) -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptics = LocalHapticFeedback.current
    val colors = MaterialTheme.colorScheme
    // UI-03 fix: layout 2-baris. Sebelumnya SEMUA kontrol (reorder, teks,
    // switch, edit, delete) dipaksa dalam 1 Row -- lebar teks jadi sangat
    // sempit di device sempit/nama panjang. Sekarang metadata (baris atas)
    // dan kontrol aksi (baris bawah) dipisah, masing-masing dapat ruang
    // penuh. Semua kontrol interaktif di baris aksi disamakan ke touch
    // target minimum ~48dp (bukan 28dp seperti sebelumnya).
    VaultCard(modifier = modifier
        .fillMaxWidth()
        .padding(vertical = 4.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Baris atas: metadata rule. maxLines+overflow terencana supaya
            // nama folder/pattern panjang tidak memaksa komposisi melebar
            // tak terkendali (UI-03), dan casing asli dipertahankan (UI-14
            // -- sebelumnya .uppercase() paksa, kini nama tampil apa adanya,
            // label "PRIORITAS #n" tetap kecil/uppercase krn itu memang
            // label, bukan identitas nama folder).
            Text(stringResource(R.string.rule_card_priority_label, priority), style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
            Text(
                rule.folderName,
                style = MaterialTheme.typography.titleMedium,
                color = colors.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                rule.pattern,
                style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                color = colors.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (rule.excludePattern.isNotBlank()) {
                Text(
                    stringResource(R.string.rule_card_exclude_prefix, rule.excludePattern),
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                    color = colors.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (hasOverlapWarning) {
                Text(
                    stringResource(R.string.rule_card_overlap_warning),
                    color = colors.tertiary,
                    style = MaterialTheme.typography.labelSmall
                )
            }

            Spacer(modifier = Modifier.size(8.dp))

            // [Fix UI polish, 2026-08-16] Baris kontrol SEBELUMNYA
            // `spacedBy(4.dp)` + `Spacer(weight(1f))` di tengah -- efeknya
            // chevron naik/turun numpuk RAPAT di ujung kiri, sementara
            // switch+edit+hapus numpuk RAPAT di ujung kanan, nyisain 1
            // celah kosong lebar persis di tengah (asimetris, bukan
            // proporsional). `Arrangement.SpaceEvenly` (TANPA Spacer
            // manual) sebarkan jarak antar SEMUA 5 kontrol secara merata
            // di lebar penuh -- baris jadi seimbang kiri-kanan, bukan cuma
            // 2 gerombolan nempel tepi.
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                IconButton(onClick = onMoveUp, enabled = canMoveUp, modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)) {
                    Icon(Icons.Filled.KeyboardArrowUp, contentDescription = stringResource(R.string.rule_card_move_up_cd), tint = if (canMoveUp) colors.primary else colors.onSurfaceVariant)
                }
                IconButton(onClick = onMoveDown, enabled = canMoveDown, modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)) {
                    Icon(Icons.Filled.KeyboardArrowDown, contentDescription = stringResource(R.string.rule_card_move_down_cd), tint = if (canMoveDown) colors.primary else colors.onSurfaceVariant)
                }
                TactileSwitch(
                    checked = rule.enabled,
                    onCheckedChange = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        onToggleEnabled(it)
                    },
                    accentColor = colors.primary
                )
                IconButton(onClick = onEdit, modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)) {
                    Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.action_edit), tint = colors.onSurfaceVariant)
                }
                IconButton(onClick = onDelete, modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)) {
                    Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.action_delete), tint = colors.onSurfaceVariant)
                }
            }
        }
    }
}
