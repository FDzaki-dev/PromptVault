package com.elprompter.promptvault.worker

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
 */
object AutoSortNotification {
    const val CHANNEL_ID = "auto_sort_channel"
    const val NOTIFICATION_ID = 1001

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

    fun foregroundInfo(context: Context): ForegroundInfo {
        ensureChannel(context)
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle(context.getString(R.string.auto_sort_notif_title))
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
}
