package com.elprompter.promptvault.ui.screens

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.elprompter.promptvault.R
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

/**
 * [Fitur baru 2026-08-17 -- "sweep-select to undo", permintaan eksplisit
 * user: "biar gak ribet buat user"] Tab "Undo Pemindahan" sekarang punya
 * mode seleksi-banyak yang bisa dipilih dengan DUA cara:
 *  1. Tekan-lama (long-press) 1 baris -> masuk mode seleksi, baris itu
 *     langsung terpilih.
 *  2. SELAGI jari masih menempel (atau di sentuhan baru manapun setelah
 *     mode seleksi aktif), SAPUKAN jari ke atas/bawah melewati baris lain
 *     -- setiap baris yang "disapu" ikut ter-toggle otomatis, TANPA perlu
 *     tap satu-satu. Arah toggle (pilih vs batal-pilih) mengikuti status
 *     baris PERTAMA yang disentuh saat sapuan itu dimulai -- sama seperti
 *     pola "sweep select" yang familiar di Gmail/Files/Galeri.
 * Setelah beberapa baris terpilih, tombol "Undo Terpilih (N)" di top bar
 * menjalankan [onUndoMultiple] sekali untuk semuanya -- user tidak perlu
 * menekan tombol Undo per baris berulang-ulang.
 *
 * Implementasi: posisi layar (window coordinates) tiap baris direkam lewat
 * [onGloballyPositioned] ke [itemBounds]; gestur sapuan ([detectDragGestures])
 * dipasang di [Box] pembungkus LazyColumn, HANYA aktif selagi mode seleksi
 * menyala (`selectionMode`) -- di luar itu, sentuhan diteruskan apa adanya
 * ke LazyColumn (scroll & tap normal, TIDAK terganggu).
 *
 * **Batas jujur**: gestur sapuan lintas-elemen seperti ini belum pernah
 * lolos `./gradlew`/device asli di project ini (konsisten dengan seluruh
 * kode UI project yang ditulis tanpa akses compiler -- lihat PROJECT_STATE.md).
 * Kalau di device asli sapuan terasa "kalah" oleh scroll LazyColumn, itu
 * kandidat pertama untuk disetel ulang (mis. batas jarak minimum sebelum
 * dianggap sapuan, bukan tap).
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ActivityLogScreen(
    logEntries: List<ActivityLogEntry>,
    undoableHistory: List<MoveHistoryEntry>,
    onUndo: suspend (MoveHistoryEntry) -> Boolean,
    onUndoMultiple: suspend (List<MoveHistoryEntry>) -> Pair<Int, Int>,
    onBack: () -> Unit
) {
    var tab by remember { mutableStateOf(0) }
    // [Roadmap Fase 2.1, 2026-08-21] Search lintas 2 tab (Log & Undo) --
    // 1 field dipakai bersama (bukan per-tab terpisah, biar tidak nambah
    // state), filter kolom BEDA per tab (message vs fileName) di titik
    // pemakaiannya masing-masing di bawah.
    var query by remember { mutableStateOf("") }
    var pendingUndo by remember { mutableStateOf<MoveHistoryEntry?>(null) }
    var pendingBatchUndo by remember { mutableStateOf<List<MoveHistoryEntry>?>(null) }
    var undoInFlight by remember { mutableStateOf(false) }
    val formatter = remember { SimpleDateFormat("dd MMM HH:mm", Locale("id", "ID")) }
    val logExportFormatter = remember { SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale("id", "ID")) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val colors = MaterialTheme.colorScheme
    val clipboardManager: ClipboardManager = LocalClipboardManager.current
    // [Roadmap Fase 1.3 batch 7/N] context.getString() dipakai di dalam
    // lambda non-composable (onClick/onConfirm/scope.launch) -- stringResource()
    // TIDAK valid di situ, sama pola dgn SettingsScreen v8.6.0.
    val context = LocalContext.current

    // --- State mode seleksi-sapuan (tab Undo saja) ---
    var selectionMode by remember { mutableStateOf(false) }
    var selectedIds by remember { mutableStateOf(setOf<String>()) }
    var sweepAdding by remember { mutableStateOf(true) }
    val itemBounds = remember { mutableStateMapOf<String, Rect>() }
    var containerCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }

    fun exitSelectionMode() {
        selectionMode = false
        selectedIds = emptySet()
    }

    Scaffold(
        topBar = {
            VaultTopBar(
                title = if (selectionMode) stringResource(R.string.activitylog_selected_count_fmt, selectedIds.size) else stringResource(R.string.activitylog_title),
                onBack = { if (selectionMode) exitSelectionMode() else onBack() },
                actions = {
                    if (selectionMode) {
                        // [Fitur baru, sweep-select] "Batal" -- keluar mode
                        // seleksi tanpa melakukan apa pun, supaya user selalu
                        // punya jalan mundur yang jelas (bukan cuma back
                        // sistem, yang biasanya berarti "keluar layar").
                        IconButton(onClick = { exitSelectionMode() }) {
                            Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.activitylog_cancel_select_desc), tint = colors.onSurfaceVariant)
                        }
                        TextButton(
                            onClick = {
                                val toUndo = undoableHistory.filter { it.id in selectedIds && !it.undone }
                                if (toUndo.isNotEmpty()) pendingBatchUndo = toUndo
                            },
                            enabled = selectedIds.isNotEmpty() && !undoInFlight
                        ) { Text(stringResource(R.string.activitylog_undo_selected_fmt, selectedIds.size)) }
                    } else if (tab == 0) {
                        // Batch fix (2026-08-06): user butuh cara cepat ekspor log utk
                        // analisis bug tanpa ADB/Logcat -- copy SEMUA entri log
                        // (bukan cuma yg kelihatan di layar) sbg teks plain ke clipboard,
                        // format [timestamp] LEVEL: pesan, urutan terbaru dulu (sama spt
                        // tampilan). Hanya tampil di tab "Log" (tab==0), tidak relevan
                        // utk tab Undo. Kosong -> tombol tetap ada tapi salin string
                        // placeholder, bukan disable, biar konsisten & tidak butuh state
                        // tambahan.
                        IconButton(onClick = {
                            val text = if (logEntries.isEmpty()) {
                                context.getString(R.string.activitylog_no_log_placeholder)
                            } else {
                                logEntries.joinToString("\n") { entry ->
                                    context.getString(
                                        R.string.activitylog_log_export_fmt,
                                        logExportFormatter.format(Date(entry.timestampMillis)),
                                        entry.level,
                                        entry.message
                                    )
                                }
                            }
                            clipboardManager.setText(AnnotatedString(text))
                            scope.launch { snackbarHostState.showSnackbar(context.getString(R.string.activitylog_log_copied)) }
                        }) {
                            Icon(Icons.Filled.ContentCopy, contentDescription = stringResource(R.string.activitylog_copy_log_desc), tint = colors.primary)
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
            options = listOf(stringResource(R.string.activitylog_tab_log), stringResource(R.string.activitylog_tab_undo)),
            selectedIndex = tab,
            onSelect = {
                tab = it
                exitSelectionMode() // ganti tab -> keluar mode seleksi, hindari state nyangkut
            }
        )

        // Disembunyikan saat mode seleksi aktif -- top bar sudah ganti fokus ke
        // aksi "Undo Terpilih", search bar di situ cuma nambah ramai tanpa guna
        // (mode seleksi cuma ada di tab Undo, sweep-select butuh fokus penuh).
        if (!selectionMode) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text(stringResource(R.string.activitylog_search_label)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            )
        }

        if (tab == 0) {
            val filteredLogEntries = remember(logEntries, query) {
                if (query.isBlank()) logEntries
                else logEntries.filter { it.message.contains(query, ignoreCase = true) }
            }
            Crossfade(targetState = filteredLogEntries.isEmpty(), label = "activityLogEmptyState", animationSpec = tween(220)) { isEmpty ->
                if (isEmpty) {
                    if (logEntries.isEmpty()) {
                        EmptyState(
                            icon = Icons.Filled.History,
                            title = stringResource(R.string.activitylog_empty_log_title),
                            message = stringResource(R.string.activitylog_empty_log_message),
                            accentColor = colors.tertiary,
                            accentContainerColor = colors.tertiaryContainer
                        )
                    } else {
                        EmptyState(
                            icon = Icons.Filled.SearchOff,
                            title = stringResource(R.string.activitylog_no_results_title),
                            message = stringResource(R.string.activitylog_no_results_message_fmt, query),
                            accentColor = colors.tertiary,
                            accentContainerColor = colors.tertiaryContainer
                        )
                    }
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(top = 8.dp)) {
                        items(filteredLogEntries, key = { it.id }) { entry ->
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
            // Fitur UNDO: file yang salah pindah bisa dikembalikan dari dalam app,
            // tanpa perlu file manager manual. Selesai & jalan penuh (bukan lagi TODO) --
            // lihat blok `pendingUndo` di bawah untuk alur konfirmasi & hasil asli.
            val undoable = undoableHistory.filter { !it.undone }
            val filteredUndoable = remember(undoable, query) {
                if (query.isBlank()) undoable
                else undoable.filter { it.fileName.contains(query, ignoreCase = true) }
            }
            Crossfade(targetState = filteredUndoable.isEmpty(), label = "undoHistoryEmptyState", animationSpec = tween(220)) { isEmpty ->
                if (isEmpty) {
                    if (undoable.isEmpty()) {
                        EmptyState(
                            icon = Icons.Filled.Undo,
                            title = stringResource(R.string.activitylog_empty_undo_title),
                            message = stringResource(R.string.activitylog_empty_undo_message),
                            accentColor = colors.tertiary,
                            accentContainerColor = colors.tertiaryContainer
                        )
                    } else {
                        EmptyState(
                            icon = Icons.Filled.SearchOff,
                            title = stringResource(R.string.activitylog_no_results_title),
                            message = stringResource(R.string.activitylog_no_results_message_fmt, query),
                            accentColor = colors.tertiary,
                            accentContainerColor = colors.tertiaryContainer
                        )
                    }
                } else {
                    Column {
                        if (!selectionMode) {
                            // [Fitur baru, sweep-select] Hint singkat -- SATU
                            // kalimat, cukup sekali di atas list, supaya user
                            // tahu fiturnya ADA tanpa perlu menemukannya sendiri.
                            Text(
                                stringResource(R.string.activitylog_sweep_hint),
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.onSurfaceVariant,
                                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                            )
                        }
                        androidx.compose.foundation.layout.Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .onGloballyPositioned { containerCoords = it }
                                .pointerInput(selectionMode) {
                                    if (!selectionMode) return@pointerInput
                                    detectDragGestures(
                                        onDragStart = { offset ->
                                            val windowPos = containerCoords?.localToWindow(offset) ?: offset
                                            val id = itemBounds.entries.firstOrNull { it.value.contains(windowPos) }?.key
                                            if (id != null) {
                                                sweepAdding = id !in selectedIds
                                                selectedIds = if (sweepAdding) selectedIds + id else selectedIds - id
                                                if (selectedIds.isEmpty()) selectionMode = false
                                            }
                                        },
                                        onDrag = { change, _ ->
                                            val windowPos = containerCoords?.localToWindow(change.position) ?: change.position
                                            val id = itemBounds.entries.firstOrNull { it.value.contains(windowPos) }?.key
                                            if (id != null) {
                                                selectedIds = if (sweepAdding) selectedIds + id else selectedIds - id
                                            }
                                        }
                                    )
                                }
                        ) {
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(top = 4.dp)) {
                                items(filteredUndoable, key = { it.id }) { entry ->
                                    val isSelected = entry.id in selectedIds
                                    VaultCard(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .animateItemPlacement()
                                            .onGloballyPositioned { coords -> itemBounds[entry.id] = coords.boundsInWindow() }
                                            .combinedClickable(
                                                onClick = {
                                                    if (selectionMode) {
                                                        selectedIds = if (isSelected) selectedIds - entry.id else selectedIds + entry.id
                                                        if (selectedIds.isEmpty()) selectionMode = false
                                                    } else {
                                                        pendingUndo = entry
                                                    }
                                                },
                                                onLongClick = {
                                                    if (!selectionMode) {
                                                        selectionMode = true
                                                        selectedIds = setOf(entry.id)
                                                    }
                                                }
                                            )
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(10.dp),
                                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                                        ) {
                                            if (selectionMode) {
                                                Checkbox(
                                                    checked = isSelected,
                                                    onCheckedChange = {
                                                        selectedIds = if (isSelected) selectedIds - entry.id else selectedIds + entry.id
                                                        if (selectedIds.isEmpty()) selectionMode = false
                                                    },
                                                    colors = CheckboxDefaults.colors(checkedColor = colors.primary)
                                                )
                                            }
                                            Column(modifier = Modifier.weight(1f)) {
                                                // [Fix Audit UX, 2026-08-21] SEBELUMNYA tanpa maxLines --
                                                // nama file APA ADANYA dari filesystem (sering tanpa
                                                // spasi, cuma "-"/"_"/".") bisa kena WORD-BREAK PAKSA
                                                // di tengah token saat tidak muat 1 baris (mis.
                                                // "AudioPlayer-v1.0.34-release-run146.apk" pecah jadi
                                                // "...release-r" / "un146.apk" -- kejadian NYATA, lihat
                                                // screenshot referensi) -- tinggi tiap row jadi TIDAK
                                                // konsisten antar entri (1 baris vs 2 baris tergantung
                                                // panjang nama file), ritme list jadi berantakan.
                                                // Diklem 1 baris + ellipsis (konvensi standar list file
                                                // Android, mis. app Files/Photos) -- nama lengkap tetap
                                                // ada di `entry.fileName` kalau nanti dibutuhkan detail
                                                // (mis. via long-press/dialog), TIDAK dihapus datanya.
                                                Text(
                                                    entry.fileName,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    // [Fix 2026-08-17] Label ini SEBELUMNYA hardcode
                                                    // "PromptVault/<rule>/" utk SEMUA entri -- sejak app
                                                    // berhenti bikin subfolder "PromptVault" sendiri di
                                                    // folder tujuan kustom SAF (lihat KDoc
                                                    // FileSorter.resolveSafRuleDestinations), itu SALAH
                                                    // utk entri SAF (destUri berupa content:// Uri).
                                                    // [Fitur baru 2026-08-17, integrasi Shizuku] Entri
                                                    // Shizuku (prefix palsu "shizuku://") ikut dibedakan
                                                    // di sini juga. Entri lokal (destUri = path absolut
                                                    // Downloads/PromptVault/...) TETAP benar pakai
                                                    // prefix lama.
                                                    text = when {
                                                        entry.destUri.startsWith("shizuku://") -> stringResource(R.string.activitylog_dest_shizuku_fmt, entry.ruleFolderName)
                                                        entry.destUri.startsWith("content://") -> stringResource(R.string.activitylog_dest_saf_fmt, entry.ruleFolderName)
                                                        else -> stringResource(R.string.activitylog_dest_local_fmt, entry.ruleFolderName)
                                                    },
                                                    style = MaterialTheme.typography.labelSmall
                                                )
                                                Text(formatter.format(Date(entry.timestampMillis)), style = MaterialTheme.typography.labelSmall)
                                            }
                                            if (!selectionMode) {
                                                TextButton(
                                                    onClick = { pendingUndo = entry },
                                                    enabled = !undoInFlight
                                                ) { Text(stringResource(R.string.action_undo)) }
                                            }
                                        }
                                    }
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
            title = stringResource(R.string.activitylog_undo_confirm_title),
            message = stringResource(R.string.activitylog_undo_confirm_message_fmt, entry.fileName),
            confirmLabel = stringResource(R.string.action_undo),
            onConfirm = {
                pendingUndo = null
                undoInFlight = true
                scope.launch {
                    // BUG lama diperbaiki (2026-08-07): sebelumnya snackbar "berhasil"
                    // SELALU muncul di sini terlepas hasil asli `onUndo` (fire-and-forget).
                    // Sekarang menunggu hasil ASLI & pesan sesuai kenyataan -- detail
                    // alasan gagal tetap ada di tab Log kalau user butuh tahu lebih
                    // spesifik.
                    val success = onUndo(entry)
                    undoInFlight = false
                    snackbarHostState.showSnackbar(
                        if (success) context.getString(R.string.activitylog_undo_success_fmt, entry.fileName)
                        else context.getString(R.string.activitylog_undo_failed_fmt, entry.fileName)
                    )
                }
            },
            onDismiss = { pendingUndo = null }
        )
    }

    // [Fitur baru 2026-08-17 -- sweep-select to undo] Konfirmasi BATCH,
    // pola sama persis dengan pendingUndo tunggal di atas (VaultActionSheet
    // yang sama, cuma pesan & aksi beda) -- supaya user tetap dapat 1
    // langkah konfirmasi terakhir sebelum banyak file sekaligus dipindahkan
    // balik, bukan langsung eksekusi begitu tombol top bar ditekan.
    pendingBatchUndo?.let { entries ->
        VaultActionSheet(
            title = stringResource(R.string.activitylog_batch_undo_title_fmt, entries.size),
            message = stringResource(R.string.activitylog_batch_undo_message_fmt, entries.size),
            confirmLabel = stringResource(R.string.activitylog_batch_undo_confirm),
            onConfirm = {
                pendingBatchUndo = null
                undoInFlight = true
                scope.launch {
                    val (success, failed) = onUndoMultiple(entries)
                    undoInFlight = false
                    exitSelectionMode()
                    snackbarHostState.showSnackbar(
                        if (failed == 0) context.getString(R.string.activitylog_batch_undo_success_fmt, success)
                        else context.getString(R.string.activitylog_batch_undo_partial_fmt, success, failed)
                    )
                }
            },
            onDismiss = { pendingBatchUndo = null }
        )
    }
}
