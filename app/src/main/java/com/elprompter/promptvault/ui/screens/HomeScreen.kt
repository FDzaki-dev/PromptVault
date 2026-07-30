package com.elprompter.promptvault.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.elprompter.promptvault.ui.components.VaultCard
import com.elprompter.promptvault.ui.theme.InkFaint
import com.elprompter.promptvault.ui.theme.Pine
import com.elprompter.promptvault.ui.theme.Stamp

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
    androidx.compose.material3.Scaffold { padding ->
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("// ARCHIVE MANIFEST", style = MaterialTheme.typography.labelLarge, color = InkFaint)
        Text("PromptVault", style = MaterialTheme.typography.headlineMedium)
        Text("Rapikan otomatis file ZIP & TXT di Downloads kamu.", style = MaterialTheme.typography.bodyMedium)

        VaultCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                ManifestRow(label = "RULE AKTIF", value = "$ruleCount")
                ManifestRow(label = "AUTO-SCAN", value = "tiap $intervalMinutes menit")
                if (lastScanSummary != null) {
                    Text(lastScanSummary, style = MaterialTheme.typography.bodySmall)
                }
                if (hasSkippedFiles) {
                    TextButton(onClick = onOpenSkippedFiles, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Filled.ErrorOutline, contentDescription = null, tint = Stamp, modifier = Modifier.size(16.dp))
                        Text(" Lihat detail file yang dilewati", color = Stamp, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }

        Button(
            onClick = onScanNow,
            enabled = !isScanning,
            colors = ButtonDefaults.buttonColors(containerColor = Stamp),
            modifier = Modifier.fillMaxWidth()
        ) {
            if (isScanning) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp))
            } else {
                Text("Scan Sekarang")
            }
        }

        ManifestNavButton("Kelola Rule", onOpenRules)
        ManifestNavButton("Riwayat Aktivitas & Undo", onOpenLog)
        ManifestNavButton("Pengaturan", onOpenSettings)
        ManifestNavButton("Diagnostik", onOpenDiagnostics)
    }
    }
}

@Composable
private fun ManifestRow(label: String, value: String) {
    Text(
        "$label   $value",
        style = MaterialTheme.typography.labelMedium,
        color = InkFaint
    )
}

@Composable
private fun ManifestNavButton(label: String, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        border = androidx.compose.foundation.BorderStroke(1.dp, Pine),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = Pine),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.ChevronRight, contentDescription = null, modifier = Modifier.size(18.dp))
            Text(label, modifier = Modifier.padding(start = 4.dp))
        }
    }
}
