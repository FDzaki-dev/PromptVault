package com.elprompter.promptvault.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import com.elprompter.promptvault.ui.components.VaultCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.elprompter.promptvault.data.SettingsRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    currentIntervalMinutes: Int,
    onIntervalSelected: (Int) -> Unit,
    onExportRequested: suspend () -> String,
    onImportRequested: (String, (Int) -> Unit) -> Unit
) {
    var exportedText by remember { mutableStateOf<String?>(null) }
    var importText by remember { mutableStateOf("") }
    var importResult by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Pengaturan", style = MaterialTheme.typography.headlineSmall)

        Text("Interval Auto-Scan", style = MaterialTheme.typography.titleMedium)
        Text(
            "TODO #2 selesai: interval kini bisa diatur, tidak lagi hardcoded 15 menit. " +
                "WorkManager tidak mendukung interval di bawah 15 menit.",
            style = MaterialTheme.typography.bodySmall
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SettingsRepository.ALLOWED_INTERVALS.forEach { minutes ->
                FilterChip(
                    selected = minutes == currentIntervalMinutes,
                    onClick = { onIntervalSelected(minutes) },
                    label = { Text(if (minutes < 60) "$minutes menit" else "${minutes / 60} jam") }
                )
            }
        }

        VaultCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Backup / Export Rule", style = MaterialTheme.typography.titleMedium)
                Text(
                    "TODO #7 selesai: rule bisa diekspor sebagai teks JSON untuk disimpan di luar app, " +
                        "lalu diimpor kembali kalau app di-uninstall.",
                    style = MaterialTheme.typography.bodySmall
                )
                Button(onClick = {
                    scope.launch {
                        exportedText = onExportRequested()
                    }
                }) { Text("Tampilkan JSON Export") }

                exportedText?.let {
                    OutlinedTextField(
                        value = it,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Salin teks ini untuk backup") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        VaultCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Import Rule", style = MaterialTheme.typography.titleMedium)
                OutlinedTextField(
                    value = importText,
                    onValueChange = { importText = it },
                    label = { Text("Tempel JSON hasil export di sini") },
                    modifier = Modifier.fillMaxWidth()
                )
                Button(onClick = {
                    onImportRequested(importText) { count ->
                        importResult = "$count rule berhasil diimpor."
                    }
                }, enabled = importText.isNotBlank()) { Text("Import") }
                importResult?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
            }
        }
    }
}
