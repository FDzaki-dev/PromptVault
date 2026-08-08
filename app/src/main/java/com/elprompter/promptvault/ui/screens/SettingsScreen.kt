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
import androidx.compose.material3.ExperimentalMaterial3Api
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
import com.elprompter.promptvault.data.ConflictStrategy
import com.elprompter.promptvault.data.SettingsRepository
import com.elprompter.promptvault.ui.components.VaultCard
import com.elprompter.promptvault.ui.components.VaultTopBar
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    currentIntervalMinutes: Int,
    onIntervalSelected: (Int) -> Unit,
    currentConflictStrategy: ConflictStrategy,
    onConflictStrategySelected: (ConflictStrategy) -> Unit,
    onExportRequested: suspend () -> String,
    onImportRequested: (String, (Int) -> Unit) -> Unit,
    onBack: () -> Unit
) {
    var exportedText by remember { mutableStateOf<String?>(null) }
    var importText by remember { mutableStateOf("") }
    var importResult by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val colors = MaterialTheme.colorScheme

    @Composable
    fun chipColors(dangerAccent: Boolean = false) = FilterChipDefaults.filterChipColors(
        selectedContainerColor = if (dangerAccent) colors.error else colors.primary,
        selectedLabelColor = if (dangerAccent) colors.onError else colors.onPrimary
    )

    Scaffold(
        topBar = { VaultTopBar(title = "Pengaturan", onBack = onBack) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
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
                        colors = chipColors()
                    )
                }
            }

            Text("Kalau Nama File Sudah Ada di Tujuan", style = MaterialTheme.typography.titleMedium)
            Text(
                "Apa yang dilakukan PromptVault kalau file dengan nama yang sama sudah ada di folder tujuan.",
                style = MaterialTheme.typography.bodySmall
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val conflictLabels = mapOf(
                    ConflictStrategy.RENAME to "Ganti nama otomatis",
                    ConflictStrategy.SKIP to "Lewati",
                    ConflictStrategy.OVERWRITE to "Timpa"
                )
                conflictLabels.forEach { (strategy, label) ->
                    FilterChip(
                        selected = strategy == currentConflictStrategy,
                        onClick = { onConflictStrategySelected(strategy) },
                        label = { Text(label) },
                        colors = chipColors(dangerAccent = strategy == ConflictStrategy.OVERWRITE)
                    )
                }
            }
            if (currentConflictStrategy == ConflictStrategy.OVERWRITE) {
                Text(
                    "Perhatian: file lama di tujuan akan tertimpa permanen dan tidak bisa di-undo.",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.error
                )
            }

            VaultCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Backup / Export Rule", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Simpan semua rule kamu sebagai teks, biar bisa dipulihkan lagi kalau app di-uninstall.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    OutlinedButton(
                        onClick = { scope.launch { exportedText = onExportRequested() } },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.primary)
                    ) {
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
                        colors = ButtonDefaults.buttonColors(containerColor = colors.primary, contentColor = colors.onPrimary)
                    ) { Text("Import") }
                    importResult?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                }
            }
        }
    }
}
