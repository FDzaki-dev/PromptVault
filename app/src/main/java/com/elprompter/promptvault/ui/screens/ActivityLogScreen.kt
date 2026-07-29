package com.elprompter.promptvault.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.elprompter.promptvault.data.ActivityLogEntry
import com.elprompter.promptvault.data.LogLevel
import com.elprompter.promptvault.data.MoveHistoryEntry
import com.elprompter.promptvault.ui.components.ConfirmDialog
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ActivityLogScreen(
    logEntries: List<ActivityLogEntry>,
    undoableHistory: List<MoveHistoryEntry>,
    onUndo: (MoveHistoryEntry) -> Unit
) {
    var tab by remember { mutableStateOf(0) }
    var pendingUndo by remember { mutableStateOf<MoveHistoryEntry?>(null) }
    val formatter = remember { SimpleDateFormat("dd MMM HH:mm", Locale("id", "ID")) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Riwayat Aktivitas", style = MaterialTheme.typography.headlineSmall)

        TabRow(selectedTabIndex = tab) {
            Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Log") })
            Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("Undo Pemindahan") })
        }

        if (tab == 0) {
            if (logEntries.isEmpty()) {
                Text("Belum ada aktivitas.", modifier = Modifier.padding(top = 12.dp))
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(top = 8.dp)) {
                    items(logEntries, key = { it.id }) { entry ->
                        val color = when (entry.level) {
                            LogLevel.SUCCESS -> MaterialTheme.colorScheme.primary
                            LogLevel.WARNING -> androidx.compose.ui.graphics.Color(0xFFCC8B00)
                            LogLevel.ERROR -> MaterialTheme.colorScheme.error
                            LogLevel.INFO -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(formatter.format(Date(entry.timestampMillis)), style = MaterialTheme.typography.labelSmall)
                                Text(entry.message, color = color, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            }
        } else {
            // TODO #1: fitur UNDO — file yang salah pindah kini bisa dikembalikan dari dalam app.
            val undoable = undoableHistory.filter { !it.undone }
            if (undoable.isEmpty()) {
                Text("Tidak ada pemindahan yang bisa di-undo.", modifier = Modifier.padding(top = 12.dp))
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(top = 8.dp)) {
                    items(undoable, key = { it.id }) { entry ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Row(modifier = Modifier.padding(10.dp)) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(entry.fileName, style = MaterialTheme.typography.bodyMedium)
                                    Text("Ke: PromptVault/${entry.ruleFolderName}/", style = MaterialTheme.typography.labelSmall)
                                    Text(formatter.format(Date(entry.timestampMillis)), style = MaterialTheme.typography.labelSmall)
                                }
                                TextButton(onClick = { pendingUndo = entry }) { Text("Undo") }
                            }
                        }
                    }
                }
            }
        }
    }

    pendingUndo?.let { entry ->
        ConfirmDialog(
            title = "Undo pemindahan?",
            message = "\"${entry.fileName}\" akan dikembalikan ke folder Downloads asal.",
            confirmLabel = "Undo",
            onConfirm = {
                onUndo(entry)
                pendingUndo = null
            },
            onDismiss = { pendingUndo = null }
        )
    }
}
