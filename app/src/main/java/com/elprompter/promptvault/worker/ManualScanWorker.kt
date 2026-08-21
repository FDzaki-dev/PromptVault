package com.elprompter.promptvault.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

/**
 * [FIX WAJIB "Auto-Sort vs Manual Scan", 2026-08-21 -- laporan user: widget
 * "Scan Sekarang" ikut terblokir saat Auto-Sort dimatikan, krn sebelumnya
 * `ScanWidgetProvider` enqueue [AutoSortWorker] yang emang sengaja punya
 * gate `autoSortEnabled` (lihat AutoSortWorker.kt)]
 *
 * Entry point TERPISAH khusus trigger MANUAL (widget, dan bisa dipakai UI
 * lain ke depannya) -- TIDAK BOLEH mengecek `autoSortEnabled` sama sekali,
 * manual scan HARUS selalu jalan kapan pun user minta.
 *
 * Badan kerja scan+lapor PERSIS SAMA dgn [AutoSortWorker] lewat
 * [runScanAndReport] yang di-share (ScanExecution.kt) -- FileSorter/SAF/
 * Shizuku/Rule Engine/notifikasi TIDAK diduplikasi maupun diubah sama
 * sekali, murni entry point kedua. Scheduler Auto-Sort ([WorkScheduler])
 * juga TIDAK disentuh -- worker ini HANYA dipicu manual (`.enqueue()`
 * satu-kali dari [ScanWidgetProvider]), tidak pernah dijadwalkan periodik.
 */
class ManualScanWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result = runScanAndReport(applicationContext)

    companion object {
        const val WORK_NAME = "prompt_vault_manual_scan"
        const val WORK_TAG = "prompt_vault_manual_scan_tag"
    }
}
