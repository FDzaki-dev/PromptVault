package com.elprompter.promptvault.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.elprompter.promptvault.data.ActivityLogRepository
import com.elprompter.promptvault.data.MoveHistoryRepository
import com.elprompter.promptvault.data.RuleRepository
import com.elprompter.promptvault.data.SettingsRepository
import com.elprompter.promptvault.util.FileSorter

class AutoSortWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        return try {
            val sorter = FileSorter(
                context = applicationContext,
                ruleRepository = RuleRepository(applicationContext),
                activityLogRepository = ActivityLogRepository(applicationContext),
                moveHistoryRepository = MoveHistoryRepository(applicationContext),
                settingsRepository = SettingsRepository(applicationContext)
            )
            sorter.scanAndSort()
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    companion object {
        const val WORK_NAME = "prompt_vault_auto_sort"
        const val WORK_TAG = "prompt_vault_auto_sort_tag"
    }
}
