package com.elprompter.promptvault.worker

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.elprompter.promptvault.data.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

/**
 * TODO #2: interval auto-scan kini bisa diatur dari UI (sebelumnya hardcoded 15 menit).
 * WorkManager PeriodicWorkRequest tidak bisa kurang dari 15 menit, jadi nilai yang
 * diizinkan (lihat SettingsRepository.ALLOWED_INTERVALS) semuanya >= 15.
 */
object WorkScheduler {

    fun schedule(context: Context, intervalMinutes: Int) {
        val constraints = Constraints.Builder().build()

        val request = PeriodicWorkRequestBuilder<AutoSortWorker>(intervalMinutes.toLong(), TimeUnit.MINUTES)
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.LINEAR, 30, TimeUnit.SECONDS)
            .addTag(AutoSortWorker.WORK_TAG)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            AutoSortWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(AutoSortWorker.WORK_NAME)
    }

    /** Dipanggil dari Application.onCreate() dan dari BootCompletedReceiver. */
    fun rescheduleFromSavedSettings(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            val minutes = SettingsRepository(context).getIntervalMinutes()
            schedule(context, minutes)
        }
    }
}
