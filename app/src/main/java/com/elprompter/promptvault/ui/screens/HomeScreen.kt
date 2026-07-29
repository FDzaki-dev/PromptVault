package com.elprompter.promptvault.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun HomeScreen(
    ruleCount: Int,
    intervalMinutes: Int,
    isScanning: Boolean,
    lastScanSummary: String?,
    hasSkippedFiles: Boolean,
    onScanNow: () -> Unit,
    onOpenRules: () -> Unit,
    onOpenLog: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenDiagnostics: () -> Unit,
    onOpenSkippedFiles: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("PromptVault", style = MaterialTheme.typography.headlineMedium)
        Text("Rapikan otomatis file ZIP & TXT di Downloads kamu.", style = MaterialTheme.typography.bodyMedium)

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("$ruleCount rule aktif")
                Text("Auto-scan setiap $intervalMinutes menit")
                if (lastScanSummary != null) {
                    Text(lastScanSummary, style = MaterialTheme.typography.bodySmall)
                }
                if (hasSkippedFiles) {
                    TextButton(onClick = onOpenSkippedFiles, modifier = Modifier.fillMaxWidth()) {
                        Text("Lihat detail file yang dilewati →")
                    }
                }
            }
        }

        Button(onClick = onScanNow, enabled = !isScanning, modifier = Modifier.fillMaxWidth()) {
            if (isScanning) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp))
            } else {
                Text("Scan Sekarang")
            }
        }

        Button(onClick = onOpenRules, modifier = Modifier.fillMaxWidth()) { Text("Kelola Rule") }
        Button(onClick = onOpenLog, modifier = Modifier.fillMaxWidth()) { Text("Riwayat Aktivitas & Undo") }
        Button(onClick = onOpenSettings, modifier = Modifier.fillMaxWidth()) { Text("Pengaturan") }
        Button(onClick = onOpenDiagnostics, modifier = Modifier.fillMaxWidth()) { Text("Diagnostik") }
    }
}
