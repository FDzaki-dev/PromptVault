# PROJECT_STATE.md -- PromptVault
> WAJIB dibaca Claude di awal SETIAP sesi baru, sebelum melanjutkan kerja apa
> pun. Jangan hapus riwayat insiden di bawah walau sudah lama/sudah fix --
> ini log kronologis permanen, bukan changelog fitur (itu ada di CHANGELOG.md).

## Versi/batch terakhir yang selesai
- **versionCode 35 / versionName 2.4.1** -- rilis terakhir yang dikirim ke
  user. User KONFIRMASI v2.4.0 (scan paralel) sudah terasa cepat di HP asli,
  lalu diminta lanjut fokus optimasi/performance. Audit lanjutan ke jalur
  yang DIPANGGIL selama scan (bukan scan itu sendiri) menemukan
  `ActivityLogRepository.add()`/`MoveHistoryRepository.record()` memanggil
  `dao.trimToMax()` (DELETE+subquery ORDER BY, scan seluruh tabel) di
  SETIAP insert -- selama scan paralel Semaphore(6), ini jadi ratusan trim
  beruntun yang berebut write-lock SQLite, menahan sebagian paralelisme
  v2.4.0. Fix: trim berkala tiap 20 insert (`AtomicInteger` counter
  per-instance), bukan tiap insert. HANYA `ActivityLogRepository.kt` +
  `MoveHistoryRepository.kt` yang diubah. Lihat CHANGELOG v2.4.1 untuk
  detail teknis lengkap.
  **BELUM ada konfirmasi user bahwa efek trim-berkala ini terasa/terverifikasi**
  (sandbox Claude tidak bisa verifikasi runtime/kompilasi) -- kalau sesi
  depan lanjut optimasi lagi, cek dulu apakah v2.4.1 sudah
  ke-install sebelum audit ulang dari nol.
- **versionCode 33 / versionName 2.3.9** -- rilis sebelumnya. Padding luar
  layar distandarkan ke 16dp di seluruh app + Onboarding dapat animasi
  Crossfade antar step. Lihat CHANGELOG v2.3.9.
- **2026-08-02, finishing batch (izin legacy + UI polish):** user konfirmasi
  fix Home v2.3.1 sudah normal, lalu minta lanjut ke tahap "finishing":
  audit menyeluruh + robustness + polish UI. Audit/robustness sudah matang
  dari batch sebelumnya (lihat entri di bawah); satu-satunya item robustness
  yang masih tersisa (celah izin Android 8-10) SEKARANG DIPERBAIKI atas
  keputusan eksplisit user -- lihat CHANGELOG v2.3.7 untuk detail teknis.
  Sekaligus batch UI polish pertama: empty state (ikon + layout konsisten)
  di 4 layar (Kelola Rule, Riwayat Aktivitas, Undo Pemindahan, File
  Dilewati) lewat komponen baru `EmptyState`, animasi `Crossfade` untuk
  transisi kosong<->berisi, `animateItemPlacement()` di semua LazyColumn
  list, dan transisi fade+slide antar layar di `NavHost` (sebelumnya potong
  instan tanpa animasi sama sekali).
- **2026-08-01, PENUTUP audit "pematangan fitur":** `data/db/` (Room DAO,
  Entity, Converters, AppDatabase, kedua Repository) dan `ui/theme/`
  (Color, Shapes, Theme, Type) sudah diperiksa -- termasuk verifikasi
  eksplisit bahwa toggle Terang/Gelap/Ikuti Sistem di Settings benar-benar
  tersambung ke Compose theme (`MainActivity` menghitung `effectiveDark`
  dari `themeMode` + `isSystemInDarkTheme()` dengan benar). **Tidak ada bug
  ditemukan di kedua modul ini.**
  **AUDIT SELESAI TOTAL** -- seluruh source code app sudah diperiksa
  modul per modul: file-move core, semua layar UI + komponen, worker
  lifecycle, database, theme. Total 3 bug nyata ditemukan & diperbaiki
  sepanjang batch ini (v2.3.3, v2.3.4, v2.3.5) -- lihat CHANGELOG.md untuk
  detail masing-masing. Sesi berikutnya TIDAK PERLU mengulang audit ini
  dari nol; kalau mau lanjut lagi, fokus ke perubahan/fitur yang terjadi
  SETELAH v2.3.6, bukan re-scan modul yang sudah tercatat bersih di sini.
