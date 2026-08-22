package com.elprompter.promptvault.ui.screens

import android.content.Context
import com.elprompter.promptvault.R
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.elprompter.promptvault.ui.components.VaultCard
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.elprompter.promptvault.util.CrashLogger
import com.elprompter.promptvault.data.SettingsRepository
import com.elprompter.promptvault.worker.AutoSortWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * TODO #4 & #5: PromptVault belum pernah diuji nyata di HP, dan status auto-sort
 * setelah reboot belum bisa diverifikasi selain lewat kode. Layar ini tidak
 * menggantikan pengujian nyata, tapi memberi bukti langsung dari perangkat
 * (status WorkManager & jadwal berikutnya) tanpa perlu adb/dev tools.
 */
@Composable
fun DiagnosticsScreen(
    downloadsFileNames: List<String>,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var statusText by remember { mutableStateOf("") }
    var crashLogs by remember { mutableStateOf<List<CrashLogger.CrashLogEntry>>(emptyList()) }
    var selectedLog by remember { mutableStateOf<CrashLogger.CrashLogEntry?>(null) }
    var openedLogContent by remember { mutableStateOf<String?>(null) }

    val loadingStatusText = stringResource(id = R.string.diag_loading_status)
    val loadingLogText = stringResource(id = R.string.diag_loading_log)
    val statusNoneText = stringResource(id = R.string.diag_status_none)
    val statusErrorFmt = stringResource(id = R.string.diag_status_error_fmt)
    val toggleFmt = stringResource(id = R.string.diag_status_toggle_fmt)
    val toggleOnText = stringResource(id = R.string.diag_toggle_on)
    val toggleOffText = stringResource(id = R.string.diag_toggle_off)
    val workInfoFmt = stringResource(id = R.string.diag_status_workinfo_fmt)
    val nextRunUnknownText = stringResource(id = R.string.diag_next_run_unknown)
    val checkedFmt = stringResource(id = R.string.diag_status_checked_fmt)

    LaunchedEffect(Unit) {
        statusText = loadingStatusText
        statusText = readWorkStatus(
            context = context,
            toggleFmt = toggleFmt,
            toggleOnText = toggleOnText,
            toggleOffText = toggleOffText,
            noneText = statusNoneText,
            workInfoFmt = workInfoFmt,
            nextRunUnknownText = nextRunUnknownText,
            checkedFmt = checkedFmt,
            errorFmt = statusErrorFmt
        )
        crashLogs = withContext(Dispatchers.IO) { CrashLogger.listLogs(context) }
    }

    LaunchedEffect(selectedLog) {
        val entry = selectedLog
        if (entry != null) {
            openedLogContent = loadingLogText
            openedLogContent = withContext(Dispatchers.IO) { CrashLogger.readLog(context, entry.uri) }
        }
    }

    if (selectedLog != null) {
        AlertDialog(
            onDismissRequest = { selectedLog = null; openedLogContent = null },
            confirmButton = { TextButton(onClick = { selectedLog = null; openedLogContent = null }) { Text(stringResource(id = R.string.diag_dialog_close)) } },
            title = { Text(selectedLog?.displayName ?: "", style = MaterialTheme.typography.titleSmall) },
            text = {
                Column(modifier = Modifier.heightIn(max = 400.dp).verticalScroll(rememberScrollState())) {
                    Text(openedLogContent ?: "", style = MaterialTheme.typography.bodySmall)
                }
            }
        )
    }

    androidx.compose.material3.Scaffold(
        topBar = { com.elprompter.promptvault.ui.components.VaultTopBar(title = stringResource(id = R.string.diag_title), onBack = onBack) }
    ) { padding ->
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            stringResource(id = R.string.diag_intro),
            style = MaterialTheme.typography.bodyMedium
        )

        VaultCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(stringResource(id = R.string.diag_downloads_title), style = MaterialTheme.typography.titleMedium)
                Text(
                    stringResource(id = R.string.diag_downloads_desc),
                    style = MaterialTheme.typography.bodySmall
                )
                if (downloadsFileNames.isEmpty()) {
                    Text(stringResource(id = R.string.diag_downloads_empty), style = MaterialTheme.typography.bodySmall)
                } else {
                    downloadsFileNames.take(20).forEach { name ->
                        // [Fix Audit UX, 2026-08-21] konsisten dgn fix ActivityLogScreen/
                        // SkippedFilesScreen -- nama file mentah tanpa maxLines.
                        Text(
                            stringResource(id = R.string.diag_downloads_item_fmt, name),
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (downloadsFileNames.size > 20) {
                        Text(stringResource(id = R.string.diag_downloads_more_fmt, downloadsFileNames.size - 20), style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }

        VaultCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(stringResource(id = R.string.diag_worker_status_title), style = MaterialTheme.typography.titleMedium)
                Text(statusText, style = MaterialTheme.typography.bodyMedium)
            }
        }

        VaultCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(id = R.string.diag_crashlog_title_fmt, crashLogs.size), style = MaterialTheme.typography.titleMedium)
                Text(
                    stringResource(id = R.string.diag_crashlog_desc),
                    style = MaterialTheme.typography.bodySmall
                )
                if (crashLogs.isEmpty()) {
                    Text(stringResource(id = R.string.diag_crashlog_empty), style = MaterialTheme.typography.bodySmall)
                } else {
                    val fmt = remember { SimpleDateFormat("dd MMM yyyy HH:mm", Locale("id", "ID")) }
                    // UI-08 & UI-15 fix: sebelumnya clickable dipasang langsung
                    // ke Text tanpa indication & tanpa touch target eksplisit --
                    // tinggi baris cuma ikut intrinsic text, terasa seperti teks
                    // biasa. Sekarang dibungkus Row min-height 48dp + padding +
                    // indication default (ripple) + chevron sebagai affordance
                    // jelas bahwa baris ini bisa diketuk.
                    crashLogs.take(10).forEach { entry ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .sizeIn(minHeight = 48.dp)
                                .clickable(indication = LocalIndication.current, interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }) { selectedLog = entry }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                stringResource(
                                    id = R.string.diag_crashlog_item_fmt,
                                    entry.displayName,
                                    fmt.format(Date(entry.dateAddedEpochSeconds * 1000)),
                                    entry.sizeBytes
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Icon(
                                Icons.Filled.ChevronRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.sizeIn(minWidth = 20.dp, minHeight = 20.dp)
                            )
                        }
                    }
                    if (crashLogs.size > 10) {
                        Text(stringResource(id = R.string.diag_crashlog_more_fmt, crashLogs.size - 10), style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }

        VaultCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(stringResource(id = R.string.diag_manual_verify_title), style = MaterialTheme.typography.titleMedium)
                Text(stringResource(id = R.string.diag_manual_step1))
                Text(stringResource(id = R.string.diag_manual_step2))
                Text(stringResource(id = R.string.diag_manual_step3))
                Text(stringResource(id = R.string.diag_manual_step4))
                Text(stringResource(id = R.string.diag_manual_step5))
            }
        }

    }
    }
}

