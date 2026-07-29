package com.elprompter.promptvault.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.elprompter.promptvault.data.Rule

@Composable
fun RuleCard(
    rule: Rule,
    hasOverlapWarning: Boolean,
    onToggleEnabled: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 4.dp)) {
        Row(modifier = Modifier.padding(12.dp)) {
            Column(modifier = Modifier.weight(1f)) {
                Text(rule.folderName, style = androidx.compose.material3.MaterialTheme.typography.titleMedium)
                Text(rule.pattern, style = androidx.compose.material3.MaterialTheme.typography.bodyMedium)
                if (hasOverlapWarning) {
                    Text(
                        "⚠ Tumpang tindih dengan rule lain",
                        color = androidx.compose.material3.MaterialTheme.colorScheme.error,
                        style = androidx.compose.material3.MaterialTheme.typography.labelSmall
                    )
                }
            }
            Switch(checked = rule.enabled, onCheckedChange = onToggleEnabled)
            IconButton(onClick = onEdit) { Icon(Icons.Filled.Edit, contentDescription = "Edit") }
            IconButton(onClick = onDelete) { Icon(Icons.Filled.Delete, contentDescription = "Hapus") }
        }
    }
}
