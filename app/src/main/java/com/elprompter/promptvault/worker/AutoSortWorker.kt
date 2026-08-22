package com.elprompter.promptvault.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.elprompter.promptvault.data.SettingsRepository

/**
 * [FIX WAJIB "Auto-Sort vs Manual Scan", 2026-08-21] Badan kerja scan+lapor
 * DIPINDAH ke [runScanAndReport] (ScanExecution.kt), dipakai ULANG APA
 * ADANYA di sini -- FileSorter/notifikasi/error-handling TIDAK diubah SAMA
 * SEKALI (murni extract-function), supaya bisa dipakai ulang juga oleh
 * [ManualScanWorker] TANPA duplikasi. Lihat javadoc ScanExecution.kt.
 */
class AutoSortWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        // [Fix Auto-Sort ON/OFF, 2026-08-21] Defense-in-depth: worker stale/
        // pending yang sempat terjadwal SEBELUM user menekan OFF (WorkManager
        // tidak selalu langsung mem-batalkan instance yang sudah running/
        // dispatched) tetap bisa dipanggil sistem -- gate ini pastikan worker
        // itu TIDAK ikut scan kalau baca OFF, cukup no-op sukses (bukan retry/
        // failure, memang sengaja tidak ada kerjaan).
        //
        // [PERTAHANKAN, 2026-08-21 -- instruksi eksplisit] Gate ini HANYA ada
        // di AutoSortWorker (auto-scan periodik). ManualScanWorker (widget &
        // trigger manual lain) SENGAJA TIDAK punya gate ini sama sekali --
        // manual scan HARUS selalu jalan kapan pun diminta user, terlepas
        // status auto-sort. Lihat ManualScanWorker.kt.
        if (!AutoSortLifecycleLogic.shouldRunPeriodicScan(SettingsRepository(applicationContext).getAutoSortEnabled())) {
            return Result.success()
        }
        return runScanAndReport(applicationContext, isManual = false)
    }

    companion object {
        const val WORK_NAME = "prompt_vault_auto_sort"
        const val WORK_TAG = "prompt_vault_auto_sort_tag"
    }
}
