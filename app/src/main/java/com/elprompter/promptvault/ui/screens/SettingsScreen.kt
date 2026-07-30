package com.elprompter.promptvault.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import com.elprompter.promptvault.ui.components.VaultCard
import com.elprompter.promptvault.ui.components.VaultTopBar
import com.elprompter.promptvault.ui.theme.Pine
import com.elprompter.promptvault.ui.theme.CardPaper
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    currentIntervalMinutes: Int,
    onIntervalSelected: (Int) -> Unit,
    onExportRequested: suspend () -> String,
    onImportRequested: (String, (Int) -> Unit) -> Unit,
    onBack: () -> Unit
) {
    var exportedText by remember { mutableStateOf<String?>(null) }
    var importText by remember { mutableStateOf("") }
    var importResult by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = { VaultTopBar(title = "Pengaturan", onBack = onBack) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Interval Auto-Scan", style = MaterialTheme.typography.titleMedium)
            Text(
                "Seberapa sering PromptVault memindai Downloads di latar belakang. " +
                    "Android tidak mengizinkan kurang dari 15 menit.",
                style = MaterialTheme.typography.bodySmall
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SettingsRepository.ALLOWED_INTERVALS.forEach { minutes ->
                    FilterChip(
                        selected = minutes == currentIntervalMinutes,
                        onClick = { onIntervalSelected(minutes) },
                        label = { Text(if (minutes < 60) "$minutes menit" else "${minutes / 60} jam") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Pine,
                            selectedLabelColor = CardPaper
                        )
                    )
                }
            }

            VaultCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Backup / Export Rule", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Simpan semua rule kamu sebagai teks, biar bisa dipulihkan lagi kalau app di-uninstall.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    OutlinedButton(onClick = { scope.launch { exportedText = onExportRequested() } }) {
                        Text("Tampilkan JSON Export")
                    }

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
                    Text(
                        "Tempel teks hasil export dari perangkat lain atau backup sebelumnya.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    OutlinedTextField(
                        value = importText,
                        onValueChange = { importText = it },
                        label = { Text("Tempel JSON hasil export di sini") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(
                        onClick = {
                            onImportRequested(importText) { count ->
                                importResult = "$count rule berhasil diimpor."
                                importText = ""
                            }
                        },
                        enabled = importText.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = Pine, contentColor = CardPaper)
                    ) { Text("Import") }
                    importResult?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                }
            }
        }
    }
}
