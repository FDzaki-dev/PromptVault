package com.elprompter.promptvault.worker

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.Configuration
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.testing.TestListenableWorkerBuilder
import androidx.work.testing.WorkManagerTestInitHelper
import com.elprompter.promptvault.data.SettingsRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * [Pending queue P2 #5-lanjutan, dituntaskan 2026-08-22] Test end-to-end
 * pakai WorkManager ASLI (lewat `WorkManagerTestInitHelper`, bukan mock) di
 * atas Robolectric -- baru bisa ditulis setelah `org.robolectric:robolectric`
 * & `androidx.work:work-testing` ditambah ke `testImplementation`
 * (`app/build.gradle.kts`, lihat komentar di sana). Cakupan yang SEBELUMNYA
 * tidak terjangkau [AutoSortLifecycleLogicTest] (v8.22.12, pure JVM, gate
 * decision doang): (1) `WorkScheduler` benar2 mengubah state WorkManager
 * nyata (bukan cuma keputusan boolean), (2) `AutoSortWorker.doWork()`
 * benar2 dieksekusi via `TestListenableWorkerBuilder` dan gate-nya
 * memengaruhi `Result` yang beneran dikembalikan.
 *
 * **Reboot survival** disimulasikan lewat pemanggilan LANGSUNG
 * [WorkScheduler.rescheduleFromSavedSettings] (badan kerja
 * [BootCompletedReceiver] SETELAH `goAsync()`/coroutine-nya, lihat
 * javadoc BootCompletedReceiver.kt) -- BUKAN lewat `onReceive()` itu
 * sendiri, karena `goAsync()`+`CoroutineScope(Dispatchers.IO).launch{}`
 * fire-and-forget TIDAK bisa di-await sinkron dari test tanpa hook
 * tambahan, dan `goAsync()` sendiri murni proteksi lifecycle proses OS
 * (Robolectric jalan 1 JVM, tidak pernah benar2 mematikan proses) --
 * bukan logic yang bisa salah/benar untuk diuji.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [26])
class BootSurvivalWorkManagerTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        val config = Configuration.Builder().build()
        WorkManagerTestInitHelper.initializeTestWorkManager(context, config)
    }

    private fun currentWorkInfos(): List<WorkInfo> =
        WorkManager.getInstance(context).getWorkInfosForUniqueWork(AutoSortWorker.WORK_NAME).get()

    /** Skenario reboot dgn toggle ON: worker periodik HARUS terjadwal ulang. */
    @Test
    fun `reboot with auto-sort ON reschedules periodic work`() = runBlocking {
        SettingsRepository(context).setAutoSortEnabled(true)

        WorkScheduler.rescheduleFromSavedSettings(context)

        val infos = currentWorkInfos()
        assertTrue("Worker periodik harus terjadwal setelah reboot dgn toggle ON", infos.isNotEmpty())
        assertEquals(WorkInfo.State.ENQUEUED, infos.first().state)
    }

    /** Skenario reboot dgn toggle OFF: worker periodik TIDAK boleh terjadwal. */
    @Test
    fun `reboot with auto-sort OFF does not reschedule periodic work`() = runBlocking {
        SettingsRepository(context).setAutoSortEnabled(false)

        WorkScheduler.rescheduleFromSavedSettings(context)

        val infos = currentWorkInfos()
        val active = infos.filter { it.state == WorkInfo.State.ENQUEUED || it.state == WorkInfo.State.RUNNING }
        assertTrue("Tidak boleh ada worker periodik aktif setelah reboot dgn toggle OFF", active.isEmpty())
    }

    /** Toggle OFF -> ON -> OFF lagi (reboot berulang) -- state akhir harus konsisten dgn toggle terakhir. */
    @Test
    fun `repeated reboot cycles end in state matching latest toggle`() = runBlocking {
        val repo = SettingsRepository(context)
        repo.setAutoSortEnabled(true)
        WorkScheduler.rescheduleFromSavedSettings(context)
        repo.setAutoSortEnabled(false)
        WorkScheduler.rescheduleFromSavedSettings(context)
        repo.setAutoSortEnabled(true)
        WorkScheduler.rescheduleFromSavedSettings(context)

        val infos = currentWorkInfos()
        assertTrue(infos.isNotEmpty())
        assertEquals(WorkInfo.State.ENQUEUED, infos.first().state)
    }

    /** `AutoSortWorker.doWork()` benar2 dieksekusi (bukan cuma pure-logic gate) saat toggle OFF -> no-op sukses. */
    @Test
    fun `AutoSortWorker doWork returns success no-op when auto-sort OFF`() = runBlocking {
        SettingsRepository(context).setAutoSortEnabled(false)

        val worker = TestListenableWorkerBuilder<AutoSortWorker>(context).build()
        val result = worker.doWork()

        assertEquals(androidx.work.ListenableWorker.Result.success(), result)
    }
}
