# Changelog PromptVault

Semua versi dan alasan perubahannya, biar sesi Claude berikutnya (atau kamu)
punya konteks penuh tanpa perlu scroll chat lama.

## v2.3.6 -- Catatan penutup: audit "pematangan fitur" selesai total
Tidak ada perubahan kode fungsional. Rilis ini murni menutup batch audit
yang diminta user ("stop nambah fitur, fokus bersihkan kecacatan logika")
dengan mencatat hasil akhir di `PROJECT_STATE.md`:

- Dua modul terakhir yang belum diperiksa (`data/db/` -- Room DAO/Entity/
  Converters/AppDatabase/Repository, dan `ui/theme/` -- Color/Shapes/Theme/
  Type, termasuk verifikasi wiring toggle Terang/Gelap/Ikuti Sistem) sudah
  diaudit. **Tidak ada bug ditemukan** di keduanya.
- Dengan ini SELURUH source code app sudah diperiksa modul per modul sejak
  v2.3.3: file-move core, semua layar UI + komponen, worker lifecycle,
  database, theme. Total 3 bug nyata ditemukan & diperbaiki di sepanjang
  batch ini -- lihat entri v2.3.3, v2.3.4, v2.3.5 di bawah untuk detail.
- Catatan di `PROJECT_STATE.md` diperbarui supaya sesi Claude berikutnya
  tidak perlu mengulang audit dari nol.

## v2.3.5 -- Pembersihan logika: worker lifecycle (AutoSortWorker + boot receiver)
Lanjutan batch pematangan fitur, audit mendalam ke `AutoSortWorker`,
`WorkScheduler`, `BootCompletedReceiver`, `PromptVaultApp`. Dua bug
korektnes nyata ditemukan & diperbaiki:

- **`AutoSortWorker.doWork()` sebelumnya menelan SEMUA exception diam-diam
  dan selalu `Result.retry()` tanpa batas.** Kalau penyebabnya permanen
  (mis. izin `MANAGE_EXTERNAL_STORAGE` dicabut user dari Setelan Android),
  worker retry setiap periode SELAMANYA tanpa pernah berhasil -- boros
  baterai, dan user tidak pernah tahu kenapa karena nol baris masuk Log
  Aktivitas. Sekarang: kegagalan level-worker selalu dicatat ke Log
  Aktivitas dulu, dan `SecurityException` (khas izin dicabut) dianggap
  permanen -> `Result.failure()` (tidak retry sia-sia). Error lain (mis.
  I/O sementara) tetap `Result.retry()` seperti semula.
- **`WorkScheduler.rescheduleFromSavedSettings()` yang dipanggil dari
  `BootCompletedReceiver` berisiko tidak selesai sebelum proses app
  dimatikan Android.** Fungsi lama membuka coroutine sendiri secara
  fire-and-forget; `onReceive()` kembali seketika, dan khususnya saat boot
  (proses baru dibuat cuma untuk broadcast ini) Android boleh mematikan
  proses SEBELUM coroutine sempat baca DataStore + enqueue WorkManager --
  auto-sort bisa gagal terjadwal ulang setelah reboot di sebagian
  device/timing, padahal "survive reboot" adalah fitur inti yang
  dijanjikan. Fix: fungsi sekarang `suspend fun` biasa; `BootCompletedReceiver`
  memakai `goAsync()` supaya proses ditahan hidup sampai reschedule
  benar-benar selesai. `PromptVaultApp.onCreate()` tidak butuh `goAsync()`
  (proses app sudah pasti hidup di titik itu), tetap fire-and-forget lewat
  coroutine scope miliknya sendiri.
- Tidak ada perubahan UI/visual, tidak ada fitur baru.

## v2.3.4 -- Pembersihan logika: "Timpa rule?" sekarang benar-benar menimpa
Lanjutan batch pematangan fitur, hasil audit menyeluruh layar UI & komponen
(`AddEditRuleScreen`, `RuleListScreen`, `ActivityLogScreen`, `SettingsScreen`,
`DiagnosticsScreen`, `SkippedFilesScreen`, `OnboardingScreen`, semua
komponen di `ui/components/`). Satu bug korektnes nyata ditemukan & diperbaiki:

