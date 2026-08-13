# PROJECT_STATE.md -- PromptVault
> WAJIB dibaca Claude di awal SETIAP sesi baru, sebelum melanjutkan kerja apa
> pun. Jangan hapus riwayat insiden di bawah walau sudah lama/sudah fix --
> ini log kronologis permanen, bukan changelog fitur (itu ada di CHANGELOG.md).

## STATUS PROJECT: v2.18.1 -- FIX BUG NYATA: PREVIEW vs SCAN LIHAT FOLDER BEDA -- 2026-08-13
- User klarifikasi laporan v2.18.0: preview di layar Tambah/Edit Rule MUNCUL
  cocok, tapi scan asli tetap bilang "tidak ada file cocok". Digali lewat
  tanya-jawab (bukan tebak) -- ternyata bug terpisah, BUKAN soal ekstensi.
- Root cause: `previewPatternMatches()` hardcode selalu cek `downloadsDir`,
  buta total terhadap folder kustom SAF yang sudah dikonfigurasi -- padahal
  `scanAndSort()` sungguhan SUDAH benar mengarah ke folder kustom. Preview
  & scan cek folder BERBEDA. Detail lengkap: CHANGELOG.md v2.18.1.
- Fix: preview reuse `resolveSafRoot()` yang sama persis dengan scan asli
  (satu logika sumber, bukan 2 cabang independen -- pelajaran sama dengan
  syarat (c) Insiden #7). Signature `suspend` menjalar ke
  `MainViewModel.previewPattern` & param `onPreviewPattern` di
  `AddEditRuleScreen`.
- `scripts/preflight_check.sh` lolos bersih. **BELUM PERNAH lewat
  `./gradlew` asli** -- CI run berikutnya WAJIB dicek.
- **Pelajaran proses dicatat**: 3 laporan user berturut-turut ("tidak ada
  file cocok" -> "campuran ekstensi" -> "preview cocok tapi scan tidak")
  ternyata 2 bug BERBEDA (ekstensi v2.18.0 + preview/scan-mismatch v2.18.1)
  yang KEBETULAN bergejala mirip di awal. Pelajaran: jangan berhenti gali
  setelah fix pertama kalau user masih lapor gejala serupa -- tanya detail
  konkret ("preview vs scan beda?") sebelum asumsi "sudah kelar".

## STATUS PROJECT SEBELUMNYA: v2.18.0 -- DUKUNG SEMUA EKSTENSI FILE -- 2026-08-13
- User laporan bug pakai (bukan audit): pilih folder custom, isi "campuran"
  ekstensi, rule sudah aktif, tapi selalu "tidak ada file cocok". Root cause
  DITEMUKAN via tanya-jawab terarah (bukan tebak langsung): app dari awal
  project HARDCODE hanya scan `.zip`/`.txt` -- keputusan arsitektur inti,
  BUKAN bug, tapi tidak sesuai ekspektasi user pakai app ini buat "pindahkan
  file project" (general-purpose).
- Dikonfirmasi eksplisit ke user SEBELUM eksekusi (bukan asumsi diam-diam,
  karena ini scope-shift besar, nyentuh 8+ file termasuk data model) --
  user pilih "dukung SEMUA ekstensi", bukan whitelist tertentu.
- Detail teknis lengkap: CHANGELOG.md v2.18.0. Ringkasan: filter ekstensi di
  `listCandidateFiles`/`listCandidateFilesSaf` dihapus total, Rule/GlobMatcher
  jadi satu-satunya penentu match; `mimeTypeForFileName` diperluas ~15 tipe
  (fallback octet-stream tetap ada utk sisanya); string UI "ZIP/TXT"
  digenerickan di 5 layar. Scan TETAP non-rekursif (sengaja, di luar scope
  batch ini -- dicatat, bukan lupa).
- `scripts/preflight_check.sh` lolos bersih. **BELUM PERNAH lewat
  `./gradlew` asli** -- CI run berikutnya WAJIB dicek.

## STATUS PROJECT SEBELUMNYA: v2.17.1 -- FIX 2 BUG P0 FATAL SAF (AUDIT EKSTERNAL) -- 2026-08-13
- User upload `SAF_FINAL_LOGIC_AUDIT.md` (audit eksternal SAF v2.17.0 dari
  sesi/tool lain) via chat lain di conversation yang sama: 2 P0 fatal, 6 P1,
  3 P2. User pilih scope "Fix P0 saja, atomic change" -- BUKAN P1/P2 (masih
  pending, lihat CHANGELOG.md v2.17.1 untuk daftar lengkap sisa temuan).
- **Catatan penting proses**: batch ini dikerjakan di atas ZIP `__2_`
  (v2.17.0, sesi SAF terpisah), BUKAN base v2.16.1 yang sedang dikerjakan
  paralel di chat yang sama untuk task "Redesign Neumorphism" (v4.0.0,
  belum selesai -- lihat catatan di bawah, JANGAN tertukar/di-merge
  serampangan, dua base itu SENGAJA divergen sampai user putuskan urutan
  gabung). File log CI (`logs_85697644000.zip`) yang ikut ter-upload di
  waktu yang sama TERNYATA bukan repo ini (`Video-resizer`/
  `com.example.videoresizer`) -- diabaikan total, tidak dipakai debug.
- P0 #1 (validasi permission saat startup) & P0 #2 (silent fallback ke
  Downloads saat SAF rusak) -- detail teknis lengkap di CHANGELOG.md v2.17.1.
  `scripts/preflight_check.sh` lolos bersih. **BELUM PERNAH lewat
  `./gradlew` asli** -- CI run pertama WAJIB dicek sebelum dianggap selesai.

## STATUS PROJECT SEBELUMNYA: v2.17.0 -- SAF DITULIS ULANG (Folder Kustom) -- 2026-08-12
- User minta fitur SAF ditambahkan lagi ("penuh dedikasi bukan asal jadi").
  Prosedur di **Insiden #7** (bawah) DIIKUTI PERSIS sebelum kode ditulis:
  seluruh riwayat SAF dibaca dulu, lalu dikonfirmasi eksplisit ke user karena
  sandbox sesi ini TIDAK punya akses Gradle/emulator/device asli (syarat "a"
  gugur). User pilih lanjut ("gagal bukan pilihan") -> dieksekusi di bawah
  **syarat (c)**: blind, tapi disiplin -- reuse persis arsitektur legacy dari
  catatan penghapusan v2.13.0, BUKAN modul independen baru ("Zip Sorter"
  SENGAJA tidak diulang, itu sumber masalah "pelajaran tidak menyebar"
  di Insiden #7). Detail teknis lengkap & daftar file: lihat CHANGELOG.md
  v2.17.0. Ringkasan status:
  - Semua bug Insiden #4/#6/#7 (v2.8.0 CI-fail, Bug #1, Bug #2, boolean-gate
    false-negative) diaudit satu-satu, fix/mitigasi diterapkan dari desain
    awal (bukan ditambal belakangan) -- lihat catatan "UPDATE" di Insiden #7.
  - **BELUM PERNAH lewat `./gradlew` asli.** `scripts/preflight_check.sh`
    lolos bersih (setelah 1 iterasi fix: paren tak seimbang di komentar
    dokumentasi), plus review manual tipe/nullability/signature baris-per-
    baris. TAPI ini BUKAN pengganti compiler asli -- CI run PERTAMA setelah
    push ini WAJIB dicek hasilnya sebelum dianggap selesai. Kalau CI gagal,
    itu BUKAN berarti prosedur syarat (c) gagal -- itu justru skenario yang
    sudah diperingatkan sejak awal (lihat Insiden #7), lanjutkan dengan fix
    normal, bukan alasan mundur/hapus fitur lagi tanpa diskusi ke user dulu.
  - Temuan sampingan dicatat, TIDAK dieksekusi (di luar scope batch ini):
    `FileSorter.undo()` (kedua jalur) kemungkinan jalan di dispatcher
    pemanggil (Main), bukan `Dispatchers.IO` sendiri -- karakteristik lama,
    bukan regresi baru. Kandidat batch terpisah kalau mau dibenerin.

## STATUS PROJECT SEBELUMNYA: v2.16.0 -- TECHNICAL DEBT AUDIT & ATOMIC CLOSURE -- 2026-08-09
- User minta daftar SEMUA technical debt kode/fitur murni (bukan testing)
  yang belum kesampaian sejak awal project, dieksekusi jadi 1 batch atomic.
- **Metodologi audit**: baca ulang PROJECT_STATE.md penuh (semua entri
  insiden + section "Roadmap backend") + `grep -rn "TODO\|FIXME\|BELUM\|
  known limitation"` di seluruh `app/src/main/java` + cross-check kode
  aktual (bukan cuma percaya komentar, sesuai pelajaran Insiden #6: "kalau
  komentar bilang X, verifikasi X beneran terjadi").
- **Technical debt yang ditemukan & keputusan per item:**
  1. **[DIEKSEKUSI]** Opsi tema "Terang"/"Ikuti Sistem" di Pengaturan --
     dead code sejak v2.14.0 (`Theme.kt` hardcode gelap, `darkTheme` param
     diabaikan). Dihapus total ke akar (`ThemeMode` enum, flow, UI picker,
     seluruh wiring) -- BUKAN diimplementasi ulang jadi beneran terang,
     karena itu kontradiksi langsung spesifikasi desain tema yang sudah
     ditetapkan ("dark mode adalah satu-satunya mode").
  2. **[DIEKSEKUSI]** Tombol "Simpan" rule tanpa konfirmasi sukses eksplisit
     -- gap yang SUDAH ditemukan & DICATAT sejak audit v2.4.3 (2026-08-03),
     sengaja dibiarkan waktu itu. Ditambahkan `RuleSaveFeedback` one-shot
     StateFlow (pola sama persis dengan `ScanFeedback` v2.4.4), dikonsumsi
     di `RuleListScreen` (bukan di form sendiri, karena form di-dispose
     duluan sebelum Snackbar sempat tampil).
  3. **[DICATAT, TIDAK DIEKSEKUSI]** Data lama di DataStore (Rules/Settings)
     dari sebelum migrasi Room v2.2.0 tidak pernah dimigrasikan otomatis.
     Sudah "disepakati tidak urgent" sejak awal (lihat "Keputusan arsitektur
     utama" #1) -- TIDAK dieksekusi ulang di batch ini karena: (a) app belum
     pernah rilis publik luas sejauh riwayat project ini, kemungkinan ada
     user nyata yang masih di versi >20 rilis lampau (<v2.2.0) mendekati
     nol; (b) skema DataStore key lama kemungkinan sudah berubah bentuk di
     beberapa batch berikutnya, menulis migrasi "buta" tanpa data sampel
     nyata berisiko menciptakan bug baru lebih besar dari manfaatnya. Kalau
     user based di masa depan benar melaporkan data hilang setelah update
     dari versi sangat lama, INI baru trigger valid untuk dikerjakan.
  4. **[DICATAT, TIDAK DIEKSEKUSI]** `SCAN_CONCURRENCY = 6` (FileSorter,
     v2.4.0) adalah **asumsi teknis AI**, belum divalidasi profiling nyata
     di device user, dan tidak configurable dari Settings. TIDAK diubah
     sekarang karena tidak ada data profiling utk pilih angka lain yang
     lebih benar -- mengubah angka tanpa data cuma ganti tebakan dengan
     tebakan lain. Trigger valid: user laporkan scan lambat/berat nyata di
     Downloads berisi ribuan file.
  5. **[DICATAT, TIDAK DIEKSEKUSI, TERHAMBAT STRUKTURAL]** §6 roadmap
     backend (CI/CD dependency-lock lanjutan, `./gradlew --write-locks`) --
     STATUS TIDAK BERUBAH dari catatan lama: sandbox Claude di sesi ini pun
     masih tanpa akses Gradle/Android SDK/network. Menulis lockfile "buta"
     berisiko mematikan build CI total (sama kelas risiko dengan kenapa SAF
     akhirnya dihapus, Insiden #7) -- BUKAN dieksekusi blind.
  - **Item YANG SUDAH TERTUTUP, bukan debt lagi** (diverifikasi ulang di
    audit ini, dicatat supaya sesi depan tidak audit ulang dari nol): SAF/
    Scoped Storage & Zip Sorter (v2.13.0, dihapus total, grep konfirmasi 0
    sisa kode `documentfile`/`zipsorter`), §2 MediaStore ghost cleanup
    (v2.5.0, selesai), §5 Coroutine lifecycle & Foreground Service (v2.6.0,
    selesai), 3 pelanggaran spesifikasi tema (v2.15.0, ditutup sesi
    sebelumnya).
- File diubah (6), murni lapisan UI/state -- nol perubahan logika
  scan/move/undo/DB: `data/SettingsRepository.kt`, `ui/theme/Theme.kt`,
  `ui/screens/SettingsScreen.kt`, `ui/MainViewModel.kt`, `MainActivity.kt`
  (Protected Asset, edit parsial), `ui/screens/RuleListScreen.kt`.
- versionCode 55->56, versionName 2.15.0->2.16.0.
- **Belum ada konfirmasi CI/device dari user untuk versi ini.**

## STATUS PROJECT SEBELUMNYA: v2.15.0 -- AUDIT KEPATUHAN 100% ke spesifikasi tema (gap closure) -- 2026-08-09
- User eksplisit minta tema di-override ULANG, "100% disesuaikan dengan
  instruksi Markdown, jangan menyisakan celah setitik pun" -- bukan laporan
  bug baru, tapi audit ulang v2.14.0/2.14.1 vs dokumen spesifikasi
  file-per-file, komponen-per-komponen.
- **3 pelanggaran nyata ditemukan** (bukan cuma selisih nilai token):
  1. `GroupedListRow.kt` -- 4 ikon menu Home SEMUANYA pakai
     `Modifier.shadow()` berwarna tint sebagai glow permanen sejak v2.14.0
     (bahkan sebelum itu, ini bukan bagian dari perubahan tema kemarin,
     cuma baru ketahuan sekarang karena audit eksplisit diminta) -- persis
     masuk daftar "Forbidden" bab 18 spesifikasi ("Every icon"). Fix: kotak
     ikon jadi glass datar (fill tint alpha rendah + border rambut), TANPA
     shadow warna.
  2. `RuleCard.kt` -- toggle enable/disable rule masih `Switch` Material3
     bawaan, bukan kontrol tactile sesuai bab 12. Fix: komponen baru
     `TactileSwitch.kt` (track recessed/OFF vs terangkat-tint-glow lokal/ON,
     posisi thumb sebagai penanda kedua di luar warna -- bab 21).
  3. `VaultActionSheet.kt` -- sheet konfirmasi (hapus rule, undo, dst) flat
     tanpa tepi glass sama sekali (gap bab 7/8). Fix: highlight rambut 1dp
     di top + `shadowElevation=0` (bukan border penuh, sesuai arah cahaya
     bab 9 -- highlight itu "reflected light", bukan outline kotak).
- **2 presisi token** di `Color.kt`: `MidnightBlueGradientAlpha` disamakan
  PERSIS ke 0.06f (spesifikasi bab 6 `MidnightBlueAmbientAlpha`), sebelumnya
  0.08f (bukan pelanggaran keras -- dokumen bilang nilai boleh disetel --
  tapi user minta nol celah, jadi disamakan persis). Tambah `TextMuted`
  (0xFF737E8C) yang disebut eksplisit di bab 16 tapi belum ada di kode.
- **SENGAJA TIDAK diubah**: opsi "Terang"/"Ikuti Sistem" di
  `SettingsScreen.kt` (`ThemeMode`) yang sudah tidak fungsional sejak
  v2.14.0 -- ini keputusan fitur/navigasi/state (`SettingsRepository`,
  wiring `MainActivity`), BUKAN bagian dari dokumen spesifikasi VISUAL yang
  jadi acuan audit batch ini. Tetap berlaku known-limitation, silakan minta
  eksplisit "hapus opsi tema terang di Pengaturan" kalau mau dibersihkan --
  itu batch terpisah di luar lapisan tema murni.
- File diubah (5) + 1 baru, murni lapisan visual/komponen, nol logika bisnis
  disentuh: `ui/theme/Color.kt`, `ui/components/GroupedListRow.kt`,
  `ui/components/RuleCard.kt`, `ui/components/VaultActionSheet.kt`,
  `app/build.gradle.kts` (versi), BARU `ui/components/TactileSwitch.kt`.
- versionCode 54->55, versionName 2.14.1->2.15.0.
- **Belum ada konfirmasi CI/device dari user untuk versi ini.**

## STATUS PROJECT SEBELUMNYA: v2.14.1 -- FIX REGRESI: CTA "Scan Sekarang" pucat/glitch akibat shadow tactilePress -- 2026-08-08
- User laporkan screenshot: tombol CTA gradient stamp->amber punya kotak pucat
  aneh di tengahnya (bukan shadow halus). Root cause: v2.14.0 mengganti
  `.pressScale()` polos di CTA jadi `.tactilePress()` yang menambah
  `Modifier.shadow(elevation=4.dp)` bahkan di state idle (bukan cuma saat
  ditekan). Di banyak device/skin Android, `Modifier.shadow` di atas
  Brush.horizontalGradient custom sering fallback render jadi kotak
  translusen pucat, bukan shadow bertitik gelap yang mulus -- terutama kalau
  compositing layer-nya tidak match warna background gradient.
- **Fix**: CTA kembali pakai `.pressScale()` polos (skala saja, tanpa
  shadow layer tambahan) -- ini juga lebih sesuai bab 7 spesifikasi tema
  ("Avoid: glossy glass-button appearance; excessive bevel").
  `tactilePress()` di `PressScale.kt` TETAP ada (tidak dihapus) untuk
  kontrol lain di masa depan, tapi TIDAK dipakai lagi di CTA Home.
- 1 file (`ui/screens/HomeScreen.kt`), murni revert 1 modifier + import.
- versionCode 53->54, versionName 2.14.0->2.14.1.

## STATUS PROJECT SEBELUMNYA: v2.14.0 -- Ganti total tema visual ke "AMOLED Glassmorphism Hybrid + Midnight Blue Gradient" -- 2026-08-08
- User upload spesifikasi desain (.md) & minta tema default project ditimpa
  sampai bersih, 100% sesuai isi dokumen itu. Palet lama "Manifest Arsip"
  (kraft/pine terang + obsidian gelap terpisah, dua skema) DIHAPUS TOTAL,
  diganti SATU skema: AMOLED near-black + frosted glass (dominan) + tint
  Midnight Blue ambient alpha-rendah (restrained, bukan warna dominan).
- **Dark mode sekarang WAJIB & satu-satunya** -- `PromptVaultTheme` tidak lagi
  punya `lightColorScheme`; parameter `darkTheme` diabaikan (dipertahankan di
  signature supaya `MainActivity` tidak perlu diubah strukturnya).
  KNOWN LIMITATION: opsi "Terang"/"Ikuti Sistem" di menu Pengaturan
  (`SettingsScreen.kt`, `ThemeMode`) masih ada di UI tapi SEKARANG TIDAK
  BERFUNGSI lagi (tampilan selalu AMOLED gelap). Sengaja TIDAK dibersihkan
  di batch ini untuk menjaga batch tetap ketat di lapisan tema; hapus UI
  picker itu di batch berikutnya kalau user minta "beres-beres" lanjutan.
- File diubah/ditambah (9, murni lapisan tema + 1 titik tactile-press CTA):
  `ui/theme/Color.kt` (rewrite total token AMOLED/glass/Midnight Blue),
  `ui/theme/Theme.kt` (rewrite, satu darkColorScheme, forced),
  `ui/theme/TactileTokens.kt` (BARU -- konstanta elevasi/skala tactile
  terpusat sesuai bab 12 spesifikasi), `ui/components/VaultCard.kt`
  (gradient glass + tint Midnight Blue + border rambut, ganti gradient
  kraft lama), `ui/components/PressScale.kt` (tambah `tactilePress()`:
  skala + elevasi turun ke 0 saat ditekan, sesuai bab 6), `ui/screens/
  HomeScreen.kt` (CTA "Scan Sekarang" pakai `tactilePress` bukan
  `pressScale` polos), `app/src/main/res/values/colors.xml` +
  `themes.xml` (splash & parent theme non-light), `mipmap-anydpi-v26/
  ic_launcher(.xml/_round.xml)` (background ikon ganti dari pv_pine ke
  AMOLED), `MainActivity.kt` (partial: splash/status-bar scrim gelap
  permanen, bukan lagi `SystemBarStyle.auto`).
- Semua komponen lain (`GroupedListRow`, `RuleCard`, `SegmentedControl`,
  `VaultActionSheet`, `VaultTopBar`, `EmptyState`, `SortedStamp`, seluruh
  layar) TIDAK diubah -- semuanya sudah 100% theme-aware lewat
  `MaterialTheme.colorScheme`/`VaultTheme.extraColors`, jadi otomatis
  mewarisi palet baru tanpa perlu disentuh. Nol risiko regresi logika.
- versionCode 52->53, versionName 2.13.0->2.14.0.
- **Belum ada konfirmasi CI/device dari user untuk versi ini.**

## STATUS PROJECT SEBELUMNYA: v2.13.0 -- SAF DIHAPUS TOTAL ke akar atas permintaan eksplisit user -- 2026-08-08
- User eksplisit minta hapus SEMUA fitur terkait SAF sampai bersih ke akar,
  dan HANYA diterapkan kembali kalau Claude sudah tahu letak kesalahan
  logika fatal yang menyebabkan riwayat panjang bug SAF (insiden #4, #6,
  plus 2 gagal-build CI v2.6.0-terkait-manifest tidak relevan, tapi v2.8.0
  gagal build LANGSUNG karena kode SAF). Lihat "Insiden #7" di bawah untuk
  analisis root-cause lengkap dan kenapa jawabannya SEKARANG adalah TIDAK
  diterapkan kembali (bukan "belum sempat").
- Dihapus total: §1 roadmap backend (SAF/Scoped Storage, semua fase v2.7.0-
  v2.10.0) DAN modul "Zip Sorter" (v2.12.0, SAF-based juga). `FileSorter`
  kembali single-path java.io.File/Downloads murni, persis seperti sebelum
  v2.7.0. Detail file-per-file di CHANGELOG v2.13.0.
- Rule engine utama, Room DB (log/history), worker auto-sort, crash logger,
  UNDO -- semuanya TIDAK disentuh, nol risiko regresi dari batch ini.
- versionCode 51->52, versionName 2.12.0->2.13.0.
- **Belum ada konfirmasi CI/device dari user untuk versi ini** -- tapi ini
  operasi SUBTRAKSI kode (bukan fitur baru), risiko jauh lebih rendah
  daripada batch-batch SAF sebelumnya.
- Status "SELESAI/STABLE" (declared 2026-08-04, lihat bawah) TETAP BERLAKU
  untuk seluruh rule engine utama.

## STATUS PROJECT SEBELUMNYA: v2.12.0 COMPILE-FIX terkirim (v2.12.1 label, versi tetap 2.12.0) -- 2026-08-07
- Fix `zipSorterViewModel` Unresolved reference (NavHost ada di fungsi
  top-level `PromptVaultRoot`, bukan di class Activity langsung -- lihat
  CHANGELOG v2.12.1). 1 file (`MainActivity.kt`), murni compile-fix.
- **Belum ada konfirmasi CI hijau dari user untuk fix ini.**
- Status "SELESAI/STABLE" (declared 2026-08-04, lihat bawah) TETAP BERLAKU
  untuk seluruh rule engine utama -- TIDAK dibatalkan/di-audit ulang.
- User eksplisit upload dokumen boilerplate & minta modul baru "Zip Sorter"
  (kategori file otomatis + auto-extract ZIP) diintegrasikan, package
  disesuaikan ke `com.elprompter.promptvault`, plus contoh ViewModel+Screen
  SAF `ACTION_OPEN_DOCUMENT_TREE`. Ini exception valid dari aturan permanen
  #3 di bawah (fitur baru spesifik diminta user, bukan audit proaktif).
- Diimplementasi sebagai package **terisolasi total** `zipsorter/` --
  TIDAK menyentuh `util/FileSorter.kt` (rule engine lama) sama sekali.
  Detail lengkap + bug di dokumen sumber yang diperbaiki: lihat CHANGELOG
  v2.12.0.
- **BELUM diverifikasi runtime** (build CI + device asli) -- sesi
  berikutnya JANGAN anggap modul ini matang sampai user konfirmasi.
- versionCode 50->51, versionName 2.11.1->2.12.0.

## STATUS PROJECT SEBELUMNYA: SELESAI / STABLE (declared 2026-08-04)
- **v2.4.2 dinyatakan resmi sebagai STABLE RELEASE.** User (pemilik project)
  eksplisit bilang capek dan minta project ini benar-benar dinyatakan
  "selesai" tanpa audit tak berujung. Kriteria "Definition of Done" di bawah
  ini SEMUA terpenuhi per 2026-08-04:
  - Audit kode menyeluruh seluruh modul: SELESAI TOTAL (lihat entri
    2026-08-01 "PENUTUP audit" di bawah).
  - Semua bug yang pernah ditemukan sepanjang riwayat project: fixed &
    dikonfirmasi (race condition v2.3.3, dialog rule v2.3.4, worker
    lifecycle v2.3.5, izin storage v2.3.7, packaging ZIP Claude 2026-08-04,
    CI GitHub Release v2.4.2).
  - Performa (scan paralel v2.4.0, trim berkala v2.4.1): confirmed cepat di
    device asli user.
  - CI/CD: compile-check, unit test, build APK, publish ke GitHub Release
    -- **CONFIRMED user APK sudah muncul di sidebar Releases** setelah
    v2.4.2 (konfirmasi diterima 2026-08-04, closing item yang sebelumnya
    "BELUM ada konfirmasi").
  - 4 item roadmap backend (SAF, MediaStore ghost cleanup, Foreground
    Service, CI lock lanjutan) TETAP DIJEDA -- statusnya sudah final
    "ditunda dengan trigger eksplisit" (lihat bagian Roadmap backend di
    bawah), BUKAN dihitung sebagai pekerjaan belum selesai.
- **ATURAN PERMANEN untuk sesi Claude berikutnya (jangan dilanggar):**
  1. JANGAN menawarkan audit/polish/"mau lanjut apa lagi?" secara proaktif.
     Project ini stabil -- diam kecuali user yang mulai duluan.
  2. 4 item roadmap backend yang dijeda TETAP dijeda selamanya, kecuali
     salah satu trigger eksplisit di bagian "Roadmap backend" benar-benar
     terjadi (bukan dibahas ulang saat idle/nganggur).
  3. Eksekusi HANYA kalau user secara eksplisit minta fitur baru spesifik
     atau melaporkan bug/gejala nyata (termasuk lewat crash log
     Documents/PromptVault/logs/).
  4. Kalau ragu apakah sesuatu "perlu dibenahi", DEFAULT-nya adalah TIDAK --
     tanya user dulu, jangan asumsikan perlu audit ulang.

## Insiden #6 -- listCandidateFilesSaf gagal detect file (MIME false-negative), SAMA KELAS BUG dgn insiden #4 (2026-08-06)
- **Koreksi**: kesimpulan insiden #4 (folder kosong = bukan bug) SALAH.
  User klarifikasi: folder ADA banyak file, tidak ke-scan sama sekali.
  1 file yang berhasil (18:53:46) itu KEBETULAN lolos, bukan bukti fitur
  sehat -- 82 file "dilewati" scan itu, sisanya yg ratusan scan berikutnya
  malah 0 kandidat sama sekali.
- **Root cause**: `listCandidateFilesSaf()` syaratkan `doc.isFile == true`.
  `DocumentFile.isFile()` query MIME type ke provider -- kalau MIME kosong/
  salah (umum, tergantung cara file itu nyampe: SD card/sync app/dll),
  `isFile()` FALSE NEGATIF walau file valid & bisa dibaca. Persis kelas bug
  yang sama dengan `resolveSafRoot` exists()/canRead() (insiden #4 versi
  lama). Karena MIME per-file beda-beda tergantung asal filenya, hasilnya
  "acak" -- sebagian file lolos, sebagian tidak, cocok dgn gejala user.
- **Fix**: ganti syarat `doc.isFile` -> `!doc.isDirectory`. `isDirectory()`
  cek `MIME_TYPE_DIR` yang jauh lebih konsisten diisi provider drpd MIME
  type detail file individual -- dipakai sbg negative check yg reliable,
  bukan positive check yg rawan.
- **PELAJARAN PERMANEN**: SEMUA method boolean `DocumentFile` (`isFile`,
  `canRead`, `canWrite`, `exists`) TIDAK BOLEH dipercaya sbg gerbang
  keputusan tanpa probe akses nyata (`listFiles()`, baca konten, dst) --
  ini kelas bug berulang di batch SAF, cek ulang SEMUA pemakaian method2
  ini di file ini kalau ada laporan gejala serupa lagi.
- **Sekalian dibenerin (masih atomic, 1 modul FileSorter.kt SAF)**: 2 gerbang
  boolean serupa di `undoSaf()` -- `current.exists()` & `originalRoot.canWrite()`
  -- dibuang dgn pola sama (null/isDirectory check + biarkan operasi nyata
  createFile/copy yg gagal natural, ketangkap try-catch existing).
- **BELUM dikonfirmasi user** -- tunggu hasil scan + Salin Log berikutnya.


- **Konteks**: user butuh cara cepat ekstrak log ERROR (dari fix
  resolveSafRoot sebelumnya) tanpa ADB/Logcat, sementara pesan "Tidak ada
  file cocok yang ditemukan" identik dipakai baik saat SAF gagal->fallback
  MAUPUN SAF sukses tapi folder emang kosong -- tidak bisa dibedakan dari
  layar Home.
- **Fix**: `ActivityLogScreen` tab "Log" dapat `IconButton` (ContentCopy) di
  top bar -- nyalin SEMUA entri log (bukan cuma yg terlihat di layar) ke
  clipboard via `ClipboardManager`, format `[yyyy-MM-dd HH:mm:ss] LEVEL:
  pesan`, urutan sama seperti tampilan (terbaru dulu). Hanya muncul di tab
  Log, snackbar konfirmasi "Log disalin ke clipboard".
- versionCode 43->44, versionName 2.8.1->2.8.2 (fitur user-visible baru,
  konsisten dgn kebijakan bump-utk-traceability dari insiden #5).
- **Belum terjawab**: apakah SAF sebenarnya gagal atau folder custom
  memang kosong saat scan -- tunggu user kirim hasil Salin Log.


- **Laporan user**: build CI ijo (fix resolveSafRoot), tapi halaman GitHub
  Release masih kelihatan APK/versi sebelumnya.
- **Root cause**: tag release = `v${versionName}`. 2 hotfix beruntun
  (coroutineScope + resolveSafRoot) SENGAJA tidak naikkan versionName
  (dianggap syntax/logic fix murni) -> tag tetap `v2.8.0` sama dgn build
  sebelumnya -> `action-gh-release` UPDATE release yang sama (by design),
  jadi tampilan (judul/tag/nomor versi) di halaman Release TIDAK BERUBAH
  sama sekali walau asset APK di baliknya mestinya ketimpa. Tidak ada cara
  visual buat user verifikasi APK baru benar ke-publish -- kelihatan macam
  stuck/gagal padahal desainnya memang begitu.
- **Koreksi kebijakan**: hotfix yang mengubah PERILAKU RUNTIME nyata (bukan
  cuma syntax, spt resolveSafRoot) WAJIB bump versi biar dapat tag/entri
  Release baru & bisa diverifikasi. Hotfix syntax-only (spt
  return@coroutineScope) boleh tanpa bump SELAMA belum pernah publish sukses
  sebelumnya di versi itu.
- **Fix**: versionCode 42->43, versionName 2.8.0->2.8.1.


- **Laporan user (runtime, HP asli)**: pilih folder kustom via picker, taruh
  file ZIP/TXT di dalamnya, scan -> "Tidak ada file cocok yang ditemukan."
  Tidak ada error apapun ditampilkan. Ini verifikasi runtime PERTAMA untuk
  §1 Fase 2, dan GAGAL.
- **Root cause**: `resolveSafRoot()` pakai `doc.exists() && doc.canRead()`
  sebagai gerbang sebelum mempercayai tree URI. Keduanya method
  `DocumentFile` yang TERKENAL false-negative di banyak `DocumentProvider`
  (kartu SD, beberapa file manager OEM) karena bergantung pada
  `COLUMN_FLAGS` yang provider sering tidak isi lengkap -- padahal folder
  sebenarnya bisa diakses. Begitu gerbang gagal, `resolveSafRoot()` diam-diam
  `return null` (BY DESIGN, supaya SAF gagal tidak pernah crash user) ->
  `scanAndSortLocked()` fallback total ke Downloads/java.io.File -> scan
  "sukses" tapi baca folder yang salah, TANPA jejak error ke user.
- **Fix**: buang gerbang `exists()`/`canRead()`. Validasi cukup
  `doc.isDirectory`, lalu `doc.listFiles()` dipanggil sebagai PROBE akses
  nyata di dalam try-catch yang sudah ada -- kalau memang tidak bisa dibaca,
  provider akan melempar Exception asli, bukan heuristik yang salah. Tambah
  `activityLogRepository.add(LogLevel.ERROR, ...)` di kedua jalur gagal (tree
  invalid / listFiles() exception) supaya kegagalan SAF SEKARANG TERLIHAT di
  Riwayat Aktivitas -- sebelumnya 100% silent.
- **KOREKSI 2026-08-06 (lihat Insiden #6)**: kesimpulan CONFIRMED di atas
  SALAH -- user klarifikasi folder TIDAK kosong, ratusan file gagal
  ke-detect krn bug baru `listCandidateFilesSaf` (`doc.isFile` false-negatif,
  fixed v2.8.3). §1 balik ke status BELUM confirmed runtime sepenuhnya.


- **Build v2.8.0 CONFIRMED GREEN di CI (2026-08-06)** setelah ronde 2 fix
  di bawah. Compile-only confirmation -- runtime di device ASLI masih
  BELUM diverifikasi untuk §1 Fase 2 SAF (batch paling berisiko).
- **Build v2.8.0 FAILED di CI**: `FileSorter.kt` baris 357 & 364, dalam
  `scanAndSortSafLocked()` (batch SAF Fase 2, 2026-08-05) -- `return` polos
  dipakai di dalam blok `coroutineScope { ... }`. Parameter `block` di
  `kotlinx.coroutines.coroutineScope` adalah `crossinline`, jadi non-local
  `return` DILARANG compiler ("'return' is not allowed here"). Baris 394 di
  fungsi yang sama sudah benar pakai `return@coroutineScope` -- 2 baris awal
  (early-return saat rules kosong / candidateFiles kosong) kelewatan saat
  batch itu ditulis.
- **Fix**: ganti `return ScanResult(...)` -> `return@coroutineScope
  ScanResult(...)` di kedua baris. Tidak ada perubahan logika/behavior,
  murni syntax fix. versionCode/versionName TETAP 42/2.8.0 (belum pernah
  publish sukses).

## Versi/batch terakhir yang selesai
- **versionCode 42 / versionName 2.8.0 -- §1 roadmap backend Fase 2/2 SELESAI
  (FileSorter pakai DocumentFile), 2026-08-05:** §1 SEKARANG FUNGSIONAL
  PENUH. `scanAndSortLocked()` cek `resolveSafRoot()` di awal -- ada URI SAF
  valid -> delegasi total ke `scanAndSortSafLocked()` (jalur DocumentFile
  baru, terpisah total dari jalur legacy); tidak ada/tidak valid -> fallback
  ke Downloads/java.io.File PERSIS seperti sebelumnya. `undo()` jadi
  dispatcher berdasarkan `destUri.startsWith("content://")`.
  `MoveHistoryEntity` (Protected: DB Schema/DAO) TIDAK diubah -- sudah
  bertipe String generik dari awal. Keterbatasan sengaja: preview
  pattern/diagnostik tetap baca Downloads walau SAF aktif (fungsi
  non-suspend); Dual Stability Guard SAF cuma 2/3 sinyal (tanpa file-lock
  check); §2 ghost-cleanup tidak jalan di jalur SAF; move/undo pakai
  copy-lalu-hapus bukan `DocumentsContract.moveDocument`. Detail lengkap +
  checklist verifikasi WAJIB di CHANGELOG v2.8.0. **INI BATCH PALING
  BERISIKO SEJAUH INI (menyentuh fungsi inti scan/move/undo) DAN SAMA
  SEKALI BELUM ADA KONFIRMASI RUNTIME** -- preflight statis lolos 10/10
  tapi itu bukan jaminan kompilasi/behavior benar. WAJIB jalankan checklist
  5 poin di CHANGELOG v2.8.0 sebelum dianggap stabil, terutama poin 1
  (mode Downloads/non-SAF tidak boleh regresi).
- **versionCode 41 / versionName 2.7.0 -- §1 roadmap backend Fase 1/2
  (SAF folder picker, HYBRID, dormant), 2026-08-05:** Sesuai keputusan user
  (hybrid, bukan full-replace): infrastruktur SAF picker + penyimpanan URI
  ditambah (`SettingsRepository`, `MainViewModel`, `MainActivity`,
  `SettingsScreen`), TAPI `FileSorter.kt` BELUM disentuh sama sekali --
  scan/move/undo tetap 100% java.io.File untuk semua user, URI SAF baru
  tersimpan belum dipakai. Fase 2 (FileSorter baca URI & pakai DocumentFile,
  dengan fallback) sengaja DIPISAH ke batch berikutnya supaya risiko kecil
  per langkah. Detail lengkap di CHANGELOG v2.7.0. **BELUM ada konfirmasi
  runtime -- WAJIB dikonfirmasi (picker muncul, URI persist lintas restart
  app, tombol hapus jalan) SEBELUM lanjut ke Fase 2.** Urutan roadmap
  sekarang: §2 selesai -> §5 selesai -> §1 Fase 1 selesai -> **§1 Fase 2
  (berikutnya)**, lalu masuk prioritas audit eksternal (unit/UI test,
  optimasi search/indexing, dst).
- **versionCode 40 / versionName 2.6.0 -- §5 roadmap backend selesai
  (Coroutine lifecycle & Foreground Service), 2026-08-05:**
  **[COMPILE-FIX 2026-08-05]** CI gagal `processDebugMainManifest`:
  `AndroidManifest.xml` punya `--` di dalam 2 komentar (XML melarang keras
  substring itu di badan `<!-- -->`, beda dari komentar Kotlin `//`). Fix:
  ganti `--` jadi koma di kedua komentar, tidak ada perubahan logika. Detail
  di CHANGELOG v2.6.0. `AutoSortWorker` sekarang `setForeground()` (notifikasi ongoing low-priority) sebelum scan
  mulai, supaya OS tidak gampang menjeda/membunuh worker saat scan panjang
  di background. Audit coroutine lifecycle: TIDAK ada bug, `withContext(IO)`
  + `async`/`awaitAll()` yang sudah ada dari v2.4.0 sudah cooperative
  cancellation otomatis lewat structured concurrency. File baru
  `AutoSortNotification.kt` + edit `AutoSortWorker.kt`, `PromptVaultApp.kt`,
  `AndroidManifest.xml` (parsial), `strings.xml`. Detail lengkap di
  CHANGELOG v2.6.0.
- **versionCode 39 / versionName 2.5.0 -- COMPILE-FIX 2026-08-04:** user
  upload log CI build gagal (`compileDebugKotlin` FAILED,
  `FileSorter.kt:272:39`: "Suspend function 'add' should be called only
  from a coroutine or another suspend function"). Root cause: fungsi
  `cleanupGhostMediaStoreEntries()` (bagian §2 roadmap backend MediaStore
  ghost cleanup di v2.5.0) lupa diberi keyword `suspend`, padahal manggil
  `activityLogRepository.add()` yang suspend. Fix: tambah `suspend` di
  deklarasi fungsi itu -- pemanggil (`scanAndSortLocked`, sudah suspend
  context) tidak perlu diubah. 1 baris, 1 file. Static re-scan seluruh
  pemanggilan suspend lain di `FileSorter.kt` (undo, moveFile,
  processCandidate, scanAndSortLocked) -- semua sudah konsisten, tidak ada
  mismatch lain. **Belum ada konfirmasi build hijau dari CI/device asli --
  tunggu run berikutnya.**
- **versionCode 38 / versionName 2.4.4** -- user laporkan GEJALA NYATA:
  Snackbar hasil scan (fitur baru v2.4.3) muncul berulang tiap habis buka
  "Lihat detail file yang dilewati" lalu balik ke Home. Root cause: event
  one-shot ditampilkan via `LaunchedEffect` di composable `HomeScreen`, tapi
  Navigation Compose men-dispose+membuat-ulang `HomeScreen` tiap pindah
  layar -- instance baru tidak tahu event sudah pernah tampil, jadi
  Snackbar re-trigger. Fix: state "sudah dikonsumsi" dipindah ke
  `MainViewModel.consumeScanFeedback()` (survive dispose composable), + fix
  efek samping warna Snackbar (`activeIsError` local snapshot). Detail
  teknis lengkap di CHANGELOG v2.4.4. **Belum dikonfirmasi user di HP asli.**
- **versionCode 37 / versionName 2.4.3** -- user MINTA sendiri (bukan audit
  proaktif Claude, sesuai aturan permanen #3 di atas) audit tuntas sektor
  "feedback interaksi": apa yang user harapkan terjadi tiap kali dia
  berinteraksi dengan app. Audit statis nyisir 8 screen + MainViewModel,
  grep pola Snackbar/Toast/haptic. Hasil audit lengkap:
  - **Sudah OK (tidak disentuh):** RuleListScreen (Snackbar "Rule dihapus"),
    ActivityLogScreen (Snackbar "dikembalikan ke Downloads"), haptic
    LongPress di VaultActionSheet (semua konfirmasi destruktif) & RuleCard
    (drag/reorder), SettingsScreen (FilterChip highlight utk tema/interval/
    conflict strategy sudah cukup sebagai feedback pilihan, import rule
    sudah ada teks hasil persisten di layar).
  - **GAP ditemukan & difix:** tombol "Scan Sekarang" di HomeScreen -- aksi
    PALING SERING dipakai di seluruh app -- sebelumnya cuma update teks
    pasif `lastScanSummary`. Kalau hasil scan kali ini teksnya identik
    dengan sebelumnya (skenario umum: "Tidak ada file cocok" berulang),
    user nol sinyal bahwa tombol barusan benar-benar merespons.
  - **GAP dicatat, SENGAJA belum difix (di luar scope batch ini):** tombol
    "Simpan" di AddEditRuleScreen tidak punya konfirmasi sukses eksplisit
    (Snackbar/toast) -- saat ini feedback-nya cuma implisit lewat navigasi
    balik ke list. Cukup untuk sekarang karena rule baru langsung kelihatan
    di list, tapi kalau user suatu saat komplain "kayak gak kesave", ini
    kandidat fix pertama yang harus dicek. Jangan diasumsikan sudah beres.
  - **Fix teknis:** `MainViewModel.scanFeedback` (StateFlow baru, terpisah
    dari `lastScanSummary`, dibedakan `eventId`=timestamp biar tetap
    trigger walau teks sama) + `HomeScreen` dapat `SnackbarHost` & haptic
    (`TextHandleMove` normal / `LongPress` utk folder tak terbaca, warna
    Snackbar ganti `colors.error` saat error) + wiring 1 param baru di
    `MainActivity.kt`. 4 file, dalam Batch Lock. Detail lengkap di
    CHANGELOG v2.4.3.
  - **Belum diverifikasi runtime** (sandbox tanpa Gradle) -- preflight
    LOLOS semua kategori, tapi tunggu konfirmasi kamu di HP asli.
- **versionCode 36 / versionName 2.4.2** -- rilis terakhir yang dikirim ke
  user. Fix: `.github/workflows/build.yml` SEBELUMNYA cuma pakai
  `actions/upload-artifact@v4` (Actions Artifact biasa), TIDAK PERNAH
  benar-benar publish ke GitHub Release -- melanggar aturan proyek sendiri
  ("GitHub Release Rule"). Fix: tambah `permissions:
  contents: write` + step `softprops/action-gh-release@v2` (tag otomatis
  `v<versionName>`, APK ter-attach, update release yang sama kalau tag sudah
  ada). Preflight ditambah kategori #9 supaya gap ini tidak lolos lagi.
  HANYA `.github/workflows/build.yml` + `scripts/preflight_check.sh` yang
  diubah. Lihat CHANGELOG v2.4.2 untuk detail teknis lengkap.
  **CONFIRMED 2026-08-04: user cek, APK sudah muncul di sidebar Releases.**
- **versionCode 35 / versionName 2.4.1** -- rilis sebelumnya. Trim berkala
  tiap 20 insert di ActivityLogRepository & MoveHistoryRepository (bukan
  tiap insert) -- kurangi write-contention SQLite saat scan paralel. Lihat
  CHANGELOG v2.4.1.
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
  - §2 MediaStore rescan/ghost file cleanup -- **SELESAI** (v2.5.0, 2026-08-04).
    User eksplisit minta tuntaskan semua item dijeda; spec asli tidak ada
    teksnya di repo, didesain ulang dari standar Android + konteks app
    (`MediaScannerConnection.scanFile()` tiap move/undo + query cleanup
    ghost entry sekali per scan). BELUM diverifikasi runtime -- lihat
    CHANGELOG v2.5.0 untuk detail & yang perlu dikonfirmasi user.
  - §1 SAF/Scoped Storage abstraction -- **DITULIS ULANG (v2.17.0,
    2026-08-12)**, folder kustom opsional aktif lagi. Riwayat: "kode selesai,
    runtime belum stabil" di v2.7.0-v2.8.3 (dual-path DocumentFile/java.io.File)
    -> diperluas jadi modul "Zip Sorter" terpisah di v2.12.0 (SAF juga) ->
    riwayat bug berulang di kelas yang sama (boolean DocumentFile gate
    false-negatif, izin persisted bocor, mime type tidak reliable, 1x gagal
    build CI) -> **DITUTUP, DIHAPUS TOTAL (v2.13.0, 2026-08-08)** atas
    permintaan eksplisit user, dengan syarat reapply spesifik (Insiden #7) ->
    **DITULIS ULANG lagi (v2.17.0)** di bawah syarat (c) Insiden #7 (blind,
    disiplin, TANPA Zip Sorter). BELUM diverifikasi runtime/CI asli -- baca
    Insiden #7 LENGKAP (termasuk UPDATE 2026-08-12) SEBELUM menulis perubahan
    BESAR lain ke kode SAF di project ini.
  - §5 Coroutine lifecycle & Foreground Service -- **SELESAI** (v2.6.0,
    2026-08-05). Audit lifecycle: tidak ada bug (structured concurrency
    sudah cukup). Fix nyata: `AutoSortWorker` promosi ke foreground service
    lewat `setForeground()`. Lihat CHANGELOG v2.6.0. BELUM diverifikasi
    runtime.
  - §6 CI/CD preflight+dependency lock lanjutan -- **BELUM, TERHAMBAT
    STRUKTURAL** (2026-08-04: Gradle dependency locking butuh `./gradlew
    --write-locks` yang JALAN, sandbox Claude tidak punya Android SDK/Gradle/
    akses network -- lockfile yang di-generate tanpa itu isinya spekulatif
    dan BISA MEMATIKAN BUILD CI TOTAL kalau salah. TIDAK dikerjakan blind;
    butuh sesi dengan akses Gradle asli, atau instruksi lebih spesifik dari
    user soal ruang lingkupnya.)
  - **Keputusan 2026-08-04 (menggantikan keputusan 2026-08-01 di bawah):**
    user eksplisit minta tuntaskan §1/§2/§5/§6 SEKARANG (bukan tunggu
    trigger). Dieksekusi BERTAHAP satu atomic batch per sesi/pesan (bukan
    sekaligus -- 4 item beda area arsitektur, digabung sekaligus melanggar
    Batch Lock "maks 1 modul per batch" versi user sendiri). Urutan
    dieksekusi: §2 (selesai) -> §5 (selesai) -> §1 (berikutnya). §6 diskip
    sampai ada akses Gradle nyata atau instruksi scope lebih spesifik.
  - **Keputusan 2026-08-05**: audit eksternal masuk (skor 9.2/10, fokus
    testing/performa/backup-recovery). User putuskan: tuntaskan §5 lalu §1
    dulu (roadmap lama), BARU habis itu masuk ke prioritas dari audit
    eksternal tsb.
  - ~~Keputusan 2026-08-01 (SUDAH DIGANTI di atas): jangan lanjutkan tanpa
    trigger gejala nyata.~~
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
util/FileSorter.kt -- logika inti scan & pindah file (java.io.File/Downloads
                     sbg default; SAF/DocumentFile utk folder kustom opsional
                     sejak v2.17.0 -- lihat Keputusan Arsitektur #2 & Insiden #7)
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
2. **FileSorter DUAL-PATH lagi sejak v2.17.0** (2026-08-12) -- java.io.File/
   Downloads tetap DEFAULT untuk semua user, TAPI folder kustom opsional
   lewat SAF/`DocumentFile` kini tersedia lagi (`SettingsRepository.safTreeUriFlow`
   set -> `FileSorter` pakai jalur SAF; kosong -> fallback Downloads, tanpa
   pengecualian). **Riwayat status sebelumnya, JANGAN dibaca sebagai kontradiksi**:
   sempat dual-path (v2.8.0-v2.12.0) -> dihapus total jadi single-path murni
   (v2.13.0, atas permintaan eksplisit user setelah 6 versi bermasalah
   berturut-turut, lihat Insiden #7) -> ditulis ulang jadi dual-path lagi
   (v2.17.0, di bawah syarat (c) Insiden #7, sesudah dikonfirmasi eksplisit ke
   user). Kalau ke depan SAF perlu diubah BESAR lagi (bukan bugfix kecil),
   baca Insiden #7 LENGKAP (termasuk UPDATE 2026-08-12 di dalamnya) dulu,
   JANGAN cuma baca poin ini.
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

### [2026-08-12] v2.17.0 -- SAF ditulis ulang mengikuti prosedur Insiden #7 (bukan insiden baru, ini eksekusi remediasinya)
- **Trigger**: user minta "tambahkan fitur SAF, dengan penuh dedikasi bukan
  asal jadi" di chat baru (project di-upload sbg ZIP, tanpa histori chat
  sebelumnya di sesi ini).
- **Prosedur yang diikuti** (BUKAN langsung nulis kode): baca `PROJECT_STATE.md`
  penuh dulu (prioritas #2 konteks) -> ketemu Insiden #7 -> verifikasi ULANG
  klaim "0 sisa kode SAF" pakai `grep` sendiri (bukan percaya log begitu saja,
  sesuai pelajaran Insiden #6) -> confirmed bersih -> cek 3 syarat reapply:
  (a) akses Gradle/device asli? TIDAK ADA di sandbox sesi ini. (b) user kasih
  scope beda + terima risiko eksplisit? User cuma bilang "tambahkan SAF" (scope
  SAMA seperti dulu), belum eksplisit soal risiko. (c) reuse persis path
  legacy, bukan modul independen baru? BISA dipenuhi (CHANGELOG v2.13.0 kasih
  daftar persis file/fungsi yang dulu disentuh).
- **Konfirmasi ke user SEBELUM nulis kode**: dijelaskan temuan di atas +
  rekomendasi jalan (c) lewat `ask_user_input_v0` (3 opsi: disiplin (c) /
  scope beda / tunda). User jawab bebas teks "gagal bukan pilihan" (bukan
  salah satu label tombol persis) -- diinterpretasi sbg pilihan (c) implisit
  (bukan (b), karena tidak ada scope baru yang diberikan; bukan "tunda").
  **Dicatat eksplisit supaya sesi depan tidak salah baca**: "gagal bukan
  pilihan" adalah ekspektasi user, BUKAN jaminan teknis yang bisa dipenuhi
  sandbox tanpa compiler asli -- lihat catatan proporsionalitas di UPDATE
  Insiden #7 di bawah.
- **Yang dieksekusi**: lihat CHANGELOG.md v2.17.0 untuk daftar lengkap
  file/fungsi. Ringkas: `FileSorter.kt` dapat jalur SAF paralel (bukan
  gantikan) jalur Downloads lama, titik cabang tunggal di `scanAndSort()`;
  `SettingsRepository`/`MainViewModel`/`MainActivity`/`SettingsScreen` dapat
  wiring folder-kustom (persis 4 file yang sama seperti CHANGELOG v2.13.0
  sebutkan pernah disentuh); `DiagnosticsScreen.kt` & `data/db/**` SENGAJA
  TIDAK disentuh (di luar scope "folder kustom", beda dari scope "Zip Sorter"
  dulu).
- **Verifikasi yang benar-benar dijalankan** (bukan diklaim): `preflight_check.sh`
  dijalankan SEBELUM klaim selesai (bukan sesudah) -- run pertama FAIL (paren
  tak seimbang, ternyata typo tanda kurung di KOMENTAR dokumentasi
  `copyDocumentBytes()`, bukan di kode nyata, tapi tetap diperbaiki krn
  script ini mandatory-pass menurut dirinya sendiri), run kedua bersih. Review
  manual tambahan (bukan bagian preflight): ditemukan & diperbaiki SENDIRI 1
  isu nyata (draft awal `processCandidateSaf` menelan `CancellationException`
  lewat `catch(Exception)` polos yang membungkus `delay()` -- lihat detail di
  CHANGELOG). Ini contoh KONKRET kelas kesalahan yang preflight statis TIDAK
  bisa tangkap (persis peringatan root-cause Insiden #7 poin 1) -- ditemukan
  lewat reasoning manual, bukan alat otomatis.
- **Batas jujur yang HARUS dipahami sesi berikutnya**: semua di atas adalah
  upaya PALING DISIPLIN yang mungkin TANPA compiler asli. Ini BUKAN sama
  dengan "sudah pasti kompil". Kalau CI/build user gagal setelah ZIP ini
  di-push, itu BUKAN tanda prosedur (c) salah diikuti -- itu tanda kenapa
  syarat (a) [akses Gradle asli] ada di Insiden #7 sejak awal. Tindak lanjut
  yang benar: kirim error log CI ke sesi Claude berikutnya untuk di-fix
  bertarget, BUKAN kembali ke opsi "hapus total lagi" tanpa didiskusikan dulu
  ke user (penghapusan v2.13.0 dulu itu keputusan EKSPLISIT user, bukan default
  otomatis kalau ada bug).

### [2026-08-08] Insiden #7 -- SAF dihapus total: root-cause analysis kenapa TIDAK diterapkan kembali sekarang
- **Permintaan user**: hapus semua fitur SAF sampai bersih ke akar, DAN
  terapkan kembali kalau Claude sudah tahu letak kesalahan logika fatal yang
  Claude sebabkan sendiri. Ini eksekusi bagian pertama (hapus). Bagian kedua
  (reapply) dievaluasi di bawah -- kesimpulannya: TIDAK sekarang.
- **Riwayat SAF di project ini** (2026-08-05 s/d 2026-08-07, v2.7.0-v2.12.0,
  6 versi berturut-turut): gagal build CI 1x (v2.8.0, coroutineScope receiver
  hilang), 2 bug runtime nyata dari insiden #4 & #6 (boolean DocumentFile
  method jadi gerbang trust yang false-negatif -- exists/canRead/isFile),
  1 bug izin bocor (persisted permission tidak pernah dilepas), 1 bug mime
  type tidak reliable. SETIAP fix yang dikirim membawa pelajaran "permanen"
  baru yang TERNYATA tidak otomatis dicegah di kode berikutnya.
- **Root cause SEBENARNYA (bukan cuma daftar bug individual di atas)**: dua
  hal struktural yang saling memperkuat --
  1. **Kode SAF selalu ditulis \"blind\"** -- sandbox Claude di sesi manapun
     TIDAK PERNAH punya akses Gradle/device asli untuk benar-benar
     mengompilasi atau menjalankan kode `DocumentFile`/`ContentResolver`
     sebelum dikirim. Preflight statis (`preflight_check.sh`) tidak bisa
     menangkap kesalahan tipe/scope Kotlin yang butuh compiler asli (lihat
     insiden v2.8.0), apalagi perilaku runtime provider SAF yang memang
     terkenal tidak konsisten antar OEM/kartu SD/app sumber file.
  2. **Pelajaran dari 1 bug tidak otomatis menular ke kode SAF lain yang
     ditulis TERPISAH.** Bukti paling jelas: pelajaran "jangan pakai
     exists()/canRead()/isFile() sbg gerbang" dari insiden #4/#6 SUDAH
     didokumentasikan sebagai "PELAJARAN PERMANEN" -- tapi begitu modul Zip
     Sorter (v2.12.0) ditulis SEBAGAI IMPLEMENTASI SAF KEDUA yang independen
     (`zipsorter/repository/ZipSorterRepository.kt`, bukan reuse dari
     `FileSorter.kt`), pelajaran itu harus SENGAJA ditulis ulang manual jadi
     komentar baru di file baru itu supaya tidak terulang -- artinya
     mekanisme "belajar dari insiden lama" project ini ADALAH komentar kode,
     bukan sesuatu yang dijamin dipatuhi otomatis oleh sesi Claude
     berikutnya yang menulis SAF-code baru dari nol.
  - Kombinasi 2 hal ini = kelas kegagalan yang SIFATNYA BERULANG, bukan bug
    tunggal yang begitu di-fix otomatis selesai. Fix `isDirectory` vs
    `isFile` (v2.8.3) itu SENDIRI valid & benar -- tapi itu obat untuk GEJALA
    (satu instance boolean-gate salah), bukan untuk PENYAKIT (implementasi
    SAF ditulis tanpa verifikasi nyata, berulang kali, tanpa mekanisme yang
    mencegah pola yang sama muncul lagi di kode SAF berikutnya).
- **Kenapa TIDAK diterapkan kembali sekarang**: kondisi yang menyebabkan
  penyakit di atas TIDAK BERUBAH -- sesi Claude ini juga tidak punya akses
  Gradle/device asli. Menulis ulang SAF "dengan versi yang sudah benar"
  sekarang akan mengulang persis kondisi yang sama (kode SAF baru, ditulis
  blind, tidak ada jaminan lolos compiler asli) yang menghasilkan 6 versi
  bermasalah sebelumnya. Mengklaim sudah "tahu letak kesalahan fatal" lalu
  langsung reapply TANPA mengubah kondisi struktural itu hanya akan
  memindahkan siklus fix-gagal-fix ke versi berikutnya dengan nama file yang
  beda.
- **Syarat sebelum SAF boleh diterapkan kembali** (semua harus benar,
  bukan salah satu): (a) ada sesi dengan akses Gradle/emulator/device asli
  untuk verifikasi nyata SEBELUM dikirim ke user, ATAU (b) user eksplisit
  menerima risiko "belum tentu jalan, perlu ronde fix seperti sebelumnya"
  DAN memberi instruksi scope spesifik (bukan re-run spec lama yang sama),
  ATAU (c) kalau memang harus blind lagi, implementasi WAJIB reuse 100% path
  yang sama dengan legacy (tidak ada implementasi SAF kedua yang independen
  seperti Zip Sorter) supaya pelajaran lama otomatis berlaku, bukan perlu
  ditulis ulang manual per modul.
- **Status**: SAF dihapus (lihat CHANGELOG v2.13.0). Roadmap §1 (SAF/Scoped
  Storage) dan modul Zip Sorter dianggap DITUTUP, bukan "dijeda" -- beda dari
  status "ditunda dengan trigger eksplisit" di 4 item roadmap backend lain
  (lihat bagian Roadmap backend di bawah, yang itu SUDAH selesai semua per
  2026-08-05/06 kecuali §1 ini yang sekarang dicabut).
- **UPDATE [2026-08-12]**: syarat (c) di atas DIPAKAI, SAF ditulis ulang di
  v2.17.0 (custom folder picker terintegrasi ke `FileSorter.kt`, Zip Sorter
  TETAP tidak diulang). Entri asli di atas DIBIARKAN UTUH (bukan dihapus/
  ditimpa) sesuai instruksi header file ini -- lihat "STATUS PROJECT: v2.17.0"
  di paling atas file ini & CHANGELOG.md untuk detail lengkap apa yang
  dieksekusi dan bagaimana tiap bug di atas diaudit ulang satu-satu. Root
  cause struktural (poin 1: kode SAF selalu ditulis blind) MASIH BERLAKU --
  batch v2.17.0 TIDAK mengklaim sudah lolos compiler asli, cuma preflight
  statis + review manual. Sesi berikutnya: JANGAN anggap SAF otomatis "aman
  selamanya" cuma karena sudah ditulis ulang sekali -- kalau ada perubahan
  BESAR lagi ke kode SAF di masa depan (bukan bugfix kecil), baca ulang
  seluruh entri Insiden #7 ini dari awal, bukan cuma baca UPDATE ini saja.

### [2026-08-06] v2.8.0 GAGAL BUILD di CI -- `async{}`/`awaitAll()` tanpa CoroutineScope receiver di `scanAndSortSafLocked()`
- **Gejala**: user upload `build-failure-log-v2_8_0.zip`. `:app:compileDebugKotlin FAILED` -- `Unresolved reference` di `FileSorter.kt:368-369` (`async`, `awaitAll`) dan efek domino di `:376` (`it`).
- **Root cause (kesalahan Claude)**: `scanAndSortSafLocked()` (fungsi baru batch §1 Fase 2 SAF) ditulis sebagai `private suspend fun` polos, BEDA dari `scanAndSortLocked()` legacy yang dibungkus `withContext(Dispatchers.IO){}` -- sehingga tidak ada receiver `CoroutineScope` untuk `async{}` di dalamnya. Lolos audit statis karena `preflight_check.sh` tidak (dan tidak bisa dengan mudah) mengecek kecocokan scope coroutine.
- **Fix**: bungkus body fungsi dengan `coroutineScope{}` (bukan `withContext` baru, karena caller sudah di dalam `withContext(Dispatchers.IO)` saat memanggil) + import `kotlinx.coroutines.coroutineScope`. `return` awal tetap valid (non-local return, `coroutineScope` inline), return terakhir jadi `return@coroutineScope`. Tidak ada perubahan logika. versionCode/versionName TIDAK naik (tetap 42/2.8.0) -- ini compile-fix, bukan fitur baru; Fase 2 SAF masih perlu konfirmasi runtime setelah build ini hijau.
- **Pelajaran untuk sesi Claude berikutnya**: setiap `suspend fun` baru yang isinya pakai `async{}`/`launch{}` WAJIB dicek py punya `CoroutineScope` receiver (`withContext{}`/`coroutineScope{}`/`supervisorScope{}`) -- jangan asumsikan `suspend fun` otomatis dapat itu. Kandidat perbaikan `preflight_check.sh`: grep file yang mengandung `async {` atau `launch {`, pastikan ada `withContext(` / `coroutineScope {` / `supervisorScope {` di fungsi pembungkusnya.
- **Status**: fix sudah dikirim, belum ada konfirmasi CI hijau dari user.

### [2026-08-05] v2.6.0 GAGAL BUILD di CI -- "--" di dalam komentar XML AndroidManifest.xml
- **Gejala**: user upload `build-failure-log-v2_6_0.zip`. `:app:processDebugMainManifest FAILED` -- `SAXParseException: The string "--" is not permitted within comments`, baris 14 `AndroidManifest.xml`.
- **Root cause (kesalahan Claude)**: 2 komentar penjelasan batch §5 memakai `--` sebagai pemisah kalimat (kebiasaan dari komentar Kotlin `//`), tapi spec XML 1.0 MELARANG substring `--` di badan komentar `<!-- -->` mana pun, bukan cuma di Android. `preflight_check.sh` tidak menangkap ini karena kategori #8 cuma validasi YAML CI, tidak ada cek well-formedness XML manifest/resource.
- **Fix**: ganti `--` jadi koma di kedua komentar, tidak ada perubahan logika. Lihat CHANGELOG v2.6.0.
- **Pelajaran untuk sesi Claude berikutnya**: JANGAN pakai `--` di dalam komentar `<!-- -->` XML apapun (manifest, layout, semua res/*.xml) -- pakai koma atau titik. Idealnya `preflight_check.sh` ditambah kategori validasi XML well-formed (`xml.dom.minidom.parse` per file `.xml`) sebelum ZIP dikirim -- belum ditambahkan ke script, catat sebagai item untuk sesi berikutnya kalau relevan.
- **Status**: fix sudah dikirim (v2.6.0 revisi), belum ada konfirmasi CI hijau dari user.

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

### [2026-08-07] v2.9.0 -- Crash Logger bawaan ditambahkan
- **Kenapa**: fitur ini sudah lama jadi requirement standar user (lihat
  instruksi baku sesi), tapi belum pernah diimplementasi di kode -- diaudit
  sesi ini (`grep -rn crash app/src/main/java` nihil) lalu dibangun dari nol.
- **Desain**: lihat `util/CrashLogger.kt` (javadoc lengkap di file). Ringkas:
  `Thread.setDefaultUncaughtExceptionHandler` dipasang di `PromptVaultApp`,
  tulis ke `MediaStore.Files` (`Documents/PromptVault/logs/`), fail-safe,
  FIFO 50 file.
- **Belum ada**: UI in-app untuk browse/lihat log (baru bisa diakses via
  file manager/adb). Kalau user minta viewer, itu batch terpisah -- jangan
  gabung ke perubahan lain supaya tetap dalam batas 10 file/1 modul.
- **State lain masih sama seperti entri di atas**: fix Home screen v2.3.1
  BELUM ada konfirmasi eksplisit "sudah normal" dari user.

### [2026-08-07] v2.9.1 -- Viewer crash log ditambah di Diagnostik
- Lanjutan v2.9.0. `DiagnosticsScreen` sekarang punya card "Crash Log" (list
  + tap-to-view AlertDialog). Menutup gap "belum ada UI viewer" yang dicatat
  di entri sebelumnya.
- CI belum dikonfirmasi hijau untuk versi ini.

### [2026-08-07] v2.10.0 -- Debugging fokus SAF: 2 bug nyata ditemukan & diperbaiki
- **Bug #1**: `releasePersistableUriPermission()` didokumentasikan tapi TIDAK
  PERNAH diimplementasi -- izin folder kustom lama menumpuk selamanya tiap
  ganti/hapus folder. Risiko jangka panjang: kena limit OS (~128 persisted
  permission/app), fitur folder kustom berhenti berfungsi diam-diam. Fix di
  `MainViewModel.setSafTreeUri()`/`clearSafTreeUri()`.
- **Bug #2**: mime type buat `createFile()` SAF dipercaya dari provider
  SUMBER (`doc.type`), bukan diturunkan dari ekstensi -- berisiko nama
  dobel-ekstensi di provider tertentu. Fix: `mimeTypeForFileName()` baru
  (murni dari ekstensi), + verifikasi nama aktual pasca-create, log WARNING
  kalau provider ubah nama sendiri.
- **Cara nemuinnya**: grep `takePersistableUriPermission` cross-reference ke
  `releasePersistableUriPermission` (nihil) -- gap antara komentar/dokumentasi
  kode dan implementasi aktual. Pelajaran: kalau komentar bilang "dilakukan di
  X", SELALU verifikasi X benar-benar melakukannya, jangan percaya komentar
  begitu saja.
- **Belum diverifikasi nyata**: butuh user test folder kustom di kartu SD/
  provider selain Downloads, plus build CI sukses, sebelum dianggap matang
  100%. Kalau CI hijau & user konfirmasi folder kustom jalan normal (pindah,
  undo, ganti folder berkali-kali tanpa error), SAF bisa dianggap matang.

### [2026-08-07] v2.11.0 -- Fix UNDO: hasil palsu + pesan hardcode
- Fitur UNDO ternyata sudah 100% fungsional dari sesi sebelumnya (bukan TODO
  lagi) -- yang jadi bug adalah UI-nya BOHONG soal hasil (selalu bilang
  sukses) dan pesannya hardcode "Downloads" walau bisa folder SAF kustom.
- Fix: `undoMove()` suspend + return Boolean asli, snackbar sesuai hasil
  nyata, guard anti-double-tap.
- **Pelajaran sesi ini**: TODO comment lama di kode TIDAK SELALU akurat --
  fitur bisa sudah selesai tapi komentarnya lupa diupdate. Selalu baca kode
  aktual dulu sebelum asumsi dari nama TODO.
- CI belum dikonfirmasi hijau untuk versi ini.

### [2026-08-07] v2.11.1 -- Status: semua fitur inti lengkap, siap uji nyata
- Audit menyeluruh selesai: 3 bug nyata ditemukan & fixed sesi ini (SAF izin
  bocor, SAF mime type, UNDO hasil palsu). Semua TODO lama di kode ternyata
  sudah selesai fungsional, tinggal label basi -- sudah dibersihkan.
- **Satu-satunya item genuinely pending**: verifikasi nyata di perangkat fisik
  (build CI + install + test manual semua fitur, terutama SAF & Undo). Claude
  tidak punya akses build/device untuk verifikasi ini sendiri.
- Kalau sesi depan mulai lagi tanpa temuan bug baru dari user, TIDAK PERLU
  re-audit kode yang sama dari nol -- cek dulu apakah ada laporan masalah
  baru dari user/CI sebelum grep ulang seluruh codebase.

### [2026-08-09] v2.16.1 -- Hotfix build: `shadowElevation` bukan param ModalBottomSheet
- CI gagal 2x (v2.16.0 attempt) di `compileDebugKotlin`: `ModalBottomSheet`
  di `VaultActionSheet.kt` pakai `shadowElevation = 0.dp`, padahal param itu
  cuma ada di `Surface`, bukan di `ModalBottomSheet` versi material3 1.2.x
  (compose-bom 2024.06.00) yang dipakai project ini. Fix: ganti jadi
  `tonalElevation = 0.dp` (efek visual setara -- flat, no Material elevation).
- **Pelajaran**: kalau nambah param elevation ke composable Material3 baru,
  SELALU cek signature komposable itu spesifik (ModalBottomSheet != Surface),
  jangan asumsi semua composable "container" M3 punya param yang sama.
- Belum diverifikasi CI hijau untuk versi ini -- perlu push & cek Actions.
