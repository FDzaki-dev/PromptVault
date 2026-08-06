package com.elprompter.promptvault.ui.screens

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import com.elprompter.promptvault.ui.components.VaultCard
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.elprompter.promptvault.data.ActivityLogEntry
import com.elprompter.promptvault.data.LogLevel
import com.elprompter.promptvault.data.MoveHistoryEntry
import com.elprompter.promptvault.ui.components.EmptyState
import com.elprompter.promptvault.ui.components.VaultActionSheet
import com.elprompter.promptvault.ui.components.SortedStamp
import com.elprompter.promptvault.ui.components.VaultTopBar
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ActivityLogScreen(
    logEntries: List<ActivityLogEntry>,
    undoableHistory: List<MoveHistoryEntry>,
    onUndo: (MoveHistoryEntry) -> Unit,
    onBack: () -> Unit
) {
    var tab by remember { mutableStateOf(0) }
    var pendingUndo by remember { mutableStateOf<MoveHistoryEntry?>(null) }
    val formatter = remember { SimpleDateFormat("dd MMM HH:mm", Locale("id", "ID")) }
    val logExportFormatter = remember { SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale("id", "ID")) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val colors = MaterialTheme.colorScheme
    val clipboardManager: ClipboardManager = LocalClipboardManager.current

    Scaffold(
        topBar = {
            VaultTopBar(
                title = "Riwayat Aktivitas",
                onBack = onBack,
                actions = {
                    // Batch fix (2026-08-06): user butuh cara cepat ekspor log utk
                    // analisis SAF/bug lain tanpa ADB/Logcat -- copy SEMUA entri log
                    // (bukan cuma yg kelihatan di layar) sbg teks plain ke clipboard,
                    // format [timestamp] LEVEL: pesan, urutan terbaru dulu (sama spt
                    // tampilan). Hanya tampil di tab "Log" (tab==0), tidak relevan
                    // utk tab Undo. Kosong -> tombol tetap ada tapi salin string
                    // placeholder, bukan disable, biar konsisten & tidak butuh state
                    // tambahan.
                    if (tab == 0) {
                        IconButton(onClick = {
                            val text = if (logEntries.isEmpty()) {
                                "(Belum ada aktivitas log)"
                            } else {
                                logEntries.joinToString("\n") { entry ->
                                    "[${logExportFormatter.format(Date(entry.timestampMillis))}] ${entry.level}: ${entry.message}"
                                }
                            }
                            clipboardManager.setText(AnnotatedString(text))
                            scope.launch { snackbarHostState.showSnackbar("Log disalin ke clipboard") }
                        }) {
                            Icon(Icons.Filled.ContentCopy, contentDescription = "Salin Log", tint = colors.primary)
                        }
                    }
                }
            )
        },
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                Snackbar(snackbarData = data, containerColor = colors.primary, contentColor = colors.onPrimary)
            }
        }
    ) { padding ->
    Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
        com.elprompter.promptvault.ui.components.SegmentedControl(
            options = listOf("Log", "Undo Pemindahan"),
            selectedIndex = tab,
            onSelect = { tab = it }
        )

        if (tab == 0) {
            Crossfade(targetState = logEntries.isEmpty(), label = "activityLogEmptyState", animationSpec = tween(220)) { isEmpty ->
                if (isEmpty) {
                    EmptyState(
                        icon = Icons.Filled.History,
                        title = "Belum ada aktivitas",
                        message = "Riwayat pemindahan file akan muncul di sini setelah scan pertama berjalan.",
                        accentColor = colors.tertiary,
                        accentContainerColor = colors.tertiaryContainer
                    )
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(top = 8.dp)) {
                        items(logEntries, key = { it.id }) { entry ->
                            val entryColor = when (entry.level) {
                                LogLevel.SUCCESS -> colors.primary
                                LogLevel.WARNING -> colors.tertiary
                                LogLevel.ERROR -> colors.error
                                LogLevel.INFO -> colors.onSurfaceVariant
                            }
                            VaultCard(modifier = Modifier.fillMaxWidth().animateItemPlacement()) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(formatter.format(Date(entry.timestampMillis)), style = MaterialTheme.typography.labelSmall)
                                        Text(entry.message, color = entryColor, style = MaterialTheme.typography.bodyMedium)
                                    }
                                    if (entry.level == LogLevel.SUCCESS && entry.message.contains("->")) {
                                        SortedStamp()
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // TODO #1: fitur UNDO — file yang salah pindah kini bisa dikembalikan dari dalam app.
            val undoable = undoableHistory.filter { !it.undone }
            Crossfade(targetState = undoable.isEmpty(), label = "undoHistoryEmptyState", animationSpec = tween(220)) { isEmpty ->
                if (isEmpty) {
                    EmptyState(
                        icon = Icons.Filled.Undo,
                        title = "Tidak ada yang bisa di-undo",
                        message = "Pemindahan file yang bisa dibatalkan akan muncul di sini setelah scan memindahkan sesuatu.",
                        accentColor = colors.tertiary,
                        accentContainerColor = colors.tertiaryContainer
                    )
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(top = 8.dp)) {
                        items(undoable, key = { it.id }) { entry ->
                            VaultCard(modifier = Modifier.fillMaxWidth().animateItemPlacement()) {
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
    }
    }

    pendingUndo?.let { entry ->
        VaultActionSheet(
            title = "Undo pemindahan?",
            message = "\"${entry.fileName}\" akan dikembalikan ke folder Downloads asal.",
            confirmLabel = "Undo",
            onConfirm = {
                onUndo(entry)
                pendingUndo = null
                scope.launch { snackbarHostState.showSnackbar("\"${entry.fileName}\" dikembalikan ke Downloads") }
            },
            onDismiss = { pendingUndo = null }
        )
    }
}
