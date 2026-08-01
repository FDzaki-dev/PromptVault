package com.elprompter.promptvault.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Rule
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.elprompter.promptvault.ui.components.GroupedList
import com.elprompter.promptvault.ui.components.GroupedListRow
import com.elprompter.promptvault.ui.components.VaultCard
import com.elprompter.promptvault.ui.components.pressScale
import com.elprompter.promptvault.ui.theme.VaultTheme

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
    val colors = MaterialTheme.colorScheme
    Scaffold { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    // Wash gradient halus di latar, supaya layar tidak terasa
                    // datar rata satu warna dari atas sampai bawah (keluhan
                    // "monoton") -- tetap subtil, bukan warna-warni mencolok.
                    Brush.verticalGradient(
                        colors = listOf(colors.surfaceVariant.copy(alpha = 0.55f), colors.background),
                        endY = 900f
                    )
                )
        ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp)
                .padding(top = 12.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("PromptVault", style = MaterialTheme.typography.headlineMedium, color = colors.onBackground)
            Text("Rapikan otomatis file ZIP & TXT di Downloads kamu.", style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)

            VaultCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    ManifestRow(icon = Icons.Filled.Rule, tint = colors.primary, label = "Rule aktif", value = "$ruleCount")
                    ManifestRow(icon = Icons.Filled.Schedule, tint = colors.tertiary, label = "Auto-scan", value = "tiap $intervalMinutes menit")
                    if (lastScanSummary != null) {
                        Text(lastScanSummary, style = MaterialTheme.typography.bodySmall, color = colors.onSurface)
                    }
                    if (hasSkippedFiles) {
                        TextButton(onClick = onOpenSkippedFiles, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Filled.ErrorOutline, contentDescription = null, tint = colors.secondary, modifier = Modifier.size(16.dp))
                            Text(" Lihat detail file yang dilewati", color = colors.secondary, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }

            val scanInteraction = remember { MutableInteractionSource() }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.large)
                    .background(
                        // CTA utama sekarang gradient hangat (Stamp -> Amber),
                        // bukan blok warna solid tunggal, supaya jadi titik
                        // fokus visual yang jelas dan lebih "hidup".
                        Brush.horizontalGradient(colors = listOf(colors.secondary, colors.tertiary))
                    )
                    .clickable(
                        interactionSource = scanInteraction,
                        indication = null,
                        enabled = !isScanning,
                        onClick = onScanNow
                    )
                    .pressScale(scanInteraction)
                    .padding(vertical = 15.dp),
                contentAlignment = Alignment.Center
            ) {
                if (isScanning) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = colors.onSecondary)
                } else {
                    Text("Scan Sekarang", color = colors.onSecondary, style = MaterialTheme.typography.titleMedium)
                }
            }

            val extraColors = VaultTheme.extraColors
            GroupedList(
                rows = listOf(
                    { GroupedListRow(Icons.Filled.Rule, "Kelola Rule", colors.primary, onOpenRules) },
                    { GroupedListRow(Icons.Filled.History, "Riwayat Aktivitas & Undo", colors.tertiary, onOpenLog) },
                    { GroupedListRow(Icons.Filled.Settings, "Pengaturan", extraColors.slate, onOpenSettings) },
                    { GroupedListRow(Icons.Filled.BugReport, "Diagnostik", colors.error, onOpenDiagnostics) }
                )
            )
        }
        }
    }
}

@Composable
private fun ManifestRow(icon: androidx.compose.ui.graphics.vector.ImageVector, tint: androidx.compose.ui.graphics.Color, label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(16.dp))
        Text(
            "  $label   $value",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
