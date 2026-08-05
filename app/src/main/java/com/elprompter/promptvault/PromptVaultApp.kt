package com.elprompter.promptvault

import android.app.Application
import com.elprompter.promptvault.worker.AutoSortNotification
import com.elprompter.promptvault.worker.WorkScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PromptVaultApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Batch §5: siapkan notification channel foreground-service auto-sort
        // sekali di awal proses app -- idempoten, murah, dan memastikan channel
        // sudah ada SEBELUM worker pertama kali butuh setForeground().
        AutoSortNotification.ensureChannel(this)
        // Pastikan auto-sort terjadwal ulang setiap kali proses app dibuat,
        // memakai interval tersimpan (item TODO #2). Aman fire-and-forget di
        // sini (beda dari BootCompletedReceiver) karena proses app ini sudah
        // pasti hidup selama onCreate() dan seterusnya.
        CoroutineScope(Dispatchers.IO).launch {
            WorkScheduler.rescheduleFromSavedSettings(this@PromptVaultApp)
        }
    }
}