- **Dialog konfirmasi "Pattern sudah dipakai rule X. Timpa rule tersebut?"
  sebelumnya TIDAK benar-benar menimpa apa pun.** Rule baru disimpan dengan
  id acak baru, rule lama yang duplikat tetap ada -- hasilnya dua rule
  dengan pattern identik hidup berdampingan, padahal user sudah bilang
  "iya, timpa". `RuleRepository` sekarang punya `upsertRule(rule, removeRuleId)`
  yang menghapus rule lama DAN menyimpan rule baru dalam satu operasi
  baca-ubah-simpan atomik (bukan dua panggilan terpisah yang berisiko race
  di DataStore). `AddEditRuleScreen` hanya mengirim `removeRuleId` untuk
  kasus DuplicatePattern -- kasus OverlapsWithOthers TETAP menyimpan
  keduanya berdampingan (memang begitu desainnya: urutan prioritas yang
  menentukan pemenang, bukan salah satu dihapus).
- Semua layar/komponen UI lain diperiksa satu per satu -- tidak ditemukan
  kecacatan logika lain. Tidak ada perubahan visual, tidak ada fitur baru.

## v2.3.3 -- Pembersihan logika: race condition scan manual vs auto-scan
Batch pematangan fitur (bukan fitur baru), hasil audit menyeluruh kode inti
atas permintaan user "fokus pematangan fitur & bersihkan kecacatan logika
sampai ke akar". Satu bug korektnes nyata ditemukan & diperbaiki:

- **`FileSorter.scanAndSort()` sekarang diserialisasi lewat `Mutex` bersama**
  (companion object, dibagi lintas semua instance `FileSorter` dalam proses
  yang sama). Sebelumnya, scan manual (tombol "Scan Sekarang" di
  `MainViewModel`) dan auto-scan latar belakang (`AutoSortWorker` via
  WorkManager) masing-masing membuat instance `FileSorter` sendiri tanpa
  koordinasi apa pun. Kalau keduanya kebetulan jalan bersamaan, dua proses
  bisa mencoba memindahkan file Downloads yang sama di saat yang sama --
  proses yang kalah race pada `File.renameTo()` tercatat sebagai "Gagal
  dipindahkan" di Log Aktivitas, padahal file itu sebenarnya sudah aman
  dipindahkan oleh proses yang menang. Sekarang panggilan kedua otomatis
  menunggu giliran (bukan gagal), lalu scan ulang dengan kondisi folder yang
  sudah terbaru.
- Tidak ada perubahan UI/visual, tidak ada fitur baru. `undo()` sengaja TIDAK
  ikut dikunci mutex yang sama di batch ini (di luar scope race condition
  yang dilaporkan) -- kalau ke depan ada gejala konflik undo-vs-scan
  berbarengan, itu trigger untuk batch terpisah.

## v2.3.2 -- Persiapan lanjut sesi lain: PROJECT_STATE.md & FILE_MANIFEST.txt
Tidak ada perubahan kode/perilaku app. Murni dokumentasi supaya sesi Claude
berikutnya bisa lanjut tanpa kehilangan konteks.

- **`PROJECT_STATE.md` (baru)** -- versi/batch terakhir, status tiap bagian
  roadmap backend (mana yang selesai vs sengaja dijeda + trigger kapan
  lanjut), struktur package, keputusan arsitektur utama, dan riwayat insiden
  kronologis lengkap (termasuk insiden regresi v2.3.0→v2.3.1 kemarin, ditulis
  detail supaya tidak terulang).
- **`FILE_MANIFEST.txt` (baru)** -- snapshot daftar file yang ditrack.
- `README.md` & `MAINTENANCE.md` diperbarui untuk merujuk 2 file baru ini di
  alur onboarding sesi baru.

## v2.3.1 -- FIX REGRESI: layar Home terpotong/ketumpuk setelah redesign v2.3.0
⚠️ Peringatan: v2.3.0 memasukkan 2 bug yang membuat tombol "Scan Sekarang" dan
menu di bawahnya hilang dari layar (dilaporkan lewat screenshot). Keduanya
sudah diperbaiki, tanpa mengubah desain visual yang sudah disepakati:

