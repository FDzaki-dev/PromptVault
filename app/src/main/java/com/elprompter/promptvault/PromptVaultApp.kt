package com.elprompter.promptvault

import android.app.Application
import com.elprompter.promptvault.data.LegacyDataMigration
import com.elprompter.promptvault.shizuku.ShizukuManager
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
        // [Fitur baru 2026-08-17, integrasi Shizuku] Daftar listener binder
        // SEKALI seumur proses -- murah & idempoten (guard di ShizukuManager
        // sendiri), sama filosofi dgn AutoSortNotification.ensureChannel di
        // atas. TIDAK meminta izin di sini -- itu aksi eksplisit user lewat
        // tombol di kartu "Mode Shizuku" (SettingsScreen), bukan otomatis
        // saat app dibuka (konsisten dgn prinsip minta izin saat relevan,
        // lihat POST_NOTIFICATIONS di MainActivity.kt).
        ShizukuManager.init(this)
        // [Technical debt #3, dieksekusi 2026-08-13] Migrasi best-effort SEKALI
        // SEUMUR INSTALL dari data lama pre-Room v2.2.0 (kalau ada) -- lihat
        // dokumentasi lengkap soal batasan & keamanannya di LegacyDataMigration.kt.
        // Fire-and-forget aman sama seperti reschedule di bawah: idempoten
        // (guard flag), murah (no-op instan setelah kali pertama), dan proses
        // app ini sudah pasti hidup selama onCreate() dan seterusnya.
        CoroutineScope(Dispatchers.IO).launch {
            LegacyDataMigration.runIfNeeded(this@PromptVaultApp)
        }
        // Pastikan auto-sort terjadwal ulang setiap kali proses app dibuat,
        // memakai interval tersimpan (fitur lengkap). Aman fire-and-forget di
        // sini (beda dari BootCompletedReceiver) karena proses app ini sudah
        // pasti hidup selama onCreate() dan seterusnya.
        CoroutineScope(Dispatchers.IO).launch {
            WorkScheduler.rescheduleFromSavedSettings(this@PromptVaultApp)
        }
    }
}
