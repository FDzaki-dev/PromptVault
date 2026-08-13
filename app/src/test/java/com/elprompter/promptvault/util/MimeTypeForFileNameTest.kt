package com.elprompter.promptvault.util

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * [SAF, syarat (c) Insiden #7] Regresi langsung untuk Bug #2 (v2.10.0): mime
 * type file tujuan SAF HARUS selalu diturunkan dari ekstensi nama file,
 * TIDAK PERNAH dari metadata provider sumber. [mimeTypeForFileName] sengaja
 * dibuat fungsi murni top-level (bukan method FileSorter) supaya test ini
 * jalan sebagai unit test JVM biasa -- TANPA Context/Robolectric/emulator --
 * jadi ini satu-satunya bagian dari fitur SAF batch ini yang benar-benar
 * tereksekusi (bukan cuma dibaca) sebelum sampai ke CI asli.
 */
class MimeTypeForFileNameTest {

    @Test
    fun `zip extension maps to application zip`() {
        assertEquals("application/zip", mimeTypeForFileName("laporan.zip"))
        assertEquals("application/zip", mimeTypeForFileName("LAPORAN.ZIP"))
    }

    @Test
    fun `txt extension maps to text plain`() {
        assertEquals("text/plain", mimeTypeForFileName("catatan.txt"))
        assertEquals("text/plain", mimeTypeForFileName("CATATAN.TXT"))
    }

    @Test
    fun `unknown or missing extension falls back to octet stream`() {
        // [update 2026-08-13, dukung SEMUA ekstensi] "berkas.pdf" DIHAPUS dari
        // sini -- pdf sekarang ADA di tabel (lihat test di bawah). Extension
        // yang genuinely tidak terdaftar dipakai sebagai gantinya, supaya
        // assertion ini tetap benar-benar menguji jalur fallback, bukan
        // kebetulan match tabel yang sudah diperluas.
        assertEquals("application/octet-stream", mimeTypeForFileName("tanpa_ekstensi"))
        assertEquals("application/octet-stream", mimeTypeForFileName("berkas.xyz123"))
    }

    @Test
    fun `common extensions added 2026-08-13 map to their real mime type`() {
        // [Feature, dukung SEMUA ekstensi -- permintaan user 2026-08-13] Tabel
        // diperluas dari cuma zip/txt supaya file project (mixed extension)
        // dapat MIME yang lebih akurat, bukan cuma octet-stream generik.
        assertEquals("application/pdf", mimeTypeForFileName("dokumen.pdf"))
        assertEquals("image/jpeg", mimeTypeForFileName("foto.jpg"))
        assertEquals("text/plain", mimeTypeForFileName("Main.kt"))
        assertEquals("application/json", mimeTypeForFileName("data.json"))
    }

    @Test
    fun `double extension uses only the last segment`() {
        // Nama seperti "arsip.zip.txt" harus dibaca sebagai .txt -- konsisten
        // dengan TEMP_FILE_MARKERS di FileSorter yang juga cek akhiran penuh.
        assertEquals("text/plain", mimeTypeForFileName("arsip.zip.txt"))
    }

    @Test
    fun `trailing dot with no extension falls back to octet stream`() {
        assertEquals("application/octet-stream", mimeTypeForFileName("berkas."))
    }
}
