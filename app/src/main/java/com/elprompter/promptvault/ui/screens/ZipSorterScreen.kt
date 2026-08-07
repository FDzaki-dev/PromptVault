package com.elprompter.promptvault.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.elprompter.promptvault.ui.components.VaultCard
import com.elprompter.promptvault.ui.components.VaultTopBar
import com.elprompter.promptvault.zipsorter.model.SortState
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import android.net.Uri

/**
 * Contoh Activity/Compose Screen sederhana yang memanggil ZipSorterRepository
 * (lewat ZipSorterViewModel) menggunakan intent SAF ACTION_OPEN_DOCUMENT_TREE.
 * Screen ini standalone -- silakan sesuaikan tampilan sesuai kebutuhan.
 */
@Composable
fun ZipSorterScreen(
    selectedFolderUri: Uri?,
    sortState: SortState,
    onPickFolder: (Uri?) -> Unit,
    onStartSort: () -> Unit,
    onBack: () -> Unit
) {
    // ActivityResultContracts.OpenDocumentTree() = wrapper resmi utk
    // Intent.ACTION_OPEN_DOCUMENT_TREE, tidak butuh permission legacy.
    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri -> onPickFolder(uri) }

    Scaffold(
        topBar = { VaultTopBar(title = "Zip Sorter", onBack = onBack) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "Pilih folder, lalu jalankan sortir: file dikelompokkan per " +
                    "kategori (Documents/Images/Videos/Audio/Archives), ZIP " +
                    "otomatis diekstrak ke sub-folder sesuai namanya.",
                style = MaterialTheme.typography.bodyMedium
            )

            VaultCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Folder Target", style = MaterialTheme.typography.titleMedium)
                    Text(
                        selectedFolderUri?.toString() ?: "Belum ada folder dipilih.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Button(onClick = { folderPickerLauncher.launch(null) }) {
                        Text("Pilih Folder (SAF)")
                    }
                }
            }

            when (sortState) {
                is SortState.Idle -> Unit
                is SortState.Scanning -> Text(sortState.currentFolder, style = MaterialTheme.typography.bodySmall)
                is SortState.Processing -> {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Memproses: ${sortState.fileName}", style = MaterialTheme.typography.bodySmall)
                        LinearProgressIndicator(
                            progress = sortState.progressPercent / 100f,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                is SortState.Success -> Text(
                    "Selesai. ${sortState.processedCount} file diproses, " +
                        "${sortState.extractedZipCount} ZIP diekstrak.",
                    style = MaterialTheme.typography.bodyMedium
                )
                is SortState.Error -> Text(
                    "Error: ${sortState.message}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Button(
                onClick = onStartSort,
                enabled = selectedFolderUri != null && sortState !is SortState.Processing
            ) {
                Text("Mulai Sortir")
            }
        }
    }
}
