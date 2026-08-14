package com.elprompter.promptvault.ui.screens

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import com.elprompter.promptvault.ui.components.VaultCard
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.elprompter.promptvault.ui.components.EmptyState
import com.elprompter.promptvault.util.SkippedFileInfo

/**
 * Jawaban langsung untuk keluhan "dilewati doang, gak jelas": layar ini menunjukkan
 * SETIAP nama file yang dilewati pada scan terakhir beserta alasannya secara eksplisit,
 * bukan cuma angka ringkasan.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SkippedFilesScreen(
    skipped: List<SkippedFileInfo>,
    // UI-09 fix: parameter baru, WAJIB diisi caller -- sebelumnya empty
    // state "Semua file cocok" & "belum pernah scan" digabung jadi 1 pesan
    // ambigu, user tidak bisa bedakan apakah sistem sudah bekerja atau
    // memang belum pernah dijalankan sama sekali. Sumber sinyal: caller
    // (MainActivity) sudah punya `lastScanSummary` (null == belum pernah
    // scan), diteruskan ke sini sebagai Boolean murni.
    hasScannedBefore: Boolean,
    onBack: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    androidx.compose.material3.Scaffold(
        topBar = { com.elprompter.promptvault.ui.components.VaultTopBar(title = "Detail File Dilewati", onBack = onBack) }
    ) { padding ->
    Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
        Text(
            "Data dari scan terakhir. Jalankan \"Scan Sekarang\" lagi untuk memperbarui daftar ini.",
            style = MaterialTheme.typography.bodySmall
        )

        Crossfade(targetState = skipped.isEmpty(), label = "skippedFilesEmptyState", animationSpec = tween(220)) { isEmpty ->
            if (isEmpty) {
                // UI-09 fix: 2 pesan eksplisit berbeda, bukan 1 pesan gabungan.
                EmptyState(
                    icon = Icons.Filled.TaskAlt,
                    title = if (hasScannedBefore) "Tidak ada file yang dilewati" else "Belum pernah scan",
                    message = if (hasScannedBefore) {
                        "Semua file cocok dengan rule pada scan terakhir."
                    } else {
                        "Jalankan \"Scan Sekarang\" dari Home dulu untuk melihat file yang dilewati di sini."
                    },
                    accentColor = colors.secondary,
                    accentContainerColor = colors.secondaryContainer
                )
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(top = 12.dp)) {
                    items(skipped, key = { it.fileName + it.reason }) { info ->
                        VaultCard(modifier = Modifier.fillMaxWidth().animateItemPlacement()) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(info.fileName, style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    info.reason,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }
        }
    }
    }
}
