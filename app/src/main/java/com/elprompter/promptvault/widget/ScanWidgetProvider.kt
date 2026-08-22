package com.elprompter.promptvault.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import android.widget.Toast
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.elprompter.promptvault.R
import com.elprompter.promptvault.data.SettingsRepository
import com.elprompter.promptvault.worker.ManualScanWorker
import kotlinx.coroutines.runBlocking

/**
 * [Fase 3.1 roadmap, 2026-08-21 -- dipilih eksplisit user dari 4 opsi Fase 3
 * (widget/cloud/lokalisasi/multi-profil) via ask_user_input_v0]
 *
 * Widget home screen: 1 area ketuk "Scan Sekarang", trigger scan Downloads
 * tanpa buka app. [FIX 2026-08-21] Enqueue [ManualScanWorker] (BUKAN
 * AutoSortWorker lagi -- lihat komentar di onReceive kenapa), entry point
 * manual terpisah TANPA gate autoSortEnabled, badan kerja scan tetap
 * di-share via runScanAndReport (ScanExecution.kt) dgn AutoSortWorker --
 * FileSorter/rule matching/notifikasi hasil TIDAK disentuh sama sekali.
 *
 * [PENDING QUEUE #1 v8.22.1 -> DITUTUP 2026-08-22] Widget TIDAK LAGI
 * 100% stateless -- baris aksi (@id/widget_action) sekarang menampilkan
 * ringkasan scan TERAKHIR ("N file • HH:mm"), dipush lewat
 * [notifyScanCompleted] segera setelah `runScanAndReport` selesai
 * (ScanExecution.kt, jalur auto-sort MAUPUN manual widget), dan dibaca
 * ulang dari [SettingsRepository.widgetLastScanSummaryFlow] di
 * [updateWidget] (`onUpdate`) supaya bertahan lintas resize/reboot/proses
 * widget di-restart OS -- BUKAN observer/update-loop real-time lintas-
 * proses (yang tetap dihindari sesuai risiko `ROADMAP.md` item 3.1),
 * murni tulis-sekali-baca-ulang lewat DataStore yang sudah ada. Toast
 * instan tap & notifikasi sistem (`AutoSortNotification.resultNotification`,
 * HANYA saat `filesMoved > 0`) TETAP dipertahankan sbg sumber kebenaran
 * hasil detail per-rule -- ringkasan widget ini pelengkap ringkas, bukan
 * pengganti.
 *
 * Tidak butuh dependency baru (`androidx.glance` dll) -- `AppWidgetProvider`
 * + `RemoteViews` polos sudah bagian framework Android, konsisten dgn
 * prinsip "nol dependency baru" yang sama dipakai `StatisticsScreen.kt`
 * (Canvas hand-rolled, v8.20.0).
 */
class ScanWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        appWidgetIds.forEach { id -> updateWidget(context, appWidgetManager, id) }
    }

    override fun onReceive(context: Context, intent: Intent) {
        // super WAJIB dipanggil duluan -- AppWidgetProvider.onReceive yang
        // mendengarkan ACTION_APPWIDGET_UPDATE/DELETED/dst & memanggil
        // onUpdate()/dst di bawahnya. Action custom kita (ACTION_SCAN_NOW)
        // tidak dikenali logic bawaan itu, jadi aman lanjut dicek manual
        // setelahnya -- tidak saling menimpa.
        super.onReceive(context, intent)
        if (intent.action == ACTION_SCAN_NOW) {
            Toast.makeText(context, context.getString(R.string.widget_scan_toast), Toast.LENGTH_SHORT).show()
            // [FIX WAJIB "Auto-Sort vs Manual Scan", 2026-08-21] SEBELUMNYA
            // enqueue AutoSortWorker -- BUG: AutoSortWorker punya gate
            // autoSortEnabled (lihat AutoSortWorker.kt), jadi "Auto-Sort OFF"
            // ikut memblokir tombol widget ini. Fix: enqueue ManualScanWorker
            // (entry point terpisah, TANPA gate) -- badan kerja scan tetap
            // PERSIS SAMA lewat runScanAndReport yang di-share (lihat
            // ScanExecution.kt), FileSorter/SAF/Shizuku/notifikasi tidak
            // diduplikasi/diubah sama sekali.
            //
            // .enqueue() biasa (BUKAN enqueueUniqueWork) -- trigger manual
            // dari widget SENGAJA tidak berbagi slot unique work dgn
            // AutoSortWorker.WORK_NAME milik auto-scan periodik ([WorkScheduler]),
            // supaya tap widget tidak pernah "ditolak/di-drop" gara-gara
            // slot unique itu lagi terisi jadwal periodik. Aman dari race
            // eksekusi ganda krn scanMutex statis di FileSorter (lihat
            // javadoc class di atas).
            WorkManager.getInstance(context).enqueue(OneTimeWorkRequestBuilder<ManualScanWorker>().build())
        }
    }

    private fun updateWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
        // [PENDING QUEUE #1, 2026-08-22] Baca ringkasan scan TERAKHIR yang
        // dipersist -- runBlocking aman di sini: baca DataStore Preferences
        // lokal (bukan network/disk besar), dan onUpdate cuma dipanggil
        // jarang (widget baru ditambah/di-resize/reboot), bukan tiap detik.
        val summary = runBlocking { SettingsRepository(context).getWidgetLastScanSummary() }
        val views = buildWidgetViews(context, appWidgetId, summary)
        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    companion object {
        const val ACTION_SCAN_NOW = "com.elprompter.promptvault.widget.ACTION_SCAN_NOW"

        /**
         * [PENDING QUEUE #1, v8.22.1 -> dieksekusi 2026-08-22] Dipanggil dari
         * `runScanAndReport` (ScanExecution.kt) SEGERA setelah scan selesai
         * (auto-sort periodik ATAU manual widget) -- push [summaryText] ke
         * SEMUA instance widget yang sedang terpasang. No-op kalau widget
         * belum pernah ditambahkan ke home screen manapun (`ids` kosong),
         * supaya tidak ada kerja sia-sia tiap scan untuk user yang tidak
         * pakai widget sama sekali.
         */
        fun notifyScanCompleted(context: Context, summaryText: String) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, ScanWidgetProvider::class.java))
            if (ids.isEmpty()) return
            ids.forEach { id -> manager.updateAppWidget(id, buildWidgetViews(context, id, summaryText)) }
        }

        /**
         * Satu sumber pembangun RemoteViews, dipakai ULANG oleh instance
         * [updateWidget] (baca dari persistensi) & [notifyScanCompleted]
         * (push langsung pasca-scan) -- supaya wiring klik (PendingIntent)
         * tidak pernah ketinggalan/berbeda antara 2 jalur update ini.
         * [summary] `null`/kosong -> fallback ke label statis lama
         * (@string/widget_scan_action) -- kondisi ini HANYA terjadi kalau
         * belum pernah ada satu scan pun sejak app diinstal.
         */
        private fun buildWidgetViews(context: Context, appWidgetId: Int, summary: String?): RemoteViews {
            val views = RemoteViews(context.packageName, R.layout.widget_scan)
            views.setTextViewText(
                R.id.widget_action,
                if (summary.isNullOrBlank()) context.getString(R.string.widget_scan_action) else summary
            )
            val scanIntent = Intent(context, ScanWidgetProvider::class.java).apply { action = ACTION_SCAN_NOW }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                appWidgetId,
                scanIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)
            return views
        }
    }
}