- **2026-08-01, audit worker lifecycle:** ditemukan & diperbaiki 2 bug:
  (1) `AutoSortWorker.doWork()` menelan exception diam-diam + retry tanpa
  batas bahkan untuk error permanen (izin dicabut) -> sekarang selalu log
  ke Activity Log + `Result.failure()` khusus untuk `SecurityException`.
  (2) `BootCompletedReceiver` berisiko proses dimatikan Android sebelum
  reschedule WorkManager selesai (tidak pakai `goAsync()`) -> auto-sort
  bisa gagal aktif lagi setelah reboot. Sudah diperbaiki, lihat CHANGELOG
  v2.3.5 untuk detail lengkap. Dengan ini, audit "pematangan fitur &
  bersihkan kecacatan logika" mencakup: file-move core (`FileSorter`,
  race condition v2.3.3), semua layar UI + komponen (v2.3.4), dan worker
  lifecycle (v2.3.5). Belum diaudit mendalam: `data/db/` (Room
  DAO/Converters/migrations) dan `ui/theme/`.
- **2026-08-01, lanjutan sesi audit:** setelah v2.3.3 (fix race condition),
  user minta lanjut audit modul lain (UI screens, worker lifecycle). Semua
  layar (`AddEditRuleScreen`, `RuleListScreen`, `ActivityLogScreen`,
  `SettingsScreen`, `DiagnosticsScreen`, `SkippedFilesScreen`,
  `OnboardingScreen`) dan semua komponen di `ui/components/` diperiksa.
  Ditemukan & diperbaiki 1 bug: dialog "Timpa rule tersebut?" pada
  DuplicatePattern tidak benar-benar menghapus rule lama (lihat CHANGELOG
  v2.3.4 untuk detail). Tidak ada bug lain ditemukan di modul-modul ini.
  Worker lifecycle (`AutoSortWorker`, `WorkScheduler`, `BootCompletedReceiver`)
  sudah diperiksa sebelumnya di sesi race-condition, tetap aman.
- **2026-08-01, sesi audit "pematangan fitur":** user secara eksplisit minta
  STOP menambah batch/fitur baru, fokus audit & bersihkan kecacatan logika di
  fitur yang sudah ada. Hasil audit menyeluruh kode inti menemukan 2 hal:
  1. **[SUDAH DIPERBAIKI, v2.3.3]** Race condition: scan manual vs
     `AutoSortWorker` bisa jalan bersamaan (tidak ada koordinasi antar
     instance `FileSorter`), berisiko file Downloads yang sama dipindah dua
     proses sekaligus -> proses kedua tercatat error padahal file aman.
     Fix: `Mutex` bersama di companion object `FileSorter`, lihat entri
     CHANGELOG v2.3.3 untuk detail lengkap.
  2. **[DITUNDA atas keputusan user, LALU DIPERBAIKI di v2.3.7]** Celah izin
     penyimpanan di Android 8-10 (API 26-29): `hasManageStoragePermission()`
     di `MainActivity.kt` hardcode `true` untuk semua device di bawah
     Android 11, padahal `minSdk = 26`. Di rentang API 26-29 app TIDAK
     PERNAH benar-benar meminta izin runtime `READ/WRITE_EXTERNAL_STORAGE`
     -- layar "Izin Diperlukan" langsung dilewati, operasi pindah file akan
     gagal diam-diam di device lawas. Device utama user (Infinix Android
     15/16) tidak kena, makanya awalnya ditunda. Saat masuk tahap
     "finishing" (2026-08-02), user minta sekalian dibenerin -- fix di
     v2.3.7: `hasManageStoragePermission()` sekarang benar-benar cek
     `ContextCompat.checkSelfPermission()` untuk API 26-29, ditambah alur
     `ActivityResultContracts.RequestMultiplePermissions()` untuk minta
     izin runtime langsung (bukan cuma lempar ke halaman Setelan umum), plus
     fallback tombol buka Pengaturan Aplikasi kalau izin ditolak permanen.
     Lihat CHANGELOG v2.3.7 untuk detail lengkap.
