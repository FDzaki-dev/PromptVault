package com.elprompter.promptvault.ui.screens

import android.content.Context
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
import androidx.compose.ui.unit.dp
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.elprompter.promptvault.util.CrashLogger
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
    var statusText by remember { mutableStateOf("Memuat status WorkManager…") }
    var crashLogs by remember { mutableStateOf<List<CrashLogger.CrashLogEntry>>(emptyList()) }
    var selectedLog by remember { mutableStateOf<CrashLogger.CrashLogEntry?>(null) }
    var openedLogContent by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        statusText = readWorkStatus(context)
        crashLogs = withContext(Dispatchers.IO) { CrashLogger.listLogs(context) }
    }

    LaunchedEffect(selectedLog) {
        val entry = selectedLog
        if (entry != null) {
            openedLogContent = "Memuat…"
            openedLogContent = withContext(Dispatchers.IO) { CrashLogger.readLog(context, entry.uri) }
        }
    }

    if (selectedLog != null) {
        AlertDialog(
            onDismissRequest = { selectedLog = null; openedLogContent = null },
            confirmButton = { TextButton(onClick = { selectedLog = null; openedLogContent = null }) { Text("Tutup") } },
            title = { Text(selectedLog?.displayName ?: "", style = MaterialTheme.typography.titleSmall) },
            text = {
                Column(modifier = Modifier.heightIn(max = 400.dp).verticalScroll(rememberScrollState())) {
                    Text(openedLogContent ?: "", style = MaterialTheme.typography.bodySmall)
                }
            }
        )
    }

    androidx.compose.material3.Scaffold(
        topBar = { com.elprompter.promptvault.ui.components.VaultTopBar(title = "Diagnostik", onBack = onBack) }
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
            "Gunakan halaman ini untuk memverifikasi sendiri di HP bahwa auto-sort " +
                "benar-benar terjadwal, termasuk setelah restart perangkat.",
            style = MaterialTheme.typography.bodyMedium
        )

        VaultCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Nama File Asli di Downloads", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Bandingkan langsung dengan pattern rule kamu. Kalau tidak persis sama " +
                        "(termasuk spasi/underscore/ekstensi), rule tidak akan cocok.",
                    style = MaterialTheme.typography.bodySmall
                )
                if (downloadsFileNames.isEmpty()) {
                    Text("Tidak ada file di Downloads saat ini.", style = MaterialTheme.typography.bodySmall)
                } else {
                    downloadsFileNames.take(20).forEach { name ->
                        Text("• $name", style = MaterialTheme.typography.bodySmall)
                    }
                    if (downloadsFileNames.size > 20) {
                        Text("+ ${downloadsFileNames.size - 20} file lainnya", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }

        VaultCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Status Auto-Sort Worker", style = MaterialTheme.typography.titleMedium)
                Text(statusText, style = MaterialTheme.typography.bodyMedium)
            }
        }

        VaultCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Crash Log (${crashLogs.size})", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Tersimpan di Documents/PromptVault/logs/. Ketuk salah satu untuk baca isi lengkap " +
                        "sebelum minta Logcat/ADB.",
                    style = MaterialTheme.typography.bodySmall
                )
                if (crashLogs.isEmpty()) {
                    Text("Belum ada crash tercatat. Bagus.", style = MaterialTheme.typography.bodySmall)
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
                                "${entry.displayName} — ${fmt.format(Date(entry.dateAddedEpochSeconds * 1000))} (${entry.sizeBytes} B)",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.weight(1f)
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
                        Text("+ ${crashLogs.size - 10} log lainnya (lihat file manager)", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }

        VaultCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Cara verifikasi manual", style = MaterialTheme.typography.titleMedium)
                Text("1. Buat rule, taruh file contoh (ekstensi apa saja) di Downloads.")
                Text("2. Tekan \"Scan Sekarang\" di Home, cek file benar-benar pindah.")
                Text("3. Restart HP, jangan buka app secara manual.")
                Text("4. Tunggu sesuai interval, lalu cek lagi apakah file baru ikut terpindah.")
                Text("5. Jika status di atas tetap \"ENQUEUED\"/\"RUNNING\" setelah restart, auto-sort survive reboot.")
            }
        }

    }
    }
}

private fun readWorkStatus(context: Context): String {
    return try {
        val infos = WorkManager.getInstance(context)
            .getWorkInfosForUniqueWork(AutoSortWorker.WORK_NAME)
            .get()
        if (infos.isNullOrEmpty()) {
            "Belum ada jadwal ditemukan. Buka Home sekali agar worker terdaftar."
        } else {
            val info: WorkInfo = infos.first()
            val fmt = SimpleDateFormat("dd MMM yyyy HH:mm", Locale("id", "ID"))
            "State: ${info.state}\nRun attempt: ${info.runAttemptCount}\nDicek pada: ${fmt.format(Date())}"
        }
    } catch (e: Exception) {
        "Gagal membaca status: ${e.message}"
    }
}
