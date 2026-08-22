package com.elprompter.promptvault.worker

import com.elprompter.promptvault.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * [Pending queue P2 #5] Cakupan pure-logic dari 7 skenario audit
 * ON/OFF/reboot/widget -- lihat javadoc [AutoSortLifecycleLogic] untuk
 * batas cakupan (reboot survival end-to-end TIDAK termasuk, tetap pending).
 */
class AutoSortLifecycleLogicTest {

    // -- Skenario 1-2: gate worker periodik (ON/OFF) --
    @Test
    fun `periodic scan runs when auto-sort ON`() {
        assertTrue(AutoSortLifecycleLogic.shouldRunPeriodicScan(autoSortEnabled = true))
    }

    @Test
    fun `periodic scan skipped when auto-sort OFF`() {
        assertFalse(AutoSortLifecycleLogic.shouldRunPeriodicScan(autoSortEnabled = false))
    }

    // -- Skenario 3: widget/manual selalu jalan, regression guard asimetri --
    @Test
    fun `manual scan always runs regardless of toggle state`() {
        assertTrue(AutoSortLifecycleLogic.shouldRunManualScan())
    }

    // -- Skenario 4-5: keputusan scheduler (dipakai startup & BootCompletedReceiver) --
    @Test
    fun `scheduler schedules work when auto-sort ON`() {
        assertTrue(AutoSortLifecycleLogic.shouldScheduleWork(autoSortEnabled = true))
    }

    @Test
    fun `scheduler cancels work when auto-sort OFF`() {
        assertFalse(AutoSortLifecycleLogic.shouldScheduleWork(autoSortEnabled = false))
    }

    // -- Skenario 6-7: judul notifikasi ongoing & hasil, manual vs periodik (v8.22.11) --
    @Test
    fun `ongoing notif title is manual variant when isManual true`() {
        assertEquals(R.string.manual_scan_notif_title, AutoSortLifecycleLogic.ongoingNotifTitleRes(isManual = true))
    }

    @Test
    fun `ongoing notif title is auto-sort variant when isManual false`() {
        assertEquals(R.string.auto_sort_notif_title, AutoSortLifecycleLogic.ongoingNotifTitleRes(isManual = false))
    }

    @Test
    fun `result notif title is manual variant when isManual true`() {
        assertEquals(R.string.manual_scan_result_notif_title, AutoSortLifecycleLogic.resultNotifTitleRes(isManual = true))
    }

    @Test
    fun `result notif title is auto-sort variant when isManual false`() {
        assertEquals(R.string.auto_sort_result_notif_title, AutoSortLifecycleLogic.resultNotifTitleRes(isManual = false))
    }
}
