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
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.elprompter.promptvault.data.Rule
import com.elprompter.promptvault.ui.components.VaultActionSheet
import com.elprompter.promptvault.ui.components.RuleCard
import com.elprompter.promptvault.ui.components.VaultTopBar
import kotlinx.coroutines.launch

@Composable
fun RuleListScreen(
    rules: List<Rule>,
    overlappingRuleIds: Set<String>,
    onToggleEnabled: (Rule, Boolean) -> Unit,
    onMoveUp: (Rule) -> Unit,
    onMoveDown: (Rule) -> Unit,
    onEditRule: (Rule) -> Unit,
    onDeleteRule: (Rule) -> Unit,
    onAddRule: () -> Unit,
    onBack: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    var pendingDelete by remember { mutableStateOf<Rule?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val colors = MaterialTheme.colorScheme

    val filtered = remember(rules, query) {
        if (query.isBlank()) rules
        else rules.filter {
            it.folderName.contains(query, ignoreCase = true) || it.pattern.contains(query, ignoreCase = true)
        }
    }
    // Reorder prioritas cuma bermakna kalau daftar tidak lagi difilter pencarian.
    val reorderEnabled = query.isBlank()

    Scaffold(
        topBar = { VaultTopBar(title = "Kelola Rule", onBack = onBack) },
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                Snackbar(snackbarData = data, containerColor = colors.primary, contentColor = colors.onPrimary)
            }
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddRule, containerColor = colors.primary, contentColor = colors.onPrimary) {
                Icon(Icons.Filled.Add, contentDescription = "Tambah rule")
            }
        }
    ) { padding ->
        Column(modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(16.dp)) {
            Text(
                "Urutan di bawah = prioritas. Kalau satu file cocok lebih dari satu rule, rule paling atas yang menang.",
                style = MaterialTheme.typography.bodySmall
            )
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
                        val globalIndex = rules.indexOfFirst { it.id == rule.id }
                        RuleCard(
                            rule = rule,
                            priority = globalIndex + 1,
                            hasOverlapWarning = overlappingRuleIds.contains(rule.id),
                            canMoveUp = reorderEnabled && globalIndex > 0,
                            canMoveDown = reorderEnabled && globalIndex < rules.lastIndex,
                            onToggleEnabled = { onToggleEnabled(rule, it) },
                            onMoveUp = { onMoveUp(rule) },
                            onMoveDown = { onMoveDown(rule) },
                            onEdit = { onEditRule(rule) },
                            onDelete = { pendingDelete = rule }
                        )
                    }
                }
            }
        }
    }

    pendingDelete?.let { rule ->
        VaultActionSheet(
            title = "Hapus rule?",
            message = "Rule \"${rule.folderName}\" (${rule.pattern}) akan dihapus. Tindakan ini tidak bisa dibatalkan.",
            confirmLabel = "Hapus",
            isDestructive = true,
            onConfirm = {
                onDeleteRule(rule)
                pendingDelete = null
                scope.launch { snackbarHostState.showSnackbar("Rule \"${rule.folderName}\" dihapus") }
            },
            onDismiss = { pendingDelete = null }
        )
    }
}
