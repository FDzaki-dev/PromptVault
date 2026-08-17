package com.elprompter.promptvault.util

import com.elprompter.promptvault.data.Rule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * [Fase 1.1 roadmap, 2026-08-18] Regresi untuk 4 fungsi pure yang diekstrak
 * MURNI (perilaku 100% identik, no-op refactor) dari `FileSorter.kt` --
 * pola yang sama persis dengan `MimeTypeForFileNameTest` (fungsi top-level,
 * unit test JVM biasa, TANPA Context/Robolectric). Logika inti pemindahan
 * file (`FileSorter.moveFile`, `scanAndSort`, dll -- yang butuh
 * `android.content.Context`/SAF/Shizuku nyata) SENGAJA TIDAK ikut di sini,
 * itu tetap butuh device asli/CI seperti sebelumnya (lihat MAINTENANCE.md).
 */
class FileSorterPureLogicTest {

    // ---- isTempOrPartialName ----

    @Test
    fun `known temp markers detected regardless of case`() {
        assertTrue(isTempOrPartialName("video.crdownload"))
        assertTrue(isTempOrPartialName("VIDEO.CRDOWNLOAD"))
        assertTrue(isTempOrPartialName("arsip.zip.tmp"))
        assertTrue(isTempOrPartialName("berkas.part"))
        assertTrue(isTempOrPartialName("berkas.download"))
        assertTrue(isTempOrPartialName("berkas.downloading"))
    }

    @Test
    fun `normal completed file names are not flagged as temp`() {
        assertFalse(isTempOrPartialName("laporan.pdf"))
        assertFalse(isTempOrPartialName("foto.jpg"))
        // Nama yang MENGANDUNG kata "tmp" tapi tidak DIAKHIRI marker -- harus
        // tetap lolos (endsWith, bukan contains).
        assertFalse(isTempOrPartialName("tmp_report_final.pdf"))
    }

    // ---- explainNoMatchByName ----

    @Test
    fun `excluded by excludePattern reports the specific rule`() {
        val rule = Rule(id = "1", folderName = "Invoice", pattern = "invoice_*.pdf", excludePattern = "invoice_draft_*.pdf")
        val reason = explainNoMatchByName("invoice_draft_agustus.pdf", sizeKb = 100, rules = listOf(rule))
        assertTrue(reason.contains("dikecualikan"))
        assertTrue(reason.contains("Invoice"))
    }

    @Test
    fun `size mismatch reports the configured range`() {
        val rule = Rule(id = "1", folderName = "Video Besar", pattern = "*.mp4", minSizeKb = 500_000)
        val reason = explainNoMatchByName("klip_pendek.mp4", sizeKb = 100, rules = listOf(rule))
        assertTrue(reason.contains("Video Besar"))
        assertTrue(reason.contains("min 500000KB"))
    }

    @Test
    fun `no matching pattern at all lists the active patterns`() {
        val rules = listOf(
            Rule(id = "1", folderName = "Foto", pattern = "*.jpg"),
            Rule(id = "2", folderName = "Dok", pattern = "*.pdf")
        )
        val reason = explainNoMatchByName("catatan.txt", sizeKb = 10, rules = rules)
        assertTrue(reason.contains("*.jpg"))
        assertTrue(reason.contains("*.pdf"))
        assertTrue(reason.contains("Tidak cocok"))
    }

    // ---- buildPreviewResult ----

    @Test
    fun `preview counts total candidates and lists only matched names`() {
        val names = listOf("invoice_1.pdf", "invoice_2.pdf", "foto.jpg", "catatan.txt")
        val result = buildPreviewResult(names, pattern = "invoice_*.pdf", excludePattern = "")
        assertEquals(4, result.totalCandidateFiles)
        assertEquals(listOf("invoice_1.pdf", "invoice_2.pdf"), result.matchedFileNames)
    }

    @Test
    fun `preview respects excludePattern on top of the include pattern`() {
        val names = listOf("invoice_1.pdf", "invoice_draft.pdf")
        val result = buildPreviewResult(names, pattern = "invoice_*.pdf", excludePattern = "invoice_draft*.pdf")
        assertEquals(listOf("invoice_1.pdf"), result.matchedFileNames)
    }

    // ---- nextAvailableFileName ----

    @Test
    fun `no conflict returns the original name untouched`() {
        val name = nextAvailableFileName("laporan.pdf") { false }
        assertEquals("laporan.pdf", name)
    }

    @Test
    fun `single conflict appends counter 1 before the extension`() {
        val existing = setOf("laporan.pdf")
        val name = nextAvailableFileName("laporan.pdf") { it in existing }
        assertEquals("laporan_1.pdf", name)
    }

    @Test
    fun `multiple conflicts increment the counter until a free name is found`() {
        val existing = setOf("laporan.pdf", "laporan_1.pdf", "laporan_2.pdf")
        val name = nextAvailableFileName("laporan.pdf") { it in existing }
        assertEquals("laporan_3.pdf", name)
    }

    @Test
    fun `extensionless file name still gets a trailing dot -- bug-for-bug parity with production`() {
        // SENGAJA menguji perilaku "aneh" ini (bukan memperbaikinya) --
        // lihat KDoc nextAvailableFileName di FileSorter.kt kenapa ini
        // dipertahankan apa adanya di batch ekstraksi ini.
        val existing = setOf("README")
        val name = nextAvailableFileName("README") { it in existing }
        assertEquals("README_1.", name)
    }
}
