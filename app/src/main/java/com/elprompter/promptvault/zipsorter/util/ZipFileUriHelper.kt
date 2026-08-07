package com.elprompter.promptvault.zipsorter.util

import androidx.documentfile.provider.DocumentFile

/**
 * Nama diberi prefix "Zip" supaya tidak collide/rancu dengan helper SAF lain
 * yang mungkin ada di modul FileSorter utama (util/FileSorter.kt) -- modul
 * ini sengaja diisolasi total, tidak saling memanggil.
 */
object ZipFileUriHelper {

    /**
     * Membuat file baru di [parentDir]. Kalau [originalName] sudah dipakai,
     * cari nama alternatif `nama_1.ext`, `nama_2.ext`, dst. sampai ketemu
     * slot kosong (auto-rename duplicate).
     */
    fun getUniqueTargetFile(parentDir: DocumentFile, originalName: String): DocumentFile {
        val existing = parentDir.findFile(originalName)
        if (existing == null) {
            return parentDir.createFile(getMimeType(originalName), originalName)
                ?: throw IllegalStateException("Gagal membuat file: $originalName")
        }

        val dotIndex = originalName.lastIndexOf('.')
        val nameWithoutExt = if (dotIndex != -1) originalName.substring(0, dotIndex) else originalName
        val ext = if (dotIndex != -1) originalName.substring(dotIndex) else ""

        var counter = 1
        while (true) {
            val candidateName = "${nameWithoutExt}_$counter$ext"
            if (parentDir.findFile(candidateName) == null) {
                return parentDir.createFile(getMimeType(originalName), candidateName)
                    ?: throw IllegalStateException("Gagal membuat file: $candidateName")
            }
            counter++
        }
    }

    /** Ambil sub-folder kalau sudah ada, atau buat baru kalau belum. */
    fun getOrCreateSubFolder(parentDir: DocumentFile, folderName: String): DocumentFile {
        return parentDir.findFile(folderName)?.takeIf { it.isDirectory }
            ?: parentDir.createDirectory(folderName)
            ?: throw IllegalStateException("Gagal membuat folder: $folderName")
    }

    private fun getMimeType(fileName: String): String {
        val ext = fileName.substringAfterLast('.', "").lowercase()
        return when (ext) {
            "pdf" -> "application/pdf"
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            "zip" -> "application/zip"
            "txt" -> "text/plain"
            else -> "application/octet-stream"
        }
    }
}
