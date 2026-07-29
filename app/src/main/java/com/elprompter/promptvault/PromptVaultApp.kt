package com.elprompter.promptvault

import android.app.Application
import com.elprompter.promptvault.worker.WorkScheduler

class PromptVaultApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Pastikan auto-sort terjadwal ulang setiap kali proses app dibuat,
        // memakai interval tersimpan (item TODO #2).
        WorkScheduler.rescheduleFromSavedSettings(this)
    }
}
