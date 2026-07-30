package com.elprompter.promptvault.ui.screens

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.elprompter.promptvault.ui.components.VaultCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.elprompter.promptvault.worker.AutoSortWorker
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
fun DiagnosticsScreen(downloadsFileNames: List<String>, onBack: () -> Unit) {
    val context = LocalContext.current
    var statusText by remember { mutableStateOf("Memuat status WorkManager…") }

    LaunchedEffect(Unit) {
        statusText = readWorkStatus(context)
    }

    androidx.compose.material3.Scaffold(
        topBar = { com.elprompter.promptvault.ui.components.VaultTopBar(title = "Diagnostik", onBack = onBack) }
    ) { padding ->
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
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
                    Text("Tidak ada file ZIP/TXT di Downloads saat ini.", style = MaterialTheme.typography.bodySmall)
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
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Cara verifikasi manual", style = MaterialTheme.typography.titleMedium)
                Text("1. Buat rule, taruh file ZIP/TXT contoh di Downloads.")
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
