package com.elprompter.promptvault.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.elprompter.promptvault.data.Rule
import com.elprompter.promptvault.ui.components.ConfirmDialog
import com.elprompter.promptvault.ui.components.RuleCard

@Composable
fun RuleListScreen(
    rules: List<Rule>,
    overlappingRuleIds: Set<String>,
    onToggleEnabled: (Rule, Boolean) -> Unit,
    onEditRule: (Rule) -> Unit,
    onDeleteRule: (Rule) -> Unit,
    onAddRule: () -> Unit
) {
    // TODO #8: pencarian/filter rule
    var query by remember { mutableStateOf("") }
    var pendingDelete by remember { mutableStateOf<Rule?>(null) }

    val filtered = remember(rules, query) {
        if (query.isBlank()) rules
        else rules.filter {
            it.folderName.contains(query, ignoreCase = true) || it.pattern.contains(query, ignoreCase = true)
        }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = onAddRule) { Icon(Icons.Filled.Add, contentDescription = "Tambah rule") }
        }
    ) { padding ->
        Column(modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(16.dp)) {
            Text("Daftar Rule", style = MaterialTheme.typography.headlineSmall)
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text("Cari rule (nama folder / pattern)") },
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
            )

            if (filtered.isEmpty()) {
                Text(
                    if (rules.isEmpty()) "Belum ada rule. Tekan + untuk menambah."
                    else "Tidak ada rule yang cocok dengan pencarian.",
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    items(filtered, key = { it.id }) { rule ->
                        RuleCard(
                            rule = rule,
                            hasOverlapWarning = overlappingRuleIds.contains(rule.id),
                            onToggleEnabled = { onToggleEnabled(rule, it) },
                            onEdit = { onEditRule(rule) },
                            onDelete = { pendingDelete = rule }
                        )
                    }
                }
            }
        }
    }

    pendingDelete?.let { rule ->
        ConfirmDialog(
            title = "Hapus rule?",
            message = "Rule \"${rule.folderName}\" (${rule.pattern}) akan dihapus. Tindakan ini tidak bisa dibatalkan.",
            confirmLabel = "Hapus",
            onConfirm = {
                onDeleteRule(rule)
                pendingDelete = null
            },
            onDismiss = { pendingDelete = null }
        )
    }
}
