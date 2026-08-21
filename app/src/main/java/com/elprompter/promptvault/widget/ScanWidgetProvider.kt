package com.elprompter.promptvault.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import android.widget.Toast
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.elprompter.promptvault.R
import com.elprompter.promptvault.worker.AutoSortWorker

/**
 * [Fase 3.1 roadmap, 2026-08-21 -- dipilih eksplisit user dari 4 opsi Fase 3
 * (widget/cloud/lokalisasi/multi-profil) via ask_user_input_v0]
 *
 * Widget home screen: 1 area ketuk "Scan Sekarang", trigger scan Downloads
 * tanpa buka app. Reuse [AutoSortWorker] APA ADANYA -- class WorkManager
 * yang SAMA PERSIS dipakai auto-scan periodik ([WorkScheduler]) & tombol
 * "Scan Sekarang" di Home (tidak langsung, lewat [MainViewModel.
 * runManualScan] yang panggil [com.elprompter.promptvault.util.FileSorter]
 * langsung -- tapi ujungnya logic SAMA, sama-sama `scanAndSort()` yang
 * dilindungi `scanMutex` statis di FileSorter, jadi trigger widget tidak
 * mungkin race dgn scan manual/periodik yang kebetulan jalan bersamaan).
 * FileSorter/rule matching/notifikasi hasil TIDAK disentuh sama sekali di
 * batch ini -- murni trigger point baru, nol logic scan baru.
 *
 * **Kenapa SENGAJA stateless** (bukan RemoteViews yang menampilkan hasil
 * scan langsung di widget, mis. "39 file dipindahkan" muncul di widget
 * itu sendiri): proses widget terpisah TOTAL dari Activity/ViewModel --
 * menyinkronkan state scan real-time ke situ butuh observer/update-loop
 * lintas-proses terpisah yang jauh lebih rawan gagal-diam, PERSIS risiko
 * yang sudah diperingatkan `ROADMAP.md` utk item 3.1 ("tidak bisa
 * diverifikasi visual sama sekali tanpa device asli, gagal-diam sulit
 * dideteksi"). Fix scope: batasi widget ke SATU tanggung jawab (trigger),
 * hasil scan tetap lewat notifikasi sistem yang SUDAH ADA & SUDAH
 * TERBUKTI jalan (`AutoSortNotification.resultNotification`, dipanggil
 * `AutoSortWorker` sendiri saat `filesMoved > 0`) -- widget cukup kasih
 * Toast instan sbg konfirmasi tap diterima, bukan sumber kebenaran hasil.
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
            // .enqueue() biasa (BUKAN enqueueUniqueWork) -- trigger manual
            // dari widget SENGAJA tidak berbagi slot unique work dgn
            // AutoSortWorker.WORK_NAME milik auto-scan periodik ([WorkScheduler]),
            // supaya tap widget tidak pernah "ditolak/di-drop" gara-gara
            // slot unique itu lagi terisi jadwal periodik. Aman dari race
            // eksekusi ganda krn scanMutex statis di FileSorter (lihat
            // javadoc class di atas).
            WorkManager.getInstance(context).enqueue(OneTimeWorkRequestBuilder<AutoSortWorker>().build())
        }
    }

    private fun updateWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
        val views = RemoteViews(context.packageName, R.layout.widget_scan)
        val scanIntent = Intent(context, ScanWidgetProvider::class.java).apply { action = ACTION_SCAN_NOW }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            appWidgetId,
            scanIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)
        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    companion object {
        const val ACTION_SCAN_NOW = "com.elprompter.promptvault.widget.ACTION_SCAN_NOW"
    }
}
