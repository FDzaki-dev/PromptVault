package com.elprompter.promptvault

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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
import com.elprompter.promptvault.ui.theme.PromptVaultTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private val onboardingDoneKey = booleanPreferencesKey("onboarding_done")

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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

    if (onboardingDone == null) return // splash kosong sesaat, hindari flicker

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

            HomeScreen(
                ruleCount = rules.count { it.enabled },
                intervalMinutes = interval,
                isScanning = isScanning,
                lastScanSummary = summary,
                onScanNow = { viewModel.runManualScan() },
                onOpenRules = { navController.navigate(Routes.RULES) },
                onOpenLog = { navController.navigate(Routes.ACTIVITY_LOG) },
                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                onOpenDiagnostics = { navController.navigate(Routes.DIAGNOSTICS) }
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
                onEditRule = { rule -> navController.navigate(Routes.addEditRule(rule.id)) },
                onDeleteRule = { rule -> viewModel.deleteRule(rule.id) },
                onAddRule = { navController.navigate(Routes.addEditRule(null)) }
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
                onUndo = { entry -> viewModel.undoMove(entry) }
            )
        }
        composable(Routes.SETTINGS) {
            val interval by viewModel.intervalMinutes.collectAsStateWithLifecycle()
            SettingsScreen(
                currentIntervalMinutes = interval,
                onIntervalSelected = { viewModel.setIntervalMinutes(it) },
                onExportRequested = { viewModel.exportRulesJson() },
                onImportRequested = { text, cb -> viewModel.importRulesJson(text, cb) }
            )
        }
        composable(Routes.DIAGNOSTICS) {
            DiagnosticsScreen()
        }
    }
}

@Composable
private fun PermissionGate(onOpenSettings: () -> Unit, onRecheck: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Izin Diperlukan", style = MaterialTheme.typography.headlineSmall)
        Text("PromptVault perlu izin \"Akses semua file\" agar bisa memindahkan file di folder Downloads.")
        Button(onClick = onOpenSettings, modifier = Modifier.fillMaxWidth()) { Text("Buka Pengaturan Izin") }
        Button(onClick = onRecheck) { Text("Sudah diizinkan, cek ulang") }
    }
}

private fun hasManageStoragePermission(): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        Environment.isExternalStorageManager()
    } else {
        true
    }
}
