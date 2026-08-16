package com.elprompter.promptvault.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * [Fix P0-1, audit gap 2026-08-16] Regresi langsung untuk temuan
 * "Rule folder name is not safely validated" -- lihat KDoc lengkap di
 * RuleFolderNameValidator.kt. Kasus paling penting: nama yang mengandung
 * `/`, `\`, atau `..` HARUS ditolak, karena itulah vektor path-traversal
 * yang disebut eksplisit di audit.
 */
class RuleFolderNameValidatorTest {

    @Test
    fun `normal folder name is valid`() {
        assertNull(validateRuleFolderName("Invoice"))
        assertNull(validateRuleFolderName("Laporan 2026"))
        assertTrue(isValidRuleFolderName("Laporan-Bulanan_v2"))
    }

    @Test
    fun `blank or whitespace only name is rejected`() {
        assertNotNull(validateRuleFolderName(""))
        assertNotNull(validateRuleFolderName("   "))
        assertFalse(isValidRuleFolderName(""))
    }

    @Test
    fun `dot and dotdot are rejected`() {
        assertNotNull(validateRuleFolderName("."))
        assertNotNull(validateRuleFolderName(".."))
    }

    @Test
    fun `path traversal via separators is rejected`() {
        // Kasus persis yang disebut audit: nama rule dipakai langsung di
        // File(destDir, rule.folderName) -- kalau lolos, bisa keluar dari
        // boundary folder PromptVault yang dimaksud.
        assertNotNull(validateRuleFolderName("../../etc"))
        assertNotNull(validateRuleFolderName("a/b"))
        assertNotNull(validateRuleFolderName("a\\b"))
        assertNotNull(validateRuleFolderName("/absolute"))
    }

    @Test
    fun `provider-unsafe characters are rejected`() {
        for (c in listOf(':', '*', '?', '"', '<', '>', '|')) {
            assertNotNull("char '$c' should be rejected", validateRuleFolderName("name${c}x"))
        }
    }

    @Test
    fun `surrounding whitespace is trimmed before validation`() {
        assertNull(validateRuleFolderName("  Invoice  "))
    }

    @Test
    fun `isContainedIn accepts direct child and rejects sibling or escape`() {
        val root = File("/data/downloads/PromptVault")
        val child = File(root, "Invoice")
        val sibling = File("/data/downloads/PromptVaultEvil")
        val escaped = File(root, "../../etc")

        assertTrue(isContainedIn(child, root))
        assertFalse(isContainedIn(sibling, root))
        assertFalse(isContainedIn(escaped, root))
    }
}
