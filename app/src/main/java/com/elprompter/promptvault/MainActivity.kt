package com.elprompter.promptvault

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.elprompter.promptvault.data.promptVaultDataStore
import com.elprompter.promptvault.ui.MainViewModel
import com.elprompter.promptvault.ui.Routes
import com.elprompter.promptvault.ui.screens.ActivityLogScreen
import com.elprompter.promptvault.ui.screens.AddEditRuleScreen
import com.elprompter.promptvault.ui.screens.DiagnosticsScreen
import com.elprompter.promptvault.ui.screens.HomeScreen
import com.elprompter.promptvault.ui.screens.OnboardingScreen
import com.elprompter.promptvault.ui.screens.PanduanScreen
import com.elprompter.promptvault.ui.screens.RuleListScreen
import com.elprompter.promptvault.ui.screens.SettingsScreen
import com.elprompter.promptvault.ui.screens.SkippedFilesScreen
import com.elprompter.promptvault.ui.theme.AppBackground
import com.elprompter.promptvault.ui.theme.PromptVaultTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File

private val onboardingDoneKey = booleanPreferencesKey("onboarding_done")
private val notificationPermissionAskedKey = booleanPreferencesKey("notification_permission_asked")

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    // Hasil izin (granted/denied) tidak dipakai langsung -- state permission
    // sebenarnya selalu dibaca ulang lewat hasManageStoragePermission() lewat
    // trigger ini, supaya satu sumber kebenaran (menghindari state ganda yang
    // bisa tidak sinkron).
    private var legacyPermissionRecheckTrigger by mutableIntStateOf(0)

    private val legacyStoragePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ -> legacyPermissionRecheckTrigger++ }

    // [SAF, syarat (c) Insiden #7] Picker folder kustom -- SAF, jadi TIDAK
    // butuh permission runtime apa pun (beda dengan legacyStoragePermissionLauncher
    // di atas). `uri == null` berarti user membatalkan dialog picker, aman diabaikan.
    private val safTreePickerLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri -> uri?.let { viewModel.setSafTreeUri(it) } }

    /**
     * [Fix audit permission edge-case, 2026-08-16] `POST_NOTIFICATIONS`
     * SUDAH dideklarasikan di `AndroidManifest.xml` sejak Batch §5 (untuk
     * notifikasi ongoing `AutoSortWorker` via `setForeground()`, lihat
     * `AutoSortNotification.kt`) TAPI TIDAK PERNAH diminta secara runtime --
     * di Android 13+ (API 33, `targetSdk = 34` app ini SUDAH di atas ambang
     * itu), deklarasi manifest SAJA TIDAK CUKUP, izin tetap "belum
     * diberikan" sampai diminta eksplisit lewat launcher spt ini. Tanpa
     * fix ini, notifikasi "Auto-sort sedang berjalan" (justru tujuan UTAMA
     * Batch §5 -- kasih user visibility scan latar belakang) kemungkinan
     * TIDAK PERNAH tampil di HP Android 13+ manapun, padahal foreground
     * service-nya sendiri tetap jalan diam-diam. Hasil (granted/denied)
     * SENGAJA diabaikan (`{ _ -> }`) -- ini fitur pelengkap (visibility),
     * BUKAN gate wajib spt izin storage; user yang menolak tidak boleh
     * "dipaksa" lewat dialog berulang (lihat flag one-shot di
     * `notificationPermissionAskedKey`, diminta MAKSIMAL sekali seumur
     * instalasi, bukan tiap buka app).
     */
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Splash brand-in AMOLED sebelum konten Compose siap -- kesan pertama
        // yang konsisten, bukan layar putih kosong khas app "belum jadi".
        installSplashScreen()
        super.onCreate(savedInstanceState)

        // v3.0.0: dark mode adalah satu-satunya mode aplikasi (lihat
        // PromptVaultTheme) -- status bar & nav bar SELALU pakai scrim gelap
        // (ikon terang), bukan lagi SystemBarStyle.auto yang ikut terang di
        // sistem terang. Ini mencegah chrome sistem "bocor" jadi terang saat
        // konten di baliknya sudah pasti AMOLED gelap.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(AppBackground.toArgb()),
            navigationBarStyle = SystemBarStyle.dark(AppBackground.toArgb())
        )

        setContent {
            // v2.16.0 -- ThemeMode (Terang/Sistem/Gelap) dihapus total.
            // v8.0.0 -- toggle preset ganda `useAltTheme` (v7.1.0) DIHAPUS
            // TOTAL (rombak tema ke Material 3 murni, lihat Theme.kt) --
            // status/nav bar sekarang statis `AppBackground`, tidak perlu
            // `SideEffect` reaktif lagi (cuma 1 warna, tidak pernah berubah).
            // v8.23.2 -- `themeStyle` dikoleksi di sini (Glassmorphism/
            // Neumorphism, DataStore via MainViewModel) & diteruskan ke
            // PromptVaultTheme supaya CompositionLocal (LocalThemeStyle,
            // Theme.kt) tersedia utk seluruh subtree, termasuk TactileSurface.
            val themeStyle by viewModel.themeStyle.collectAsStateWithLifecycle()
            PromptVaultTheme(themeStyle = themeStyle) {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    PromptVaultRoot(
                        viewModel = viewModel,
                        legacyPermissionRecheckTrigger = legacyPermissionRecheckTrigger,
                        onRequestLegacyStoragePermission = {
                            legacyStoragePermissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.READ_EXTERNAL_STORAGE,
                                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                                )
                            )
                        },
                        onPickSafFolder = { safTreePickerLauncher.launch(null) },
                        onRequestNotificationPermission = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun PromptVaultRoot(
    viewModel: MainViewModel,
    legacyPermissionRecheckTrigger: Int,
    onRequestLegacyStoragePermission: () -> Unit,
    onPickSafFolder: () -> Unit,
    onRequestNotificationPermission: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val navController = rememberNavController()
    val scope = rememberCoroutineScope()

    var onboardingDone by remember { mutableStateOf<Boolean?>(null) }
    LaunchedEffect(Unit) {
        val prefs = context.promptVaultDataStore.data.first()
        onboardingDone = prefs[onboardingDoneKey] ?: false
    }

    var hasStoragePermission by remember { mutableStateOf(hasManageStoragePermission(context)) }

    // Recheck otomatis: (1) tiap kali user kembali dari dialog izin sistem
    // (API 26-29) lewat legacyPermissionRecheckTrigger, dan (2) tiap kali
    // Activity resume (mis. user balik dari layar "Izin Akses Semua File" di
    // Setelan Android untuk API 30+) -- supaya user tidak harus tekan tombol
    // "cek ulang" manual tiap kali, tapi tombolnya tetap ada sebagai fallback.
    LaunchedEffect(legacyPermissionRecheckTrigger) {
        hasStoragePermission = hasManageStoragePermission(context)
    }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasStoragePermission = hasManageStoragePermission(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    if (onboardingDone == null) return // jeda singkat sebelum splash native selesai, hindari flicker

    if (!hasStoragePermission) {
        PermissionGate(
            onPrimaryAction = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    val intent = Intent(
                        Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                        Uri.parse("package:${context.packageName}")
                    )
                    context.startActivity(intent)
                } else {
                    // API 26-29: minta dialog izin runtime langsung (lebih cepat
                    // & jelas daripada melempar user ke halaman Setelan umum).
                    onRequestLegacyStoragePermission()
                }
            },
            onOpenAppSettings = {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))
                context.startActivity(intent)
            },
            onRecheck = { hasStoragePermission = hasManageStoragePermission(context) },
            showAppSettingsFallback = Build.VERSION.SDK_INT < Build.VERSION_CODES.R
        )
        return
    }

    if (onboardingDone == false) {
        OnboardingScreen(onFinished = {
            scope.launch {
                context.promptVaultDataStore.edit { it[onboardingDoneKey] = true }
                onboardingDone = true
            }
        })
        return
    }

    // [Fix audit permission edge-case, 2026-08-16] Diminta SEKALI di sini
    // (bukan di dalam PermissionGate storage di atas -- POST_NOTIFICATIONS
    // BUKAN gate wajib, app tetap 100% fungsional tanpanya, cuma notifikasi
    // ongoing auto-sort yang tidak tampil). Titik ini tepat setelah user
    // resmi masuk ke app (storage granted + onboarding selesai) -- konteks
    // paling wajar utk Android best-practice "minta izin saat relevan",
    // BUKAN saat AutoSortWorker tiba-tiba jalan di background tanpa UI utk
    // menampilkan dialog sama sekali. Lihat javadoc lengkap di
    // `notificationPermissionLauncher`/`notificationPermissionAskedKey`.
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val alreadyAsked = context.promptVaultDataStore.data.first()[notificationPermissionAskedKey] ?: false
            val alreadyGranted = ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!alreadyAsked && !alreadyGranted) {
                context.promptVaultDataStore.edit { it[notificationPermissionAskedKey] = true }
                onRequestNotificationPermission()
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = Routes.HOME,
        // [Fix layout/inset 2026-08-27] `imePadding()` di 1 titik fondasi ini
        // (bukan disebar ke 9 file layar) -- efek menjalar OTOMATIS ke SEMUA
        // tab/layar tanpa perlu disentuh satu-satu, pola sama persis
        // `GlassPanel`/`TactileSurface` (1 primitif diubah, seluruh app ikut).
        // Root cause sebelumnya: `enableEdgeToEdge()` (MainActivity.onCreate)
        // aktif TANPA `imePadding()` di mana pun & TANPA `windowSoftInputMode`
        // di AndroidManifest.xml -- keyboard menimpa konten alih-alih
        // mendorongnya, field input di bagian bawah form (mis. Tambah/Edit
        // Rule, 5 field) jadi ketutup/tidak kebaca ("truncated") saat diketik.
        // Scaffold+VaultTopBar (status bar) & contentWindowInsets (system
        // bars di body) SUDAH benar di semua 9 layar (diverifikasi grep --
        // semua konsumsi `padding` dari Scaffold), gap SATU-SATUNYA yang
        // ditemukan murni di sisi IME/keyboard.
        modifier = Modifier.imePadding(),
        // Transisi halus antar layar (geser + fade tipis, 220ms) -- sebelumnya
        // navigasi antar layar langsung potong instan tanpa transisi sama
        // sekali, terasa kaku dibanding sisa app yang sudah banyak animasi
        // kecil (press-scale, segmented control). Arah geser mengikuti
        // konvensi umum: masuk dari kanan, keluar (pop/back) ke kanan.
        enterTransition = { fadeIn(tween(220)) + slideInHorizontally(tween(220)) { it / 10 } },
        exitTransition = { fadeOut(tween(180)) },
        popEnterTransition = { fadeIn(tween(220)) },
        popExitTransition = { fadeOut(tween(180)) + slideOutHorizontally(tween(180)) { it / 10 } }
    ) {
        composable(Routes.HOME) {
            val rules by viewModel.rules.collectAsStateWithLifecycle()
            val interval by viewModel.intervalMinutes.collectAsStateWithLifecycle()
            // [Fix Auto-Sort ON/OFF, 2026-08-21]
            val autoSortEnabledHome by viewModel.autoSortEnabled.collectAsStateWithLifecycle()
            val isScanning by viewModel.isScanning.collectAsStateWithLifecycle()
            val summary by viewModel.lastScanSummary.collectAsStateWithLifecycle()
            val skipped by viewModel.lastSkippedFiles.collectAsStateWithLifecycle()
            val scanFeedback by viewModel.scanFeedback.collectAsStateWithLifecycle()
            val homeStats by viewModel.homeStats.collectAsStateWithLifecycle()
            val themeStyleHome by viewModel.themeStyle.collectAsStateWithLifecycle()

            HomeScreen(
                ruleCount = rules.count { it.enabled },
                intervalMinutes = interval,
                autoSortEnabled = autoSortEnabledHome,
                isScanning = isScanning,
                lastScanSummary = summary,
                hasSkippedFiles = skipped.isNotEmpty(),
                homeStats = homeStats,
                scanFeedback = scanFeedback,
                onScanFeedbackConsumed = { viewModel.consumeScanFeedback() },
                onScanNow = { viewModel.runManualScan() },
                onOpenRules = { navController.navigate(Routes.RULES) },
                onOpenLog = { navController.navigate(Routes.ACTIVITY_LOG) },
                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                onOpenDiagnostics = { navController.navigate(Routes.DIAGNOSTICS) },
                onOpenSkippedFiles = { navController.navigate(Routes.SKIPPED_FILES) },
                onOpenPanduan = { navController.navigate(Routes.PANDUAN) },
                onOpenStatistics = { navController.navigate(Routes.STATISTICS) },
                themeStyle = themeStyleHome,
                onSelectThemeStyle = { viewModel.setThemeStyle(it) }
            )
        }
        composable(Routes.STATISTICS) {
            val statisticsData by viewModel.statisticsData.collectAsStateWithLifecycle()
            com.elprompter.promptvault.ui.screens.StatisticsScreen(
                data = statisticsData,
                onBack = { navController.popBackStack() }
            )
        }
        composable(Routes.PANDUAN) {
            PanduanScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.RULES) {
            val rules by viewModel.rules.collectAsStateWithLifecycle()
            var overlapIds by remember { mutableStateOf(setOf<String>()) }
            LaunchedEffect(rules) {
                overlapIds = viewModel.findAllOverlaps().keys.map { it.id }.toSet()
            }
            val ruleSaveFeedback by viewModel.ruleSaveFeedback.collectAsStateWithLifecycle()
            RuleListScreen(
                rules = rules,
                overlappingRuleIds = overlapIds,
                onToggleEnabled = { rule, enabled -> viewModel.saveRule(rule.copy(enabled = enabled)) },
                onMoveUp = { rule -> viewModel.moveRuleUp(rule.id) },
                onMoveDown = { rule -> viewModel.moveRuleDown(rule.id) },
                onEditRule = { rule -> navController.navigate(Routes.addEditRule(rule.id)) },
                onDeleteRule = { rule -> viewModel.deleteRule(rule.id) },
                onAddRule = { navController.navigate(Routes.addEditRule(null)) },
                ruleSaveFeedback = ruleSaveFeedback,
                onRuleSaveFeedbackConsumed = { viewModel.consumeRuleSaveFeedback() },
                onBack = { navController.popBackStack() }
            )
        }
        composable(
            Routes.ADD_EDIT_RULE,
            arguments = listOf(navArgument("ruleId") { type = NavType.StringType; defaultValue = "" })
        ) { backStackEntry ->
            val ruleId = backStackEntry.arguments?.getString("ruleId")?.ifBlank { null }
            val rules by viewModel.rules.collectAsStateWithLifecycle()
            val existing = rules.firstOrNull { it.id == ruleId }
            AddEditRuleScreen(
                existingRule = existing,
                onCheckBeforeSave = { rule -> viewModel.checkBeforeSave(rule) },
                onPreviewPattern = { pattern, excludePattern -> viewModel.previewPattern(pattern, excludePattern) },
                onSave = { rule, removeDuplicateRuleId ->
                    // announce=true: technical debt closure v2.16.0, lihat
                    // MainViewModel.RuleSaveFeedback -- Snackbar konfirmasi
                    // muncul di RuleListScreen setelah pop kembali ke sana.
                    // [Fix bug laporan user 2026-08-27] SEBELUMNYA popBackStack()
                    // dipanggil LANGSUNG tanpa menunggu tulis DataStore selesai --
                    // kalau user buka lagi Edit rule yang sama sebelum tulisan
                    // rampung, toggle (mis. "Tahan versi .zip terbaru") kelihatan
                    // balik OFF krn baca data lama. Sekarang tunggu Job simpan
                    // (saveRuleJob.join()) SEBELUM pindah layar -- lihat KDoc
                    // lengkap di MainViewModel.saveRule().
                    val saveRuleJob = viewModel.saveRule(rule, removeDuplicateRuleId, announce = true)
                    scope.launch {
                        saveRuleJob.join()
                        navController.popBackStack()
                    }
                },
                onCancel = { navController.popBackStack() }
            )
        }
        composable(Routes.ACTIVITY_LOG) {
            val logEntries by viewModel.logEntries.collectAsStateWithLifecycle()
            val history by viewModel.undoableHistory.collectAsStateWithLifecycle()
            ActivityLogScreen(
                logEntries = logEntries,
                undoableHistory = history,
                onUndo = { entry -> viewModel.undoMove(entry) },
                onUndoMultiple = { entries -> viewModel.undoMultiple(entries) },
                onBack = { navController.popBackStack() }
            )
        }
        composable(Routes.SETTINGS) {
            val interval by viewModel.intervalMinutes.collectAsStateWithLifecycle()
            // [Fix Auto-Sort ON/OFF, 2026-08-21]
            val autoSortEnabled by viewModel.autoSortEnabled.collectAsStateWithLifecycle()
            val conflictStrategy by viewModel.conflictStrategy.collectAsStateWithLifecycle()
            val scanConcurrency by viewModel.scanConcurrency.collectAsStateWithLifecycle()
            val safTreeUri by viewModel.safTreeUri.collectAsStateWithLifecycle()
            val safAccessLost by viewModel.safAccessLost.collectAsStateWithLifecycle()
            // [Fitur baru 2026-08-18, "selamatkan uninstall"] lihat KDoc lengkap di MainViewModel.
            val vaultRestoreOffer by viewModel.vaultRestoreOffer.collectAsStateWithLifecycle()
            val shizukuStatus by viewModel.shizukuStatus.collectAsStateWithLifecycle()
            val shizukuDestPath by viewModel.shizukuDestPath.collectAsStateWithLifecycle()
            val useShizuku by viewModel.useShizuku.collectAsStateWithLifecycle()
            // [Fitur baru 2026-08-19, Release Downloader Spec]
            val updateCheckState by viewModel.updateCheckState.collectAsStateWithLifecycle()
            val downloadState by viewModel.downloadState.collectAsStateWithLifecycle()
            // [Fitur baru 2026-08-20] PAT GitHub opsional, lihat KDoc lengkap di MainViewModel/SettingsRepository.
            val githubToken by viewModel.githubToken.collectAsStateWithLifecycle()
            SettingsScreen(
                currentIntervalMinutes = interval,
                onIntervalSelected = { viewModel.setIntervalMinutes(it) },
                autoSortEnabled = autoSortEnabled,
                onAutoSortEnabledChanged = { viewModel.setAutoSortEnabled(it) },
                currentConflictStrategy = conflictStrategy,
                onConflictStrategySelected = { viewModel.setConflictStrategy(it) },
                currentScanConcurrency = scanConcurrency,
                onScanConcurrencySelected = { viewModel.setScanConcurrency(it) },
                onExportRequested = { viewModel.exportRulesJson() },
                onImportRequested = { text, cb -> viewModel.importRulesJson(text, cb) },
                safTreeUri = safTreeUri,
                safAccessLost = safAccessLost,
                onPickSafFolder = onPickSafFolder,
                onClearSafFolder = { viewModel.clearSafTreeUri() },
                vaultRestoreOffer = vaultRestoreOffer,
                onConfirmVaultRestore = { viewModel.confirmVaultRestore() },
                onDismissVaultRestore = { viewModel.dismissVaultRestoreOffer() },
                shizukuStatus = shizukuStatus,
                shizukuDestPath = shizukuDestPath,
                useShizuku = useShizuku,
                onUseShizukuChanged = { viewModel.setUseShizuku(it) },
                onShizukuDestPathChanged = { viewModel.setShizukuDestPath(it) },
                onRequestShizukuPermission = { viewModel.requestShizukuPermission() },
                onRefreshShizukuStatus = { viewModel.refreshShizukuStatus() },
                onOpenPanduan = { navController.navigate(Routes.PANDUAN) },
                updateCheckState = updateCheckState,
                downloadState = downloadState,
                onCheckForUpdate = { viewModel.checkForUpdate() },
                onDismissUpdateCheck = { viewModel.dismissUpdateCheck() },
                onDownloadUpdate = { asset -> viewModel.downloadUpdate(asset) },
                onInstallUpdate = { filePath -> installApk(context, filePath) },
                githubToken = githubToken,
                onGithubTokenChanged = { viewModel.setGithubToken(it) },
                onClearGithubToken = { viewModel.clearGithubToken() },
                onBack = { navController.popBackStack() }
            )
        }
        composable(Routes.DIAGNOSTICS) {
            var fileNames by remember { mutableStateOf<List<String>>(emptyList()) }
            LaunchedEffect(Unit) { fileNames = viewModel.listDownloadsFileNames() }
            DiagnosticsScreen(
                downloadsFileNames = fileNames,
                onBack = { navController.popBackStack() }
            )
        }
        composable(Routes.SKIPPED_FILES) {
            val skipped by viewModel.lastSkippedFiles.collectAsStateWithLifecycle()
            // UI-09 fix: lastScanSummary null == belum pernah scan sekalipun
            // sekali sejak app dibuka -- diteruskan sbg sinyal eksplisit ke
            // SkippedFilesScreen supaya empty state tidak lagi ambigu.
            val summary by viewModel.lastScanSummary.collectAsStateWithLifecycle()
            SkippedFilesScreen(
                skipped = skipped,
                hasScannedBefore = summary != null,
                onBack = { navController.popBackStack() }
            )
        }
    }
}

