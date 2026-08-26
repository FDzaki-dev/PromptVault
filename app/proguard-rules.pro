# Add project specific ProGuard rules here.
-keep class com.elprompter.promptvault.data.** { *; }

# [Fitur baru 2026-08-26, "pangkas ukuran aplikasi", minify diaktifkan di
# app/build.gradle.kts] 3 area WAJIB dikecualikan -- kegagalannya SILENT
# saat runtime (nama field/kelas cocok BY STRING, bukan reference langsung
# yang otomatis R8-aware), TIDAK muncul sbg compile error sama sekali.

# 1) kotlinx.serialization: field JSON di-mapping BY NAME saat
# decodeFromString<T>()/encodeToString() -- kalau nama properti diacak R8,
# hasil decode diam-diam null/gagal. Cakupan GENERIK lewat pola
# "$$serializer" (bukan daftar nama kelas manual per data class) supaya
# otomatis ikut kelas @Serializable BARU sesi mendatang (Rule.kt,
# ActivityLogEntry.kt, MoveHistoryEntry.kt sudah tercakup rule data.** di
# atas; VaultConfigBackup.kt di package util/ dan UpdateModels.kt di
# package update/ BELUM, makanya rule generik project-wide di bawah,
# bukan cuma package data/).
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.elprompter.promptvault.**$$serializer { *; }
-keepclassmembers class com.elprompter.promptvault.** {
    *** Companion;
}
-keepclasseswithmembers class com.elprompter.promptvault.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# 2) WorkManager: AutoSortWorker/ManualScanWorker (worker/ package)
# diinstansiasi via Class.forName(namaKelasTersimpan) oleh WorkManager saat
# eksekusi terjadwal -- nama kelas disimpan sbg STRING di WorkSpec internal
# WorkManager saat enqueue, BUKAN reference Class<T> langsung yang otomatis
# R8-aware. Kalau nama kelas diacak, WorkManager gagal ClassNotFoundException
# saat coba eksekusi worker terjadwal (baru ketahuan user, bukan CI/compile).
-keep class * extends androidx.work.ListenableWorker { <init>(...); }

# 3) Shizuku: proses UserService EKSTERNAL (jalan di proses Shizuku, di
# luar proses app ini) berkomunikasi lewat AIDL Stub/Proxy
# (IFileOpsService) + binder Shizuku -- kedua sisi WAJIB nama kelas/method
# identik, R8 di sisi app ini tidak tahu apa pun soal sisi proses lain.
# rikka.shizuku.** = library API (AAR biasanya sudah bawa consumer-rules
# sendiri, ini lapis kedua eksplisit -- kegagalannya binder mismatch,
# tidak pernah muncul sbg compile/lint error). shizuku/** = AIDL Stub/Proxy
# hasil generate AGP (IFileOpsService) + ShizukuManager.kt/
# FileOpsUserService.kt sendiri.
-keep class rikka.shizuku.** { *; }
-keep class com.elprompter.promptvault.shizuku.** { *; }
