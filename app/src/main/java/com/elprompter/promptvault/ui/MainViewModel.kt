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

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _lastScanSummary = MutableStateFlow<String?>(null)
    val lastScanSummary: StateFlow<String?> = _lastScanSummary.asStateFlow()

    /** Detail file yang dilewati pada scan TERAKHIR, lengkap dengan alasannya. */
    private val _lastSkippedFiles = MutableStateFlow<List<SkippedFileInfo>>(emptyList())
    val lastSkippedFiles: StateFlow<List<SkippedFileInfo>> = _lastSkippedFiles.asStateFlow()

    fun runManualScan() {
        viewModelScope.launch {
            _isScanning.value = true
            val result = fileSorter.scanAndSort()
            _lastScanSummary.value = when {
                result.foldersUnreadable -> "Folder Downloads tidak terbaca. Cek izin penyimpanan."
                result.filesMoved == 0 && result.filesSkippedNoMatch == 0 -> "Tidak ada file cocok yang ditemukan."
                else -> "${result.filesMoved} file dipindahkan, ${result.filesSkippedNoMatch} dilewati."
            }
            _lastSkippedFiles.value = result.skippedDetails
            _isScanning.value = false
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

    fun undoMove(entry: MoveHistoryEntry) {
        viewModelScope.launch { fileSorter.undo(entry) }
    }

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