@Composable
private fun PermissionGate(
    onPrimaryAction: () -> Unit,
    onOpenAppSettings: () -> Unit,
    onRecheck: () -> Unit,
    showAppSettingsFallback: Boolean
) {
    val colors = MaterialTheme.colorScheme
    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(colors.primary, RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.FolderOpen, contentDescription = null, tint = colors.onPrimary, modifier = Modifier.size(32.dp))
            }
            Text(stringResource(R.string.permission_gate_title), style = MaterialTheme.typography.headlineSmall, color = colors.onBackground)
            Text(
                stringResource(R.string.permission_gate_message),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onBackground
            )
            Button(
                onClick = onPrimaryAction,
                colors = ButtonDefaults.buttonColors(containerColor = colors.secondary, contentColor = colors.onSecondary),
                modifier = Modifier.fillMaxWidth()
            ) { Text(stringResource(R.string.permission_gate_open_settings)) }
            OutlinedButton(
                onClick = onRecheck,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.primary),
                modifier = Modifier.fillMaxWidth()
            ) { Text(stringResource(R.string.permission_gate_recheck)) }
            // Fallback khusus API 26-29: kalau user pernah menolak dialog izin
            // dan Android tidak akan menampilkannya lagi otomatis (permanently
            // denied), satu-satunya jalan adalah pengaturan aplikasi manual.
            if (showAppSettingsFallback) {
                OutlinedButton(
                    onClick = onOpenAppSettings,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.onSurfaceVariant),
                    modifier = Modifier.fillMaxWidth()
                ) { Text(stringResource(R.string.permission_gate_denied_fallback)) }
            }
        }
    }
}

