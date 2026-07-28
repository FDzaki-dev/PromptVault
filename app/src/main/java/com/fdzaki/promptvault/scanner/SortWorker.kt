package com.fdzaki.promptvault.scanner

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.fdzaki.promptvault.data.RuleRepository
import kotlinx.coroutines.flow.first

/**
 * Periodic background worker that re-runs the sorter without the app being open.
 * Scheduled from MainActivity via WorkManager's PeriodicWorkRequest (min interval 15 min,
 * the OS-enforced floor for periodic work).
 */
class SortWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val repo = RuleRepository(applicationContext)
            val rules = repo.rules.first()
            DownloadsSorter().scanAndSort(rules)
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    companion object {
        const val WORK_NAME = "promptvault_periodic_sort"
    }
}
