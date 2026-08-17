package com.elprompter.promptvault.shizuku

import java.io.File

/**
 * Implementasi [IFileOpsService.Stub] -- dijalankan Shizuku di PROSES
 * TERPISAH dengan UID shell/adb (atau root, tergantung backend Shizuku
 * aktif di device user), BUKAN di proses app ini. Karena itu java.io.File
 * biasa di sini bisa membaca/menulis path yang mungkin ditolak Scoped
 * Storage kalau dipanggil langsung dari proses app (mis. Android/data
 * milik app lain, atau folder tujuan kustom tanpa lewat SAF picker).
 *
 * WAJIB constructor tanpa argumen -- kontrak Shizuku UserService.
 *
 * [Requirement eksplisit user, 2026-08-17] Kelas ini SENGAJA TIDAK punya
 * method semacam "createRootIfMissing" -- app-side ([FileSorter]) yang
 * memutuskan kapan [mkdirs] dipanggil, dan SENGAJA TIDAK PERNAH
 * memanggilnya untuk folder ROOT tujuan kustom (lihat
 * FileSorter.resolveShizukuRuleDestinations) -- hanya untuk subfolder RULE
 * di dalam root yang sudah dipastikan ADA lebih dulu lewat [exists]/[isDirectory].
 */
class FileOpsUserService : IFileOpsService.Stub() {

    override fun ping(): Boolean = true

    override fun exists(path: String): Boolean = try {
        File(path).exists()
    } catch (e: Exception) {
        false
    }

    override fun isDirectory(path: String): Boolean = try {
        File(path).isDirectory
    } catch (e: Exception) {
        false
    }

    override fun mkdirs(path: String): Boolean = try {
        val f = File(path)
        (f.exists() && f.isDirectory) || f.mkdirs()
    } catch (e: Exception) {
        false
    }

    /**
     * Pola sama dengan [FileSorter.copyThenDelete] (pelajaran P0-2 project
     * ini, 2026-08-16): tulis ke file sementara di folder tujuan yang sama
     * dulu, verifikasi, BARU rename ke nama final -- `dest` (nama final)
     * tidak pernah tersentuh sampai transfer benar-benar tuntas, supaya
     * tidak ada file parsial nyangkut kalau proses ini mati di tengah jalan.
     */
    override fun moveFile(srcPath: String, destPath: String): Boolean = try {
        val src = File(srcPath)
        val dest = File(destPath)
        if (src.renameTo(dest)) {
            true
        } else {
            val destDir = dest.parentFile
            if (destDir == null) {
                false
            } else {
                val tempDest = File(destDir, "${dest.name}.tmp_${System.nanoTime()}")
                src.copyTo(tempDest, overwrite = false)
                if (!tempDest.renameTo(dest)) {
                    runCatching { tempDest.delete() }
                    false
                } else {
                    src.delete()
                    true
                }
            }
        }
    } catch (e: Exception) {
        false
    }

    override fun deleteFile(path: String): Boolean = try {
        File(path).delete()
    } catch (e: Exception) {
        false
    }

    override fun fileLength(path: String): Long = try {
        File(path).length()
    } catch (e: Exception) {
        -1L
    }

    /** Dipanggil ShizukuManager saat unbind -- matikan proses ini sendiri, jangan jadi proses menggantung. */
    override fun destroy() {
        System.exit(0)
    }
}