1. **`VaultCard` (bug utama)** -- implementasi gradient di v2.3.0 salah pakai
   `Modifier.fillMaxSize()` di dalam Box pembungkus. Karena Surface aslinya
   wrap-content, ini membuat kartu "Rule aktif / Auto-scan" merebut SISA
   SELURUH tinggi layar, mendorong tombol Scan & menu ke luar area yang
   terlihat. Diperbaiki: gradient sekarang digambar langsung via
   `Modifier.background(brush, shape)` pada Surface itu sendiri, tanpa Box
   tambahan -- kartu kembali mengikuti ukuran kontennya seperti semula.
2. **`AndroidManifest.xml` -- `MainActivity` tidak punya `launchMode`.**
   Tanpa ini, membuka app dari launcher saat instance lama masih berjalan di
   background (umum terjadi di XOS) berisiko membuat instance Activity BARU
   menumpuk di atas yang lama dalam task yang sama, menyebabkan layar terlihat
   "dobel" dengan konten terpotong. Ditambahkan `android:launchMode="singleTask"`
   supaya app selalu memakai satu instance Activity yang sama.
- Tidak ada perubahan desain/warna dari v2.3.0 -- ini murni perbaikan bug.

## v2.3.0 -- Redesign Visual: variasi aksen & kedalaman (murni visual, nol perubahan logika)
Respons atas feedback "desain terlalu monoton, bikin mata cepat lelah". SEMUA
perubahan di bawah murni tampilan -- tidak ada file logika/data/worker yang
disentuh.

- **4 warna aksen dipakai bergantian** di menu Home (sebelumnya nyaris semua
  hijau): Kelola Rule = hijau (Pine/PineGlow), Riwayat = amber, Pengaturan =
  aksen BARU "Slate" (biru batu tenang), Diagnostik = merah (error/Rust).
- **Peran warna Material3 yang sebelumnya kosong** (`primaryContainer`,
  `secondaryContainer`, `tertiaryContainer`, `errorContainer`, `outlineVariant`,
  `inverseSurface`, dst.) sekarang diisi eksplisit dari palet brand sendiri --
  sebelumnya diam-diam jatuh ke default ungu Material bawaan kalau ada
  komponen yang memakainya.
- **Kartu (`VaultCard`)** kini gradient vertikal sangat halus (bukan warna
  solid rata) supaya terasa berlapis/"bernapas", dipakai otomatis di semua
  layar yang pakai VaultCard (Home, Log, Rule, dst.) karena ini komponen
  bersama.
- **Chip ikon di menu list** kini gradient + bayangan lembut senada warna
  aksennya (bukan kotak warna flat), memberi sedikit kedalaman tanpa
  berlebihan.
- **Latar Home** diberi wash gradient tipis di bagian atas.
- **Tombol "Scan Sekarang"** sekarang gradient hangat (Stamp -> Amber),
  jadi titik fokus visual yang lebih jelas dibanding sebelumnya (blok
  oranye datar).
- Kartu ringkasan ("Rule aktif" / "Auto-scan") ditambah ikon kecil
  berwarna di depan tiap baris untuk penanda visual yang lebih cepat dibaca.
- Tidak ada perubahan pada `MainViewModel`, `FileSorter`, Room, WorkManager,
  atau CI -- murni file di `ui/theme` dan `ui/components`/`ui/screens/HomeScreen.kt`.

## v2.2.1 -- Batch 2: File Writing Stability & Temp-File Filter (§4)
- **`isLikelyStillWriting()` sekarang "Dual Stability Guard"**: sebelumnya
  cuma cek `lastModified()`. Sekarang tambah 2 sinyal lagi -- ukuran file
  diverifikasi tidak berubah dalam jeda 1 detik, lalu dicoba `FileChannel
  .tryLock()` untuk deteksi apakah masih dikunci proses lain (downloader/
  browser yang belum selesai flush ke disk). Kalau salah satu gagal, file
  ditunda ke scan berikutnya -- bukan dipaksa pindah dalam kondisi berisiko.
- **Filter file sementara/partial-download**: file berakhiran `.crdownload`,
  `.tmp`, `.part`, `.download`, `.downloading` sekarang TIDAK PERNAH masuk
  daftar kandidat sama sekali (bukan cuma "ditunda"), dicek dari nama
  lengkap file (bukan cuma ekstensi Kotlin) supaya pola akhiran ganda
  seperti `prompt.zip.crdownload` ikut tertangkap.