- Roadmap backend (spec "PROMPTVAULT - BACKEND & CI/CD EXECUTABLE SPECIFICATION")
  dipecah jadi batch. Status per bagian:
  - §3 Room DB Migration -- **SELESAI** (Batch 1, v2.2.0)
  - §4 File Writing Stability & Temp-File Filter -- **SELESAI** (Batch 2, v2.2.1)
  - §1 SAF/Scoped Storage abstraction -- **BELUM, DIJEDA** atas keputusan user
  - §2 MediaStore rescan/ghost file cleanup -- **BELUM, DIJEDA**
  - §5 Coroutine lifecycle & Foreground Service -- **BELUM, DIJEDA**
  - §6 CI/CD preflight+dependency lock lanjutan -- **BELUM, DIJEDA** (sebagian
    kecil, yaitu compile-check fail-fast, sudah ada duluan di build.yml
    sebelum roadmap ini, di luar batch manapun)
  - **Keputusan eksplisit dari user (2026-08-01):** JANGAN lanjutkan batch
    yang belum dikerjakan kecuali benar-benar urgent/ada manfaat langsung.
    Trigger untuk lanjut: crash nyata saat auto-sort >50 file (→ §5), akses
    folder hilang setelah restart HP (→ §1), atau file "hantu" muncul di file
    manager bawaan (→ §2). Jangan proaktif menawarkan lanjut batch tanpa salah
    satu gejala ini muncul duluan.
- Redesign visual besar (v2.3.0) sudah dikirim & di-fix regresinya (v2.3.1).
  Arah desain "4-color accent system" ini sekarang JADI STANDAR -- jangan
  balik ke skema lama (hijau dominan di semua ikon) di update berikutnya.

## Struktur package/modul singkat
```
data/            -- domain model + repository (Rule, Settings via DataStore;
                     ActivityLog & MoveHistory via Room -- lihat data/db/)
data/db/         -- Room: AppDatabase, Entity, Dao, Converters (KHUSUS log &
                     history, bukan untuk Rules/Settings)
ui/screens/      -- satu file per layar (Home, RuleList, ActivityLog,
                     Settings, Diagnostics, SkippedFiles)
ui/components/   -- widget bersama (VaultCard, GroupedListRow, RuleCard, dst)
                     -- HATI-HATI: dipakai lintas layar, bug di sini nyebar
                     ke semua tempat yang pakai (lihat insiden #3 di bawah)
ui/theme/        -- Color.kt, Theme.kt (ColorScheme + VaultExtraColors utk
                     aksen Slate), Shapes.kt, Type.kt
util/FileSorter.kt -- logika inti scan & pindah file (java.io.File based,
                     BELUM SAF -- lihat §1 di atas)
worker/          -- AutoSortWorker (WorkManager), BootCompletedReceiver,
                     WorkScheduler
```

## Keputusan arsitektur utama
1. **Split penyimpanan**: Rules & Settings tetap DataStore Preferences
   (kecil, key-value). ActivityLog & MoveHistory pindah ke Room SQLite sejak
   v2.2.0 (bisa tumbuh ribuan baris, butuh trim query & Flow paginasi-siap).
   Data lama di DataStore TIDAK dimigrasikan ke Room (disepakati: tidak
   kritis, tidak urgent) -- kalau ada user lama upgrade dari <v2.2.0, log &
   riwayat undo mereka reset sekali.
2. **FileSorter masih `java.io.File`**, bukan SAF/`DocumentFile`. Ini
   keputusan SADAR menunda §1 (bukan lupa) -- app pakai
   `MANAGE_EXTERNAL_STORAGE` yang sudah cukup untuk kasus penggunaan saat
   ini (folder Downloads, akses penuh). Jangan refactor ke SAF tanpa trigger
   yang disebut di atas.
3. **Sistem warna 4-aksen**: Material3 `primary`(hijau/Pine) `tertiary`(amber)
   `error`(merah/Rust) + aksen kustom ke-4 `VaultTheme.extraColors.slate`
   (biru batu, di luar 4 role Material3 baku, disimpan lewat
   `CompositionLocal` di `Theme.kt`). Dipakai supaya menu tidak monoton satu
   warna. Semua role Material3 (`primaryContainer`, `secondaryContainer`,
   dst.) SUDAH diisi eksplisit dari palet brand -- jangan biarkan kosong lagi
   (defaultnya ungu Material generik, tidak sesuai brand).
4. **`VaultCard` harus wrap-content, TIDAK BOLEH pakai `Modifier.fillMaxSize()`
   di dalam Box pembungkus** -- ini penyebab insiden #3 di bawah. Kalau mau
   nambah efek visual (gradient/shadow) ke VaultCard lagi nanti, terapkan
   lewat `Modifier.background(brush, shape)` langsung di Surface, JANGAN
   bungkus content dengan Box+fillMaxSize.
