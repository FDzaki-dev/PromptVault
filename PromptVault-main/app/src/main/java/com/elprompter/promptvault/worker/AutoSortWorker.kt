package com.elprompter.promptvault.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.elprompter.promptvault.data.ActivityLogRepository
import com.elprompter.promptvault.data.LogLevel
import com.elprompter.promptvault.data.MoveHistoryRepository
import com.elprompter.promptvault.data.RuleRepository
import com.elprompter.promptvault.data.SettingsRepository
import com.elprompter.promptvault.util.FileSorter

class AutoSortWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        return try {
            // Batch §5: promosikan ke foreground service SEBELUM scan mulai, supaya
            // OS tidak menjeda/membunuh worker di tengah scan panjang (lihat
            // AutoSortNotification.kt untuk alasan lengkap). Best-effort: kalau
            // sistem menolak (skenario tak terduga di sebagian OEM), auto-sort
            // TETAP lanjut jalan sebagai background worker biasa -- jangan sampai
            // kegagalan promosi foreground menggagalkan seluruh proses sortir.
            try {
                setForeground(AutoSortNotification.foregroundInfo(applicationContext))
            } catch (e: Exception) {
                // sengaja ditelan -- lihat komentar di atas
            }
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
            // Batch [worker-lifecycle-fix]: sebelumnya SEMUA exception di sini
            // ditelan diam-diam lalu selalu Result.retry() tanpa batas -- kalau
            // penyebabnya PERMANEN (mis. izin MANAGE_EXTERNAL_STORAGE dicabut
            // user dari Setelan Android), worker akan retry berulang setiap
            // periode SELAMANYA tanpa pernah berhasil, boros baterai, dan user
            // TIDAK PERNAH tahu kenapa karena tidak ada satu pun baris di Log
            // Aktivitas. Sekarang: (1) selalu dicatat ke Log Aktivitas dulu,
            // supaya kegagalan level-worker (bukan per-file) tetap kelihatan;
            // (2) SecurityException (khas izin dicabut) dianggap PERMANEN ->
            // Result.failure(), tidak retry sia-sia. Error lain (mis. I/O
            // sementara) tetap Result.retry() seperti semula.
            try {
                ActivityLogRepository(applicationContext).add(
                    LogLevel.ERROR,
                    "Auto-sort gagal dijalankan: ${e.javaClass.simpleName} - ${e.message ?: "tanpa pesan"}"
                )
            } catch (_: Exception) {
                // Kalau mencatat log pun gagal (mis. DB korup), jangan sampai
                // menutupi exception asli dengan crash baru -- lanjut ke Result di bawah.
            }
            if (e is SecurityException) Result.failure() else Result.retry()
        }
    }

    companion object {
        const val WORK_NAME = "prompt_vault_auto_sort"
        const val WORK_TAG = "prompt_vault_auto_sort_tag"
    }
}
