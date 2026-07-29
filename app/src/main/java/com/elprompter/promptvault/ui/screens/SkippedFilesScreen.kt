package com.elprompter.promptvault.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.elprompter.promptvault.util.SkippedFileInfo

/**
 * Jawaban langsung untuk keluhan "dilewati doang, gak jelas": layar ini menunjukkan
 * SETIAP nama file yang dilewati pada scan terakhir beserta alasannya secara eksplisit,
 * bukan cuma angka ringkasan.
 */
@Composable
fun SkippedFilesScreen(skipped: List<SkippedFileInfo>) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Detail File Dilewati", style = MaterialTheme.typography.headlineSmall)
        Text(
            "Data dari scan terakhir. Jalankan \"Scan Sekarang\" lagi untuk memperbarui daftar ini.",
            style = MaterialTheme.typography.bodySmall
        )

        if (skipped.isEmpty()) {
            Text(
                "Tidak ada file yang dilewati pada scan terakhir (atau belum pernah scan).",
                modifier = Modifier.padding(top = 16.dp)
            )
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(top = 12.dp)) {
                items(skipped, key = { it.fileName + it.reason }) { info ->
                    Card(modifier = Modifier.fillMaxWidth()) {
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
