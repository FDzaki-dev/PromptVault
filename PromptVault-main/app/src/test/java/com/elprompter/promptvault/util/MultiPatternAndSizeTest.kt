package com.elprompter.promptvault.util

import com.elprompter.promptvault.data.Rule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MultiPatternAndSizeTest {

    @Test
    fun `matchesAny returns true if any comma-separated pattern matches`() {
        assertTrue(GlobMatcher.matchesAny("receipt_01.txt", "invoice_*.zip, receipt_*.txt"))
        assertTrue(GlobMatcher.matchesAny("invoice_99.zip", "invoice_*.zip, receipt_*.txt"))
        assertFalse(GlobMatcher.matchesAny("other.pdf", "invoice_*.zip, receipt_*.txt"))
    }

    @Test
    fun `splitPatterns trims whitespace and drops blanks`() {
        val result = GlobMatcher.splitPatterns(" *.zip ,  , *.txt")
        assertEquals(listOf("*.zip", "*.txt"), result)
    }

    @Test
    fun `matchesSizeConstraint respects min and max`() {
        val rule = Rule(id = "1", folderName = "F", pattern = "*.zip", minSizeKb = 10, maxSizeKb = 100)
        assertTrue(RuleOverlapChecker.matchesSizeConstraint(50, rule))
        assertFalse(RuleOverlapChecker.matchesSizeConstraint(5, rule))
        assertFalse(RuleOverlapChecker.matchesSizeConstraint(200, rule))
    }

    @Test
    fun `matchesSizeConstraint with no bounds always passes`() {
        val rule = Rule(id = "1", folderName = "F", pattern = "*.zip")
        assertTrue(RuleOverlapChecker.matchesSizeConstraint(0, rule))
        assertTrue(RuleOverlapChecker.matchesSizeConstraint(999999, rule))
    }

    @Test
    fun `matchingRules combines multi-pattern, exclude, and size filter`() {
        val rule = Rule(
            id = "1",
            folderName = "Invoices",
            pattern = "invoice_*.zip, invoice_*.txt",
            excludePattern = "invoice_draft_*.zip",
            minSizeKb = 5,
            enabled = true
        )
        assertEquals(1, RuleOverlapChecker.matchingRules("invoice_final.zip", 10, listOf(rule)).size)
        assertTrue(RuleOverlapChecker.matchingRules("invoice_draft_final.zip", 10, listOf(rule)).isEmpty())
        assertTrue(RuleOverlapChecker.matchingRules("invoice_final.zip", 2, listOf(rule)).isEmpty())
    }
}
