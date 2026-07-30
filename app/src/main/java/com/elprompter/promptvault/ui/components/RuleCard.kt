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
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.elprompter.promptvault.data.Rule
import com.elprompter.promptvault.ui.theme.Amber
import com.elprompter.promptvault.ui.theme.InkFaint
import com.elprompter.promptvault.ui.theme.Pine

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
    onDelete: () -> Unit
) {
    VaultCard(modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 4.dp)) {
        Row(modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 4.dp, bottom = 10.dp)) {
            Column {
                IconButton(onClick = onMoveUp, enabled = canMoveUp, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Filled.KeyboardArrowUp, contentDescription = "Naikkan prioritas", tint = if (canMoveUp) Pine else InkFaint)
                }
                IconButton(onClick = onMoveDown, enabled = canMoveDown, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "Turunkan prioritas", tint = if (canMoveDown) Pine else InkFaint)
                }
            }
            Column(modifier = Modifier.weight(1f).padding(start = 4.dp)) {
                Text("PRIORITAS #$priority", style = MaterialTheme.typography.labelSmall, color = InkFaint)
                Text(rule.folderName.uppercase(), style = MaterialTheme.typography.titleMedium)
                Text(
                    rule.pattern,
                    style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                    color = InkFaint
                )
                if (rule.excludePattern.isNotBlank()) {
                    Text(
                        "kecuali: ${rule.excludePattern}",
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                        color = InkFaint
                    )
                }
                if (hasOverlapWarning) {
                    Text(
                        "⚠ tumpang tindih dengan rule lain",
                        color = Amber,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
            Switch(
                checked = rule.enabled,
                onCheckedChange = onToggleEnabled,
                colors = SwitchDefaults.colors(checkedTrackColor = Pine)
            )
            IconButton(onClick = onEdit) { Icon(Icons.Filled.Edit, contentDescription = "Edit") }
            IconButton(onClick = onDelete) { Icon(Icons.Filled.Delete, contentDescription = "Hapus") }
        }
    }
}
