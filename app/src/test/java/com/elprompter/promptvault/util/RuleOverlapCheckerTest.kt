package com.elprompter.promptvault.util

import com.elprompter.promptvault.data.Rule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RuleOverlapCheckerTest {

    private fun rule(id: String, folder: String, pattern: String, enabled: Boolean = true) =
        Rule(id = id, folderName = folder, pattern = pattern, enabled = enabled)

    @Test
    fun `findOverlaps flags a pure wildcard rule against a narrower rule`() {
        val candidate = rule("1", "Semua Zip", "*.zip")
        val others = listOf(rule("2", "Invoice", "invoice_*.zip"))
        val overlaps = RuleOverlapChecker.findOverlaps(candidate, others)
        assertEquals(1, overlaps.size)
    }

    @Test
    fun `findOverlaps ignores disabled rules`() {
        val candidate = rule("1", "Semua Zip", "*.zip")
        val others = listOf(rule("2", "Invoice", "invoice_*.zip", enabled = false))
        val overlaps = RuleOverlapChecker.findOverlaps(candidate, others)
        assertTrue(overlaps.isEmpty())
    }

    @Test
    fun `matchingRules returns every enabled rule matching a real file name`() {
        val rules = listOf(
            rule("1", "Semua Zip", "*.zip"),
            rule("2", "Invoice", "invoice_*.zip"),
            rule("3", "Txt", "*.txt")
        )
        val matches = RuleOverlapChecker.matchingRules("invoice_july.zip", 100, rules)
        assertEquals(2, matches.size)
    }

    @Test
    fun `matchingRules returns empty list when nothing matches`() {
        val rules = listOf(rule("1", "Txt", "*.txt"))
        val matches = RuleOverlapChecker.matchingRules("invoice_july.zip", 100, rules)
        assertTrue(matches.isEmpty())
    }
}
