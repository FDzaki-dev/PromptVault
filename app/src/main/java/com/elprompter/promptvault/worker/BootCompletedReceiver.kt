package com.elprompter.promptvault.worker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Menjadwalkan ulang WorkManager setelah reboot perangkat, agar auto-sort
 * tetap jalan tanpa harus membuka app secara manual (survive reboot).
 *
 * Batch [worker-lifecycle-fix]: pakai goAsync() supaya proses ditahan hidup
 * sampai reschedule beneran selesai (baca DataStore + enqueue WorkManager).
 * Tanpa ini, onReceive() kembali seketika dan Android boleh mematikan
 * proses App yang baru dibuat khusus untuk broadcast boot ini SEBELUM
 * coroutine sempat jalan -- auto-sort jadi tidak terjadwal ulang di
 * sebagian device/timing, padahal "survive reboot" adalah fitur inti.
 */
class BootCompletedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || intent.action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    WorkScheduler.rescheduleFromSavedSettings(context)
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
