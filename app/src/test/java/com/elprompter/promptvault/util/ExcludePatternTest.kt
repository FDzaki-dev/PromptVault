package com.elprompter.promptvault.util

import com.elprompter.promptvault.data.Rule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExcludePatternTest {

    private fun rule(id: String, folder: String, pattern: String, exclude: String = "") =
        Rule(id = id, folderName = folder, pattern = pattern, excludePattern = exclude, enabled = true)

    @Test
    fun `file matching exclude pattern is not returned by matchingRules`() {
        val rules = listOf(rule("1", "Semua Zip", "*.zip", exclude = "backup_*.zip"))
        val matches = RuleOverlapChecker.matchingRules("backup_2026.zip", 100, rules)
        assertTrue(matches.isEmpty())
    }

    @Test
    fun `file not matching exclude pattern is still returned`() {
        val rules = listOf(rule("1", "Semua Zip", "*.zip", exclude = "backup_*.zip"))
        val matches = RuleOverlapChecker.matchingRules("report_2026.zip", 100, rules)
        assertEquals(1, matches.size)
    }

    @Test
    fun `isExcluded is false when excludePattern is blank`() {
        val r = rule("1", "Semua Zip", "*.zip")
        assertTrue(!RuleOverlapChecker.isExcluded("anything.zip", r))
    }

    @Test
    fun `isExcluded is true when file matches excludePattern`() {
        val r = rule("1", "Semua Zip", "*.zip", exclude = "temp_*.zip")
        assertTrue(RuleOverlapChecker.isExcluded("temp_123.zip", r))
    }
}
