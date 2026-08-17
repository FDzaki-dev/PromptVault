package com.elprompter.promptvault.shizuku

import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.IBinder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import rikka.shizuku.Shizuku

/**
 * [Fitur baru 2026-08-17 -- integrasi Shizuku] Titik tunggal siklus hidup
 * binder Shizuku + [IFileOpsService] privileged -- singleton object karena
 * binder Shizuku bersifat proses-lebar (filosofi sama dengan
 * `FileSorter.scanMutex` di companion object).
 *
 * **Batas jujur (konsisten dengan seluruh riwayat kode SAF project ini,
 * lihat Insiden #7 di PROJECT_STATE.md)**: kelas ini BELUM PERNAH
 * dikompilasi/diuji di device asli -- sandbox kerja sesi ini tidak punya
 * Android SDK/Gradle/Shizuku terpasang. Ditulis seketat mungkin mengikuti
 * permukaan API publik `dev.rikka.shizuku:api` (dipakai app.build.gradle.kts),
 * TAPI tidak ada jaminan lolos compiler asli sampai `./gradlew` sungguhan
 * dijalankan. Kalau CI/build gagal di file ini, itu bukan tanda proses
 * ditulis ceroboh -- itu justru alasan kenapa batasan ini didokumentasikan
 * eksplisit di sini, bukan diklaim "pasti jalan".
 */
object ShizukuManager {

    /** Status yang ditampilkan ke user di kartu "Mode Shizuku" (SettingsScreen). */
    enum class Status { NOT_INSTALLED, NOT_RUNNING, PERMISSION_DENIED, BINDING, READY, ERROR }

    private const val REQUEST_CODE = 9100

    private val _status = MutableStateFlow(Status.NOT_INSTALLED)
    val status: StateFlow<Status> = _status.asStateFlow()

    /** `null` selama belum ter-bind -- SEMUA pemanggil (FileSorter) WAJIB null-check sebelum pakai. */
    var service: IFileOpsService? = null
        private set

    private var appContext: Context? = null
    private var listenersRegistered = false

    private val userServiceArgs by lazy {
        Shizuku.UserServiceArgs(ComponentName(appContext!!.packageName, FileOpsUserService::class.java.name))
            .daemon(false)
            .processNameSuffix("fileops")
            .debuggable(false)
            .version(1)
    }

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, binder: IBinder) {
            service = if (binder.pingBinder()) IFileOpsService.Stub.asInterface(binder) else null
            _status.value = if (service != null) Status.READY else Status.ERROR
        }

        override fun onServiceDisconnected(name: ComponentName) {
            service = null
            refreshStatus()
        }
    }

    private val binderReceivedListener = Shizuku.OnBinderReceivedListener { refreshStatus() }

    private val binderDeadListener = Shizuku.OnBinderDeadListener {
        service = null
        _status.value = Status.NOT_RUNNING
    }

    private val permissionResultListener =
        Shizuku.OnRequestPermissionResultListener { requestCode, grantResult ->
            if (requestCode == REQUEST_CODE) {
                if (grantResult == PackageManager.PERMISSION_GRANTED) bindService() else _status.value = Status.PERMISSION_DENIED
            }
        }

    /** Dipanggil SEKALI dari `PromptVaultApp.onCreate()` -- daftar listener seumur proses app. */
    fun init(context: Context) {
        if (listenersRegistered) return
        appContext = context.applicationContext
        try {
            Shizuku.addBinderReceivedListenerSticky(binderReceivedListener)
            Shizuku.addBinderDeadListener(binderDeadListener)
            Shizuku.addRequestPermissionResultListener(permissionResultListener)
            listenersRegistered = true
        } catch (e: Exception) {
            // Shizuku library gagal dimuat sepenuhnya -- status tetap NOT_INSTALLED (default),
            // kartu Pengaturan menampilkan itu apa adanya, bukan crash.
        }
        refreshStatus()
    }

    /** Dipanggil manual dari SettingsScreen (mis. resume setelah user pasang/buka app Shizuku). */
    fun refreshStatus() {
        val available = try {
            Shizuku.pingBinder()
        } catch (e: Exception) {
            false
        }
        if (!available) {
            _status.value = Status.NOT_RUNNING
            service = null
            return
        }
        val granted = try {
            Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        } catch (e: Exception) {
            false
        }
        if (!granted) {
            _status.value = Status.PERMISSION_DENIED
            return
        }
        if (service == null) bindService() else _status.value = Status.READY
    }

    fun requestPermission() {
        try {
            if (Shizuku.pingBinder() && Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
                Shizuku.requestPermission(REQUEST_CODE)
            }
        } catch (e: Exception) {
            // Shizuku tidak terpasang/tidak jalan -- diam, status sudah NOT_RUNNING dari refreshStatus().
        }
    }

    private fun bindService() {
        if (appContext == null) return
        _status.value = Status.BINDING
        try {
            Shizuku.bindUserService(userServiceArgs, serviceConnection)
        } catch (e: Exception) {
            _status.value = Status.ERROR
        }
    }
}
