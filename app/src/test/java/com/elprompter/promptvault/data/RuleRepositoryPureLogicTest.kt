package com.elprompter.promptvault.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * [Fix audit P2 #3, 2026-08-22] Regresi untuk [isValidImportedRule] --
 * gerbang validasi baru sebelum rule hasil import JSON di-persist. Pola
 * sama dgn `FileSorterPureLogicTest` (fungsi top-level pure, JVM biasa,
 * tanpa Context/DataStore).
 */
class RuleRepositoryPureLogicTest {

    private fun validRule(
        id: String = "1",
        folderName: String = "Gambar",
        pattern: String = "*.jpg",
        minSizeKb: Long? = null,
        maxSizeKb: Long? = null
    ) = Rule(id = id, folderName = folderName, pattern = pattern, minSizeKb = minSizeKb, maxSizeKb = maxSizeKb)

    @Test
    fun `well-formed rule passes`() {
        assertTrue(isValidImportedRule(validRule()))
    }

    @Test
    fun `blank id is rejected`() {
        assertFalse(isValidImportedRule(validRule(id = "")))
    }

    @Test
    fun `blank pattern is rejected`() {
        assertFalse(isValidImportedRule(validRule(pattern = "")))
    }

    @Test
    fun `path traversal folder name is rejected`() {
        assertFalse(isValidImportedRule(validRule(folderName = "../../etc")))
        assertFalse(isValidImportedRule(validRule(folderName = "a/b")))
        assertFalse(isValidImportedRule(validRule(folderName = "..")))
    }

    @Test
    fun `blank folder name is rejected`() {
        assertFalse(isValidImportedRule(validRule(folderName = "  ")))
    }

    @Test
    fun `negative size bounds are rejected`() {
        assertFalse(isValidImportedRule(validRule(minSizeKb = -1)))
        assertFalse(isValidImportedRule(validRule(maxSizeKb = -1)))
    }

    @Test
    fun `min greater than max is rejected`() {
        assertFalse(isValidImportedRule(validRule(minSizeKb = 500, maxSizeKb = 100)))
    }

    @Test
    fun `min equal to max is accepted`() {
        assertTrue(isValidImportedRule(validRule(minSizeKb = 100, maxSizeKb = 100)))
    }

    @Test
    fun `null size bounds are accepted`() {
        assertTrue(isValidImportedRule(validRule(minSizeKb = null, maxSizeKb = null)))
    }
}
