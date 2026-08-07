package com.elprompter.promptvault.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.elprompter.promptvault.data.ActivityLogEntry
import com.elprompter.promptvault.data.ActivityLogRepository
import com.elprompter.promptvault.data.ConflictStrategy
import com.elprompter.promptvault.data.MoveHistoryEntry
import com.elprompter.promptvault.data.MoveHistoryRepository
import com.elprompter.promptvault.data.Rule
import com.elprompter.promptvault.data.RuleRepository
import com.elprompter.promptvault.data.SaveRuleCheck
import com.elprompter.promptvault.data.SettingsRepository
import com.elprompter.promptvault.data.ThemeMode
import com.elprompter.promptvault.util.FileSorter
import com.elprompter.promptvault.util.PatternPreviewResult
import com.elprompter.promptvault.util.SkippedFileInfo
import com.elprompter.promptvault.worker.WorkScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val ruleRepository = RuleRepository(application)
    private val activityLogRepository = ActivityLogRepository(application)
    private val moveHistoryRepository = MoveHistoryRepository(application)
    private val settingsRepository = SettingsRepository(application)
    private val fileSorter = FileSorter(application, ruleRepository, activityLogRepository, moveHistoryRepository, settingsRepository)

    val rules: StateFlow<List<Rule>> = ruleRepository.rulesFlow
        .let { flow ->
            val state = MutableStateFlow<List<Rule>>(emptyList())
            viewModelScope.launch { flow.collect { state.value = it } }
            state.asStateFlow()
        }

    val logEntries: StateFlow<List<ActivityLogEntry>> = activityLogRepository.logFlow
        .let { flow ->
            val state = MutableStateFlow<List<ActivityLogEntry>>(emptyList())
            viewModelScope.launch { flow.collect { state.value = it } }
            state.asStateFlow()
        }

    val undoableHistory: StateFlow<List<MoveHistoryEntry>> = moveHistoryRepository.historyFlow
        .let { flow ->
            val state = MutableStateFlow<List<MoveHistoryEntry>>(emptyList())
            viewModelScope.launch { flow.collect { state.value = it } }
            state.asStateFlow()
        }

    val intervalMinutes: StateFlow<Int> = settingsRepository.intervalMinutesFlow
        .let { flow ->
            val state = MutableStateFlow(SettingsRepository.DEFAULT_INTERVAL_MINUTES)
            viewModelScope.launch { flow.collect { state.value = it } }
            state.asStateFlow()
        }

    val conflictStrategy: StateFlow<ConflictStrategy> = settingsRepository.conflictStrategyFlow
        .let { flow ->
            val state = MutableStateFlow(SettingsRepository.DEFAULT_CONFLICT_STRATEGY)
            viewModelScope.launch { flow.collect { state.value = it } }
            state.asStateFlow()
        }

    val themeMode: StateFlow<ThemeMode> = settingsRepository.themeModeFlow
        .let { flow ->
            val state = MutableStateFlow(SettingsRepository.DEFAULT_THEME_MODE)
            viewModelScope.launch { flow.collect { state.value = it } }
            state.asStateFlow()
        }

    /** Batch §1 Fase 1/2 (hybrid SAF, opsional) -- null berarti masih pakai Downloads/java.io.File biasa. */
    val safTreeUri: StateFlow<String?> = settingsRepository.safTreeUriFlow
        .let { flow ->
            val state = MutableStateFlow<String?>(null)
            viewModelScope.launch { flow.collect { state.value = it } }
            state.asStateFlow()
        }

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _lastScanSummary = MutableStateFlow<String?>(null)
    val lastScanSummary: StateFlow<String?> = _lastScanSummary.asStateFlow()

    /**
     * Sinyal one-shot terpisah dari [lastScanSummary]. Dibedakan lewat [ScanFeedback.eventId]
     * (bukan isi teks) supaya Snackbar TETAP muncul walau hasil scan kali ini teksnya
     * sama persis dengan scan sebelumnya (mis. "Tidak ada file cocok" dua kali berturut) --
     * StateFlow biasa tidak akan re-trigger LaunchedEffect kalau value-nya identik.
     */
    data class ScanFeedback(val message: String, val isError: Boolean, val eventId: Long)

    private val _scanFeedback = MutableStateFlow<ScanFeedback?>(null)
    val scanFeedback: StateFlow<ScanFeedback?> = _scanFeedback.asStateFlow()

    /**
     * Dipanggil UI SEGERA setelah Snackbar ditampilkan. Wajib ada supaya event
     * tidak nyangkut di StateFlow -- kalau tidak di-null-kan, HomeScreen yang
     * di-dispose+dibuat ulang oleh Navigation Compose (mis. pergi ke
     * SkippedFilesScreen lalu balik) akan menganggapnya event BARU dan
     * menampilkan Snackbar yang sama lagi (bug yang dilaporkan user 2026-08-04).
     */
    fun consumeScanFeedback() {
        _scanFeedback.value = null
    }

    /** Detail file yang dilewati pada scan TERAKHIR, lengkap dengan alasannya. */
    private val _lastSkippedFiles = MutableStateFlow<List<SkippedFileInfo>>(emptyList())
    val lastSkippedFiles: StateFlow<List<SkippedFileInfo>> = _lastSkippedFiles.asStateFlow()

    fun runManualScan() {
        viewModelScope.launch {
            _isScanning.value = true
            val result = fileSorter.scanAndSort()
            val isError = result.foldersUnreadable
            val summary = when {
                result.foldersUnreadable -> "Folder Downloads tidak terbaca. Cek izin penyimpanan."
                result.filesMoved == 0 && result.filesSkippedNoMatch == 0 -> "Tidak ada file cocok yang ditemukan."
                else -> "${result.filesMoved} file dipindahkan, ${result.filesSkippedNoMatch} dilewati."
            }
            _lastScanSummary.value = summary
            _lastSkippedFiles.value = result.skippedDetails
            _isScanning.value = false
            // Dikirim TERAKHIR, setelah isScanning kembali false, supaya urutan
            // yang diterima UI selalu: spinner hilang -> baru Snackbar/haptic muncul.
            _scanFeedback.value = ScanFeedback(summary, isError, System.currentTimeMillis())
        }
    }

    suspend fun checkBeforeSave(rule: Rule): SaveRuleCheck = ruleRepository.checkBeforeSave(rule)

    fun saveRule(rule: Rule, removeDuplicateRuleId: String? = null) {
        viewModelScope.launch { ruleRepository.upsertRule(rule, removeDuplicateRuleId) }
    }

    fun deleteRule(ruleId: String) {
        viewModelScope.launch { ruleRepository.deleteRule(ruleId) }
    }

    fun moveRuleUp(ruleId: String) {
        viewModelScope.launch { ruleRepository.moveRuleUp(ruleId) }
    }

    fun moveRuleDown(ruleId: String) {
        viewModelScope.launch { ruleRepository.moveRuleDown(ruleId) }
    }

    fun setIntervalMinutes(minutes: Int) {
        viewModelScope.launch {
            settingsRepository.setIntervalMinutes(minutes)
            WorkScheduler.schedule(getApplication(), minutes)
        }
    }

    fun setConflictStrategy(strategy: ConflictStrategy) {
        viewModelScope.launch { settingsRepository.setConflictStrategy(strategy) }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { settingsRepository.setThemeMode(mode) }
    }

    /**
     * Dipanggil MainActivity SETELAH takePersistableUriPermission() sukses -- URI mentah,
     * sudah persistable. Kalau sebelumnya sudah ada folder kustom lain tersimpan, lepas
     * dulu izin persisted-nya (lihat catatan bug di [clearSafTreeUri]) sebelum menyimpan
     * yang baru -- ganti folder berkali-kali TIDAK BOLEH menumpuk izin lama yang sudah
     * tidak dipakai.
     */
    fun setSafTreeUri(uriString: String) {
        viewModelScope.launch {
            val previous = settingsRepository.getSafTreeUri()
            if (previous != null && previous != uriString) {
                releaseSafPermission(previous)
            }
            settingsRepository.setSafTreeUri(uriString)
        }
    }

    /**
     * Lepas SAF & kembali ke Downloads/java.io.File.
     *
     * BUG lama (ditemukan & diperbaiki 2026-08-07): komentar di sini sebelumnya bilang
     * "pelepasan persisted permission dilakukan di pemanggil (MainActivity)", tapi
     * MainActivity TIDAK PERNAH benar-benar memanggilnya -- izin persisted menumpuk
     * selamanya tiap kali user ganti/lepas folder kustom. Android membatasi jumlah
     * persisted URI permission per app (~128, riwayat resmi Android); kalau limit
     * tercapai, `takePersistableUriPermission()` berikutnya lempar SecurityException
     * dan fitur folder kustom berhenti bisa dipakai sama sekali tanpa pesan jelas ke
     * user. Sekarang dilepas EKSPLISIT di sini, sebelum state dibersihkan.
     */
    fun clearSafTreeUri() {
        viewModelScope.launch {
            val current = settingsRepository.getSafTreeUri()
            if (current != null) {
                releaseSafPermission(current)
            }
            settingsRepository.setSafTreeUri(null)
        }
    }

    /** Best-effort: kegagalan lepas izin (URI sudah invalid/dicabut OS) tidak boleh menghalangi ganti/hapus folder. */
    private fun releaseSafPermission(uriString: String) {
        try {
            val uri = android.net.Uri.parse(uriString)
            getApplication<Application>().contentResolver.releasePersistableUriPermission(
                uri,
                android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
        } catch (_: SecurityException) {
            // Izin memang sudah tidak ada/sudah dicabut duluan -- aman diabaikan.
        }
    }

    /**
     * BUG lama (ditemukan & diperbaiki 2026-08-07): sebelumnya fire-and-forget
     * (`viewModelScope.launch { fileSorter.undo(entry) }`), jadi caller (UI)
     * TIDAK PERNAH tahu hasil sebenarnya -- `ActivityLogScreen` menampilkan
     * snackbar "berhasil" SELALU, walau undo aslinya gagal (mis. file tujuan
     * sudah tidak ada, folder SAF izin dicabut). Sekarang `suspend` dan
     * mengembalikan hasil asli dari [FileSorter.undo], supaya UI bisa
     * menampilkan pesan yang jujur.
     */
    suspend fun undoMove(entry: MoveHistoryEntry): Boolean = fileSorter.undo(entry)

    suspend fun exportRulesJson(): String = ruleRepository.exportAsJson()

    fun importRulesJson(text: String, onDone: (Int) -> Unit) {
        viewModelScope.launch {
            val count = ruleRepository.importFromJson(text)
            onDone(count)
        }
    }

    suspend fun findAllOverlaps() = ruleRepository.findAllOverlaps()

    /** Uji pattern include+exclude langsung terhadap isi Downloads saat ini (belum tersimpan sebagai rule). */
    fun previewPattern(pattern: String, excludePattern: String = ""): PatternPreviewResult =
        fileSorter.previewPatternMatches(pattern, excludePattern)

    /** Nama file ZIP/TXT asli di Downloads, untuk layar Diagnostik. */
    fun listDownloadsFileNames(): List<String> = fileSorter.listDownloadsCandidateFileNames()
}
