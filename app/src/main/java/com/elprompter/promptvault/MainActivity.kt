package com.elprompter.promptvault

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
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
import com.elprompter.promptvault.ui.screens.RuleListScreen
import com.elprompter.promptvault.ui.screens.SettingsScreen
import com.elprompter.promptvault.ui.screens.SkippedFilesScreen
import com.elprompter.promptvault.ui.theme.CardPaper
import com.elprompter.promptvault.ui.theme.Ink
import com.elprompter.promptvault.ui.theme.Kraft
import com.elprompter.promptvault.ui.theme.Pine
import com.elprompter.promptvault.ui.theme.PromptVaultTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private val onboardingDoneKey = booleanPreferencesKey("onboarding_done")

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        // Splash brand-in (Pine) sebelum konten Compose siap -- kesan pertama
        // yang konsisten, bukan layar putih kosong khas app "belum jadi".
        installSplashScreen()
        super.onCreate(savedInstanceState)

        // Edge-to-edge dengan status bar & nav bar mengikuti warna kraft,
        // supaya chrome sistem tidak terasa "nempel" tapi menyatu dengan tema.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Kraft.toArgb(), Ink.toArgb()),
            navigationBarStyle = SystemBarStyle.light(Kraft.toArgb(), Ink.toArgb())
        )

        setContent {
            PromptVaultTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    PromptVaultRoot(viewModel)
                }
            }
        }
    }
}

@Composable
private fun PromptVaultRoot(viewModel: MainViewModel) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val navController = rememberNavController()
    val scope = rememberCoroutineScope()

    var onboardingDone by remember { mutableStateOf<Boolean?>(null) }
    LaunchedEffect(Unit) {
        val prefs = context.promptVaultDataStore.data.first()
        onboardingDone = prefs[onboardingDoneKey] ?: false
    }

    var hasStoragePermission by remember { mutableStateOf(hasManageStoragePermission()) }

    if (onboardingDone == null) return // jeda singkat sebelum splash native selesai, hindari flicker

    if (!hasStoragePermission) {
        PermissionGate(
            onOpenSettings = {
                val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, Uri.parse("package:${context.packageName}"))
                } else {
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))
                }
                context.startActivity(intent)
            },
            onRecheck = { hasStoragePermission = hasManageStoragePermission() }
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

    NavHost(navController = navController, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            val rules by viewModel.rules.collectAsStateWithLifecycle()
            val interval by viewModel.intervalMinutes.collectAsStateWithLifecycle()
            val isScanning by viewModel.isScanning.collectAsStateWithLifecycle()
            val summary by viewModel.lastScanSummary.collectAsStateWithLifecycle()
            val skipped by viewModel.lastSkippedFiles.collectAsStateWithLifecycle()

            HomeScreen(
                ruleCount = rules.count { it.enabled },
                intervalMinutes = interval,
                isScanning = isScanning,
                lastScanSummary = summary,
                hasSkippedFiles = skipped.isNotEmpty(),
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
                onSave = { rule ->
                    viewModel.saveRule(rule)
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
            SettingsScreen(
                currentIntervalMinutes = interval,
                onIntervalSelected = { viewModel.setIntervalMinutes(it) },
                onExportRequested = { viewModel.exportRulesJson() },
                onImportRequested = { text, cb -> viewModel.importRulesJson(text, cb) },
                onBack = { navController.popBackStack() }
            )
        }
        composable(Routes.DIAGNOSTICS) {
            var fileNames by remember { mutableStateOf<List<String>>(emptyList()) }
            LaunchedEffect(Unit) { fileNames = viewModel.listDownloadsFileNames() }
            DiagnosticsScreen(downloadsFileNames = fileNames, onBack = { navController.popBackStack() })
        }
        composable(Routes.SKIPPED_FILES) {
            val skipped by viewModel.lastSkippedFiles.collectAsStateWithLifecycle()
            SkippedFilesScreen(skipped = skipped, onBack = { navController.popBackStack() })
        }
    }
}

@Composable
private fun PermissionGate(onOpenSettings: () -> Unit, onRecheck: () -> Unit) {
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
                    .background(Pine, RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.FolderOpen, contentDescription = null, tint = CardPaper, modifier = Modifier.size(32.dp))
            }
            Text("Izin Diperlukan", style = MaterialTheme.typography.headlineSmall)
            Text(
                "PromptVault butuh akses ke semua file supaya bisa memindahkan ZIP & TXT " +
                    "di Downloads ke folder yang kamu tentukan. Tanpa izin ini, app tidak bisa bekerja.",
                style = MaterialTheme.typography.bodyMedium
            )
            Button(
                onClick = onOpenSettings,
                colors = ButtonDefaults.buttonColors(containerColor = com.elprompter.promptvault.ui.theme.Stamp, contentColor = CardPaper),
                modifier = Modifier.fillMaxWidth()
            ) { Text("Buka Pengaturan Izin") }
            OutlinedButton(
                onClick = onRecheck,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Pine),
                modifier = Modifier.fillMaxWidth()
            ) { Text("Sudah diizinkan, cek ulang") }
        }
    }
}

private fun hasManageStoragePermission(): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        Environment.isExternalStorageManager()
    } else {
        true
    }
}