- Tidak ada perubahan pada `MoveOutcome`, `ScanResult`, atau API publik
  `FileSorter` lain -- `MainViewModel` tidak perlu disentuh.

## v2.2.0 -- Batch 1: Migrasi ActivityLog & MoveHistory ke Room SQLite
Batch pertama dari roadmap backend spec (SAF, MediaStore sync, Room, file
stability, coroutine lifecycle, CI/CD). Batch ini KHUSUS §3 (Room DB), sisanya
menyusul di batch terpisah (anti "rombak total" sekali jalan).

- **`ActivityLogRepository` & `MoveHistoryRepository`** kini disimpan di Room
  SQLite (tabel `activity_log` & `move_history`), bukan lagi JSON blob di
  DataStore. Alasan: decode ulang JSON ratusan/ribuan baris tiap ada 1 entri
  baru jadi lambat & boros memori seiring riwayat menumpuk.
- **`SettingsRepository` & `RuleRepository` TETAP di DataStore** -- keduanya
  kecil (key-value ringan), tidak ada alasan dipindah. Sesuai batas scope
  yang disepakati.
- **API publik kedua repository TIDAK BERUBAH** (`logFlow`, `add`, `clear`,
  `historyFlow`, `record`, `markUndone`, `getUndoableEntries`) -- artinya
  `MainViewModel`, `FileSorter`, dan `AutoSortWorker` tidak perlu disentuh
  sama sekali. Nol risiko regresi di layer UI/business logic.
- Penambahan `androidx.room` (runtime + ktx + compiler via KSP) dan plugin
  `com.google.devtools.ksp` di `build.gradle.kts` (root & `app/`).
- **Trim otomatis** tetap dipertahankan: log dipangkas ke 500 baris terbaru,
  riwayat undo ke 200 baris terbaru -- sekarang lewat query SQL `DELETE ...
  NOT IN (...)`, bukan `.take()` di memori.
- **Data lama di DataStore TIDAK dimigrasikan** ke Room (disepakati: bukan
  data kritis, tidak urgent). Konsekuensinya: setelah update ke versi ini,
  tab "Log" dan "Undo Pemindahan" akan kosong lagi untuk sekali ini saja.
  File yang SUDAH dipindah sebelumnya tetap aman di lokasi barunya --
  hanya riwayatnya di tab Undo yang tidak lagi tampil.

## v2.1.4 -- Konsolidasi Maintainability (final pass)
- **`scripts/preflight_check.sh`** -- semua 7 audit statis yang sebelumnya
  cuma tertulis sebagai command manual di `MAINTENANCE.md` sekarang jadi
  SATU script executable (`bash scripts/preflight_check.sh`). Konsisten,
  tidak perlu direkonstruksi ulang tiap sesi, exit code jelas (0=aman, 1=ada
  masalah)
- **Panduan onboarding sesi baru** di `MAINTENANCE.md` & `README.md`: cara
  tercepat Claude di sesi manapun dapat konteks penuh project ini adalah
  `web_fetch` langsung `CHANGELOG.md`/`MAINTENANCE.md` dari repo publik
  GitHub, TANPA perlu minta user upload ZIP ulang -- hemat waktu & token
- `README.md` dirombak jadi pintu masuk yang jelas: link repo, daftar
  dokumen, instruksi eksplisit "lanjutkan sesi baru"

## v2.1.3 -- Fix compile error SettingsScreen (bukti CI logging-nya sekarang jalan)
- **Ini pembuktian nyata perbaikan CI di v2.1.1/v2.1.2 berhasil**: kali ini
  log kegagalan yang ter-upload berisi error compile ASLI dan jelas:
  `e: SettingsScreen.kt:56:9 Functions which invoke @Composable functions
  must be marked with the @Composable annotation`
- Root cause: fungsi lokal `chipColors()` di dalam `SettingsScreen` manggil
  `FilterChipDefaults.filterChipColors(...)` (fungsi `@Composable`) tapi
  `chipColors()`-nya sendiri lupa ditandai `@Composable`. Sudah diperbaiki.
- Diaudit ulang seluruh proyek untuk pola yang sama (fungsi lokal manggil
  API `@Composable` tanpa anotasi) -- cuma satu kejadian, sudah bersih.
  Ditambahkan ke `MAINTENANCE.md` poin 7 sebagai pola bug baru yang dicek
  otomatis ke depannya.

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
