# Changelog PromptVault

Semua versi dan alasan perubahannya, biar sesi Claude berikutnya (atau kamu)
punya konteks penuh tanpa perlu scroll chat lama.

## v2.1.2 -- Fix CI (pipefail masking + diagnostik APK hilang)
- **Root cause "cp: cannot stat app-release.apk"**: step `assembleRelease`
  di v2.1.1 pakai `... 2>&1 | tee file.log` TANPA `set -o pipefail`. Di bash,
  exit code sebuah pipeline ikut command TERAKHIR (`tee`, yang selalu
  sukses), bukan `gradlew`. Akibatnya kalau gradlew gagal, GitHub Actions
  tetap menganggap step itu SUKSES dan lanjut ke step berikutnya (rename
  APK), yang jelas gagal karena APK-nya memang tidak pernah dihasilkan
- Semua step yang pakai `| tee` sekarang diawali `set -euo pipefail`
- `Decode keystore` sekarang memverifikasi keystore benar-benar valid
  (non-kosong + bisa dibuka `keytool -list`) SEBELUM lanjut build, dengan
  pesan error jelas kalau secret salah/kosong
- Step diagnostik baru yang SELALU jalan: `ls -la` isi folder output APK,
  supaya kalau nama file ternyata beda dugaan (mis. `app-release-unsigned.apk`
  karena signing tidak terpasang), langsung ketahuan dari log
- Pencarian APK sekarang dinamis (`find ... -name "*.apk"`), bukan hardcode
  nama file, dan otomatis warning kalau APK yang ketemu tidak bertanda tangan

## v2.1.1 -- Fix CI (Gradle version mismatch + logging yang gak kepakai)
- **Root cause build v2.1.0 gagal**: runner GitHub Actions memakai Gradle
  9.6.1 (sangat baru), tidak kompatibel dengan AGP 8.5.2 yang dipakai project
  ini (resmi cuma didukung sampai ~Gradle 8.9). CI sekarang generate Gradle
  Wrapper terkunci ke versi 8.9 di awal job, semua langkah pakai `./gradlew`
- **Fix logging CI yang ternyata gak kepakai**: perbaikan "auto-upload log
  kegagalan" di v2.0.1 ternyata cuma menangkap laporan deprecation warning,
  BUKAN error compile asli (yang dicetak ke konsol/stdout, bukan ke file
  report). Sekarang output tiap langkah penting di-redirect ke file log lewat
  `tee` dan itu yang di-upload -- baris `e: file:///...` (lokasi error
  sesungguhnya) sekarang benar-benar ke-capture

## v2.1.0 -- Dark Mode Ultra Premium
- Skema gelap didesain dari nol (bukan invert warna terang): 3 lapisan
  permukaan (background hampir hitam OLED, surface, surfaceVariant "raised"),
  aksen dicerahkan (PineGlow/StampGlow/AmberGlow) biar tetap hidup di gelap
- Pengaturan tema baru: Ikuti Sistem / Terang / Gelap (tersimpan permanen)
- **Refactor besar**: HAMPIR SEMUA komponen & layar sebelumnya hardcode warna
  literal terang langsung (bukan lewat `MaterialTheme.colorScheme`), yang
  berarti dark mode tidak akan berfungsi kalau tidak dibenahi. Semua sudah
  diganti ke referensi theme-aware -- lihat `MAINTENANCE.md` poin 6 untuk cara
  cek pola ini ke depannya
- Status bar & nav bar edge-to-edge otomatis ikut terang/gelap sistem

## v2.0.1 -- Maintainability Pass
- CI dipecah: compile-check cepat dulu (fail fast, hitungan detik) sebelum
  proses assembleRelease yang lambat (signing, packaging)
- Log build otomatis jadi artifact yang bisa diunduh kalau ada langkah CI
  yang gagal -- tidak perlu lagi copy-paste manual
- Tambah `CHANGELOG.md`, `TROUBLESHOOTING.md`, `MAINTENANCE.md` -- dokumentasi
  hidup yang jadi jaring pengaman & konteks lintas-sesi
- Audit ulang seluruh kode untuk pola bug yang sudah pernah terjadi (import
  `weight`/`align` salah, delegate `by` tanpa `getValue`/`setValue`)
- Tambah `@OptIn(ExperimentalMaterial3Api::class)` di `SettingsScreen` (jaga-jaga
  untuk `FilterChip`, mencegah potensi compile error tergantung versi Material3)

## v2.0.0
- Fix build gagal v1.9.0 (root cause: import `weight` salah + `getValue` hilang)
- Multi-pattern per rule (dipisah koma)
- Filter ukuran file min/max per rule
- Strategi konflik nama file (Ganti nama otomatis / Lewati / Timpa)

## v1.9.0
- Polish "Apple-style": large title, grouped list ala Settings iOS,
  action sheet dari bawah, segmented control, haptic feedback, animasi tekan
- **(build ini yang gagal, diperbaiki di v2.0.0)**

## v1.8.0
- Prioritas rule eksplisit + reorder naik/turun
- Exclude pattern per rule
- File-stability check (jangan pindahkan file yang masih ditulis/didownload)

## v1.7.0
- Fix glyph panah unicode `→` yang render jadi `'n` di font monospace
  (diganti Icon Compose asli)
- Penamaan artifact/APK CI dibuat dinamis dari `versionName`, bukan manual

## v1.6.0
- Audit navigasi: top bar + tombol back konsisten di semua layar
- Splash screen, edge-to-edge, snackbar feedback aksi

## v1.5.0
- Tema visual "Manifest Arsip": kraft/pine/stamp, tipografi monospace,
  VaultCard signature, ikon app baru

## v1.4.0
- Live preview pattern rule terhadap isi Downloads nyata
- Detail file dilewati per-scan dengan alasan spesifik
- Diagnostik: sample nama file asli di Downloads

## v1.3.0 (baseline awal iterasi lanjutan)
- Undo pemindahan file
- Interval auto-scan bisa diatur (15/30/60/120/240 menit)
- Peringatan rule tumpang tindih
- Backup/export & import rule (JSON)
- Pencarian/filter rule
- Konfirmasi pattern duplikat
- Layar Diagnostik (status WorkManager)
- Unit test dasar (GlobMatcher, RuleOverlapChecker)
