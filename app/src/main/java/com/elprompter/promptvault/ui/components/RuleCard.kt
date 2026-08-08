package com.elprompter.promptvault.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
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
    VaultCard(modifier = modifier
        .fillMaxWidth()
        .padding(vertical = 4.dp)) {
        Row(modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 4.dp, bottom = 10.dp)) {
            Column {
                IconButton(onClick = onMoveUp, enabled = canMoveUp, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Filled.KeyboardArrowUp, contentDescription = "Naikkan prioritas", tint = if (canMoveUp) colors.primary else colors.onSurfaceVariant)
                }
                IconButton(onClick = onMoveDown, enabled = canMoveDown, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "Turunkan prioritas", tint = if (canMoveDown) colors.primary else colors.onSurfaceVariant)
                }
            }
            Column(modifier = Modifier.weight(1f).padding(start = 4.dp)) {
                Text("PRIORITAS #$priority", style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
                Text(rule.folderName.uppercase(), style = MaterialTheme.typography.titleMedium, color = colors.onSurface)
                Text(
                    rule.pattern,
                    style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                    color = colors.onSurfaceVariant
                )
                if (rule.excludePattern.isNotBlank()) {
                    Text(
                        "kecuali: ${rule.excludePattern}",
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                        color = colors.onSurfaceVariant
                    )
                }
                if (hasOverlapWarning) {
                    Text(
                        "⚠ tumpang tindih dengan rule lain",
                        color = colors.tertiary,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
            TactileSwitch(
                checked = rule.enabled,
                onCheckedChange = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onToggleEnabled(it)
                },
                accentColor = colors.primary
            )
            IconButton(onClick = onEdit) { Icon(Icons.Filled.Edit, contentDescription = "Edit", tint = colors.onSurfaceVariant) }
            IconButton(onClick = onDelete) { Icon(Icons.Filled.Delete, contentDescription = "Hapus", tint = colors.onSurfaceVariant) }
        }
    }
}
