package com.elprompter.promptvault.util

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * Crash logger bawaan (fail-safe, tanpa permission legacy).
 *
 * - Entry point: uncaught exception handler global, terpasang sekali di
 *   [PromptVaultApp.onCreate].
 * - Penyimpanan: MediaStore.Files (API 29+), path publik
 *   `Documents/PromptVault/logs/crash_<yyyyMMdd_HHmmss>_<uuid8>.txt`,
 *   TIDAK butuh WRITE_EXTERNAL_STORAGE (scoped storage resmi).
 * - Fail-safe: seluruh proses penulisan log dibungkus try-catch. Kalau
 *   penulisan log sendiri gagal, JANGAN sampai menutupi/menggantikan
 *   crash asli -- handler sebelumnya (default sistem) tetap selalu
 *   dipanggil di blok `finally`.
 * - Retention: FIFO, maksimal 50 file crash log tersimpan sekaligus.
 */
object CrashLogger {

    private const val RELATIVE_DIR = "Documents/PromptVault/logs/"
    private const val MAX_LOGS = 50

    /** Pasang uncaught exception handler global. Panggil sekali di Application.onCreate(). */
    fun install(context: Context) {
        val appContext = context.applicationContext
        val previousHandler = Thread.getDefaultUncaughtExceptionHandler()

        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                writeCrashLog(appContext, thread, throwable)
            } catch (loggerFailure: Throwable) {
                // Fail-safe: kegagalan logger sendiri tidak boleh menutupi
                // exception asli maupun bikin proses macet.
                Log.e("CrashLogger", "Gagal menulis crash log", loggerFailure)
            } finally {
                // Selalu teruskan ke handler default (sistem) supaya perilaku
                // crash/dialog "App berhenti" Android tetap normal.
                if (previousHandler != null) {
                    previousHandler.uncaughtException(thread, throwable)
                } else {
                    android.os.Process.killProcess(android.os.Process.myPid())
                    kotlin.system.exitProcess(10)
                }
            }
        }
    }

    private fun writeCrashLog(context: Context, thread: Thread, throwable: Throwable) {
        val fileTimestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val shortUuid = UUID.randomUUID().toString().substring(0, 8)
        val fileName = "crash_${fileTimestamp}_$shortUuid.txt"

        val versionInfo = try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            "${pInfo.versionName} (${pInfo.longVersionCode})"
        } catch (_: Exception) {
            "unknown"
        }

        val content = buildString {
            appendLine("=== PromptVault Crash Report ===")
            appendLine("Timestamp  : ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())}")
            appendLine("App Version: $versionInfo")
            appendLine("OS         : Android ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})")
            appendLine("Device     : ${Build.MANUFACTURER} ${Build.MODEL}")
            appendLine("Thread     : ${thread.name}")
            appendLine()
            appendLine("--- Stack Trace ---")
            append(Log.getStackTraceString(throwable))
        }

        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, "text/plain")
            put(MediaStore.MediaColumns.RELATIVE_PATH, RELATIVE_DIR)
        }

        val uri = resolver.insert(MediaStore.Files.getContentUri("external"), values) ?: return
        resolver.openOutputStream(uri)?.use { stream ->
            stream.write(content.toByteArray(Charsets.UTF_8))
        }

        enforceRetention(context)
    }

    /** FIFO: hapus log tertua sampai jumlah tersisa <= MAX_LOGS. */
    private fun enforceRetention(context: Context) {
        val resolver = context.contentResolver
        val collection = MediaStore.Files.getContentUri("external")
        val projection = arrayOf(MediaStore.MediaColumns._ID)
        val selection = "${MediaStore.MediaColumns.RELATIVE_PATH} = ? AND " +
            "${MediaStore.MediaColumns.DISPLAY_NAME} LIKE ?"
        val selectionArgs = arrayOf(RELATIVE_DIR, "crash_%.txt")
        val sortOrder = "${MediaStore.MediaColumns.DATE_ADDED} ASC"

        resolver.query(collection, projection, selection, selectionArgs, sortOrder)?.use { cursor ->
            val total = cursor.count
            var excess = total - MAX_LOGS
            if (excess <= 0) return
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
            while (cursor.moveToNext() && excess > 0) {
                val id = cursor.getLong(idColumn)
                val uri = ContentUris.withAppendedId(collection, id)
                resolver.delete(uri, null, null)
                excess--
            }
        }
    }

    /** Satu entri crash log tersimpan, untuk ditampilkan di Diagnostik. */
    data class CrashLogEntry(
        val uri: android.net.Uri,
        val displayName: String,
        val dateAddedEpochSeconds: Long,
        val sizeBytes: Long
    )

    /** Daftar crash log tersimpan, terbaru dulu. Aman dipanggil dari IO thread. */
    fun listLogs(context: Context): List<CrashLogEntry> {
        val resolver = context.contentResolver
        val collection = MediaStore.Files.getContentUri("external")
        val projection = arrayOf(
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.DATE_ADDED,
            MediaStore.MediaColumns.SIZE
        )
        val selection = "${MediaStore.MediaColumns.RELATIVE_PATH} = ? AND " +
            "${MediaStore.MediaColumns.DISPLAY_NAME} LIKE ?"
        val selectionArgs = arrayOf(RELATIVE_DIR, "crash_%.txt")
        val sortOrder = "${MediaStore.MediaColumns.DATE_ADDED} DESC"

        val results = mutableListOf<CrashLogEntry>()
        try {
            resolver.query(collection, projection, selection, selectionArgs, sortOrder)?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
                val nameCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
                val dateCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_ADDED)
                val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)
                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    results.add(
                        CrashLogEntry(
                            uri = ContentUris.withAppendedId(collection, id),
                            displayName = cursor.getString(nameCol),
                            dateAddedEpochSeconds = cursor.getLong(dateCol),
                            sizeBytes = cursor.getLong(sizeCol)
                        )
                    )
                }
            }
        } catch (_: Exception) {
            // Fail-safe: kalau query gagal (mis. provider tidak siap), tampilkan list kosong
            // daripada crash layar Diagnostik.
        }
        return results
    }

    /** Baca isi satu crash log sebagai teks. Aman dipanggil dari IO thread. */
    fun readLog(context: Context, uri: android.net.Uri): String {
        return try {
            context.contentResolver.openInputStream(uri)?.use { it.readBytes().toString(Charsets.UTF_8) }
                ?: "(gagal buka file)"
        } catch (e: Exception) {
            "(gagal baca file: ${e.message})"
        }
    }
}
