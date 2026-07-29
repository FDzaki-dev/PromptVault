package com.elprompter.promptvault.worker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Menjadwalkan ulang WorkManager setelah reboot perangkat, agar auto-sort
 * tetap jalan tanpa harus membuka app secara manual (survive reboot).
 */
class BootCompletedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || intent.action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            WorkScheduler.rescheduleFromSavedSettings(context)
        }
    }
}
