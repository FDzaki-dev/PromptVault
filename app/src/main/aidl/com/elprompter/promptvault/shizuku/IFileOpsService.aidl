package com.elprompter.promptvault.shizuku;

// Kontrak IPC ke proses UserService yang dijalankan Shizuku (UID shell/adb,
// atau root -- tergantung backend Shizuku aktif di device user), TERPISAH
// dari proses app ini dan TIDAK terikat sandbox Scoped Storage biasa.
// Semua path adalah path filesystem absolut polos (bukan content:// URI) --
// proses Shizuku memakai java.io.File langsung, bukan SAF/ContentResolver.
//
// [Fitur baru 2026-08-17, integrasi Shizuku] Lihat FileOpsUserService.kt
// (implementasi Stub, jalan di proses Shizuku) & ShizukuManager.kt (binder
// lifecycle di proses app) & FileSorter.kt (pemakai, jalur tujuan Shizuku).
interface IFileOpsService {
    boolean ping();
    boolean exists(String path);
    boolean isDirectory(String path);
    // HANYA dipakai untuk subfolder RULE di dalam root yang SUDAH ADA --
    // FileSorter SENGAJA tidak pernah memanggil ini untuk folder ROOT tujuan
    // kustom (lihat FileSorter.resolveShizukuRuleDestinations & permintaan
    // eksplisit user 2026-08-17: root TIDAK PERNAH dibuat otomatis).
    boolean mkdirs(String path);
    boolean moveFile(String srcPath, String destPath);
    boolean deleteFile(String path);
    long fileLength(String path);
    void destroy();
}
