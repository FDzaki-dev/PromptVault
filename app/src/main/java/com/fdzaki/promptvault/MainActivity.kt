package com.fdzaki.promptvault

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.*
import androidx.lifecycle.lifecycleScope
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.fdzaki.promptvault.data.LogRepository
import com.fdzaki.promptvault.data.OnboardingRepository
import com.fdzaki.promptvault.data.RuleRepository
import com.fdzaki.promptvault.data.SortLogEntry
import com.fdzaki.promptvault.data.SortRule
import com.fdzaki.promptvault.scanner.DownloadsSorter
import com.fdzaki.promptvault.scanner.ScanResult
import com.fdzaki.promptvault.scanner.SortWorker
import com.fdzaki.promptvault.ui.OnboardingScreen
import com.fdzaki.promptvault.ui.VaultScreen
import com.fdzaki.promptvault.ui.theme.PromptVaultTheme
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {

    private lateinit var ruleRepository: RuleRepository
    private lateinit var logRepository: LogRepository
    private lateinit var onboardingRepository: OnboardingRepository
    private val sorter = DownloadsSorter()

    private val manageStorageLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { refreshPermissionState() }

    private var hasPermissionState = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ruleRepository = RuleRepository(applicationContext)
        logRepository = LogRepository(applicationContext)
        onboardingRepository = OnboardingRepository(applicationContext)
        hasPermissionState.value = hasStoragePermission()

        setContent {
            var rules by remember { mutableStateOf(listOf<SortRule>()) }
            var logs by remember { mutableStateOf(listOf<SortLogEntry>()) }
            var onboardingCompleted by remember { mutableStateOf<Boolean?>(null) }
            val snackbarHostState = remember { SnackbarHostState() }

            LaunchedEffect(Unit) {
                launch { ruleRepository.rules.collect { rules = it } }
                launch { logRepository.logs.collect { logs = it } }
                launch { onboardingRepository.isCompleted.collect { onboardingCompleted = it } }
            }

            PromptVaultTheme {
                when (onboardingCompleted) {
                    null -> {
                        // still loading the flag from DataStore; render nothing to avoid a flash
                    }
                    false -> {
                        OnboardingScreen(
                            onFinish = {
                                lifecycleScope.launch { onboardingRepository.markCompleted() }
                            }
                        )
                    }
                    true -> {
                        VaultScreen(
                            rules = rules,
                            logs = logs,
                            hasStoragePermission = hasPermissionState.value,
                            snackbarHostState = snackbarHostState,
                            onRequestPermission = { requestStoragePermission() },
                            onScanNow = {
                                lifecycleScope.launch {
                                    when (val result = sorter.scanAndSort(rules)) {
                                        is ScanResult.Success -> {
                                            logRepository.append(result.movedEntries)
                                            snackbarHostState.showSnackbar(
                                                "${result.movedEntries.size} file berhasil dirapikan."
                                            )
                                        }
                                        ScanResult.NoMatchingFiles -> {
                                            snackbarHostState.showSnackbar(
                                                "Tidak ada file baru yang cocok dengan aturanmu."
                                            )
                                        }
                                        ScanResult.DownloadsDirUnavailable -> {
                                            snackbarHostState.showSnackbar(
                                                "Folder Downloads tidak bisa diakses. Cek izin penyimpanan."
                                            )
                                        }
                                    }
                                }
                            },
                            onAddRule = { pattern, folder ->
                                lifecycleScope.launch {
                                    ruleRepository.saveRule(SortRule(pattern = pattern, folderName = folder))
                                }
                            },
                            onDeleteRule = { pattern ->
                                lifecycleScope.launch { ruleRepository.deleteRule(pattern) }
                            }
                        )
                    }
                }
            }
        }

        schedulePeriodicSort()
    }

    override fun onResume() {
        super.onResume()
        refreshPermissionState()
    }

    private fun refreshPermissionState() {
        hasPermissionState.value = hasStoragePermission()
    }

    private fun hasStoragePermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            true
        }
    }

    private fun requestStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                data = Uri.parse("package:$packageName")
            }
            manageStorageLauncher.launch(intent)
        }
    }

    private fun schedulePeriodicSort() {
        val request = PeriodicWorkRequestBuilder<SortWorker>(15, TimeUnit.MINUTES).build()
        WorkManager.getInstance(applicationContext).enqueueUniquePeriodicWork(
            SortWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }
}
