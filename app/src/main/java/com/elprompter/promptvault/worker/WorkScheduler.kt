package com.elprompter.promptvault.worker

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.elprompter.promptvault.data.SettingsRepository
import java.util.concurrent.TimeUnit

/**
 * Interval auto-scan bisa diatur dari UI (fitur lengkap, sebelumnya hardcoded 15 menit).
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

    /**
     * Batch [worker-lifecycle-fix]: dulu fungsi ini sendiri yang membuka
     * CoroutineScope(Dispatchers.IO).launch{} secara internal (fire-and-forget).
     * Itu AMAN dipanggil dari Application.onCreate() (proses app sudah pasti
     * hidup), tapi BERBAHAYA dipanggil dari BroadcastReceiver: onReceive()
     * kembali seketika, dan Android boleh mematikan proses App SEBELUM
     * coroutine sempat baca DataStore + enqueue WorkManager -- terutama pas
     * boot, di mana proses baru dibuat cuma untuk broadcast ini saja tanpa
     * komponen lain yang menahannya hidup. Akibatnya: auto-sort bisa TIDAK
     * kejadwal ulang setelah reboot di sebagian device/timing, padahal itu
     * fitur inti "reboot survival" yang dijanjikan.
     * Fix: sekarang suspend fun biasa (bukan yang buka scope sendiri).
     * Pemanggil yang menentukan lifetime coroutine-nya -- lihat
     * PromptVaultApp (scope app biasa) vs BootCompletedReceiver (goAsync()
     * supaya proses ditahan hidup sampai selesai).
     */
    suspend fun rescheduleFromSavedSettings(context: Context) {
        val minutes = SettingsRepository(context).getIntervalMinutes()
        schedule(context, minutes)
    }
}
