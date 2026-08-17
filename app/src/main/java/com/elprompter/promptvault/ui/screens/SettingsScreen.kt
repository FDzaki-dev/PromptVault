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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.elprompter.promptvault.data.ConflictStrategy
import com.elprompter.promptvault.data.SettingsRepository
import com.elprompter.promptvault.shizuku.ShizukuManager
import com.elprompter.promptvault.ui.components.TactileSwitch
import com.elprompter.promptvault.ui.components.VaultCard
import com.elprompter.promptvault.ui.components.VaultTopBar
import com.elprompter.promptvault.ui.components.WarningBanner
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    currentIntervalMinutes: Int,
    onIntervalSelected: (Int) -> Unit,
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
    // [Fix audit P2 #UI-12, 2026-08-15] Dipakai tombol "Salin JSON" export --
    // pola identik dgn tombol "Salin Log" di ActivityLogScreen (Insiden #6).
    val clipboardManager: ClipboardManager = LocalClipboardManager.current
    val snackbarHostState = remember { SnackbarHostState() }

    @Composable
    fun chipColors(dangerAccent: Boolean = false) = FilterChipDefaults.filterChipColors(
        selectedContainerColor = if (dangerAccent) colors.error else colors.primary,
        selectedLabelColor = if (dangerAccent) colors.onError else colors.onPrimary
    )

    Scaffold(
        topBar = { VaultTopBar(title = "Pengaturan", onBack = onBack) },
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
                Text(" Buka Panduan Penggunaan")
            }

            Text("Interval Auto-Scan", style = MaterialTheme.typography.titleMedium)
            Text(
                "Seberapa sering PromptVault memindai Downloads di latar belakang. " +
                    "Android tidak mengizinkan kurang dari 15 menit.",
                style = MaterialTheme.typography.bodySmall
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SettingsRepository.ALLOWED_INTERVALS.forEach { minutes ->
                    FilterChip(
                        selected = minutes == currentIntervalMinutes,
                        onClick = { onIntervalSelected(minutes) },
                        label = { Text(if (minutes < 60) "$minutes menit" else "${minutes / 60} jam") },
                        colors = chipColors()
                    )
                }
            }

            // v8.0.0 -- Toggle tema (2 preset kustom) DIHAPUS TOTAL, seksi
            // "Tema" ikut dihapus dari Pengaturan -- app sekarang SATU
            // ColorScheme Material 3 murni, tidak ada lagi yang dipilih user
            // di sini (lihat Theme.kt/Color.kt).

            Text("Kalau Nama File Sudah Ada di Tujuan", style = MaterialTheme.typography.titleMedium)
            Text(
                "Apa yang dilakukan PromptVault kalau file dengan nama yang sama sudah ada di folder tujuan.",
                style = MaterialTheme.typography.bodySmall
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val conflictLabels = mapOf(
                    ConflictStrategy.RENAME to "Ganti nama otomatis",
                    ConflictStrategy.SKIP to "Lewati",
                    ConflictStrategy.OVERWRITE to "Timpa"
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
                    "Perhatian: file lama di tujuan akan tertimpa permanen dan tidak bisa di-undo.",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.error
                )
            }

            // [Technical debt #4, dieksekusi 2026-08-13] Dulu SCAN_CONCURRENCY
            // hardcode 6 di FileSorter.kt, tidak bisa diubah user. Sekarang
            // configurable di sini -- default tetap 6 (SettingsRepository.
            // DEFAULT_SCAN_CONCURRENCY), jadi user yang tidak pernah membuka
            // kartu ini tidak terdampak sama sekali.
            Text("Kecepatan Scan (Lanjutan)", style = MaterialTheme.typography.titleMedium)
            Text(
                "Jumlah file yang diproses BERSAMAAN saat scan. Lebih tinggi = scan lebih " +
                    "cepat di Downloads berisi banyak file, tapi lebih berat di HP kelas bawah. " +
                    "6 adalah nilai default yang aman untuk kebanyakan HP.",
                style = MaterialTheme.typography.bodySmall
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SettingsRepository.ALLOWED_SCAN_CONCURRENCY.forEach { value ->
                    FilterChip(
                        selected = value == currentScanConcurrency,
                        onClick = { onScanConcurrencySelected(value) },
                        label = { Text(if (value == SettingsRepository.DEFAULT_SCAN_CONCURRENCY) "$value (default)" else "$value") },
                        colors = chipColors()
                    )
                }
            }

            VaultCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Folder Tujuan Kustom (Opsional)", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "File tetap DIPINDAI dari Downloads seperti biasa. Folder ini cuma menentukan " +
                            "KE MANA hasil sortir disimpan (lewat Storage Access Framework) -- cocok " +
                            "untuk folder di kartu SD atau folder khusus lain. Kosongkan lagi untuk " +
                            "kembali menyimpan hasil sortir ke Downloads/PromptVault. App otomatis membuat " +
                            "subfolder \"PromptVault\" di dalam folder yang kamu pilih -- kamu tidak perlu " +
                            "membuatnya manual.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    if (safTreeUri != null) {
                        Text(
                            "Folder aktif: ${friendlySafFolderLabel(safTreeUri)}",
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
                                "ℹ Folder ini juga dipakai buat menyimpan crash log internal app " +
                                    "(di subfolder \"PromptVault/logs\"). Sudah ada penanganan otomatis kalau " +
                                    "sampai bentrok, tapi kalau mau pisah total, pilih subfolder di dalam " +
                                    "Documents (bukan Documents-nya langsung).",
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
                                "⚠ Folder ini sudah tidak bisa diakses (mungkin dihapus/dipindah/izin " +
                                    "dicabut). Scan TIDAK akan fallback diam-diam ke Downloads -- pilih " +
                                    "ulang folder atau kembali ke Downloads.",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.error
                            )
                        }
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = onPickSafFolder,
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.primary)
                            ) { Text("Ganti Folder") }
                            OutlinedButton(
                                onClick = onClearSafFolder,
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.error)
                            ) { Text("Kembali ke Downloads") }
                        }
                    } else {
                        OutlinedButton(
                            onClick = onPickSafFolder,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.primary)
                        ) { Text("Pilih Folder") }
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
                        Text("Mode Shizuku (Lanjutan)", style = MaterialTheme.typography.titleMedium)
                        TactileSwitch(checked = useShizuku, onCheckedChange = onUseShizukuChanged, accentColor = colors.primary)
                    }
                    Text(
                        "Pakai izin privileged dari aplikasi Shizuku (bukan SAF) untuk menulis ke folder " +
                            "tujuan kustom -- termasuk lokasi yang biasanya diblokir Scoped Storage. Butuh " +
                            "aplikasi Shizuku terpasang & jalan (root ATAU wireless debugging/adb) di HP kamu.",
                        style = MaterialTheme.typography.bodySmall
                    )

                    val (statusLabel, statusColor) = when (shizukuStatus) {
                        ShizukuManager.Status.READY -> "Siap digunakan" to colors.primary
                        ShizukuManager.Status.BINDING -> "Menyambungkan..." to colors.tertiary
                        ShizukuManager.Status.PERMISSION_DENIED -> "Izin belum diberikan" to colors.error
                        ShizukuManager.Status.NOT_RUNNING -> "Shizuku belum jalan / belum terpasang" to colors.error
                        ShizukuManager.Status.NOT_INSTALLED -> "Status belum dicek" to colors.onSurfaceVariant
                        ShizukuManager.Status.ERROR -> "Gagal menyambung ke Shizuku" to colors.error
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Status: $statusLabel", style = MaterialTheme.typography.bodySmall, color = statusColor)
                    }
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (shizukuStatus != ShizukuManager.Status.READY) {
                            OutlinedButton(
                                onClick = onRequestShizukuPermission,
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.primary)
                            ) { Text("Minta Izin Shizuku") }
                        }
                        OutlinedButton(
                            onClick = onRefreshShizukuStatus,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.onSurfaceVariant)
                        ) { Text("Cek Ulang Status") }
                    }

                    if (useShizuku) {
                        // [Fitur baru 2026-08-17, permintaan eksplisit user --
                        // "berikan warning sejelas-jelasnya"] Sama seperti
                        // kartu SAF di atas: SATU peringatan yang TIDAK BOLEH
                        // terlewat -- root folder Shizuku juga TIDAK PERNAH
                        // dibuat otomatis (lihat FileSorter.scanAndSortViaShizuku,
                        // yang MENOLAK scan kalau root belum ada, bukan
                        // membuatnya diam-diam).
                        WarningBanner(
                            "Path folder di bawah HARUS SUDAH ADA secara fisik di storage -- aplikasi ini " +
                                "TIDAK PERNAH membuat folder root itu sendiri lewat Shizuku. Buat dulu " +
                                "foldernya sendiri lewat file manager (contoh: /storage/emulated/0/PromptVaultShizuku), " +
                                "baru isi path yang PERSIS SAMA di sini. Kalau folder belum ada, scan akan " +
                                "GAGAL dengan pesan error, bukan membuatkan foldernya."
                        )
                        var pathText by remember(shizukuDestPath) { mutableStateOf(shizukuDestPath.orEmpty()) }
                        OutlinedTextField(
                            value = pathText,
                            onValueChange = { pathText = it },
                            label = { Text("Path absolut folder tujuan") },
                            placeholder = { Text("/storage/emulated/0/NamaFolderKamu") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedButton(
                            onClick = { onShizukuDestPathChanged(pathText) },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.primary)
                        ) { Text("Simpan Path") }
                    }
                }
            }

            VaultCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Backup / Export Rule", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Simpan semua rule kamu sebagai teks, biar bisa dipulihkan lagi kalau app di-uninstall.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    OutlinedButton(
                        onClick = { scope.launch { exportedText = onExportRequested() } },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.primary)
                    ) {
                        Text("Tampilkan JSON Export")
                    }

                    exportedText?.let { text ->
                        OutlinedTextField(
                            value = text,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Salin teks ini untuk backup") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        // [Fix audit P2 #UI-12, 2026-08-15] Sebelumnya user harus
                        // long-press + select manual di field read-only -- rendah
                        // discoverability utk aksi UTAMA fitur backup. Field
                        // read-only TETAP ada sbg preview (tidak dihapus).
                        OutlinedButton(
                            onClick = {
                                clipboardManager.setText(AnnotatedString(text))
                                scope.launch { snackbarHostState.showSnackbar("JSON disalin ke clipboard") }
                            },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.primary)
                        ) {
                            Icon(Icons.Filled.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                            Text(" Salin JSON")
                        }
                    }
                }
            }

            VaultCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Import Rule", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Tempel teks hasil export dari perangkat lain atau backup sebelumnya.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    OutlinedTextField(
                        value = importText,
                        onValueChange = { importText = it },
                        label = { Text("Tempel JSON hasil export di sini") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(
                        onClick = {
                            onImportRequested(importText) { parseSuccess, count ->
                                importResult = when {
                                    !parseSuccess -> ImportResultUiState.Error(
                                        "Format JSON tidak valid -- tidak ada rule yang diimpor."
                                    )
                                    count == 0 -> ImportResultUiState.Warning(
                                        "JSON valid, tapi tidak ada rule di dalamnya untuk diimpor."
                                    )
                                    else -> ImportResultUiState.Success("$count rule berhasil diimpor.")
                                }
                                // Kosongkan field HANYA kalau parse berhasil (biar user
                                // bisa perbaiki teks yang salah tanpa ngetik ulang dari nol).
                                if (parseSuccess) importText = ""
                            }
                        },
                        enabled = importText.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = colors.primary, contentColor = colors.onPrimary)
                    ) { Text("Import") }
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
