package com.elprompter.promptvault.worker

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.elprompter.promptvault.data.SettingsRepository
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.TimeUnit

/**
 * Interval auto-scan bisa diatur dari UI (fitur lengkap, sebelumnya hardcoded 15 menit).
 * WorkManager PeriodicWorkRequest tidak bisa kurang dari 15 menit, jadi nilai yang
 * diizinkan (lihat SettingsRepository.ALLOWED_INTERVALS) semuanya >= 15.
 */
object WorkScheduler {

    /**
     * [Fix race ON/OFF, 2026-08-22] Bug nyata (audit): startup
     * (PromptVaultApp.onCreate) baca DataStore lama (ON) SEBELUM user
     * sempat matiin toggle, tapi baru benar-benar panggil schedule()
     * SETELAH coroutine toggle user (yang lebih baru) sudah kelar
     * cancel() -- hasil akhir: scheduler balik ON walau user baru saja
     * matiin. Penyebabnya BUKAN nilai yang salah (baca DataStore-nya
     * benar), tapi URUTAN EKSEKUSI 2 coroutine independen terhadap
     * WorkManager yang tidak terjamin.
     * Fix: 1 [Mutex] menyerialkan SEMUA jalur apply (startup/reboot/
     * toggle/ganti interval) -- dan baca DataStore FRESH di DALAM
     * critical section (bukan pakai parameter yang mungkin sudah basi
     * saat giliran coroutine ini tiba). Siapa pun yang menang antrean
     * mutex, keputusan schedule/cancel SELALU berdasarkan state
     * TERBARU yang tersimpan saat itu -- toggle OFF user tidak lagi
     * bisa tertimpa coroutine lama yang baru dapat giliran belakangan.
     */
    private val mutex = Mutex()

    private fun schedule(context: Context, intervalMinutes: Int) {
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

    private fun cancel(context: Context) {
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
    /**
     * [Fix Auto-Sort ON/OFF, 2026-08-21] Dulu unconditional schedule() --
     * sekarang baca SettingsRepository.autoSortEnabled dulu: OFF -> cancel()
     * (bukan schedule), supaya PromptVaultApp.onCreate()/BootCompletedReceiver
     * TIDAK diam-diam menghidupkan lagi scheduler yang sudah user matikan.
     * Interval tetap dibaca dari DataStore SAAT ON (sumber tunggal, tidak ada
     * duplikasi logic default interval di caller manapun).
     * [Fix race ON/OFF, 2026-08-22] Sekarang cuma alias tipis ke
     * [syncFromSavedSettings] -- 1 satu-satunya jalur baca+terapkan state,
     * dipakai baik dari startup/reboot maupun dari toggle/ganti interval
     * user, semua lewat mutex yang sama.
     */
    suspend fun rescheduleFromSavedSettings(context: Context) = syncFromSavedSettings(context)

    /**
     * [Fix race ON/OFF, 2026-08-22] Satu-satunya jalur yang boleh menyentuh
     * WorkManager (schedule/cancel) utk auto-sort. Dipanggil dari:
     * PromptVaultApp.onCreate, BootCompletedReceiver, dan
     * MainViewModel.setAutoSortEnabled/setIntervalMinutes SETELAH persist
     * ke DataStore selesai -- fungsi ini sendiri yang baca ulang state
     * terbaru di dalam mutex, jadi pemanggil tidak perlu (dan sebaiknya
     * tidak) kirim nilai enabled/interval hasil hitungan sendiri.
     */
    suspend fun syncFromSavedSettings(context: Context) {
        mutex.withLock {
            val repo = SettingsRepository(context)
            if (AutoSortLifecycleLogic.shouldScheduleWork(repo.getAutoSortEnabled())) {
                schedule(context, repo.getIntervalMinutes())
            } else {
                cancel(context)
            }
        }
    }
}
