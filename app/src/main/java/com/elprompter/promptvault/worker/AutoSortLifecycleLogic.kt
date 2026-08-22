package com.elprompter.promptvault.worker

import com.elprompter.promptvault.R

/**
 * [Pending queue P2 #5, 2026-08-22] Konsolidasi SEMUA keputusan pure
 * (tanpa Context/WorkManager/DataStore) di lifecycle Auto-Sort vs Manual
 * Scan ke satu tempat -- murni extract-function dari [AutoSortWorker],
 * [WorkScheduler], [AutoSortNotification] APA ADANYA, 0 perubahan
 * perilaku. Tujuannya supaya bisa di-unit-test murni JVM (pola sama dgn
 * `RuleRepositoryPureLogicTest`/`FileSorterPureLogicTest`, 0 dependency
 * baru, 0 Robolectric) -- project ini belum punya infra
 * instrumented/Robolectric sama sekali (lihat PROJECT_STATE.md v8.22.11).
 *
 * **Cakupan test yang TERJANGKAU lewat pure logic**: gate ON/OFF worker
 * periodik, asimetri manual-selalu-jalan, keputusan schedule/cancel
 * scheduler, pemilihan judul notifikasi ongoing & hasil.
 * **TIDAK terjangkau** (tetap di pending queue, BUKAN dianggap selesai):
 * eksekusi nyata `doWork()`/`WorkManager` end-to-end, dan reboot survival
 * (`BootCompletedReceiver` benar2 mem-restart proses & meng-enqueue ulang)
 * -- keduanya butuh Robolectric/instrumented test yang belum ada infra-nya.
 */
object AutoSortLifecycleLogic {

    /** [AutoSortWorker.doWork] -- worker periodik HANYA jalan kalau toggle ON. */
    fun shouldRunPeriodicScan(autoSortEnabled: Boolean): Boolean = autoSortEnabled

    /**
     * [ManualScanWorker.doWork] -- SENGAJA selalu true, tidak pernah dipengaruhi
     * toggle auto-sort (lihat javadoc ManualScanWorker.kt). Fungsi ini ADA
     * supaya asimetri ini punya regression test eksplisit, bukan cuma
     * "ketiadaan gate" yang gampang lolos kalau suatu saat ada yang tanpa
     * sadar menambahkan gate serupa ke ManualScanWorker.
     */
    fun shouldRunManualScan(): Boolean = true

    /** [WorkScheduler.syncFromSavedSettings] -- ON -> schedule, OFF -> cancel. */
    fun shouldScheduleWork(autoSortEnabled: Boolean): Boolean = autoSortEnabled

    /** [AutoSortNotification.foregroundInfo] -- judul notifikasi ongoing. */
    fun ongoingNotifTitleRes(isManual: Boolean): Int =
        if (isManual) R.string.manual_scan_notif_title else R.string.auto_sort_notif_title

    /** [AutoSortNotification.resultNotification] -- judul notifikasi hasil. */
    fun resultNotifTitleRes(isManual: Boolean): Int =
        if (isManual) R.string.manual_scan_result_notif_title else R.string.auto_sort_result_notif_title
}
