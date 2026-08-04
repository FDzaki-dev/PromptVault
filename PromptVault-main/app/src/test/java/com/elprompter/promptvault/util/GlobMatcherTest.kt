package com.elprompter.promptvault.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GlobMatcherTest {

    @Test
    fun `star wildcard matches any characters`() {
        assertTrue(GlobMatcher.matches("laporan_2026.zip", "laporan_*.zip"))
        assertTrue(GlobMatcher.matches("laporan_.zip", "laporan_*.zip"))
        assertFalse(GlobMatcher.matches("invoice_2026.zip", "laporan_*.zip"))
    }

    @Test
    fun `question mark matches exactly one character`() {
        assertTrue(GlobMatcher.matches("a.txt", "?.txt"))
        assertFalse(GlobMatcher.matches("ab.txt", "?.txt"))
    }

    @Test
    fun `matching is case insensitive`() {
        assertTrue(GlobMatcher.matches("REPORT.TXT", "*.txt"))
    }

    @Test
    fun `pure extension wildcard matches everything with that extension`() {
        assertTrue(GlobMatcher.matches("anything_at_all.zip", "*.zip"))
    }

    @Test
    fun `special regex characters in pattern are escaped literally`() {
        assertTrue(GlobMatcher.matches("data (1).zip", "data (1).zip"))
        assertFalse(GlobMatcher.matches("data 1.zip", "data (1).zip"))
    }

    @Test
    fun `patternsCanOverlap detects wildcard extension overlap`() {
        assertTrue(GlobMatcher.patternsCanOverlap("*.zip", "report_*.zip"))
    }

    @Test
    fun `patternsCanOverlap returns false for unrelated patterns`() {
        assertFalse(GlobMatcher.patternsCanOverlap("*.zip", "*.txt"))
    }

    @Test
    fun `patternsCanOverlap detects identical patterns`() {
        assertTrue(GlobMatcher.patternsCanOverlap("invoice_*.zip", "invoice_*.zip"))
    }
}