5. **`MainActivity` pakai `android:launchMode="singleTask"`** (ditambah di
   v2.3.1) -- jangan dihapus, mencegah instance Activity dobel menumpuk di
   task yang sama saat dibuka ulang dari launcher (umum di custom ROM XOS).
6. **`FileSorter.scanAndSort()` sekarang paralel dengan batas
   `SCAN_CONCURRENCY = 6`** (v2.4.0).
   - **Tanggal**: 2026-08-03.
   - **Alasan**: scan sebelumnya sekuensial + jalan di Main thread -> freeze/
     ANR/force-close bahkan di ratusan file (lihat CHANGELOG v2.4.0). Fix
     butuh paralelisme supaya wall-time stability-check (delay 1 detik/file)
     tidak sekedar dikali jumlah file kandidat.
   - **Konsekuensi**: `SCAN_CONCURRENCY = 6` adalah **ASUMSI TEKNIS AI**
     (belum divalidasi profiling nyata di HP user) -- titik tengah antara
     memangkas wall-time signifikan vs tidak membuka terlalu banyak file
     handle/`RandomAccessFile` bersamaan di HP kelas menengah-bawah (target:
     Infinix Android 15/16). Kalau nanti user punya Downloads berisi ribuan
     file dan masih terasa berat, INI kandidat pertama untuk di-tuning
     (naikkan angka atau buat konfigurable dari Settings), bukan trigger
     redesain ulang arsitektur scan.
   - **Alternatif yang ditolak**: (a) concurrency tak terbatas (`async` tanpa
     `Semaphore`) -- ditolak, resiko terlalu banyak file handle dibuka
     bersamaan kalau Downloads berisi ribuan file; (b) tetap sekuensial tapi
     hapus delay 1 detik sepenuhnya -- ditolak, itu bagian dari Dual
     Stability Guard (§4 lama) yang mencegah file yang masih di-download
     ikut terpindah setengah jadi/korup, tidak aman dihapus begitu saja.

## Riwayat insiden kronologis (JANGAN DIHAPUS, tambah entri baru di ATAS)

### [2026-08-04] Bug packaging Claude: ZIP kehilangan .github/workflows/ dan .gitignore
- **Gejala**: user push ZIP v2.4.1, repo GitHub jadi kehilangan folder
  `.github/workflows/` (CI workflow hilang total) dan `.gitignore`, padahal
  file-file lain lengkap. User laporan "ada yang hilang" setelah cek repo.
- **Root cause (kesalahan Claude, di proses packaging bukan di kode app)**:
  command `zip -x '*.git*'` yang dimaksudkan untuk mengecualikan folder
  `.git/` (version control internal) ternyata pakai pattern terlalu lebar --
  `*.git*` cocok dengan SEMUA path yang mengandung substring "git" di posisi
  manapun, termasuk `.github` (mengandung ".git" di awal + "hub") dan
  `.gitignore` (mengandung ".git" di awal + "ignore"). Akibatnya kedua file
  itu ikut ter-exclude dari ZIP tanpa disadari sampai user cek repo langsung.
- **Insiden terkait**: sebelum ini juga ada bug packaging (ZIP dibungkus
  folder `PromptVault-main/` padahal aturan proyek adalah file langsung di
  root ZIP) yang bikin Termux extract nested -- sempat difix manual oleh
  user via `mv`+`shopt -s dotglob`. Kedua bug ini SATU AKAR: proses
  packaging ZIP tidak divalidasi against daftar file yang sudah diketahui
  (`FILE_MANIFEST.txt`) sebelum dikirim.
- **Fix**: exclude pattern diganti jadi spesifik (`-x '.git/*' -x '.git'`,
  bukan wildcard longgar), dan ZIP di-generate langsung dari root project
  (`zip -r ... .`, bukan `zip -r ... PromptVault-main`) supaya flat di root
  sesuai aturan. Dikirim ulang sebagai ZIP v2.4.1 revisi.
- **Pelajaran untuk sesi Claude berikutnya**: SEBELUM present_files ZIP
  apapun, WAJIB `unzip -l` hasil ZIP dan bandingkan jumlah entri + cek
  eksplisit `.github`/`.gitignore` ada, JANGAN cuma percaya command zip
  berhasil (exit code 0 tidak menjamin isi lengkap kalau exclude pattern
  salah). Idealnya diff nama file dalam ZIP vs `FILE_MANIFEST.txt`.
