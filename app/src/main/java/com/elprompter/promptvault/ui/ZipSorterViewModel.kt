package com.elprompter.promptvault.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.elprompter.promptvault.zipsorter.model.SortConfig
import com.elprompter.promptvault.zipsorter.model.SortState
import com.elprompter.promptvault.zipsorter.repository.ZipSorterRepositoryImpl
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Contoh ViewModel sederhana untuk modul Zip Sorter. Terpisah dari
 * MainViewModel supaya tidak menyentuh state/logika rule engine utama.
 */
class ZipSorterViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ZipSorterRepositoryImpl(application)

    private val _sortState = MutableStateFlow<SortState>(SortState.Idle)
    val sortState: StateFlow<SortState> = _sortState.asStateFlow()

    /** Uri folder SAF terakhir yang dipilih user lewat ACTION_OPEN_DOCUMENT_TREE. */
    private val _selectedFolderUri = MutableStateFlow<Uri?>(null)
    val selectedFolderUri: StateFlow<Uri?> = _selectedFolderUri.asStateFlow()

    /** Dipanggil dari callback ActivityResultContracts.OpenDocumentTree di Compose. */
    fun onFolderPicked(uri: Uri?) {
        if (uri == null) return
        // Persist permission supaya URI tetap valid dipakai lagi setelah app/HP di-restart.
        getApplication<Application>().contentResolver.takePersistableUriPermission(
            uri,
            android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or
                android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        )
        _selectedFolderUri.value = uri
    }

    fun startSort(config: SortConfig = SortConfig()) {
        val uri = _selectedFolderUri.value ?: run {
            _sortState.value = SortState.Error("Pilih folder dulu sebelum mulai sortir.")
            return
        }
        viewModelScope.launch {
            repository.processFolder(uri, config).collect { state ->
                _sortState.value = state
            }
        }
    }

    fun resetState() {
        _sortState.value = SortState.Idle
    }
}
