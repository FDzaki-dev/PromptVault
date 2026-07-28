package com.fdzaki.promptvault.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.fdzaki.promptvault.data.SortLogEntry
import com.fdzaki.promptvault.data.SortRule

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultScreen(
    rules: List<SortRule>,
    logs: List<SortLogEntry>,
    hasStoragePermission: Boolean,
    onRequestPermission: () -> Unit,
    onScanNow: () -> Unit,
    onAddRule: (pattern: String, folder: String) -> Unit,
    onDeleteRule: (pattern: String) -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("PromptVault") })
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add rule")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize()
        ) {
            if (!hasStoragePermission) {
                PermissionBanner(onRequestPermission)
                Spacer(Modifier.height(16.dp))
            }

            Button(
                onClick = onScanNow,
                enabled = hasStoragePermission,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Scan Downloads Now")
            }

            Spacer(Modifier.height(20.dp))
            Text("Sorting Rules", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))

            LazyColumn(modifier = Modifier.weight(1f)) {
                items(rules, key = { it.pattern }) { rule ->
                    RuleRow(rule = rule, onDelete = { onDeleteRule(rule.pattern) })
                }
                if (logs.isNotEmpty()) {
                    item {
                        Spacer(Modifier.height(20.dp))
                        Text("Recent Activity", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(8.dp))
                    }
                    items(logs.takeLast(20).reversed()) { log ->
                        LogRow(log)
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddRuleDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { pattern, folder ->
                onAddRule(pattern, folder)
                showAddDialog = false
            }
        )
    }
}

@Composable
private fun PermissionBanner(onRequestPermission: () -> Unit) {
    Card {
        Column(Modifier.padding(16.dp)) {
            Text(
                "PromptVault needs full storage access to move files inside Downloads.",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(8.dp))
            Button(onClick = onRequestPermission) {
                Text("Grant Access")
            }
        }
    }
}

@Composable
private fun RuleRow(rule: SortRule, onDelete: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(rule.pattern, style = MaterialTheme.typography.bodyLarge)
            Text(
                "→ Downloads/PromptVault/${rule.folderName}/",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary
            )
        }
        IconButton(onClick = onDelete) {
            Icon(Icons.Default.Delete, contentDescription = "Delete rule")
        }
    }
}

@Composable
private fun LogRow(log: SortLogEntry) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Text(
            "${log.fileName} → ${log.destinationFolder}/",
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
private fun AddRuleDialog(
    onDismiss: () -> Unit,
    onConfirm: (pattern: String, folder: String) -> Unit
) {
    var pattern by remember { mutableStateOf("") }
    var folder by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Sort Rule") },
        text = {
            Column {
                OutlinedTextField(
                    value = pattern,
                    onValueChange = { pattern = it },
                    label = { Text("Filename pattern (e.g. AudioPlayer*)") },
                    singleLine = true
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = folder,
                    onValueChange = { folder = it },
                    label = { Text("Destination folder name") },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (pattern.isNotBlank() && folder.isNotBlank()) onConfirm(pattern, folder) }
            ) { Text("Add") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
