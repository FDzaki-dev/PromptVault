package com.elprompter.promptvault.util

import java.io.File
import java.io.IOException

/**
 * [Fix P0-1, audit gap 2026-08-16 -- PromptVault_real_functional_polish_gap_audit.md]
 * Validasi nama folder tujuan rule SEBELUM dipakai membangun path filesystem
 * nyata. SEBELUM fix ini, `rule.folderName` cuma dicek `isNotBlank()` di UI
 * ([com.elprompter.promptvault.ui.screens.AddEditRuleScreen]) lalu dipakai
 * LANGSUNG di `File(destDir, rule.folderName)` ([FileSorter.moveFile]) TANPA
 * penolakan path separator/`.`/`..` sama sekali -- nama seperti `"../../"`
 * atau `"a/b"` bisa membuat destinasi KELUAR dari folder `PromptVault` yang
 * dimaksud (path traversal) di jalur lokal, dan berperilaku BEDA (tidak
 * terjamin containment yang sama) di jalur SAF karena provider tidak selalu
 * menolak nama semacam itu.
 *
 * Fungsi top-level murni (bukan method [FileSorter]) SUPAYA unit-testable
 * tanpa Context Android, konsisten pola project ([mimeTypeForFileName],
 * [GlobMatcher]) -- lihat RuleFolderNameValidatorTest.
 *
 * DIPANGGIL DI DUA LAPIS WAJIB (sesuai required-fix audit "apply same
 * invariant before local filesystem writes and before SAF folder creation",
 * bukan cuma UI):
 *  1. [com.elprompter.promptvault.ui.screens.AddEditRuleScreen] -- validasi
 *     inline SEBELUM rule sempat disimpan (mencegah rule tidak valid masuk
 *     DataStore sama sekali).
 *  2. [FileSorter.moveFile] & [FileSorter.resolveSafRuleDestinations] --
 *     GERBANG TERAKHIR tepat sebelum path benar-benar dipakai untuk operasi
 *     tulis nyata. WAJIB tetap ada meskipun (1) sudah ada, karena rule LAMA
 *     yang tersimpan SEBELUM validator ini pernah ada (dari versi app
 *     sebelum fix ini dipasang) tidak pernah lolos lapis (1) -- lapis (2)
 *     memastikan rule lama itu tetap aman dipakai scan, bukan cuma dicegah
 *     untuk rule baru ke depan.
 */
private val RESERVED_NAMES = setOf(".", "..")

/**
 * Karakter yang ditolak di SEMUA platform tujuan folder: `/`/`\\` (path
 * separator lokal DAN SAF), sisanya (`:*?"<>|` + kontrol) ditolak preventif
 * karena provider SAF non-AOSP (kartu SD FAT32, provider OEM/cloud) sering
 * mewarisi batasan penamaan FAT/NTFS walau filesystem lokal Android sendiri
 * (ext4) sebenarnya mengizinkan sebagian besar karakter itu.
 */
private val FORBIDDEN_CHARS = charArrayOf('/', '\\', ':', '*', '?', '"', '<', '>', '|')

/**
 * `null` = nama valid, siap dipakai membangun folder tujuan.
 * Non-null = alasan penolakan dalam Bahasa Indonesia, siap ditampilkan
 * langsung ke user (baik sebagai error inline UI maupun entri Activity Log).
 */
fun validateRuleFolderName(rawName: String): String? {
    val name = rawName.trim()
    if (name.isEmpty()) return "Nama folder tidak boleh kosong."
    if (name in RESERVED_NAMES) return "Nama folder tidak boleh \".\" atau \"..\"."
    val badChar = name.firstOrNull { it in FORBIDDEN_CHARS || it.code < 0x20 }
    if (badChar != null) {
        return "Nama folder mengandung karakter tidak diizinkan (\"$badChar\"). Hindari / \\ : * ? \" < > | dan karakter kontrol."
    }
    return null
}

fun isValidRuleFolderName(rawName: String): Boolean = validateRuleFolderName(rawName) == null

/**
 * Pertahanan lapis-kedua (defense-in-depth) SETELAH [File] tujuan dibangun --
 * memastikan hasil akhirnya BENAR-BENAR masih di dalam [expectedParent]
 * secara canonical path (mis. `..` yang lolos dari celah lain, symlink,
 * atau normalisasi path OS yang tidak terduga), bukan cuma percaya nama
 * sudah lolos [validateRuleFolderName]. Dipanggil di [FileSorter] tepat
 * sebelum operasi tulis/mkdirs nyata dieksekusi terhadap path lokal.
 */
fun isContainedIn(child: File, expectedParent: File): Boolean {
    return try {
        val childCanonical = child.canonicalFile
        val parentCanonical = expectedParent.canonicalFile
        childCanonical == parentCanonical ||
            childCanonical.path.startsWith(parentCanonical.path + File.separator)
    } catch (e: IOException) {
        false
    }
}
