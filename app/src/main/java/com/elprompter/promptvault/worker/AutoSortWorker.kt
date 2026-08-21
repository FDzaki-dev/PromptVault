package com.elprompter.promptvault.worker

import android.content.Context
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.elprompter.promptvault.data.ActivityLogRepository
import com.elprompter.promptvault.data.LogLevel
import com.elprompter.promptvault.data.MoveHistoryRepository
import com.elprompter.promptvault.data.RuleRepository
import com.elprompter.promptvault.data.SettingsRepository
import com.elprompter.promptvault.util.FileSorter
import kotlinx.coroutines.flow.first

class AutoSortWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        return try {
            // [Fix Auto-Sort ON/OFF, 2026-08-21] Defense-in-depth: worker stale/
            // pending yang sempat terjadwal SEBELUM user menekan OFF (WorkManager
            // tidak selalu langsung mem-batalkan instance yang sudah running/
            // dispatched) tetap bisa dipanggil sistem -- gate ini pastikan worker
            // itu TIDAK ikut scan kalau baca OFF, cukup no-op sukses (bukan retry/
            // failure, memang sengaja tidak ada kerjaan).
            if (!SettingsRepository(applicationContext).getAutoSortEnabled()) {
                return Result.success()
            }
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
            val moveHistoryRepository = MoveHistoryRepository(applicationContext)
            val sorter = FileSorter(
                context = applicationContext,
                ruleRepository = RuleRepository(applicationContext),
                activityLogRepository = ActivityLogRepository(applicationContext),
                moveHistoryRepository = moveHistoryRepository,
                settingsRepository = SettingsRepository(applicationContext)
            )
            // [Fase 2.2 roadmap, 2026-08-21] Timestamp SEBELUM scan mulai --
            // dipakai setelah scan buat ambil "entri mana saja yang baru
            // ditulis scan INI" dari MoveHistoryRepository (bukan sejak awal
            // waktu), lihat pemakaiannya di bawah.
            val scanStartMillis = System.currentTimeMillis()
            val result = sorter.scanAndSort()
            // Notifikasi hasil HANYA kalau benar-benar ada file dipindah --
            // sengaja TIDAK notif tiap siklus auto-scan (tiap 240 menit
            // default) kalau nihil, supaya tidak jadi notification fatigue.
            // Detail "0 dipindahkan" tetap tercatat di Log Aktivitas seperti
            // biasa (FileSorter yang urus itu, tidak berubah di sini).
            if (result.filesMoved > 0) {
                try {
                    // [perf] historyFlow.first() ambil snapshot SEKALI (bukan
                    // collect terus), sama pola dgn cara MainViewModel baca
                    // Flow one-shot lain di project ini. Filter by
                    // timestampMillis >= scanStartMillis -- cara termurah
                    // dapat "entri dari scan ini saja" TANPA nambah kolom/
                    // query baru di MoveHistoryDao (Protected Asset, DB
                    // Schema/DAO -- dihindari kalau ada cara lain yang aman).
                    // Caveat jujur: kalau scan manual dari MainViewModel
                    // kebetulan jalan di window waktu yang SAMA PERSIS
                    // (praktis mustahil, scanMutex sudah cegah 2 scan
                    // beriringan), breakdown per-rule bisa sedikit meleset
                    // dari [result.filesMoved] -- total di judul notifikasi
                    // TETAP dari [result] (sumber kebenaran asli), cuma
                    // baris per-rule yang derived best-effort.
                    val perRule = moveHistoryRepository.historyFlow.first()
                        .filter { it.timestampMillis >= scanStartMillis }
                        .groupingBy { it.ruleFolderName }
                        .eachCount()
                    NotificationManagerCompat.from(applicationContext).notify(
                        AutoSortNotification.RESULT_NOTIFICATION_ID,
                        AutoSortNotification.resultNotification(applicationContext, result.filesMoved, perRule)
                    )
                } catch (e: SecurityException) {
                    // Izin POST_NOTIFICATIONS dicabut runtime (Android 13+) --
                    // best-effort, jangan gagalkan hasil scan yang sudah sukses.
                } catch (e: Exception) {
                    // Kegagalan notifikasi hasil (device aneh, dll) TIDAK BOLEH
                    // menggagalkan Result.success() -- file sudah benar-benar
                    // tersortir, itu yang utama.
                }
            }
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
