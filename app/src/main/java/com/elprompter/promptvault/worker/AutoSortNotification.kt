package com.elprompter.promptvault.worker

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.ForegroundInfo
import com.elprompter.promptvault.R

/**
 * Batch §5 (roadmap backend "Coroutine lifecycle & Foreground Service").
 *
 * Masalah yang diperbaiki: sebelum ini, AutoSortWorker (CoroutineWorker
 * periodic via WorkManager) jalan murni sebagai background worker biasa.
 * Di Android 12+ (API 31+) sistem punya batasan eksekusi background yang
 * lebih agresif -- worker yang jalan lama (scan ratusan file, tiap file ada
 * stability-check 1 detik, lihat FileSorter.kt) beresiko dijeda/dibunuh
 * OS di device yang agresif membatasi baterai (umum di custom ROM seperti
 * XOS/Infinix yang jadi device utama user), TANPA notifikasi apapun ke user
 * kenapa auto-sort kadang tidak selesai.
 *
 * Fix: promosikan worker ke foreground service (`setForeground()`) selama
 * scan berjalan -- notifikasi ongoing level rendah (IMPORTANCE_LOW, tanpa
 * suara) yang kasih user visibility ("Auto-sort sedang berjalan") DAN kasih
 * proses prioritas lebih tinggi di mata OS supaya tidak gampang dijeda.
 * Notifikasi otomatis hilang begitu doWork() selesai (WorkManager yang urus
 * lifecycle-nya, bukan manual di sini).
 *
 * [Fase 2.2 roadmap, 2026-08-21] Ditambah [resultNotification] -- notifikasi
 * HASIL (post-scan), TERPISAH dari notifikasi ongoing di atas (yang otomatis
 * hilang begitu doWork() selesai). ID beda ([RESULT_NOTIFICATION_ID]) supaya
 * tidak saling menimpa & tetap kelihatan di tray setelah scan tuntas. Channel
 * SAMA ([CHANNEL_ID], IMPORTANCE_LOW, silent) -- bukan notifikasi urgent,
 * tidak perlu bunyi/getar terpisah. Dipanggil dari [AutoSortWorker] SETELAH
 * `scanAndSort()` selesai, HANYA kalau ada file yang benar-benar dipindah
 * (lihat komentar di pemanggilnya kenapa 0-file di-skip, bukan bug).
 */
object AutoSortNotification {
    const val CHANNEL_ID = "auto_sort_channel"
    const val NOTIFICATION_ID = 1001
    const val RESULT_NOTIFICATION_ID = 1002

    /** Idempoten -- aman dipanggil berkali-kali (mis. dari Application.onCreate() DAN dari worker). */
    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.auto_sort_channel_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = context.getString(R.string.auto_sort_channel_desc)
            setShowBadge(false)
        }
        manager.createNotificationChannel(channel)
    }

    fun foregroundInfo(context: Context, isManual: Boolean = false): ForegroundInfo {
        ensureChannel(context)
        // [Fix audit P2 #4, 2026-08-22] Notifikasi ini dipakai BERSAMA oleh
        // AutoSortWorker (periodik) & ManualScanWorker (widget/manual) lewat
        // `runScanAndReport` yang di-share -- sebelumnya title SELALU
        // "Auto-sort berjalan" walau scan-nya dipicu manual (audit user:
        // "Notifikasi Manual Scan salah semantik"). `isManual` pilih string
        // title yang sesuai; `text` di bawah TIDAK berubah (sudah generik
        // sejak awal, "PromptVault sedang menyortir file di Downloads" tidak
        // pernah sebut kata "Auto-sort").
        val title = AutoSortLifecycleLogic.ongoingNotifTitleRes(isManual)
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle(context.getString(title))
            .setContentText(context.getString(R.string.auto_sort_notif_text))
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        // foregroundServiceType wajib dilampirkan sejak API 29 (dipakai OS API 34+
        // untuk validasi izin FOREGROUND_SERVICE_DATA_SYNC di manifest), tapi
        // constructor 3-argumen ini aman dipanggil di semua minSdk -- nilainya
        // cuma diabaikan library di device API < 29.
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            ForegroundInfo(NOTIFICATION_ID, notification)
        }
    }

    /**
     * Notifikasi hasil scan, dgn ringkasan per-rule (bukan cuma total).
     * [perRule] = nama folder rule -> jumlah file yang masuk ke situ, diambil
     * caller dari [com.elprompter.promptvault.data.MoveHistoryRepository]
     * (bukan diteruskan manual per-file dari [com.elprompter.promptvault.util.FileSorter]
     * -- lihat komentar di [AutoSortWorker], pola sama dgn `computeHomeStats()`
     * v8.17.0: sumber data existing yang sudah bersih, bukan pipa baru).
     */
    fun resultNotification(context: Context, totalMoved: Int, perRule: Map<String, Int>, isManual: Boolean = false): Notification {
        ensureChannel(context)
        val summary = context.getString(R.string.auto_sort_result_notif_text, totalMoved)
        // Baris per-rule diurutkan by count DESC (rule paling "sibuk" duluan) --
        // lebih informatif drpd urutan Map yang tidak terjamin stabil.
        val breakdown = perRule.entries
            .sortedByDescending { it.value }
            .joinToString("\n") { (folder, count) ->
                context.getString(R.string.auto_sort_result_notif_rule_line, folder, count)
            }
        val style = NotificationCompat.BigTextStyle()
            .bigText(breakdown.ifEmpty { summary })
            .setSummaryText(summary)
        // [Fix audit P2 #4, 2026-08-22] Sama seperti foregroundInfo di atas --
        // title generik "Scan selesai" utk jalur manual, "Auto-sort selesai"
        // tetap dipakai jalur periodik. `summary`/`breakdown` di atas TIDAK
        // berubah (sudah generik, tidak sebut "Auto-sort").
        val title = AutoSortLifecycleLogic.resultNotifTitleRes(isManual)
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle(context.getString(title))
            .setContentText(summary)
            .setStyle(style)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(false)
            .setAutoCancel(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
}