- **Status**: fix sudah dikirim (ZIP v2.4.1 revisi), BELUM ada konfirmasi
  user bahwa CI Actions sekarang jalan normal dengan workflow yang sudah
  lengkap.

### [2026-08-03] v2.4.0 -- Scan "kewalahan" di ratusan file: freeze/force-close/auto-sort lambat
- **Gejala**: user laporan app kewalahan scan file di Downloads walau cuma
  ratusan (100-500) file -- SEMUA gejala sekaligus (freeze/lag, force close,
  auto-sort background lambat/telat), bukan cuma satu.
- **Root cause #1**: `FileSorter.scanAndSort()` tidak pernah pindah
  dispatcher. Dipanggil dari `MainViewModel.runManualScan()` lewat
  `viewModelScope.launch` (default Main) -> semua I/O blocking (`listFiles`,
  `RandomAccessFile` lock check, `renameTo`, `copyTo`) jalan LANGSUNG di UI
  thread -> freeze/ANR/force-close.
- **Root cause #2**: `isLikelyStillWriting()` (delay 1 detik + buka file
  handle) jalan untuk SEMUA kandidat ZIP/TXT termasuk yang tidak cocok rule
  apapun -- boros untuk file yang toh tidak akan pernah dipindah.
- **Root cause #3**: loop `for (file in candidateFiles)` sekuensial, murni
  satu-per-satu -- 300 file yang lolos ke stability check = ~300 detik
  (1 detik delay/file berturutan tanpa paralelisme).
- **Fix (`FileSorter.kt` saja, 1 file)**: reorder cek-rule-dulu (murah)
  sebelum stability-check (mahal); bungkus seluruh `scanAndSortLocked()`
  dengan `withContext(Dispatchers.IO)`; proses tiap kandidat lewat
  `async` + `Semaphore(SCAN_CONCURRENCY=6)`, hasil digabung `awaitAll()`
  lalu diagregasi sekuensial (bukan mutable var lintas coroutine, hindari
  Mutex tambahan). Lihat Keputusan Arsitektur #6 & CHANGELOG v2.4.0.
- **Status**: BELUM dikonfirmasi user (CI build + tes di HP asli belum
  jalan saat entri ini ditulis). Sesi berikutnya WAJIB tanya dulu apakah
  v2.4.0 sudah terasa lebih cepat sebelum audit ulang dari nol.

### [2026-08-02] v2.3.7 GAGAL BUILD di CI -- animateItemPlacement salah pakai + alias ikon salah
- **Gejala**: user upload `build-failure-log-v2_3_7.zip` (log Gradle CI).
  `:app:compileDebugKotlin FAILED` dengan 4 error nyata di 3 file:
  `RuleListScreen.kt`, `ActivityLogScreen.kt`, `SkippedFilesScreen.kt`.
