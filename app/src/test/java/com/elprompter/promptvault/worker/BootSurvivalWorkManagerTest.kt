package com.elprompter.promptvault.worker

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.Configuration
import androidx.work.ListenableWorker
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.testing.SynchronousExecutor
import androidx.work.testing.TestListenableWorkerBuilder
import androidx.work.testing.WorkManagerTestInitHelper
import com.elprompter.promptvault.data.SettingsRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * [Pending queue P2 #5-lanjutan, re-add setelah rollback v8.22.15] 4 skenario
 * end-to-end WorkManager ASLI (bukan mock, [SynchronousExecutor] biar jalan
 * sinkron di JVM test) yang TIDAK terjangkau pure-logic test
 * ([AutoSortLifecycleLogicTest]) -- lihat javadoc [AutoSortLifecycleLogic]
 * untuk pembagian cakupan.
 *
 * Reboot disimulasikan LANGSUNG panggil [WorkScheduler.rescheduleFromSavedSettings]
 * (badan kerja [BootCompletedReceiver] SETELAH `goAsync()`), BUKAN lewat
 * `onReceive()` itu sendiri -- `goAsync()`+coroutine fire-and-forget tidak
 * bisa di-await sinkron dari test, dan itu sendiri murni proteksi lifecycle
 * proses OS (Robolectric 1 JVM, tidak pernah benar2 matikan proses), bukan
 * logic yang perlu diuji benar/salah.
 *
 * [Fix OOM v8.22.15/v8.22.15b] SATU-SATUNYA file Robolectric di project ini
 * -- CI (`build.yml`) sengaja cuma jalankan `testDebugUnitTest` (bukan +
 * `testReleaseUnitTest` paralel), dan `testOptions.unitTests.all{}`
 * (`app/build.gradle.kts`) set `maxParallelForks=1`+`maxHeapSize` eksplisit.
 * Percobaan pertama (v8.22.14) OOM karena 2 JVM Robolectric jalan bersamaan
 * di runner CI terbatas -- 2 mitigasi di atas ADALAH fix-nya, bukan
 * ditambal di sini.
 */
@RunWith(RobolectricTestRunner::class)
class BootSurvivalWorkManagerTest {

    private lateinit var context: Context
    private lateinit var workManager: WorkManager
    private lateinit var settingsRepository: SettingsRepository

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        val config = Configuration.Builder()
            .setExecutor(SynchronousExecutor())
            .build()
        WorkManagerTestInitHelper.initializeTestWorkManager(context, config)
        workManager = WorkManager.getInstance(context)
        settingsRepository = SettingsRepository(context)
    }

    // -- Skenario 1: reboot dgn toggle ON -> worker periodik ENQUEUED --
    @Test
    fun `reboot with toggle ON reschedules periodic worker`() = runBlocking {
        settingsRepository.setAutoSortEnabled(true)

        WorkScheduler.rescheduleFromSavedSettings(context)

        val infos = workManager.getWorkInfosForUniqueWork(AutoSortWorker.WORK_NAME).get()
        assertTrue("Expected periodic work to be enqueued", infos.isNotEmpty())
        assertEquals(WorkInfo.State.ENQUEUED, infos.first().state)
    }

    // -- Skenario 2: reboot dgn toggle OFF -> tidak ada worker aktif --
    @Test
    fun `reboot with toggle OFF leaves no active worker`() = runBlocking {
        settingsRepository.setAutoSortEnabled(false)

        WorkScheduler.rescheduleFromSavedSettings(context)

        val infos = workManager.getWorkInfosForUniqueWork(AutoSortWorker.WORK_NAME).get()
        val hasActiveWork = infos.any {
            it.state == WorkInfo.State.ENQUEUED || it.state == WorkInfo.State.RUNNING
        }
        assertFalse("Expected no enqueued/running work when toggle is OFF", hasActiveWork)
    }

    // -- Skenario 3: reboot berulang ON->OFF->ON -> state akhir konsisten --
    @Test
    fun `repeated reboot ON then OFF then ON ends consistent with last toggle`() = runBlocking {
        settingsRepository.setAutoSortEnabled(true)
        WorkScheduler.rescheduleFromSavedSettings(context)

        settingsRepository.setAutoSortEnabled(false)
        WorkScheduler.rescheduleFromSavedSettings(context)

        settingsRepository.setAutoSortEnabled(true)
        WorkScheduler.rescheduleFromSavedSettings(context)

        val infos = workManager.getWorkInfosForUniqueWork(AutoSortWorker.WORK_NAME).get()
        assertTrue("Expected periodic work to be enqueued after final ON", infos.isNotEmpty())
        assertEquals(WorkInfo.State.ENQUEUED, infos.first().state)
    }

    // -- Skenario 4: AutoSortWorker.doWork() BENAR-BENAR dieksekusi saat --
    // -- toggle OFF -> no-op nyata, bukan cuma gate pure-logic --
    @Test
    fun `AutoSortWorker doWork is a real no-op success when toggle is OFF`() = runBlocking {
        settingsRepository.setAutoSortEnabled(false)

        val worker = TestListenableWorkerBuilder<AutoSortWorker>(context).build()
        val result = worker.doWork()

        assertTrue(
            "Expected Result.Success (no-op), got $result",
            result is ListenableWorker.Result.Success
        )
    }
}
