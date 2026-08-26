package com.elprompter.promptvault.ui.screens

import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.elprompter.promptvault.R
import com.elprompter.promptvault.data.ConflictStrategy
import com.elprompter.promptvault.data.SettingsRepository
import com.elprompter.promptvault.shizuku.ShizukuManager
import com.elprompter.promptvault.ui.MainViewModel
import com.elprompter.promptvault.update.DownloadState
import com.elprompter.promptvault.update.GithubAssetDto
import com.elprompter.promptvault.update.UpdateCheckResult
import com.elprompter.promptvault.ui.components.TactileSwitch
import com.elprompter.promptvault.ui.components.VaultActionSheet
import com.elprompter.promptvault.ui.components.VaultCard
import com.elprompter.promptvault.ui.components.VaultTopBar
import com.elprompter.promptvault.ui.components.WarningBanner
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    currentIntervalMinutes: Int,
    onIntervalSelected: (Int) -> Unit,
    // [Fix Auto-Sort ON/OFF, 2026-08-21] Master switch scheduler background.
    autoSortEnabled: Boolean,
    onAutoSortEnabledChanged: (Boolean) -> Unit,
    currentConflictStrategy: ConflictStrategy,
    onConflictStrategySelected: (ConflictStrategy) -> Unit,
    currentScanConcurrency: Int,
    onScanConcurrencySelected: (Int) -> Unit,
    onExportRequested: suspend () -> String,
    onImportRequested: (String, (Boolean, Int) -> Unit) -> Unit,
    safTreeUri: String?,
    safAccessLost: Boolean,
    onPickSafFolder: () -> Unit,
    onClearSafFolder: () -> Unit,
    // [Fitur baru 2026-08-18, "selamatkan uninstall"] Tawaran restore
    // config lama (rule/log/riwayat) terdeteksi di folder tujuan kustom
    // yang baru dipilih -- lihat FileSorter.peekVaultBackup/MainViewModel.
    vaultRestoreOffer: MainViewModel.VaultRestoreOfferUi?,
    onConfirmVaultRestore: () -> Unit,
    onDismissVaultRestore: () -> Unit,
    // [Fitur baru 2026-08-17, integrasi Shizuku]
    shizukuStatus: ShizukuManager.Status,
    shizukuDestPath: String?,
    useShizuku: Boolean,
    onUseShizukuChanged: (Boolean) -> Unit,
    onShizukuDestPathChanged: (String) -> Unit,
    onRequestShizukuPermission: () -> Unit,
    onRefreshShizukuStatus: () -> Unit,
    // [Fitur baru, batch "Panduan User Baru" 2026-08-17] Entry point kedua ke
    // PanduanScreen (yang pertama ada di grouped menu Home) -- ditaruh di
    // sini juga krn Pengaturan adalah layar yang paling sering dibuka user
    // saat setup awal (SAF/Shizuku/konflik/interval), konteks paling wajar
    // utk menawarkan "baca panduan lengkap" tanpa harus balik ke Home dulu.
    onOpenPanduan: () -> Unit,
    // [Fitur baru 2026-08-19, Release Downloader Spec] In-app updater.
    updateCheckState: UpdateCheckResult?,
    downloadState: DownloadState,
    onCheckForUpdate: () -> Unit,
    onDismissUpdateCheck: () -> Unit,
    onDownloadUpdate: (GithubAssetDto) -> Unit,
    onInstallUpdate: (String) -> Unit,
    // [Fitur baru 2026-08-20] PAT GitHub opsional -- naikkan rate-limit
    // REST API 60/jam->5000/jam, TIDAK wajib diisi (repo publik).
    githubToken: String?,
    onGithubTokenChanged: (String) -> Unit,
    onClearGithubToken: () -> Unit,
    onBack: () -> Unit
) {
    var exportedText by remember { mutableStateOf<String?>(null) }
    var importText by remember { mutableStateOf("") }
    // [Fix audit P2 #UI-13, 2026-08-15] Sebelumnya String? polos ("$count rule
    // berhasil diimpor.") -- tidak ada perbedaan visual sukses/kosong/gagal.
    // Sekarang sealed state eksplisit + warna berbeda per kondisi.
    var importResult by remember { mutableStateOf<ImportResultUiState?>(null) }
    val scope = rememberCoroutineScope()
    val colors = MaterialTheme.colorScheme
    val uriHandler = LocalUriHandler.current
    // [Fix audit P2 #UI-12, 2026-08-15] Dipakai tombol "Salin JSON" export --
    // pola identik dgn tombol "Salin Log" di ActivityLogScreen (Insiden #6).
    val clipboardManager: ClipboardManager = LocalClipboardManager.current
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    // [Fitur baru 2026-08-18, "selamatkan uninstall"] Ditampilkan SEGERA
    // setelah user memilih folder tujuan kustom yang TERNYATA sudah pernah
    // dipakai app ini sebelumnya (root vault "PromptVault" + cermin backup
    // config masih ada di dalamnya) -- skenario utama: install ulang app.
    // Reuse VaultActionSheet yang sudah ada (pola konfirmasi standar app
    // ini), BUKAN AlertDialog baru.
    vaultRestoreOffer?.let { offer ->
        val fmt = remember { SimpleDateFormat("dd MMM yyyy HH:mm", Locale("id", "ID")) }
        VaultActionSheet(
            title = stringResource(R.string.settings_restore_title),
            message = stringResource(
                R.string.settings_restore_message,
                offer.rootFolderLabel,
                fmt.format(Date(offer.savedAtEpochMillis)),
                offer.ruleCount,
                offer.logCount,
                offer.historyCount
            ),
            confirmLabel = stringResource(R.string.settings_restore_confirm),
            dismissLabel = stringResource(R.string.settings_restore_dismiss),
            onConfirm = onConfirmVaultRestore,
            onDismiss = onDismissVaultRestore
        )
    }

    @Composable
    fun chipColors(dangerAccent: Boolean = false) = FilterChipDefaults.filterChipColors(
        selectedContainerColor = if (dangerAccent) colors.error else colors.primary,
        selectedLabelColor = if (dangerAccent) colors.onError else colors.onPrimary
    )

    Scaffold(
        topBar = { VaultTopBar(title = stringResource(R.string.settings_title), onBack = onBack) },
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                Snackbar(snackbarData = data, containerColor = colors.primary, contentColor = colors.onPrimary)
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // [Fitur baru, batch "Panduan User Baru" 2026-08-17] Lihat komentar
            // param onOpenPanduan di atas -- kartu ini SENGAJA ditaruh paling
            // atas (sebelum kartu pengaturan teknis apapun) supaya user yang
            // bingung soal makna kartu-kartu di bawah (SAF/Shizuku/konflik)
            // langsung lihat jalan ke penjelasan lengkap duluan, bukan setelah
            // scroll melewati semuanya.
            OutlinedButton(
                onClick = onOpenPanduan,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.tertiary),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Filled.HelpOutline, contentDescription = null, modifier = Modifier.size(18.dp))
                Text(" " + stringResource(R.string.settings_open_guide))
            }

            // [Fix Auto-Sort ON/OFF, 2026-08-21] Master switch, ditaruh
            // SEBELUM interval (interval tetap bisa diubah saat OFF, tapi
            // tidak akan menjadwalkan apa-apa sampai toggle ini ON lagi).
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    stringResource(R.string.settings_autosort_title),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f).padding(end = 12.dp)
                )
                TactileSwitch(checked = autoSortEnabled, onCheckedChange = onAutoSortEnabledChanged, accentColor = colors.primary)
            }
            Text(
                if (autoSortEnabled) stringResource(R.string.settings_autosort_desc_on) else stringResource(R.string.settings_autosort_desc_off),
                style = MaterialTheme.typography.bodySmall
            )

            Text(stringResource(R.string.settings_interval_section_title), style = MaterialTheme.typography.titleMedium)
            Text(
                stringResource(R.string.settings_interval_section_desc),
                style = MaterialTheme.typography.bodySmall
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SettingsRepository.ALLOWED_INTERVALS.forEach { minutes ->
                    FilterChip(
                        selected = minutes == currentIntervalMinutes,
                        onClick = { onIntervalSelected(minutes) },
                        label = {
                            Text(
                                if (minutes < 60) stringResource(R.string.settings_interval_unit_minutes_fmt, minutes)
                                else stringResource(R.string.settings_interval_unit_hours_fmt, minutes / 60)
                            )
                        },
                        colors = chipColors()
                    )
                }
            }

            // v8.0.0 -- Toggle tema (2 preset kustom) DIHAPUS TOTAL, seksi
            // "Tema" ikut dihapus dari Pengaturan -- app sekarang SATU
            // ColorScheme Material 3 murni, tidak ada lagi yang dipilih user
            // di sini (lihat Theme.kt/Color.kt).

            Text(stringResource(R.string.settings_conflict_section_title), style = MaterialTheme.typography.titleMedium)
            Text(
                stringResource(R.string.settings_conflict_section_desc),
                style = MaterialTheme.typography.bodySmall
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val conflictLabels = mapOf(
                    ConflictStrategy.RENAME to stringResource(R.string.settings_conflict_rename),
                    ConflictStrategy.SKIP to stringResource(R.string.settings_conflict_skip),
                    ConflictStrategy.OVERWRITE to stringResource(R.string.settings_conflict_overwrite)
                )
                conflictLabels.forEach { (strategy, label) ->
                    FilterChip(
                        selected = strategy == currentConflictStrategy,
                        onClick = { onConflictStrategySelected(strategy) },
                        label = { Text(label) },
                        colors = chipColors(dangerAccent = strategy == ConflictStrategy.OVERWRITE)
                    )
                }
            }
            if (currentConflictStrategy == ConflictStrategy.OVERWRITE) {
                Text(
                    stringResource(R.string.settings_conflict_overwrite_warning),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.error
                )
            }

            // [Technical debt #4, dieksekusi 2026-08-13] Dulu SCAN_CONCURRENCY
            // hardcode 6 di FileSorter.kt, tidak bisa diubah user. Sekarang
            // configurable di sini -- default tetap 6 (SettingsRepository.
            // DEFAULT_SCAN_CONCURRENCY), jadi user yang tidak pernah membuka
            // kartu ini tidak terdampak sama sekali.
            Text(stringResource(R.string.settings_concurrency_section_title), style = MaterialTheme.typography.titleMedium)
            Text(
                stringResource(R.string.settings_concurrency_section_desc),
                style = MaterialTheme.typography.bodySmall
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SettingsRepository.ALLOWED_SCAN_CONCURRENCY.forEach { value ->
                    FilterChip(
                        selected = value == currentScanConcurrency,
                        onClick = { onScanConcurrencySelected(value) },
                        label = {
                            Text(
                                if (value == SettingsRepository.DEFAULT_SCAN_CONCURRENCY) stringResource(R.string.settings_concurrency_default_fmt, value)
                                else stringResource(R.string.settings_concurrency_value_fmt, value)
                            )
                        },
                        colors = chipColors()
                    )
                }
            }

            VaultCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.settings_saf_section_title), style = MaterialTheme.typography.titleMedium)
                    Text(
                        stringResource(R.string.settings_saf_section_desc),
                        style = MaterialTheme.typography.bodySmall
                    )
                    if (safTreeUri != null) {
                        Text(
                            stringResource(R.string.settings_saf_active_folder_fmt, friendlySafFolderLabel(safTreeUri)),
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.primary
                        )
                        // [Baru 2026-08-17 v2, temuan user saat diskusi duplikat
                        // root] Kalau folder yang dipilih adalah "Documents" ITU
                        // SENDIRI (bukan subfolder di dalamnya) -- path fisiknya
                        // SAMA PERSIS dengan "Documents/PromptVault/logs/" yang
                        // dipakai CrashLogger.kt (lewat MediaStore, subsistem
                        // BEDA dari SAF). Dua subsistem storage berbeda menulis
                        // ke folder bernama sama di lokasi sama -> potensi
                        // staleness silang tambahan. resolveCanonicalRootDirSaf
                        // di FileSorter SUDAH menangani (konvergen otomatis kalau
                        // sampai terjadi duplikat) -- ini cuma info pencegahan
                        // supaya user TAHU opsinya, bukan blocking/error.
                        if (isSafRootDocumentsFolder(safTreeUri)) {
                            Text(
                                stringResource(R.string.settings_saf_documents_warning),
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.tertiary
                            )
                        }
                        // [fix audit P0 #1/#2, 2026-08-12] Sebelumnya akses hilang
                        // cuma ketahuan diam-diam lewat Log setelah scan gagal --
                        // sekarang ditampilkan LANGSUNG di kartu ini (dicek reaktif
                        // di MainViewModel setiap URI berubah, termasuk saat app
                        // baru dibuka) supaya user tahu SEBELUM scan berikutnya.
                        if (safAccessLost) {
                            Text(
                                stringResource(R.string.settings_saf_access_lost_warning),
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.error
                            )
                        }
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = onPickSafFolder,
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.primary)
                            ) { Text(stringResource(R.string.settings_saf_change_folder)) }
                            OutlinedButton(
                                onClick = onClearSafFolder,
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.error)
                            ) { Text(stringResource(R.string.settings_saf_reset_folder)) }
                        }
                    } else {
                        OutlinedButton(
                            onClick = onPickSafFolder,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.primary)
                        ) { Text(stringResource(R.string.settings_saf_pick_folder)) }
                    }
                }
            }

            // [Fitur baru 2026-08-17, integrasi Shizuku -- permintaan eksplisit
            // user] Alternatif folder tujuan kustom yang bypass SAF/Scoped
            // Storage sepenuhnya lewat proses privileged Shizuku (lihat
            // shizuku/ShizukuManager.kt & FileSorter.scanAndSortViaShizuku).
            // SALING EKSKLUSIF dengan kartu SAF di atas -- kalau `useShizuku`
            // aktif, FileSorter TIDAK menyentuh cabang SAF sama sekali.
            VaultCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            stringResource(R.string.settings_shizuku_section_title),
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.weight(1f).padding(end = 12.dp)
                        )
                        TactileSwitch(checked = useShizuku, onCheckedChange = onUseShizukuChanged, accentColor = colors.primary)
                    }
                    Text(
                        stringResource(R.string.settings_shizuku_section_desc),
                        style = MaterialTheme.typography.bodySmall
                    )

                    val (statusLabel, statusColor) = when (shizukuStatus) {
                        ShizukuManager.Status.READY -> stringResource(R.string.settings_shizuku_status_ready) to colors.primary
                        ShizukuManager.Status.BINDING -> stringResource(R.string.settings_shizuku_status_binding) to colors.tertiary
                        ShizukuManager.Status.PERMISSION_DENIED -> stringResource(R.string.settings_shizuku_status_permission_denied) to colors.error
                        ShizukuManager.Status.NOT_RUNNING -> stringResource(R.string.settings_shizuku_status_not_running) to colors.error
                        ShizukuManager.Status.NOT_INSTALLED -> stringResource(R.string.settings_shizuku_status_not_installed) to colors.onSurfaceVariant
                        ShizukuManager.Status.ERROR -> stringResource(R.string.settings_shizuku_status_error) to colors.error
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(stringResource(R.string.settings_shizuku_status_fmt, statusLabel), style = MaterialTheme.typography.bodySmall, color = statusColor)
                    }
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (shizukuStatus != ShizukuManager.Status.READY) {
                            OutlinedButton(
                                onClick = onRequestShizukuPermission,
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.primary)
                            ) { Text(stringResource(R.string.settings_shizuku_request_permission)) }
                        }
                        OutlinedButton(
                            onClick = onRefreshShizukuStatus,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.onSurfaceVariant)
                        ) { Text(stringResource(R.string.settings_shizuku_recheck_status)) }
                    }

                    if (useShizuku) {
                        // [Fitur baru 2026-08-17, permintaan eksplisit user --
                        // "berikan warning sejelas-jelasnya"] Sama seperti
                        // kartu SAF di atas: SATU peringatan yang TIDAK BOLEH
                        // terlewat -- root folder Shizuku juga TIDAK PERNAH
                        // dibuat otomatis (lihat FileSorter.scanAndSortViaShizuku,
                        // yang MENOLAK scan kalau root belum ada, bukan
                        // membuatnya diam-diam).
                        WarningBanner(stringResource(R.string.settings_shizuku_path_warning))
                        var pathText by remember(shizukuDestPath) { mutableStateOf(shizukuDestPath.orEmpty()) }
                        OutlinedTextField(
                            value = pathText,
                            onValueChange = { pathText = it },
                            label = { Text(stringResource(R.string.settings_shizuku_path_label)) },
                            placeholder = { Text(stringResource(R.string.settings_shizuku_path_placeholder)) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedButton(
                            onClick = { onShizukuDestPathChanged(pathText) },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.primary)
                        ) { Text(stringResource(R.string.settings_shizuku_save_path)) }
                    }
                }
            }

            VaultCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.settings_backup_section_title), style = MaterialTheme.typography.titleMedium)
                    Text(
                        stringResource(R.string.settings_backup_section_desc),
                        style = MaterialTheme.typography.bodySmall
                    )
                    OutlinedButton(
                        onClick = { scope.launch { exportedText = onExportRequested() } },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.primary)
                    ) {
                        Text(stringResource(R.string.settings_backup_show_export))
                    }

                    exportedText?.let { text ->
                        OutlinedTextField(
                            value = text,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(stringResource(R.string.settings_backup_export_field_label)) },
                            modifier = Modifier.fillMaxWidth()
                        )
                        // [Fix audit P2 #UI-12, 2026-08-15] Sebelumnya user harus
                        // long-press + select manual di field read-only -- rendah
                        // discoverability utk aksi UTAMA fitur backup. Field
                        // read-only TETAP ada sbg preview (tidak dihapus).
                        OutlinedButton(
                            onClick = {
                                clipboardManager.setText(AnnotatedString(text))
                                val copiedMsg = context.getString(R.string.settings_backup_copied_snackbar)
                                scope.launch { snackbarHostState.showSnackbar(copiedMsg) }
                            },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.primary)
                        ) {
                            Icon(Icons.Filled.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                            Text(" " + stringResource(R.string.settings_backup_copy_json))
                        }
                    }
                }
            }

            VaultCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.settings_import_section_title), style = MaterialTheme.typography.titleMedium)
                    Text(
                        stringResource(R.string.settings_import_section_desc),
                        style = MaterialTheme.typography.bodySmall
                    )
                    OutlinedTextField(
                        value = importText,
                        onValueChange = { importText = it },
                        label = { Text(stringResource(R.string.settings_import_field_label)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    val importInvalidMsg = stringResource(R.string.settings_import_invalid)
                    val importEmptyMsg = stringResource(R.string.settings_import_empty)
                    Button(
                        onClick = {
                            onImportRequested(importText) { parseSuccess, count ->
                                importResult = when {
                                    !parseSuccess -> ImportResultUiState.Error(importInvalidMsg)
                                    count == 0 -> ImportResultUiState.Warning(importEmptyMsg)
                                    else -> ImportResultUiState.Success(
                                        context.getString(R.string.settings_import_success_fmt, count)
                                    )
                                }
                                // Kosongkan field HANYA kalau parse berhasil (biar user
                                // bisa perbaiki teks yang salah tanpa ngetik ulang dari nol).
                                if (parseSuccess) importText = ""
                            }
                        },
                        enabled = importText.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = colors.primary, contentColor = colors.onPrimary)
                    ) { Text(stringResource(R.string.settings_import_button)) }
                    importResult?.let { result ->
                        Text(
                            result.message,
                            style = MaterialTheme.typography.bodySmall,
                            color = when (result) {
                                is ImportResultUiState.Success -> colors.primary
                                is ImportResultUiState.Warning -> colors.tertiary
                                is ImportResultUiState.Error -> colors.error
                            }
                        )
                    }
                }
            }

            // [Fitur baru 2026-08-19, Release Downloader Spec] Kartu updater --
            // cek rilis terbaru GitHub + download APK streaming + trigger
            // instal, lihat UpdateRepository.kt utk implementasi lengkap
            // sesuai spec (Okio sink, timeout 15s/20s, followRedirects, header
            // Authorization/Accept). Ditaruh PALING BAWAH (bukan urusan
            // rutin sehari-hari spt interval/konflik di atas).
            VaultCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Filled.SystemUpdate, contentDescription = null, tint = colors.primary)
                        Text(stringResource(R.string.settings_update_section_title), style = MaterialTheme.typography.titleMedium)
                    }
                    Text(
                        stringResource(R.string.settings_update_section_desc),
                        style = MaterialTheme.typography.bodySmall
                    )

                    // [Fitur baru 2026-08-20] UI input PAT GitHub -- titik ekstensi
                    // yang sudah disiapkan sejak v8.5.0 (UpdateRepository.checkLatestRelease/
                    // downloadApk sudah terima parameter githubToken: String? = null,
                    // 0 baris diubah di UpdateRepository.kt batch ini). Opsional murni --
                    // repo publik, rate-limit default 60/jam cukup utk pemakaian normal.
                    var tokenInput by remember(githubToken) { mutableStateOf(githubToken.orEmpty()) }
                    var tokenVisible by remember { mutableStateOf(false) }
                    OutlinedTextField(
                        value = tokenInput,
                        onValueChange = { tokenInput = it },
                        label = { Text(stringResource(R.string.settings_update_token_label)) },
                        placeholder = { Text(stringResource(R.string.settings_update_token_placeholder)) },
                        singleLine = true,
                        visualTransformation = if (tokenVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        trailingIcon = {
                            IconButton(onClick = { tokenVisible = !tokenVisible }) {
                                Icon(Icons.Filled.VpnKey, contentDescription = null, tint = colors.onSurfaceVariant)
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        stringResource(R.string.settings_update_token_hint),
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.onSurfaceVariant
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = { onGithubTokenChanged(tokenInput) },
                            enabled = tokenInput.isNotBlank() && tokenInput != githubToken.orEmpty(),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.primary),
                            modifier = Modifier.weight(1f)
                        ) { Text(stringResource(R.string.action_save)) }
                        if (!githubToken.isNullOrBlank()) {
                            OutlinedButton(
                                onClick = { tokenInput = ""; onClearGithubToken() },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.error),
                                modifier = Modifier.weight(1f)
                            ) { Text(stringResource(R.string.action_delete)) }
                        }
                    }

                    when (val state = updateCheckState) {
                        null -> {
                            OutlinedButton(
                                onClick = onCheckForUpdate,
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.primary),
                                modifier = Modifier.fillMaxWidth()
                            ) { Text(stringResource(R.string.settings_update_check_button)) }
                        }
                        is UpdateCheckResult.Checking -> {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                Text(stringResource(R.string.settings_update_checking), style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        is UpdateCheckResult.UpToDate -> {
                            Text(
                                stringResource(R.string.settings_update_uptodate_fmt, state.currentVersion),
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.primary
                            )
                            OutlinedButton(onClick = onDismissUpdateCheck, modifier = Modifier.fillMaxWidth()) {
                                Text(stringResource(R.string.settings_update_close))
                            }
                        }
                        is UpdateCheckResult.NoApkAsset -> {
                            Text(
                                stringResource(R.string.settings_update_no_apk_fmt, state.latestVersion),
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.tertiary
                            )
                            OutlinedButton(onClick = onDismissUpdateCheck, modifier = Modifier.fillMaxWidth()) {
                                Text(stringResource(R.string.settings_update_close))
                            }
                        }
                        is UpdateCheckResult.Error -> {
                            Text(state.message, style = MaterialTheme.typography.bodySmall, color = colors.error)
                            OutlinedButton(onClick = onCheckForUpdate, modifier = Modifier.fillMaxWidth()) {
                                Text(stringResource(R.string.settings_update_retry))
                            }
                        }
                        is UpdateCheckResult.UpdateAvailable -> {
                            Text(
                                stringResource(R.string.settings_update_available_fmt, state.latestVersion, state.currentVersion),
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.primary
                            )
                            // Instruksi user 2026-08-21: jangan cuma banding
                            // nomor versi -- tampilkan potongan `body` rilis
                            // GitHub (`releaseNotes`, sudah ada di model sejak
                            // awal tapi belum pernah dirender). Dibatasi
                            // maxLines=4 (release notes markdown mentah bisa
                            // panjang) + tautan buka rilis lengkap di browser.
                            if (state.releaseNotes.isNotBlank()) {
                                Text(
                                    state.releaseNotes.trim(),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.onSurfaceVariant,
                                    maxLines = 4,
                                    overflow = TextOverflow.Ellipsis
                                )
                                TextButton(onClick = { uriHandler.openUri(state.releaseUrl) }) {
                                    Text(stringResource(R.string.settings_update_view_full_notes))
                                }
                            }
                            when (val dl = downloadState) {
                                is DownloadState.Idle -> {
                                    Button(
                                        onClick = { onDownloadUpdate(state.asset) },
                                        colors = ButtonDefaults.buttonColors(containerColor = colors.primary, contentColor = colors.onPrimary),
                                        modifier = Modifier.fillMaxWidth()
                                    ) { Text(stringResource(R.string.settings_update_download_install)) }
                                }
                                is DownloadState.Downloading -> {
                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        if (dl.percent in 0..100) {
                                            LinearProgressIndicator(
                                                progress = { dl.percent / 100f },
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                            Text(stringResource(R.string.settings_update_progress_percent_fmt, dl.percent, dl.bytesRead / 1024), style = MaterialTheme.typography.bodySmall)
                                        } else {
                                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                                            Text(stringResource(R.string.settings_update_progress_bytes_fmt, dl.bytesRead / 1024), style = MaterialTheme.typography.bodySmall)
                                        }
                                    }
                                }
                                is DownloadState.Completed -> {
                                    Button(
                                        onClick = { onInstallUpdate(dl.filePath) },
                                        colors = ButtonDefaults.buttonColors(containerColor = colors.primary, contentColor = colors.onPrimary),
                                        modifier = Modifier.fillMaxWidth()
                                    ) { Text(stringResource(R.string.settings_update_install_now)) }
                                }
                                is DownloadState.Failed -> {
                                    Text(dl.message, style = MaterialTheme.typography.bodySmall, color = colors.error)
                                    OutlinedButton(
                                        onClick = { onDownloadUpdate(state.asset) },
                                        modifier = Modifier.fillMaxWidth()
                                    ) { Text(stringResource(R.string.settings_update_retry_download)) }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Status hasil import rule, dipisah biar warna/pesan tidak ambigu (fix #UI-13). */
private sealed class ImportResultUiState(val message: String) {
    class Success(message: String) : ImportResultUiState(message)
    class Warning(message: String) : ImportResultUiState(message)
    class Error(message: String) : ImportResultUiState(message)
}

/**
 * [SAF] URI tree SAF berbentuk mis. "content://.../tree/primary%3ADownload%2FFoo"
 * -- tidak informatif ditampilkan mentah ke user non-teknis. Fungsi murni &
 * private -- kalau decode gagal/format tidak dikenali, tampilkan hasil decode
 * apa adanya daripada crash layar Pengaturan.
 *
 * [Fix audit P2 #UI-11, 2026-08-15] Versi LAMA cuma ambil bagian setelah ':'
 * TERAKHIR di seluruh string -- root/provider (mis. "primary" vs id kartu SD
 * lain) hilang total dari label. Dua folder di ROOT/PROVIDER BERBEDA tapi
 * path akhir sama (mis. "Download/Foo" di penyimpanan internal DAN di kartu
 * SD) akan tampil IDENTIK, ambigu bagi user yang punya lebih dari satu
 * penyimpanan. Sekarang: ambil segmen SETELAH "/tree/" dulu (root:path tetap
 * satu kesatuan, tidak ikut ':' dalam skema "content://"), root & path
 * relatif SAMA-SAMA ditampilkan (path duluan, root dlm kurung) -- tetap
 * ringkas tapi tidak lagi kehilangan konteks.
 */
private fun friendlySafFolderLabel(treeUri: String): String {
    val decoded = runCatching { Uri.decode(treeUri) }.getOrDefault(treeUri)
    // Provider tree URI standar berakhir dgn segmen "root:path/relatif" tepat
    // setelah "/tree/" (mis. ".../tree/primary:Download/Foo", atau
    // ".../tree/1234-5678:Download/Foo" utk kartu SD). Ambil segmen SETELAH
    // "/tree/" biar root:path tetap satu kesatuan, bukan cuma cari ':' di
    // seluruh string (yang juga match ':' dalam skema "content://").
    val treeSegment = decoded.substringAfterLast("/tree/", missingDelimiterValue = decoded)
        .substringBefore("/document/")
    val colonIndex = treeSegment.indexOf(':')
    if (colonIndex < 0) return treeSegment.ifBlank { decoded }
    val root = treeSegment.substring(0, colonIndex).ifBlank { "?" }
    val path = treeSegment.substring(colonIndex + 1).ifBlank { "/" }
    return "$path ($root)"
}

/**
 * [Baru 2026-08-17 v2] `true` kalau [treeUri] menunjuk PERSIS ke folder
 * "Documents" di storage utama (path relatif == "Documents", root == "primary"),
 * BUKAN subfolder di dalamnya. Dipakai [SettingsScreen] utk info non-blocking
 * soal folder ini jg dipakai CrashLogger.kt -- lihat KDoc di titik pemanggilan.
 * Parsing sengaja reuse pola yang sama dengan [friendlySafFolderLabel] (bukan
 * fungsi baru dari nol) supaya konsisten kalau format URI provider berubah.
 */
private fun isSafRootDocumentsFolder(treeUri: String): Boolean {
    val decoded = runCatching { Uri.decode(treeUri) }.getOrDefault(treeUri)
    val treeSegment = decoded.substringAfterLast("/tree/", missingDelimiterValue = decoded)
        .substringBefore("/document/")
    val colonIndex = treeSegment.indexOf(':')
    if (colonIndex < 0) return false
    val root = treeSegment.substring(0, colonIndex)
    val path = treeSegment.substring(colonIndex + 1).trim('/')
    return root == "primary" && path.equals("Documents", ignoreCase = true)
}
