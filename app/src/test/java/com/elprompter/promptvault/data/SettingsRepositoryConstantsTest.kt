package com.elprompter.promptvault.data

import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsRepositoryConstantsTest {

    @Test
    fun `all allowed intervals respect WorkManager 15 minute minimum`() {
        SettingsRepository.ALLOWED_INTERVALS.forEach { minutes ->
            assertTrue("Interval $minutes menit kurang dari batas minimum WorkManager (15 menit)", minutes >= 15)
        }
    }

    @Test
    fun `default interval is included in allowed list`() {
        assertTrue(SettingsRepository.ALLOWED_INTERVALS.contains(SettingsRepository.DEFAULT_INTERVAL_MINUTES))
    }
}
