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
import androidx.compose.runtime.*
import androidx.lifecycle.lifecycleScope
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.fdzaki.promptvault.data.RuleRepository
import com.fdzaki.promptvault.data.SortLogEntry
import com.fdzaki.promptvault.data.SortRule
import com.fdzaki.promptvault.scanner.DownloadsSorter
import com.fdzaki.promptvault.scanner.SortWorker
import com.fdzaki.promptvault.ui.VaultScreen
import com.fdzaki.promptvault.ui.theme.PromptVaultTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {

    private lateinit var repository: RuleRepository
    private val sorter = DownloadsSorter()

    private val manageStorageLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { refreshPermissionState() }

    private var hasPermissionState = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        repository = RuleRepository(applicationContext)
        hasPermissionState.value = hasStoragePermission()

        setContent {
            var rules by remember { mutableStateOf(listOf<SortRule>()) }
            var logs by remember { mutableStateOf(listOf<SortLogEntry>()) }

            LaunchedEffect(Unit) {
                repository.rules.collect { rules = it }
            }

            PromptVaultTheme {
                VaultScreen(
                    rules = rules,
                    logs = logs,
                    hasStoragePermission = hasPermissionState.value,
                    onRequestPermission = { requestStoragePermission() },
                    onScanNow = {
                        lifecycleScope.launch {
                            val result = sorter.scanAndSort(rules)
                            logs = logs + result
                        }
                    },
                    onAddRule = { pattern, folder ->
                        lifecycleScope.launch {
                            repository.saveRule(SortRule(pattern = pattern, folderName = folder))
                        }
                    },
                    onDeleteRule = { pattern ->
                        lifecycleScope.launch { repository.deleteRule(pattern) }
                    }
                )
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
