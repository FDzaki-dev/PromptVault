package com.elprompter.promptvault

import android.app.Application
import com.elprompter.promptvault.util.CrashLogger
import com.elprompter.promptvault.worker.AutoSortNotification
import com.elprompter.promptvault.worker.WorkScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PromptVaultApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Crash logger bawaan: pasang PALING AWAL (sebelum apapun lain bisa
        // crash) supaya semua uncaught exception dari proses app ini tertangkap.
        // Lihat util/CrashLogger.kt untuk detail (MediaStore, tanpa permission
        // legacy, FIFO retention 50 file).
        CrashLogger.install(this)
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