/**
 * [Pending queue P3 #6, dituntaskan 2026-08-22] Sebelumnya cuma tampilkan
 * `WorkInfo.state` mentah -- tidak jelas apakah state itu MEMANG
 * merefleksikan toggle Auto-Sort user (SettingsRepository, sumber
 * kebenaran UI Pengaturan) atau WorkManager belum sempat sinkron, dan
 * tidak ada info kapan scan berikutnya benar-benar akan jalan. Sekarang
 * 3 hal ditampilkan terpisah & eksplisit: (1) toggle tersimpan di
 * DataStore, (2) state WorkManager APA ADANYA (bisa beda sesaat dari
 * toggle kalau baru saja diubah, WorkManager punya latency propagasi),
 * (3) `nextScheduleTimeMillis` (androidx.work 2.9.1+) -- `Long.MAX_VALUE`
 * berarti tidak ada jadwal berikut yang diketahui (worker CANCELLED atau
 * one-shot beres), diformat sbg teks "tidak diketahui" alih-alih angka
 * mentah yang membingungkan.
 */
private suspend fun readWorkStatus(
    context: Context,
    toggleFmt: String,
    toggleOnText: String,
    toggleOffText: String,
    noneText: String,
    workInfoFmt: String,
    nextRunUnknownText: String,
    checkedFmt: String,
    errorFmt: String
): String {
    return try {
        val autoSortEnabled = SettingsRepository(context).getAutoSortEnabled()
        val toggleLine = String.format(toggleFmt, if (autoSortEnabled) toggleOnText else toggleOffText)

        val infos = WorkManager.getInstance(context)
            .getWorkInfosForUniqueWork(AutoSortWorker.WORK_NAME)
            .get()
        val fmt = SimpleDateFormat("dd MMM yyyy HH:mm", Locale("id", "ID"))
        val workInfoLine = if (infos.isNullOrEmpty()) {
            noneText
        } else {
            val info: WorkInfo = infos.first()
            val nextRunText = if (info.nextScheduleTimeMillis != Long.MAX_VALUE) {
                fmt.format(Date(info.nextScheduleTimeMillis))
            } else {
                nextRunUnknownText
            }
            String.format(workInfoFmt, info.state, info.runAttemptCount, nextRunText)
        }
        val checkedLine = String.format(checkedFmt, fmt.format(Date()))

        "$toggleLine\n\n$workInfoLine\n\n$checkedLine"
    } catch (e: Exception) {
        String.format(errorFmt, e.message)
    }
}
