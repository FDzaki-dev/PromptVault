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
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
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
import com.elprompter.promptvault.data.ThemeMode
import com.elprompter.promptvault.data.promptVaultDataStore
import com.elprompter.promptvault.ui.MainViewModel
import com.elprompter.promptvault.ui.Routes
import com.elprompter.promptvault.ui.screens.ActivityLogScreen
import com.elprompter.promptvault.ui.screens.AddEditRuleScreen
import com.elprompter.promptvault.ui.screens.DiagnosticsScreen
import com.elprompter.promptvault.ui.screens.HomeScreen
import com.elprompter.promptvault.ui.screens.OnboardingScreen
import com.elprompter.promptvault.ui.screens.RuleListScreen
import com.elprompter.promptvault.ui.screens.SettingsScreen
import com.elprompter.promptvault.ui.screens.SkippedFilesScreen
import com.elprompter.promptvault.ui.screens.ZipSorterScreen
import com.elprompter.promptvault.ui.ZipSorterViewModel
import com.elprompter.promptvault.ui.theme.Kraft
import com.elprompter.promptvault.ui.theme.ObsidianBase
import com.elprompter.promptvault.ui.theme.PromptVaultTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private val onboardingDoneKey = booleanPreferencesKey("onboarding_done")

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()
    private val zipSorterViewModel: ZipSorterViewModel by viewModels()

    // Hasil izin (granted/denied) tidak dipakai langsung -- state permission
    // sebenarnya selalu dibaca ulang lewat hasManageStoragePermission() lewat
    // trigger ini, supaya satu sumber kebenaran (menghindari state ganda yang
    // bisa tidak sinkron).
    private var legacyPermissionRecheckTrigger by mutableIntStateOf(0)

    private val legacyStoragePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ -> legacyPermissionRecheckTrigger++ }

    /**
     * Batch §1 Fase 1/2 (hybrid SAF, opsional, keputusan user 2026-08-05).
     * `OpenDocumentTree()` menampilkan picker folder bawaan sistem. Hasil
     * `uri` bisa null (user batal). Kalau tidak null, WAJIB
     * `takePersistableUriPermission()` di sini SEBELUM disimpan -- tanpa itu
     * izin akses ke tree tsb hilang begitu proses app mati (izin dari
     * OpenDocumentTree defaultnya cuma hidup selama "sesi" intent result,
     * bukan permanen).
     */
    private val safTreePickerLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            try {
                contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
                viewModel.setSafTreeUri(uri.toString())
            } catch (e: SecurityException) {
                // Best-effort: kalau gagal ambil persisted permission (jarang,
                // biasanya storage rusak/OEM aneh), JANGAN simpan URI yang
                // izinnya tidak akan bertahan -- biarkan user tetap di mode
                // Downloads/java.io.File biasa (fallback aman, bukan crash).
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Splash brand-in (Pine) sebelum konten Compose siap -- kesan pertama
        // yang konsisten, bukan layar putih kosong khas app "belum jadi".
        installSplashScreen()
        super.onCreate(savedInstanceState)

        // Edge-to-edge dengan status bar & nav bar otomatis ikut terang/gelap
        // sistem (SystemBarStyle.auto). Kalau user override manual "Selalu
        // Gelap"/"Selalu Terang" di Pengaturan, area konten tetap benar lewat
        // PromptVaultTheme -- cuma chrome status bar yang mungkin tidak 100%
        // sinkron di kasus override manual itu (batasan kecil yang wajar).
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(Kraft.toArgb(), ObsidianBase.toArgb()),
            navigationBarStyle = SystemBarStyle.auto(Kraft.toArgb(), ObsidianBase.toArgb())
        )

        setContent {
            val viewModelForTheme = viewModel
            val themeMode by viewModelForTheme.themeMode.collectAsStateWithLifecycle()
            val systemDark = isSystemInDarkTheme()
            val effectiveDark = when (themeMode) {
                ThemeMode.SYSTEM -> systemDark
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }
            PromptVaultTheme(darkTheme = effectiveDark) {
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
                        onPickSafFolder = { safTreePickerLauncher.launch(null) }
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
    onPickSafFolder: () -> Unit
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

    NavHost(
        navController = navController,
        startDestination = Routes.HOME,
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
            val isScanning by viewModel.isScanning.collectAsStateWithLifecycle()
            val summary by viewModel.lastScanSummary.collectAsStateWithLifecycle()
            val skipped by viewModel.lastSkippedFiles.collectAsStateWithLifecycle()
            val scanFeedback by viewModel.scanFeedback.collectAsStateWithLifecycle()

            HomeScreen(
                ruleCount = rules.count { it.enabled },
                intervalMinutes = interval,
                isScanning = isScanning,
                lastScanSummary = summary,
                hasSkippedFiles = skipped.isNotEmpty(),
                scanFeedback = scanFeedback,
                onScanFeedbackConsumed = { viewModel.consumeScanFeedback() },
                onScanNow = { viewModel.runManualScan() },
                onOpenRules = { navController.navigate(Routes.RULES) },
                onOpenLog = { navController.navigate(Routes.ACTIVITY_LOG) },
                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                onOpenDiagnostics = { navController.navigate(Routes.DIAGNOSTICS) },
                onOpenSkippedFiles = { navController.navigate(Routes.SKIPPED_FILES) }
            )
        }
        composable(Routes.RULES) {
            val rules by viewModel.rules.collectAsStateWithLifecycle()
            var overlapIds by remember { mutableStateOf(setOf<String>()) }
            LaunchedEffect(rules) {
                overlapIds = viewModel.findAllOverlaps().keys.map { it.id }.toSet()
            }
            RuleListScreen(
                rules = rules,
                overlappingRuleIds = overlapIds,
                onToggleEnabled = { rule, enabled -> viewModel.saveRule(rule.copy(enabled = enabled)) },
                onMoveUp = { rule -> viewModel.moveRuleUp(rule.id) },
                onMoveDown = { rule -> viewModel.moveRuleDown(rule.id) },
                onEditRule = { rule -> navController.navigate(Routes.addEditRule(rule.id)) },
                onDeleteRule = { rule -> viewModel.deleteRule(rule.id) },
                onAddRule = { navController.navigate(Routes.addEditRule(null)) },
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
                    viewModel.saveRule(rule, removeDuplicateRuleId)
                    navController.popBackStack()
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
                onBack = { navController.popBackStack() }
            )
        }
        composable(Routes.SETTINGS) {
            val interval by viewModel.intervalMinutes.collectAsStateWithLifecycle()
            val conflictStrategy by viewModel.conflictStrategy.collectAsStateWithLifecycle()
            val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
            val safTreeUri by viewModel.safTreeUri.collectAsStateWithLifecycle()
            SettingsScreen(
                currentIntervalMinutes = interval,
                onIntervalSelected = { viewModel.setIntervalMinutes(it) },
                currentConflictStrategy = conflictStrategy,
                onConflictStrategySelected = { viewModel.setConflictStrategy(it) },
                currentThemeMode = themeMode,
                onThemeModeSelected = { viewModel.setThemeMode(it) },
                currentSafTreeUri = safTreeUri,
                onPickSafFolder = { onPickSafFolder() },
                onClearSafFolder = { viewModel.clearSafTreeUri() },
                onExportRequested = { viewModel.exportRulesJson() },
                onImportRequested = { text, cb -> viewModel.importRulesJson(text, cb) },
                onBack = { navController.popBackStack() }
            )
        }
        composable(Routes.DIAGNOSTICS) {
            var fileNames by remember { mutableStateOf<List<String>>(emptyList()) }
            LaunchedEffect(Unit) { fileNames = viewModel.listDownloadsFileNames() }
            DiagnosticsScreen(
                downloadsFileNames = fileNames,
                onBack = { navController.popBackStack() },
                onOpenZipSorter = { navController.navigate(Routes.ZIP_SORTER) }
            )
        }
        composable(Routes.SKIPPED_FILES) {
            val skipped by viewModel.lastSkippedFiles.collectAsStateWithLifecycle()
            SkippedFilesScreen(skipped = skipped, onBack = { navController.popBackStack() })
        }
        composable(Routes.ZIP_SORTER) {
            val folderUri by zipSorterViewModel.selectedFolderUri.collectAsStateWithLifecycle()
            val sortState by zipSorterViewModel.sortState.collectAsStateWithLifecycle()
            ZipSorterScreen(
                selectedFolderUri = folderUri,
                sortState = sortState,
                onPickFolder = { uri -> zipSorterViewModel.onFolderPicked(uri) },
                onStartSort = { zipSorterViewModel.startSort() },
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
            Text("Izin Diperlukan", style = MaterialTheme.typography.headlineSmall, color = colors.onBackground)
            Text(
                "PromptVault butuh akses ke semua file supaya bisa memindahkan ZIP & TXT " +
                    "di Downloads ke folder yang kamu tentukan. Tanpa izin ini, app tidak bisa bekerja.",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onBackground
            )
            Button(
                onClick = onPrimaryAction,
                colors = ButtonDefaults.buttonColors(containerColor = colors.secondary, contentColor = colors.onSecondary),
                modifier = Modifier.fillMaxWidth()
            ) { Text("Buka Pengaturan Izin") }
            OutlinedButton(
                onClick = onRecheck,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.primary),
                modifier = Modifier.fillMaxWidth()
            ) { Text("Sudah diizinkan, cek ulang") }
            // Fallback khusus API 26-29: kalau user pernah menolak dialog izin
            // dan Android tidak akan menampilkannya lagi otomatis (permanently
            // denied), satu-satunya jalan adalah pengaturan aplikasi manual.
            if (showAppSettingsFallback) {
                OutlinedButton(
                    onClick = onOpenAppSettings,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.onSurfaceVariant),
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Izin ditolak permanen? Buka Pengaturan Aplikasi") }
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
