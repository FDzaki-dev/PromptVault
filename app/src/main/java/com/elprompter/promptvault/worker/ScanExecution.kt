package com.elprompter.promptvault.worker

import android.content.Context
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.ListenableWorker.Result
import com.elprompter.promptvault.data.ActivityLogRepository
import com.elprompter.promptvault.data.LogLevel
import com.elprompter.promptvault.data.MoveHistoryRepository
import com.elprompter.promptvault.data.RuleRepository
import com.elprompter.promptvault.data.SettingsRepository
import com.elprompter.promptvault.R
import com.elprompter.promptvault.util.FileSorter
import com.elprompter.promptvault.widget.ScanWidgetProvider
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * [FIX WAJIB "Auto-Sort vs Manual Scan", 2026-08-21] Bug: `ScanWidgetProvider`
 * enqueue `AutoSortWorker`, yang punya gate `autoSortEnabled` -- akibatnya
 * "Auto-Sort OFF" ikut memblokir tombol widget "Scan Sekarang" (harusnya
 * manual scan SELALU jalan terlepas status auto-sort).
 *
 * Fix: PISAHKAN entry point (gate vs tidak), BUKAN sorting engine. Fungsi
 * ini adalah badan kerja scan+lapor yang PERSIS SAMA dgn isi lama
 * `AutoSortWorker.doWork()` (FileSorter/notifikasi/error-handling TIDAK
 * diubah SAMA SEKALI, murni extract-function) -- dipakai ULANG oleh:
 * - [AutoSortWorker]: setelah gate `autoSortEnabled` (auto-scan periodik).
 * - [ManualScanWorker]: TANPA gate sama sekali (widget & manual lain).
 *
 * Extension function di `CoroutineWorker` (bukan top-level biasa) krn
 * `setForeground()` adalah method instance worker -- BUKAN duplikasi, cuma
 * cara Kotlin berbagi kode yang butuh akses ke instance pemanggil.
 *
 * TIDAK menyentuh: FileSorter, SAF, Shizuku, Rule Engine, scheduler
 * Auto-Sort ([WorkScheduler]), atau sorting logic apa pun -- sesuai scope.
 *
 * [Fix audit P2 #4, 2026-08-22] Parameter [isManual] baru -- dulu
 * notifikasi ongoing/hasil SELALU pakai wording "Auto-sort..." walau
 * dipicu manual (widget/ManualScanWorker), audit user konfirmasi ini
 * salah semantik. Diteruskan APA ADANYA ke [AutoSortNotification], TIDAK
 * mempengaruhi FileSorter/scan logic sama sekali -- murni pilihan string
 * title notifikasi.
 */
internal suspend fun CoroutineWorker.runScanAndReport(applicationContext: Context, isManual: Boolean): Result {
    return try {
        // Batch §5: promosikan ke foreground service SEBELUM scan mulai, supaya
        // OS tidak menjeda/membunuh worker di tengah scan panjang (lihat
        // AutoSortNotification.kt untuk alasan lengkap). Best-effort: kalau
        // sistem menolak (skenario tak terduga di sebagian OEM), scan
        // TETAP lanjut jalan sebagai background worker biasa -- jangan sampai
        // kegagalan promosi foreground menggagalkan seluruh proses sortir.
        try {
            setForeground(AutoSortNotification.foregroundInfo(applicationContext, isManual))
        } catch (e: Exception) {
            // sengaja ditelan -- lihat komentar di atas
        }
        val moveHistoryRepository = MoveHistoryRepository(applicationContext)
        val sorter = FileSorter(
            context = applicationContext,
            ruleRepository = RuleRepository(applicationContext),
            activityLogRepository = ActivityLogRepository(applicationContext),
            moveHistoryRepository = moveHistoryRepository,
            settingsRepository = SettingsRepository(applicationContext)
        )
        // [Fase 2.2 roadmap, 2026-08-21] Timestamp SEBELUM scan mulai --
        // dipakai setelah scan buat ambil "entri mana saja yang baru
        // ditulis scan INI" dari MoveHistoryRepository (bukan sejak awal
        // waktu), lihat pemakaiannya di bawah.
        val scanStartMillis = System.currentTimeMillis()
        val result = sorter.scanAndSort()
        // [PENDING QUEUE #1, v8.22.1 -> dieksekusi 2026-08-22] Widget dulu
        // 100% stateless -- teks tidak pernah berubah walau scan selesai
        // (auto-sort ATAU tap widget manual). Push ringkasan ke RemoteViews
        // TIAP scan (termasuk 0 file, beda dari notifikasi sistem di bawah
        // yang sengaja hanya muncul kalau filesMoved>0 -- widget ini
        // konfirmasi "scan barusan jalan", bukan alert seperti notifikasi).
        // Best-effort murni: kegagalan simpan/push TIDAK BOLEH menggagalkan
        // scan yang sudah sukses memindahkan file.
        try {
            val timeLabel = SimpleDateFormat("HH:mm", Locale("id", "ID")).format(Date())
            val widgetSummary = applicationContext.getString(
                R.string.widget_scan_last_result_fmt,
                result.filesMoved,
                timeLabel
            )
            SettingsRepository(applicationContext).setWidgetLastScanSummary(widgetSummary)
            ScanWidgetProvider.notifyScanCompleted(applicationContext, widgetSummary)
        } catch (e: Exception) {
            // sengaja ditelan -- lihat komentar di atas
        }
        // Notifikasi hasil HANYA kalau benar-benar ada file dipindah --
        // sengaja TIDAK notif tiap siklus (auto-scan tiap 240 menit default
        // ATAU tap widget) kalau nihil, supaya tidak jadi notification fatigue.
        // Detail "0 dipindahkan" tetap tercatat di Log Aktivitas seperti
        // biasa (FileSorter yang urus itu, tidak berubah di sini).
        if (result.filesMoved > 0) {
            try {
                // [perf] historyFlow.first() ambil snapshot SEKALI (bukan
                // collect terus), sama pola dgn cara MainViewModel baca
                // Flow one-shot lain di project ini. Filter by
                // timestampMillis >= scanStartMillis -- cara termurah
                // dapat "entri dari scan ini saja" TANPA nambah kolom/
                // query baru di MoveHistoryDao (Protected Asset, DB
                // Schema/DAO -- dihindari kalau ada cara lain yang aman).
                // Caveat jujur: kalau scan lain kebetulan jalan di window
                // waktu yang SAMA PERSIS (praktis mustahil, scanMutex sudah
                // cegah 2 scan beriringan), breakdown per-rule bisa sedikit
                // meleset dari [result.filesMoved] -- total di judul
                // notifikasi TETAP dari [result] (sumber kebenaran asli),
                // cuma baris per-rule yang derived best-effort.
                val perRule = moveHistoryRepository.historyFlow.first()
                    .filter { it.timestampMillis >= scanStartMillis }
                    .groupingBy { it.ruleFolderName }
                    .eachCount()
                NotificationManagerCompat.from(applicationContext).notify(
                    AutoSortNotification.RESULT_NOTIFICATION_ID,
                    AutoSortNotification.resultNotification(applicationContext, result.filesMoved, perRule, isManual)
                )
            } catch (e: SecurityException) {
                // Izin POST_NOTIFICATIONS dicabut runtime (Android 13+) --
                // best-effort, jangan gagalkan hasil scan yang sudah sukses.
            } catch (e: Exception) {
                // Kegagalan notifikasi hasil (device aneh, dll) TIDAK BOLEH
                // menggagalkan Result.success() -- file sudah benar-benar
                // tersortir, itu yang utama.
            }
        }
        Result.success()
    } catch (e: Exception) {
        // Batch [worker-lifecycle-fix]: sebelumnya SEMUA exception di sini
        // ditelan diam-diam lalu selalu Result.retry() tanpa batas -- kalau
        // penyebabnya PERMANEN (mis. izin MANAGE_EXTERNAL_STORAGE dicabut
        // user dari Setelan Android), worker akan retry berulang setiap
        // periode SELAMANYA tanpa pernah berhasil, boros baterai, dan user
        // TIDAK PERNAH tahu kenapa karena tidak ada satu pun baris di Log
        // Aktivitas. Sekarang: (1) selalu dicatat ke Log Aktivitas dulu,
        // supaya kegagalan level-worker (bukan per-file) tetap kelihatan;
        // (2) SecurityException (khas izin dicabut) dianggap PERMANEN ->
        // Result.failure(), tidak retry sia-sia. Error lain (mis. I/O
        // sementara) tetap Result.retry() seperti semula.
        try {
            ActivityLogRepository(applicationContext).add(
                LogLevel.ERROR,
                "Scan gagal dijalankan: ${e.javaClass.simpleName} - ${e.message ?: "tanpa pesan"}"
            )
        } catch (_: Exception) {
            // Kalau mencatat log pun gagal (mis. DB korup), jangan sampai
            // menutupi exception asli dengan crash baru -- lanjut ke Result di bawah.
        }
        if (e is SecurityException) Result.failure() else Result.retry()
    }
}