- **Root cause #1 (di ketiga file)**: `import
  androidx.compose.foundation.lazy.animateItemPlacement` SALAH TOTAL --
  `animateItemPlacement()` bukan top-level function, melainkan member
  extension dari `LazyItemScope` (otomatis tersedia tanpa import di dalam
  lambda `items { }`). Error compiler: "Unresolved reference:
  animateItemPlacement" persis di baris import.
- **Root cause #2 (di ketiga file, sekali root cause #1 dianggap "fixed"
  secara naif)**: fungsi ini ber-anotasi `@ExperimentalFoundationApi`.
  Tanpa `@OptIn` eksplisit di fungsi Composable pemanggil, Kotlin
  menjadikannya compile ERROR (bukan cuma warning) -- ini pola umum utk
  API yang perlu opt-in di Kotlin, BUKAN soal `allWarningsAsErrors` di
  Gradle config (project ini tidak mengaktifkan itu).
- **Root cause #3 (khusus `RuleListScreen.kt`)**: alias `import
  androidx.compose.material.icons.filled.Rule as RuleIcon` (dibuat sesi
  sebelumnya untuk mengatasi tabrakan nama dengan `data.Rule`) dipanggil
  sebagai `RuleIcon` polos -- SALAH, karena `Icons.Filled.Rule` adalah
  *extension property* dengan receiver `Icons.Filled`; alias tetap wajib
  dipanggil sebagai `Icons.Filled.RuleIcon`. Error: "receiver type
  mismatch".
- **Pelajaran penting**: sesi Claude yang membuat v2.3.7 TIDAK punya akses
  jaringan Gradle di sandbox-nya, jadi tidak bisa benar-benar
  mengompilasi kode sebelum dikirim -- sudah diberi disclaimer eksplisit
  di CHANGELOG v2.3.7, dan risikonya benar terjadi. **Kalau menambah API
  Compose yang jarang dipakai (terutama yang berbau "experimental" atau
  alias import untuk mengatasi tabrakan nama), ekstra hati-hati / pilih
  pendekatan paling sederhana yang tidak butuh alias sama sekali kalau
  bisa** -- lihat fix di bawah.
- **Fix (v2.3.8)**: hapus import salah di ketiga file, tambah
  `@OptIn(ExperimentalFoundationApi::class)` di fungsi Composable
  masing-masing (`RuleListScreen`, `ActivityLogScreen`,
  `SkippedFilesScreen`). Untuk ikon `RuleListScreen`, daripada
  memperbaiki alias yang sudah 2x salah, langsung diganti ke
  `Icons.Filled.PlaylistAdd` (ikon lain yang tidak collide dengan
  `data.Rule`, tidak perlu alias sama sekali).
- **Status**: fix sudah dikirim (v2.3.8) DAN user konfirmasi build CI
  sukses. Insiden ditutup.

### [2026-08-02] Batch finishing: fix izin legacy (ditunda -> dibenerin) + UI polish pertama
- User konfirmasi: layar Home di v2.3.1 sudah normal (regresi #1 di bawah
  benar-benar selesai, bukan cuma diklaim fix).
- User minta lanjut ke tahap "finishing": audit menyeluruh + robustness +
  polish UI/UX, digabung jadi satu ZIP.
- Karena audit/robustness sebagian besar sudah selesai di batch-batch
  sebelumnya (lihat bagian "Versi/batch terakhir" di atas), fokus utama
  finishing ini adalah: (1) menuntaskan 1 item robustness yang tadinya
  sengaja ditunda -- celah izin Android 8-10 -- karena user memilih "sekalian
  benerin sekarang" saat ditanya, dan (2) UI polish yang memang belum pernah
  disentuh sama sekali di project ini (empty state & animasi).
- **Perubahan izin (`MainActivity.kt`)**: `hasManageStoragePermission()`
  sekarang menerima `Context` dan benar-benar cek `ContextCompat.
  checkSelfPermission()` untuk API 26-29 (sebelumnya hardcode `true`).
  Ditambah `ActivityResultLauncher` (`RequestMultiplePermissions`) supaya
  user API 26-29 langsung dapat dialog izin sistem, bukan cuma dilempar ke
  halaman Setelan umum. `PermissionGate` dapat tombol fallback "buka
  Pengaturan Aplikasi" khusus untuk kasus izin ditolak permanen. Ditambah
  auto-recheck izin lewat `DisposableEffect` + `Lifecycle.Event.ON_RESUME`,
  supaya user tidak wajib pencet tombol "cek ulang" manual tiap balik dari
  Setelan (tombol manual tetap ada sebagai fallback).
- **UI polish (empty state)**: komponen baru `ui/components/EmptyState.kt`
  (ikon bulat bertema warna aksen layar + judul + pesan), menggantikan
  `Text()` polos yang sebelumnya dipakai berbeda-beda gaya di 4 layar:
  `RuleListScreen`, `ActivityLogScreen` (2 tab: Log & Undo), dan
  `SkippedFilesScreen`. Warna aksen ikut identitas 4-warna tiap layar dari
  Home (primary/hijau untuk Kelola Rule, tertiary/amber untuk Riwayat,
  secondary/stamp untuk File Dilewati).
- **UI polish (animasi)**: `Crossfade` untuk transisi kosong<->berisi konten
  di keempat layar tsb, `Modifier.animateItemPlacement()` di semua item
  `LazyColumn` (perlu tambah parameter `modifier` ke `RuleCard` yang
  sebelumnya tidak punya -- lihat catatan arsitektur di bawah), dan
  transisi fade+slide (`enterTransition`/`exitTransition`/dst.) di
  `NavHost` untuk perpindahan antar layar (sebelumnya potong instan tanpa
  animasi sama sekali).
- **Catatan arsitektur baru**: `RuleCard` (di `ui/components/RuleCard.kt`)
  sekarang punya parameter `modifier: Modifier = Modifier` mengikuti
  konvensi Compose standar (diteruskan ke `VaultCard` internal). Kalau
  nambah komponen list-item baru ke depannya, SELALU sediakan parameter
  `modifier` sejak awal supaya bisa dipasangi `animateItemPlacement()` atau
  modifier lain dari pemanggil tanpa perlu ubah signature belakangan.
- **Belum diverifikasi nyata di device** (build lokal tanpa akses jaringan
  Gradle di sesi Claude ini) -- hanya lolos `preflight_check.sh` + review
  manual menyeluruh tiap file yang diubah. Minta user konfirmasi build APK
  CI sukses & tampilan di HP sesuai sebelum dianggap selesai total.

### [2026-08-01] Regresi layar Home terpotong/ketumpuk setelah redesign v2.3.0
- **Gejala**: user screenshot Home screen -- judul "PromptVault" + subtitle +
  kartu statistik muncul, lalu jeda kosong besar, lalu MUNCUL LAGI judul yang
  sama, tombol "Scan Sekarang" dan menu grouped-list (Kelola Rule/Riwayat/
  Pengaturan/Diagnostik) TIDAK TERLIHAT sama sekali.
- **Kesalahan Claude di awal**: sempat curiga ini artefak alat screenshot
  ("scrolling capture") dan meminta user screenshot ulang 2x. User menegaskan
  ini screenshot biasa & terjadi konsisten (termasuk setelah force-stop app).
  ⚠️ Pelajaran: kalau user bilang sesuatu nyata & konsisten, jangan ulang
  minta verifikasi yang sama -- langsung audit kode.
- **Root cause #1 (bug kode, di `VaultCard.kt`)**: saat menambah gradient
  untuk redesign visual, content dibungkus `Box(Modifier.fillMaxSize())`
  DI DALAM `Surface`. Karena `Surface` aslinya wrap-content (ukurannya
  mengikuti konten), child yang minta `fillMaxSize()` justru memaksa
  `Surface` merebut SISA SELURUH tinggi `Column` induknya. Kartu statistik
  (isinya cuma 2 baris teks) jadi setinggi hampir seluruh layar, mendorong
  sibling di bawahnya (tombol Scan, GroupedList menu) ke luar area yang
  digambar/terlihat.
- **Root cause #2 (gap konfigurasi, di `AndroidManifest.xml`)**: `MainActivity`
  tidak punya `android:launchMode` (default `standard`). Berisiko membuat
  instance Activity baru menumpuk di atas instance lama dalam task yang sama
  kalau app dibuka ulang dari launcher saat masih berjalan di background --
  pola ini dikenal terjadi di custom ROM XOS (Infinix) dan cocok dengan
  gejala "layar dobel".
- **Fix**: v2.3.1 -- `VaultCard` diubah supaya gradient digambar via
  `Modifier.background(brush, shape)` langsung di `Surface` (tanpa Box
  fillMaxSize tambahan), dan `MainActivity` ditambah
  `android:launchMode="singleTask"`.
- **Status**: fix sudah dikirim ke user, BELUM ada konfirmasi eksplisit "sudah
  normal" dari user di sesi ini -- kalau sesi berikutnya user lanjut komplain
  soal layar Home, cek dulu apakah v2.3.1 benar sudah ke-install (bukan versi
  lama yang belum diupdate) sebelum mencari bug baru.

### [~2026-06 s/d 2026-08, sebelum PROJECT_STATE.md ini dibuat] Batch 1 & 2 backend
- Migrasi ActivityLog & MoveHistory dari DataStore JSON blob ke Room SQLite
  (Batch 1, v2.2.0) -- tanpa migrasi data lama (disepakati tidak urgent).
- Dual Stability Guard + filter file temp/partial-download (Batch 2, v2.2.1).
- Tidak ada insiden/regresi tercatat dari 2 batch ini.

## Cara cepat "onboarding" sesi baru
Ikuti `MAINTENANCE.md` (fetch README/CHANGELOG/MAINTENANCE dari GitHub raw
URL dulu sebelum minta user upload ZIP). File ini (`PROJECT_STATE.md`)
melengkapi `CHANGELOG.md` dengan konteks KEPUTUSAN & INSIDEN yang tidak masuk
akal ditulis sebagai entri changelog per-versi.
