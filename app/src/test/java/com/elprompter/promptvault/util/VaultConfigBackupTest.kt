package com.elprompter.promptvault.util

import com.elprompter.promptvault.data.ActivityLogEntry
import com.elprompter.promptvault.data.LogLevel
import com.elprompter.promptvault.data.MoveHistoryEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * [Fitur baru 2026-08-18, "selamatkan uninstall"] Regresi untuk 2 fungsi
 * pure logic [VaultConfigBackup] -- I/O DocumentFile (writeBackup/tryReadBackup)
 * butuh Android instrumentation, TIDAK ditest di sini (konsisten pola project:
 * hanya logika murni yang di-unit-test, lihat FileSorterPureLogicTest.kt).
 */
class VaultConfigBackupTest {

    private fun payload(
        rulesJson: String = "[]",
        log: List<ActivityLogEntry> = emptyList(),
        history: List<MoveHistoryEntry> = emptyList()
    ) = VaultConfigBackup.buildPayload(
        appVersionName = "8.4.0",
        rulesJson = rulesJson,
        intervalMinutes = 15,
        conflictStrategy = "RENAME",
        scanConcurrency = 6,
        log = log,
        history = history
    )

    @Test
    fun `empty payload is not worth offering`() {
        assertFalse(VaultConfigBackup.isPayloadWorthOffering(payload()))
        assertFalse(VaultConfigBackup.isPayloadWorthOffering(payload(rulesJson = "")))
    }

    @Test
    fun `payload with rules is worth offering`() {
        val rulesJson = """[{"id":"1","folderName":"Invoice","pattern":"*.zip","excludePattern":"","enabled":true}]"""
        assertTrue(VaultConfigBackup.isPayloadWorthOffering(payload(rulesJson = rulesJson)))
    }

    @Test
    fun `payload with only log or history is still worth offering`() {
        val log = listOf(ActivityLogEntry("1", 1000L, LogLevel.INFO, "test"))
        assertTrue(VaultConfigBackup.isPayloadWorthOffering(payload(log = log)))

        val history = listOf(MoveHistoryEntry("1", 1000L, "a.zip", "/x", "/y", "Invoice"))
        assertTrue(VaultConfigBackup.isPayloadWorthOffering(payload(history = history)))
    }

    @Test
    fun `countRules counts valid json array`() {
        val rulesJson = """[{"id":"1","folderName":"Invoice","pattern":"*.zip","excludePattern":"","enabled":true},
            |{"id":"2","folderName":"Receipt","pattern":"*.txt","excludePattern":"","enabled":false}]""".trimMargin()
        assertEquals(2, VaultConfigBackup.countRules(rulesJson))
    }

    @Test
    fun `countRules returns 0 for empty or malformed json`() {
        assertEquals(0, VaultConfigBackup.countRules("[]"))
        assertEquals(0, VaultConfigBackup.countRules(""))
        assertEquals(0, VaultConfigBackup.countRules("not json"))
    }

    @Test
    fun `buildPayload caps log and history to 200 most recent entries`() {
        val log = (1..250).map { ActivityLogEntry(it.toString(), it.toLong(), LogLevel.INFO, "msg $it") }
        val built = payload(log = log)
        assertEquals(200, built.activityLog.size)
        // Terbaru (timestamp tertinggi) yang dipertahankan, bukan yang tertua.
        assertEquals(250L, built.activityLog.first().timestampMillis)
    }
}