/**
 * SDK 30+ (R): dicek lewat MANAGE_EXTERNAL_STORAGE ("All files access").
 * SDK 26-29: TIDAK ADA all-files-access -- app harus benar-benar pakai izin
 * runtime READ/WRITE_EXTERNAL_STORAGE biasa. Sebelum v2.3.7 fungsi ini
 * hardcode `true` untuk seluruh rentang SDK 26-29, jadi layar "Izin
 * Diperlukan" tidak pernah muncul dan operasi file gagal diam-diam di device
 * lawas yang belum pernah memberi izin. Lihat PROJECT_STATE.md.
 */
private fun hasManageStoragePermission(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        Environment.isExternalStorageManager()
    } else {
        val readGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
        val writeGranted = if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            // WRITE_EXTERNAL_STORAGE dideklarasikan maxSdkVersion=28 di manifest
            // (tidak relevan lagi di atas itu); READ saja cukup untuk API 29.
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
        readGranted && writeGranted
    }
}

/**
 * [Fitur baru 2026-08-19, Release Downloader Spec] Picu instalasi APK yang
 * sudah selesai diunduh ke cacheDir/updates/ ([UpdateRepository.downloadApk])
 * lewat dialog installer sistem -- BUKAN instal diam-diam (app pihak ketiga
 * TIDAK PERNAH boleh self-install tanpa konfirmasi user, ini batasan OS,
 * bukan pilihan desain). Reuse FileProvider yang SUDAH dideklarasikan di
 * manifest (authorities `${applicationId}.fileprovider`, sebelumnya
 * disiapkan tapi belum ada pemakai nyata) -- content:// URI wajib sejak
 * Android 7 (scoped file access), file:// URI langsung akan ditolak sistem.
 */
private fun installApk(context: Context, apkFilePath: String) {
    val apkFile = File(apkFilePath)
    val apkUri: Uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", apkFile)
    val installIntent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(apkUri, "application/vnd.android.package-archive")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(installIntent)
}
