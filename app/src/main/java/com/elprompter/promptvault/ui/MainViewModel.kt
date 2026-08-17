package com.elprompter.promptvault.ui

import android.app.Application
import android.content.Intent
import android.net.Uri
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
import com.elprompter.promptvault.shizuku.ShizukuManager
import com.elprompter.promptvault.util.FileSorter
import com.elprompter.promptvault.util.PatternPreviewResult
import com.elprompter.promptvault.util.SkippedFileInfo
import com.elprompter.promptvault.worker.WorkScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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

    /** [Technical debt #4] Lihat dokumentasi lengkap di SettingsRepository.DEFAULT_SCAN_CONCURRENCY. */
    val scanConcurrency: StateFlow<Int> = settingsRepository.scanConcurrencyFlow
        .let { flow ->
            val state = MutableStateFlow(SettingsRepository.DEFAULT_SCAN_CONCURRENCY)
            viewModelScope.launch { flow.collect { state.value = it } }
            state.asStateFlow()
        }


    /** [SAF] URI folder TUJUAN kustom aktif (tree URI, `null` = belum diset / tujuan tetap Downloads/PromptVault). Sumber scan SELALU Downloads, lihat FileSorter.scanAndSort. */
    val safTreeUri: StateFlow<String?> = settingsRepository.safTreeUriFlow
        .let { flow ->
            val state = MutableStateFlow<String?>(null)
            viewModelScope.launch { flow.collect { state.value = it } }
            state.asStateFlow()
        }

    /**
     * [SAF, fix audit P0 #1 -- "validasi permission saat startup",
     * SAF_FINAL_LOGIC_AUDIT.md 2026-08-12] `true` kalau folder kustom SUDAH
     * DIKONFIGURASI tapi tidak bisa diakses lagi. Dicek ULANG setiap kali
     * [safTreeUri] berubah nilainya -- termasuk emisi PERTAMA saat app baru
     * dibuka (StateFlow selalu replay nilai terakhir ke collector baru),
     * jadi ini otomatis mencakup "validasi saat startup" TANPA butuh init
     * block terpisah yang gampang lupa dipanggil. `false` kalau memang
     * belum pernah diset (bukan error) atau folder aktif & sehat --
     * SettingsScreen HANYA menampilkan warning saat benar-benar `true`.
     */
    val safAccessLost: StateFlow<Boolean> = safTreeUri
        .let { flow ->
            val state = MutableStateFlow(false)
            viewModelScope.launch {
                flow.collect { uri ->
                    state.value = if (uri == null) {
                        false
                    } else {
                        withContext(Dispatchers.IO) { fileSorter.checkSafAccessLost() }
                    }
                }
            }
            state.asStateFlow()
        }

    /** [Fitur baru 2026-08-17, integrasi Shizuku] Status binder/permission -- langsung dari singleton, sudah StateFlow. */
    val shizukuStatus: StateFlow<ShizukuManager.Status> = ShizukuManager.status

    /** [Fitur baru 2026-08-17, integrasi Shizuku] `null` = belum diisi -- lihat peringatan lengkap di SettingsRepository.shizukuDestPathFlow. */
    val shizukuDestPath: StateFlow<String?> = settingsRepository.shizukuDestPathFlow
        .let { flow ->
            val state = MutableStateFlow<String?>(null)
            viewModelScope.launch { flow.collect { state.value = it } }
            state.asStateFlow()
        }

    /** [Fitur baru 2026-08-17, integrasi Shizuku] `true` = tujuan kustom lewat Shizuku aktif, mengesampingkan cabang SAF. */
    val useShizuku: StateFlow<Boolean> = settingsRepository.useShizukuFlow
        .let { flow ->
            val state = MutableStateFlow(false)
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

    /**
     * v2.16.0 -- technical debt closure: tombol "Simpan" di AddEditRuleScreen
     * sebelumnya TIDAK PERNAH punya konfirmasi sukses eksplisit (gap yang
     * sudah dicatat sejak audit v2.4.3, sengaja belum difix). Pola one-shot
     * sama seperti [ScanFeedback] (eventId, BUKAN isi teks, supaya tetap
     * trigger walau nama folder sama persis dgn save sebelumnya) --
     * DISIMPAN DI SINI (ViewModel), bukan state lokal AddEditRuleScreen,
     * karena layar itu langsung di-pop/dispose sesaat setelah simpan
     * (persis kelas bug yang sama dgn Snackbar Home v2.4.4: composable yang
     * sudah dibuang tidak bisa lagi menampilkan Snackbar-nya sendiri).
     * Dikonsumsi oleh RuleListScreen (layar TUJUAN setelah pop back), yang
     * bertahan hidup lintas navigasi tsb.
     */
    data class RuleSaveFeedback(val folderName: String, val eventId: Long)

    private val _ruleSaveFeedback = MutableStateFlow<RuleSaveFeedback?>(null)
    val ruleSaveFeedback: StateFlow<RuleSaveFeedback?> = _ruleSaveFeedback.asStateFlow()

    fun consumeRuleSaveFeedback() {
        _ruleSaveFeedback.value = null
    }

    fun runManualScan() {
        viewModelScope.launch {
            _isScanning.value = true
            val result = fileSorter.scanAndSort()
            // [fix audit P0 #2] safAccessLost dicek PALING AWAL -- dua kegagalan
            // ini butuh pesan beda (fix izin storage vs pilih ulang folder kustom
            // di Pengaturan), jangan sampai tertutup pesan Downloads generik.
            val isError = result.foldersUnreadable || result.safAccessLost
            val summary = when {
                result.safAccessLost -> "Folder kustom tidak bisa diakses. Pilih ulang folder atau kembali ke Downloads lewat Pengaturan."
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

    /**
     * [announce]=true HANYA dipakai jalur simpan eksplisit dari form
     * AddEditRuleScreen (lihat [RuleSaveFeedback]) -- default false supaya
     * toggle enable/disable Switch di RuleCard (yang juga lewat fungsi ini)
     * TIDAK memicu Snackbar "disimpan" berulang tiap kali digeser, yang
     * justru jadi noise bukan feedback berguna.
     */
    fun saveRule(rule: Rule, removeDuplicateRuleId: String? = null, announce: Boolean = false) {
        viewModelScope.launch {
            ruleRepository.upsertRule(rule, removeDuplicateRuleId)
            if (announce) {
                _ruleSaveFeedback.value = RuleSaveFeedback(rule.folderName, System.currentTimeMillis())
            }
        }
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

    /** [Technical debt #4] Lihat dokumentasi lengkap di SettingsRepository.setScanConcurrency. */
    fun setScanConcurrency(value: Int) {
        viewModelScope.launch { settingsRepository.setScanConcurrency(value) }
    }

    /** [Fitur baru 2026-08-17, integrasi Shizuku] Minta izin Shizuku -- no-op aman kalau Shizuku belum terpasang/jalan (lihat ShizukuManager.requestPermission). */
    fun requestShizukuPermission() = ShizukuManager.requestPermission()

    /** [Fitur baru 2026-08-17, integrasi Shizuku] Cek ulang status manual, mis. setelah user balik dari app Shizuku Manager. */
    fun refreshShizukuStatus() = ShizukuManager.refreshStatus()

    /**
     * [Fitur baru 2026-08-17, integrasi Shizuku] Simpan path folder tujuan
     * Shizuku APA ADANYA (trim spasi saja, lihat SettingsRepository) --
     * TIDAK divalidasi/dibuat di sini. Validasi keberadaan folder terjadi
     * saat scan ([FileSorter.scanAndSortViaShizuku]), BUKAN saat disimpan --
     * user boleh isi path folder yang BELUM dibuat dulu, lalu buat foldernya
     * belakangan lewat file manager, baru scan. Peringatan lengkap ada di
     * kartu "Mode Shizuku" (SettingsScreen).
     */
    fun setShizukuDestPath(path: String) {
        viewModelScope.launch { settingsRepository.setShizukuDestPath(path) }
    }

    fun clearShizukuDestPath() {
        viewModelScope.launch { settingsRepository.clearShizukuDestPath() }
    }

    fun setUseShizuku(value: Boolean) {
        viewModelScope.launch { settingsRepository.setUseShizuku(value) }
    }

    /**
     * [SAF, syarat (c) Insiden #7] Simpan folder kustom baru dari hasil
     * `ActivityResultContracts.OpenDocumentTree()` di MainActivity.
     * `takePersistableUriPermission` WAJIB dipanggil di sini SEBELUM URI
     * disimpan -- kalau provider menolak (SecurityException), URI TIDAK
     * disimpan sama sekali, supaya user tidak pernah lihat state "folder
     * aktif" yang sebenarnya tidak akan bertahan lintas restart app.
     * Folder LAMA (kalau ada) baru dilepas SETELAH folder baru sukses
     * tersimpan -- urutan ini sengaja, mencegah window singkat tanpa izin
     * valid sama sekali kalau app mati di antara dua operasi.
     */
    fun setSafTreeUri(uri: Uri) {
        viewModelScope.launch {
            val app = getApplication<Application>()
            val previous = settingsRepository.getSafTreeUri()
            try {
                app.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
            } catch (e: SecurityException) {
                return@launch
            }
            settingsRepository.setSafTreeUri(uri.toString())
            if (previous != null && previous != uri.toString()) {
                releaseSafPermission(previous)
            }
        }
    }

    /** [SAF] Kembali menyimpan hasil sortir ke Downloads/PromptVault biasa; lepas persistable permission folder tujuan yang sedang aktif. */
    fun clearSafTreeUri() {
        viewModelScope.launch {
            val previous = settingsRepository.getSafTreeUri()
            settingsRepository.clearSafTreeUri()
            if (previous != null) releaseSafPermission(previous)
        }
    }

    /**
     * [SAF, fix Bug #1 v2.10.0] Sebelumnya "release" ini terdokumentasi di
     * komentar tapi TIDAK PERNAH benar-benar dipanggil di kode manapun --
     * setiap ganti folder kustom membiarkan izin folder lama menumpuk tanpa
     * batas (leak). Di sini fungsi ini BENAR-BENAR dipanggil dari
     * [setSafTreeUri] & [clearSafTreeUri], bukan cuma didokumentasikan.
     */
    private fun releaseSafPermission(uriString: String) {
        try {
            getApplication<Application>().contentResolver.releasePersistableUriPermission(
                Uri.parse(uriString),
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
        } catch (e: SecurityException) {
            // Izin memang sudah tidak ada/sudah dilepas sebelumnya -- aman diabaikan.
        }
    }

    /**
     * BUG lama (ditemukan & diperbaiki 2026-08-07): sebelumnya fire-and-forget
     * (`viewModelScope.launch { fileSorter.undo(entry) }`), jadi caller (UI)
     * TIDAK PERNAH tahu hasil sebenarnya -- `ActivityLogScreen` menampilkan
     * snackbar "berhasil" SELALU, walau undo aslinya gagal (mis. file tujuan
     * sudah tidak ada). Sekarang `suspend` dan mengembalikan hasil asli dari
     * [FileSorter.undo], supaya UI bisa menampilkan pesan yang jujur.
     *
     * [Dispatcher fix, sesi ini] `FileSorter.undo()` (dan kedua turunan SAF-nya,
     * `undoSaf`/`undoSafDestination`) TIDAK punya `withContext` sendiri --
     * catatan lama di PROJECT_STATE.md soal ini SENGAJA tidak diubah di
     * [FileSorter] biar tidak menyentuh 3 fungsi sekaligus (batas file/modul).
     * Caller di `MainActivity.kt` (`onUndo = { entry -> viewModel.undoMove(entry) }`)
     * dipanggil dari `rememberCoroutineScope()` Compose -- scope itu default ke
     * `Dispatchers.Main`, jadi tanpa pembungkus ini SEMUA I/O undo (baca/tulis
     * file lokal, atau `DocumentFile`/`ContentResolver` utk SAF) jalan di
     * main thread. Sama pola & alasan dgn [checkSafAccessLost] di atas: bungkus
     * di titik pemanggilan (ViewModel), bukan ubah signature [FileSorter.undo].
     */
    suspend fun undoMove(entry: MoveHistoryEntry): Boolean =
        withContext(Dispatchers.IO) { fileSorter.undo(entry) }

    /**
     * [Fitur baru 2026-08-17 -- "sweep-select to undo", permintaan eksplisit
     * user] Undo BANYAK entri sekaligus, dipanggil dari mode seleksi-sapuan
     * di ActivityLogScreen (drag jari di atas beberapa baris utk pilih,
     * bukan tap tombol Undo satu-satu). SENGAJA sekuensial (bukan
     * `async`/paralel spt scan) -- volume undo batch biasanya kecil (user
     * pilih manual lewat sapuan jari, bukan ratusan file spt scan), dan
     * sekuensial menghindari kompleksitas tambahan menggabungkan hasil
     * paralel utk kasus yang tidak butuh performa setinggi itu. Mengembalikan
     * (jumlah sukses, jumlah gagal) -- UI menampilkan ringkasan, bukan
     * per-file (detail tetap ada di tab Log seperti biasa).
     */
    suspend fun undoMultiple(entries: List<MoveHistoryEntry>): Pair<Int, Int> = withContext(Dispatchers.IO) {
        var success = 0
        var failed = 0
        for (entry in entries) {
            if (fileSorter.undo(entry)) success++ else failed++
        }
        success to failed
    }

    suspend fun exportRulesJson(): String = ruleRepository.exportAsJson()

    /**
     * [Fix audit P2 #UI-13, 2026-08-15] `onDone` sekarang `(Boolean, Int) ->
     * Unit` (parseSuccess, importedCount) -- sebelumnya `(Int) -> Unit` tidak
     * bisa membedakan "gagal parse" dari "berhasil parse tapi 0 rule". Murni
     * pass-through hasil [RuleRepository.ImportOutcome], nol perubahan logika.
     */
    fun importRulesJson(text: String, onDone: (Boolean, Int) -> Unit) {
        viewModelScope.launch {
            val outcome = ruleRepository.importFromJson(text)
            onDone(outcome.parseSuccess, outcome.importedCount)
        }
    }

    suspend fun findAllOverlaps() = ruleRepository.findAllOverlaps()

    /** Uji pattern include+exclude langsung terhadap isi Downloads saat ini (belum tersimpan sebagai rule). */
    suspend fun previewPattern(pattern: String, excludePattern: String = ""): PatternPreviewResult =
        fileSorter.previewPatternMatches(pattern, excludePattern)

    /** Nama file asli (semua ekstensi, sejak fix 2026-08-13) di Downloads, untuk layar Diagnostik. */
    fun listDownloadsFileNames(): List<String> = fileSorter.listDownloadsCandidateFileNames()
}
