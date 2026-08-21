# PROJECT_STATE.md -- PromptVault
> WAJIB dibaca Claude di awal SETIAP sesi baru, sebelum melanjutkan kerja apa
> pun. Jangan hapus riwayat insiden di bawah walau sudah lama/sudah fix --
> ini log kronologis permanen, bukan changelog fitur (itu ada di CHANGELOG.md).

## v8.15.1 -- Audit UX 100% batch 2: guard double-tap "Simpan" (2026-08-21)
- Lanjutan pending queue #1 dari v8.15.0. `AddEditRuleScreen.kt`: tombol
  "Simpan" sebelumnya `enabled` hanya cek isi field, TIDAK guard terhadap
  tap cepat 2x saat `onCheckBeforeSave` (suspend) masih in-flight -- bisa
  trigger 2 proses cek/simpan bertumpuk.
- Fix: state `isSaving` (pola sama seperti `undoInFlight` di
  `ActivityLogScreen.kt`) -- `true` sebelum `scope.launch`, `false` setelah
  `onCheckBeforeSave` selesai; `enabled` tombol ikut cek `!isSaving`.
- File diubah (1): `AddEditRuleScreen.kt`. `app/build.gradle.kts` (versi).
- Preflight: N/A (tidak ada script gradlew di sandbox ini, cek manual OK).
- **BELUM PERNAH lewat `./gradlew` asli / device asli.** User WAJIB
  verifikasi di HP: tap cepat 2x tombol "Simpan" saat menambah/edit rule
  TIDAK lagi memicu 2 proses simpan bertumpuk, tombol sempat nonaktif
  singkat lalu normal lagi.
- **PENDING QUEUE (masih tersisa dari v8.15.0, belum dikerjakan)**:
  1. `OnboardingScreen.kt` -- verifikasi manual perlu `TextField`/
     `KeyboardOptions` ada/tidak (lihat detail di entri v8.15.0 di bawah).
  2. Cakupan lanjutan: kontras warna disabled-state, konsistensi durasi
     animasi, predictive back gesture, landscape/tablet layout.
- versionCode 108->109, versionName 8.15.0->8.15.1.

## v8.15.0 -- Audit UX 100% (batch 1: fix nama file mentah tanpa maxLines) (2026-08-21)
- **Instruksi langsung user**: "audit UX 100%". Audit dilakukan lintas
  SEMUA layar (`ui/screens/*.kt`, 9 file) + komponen (`ui/components/*.kt`,
  10 file) -- cakupan dicek: accessibility `contentDescription`, ukuran
  touch target, dialog konfirmasi utk aksi destruktif, feedback (snackbar)
  setelah aksi, cakupan `EmptyState`, `maxLines`/`TextOverflow` utk teks
  dinamis, `KeyboardOptions`/`ImeAction` di form.
- **1 BUG NYATA ditemukan & diperbaiki (sesuai batch limit -- max 3 file)**:
  `entry.fileName`/`info.fileName`/`entry.displayName` (nama file MENTAH
  dari filesystem, sering tanpa spasi) ditampilkan TANPA `maxLines` di 3
  layar -- beresiko WORD-BREAK PAKSA di tengah token saat tidak muat 1
  baris. **BUKTI NYATA, bukan spekulasi**: screenshot referensi v2.20.3 di
  sesi ini SUDAH menunjukkan gejalanya --
  `"AudioPlayer-v1.0.34-release-run146.apk"` pecah jadi
  `"...release-r"` / `"un146.apk"` di baris ke-2, tinggi row jadi tidak
  konsisten antar entri.
  - Fix: `maxLines = 1, overflow = TextOverflow.Ellipsis` (konvensi standar
    list file Android) di 4 titik, 3 file: `ActivityLogScreen.kt` (baris
    file di tab Undo), `SkippedFilesScreen.kt` (baris "Detail File
    Dilewati"), `DiagnosticsScreen.kt` (2 titik: daftar kandidat Downloads
    + baris crash log yang isi `entry.displayName`-nya format
    `crash_<timestamp>_<UUID>.txt`, UUID = token panjang tanpa titik
    break alami).
- **PENDING QUEUE (ditemukan saat audit, BELUM dikerjakan -- batch
  berikutnya)**:
  1. **`AddEditRuleScreen.kt`** -- tombol "Simpan" (`enabled =
     folderName.isNotBlank() && ...` SUDAH ada, itu bagus) TAPI `onClick`
     men-trigger `scope.launch { onCheckBeforeSave(rule) ... }` TANPA
     guard against double-tap SAAT coroutine check masih in-flight -- tap
     cepat 2x sebelum check pertama selesai berpotensi trigger 2 proses
     cek/simpan bertumpuk. Fix tersedia: state `isSaving` +
     `enabled = ... && !isSaving`, pola SAMA seperti yang sudah dipakai di
     `undoInFlight` (`ActivityLogScreen.kt`).
  2. **`OnboardingScreen.kt`** -- 0 `KeyboardOptions`/`ImeAction` ditemukan
     lewat grep. BELUM diverifikasi manual apakah layar ini memang tidak
     punya `TextField` sama sekali (kalau iya, N/A bukan bug) atau ada
     input yang lolos kena grep pattern beda. Perlu `view` manual sebelum
     diputuskan ada bug atau tidak.
  3. Cakupan audit BELUM menyentuh: kontras warna teks disabled-state,
     konsistensi durasi animasi/transisi antar layar, perilaku predictive
     back gesture (Android 13+), landscape/tablet layout. Kalau user mau
     lanjut "audit UX 100%" lebih dalam, area ini titik mulai berikutnya.
- File diubah (3): `ActivityLogScreen.kt`, `SkippedFilesScreen.kt`,
  `DiagnosticsScreen.kt` (tambah import `TextOverflow` + `maxLines`/
  `overflow` di masing2 Text nama file). `app/build.gradle.kts` (versi).
  `FILE_MANIFEST.txt` TIDAK berubah.
- Preflight: 13/13 kategori PASS.
- **BELUM PERNAH lewat `./gradlew` asli / device asli.** User WAJIB
  verifikasi di HP: nama file panjang di tab "Undo Pemindahan", "Detail
  File Dilewati", & Diagnostics TIDAK lagi pecah 2 baris (dipotong "..."
  di 1 baris), tidak ada regresi baca nama file pendek yang sudah muat 1
  baris.
- versionCode 107->108, versionName 8.14.0->8.15.0.

## v8.14.0 -- Eksperimen percepatan kompilasi CI, batch 2 (gabung invocation + KSP incremental) (2026-08-21)
- Lanjutan v8.13.0 (CI CONFIRMED hijau, run #118, 7m15s) -- instruksi
  langsung user: "Gabung 3 invocation+KSP incremental".
- **`.github/workflows/build.yml` (protected asset, edit PARSIAL)**: 3
  invocation `./gradlew` terpisah (`compileDebugKotlin` -> `testDebugUnitTest`
  +`testReleaseUnitTest` -> `assembleRelease`) DIGABUNG jadi 1 step
  "Compile, test, and build release APK" dgn 1 invocation berisi ke-4 task
  sekaligus -- alasan: `configuration-cache` (aktif sejak v8.13.0) di-key
  per SET task yang diminta, jadi config utk request "compileDebugKotlin
  saja" TIDAK otomatis kepake ulang utk request "assembleRelease" (set
  task beda) -- gabung 1 invocation = project CUMA dikonfigurasi SEKALI
  utuh. Step "Decode keystore" DIPINDAH ke ATAS step gabungan ini (SEBELUMNYA
  di antara "Run unit tests" & "Build release APK" lama) krn `assembleRelease`
  butuh file+env signing SEJAK AWAL invocation gabungan (tidak bisa lagi
  didekode di tengah, karena tengahnya sudah tidak ada step terpisah).
  Fail-fast TETAP terjaga: tanpa `--continue`, task gagal = build berhenti,
  task berikutnya tidak jalan -- perilaku setara 3 step lama.
- Log 3 file lama (`compile-check.log`/`unit-tests.log`/`assemble-release
  .log`) jadi 1 file `build-all.log` -- step "Upload build log on failure"
  disesuaikan (path lama akan 404 diam2 krn `if-no-files-found: ignore`
  kalau tidak diperbaiki; SUDAH diperbaiki batch ini).
- **`gradle.properties` (parsial)**: +1 baris `ksp.incremental=true` --
  SUDAH default true sejak KSP 1.0.4+ (versi proyek 1.9.24-1.0.20), baris
  ini eksplisit murni supaya niatnya tercatat, BUKAN perubahan perilaku.
  Room compiler (`ksp("androidx.room:room-compiler")`) satu-satunya
  processor KSP di proyek ini.
- File diubah (3): `.github/workflows/build.yml` (parsial),
  `gradle.properties` (parsial), `app/build.gradle.kts` (versi).
  `FILE_MANIFEST.txt` TIDAK berubah.
- Preflight: cek hasil di bawah entri ini sebelum ZIP dikirim (kategori #8
  YAML validity WAJIB tetap hijau setelah restrukturisasi step).
- **CI CONFIRMED HIJAU oleh user** (2026-08-21, run #120, commit e1a5cab):
  `Success`, total durasi **6m 23s** (job `build` 6m 20s), 1 artifact --
  turun dari baseline v8.13.0 (run #118, 7m15s) = **~52 detik lebih
  cepat**. Tidak ada rollback diperlukan.
- **BELUM PERNAH lewat `./gradlew` asli / CI asli** -- sandbox Claude TIDAK
  punya Android SDK/Gradle/jaringan. Kalau ada run berikutnya yang gagal,
  rollback termudah: revert `.github/workflows/build.yml` ke versi 3-step
  (v8.13.0) -- `gradle.properties` v8.13.0/v8.14.0 TIDAK perlu ikut
  di-rollback, sudah terbukti aman terpisah.

## v8.13.0 -- Eksperimen percepatan kompilasi CI (gradle.properties) (2026-08-21)
- **Instruksi langsung user**: "terapkan percepatan kompilasi (experimental)".
- CI (`build.yml`) menjalankan 3 invocation `./gradlew` TERPISAH dalam 1 job
  yang sama (`compileDebugKotlin` -> `testDebugUnitTest`+`testReleaseUnitTest`
  -> `assembleRelease`) -- target utama: hemat waktu KONFIGURASI ulang
  project berkali-kali di job yang sama, + build cache lintas run (`gradle/
  actions/setup-gradle@v3` di CI sudah cache `~/.gradle` antar run workflow
  secara bawaan, jadi flag lokal ini MEMANFAATKAN cache itu, bukan bikin
  cache baru).
- **`gradle.properties` (protected asset, edit PARSIAL) -- 5 baris baru**:
  `org.gradle.parallel=true`, `org.gradle.caching=true` (stabil, low-risk),
  `org.gradle.vfs.watch=true` (stabil), + 2 baris **EKSPERIMENTAL sungguhan**
  `org.gradle.configuration-cache=true` dengan
  `org.gradle.configuration-cache.problems=warn` (BUKAN default `fail`) --
  `warn` dipilih SENGAJA karena `signingConfigs` di `app/build.gradle.kts`
  baca `System.getenv(...)` LANGSUNG di fase konfigurasi (input "untracked"
  bagi configuration cache, akan selalu ke-flag sbg "problem") -- kalau pakai
  default `fail`, build PASTI gagal keras gara2 ini padahal secara fungsi
  tidak masalah. `warn` supaya itu cuma jadi catatan, build tetap lanjut.
- **`org.gradle.jvmargs`**: `-Xmx2048m` -> `-Xmx3072m` (runner ubuntu-latest
  GitHub Actions, margin lebih aman utk Compose+Room+KSP+config-cache
  sekaligus tanpa OOM daemon).
- **SENGAJA TIDAK diaktifkan**: Kotlin K2 compiler
  (`kotlin.experimental.tryK2=true`) -- proyek ini Kotlin 1.9.24 + Compose
  compiler plugin `1.5.14`, dan dukungan K2 utk Compose compiler plugin
  BELUM stabil sebelum Kotlin 2.0 (K2 baru resmi didukung penuh saat Compose
  compiler dibundel ke Kotlin 2.0+) -- risiko build GAGAL TOTAL jauh lebih
  besar drpd potensi speedup, tidak sepadan utk perubahan yang diminta
  "sampai berhasil". Kalau proyek ini upgrade ke Kotlin 2.0+ di masa depan,
  K2 layak dipertimbangkan ulang saat itu.
- **Rollback cepat kalau CI ternyata bermasalah**: cukup hapus 2 baris
  `org.gradle.configuration-cache*` di `gradle.properties` (3 baris lain --
  parallel/caching/vfs.watch -- aman/stabil, tidak perlu ikut dihapus).
- File diubah (2): `gradle.properties` (parsial), `app/build.gradle.kts`
  (versi saja, TIDAK ada perubahan dependency/plugin/logic).
  `FILE_MANIFEST.txt` TIDAK berubah.
- Preflight: cek hasil di bawah entri ini sebelum ZIP dikirim.
- **CI CONFIRMED HIJAU oleh user** (2026-08-21, run #118, commit 084539a):
  `Success`, total durasi **7m 15s** (job `build` 7m 12s), 1 artifact. User
  konfirmasi via screenshot GitHub Actions -- eksperimen berhasil, TIDAK
  ada rollback diperlukan. (Baseline durasi run SEBELUM eksperimen ini
  tidak tercatat di sesi manapun -- tidak ada angka pembanding "sebelum"
  yang valid dikutip; 7m15s dicatat sbg REFERENSI durasi baru ke depan,
  bukan klaim persentase speedup.)
- **BELUM PERNAH lewat `./gradlew` asli / CI asli** -- sandbox Claude TIDAK
  punya Android SDK/Gradle/jaringan (lihat header `scripts/preflight_check.sh`),
  jadi "sampai berhasil" TIDAK BISA diverifikasi tuntas di sini secara
  teknis. **User WAJIB pantau run GitHub Actions berikutnya setelah push**
  -- kalau `assembleRelease` gagal krn config-cache, langsung terapkan
  rollback 2-baris di atas (JANGAN revert seluruh 5 baris, cuma yang
  eksperimental).

## v8.12.0 -- Fitur: UI input PAT GitHub (opsional, hindari rate-limit updater) (2026-08-20)
- **Item pending eksplisit dari user** (dicatat sejak v8.5.0 sbg "titik
  ekstensi siap pakai") -- dieksekusi sekarang atas instruksi langsung.
- `UpdateRepository.checkLatestRelease()`/`downloadApk()` SUDAH menerima
  parameter `githubToken: String? = null` sejak v8.5.0 -- batch ini **0
  baris `UpdateRepository.kt` diubah**, murni menyambungkan penyimpanan +
  UI ke parameter yang sudah ada (bukan fitur baru dari nol).
- **`SettingsRepository.kt`**: key DataStore baru `github_pat_token`
  (`stringPreferencesKey`, nullable) + `githubTokenFlow`/`getGithubToken()`/
  `setGithubToken()`/`clearGithubToken()` -- pola diverifikasi IDENTIK
  `shizukuDestPathKey` (string opsional, `.trim()` sebelum simpan, method
  `clear` terpisah dari `set`).
- **`MainViewModel.kt`**: `val githubToken: StateFlow<String?>` via
  manual `stateIn`-style collect (pola sama persis `shizukuDestPath`,
  BUKAN `.stateIn()` Flow operator resmi -- proyek ini konsisten pakai
  pola manual di seluruh file, ikuti konvensi yang ada). `setGithubToken()`/
  `clearGithubToken()` wrapper `viewModelScope.launch`. `checkForUpdate()`/
  `downloadUpdate()` diteruskan `githubToken.value` sbg argumen ke-2
  (sebelumnya default `null` implisit, sekarang eksplisit dari state).
- **`MainActivity.kt`** (protected asset, edit PARSIAL): 1
  `collectAsStateWithLifecycle` (`githubToken`) + 3 parameter baru
  diteruskan ke `SettingsScreen(...)` (`githubToken`,
  `onGithubTokenChanged`, `onClearGithubToken`) -- TIDAK ada logika
  permission/lifecycle lain disentuh.
- **`SettingsScreen.kt`**: `OutlinedTextField` masked
  (`PasswordVisualTransformation`, toggle show/hide via `trailingIcon`
  ikon `Icons.Filled.VpnKey`, `KeyboardType.Password`) ditaruh DI DALAM
  kartu "Pembaruan Aplikasi" yang sudah ada (bawah deskripsi, ATAS blok
  `when(updateCheckState)`) -- BUKAN kartu terpisah, karena token ini
  murni parameter internal fitur updater, bukan setting mandiri. Tombol
  "Simpan" (`action_save`, REUSE, sama seperti `action_edit`/`action_delete`
  yg sudah dipakai lintas layar sejak v8.3.0) disabled kalau input kosong
  ATAU sama persis dgn token tersimpan (`tokenInput != githubToken.orEmpty()`).
  Tombol "Hapus" (`action_delete`, REUSE) HANYA muncul kalau token SUDAH
  ada tersimpan (`!githubToken.isNullOrBlank()`) -- tidak menawarkan hapus
  utk state yang memang sudah kosong.
- **3 string baru** (`settings_update_token_label/placeholder/hint`) --
  hint eksplisit sebut "Token disimpan di HP kamu saja" (DataStore lokal,
  BUKAN dikirim kemana pun selain header `Authorization` request ke GitHub
  API sendiri saat cek/unduh update -- sesuai `UpdateRepository.kt` yang
  TIDAK disentuh batch ini).
- File diubah (4): `data/SettingsRepository.kt`, `ui/MainViewModel.kt`,
  `MainActivity.kt` (parsial), `ui/screens/SettingsScreen.kt`. `res/values/
  strings.xml` (3 string baru). `app/build.gradle.kts` (versi).
  `FILE_MANIFEST.txt` TIDAK berubah (0 file baru/dihapus).
- Preflight: 13/13 kategori PASS.
- **Belum diverifikasi CI hijau** -- WAJIB dicek run Actions berikutnya
  setelah push. **User WAJIB verifikasi di HP**: (1) isi token PAT GitHub
  valid -> Simpan -> tutup app -> buka lagi -> field masih terisi
  (persistensi DataStore), (2) tombol Hapus muncul setelah ada token
  tersimpan, hilang lagi setelah dihapus, (3) cek update tetap jalan
  normal baik dengan maupun tanpa token diisi (fungsi dasar TIDAK boleh
  regresi utk user yang tidak pernah menyentuh field ini).
- versionCode 104->105, versionName 8.11.0->8.12.0.

## v8.11.0 -- Roadmap Fase 1.3 batch 7/N: ekstraksi string `ActivityLogScreen.kt` (2026-08-20)
- Lanjut item roadmap Fase 1.3: 26 string resource baru (`activitylog_*` +
  1 `action_undo` generik reuse 2x) menggantikan literal Kotlin di
  `ActivityLogScreen.kt` -- MURNI ekstraksi, nilai teks tidak berubah.
- **Layar paling kompleks strukturnya sejauh Fase 1.3** -- pola CAMPURAN,
  BUKAN 1 pola tunggal spt batch sebelumnya: teks di scope composable
  langsung (topBar/actions lambda TopAppBar, `EmptyState`, `items{}`
  LazyColumn, `pendingUndo?.let{}`/`pendingBatchUndo?.let{}` yang eksekusi
  LANGSUNG di body composable -- bukan di dalam lambda parameter lain)
  pakai `stringResource()` biasa. Teks di dalam `onClick`/`onConfirm`/
  `scope.launch{}` (lambda non-composable, BUKAN diberi anotasi
  `@Composable`) pakai `context.getString()` -- `val context =
  LocalContext.current` ditambah 1x di awal fungsi, pola IDENTIK
  `SettingsScreen.kt` v8.6.0 (`readWorkStatus` dkk).
- `action_undo` ("Undo") REUSE di 2 titik BERBEDA: label `TextButton`
  per-baris entri Undo (klik satuan) DAN `confirmLabel` `VaultActionSheet`
  konfirmasi undo tunggal -- keduanya teks identik persis, 1 sumber
  kebenaran, konsisten preseden `action_save`/`action_edit`/`action_delete`
  (batch 1/N Fase 1.3, v8.3.0). `confirmLabel` "Undo Semua" (batch undo)
  TIDAK direuse dari `action_undo` -- teks beda (\"Semua\" bukan cuma
  \"Undo\"), dibuat `activitylog_batch_undo_confirm` terpisah.
- **1 literal sempat terlewat di audit pertama, ketangkap SEBELUM ZIP
  dipaket** (bukan lolos ke user): `SegmentedControl(options = listOf(\"Log\",
  \"Undo Pemindahan\"), ...)` -- 2 label tab, gampang terlewat krn bukan
  `Text()`/`Icon()` langsung, cuma `String` mentah di parameter `List<String>`.
  Ditemukan lewat re-grep manual `grep '\"'` menyeluruh ke seluruh file
  SEBELUM klaim selesai (bukan sesudah) -- ditambah sbg
  `activitylog_tab_log`/`activitylog_tab_undo`. **Pelajaran utk sesi
  berikutnya**: kalau ada komponen yang menerima `List<String>`/`String`
  mentah (bukan wrapper `Text`/`Icon` composable yang jelas terlihat),
  WAJIB di-grep ulang eksplisit -- pola visual "cari `Text(...)`/`title
  =`/`message =`" saja tidak cukup menangkap semua kasus.
- **XML escaping**: kutip literal nama file dinamis `\"%1$s\"` (preseden
  `pandu_section6_body` v8.8.0), `&` di teks sweep-hint -> `&amp;`.
  Divalidasi `xml.dom.minidom.parse` (0 pelanggaran) sebelum preflight.
- Preflight: 13/13 kategori PASS.
- **Belum diverifikasi CI hijau** -- WAJIB dicek run Actions berikutnya
  setelah push.
- **Sisa Fase 1.3**: SkippedFilesScreen, MainActivity (dialog izin/error)
  -- urutan bebas.
- versionCode 103->104, versionName 8.10.0->8.11.0.

## v8.10.0 -- Roadmap Fase 1.3 batch 6/N: ekstraksi string `OnboardingScreen.kt` (2026-08-20)
- Lanjut item roadmap Fase 1.3: 18 string resource baru (`onboarding_*`)
  menggantikan literal Kotlin di `OnboardingScreen.kt` -- MURNI ekstraksi,
  nilai teks tidak berubah.
- **Pola BARU, beda dari 5 batch sebelumnya**: `steps` sebelumnya `private
  val` TOP-LEVEL (bukan di dalam scope composable manapun) -- `stringResource()`
  butuh scope composable, TIDAK BISA dipanggil di inisialisasi `val`
  top-level. Diubah jadi `@Composable private fun onboardingSteps(): List<
  OnboardingStep>`, dipanggil SEKALI di awal body `OnboardingScreen` (`val
  steps = onboardingSteps()`) -- list re-build murah (7 item literal), tidak
  perlu `remember`. Kandidat pola dipakai lagi kalau sisa layar Fase 1.3 py
  `val`/`data class` list top-level berisi teks (beda dari kasus
  `DiagnosticsScreen` yang fungsinya non-composable tapi tetap top-level
  function, bukan `val`).
- **XML escaping**: `<nama rule>` -> `&lt;nama rule&gt;` (preseden
  `pandu_section3_body` v8.8.0), `&` -> `&amp;` (2 titik, step3 & step7),
  kutip literal `"PromptVault"`/`"Riwayat Aktivitas & Undo"`/`"Panduan
  Penggunaan"` -> `\"..\"` (preseden `pandu_section6_body`, BUKAN `&quot;`,
  konsisten gaya escaping yang sudah dipakai project). Divalidasi
  `xml.dom.minidom.parse` (0 pelanggaran) SEBELUM preflight.
- **0 reuse string** -- semua 14 teks title/body Onboarding beda kalimat
  persis dari `pandu_*` (PanduanScreen) walau topiknya tumpang tindih,
  SESUAI keputusan v7.4.0 (Onboarding = wizard ringkas per-langkah,
  Panduan = referensi satu-halaman lengkap, 2 gaya penulisan BEDA SENGAJA,
  bukan boleh disatukan jadi 1 sumber).
- Preflight: 13/13 kategori PASS.
- **Belum diverifikasi CI hijau** -- WAJIB dicek run Actions berikutnya
  setelah push.
- **Sisa Fase 1.3**: ActivityLogScreen, SkippedFilesScreen, MainActivity
  (dialog izin/error) -- urutan bebas.
- versionCode 102->103, versionName 8.9.0->8.10.0.

## v8.9.0 -- Roadmap Fase 1.3 batch 5/N: ekstraksi string `HomeScreen.kt` (2026-08-19)
- Lanjut item roadmap Fase 1.3: 7 string resource baru (`home_*`) +
  **5 REUSE string yang sudah ada** (bukan duplikat baru) menggantikan
  literal Kotlin di `HomeScreen.kt` -- MURNI ekstraksi, nilai teks tidak
  berubah.
- **Reuse, bukan duplikasi**: `"PromptVault"` -> `R.string.app_name`
  (sudah ada sejak awal project), `"Kelola Rule"` -> `R.string.rule_list_title`,
  `"Panduan Penggunaan"` -> `R.string.pandu_title` (v8.8.0),
  `"Pengaturan"` -> `R.string.settings_title`, `"Diagnostik"` ->
  `R.string.diag_title` (v8.7.0) -- kelimanya IDENTIK persis dgn title
  layar tujuan navigasi masing-masing (menu Home = pintu masuk ke layar
  itu), jadi 1 sumber kebenaran lebih benar drpd string terpisah yang bisa
  drift kalau title diubah di satu tempat tapi lupa di tempat lain.
  `"Riwayat Aktivitas & Undo"` TIDAK direuse (beda persis dgn title internal
  `ActivityLogScreen` yang belum diekstrak/diverifikasi -- dibuat sbg
  `home_menu_riwayat` baru, aman drpd asumsi sama).
- **`GroupedList.rows` adalah `List<@Composable () -> Unit>`** (diverifikasi
  ke `GroupedListRow.kt` SEBELUM menulis kode) -- `stringResource()` valid
  dipanggil di dalam tiap lambda karena lambda itu sendiri `@Composable`,
  bukan lambda biasa. Dicek eksplisit supaya tidak salah asumsi sama seperti
  pelajaran Insiden #8 (composable scope semantics).
- Sisa 2 literal yang SENGAJA tidak diubah: `value = "$ruleCount"`
  (interpolasi angka dinamis, bukan literal teks) & `label = "ctaScale"`
  (tag internal `animateFloatAsState`, bukan teks user-facing).
- Preflight: 13/13 kategori PASS.
- **Belum diverifikasi CI hijau** -- WAJIB dicek run Actions berikutnya
  setelah push.
- **Sisa Fase 1.3**: OnboardingScreen, ActivityLogScreen, SkippedFilesScreen,
  MainActivity (dialog izin/error) -- urutan bebas.
- versionCode 101->102, versionName 8.8.0->8.9.0.

## v8.8.0 -- Roadmap Fase 1.3 batch 4/N: ekstraksi string `PanduanScreen.kt` (2026-08-19)
- Lanjut item roadmap Fase 1.3: 18 string resource baru (`pandu_*`)
  menggantikan literal Kotlin di `PanduanScreen.kt` -- MURNI ekstraksi,
  nilai teks tidak berubah.
- **Beda karakter dari batch lain** (sesuai catatan v8.3.0: "9 PARAGRAF
  BESAR, pertimbangkan batch tersendiri") -- semua 100% dalam scope
  `@Composable` (title layar, 1 intro, 7 title+body section, 1
  `WarningBanner`, 1 footer), TIDAK ada fungsi non-composable/callback --
  jadi pola paling sederhana dari seluruh Fase 1.3 sejauh ini, murni
  `stringResource(R.string.pandu_*)` langsung di parameter.
- **XML entity escaping**: body section 3 mengandung literal `<nama rule>`
  -> di-escape `&lt;nama rule&gt;` (bukan sekadar hapus tanda kurung siku,
  makna placeholder dipertahankan persis). 4 titik `&` (section 3/5/6, 2x
  di section 6) -> `&amp;`, preseden sudah ada di `settings_shizuku_section_desc`
  sebelumnya. Divalidasi `xml.dom.minidom.parse` (0 pelanggaran) SEBELUM
  preflight, bukan sesudah.
- Preflight: 13/13 kategori PASS.
- **Belum diverifikasi CI hijau** -- WAJIB dicek run Actions berikutnya
  setelah push.
- **Sisa Fase 1.3**: HomeScreen, OnboardingScreen, ActivityLogScreen,
  SkippedFilesScreen, MainActivity (dialog izin/error) -- urutan bebas.
- versionCode 100->101, versionName 8.7.0->8.8.0.

## v8.7.0 -- Roadmap Fase 1.3 batch 3/N: ekstraksi string `DiagnosticsScreen.kt` (2026-08-19)
- Lanjut item roadmap Fase 1.3 (berjalan sejak v8.3.0): 25 string resource
  baru (`diag_*`) menggantikan literal Kotlin di `DiagnosticsScreen.kt` --
  MURNI ekstraksi, nilai teks tidak berubah.
- **Pola berbeda dari `SettingsScreen.kt` (v8.6.0)**: `readWorkStatus()`
  adalah top-level `private fun` non-composable dipanggil dari dalam
  `LaunchedEffect` -- BUKAN callback lambda spt di v8.6.0, jadi
  `context.getString()` TIDAK dipakai. Sebagai gantinya, 3 string
  (`diag_status_none`/`diag_status_fmt`/`diag_status_error_fmt`) di-resolve
  lewat `stringResource()` di badan `@Composable` (SEBELUM `LaunchedEffect`,
  bukan di dalamnya -- `stringResource()` juga butuh scope composable, tidak
  bisa dipanggil di dalam lambda suspend `LaunchedEffect`), lalu diteruskan
  sbg parameter fungsi ke `readWorkStatus(context, noneText, statusFmt,
  errorFmt)`. Pola ini lebih sederhana drpd `context.getString()` utk kasus
  fungsi top-level yang dipanggil dari composable (bukan dari lambda callback
  UI langsung) -- kandidat pola dipakai lagi kalau sisa layar Fase 1.3 py
  fungsi serupa (fungsi pure/helper non-composable yg butuh string).
- Fmt 3-parameter (`diag_crashlog_item_fmt`, nama file + tanggal + ukuran)
  & fmt 3-parameter lain (`diag_status_fmt`, state + attempt + waktu cek)
  divalidasi manual urutan `%1$s`/`%2$s`/`%3$d` cocok urutan argumen
  `stringResource(id, arg1, arg2, arg3)`.
- Import `com.elprompter.promptvault.R` ditambah (konsisten pola
  `SettingsScreen.kt`) supaya referensi `R.string.diag_*` singkat, bukan
  fully-qualified.
- Preflight: 13/13 kategori PASS. `strings.xml` divalidasi
  `xml.dom.minidom.parse` (0 pelanggaran) sebelum preflight -- 0 karakter
  `--` ditambahkan (pelajaran v8.5.0b/v8.6.0, tidak terulang).
- **Belum diverifikasi CI hijau** -- WAJIB dicek run Actions berikutnya
  setelah push.
- **Sisa Fase 1.3**: PanduanScreen, HomeScreen, OnboardingScreen,
  ActivityLogScreen, SkippedFilesScreen, MainActivity (dialog izin/error) --
  urutan bebas, independen satu sama lain.
- versionCode 99->100, versionName 8.6.0->8.7.0.

## v8.6.0 -- Roadmap Fase 1.3 batch 2/N: ekstraksi string `SettingsScreen.kt` (2026-08-19)
- **Konfirmasi**: v8.5.0c (fix `this@MainActivity`) sudah CI hijau (run #110,
  commit `161ca4d`, dikonfirmasi user via screenshot). Ditutup, tidak perlu
  tindakan lanjutan.
- Lanjut item roadmap Fase 1.3 (sedang berjalan sejak v8.3.0): 70 string
  resource baru (`settings_*`) menggantikan literal Kotlin di
  `SettingsScreen.kt` -- MURNI ekstraksi, nilai teks tidak berubah.
- Detail teknis lengkap ada di `CHANGELOG.md` v8.6.0 (pola
  `context.getString()` vs `stringResource()` ditangkap-lebih-awal utk
  callback non-composable, insiden minor `--` di komentar XML ketangkap
  preflight sebelum commit).
- **Belum diverifikasi CI hijau** -- WAJIB dicek run Actions berikutnya
  setelah push.
- **Sisa Fase 1.3**: DiagnosticsScreen, PanduanScreen, HomeScreen,
  OnboardingScreen, ActivityLogScreen, SkippedFilesScreen, MainActivity
  (dialog izin/error) -- urutan bebas, independen satu sama lain.

## v8.5.0c COMPILE-FIX -- `this@MainActivity` unresolved di `PromptVaultRoot` (2026-08-19)
- **Gejala**: user upload `build-failure-log-v8_5_0__1_.zip`. Manifest fix (v8.5.0b) TERKONFIRMASI berhasil (`processDebugManifest` lolos), tapi `:app:compileDebugKotlin FAILED` -- `Unresolved reference: @MainActivity` di `MainActivity.kt:416`, titik `onInstallUpdate` kartu updater baru.
- **Root cause**: `installApk(this@MainActivity, filePath)` dipanggil di dalam `PromptVaultRoot()` -- `@Composable private fun` TOP-LEVEL (baris 181), BUKAN method di dalam `class MainActivity` (baris 90) -- label `this@MainActivity` tidak eksis di scope itu, compiler benar menolaknya.
- **Fix**: ganti ke `installApk(context, filePath)` -- `installApk()` cuma butuh `Context` biasa (bukan Activity), dan `val context = LocalContext.current` SUDAH ADA di scope `PromptVaultRoot` (baris 189), reuse langsung. 1 baris, nol perubahan logika lain.
- **versionCode/versionName TIDAK naik** (tetap 98/8.5.0) -- compile-fix, bukan fitur baru.
- **Belum diverifikasi CI hijau** -- WAJIB dicek run Actions berikutnya. Kalau masih gagal, kirim log baru.

## v8.5.0b COMPILE-FIX -- "--" di komentar XML AndroidManifest.xml (2026-08-19)
- **Gejala**: user upload `build-failure-log-v8_5_0.zip`. `:app:processDebugMainManifest FAILED` -- `SAXParseException: The string "--" is not permitted within comments`, baris 21 `AndroidManifest.xml` (komentar baru fitur in-app updater v8.5.0, poin INTERNET/REQUEST_INSTALL_PACKAGES).
- **KELAS BUG BERULANG** (4x sekarang: v2.6.0, v2.24.1, v7.0.0/Insiden non-fatal, sekarang v8.5.0) -- pelajaran permanen "jangan pakai `--` di komentar XML manapun" TIDAK otomatis dicek AI saat menulis komentar baru; `preflight_check.sh` kategori #10 (well-formedness XML) MENANGKAP ini dgn benar tapi HANYA setelah CI compile, karena kategori #10 butuh Gradle utk generate merged manifest -- gap-nya ada di titik PENULISAN komentar, bukan di preflight.
- **Fix**: ganti `--` jadi `;` di komentar tersebut (baris 17-21), nol perubahan logika/permission. Divalidasi `xml.dom.minidom.parse` (0 pelanggaran) + `preflight_check.sh` kategori #10 penuh (13/13 kategori PASS).
- **versionCode/versionName TIDAK naik** (tetap 98/8.5.0) -- ini compile-fix, bukan fitur baru, konsisten kebijakan CHANGELOG.
- **Belum diverifikasi CI hijau** -- WAJIB dicek run Actions berikutnya (run #108 sebelumnya FAILED, ini fix untuk run berikutnya).

## STATUS PROJECT: v8.5.0 -- FITUR BARU: In-app Updater ("Release Downloader Spec") -- 2026-08-19
- **Sumber**: gap ditemukan lewat audit rule preferensi user (bukan laporan
  bug/permintaan fitur baru terpisah) -- app SEBELUMNYA sama sekali TIDAK
  punya cara update selain reinstall manual APK dari GitHub Releases.
- **Implementasi** (BARU, package `update/`): `UpdateRepository.kt` --
  `checkLatestRelease()` panggil GitHub API `releases/latest`, parsing versi
  numerik per-segmen (bukan string compare polos, "8.10.0" > "8.9.0" benar);
  `downloadApk()` streaming chunk-by-chunk (Okio `BufferedSource.read(Buffer,
  Long)` -> `BufferedSink.write`) ke `cacheDir/updates/` via file `.part`
  sementara, timeout connect 15s/read 20s, `followRedirects(true)` (asset
  binary GitHub SELALU redirect 302 ke CDN). **TIDAK PERNAH** panggil
  `body.bytes()`/`readBytes()` -- itu akan memuat seluruh APK ke RAM, persis
  yang dilarang spesifikasi proyek.
- **UI**: kartu "Pembaruan Aplikasi" di `SettingsScreen.kt` (state dari
  `MainViewModel.updateCheckState`/`downloadState`, StateFlow biasa, pola
  sama dgn state lain di file itu) -- cek versi, progress bar, tombol Pasang
  yang trigger `installApk()` (BARU, top-level fun di `MainActivity.kt`)
  via `FileProvider` yang SUDAH dideklarasikan di manifest sejak lama tapi
  baru sekarang ada pemakainya.
- **Manifest**: tambah permission `INTERNET` + `REQUEST_INSTALL_PACKAGES`.
- **Dependency baru**: `okhttp:4.12.0`, `okio:3.9.0` (eksplisit, dipakai
  langsung -- bukan cuma transitif diam-diam).
- **Repo target hardcode**: `FDzaki-dev/PromptVault` (sesuai README.md,
  "Repo publik"). Kalau repo pernah dipindah/rename, konstanta `OWNER`/`REPO`
  di `UpdateRepository.kt` WAJIB ikut diupdate manual -- TIDAK ada mekanisme
  auto-detect.
- **Token GitHub PAT**: didukung opsional lewat parameter `githubToken` di
  kedua fungsi repository, TAPI **belum ada UI input token di Pengaturan**
  (belum dibutuhkan -- repo publik, rate-limit 60/jam cukup utk cek manual
  sesekali). Kalau nanti user sering dapat error rate-limit, ini titik
  ekstensi yang sudah siap, bukan re-desain dari nol.
- **Housekeeping terpisah, batch sama**: `FILE_MANIFEST.txt` diregenerasi
  penuh dari isi disk (`find` aktual, bukan ditulis manual) -- memperbaiki
  desync lama (`data/BackupManager.kt` sempat tidak tercatat, ditemukan
  lewat audit yang sama). Total file naik dari 93 (dgn 1 desync) jadi 95
  (95 tercatat SEMUA cocok isi disk, termasuk `FILE_MANIFEST.txt` itu
  sendiri) -- 2 file baru (`update/UpdateModels.kt`, `update/
  UpdateRepository.kt`), fix 1 file lama yang sebelumnya tidak tercatat.
- **Versi**: 8.4.0 (code 97) -> 8.5.0 (code 98).

## FIX CI: GitHub Releases page nempel di versi lama walau build sukses -- 2026-08-18
- **Gejala user**: Actions run hijau semua (Artifact selalu ada), tapi tab
  Releases repo nunjuk versi LAMA sebagai "Latest".
- **Sebab**: `softprops/action-gh-release` UPDATE release yang tag-nya sudah
  ada (bukan create baru) kalau versionName sempat tidak naik antar push;
  GitHub urut "Latest" dari `created_at`, jadi update ke release lama tidak
  otomatis pindahin flag walau APK di dalamnya sudah yang terbaru.
- **Fix** (`.github/workflows/build.yml`): tambah `make_latest: true` eksplisit
  di step Publish, PLUS step baru sesudahnya `gh release edit v<versi>
  --latest` yang paksa ulang flag tiap run sukses, terlepas dari riwayat
  created_at. Tidak ada perubahan versionName/versionCode (murni CI, no app
  code change).

## STATUS PROJECT: v8.4.0 -- FITUR BARU: "Selamatkan Uninstall" (deteksi & restore config lama dari folder SAF) -- 2026-08-18
- **Permintaan eksplisit user**: kalau app tidak sengaja ke-uninstall lalu
  diinstal ulang, dan user memilih folder tujuan kustom SAF yang SAMA
  (masih berisi banyak file lama dari instalasi sebelumnya) -- app HARUS
  (1) mendeteksi root folder yang sudah pernah dibuat sebelumnya & PAKAI
  ITU SAJA (bukan bikin folder duplikat baru), dan (2) mengembalikan semua
  konfigurasi yang ikut terhapus saat uninstall (rule, log, riwayat
  pemindahan, dsb -- Android uninstall menghapus TOTAL DataStore & Room,
  itu perilaku OS, tidak bisa dihindari dari sisi app).
- **Bagian (1), anti-duplikat folder root, SUDAH SELESAI sejak v7.5.0/v8.x**
  lewat `FileSorter.resolveCanonicalRootDirSaf()` (self-healing regex+cache,
  lihat Keputusan Arsitektur & riwayat panjang v7.1.5-v7.5.0 di bawah) --
  TIDAK diulang/ditulis ulang di sini (pelajaran permanen Insiden #7: jangan
  bikin implementasi kedua independen). Batch ini MURNI menambah lapisan
  BARU di atasnya: cermin/manifest config yang SEBELUMNYA tidak ada sama
  sekali (root folder anti-duplikat != config yang ikut terselamatkan --
  2 masalah beda yang user gabung dalam 1 permintaan, keduanya valid & saling
  melengkapi, bukan tumpang tindih).
- **Desain lapisan baru**: file JSON tersembunyi
  `.promptvault_config_backup.json` (BARU `util/VaultConfigBackup.kt`, murni
  I/O serialisasi + 2 fungsi pure logic `isPayloadWorthOffering`/`countRules`,
  di-unit-test) ditulis di root vault "PromptVault" SAF yang sama -- berisi
  rule (string JSON, REUSE `RuleRepository.exportAsJson()`/`importFromJson()`
  yang SUDAH ADA, BUKAN skema serialisasi baru), setting relevan (interval/
  conflict strategy/scan concurrency), snapshot log aktivitas & riwayat
  pemindahan (masing-masing dibatasi 200 entri terbaru, biar file tidak
  membengkak tanpa batas).
- **Tulis, opportunistic & best-effort, 2 titik**: `FileSorter.
  syncConfigBackupToSaf()` dipanggil dari (a) `MainViewModel` reaktif tiap
  rule berubah (`rules.drop(1).collect{}` -- `drop(1)` melewati emisi awal
  StateFlow yang BUKAN perubahan nyata dari user, lihat komentar di kode),
  kalau folder SAF sedang aktif; (b) `scanAndSortToDestination()` sendiri,
  setelah tiap scan sukses ke tujuan SAF. **Root vault TIDAK PERNAH dibuat
  lebih awal cuma gara-gara sinkronisasi backup** -- dipakai varian
  peek-only baru (`peekCanonicalRootDirSaf`, TIDAK PERNAH `createDirectory()`
  sendiri); root "asli" tetap HANYA dibuat lewat jalur normal yang sudah ada
  (`resolveCanonicalRootDirSaf`, dipanggil scan). Kegagalan tulis SELALU
  ditelan diam-diam (try-catch) -- BUKAN gerbang yang boleh menggagalkan
  scan/simpan rule utama, murni best-effort.
- **Baca & tawarkan, SEKALI, dipicu picker**: `MainViewModel.setSafTreeUri()`
  memanggil `detectVaultRestoreOffer()` PERSIS SEKALI segera setelah URI
  baru tersimpan (BUKAN reaktif berulang tiap kali Pengaturan dibuka).
  `FileSorter.peekVaultBackup()` menemukan backup non-kosong -> dialog
  `VaultActionSheet` (REUSE komponen konfirmasi standar app yang sudah ada,
  BUKAN `AlertDialog` baru) muncul di `SettingsScreen`, menampilkan ringkasan
  (jumlah rule/log/riwayat + tanggal backup) dengan 2 pilihan eksplisit:
  "Pulihkan Konfigurasi Lama" / "Mulai Kosong Saja".
- **Restore**: `FileSorter.applyVaultRestore()` -- rule lewat
  `RuleRepository.importFromJson()` yang SUDAH ADA (merge by-id, di
  instalasi baru/0 rule lokal hasilnya otomatis = full restore, BUKAN
  implementasi restore kedua yang independen); log & riwayat lewat
  `restoreEntries()` BARU di `ActivityLogRepository`/`MoveHistoryRepository`
  (insert via `OnConflictStrategy.REPLACE` yang SUDAH ADA di DAO -- dedupe
  otomatis by-id, aman dipanggil 2x kalau user tidak sengaja konfirmasi
  ulang). Riwayat pemindahan (`MoveHistoryEntry`, termasuk yang belum
  di-undo) SENGAJA tetap direstore walau `destUri`/`originalParentUri` SAF
  berpotensi stale pasca-reinstall -- `FileSorter.undo()` SUDAH punya lapis
  try-catch/verifikasi matang dari riwayat pengerasan berulang (P0-3
  v7.1.4, OVERWRITE-delete v7.1.9/v2.19.1, dst), jadi kegagalan undo pada
  entri lama tetap gagal DENGAN AMAN (hasil eksplisit ke UI, bukan crash) --
  bukan risiko baru yang butuh penanganan khusus.
- **Scope SENGAJA terbatas ke mode SAF saja** (bukan Shizuku) -- Shizuku
  pakai path filesystem yang user ketik manual (bukan picker folder), jadi
  tidak ada titik alami "pilih folder" untuk memicu deteksi restore ini.
- File diubah (6) + 2 baru: `util/FileSorter.kt` (6 fungsi baru + 1 hook di
  `scanAndSortToDestination`), `data/ActivityLogRepository.kt`/
  `data/MoveHistoryRepository.kt` (`restoreEntries` + mapper),
  `ui/MainViewModel.kt` (StateFlow tawaran + detect/confirm/dismiss + hook
  reaktif di `rules`), `ui/screens/SettingsScreen.kt` (3 param baru +
  dialog `VaultActionSheet`), `MainActivity.kt` (protected asset, edit
  parsial: 1 `collectAsStateWithLifecycle` + 3 param diteruskan ke
  `SettingsScreen`), `app/build.gradle.kts` (versi). BARU:
  `util/VaultConfigBackup.kt`, `app/src/test/.../util/VaultConfigBackupTest.kt`.
  `scripts/preflight_check.sh` 13/13 lolos bersih.
- **Belum lewat `./gradlew`/device asli** (konsisten seluruh riwayat
  project) -- 2 risiko BARU spesifik batch ini yang BELUM ada preseden
  internal utk dibandingkan: (1) `DocumentFile.createFile()`+
  `contentResolver.openOutputStream()` menulis file JSON MILIK APP SENDIRI
  ke SAF -- SAF sebelumnya HANYA dipakai memindahkan file milik USER
  (moveFileToSafDestination/copyDocumentBytes), belum pernah dipakai app
  ini untuk menulis manifest/state-nya sendiri; (2) `StateFlow.drop(1).
  collect{}` reaktif di `MainViewModel` (pola custom, BUKAN `Flow.combine`+
  `debounce` yang sengaja dihindari krn butuh anotasi eksperimental tanpa
  preseden di project ini) adalah pola trigger baru yang belum pernah
  dipakai utk efek samping semacam ini.
- **User WAJIB verifikasi di HP asli**: (1) build CI hijau, (2) pilih
  folder SAF BARU (kosong) -> isi 1+ rule -> jalankan scan -> cek file
  `.promptvault_config_backup.json` muncul di root "PromptVault" (lewat
  file manager, "tampilkan file tersembunyi"), (3) uninstall app (atau
  clear app data dari Pengaturan Android, lebih cepat drpd uninstall
  sungguhan) -> install ulang -> pilih folder SAF yang SAMA -> dialog
  "Konfigurasi Lama Ditemukan" HARUS muncul dengan angka rule/log/riwayat
  yang benar -> konfirmasi "Pulihkan" -> rule & log lama HARUS muncul lagi
  di Kelola Rule/Riwayat Aktivitas, (4) ulangi tapi pilih "Mulai Kosong
  Saja" -> pastikan TIDAK ada rule/log yang berubah & root folder tetap
  dipakai apa adanya (BUKAN folder baru "PromptVault (1)" -- ini
  memverifikasi klaim utama user "root folder yang sama... dipakai saja").
- Confidence Rating: **80%** (arsitektur REUSE jalur SAF yang sudah matang
  + DAO conflict-replace yang sudah ada, jadi bukan risiko dari nol -- tapi
  2 permukaan I/O di poin risiko di atas baru pertama kali dipakai project
  ini, jadi turun dari 90%+ standar batch reuse-berat sampai lolos
  verifikasi CI/device pertama).
- versionCode 96->97, versionName 8.3.0->8.4.0.

## STATUS PROJECT SEBELUMNYA: v8.3.0 -- FIX CI: Stale Run Guard di build.yml (anti-desync "Latest" release) -- 2026-08-18
- **Bug nyata**: `gh run 3209` (build commit lama v8.2.0) sempat re-run
  SETELAH v8.3.0 & v8.4.0 sudah publish -- `softprops/action-gh-release`
  nge-tandain v8.2.0 sebagai "Latest" (default `make_latest`), sidebar repo
  jadi nunjuk APK usang walau job hijau/sukses & fitur baru sudah compile.
- **Fix**: step baru "Stale run guard (anti-desync)" ditambah persis setelah
  Checkout -- `git ls-remote origin refs/heads/main` dibanding `$GITHUB_SHA`.
  Kalau beda (stale re-run), `exit 1` sebelum compile & sebelum publish.
- **Immediate action user**: `gh release edit v8.3.0 --latest` (release
  v8.3.0 sudah ada, cuma flag "Latest"-nya yang ketiban v8.2.0).
- Tidak ada perubahan kode aplikasi/UI -- versionCode/versionName TETAP
  96/8.3.0 (murni infra CI). File tersentuh: `.github/workflows/build.yml`
  (protected asset, edit parsial: 1 step baru ditambah).
- Release ganjil `v8.4.0` (published ~6 jam sebelum sesi ini, gak match
  history commit lokal) BELUM diinvestigasi -- kemungkinan sisa eksperimen
  lain. Jangan dipakai sampai dicek asalnya di batch berikutnya.

## STATUS PROJECT SEBELUMNYA: v8.3.0 -- Roadmap Fase 1.3 (batch 1/N): ekstraksi string cluster "Kelola Rule" -- 2026-08-18
- Item ketiga `ROADMAP.md`, batch PERTAMA dari beberapa (roadmap sendiri
  bilang "bertahap per layar" -- ini bukan item yang selesai 1 batch seperti
  1.1/1.2). Detail lengkap di `CHANGELOG.md` v8.3.0.
- **Cakupan batch ini**: `AddEditRuleScreen.kt`, `RuleListScreen.kt`,
  `RuleCard.kt` (cluster "Kelola Rule") + 35 string resource baru di
  `strings.xml`.
- **Sisa utk batch lanjutan** (independen, urutan bebas): `SettingsScreen.kt`
  (~22 literal, TERBESAR), `DiagnosticsScreen.kt` (~15), `PanduanScreen.kt`
  (~9 PARAGRAF BESAR, bukan literal pendek -- beda karakter dari yang lain,
  pertimbangkan batch tersendiri), `HomeScreen.kt`, `OnboardingScreen.kt`
  (termasuk data class `steps` -- cek strukturnya dulu sebelum ekstrak),
  `ActivityLogScreen.kt`, `SkippedFilesScreen.kt`, `MainActivity.kt`
  (dialog izin & error).
- **Catatan teknis WAJIB dibaca sebelum lanjut batch berikutnya**: aturan
  `stringResource()` vs `Context.getString()` (composable vs lambda
  belakangan) ada di `CHANGELOG.md` v8.3.0 -- baca itu dulu, jangan
  re-investigasi dari nol tiap batch. Juga: XML comment TIDAK BOLEH
  mengandung `--` literal (dobel-hyphen) di mana pun, termasuk lintas baris
  -- validasi selalu pakai `python3 -c "import xml.dom.minidom as m;
  m.parse('app/src/main/res/values/strings.xml')"` sebelum preflight check.
- Roadmap item 1.3 TETAP di `ROADMAP.md` (belum dicoret, masih ada batch
  lanjutan) -- ditandai "batch 1/N selesai" saja.

## STATUS PROJECT SEBELUMNYA: v8.2.0 -- Roadmap Fase 1.2: audit aksesibilitas TalkBack menyeluruh -- 2026-08-18
- Item kedua `ROADMAP.md` dikerjakan & **selesai dalam 1 batch** (bukan 4
  batch per-layar seperti estimasi awal roadmap) -- audit menyeluruh
  langsung di sesi ini menemukan app SUDAH compliant di hampir semua titik
  (label ikon, target sentuh), gap nyata cuma 1 komponen. Detail lengkap di
  `CHANGELOG.md` v8.2.0.
- **Gap yang ditemukan & diperbaiki**: `SegmentedControl.kt` (tab Log/Undo
  Pemindahan di `ActivityLogScreen`) -- (1) tidak ada semantics
  `selected`/`Role.Tab`, (2) target sentuh ~38dp (di bawah standar 48dp).
  Kedua fix murni aditif (`Modifier.semantics`/`.selectableGroup()`/naikkan
  padding), TIDAK menyentuh `onClick`/`interactionSource` yang sudah teruji.
- **Catatan utk sesi depan**: `ROADMAP.md` item 1.2 tadinya diestimasi "4
  batch per grup layar" -- itu ESTIMASI AWAL yang ternyata terlalu
  pesimis, bukan proses yang harus diikuti persis. Kalau nemu roadmap item
  lain yang estimasinya juga overshoot, audit dulu SEBELUM asumsi perlu
  banyak batch -- kadang app memang sudah lebih rapi dari perkiraan.
- Roadmap item 1.2 dicoret dari `ROADMAP.md`, lanjut ke 1.3 (audit string
  hardcode vs `strings.xml`) di sesi berikutnya sesuai urutan fase.

## STATUS PROJECT SEBELUMNYA: v8.1.0 -- Roadmap Fase 1.1: ekstraksi + unit test logika pure FileSorter -- 2026-08-18
- Item pertama `ROADMAP.md` dikerjakan (low-risk/high-value pertama).
  Detail lengkap 4 fungsi yang diekstrak & alasan bug-for-bug parity ada di
  `CHANGELOG.md` v8.1.0.
- **Batch kecil, risiko rendah**: 1 file produksi diubah (`FileSorter.kt`,
  murni pindah lokasi fungsi + 1 ekstraksi loop rename), 1 file test baru
  (`FileSorterPureLogicTest.kt`). TIDAK ada perubahan perilaku produksi
  yang disengaja -- semua diverifikasi lewat grep referensi + preflight
  check sebelum ditutup.
- **Catatan utk sesi depan**: `nextAvailableFileName` masih punya kuirk
  lama (extensionless file -> trailing dot `"nama_1."`) yang SENGAJA belum
  diperbaiki (di luar scope batch ini, murni ekstraksi). Kalau user
  laporkan bug soal ini, root cause & lokasi fix-nya sudah jelas (fungsi
  ini, top-level di `FileSorter.kt`) -- bukan investigasi baru.
- Roadmap item 1.1 dicoret dari `ROADMAP.md`, lanjut ke 1.2 (audit
  aksesibilitas TalkBack) di sesi berikutnya sesuai urutan fase.

## STATUS PROJECT SEBELUMNYA: v8.0.0 -- ROMBAK TOTAL TEMA: Material 3 murni, calm bukan warm, Premium Tactile -- 2026-08-18
- **Permintaan eksplisit user (sesi ini)**: "Rombak total theme aplikasi
  jadi default Material 3 murni, pendekatan Premium Tactile experience,
  base warna calm bukan warm, tetap sesuai standar WCAG." Detail lengkap
  perubahan file & angka kontras WCAG ada di CHANGELOG.md v8.0.0 -- ringkasan
  keputusan arsitektur di bawah.
- **Atomic change**: 19 file tersentuh (batch limit 10 dilampaui dgn
  justifikasi eksplisit -- color tokens, primitif tactile, dan penghapusan
  toggle preset saling terikat erat, kompilasi gagal kalau diterapkan
  parsial).
- **Keputusan #1 -- Glassmorphism dihapus total**: `GlassPanel.kt` (border
  kaca+gradient highlight+shadow kustom) DIHAPUS, diganti `TactileSurface.kt`
  (Surface M3 baku: `tonalElevation`+`shadowElevation`, TANPA border/bevel
  dekoratif -- itu bukan bahasa visual M3). Ini pengganti langsung, bukan
  refactor nama saja -- visualnya berubah (tidak ada lagi hairline glass
  border di semua kartu/kontrol).
- **Keputusan #2 -- toggle preset ganda `useAltTheme` DIHAPUS TOTAL**
  (bukan direvisi/direkolor): "default Material 3 murni" berarti SATU
  ColorScheme baku, bukan 2 preset kustom (Deep Navy+Brass / Charcoal+Copper,
  v7.1.0) untuk dipilih user. Infra ikut dihapus: `SettingsRepository`
  (key/flow/getter/setter), `MainViewModel` (StateFlow/setter), seksi "Tema"
  di `SettingsScreen`, `SideEffect` reaktif status/nav bar di `MainActivity`.
  **Kalau user MINTA toggle tema balik di sesi depan**: ini PENGHAPUSAN FITUR
  SENGAJA sesi ini, bukan bug -- tanya dulu preset seperti apa yang diinginkan,
  jangan asal restore v7.1.0 (paletnya sudah tidak sesuai syarat "calm").
- **Keputusan #3 -- dark-only TIDAK diubah**: keputusan v3.0.0 dipertahankan
  murni krn user minta rombak WARNA/TEMA, bukan minta Light mode baru. Kalau
  App Widget/dsb butuh Light mode kelak, itu scope terpisah, bukan bagian
  dari "Material 3 murni" yang diminta sesi ini.
- **Keputusan #4 -- warna semantik (tertiary=warning amber, error=merah)
  SENGAJA TIDAK ikut hue calm murni**: konvensi universal, porsi kecil/aksen
  saja, bukan "base warna dominan" yang jadi syarat user. Base/dominan
  (background, 5-tingkat surfaceContainer, primary CTA) 100% cool/calm (seed
  biru H222).
- **WCAG**: semua pasangan teks/ikon dihitung manual (relative luminance
  formula W3C, script python di sesi ini, sama metode dgn audit 2026-08-16
  sebelumnya) -- lulus AA (>=4.5:1 teks, >=3:1 batas grafis 1.4.11) di
  worst-case (surface paling terang). Container fill (~1.8-2:1 vs root
  background) TIDAK melanggar 1.4.11 -- selalu dipakai sbg bentuk jelas
  (kotak ikon bulat) di dalam TactileSurface yang sudah py shadow sendiri,
  bukan blok warna mengambang tanpa bentuk (precedent sama dgn audit
  GlassHighlight sebelumnya).
- Preflight check `scripts/preflight_check.sh`: 12/13 kategori PASS otomatis,
  kategori #7 (review manual fungsi lokal) diperiksa manual, tidak ada
  temuan baru dari batch ini.
- versionCode 92→93, versionName 7.5.2→8.0.0.

## STATUS PROJECT SEBELUMNYA: v7.5.2 -- FIX crash pertama produksi (UnsupportedOperationException, SingleDocumentFile.listFiles) -- 2026-08-17
- Crash pertama sepanjang project, user upload log asli
  (`crash_20260817_174626_f7fac68a.txt`, Infinix X6855/Android 16). Trigger:
  edit rule sortir -> tekan Scan di beranda.
- **Root cause**: `findOrCreateChildDirSaf()` & `resolveCanonicalRootDirSaf()`
  (keduanya di `FileSorter.kt`) rekonstruksi folder ter-cache pakai
  `DocumentFile.fromSingleUri()` -> selalu `SingleDocumentFile`, yang
  `listFiles()`-nya UNCONDITIONALLY throw `UnsupportedOperationException`
  (hardcoded androidx, apapun URI-nya). Objek itu lalu dipakai lagi sbg
  `parent` di panggilan `findOrCreateChildDirSaf` berikutnya ->
  `parent.findFile(name)` -> internal `listFiles()` -> crash. Match PERSIS
  dgn stack trace user (line 846).
- **Fix**: ganti `fromSingleUri()` -> `DocumentFile.fromTreeUri()` di 2 titik
  cache reconstruction. URI cache selalu child dari tree yang sama (ada
  segmen `/tree/`), jadi `fromTreeUri()` bangun ulang `TreeDocumentFile`
  yang benar & tetap listable.
- **Bukan disebabkan overlap "Documents" v7.5.1** -- 2 subsistem storage beda
  (MediaStore CrashLogger vs SAF FileSorter), tidak saling panggil
  `listFiles()`. Overlap itu tetap valid & tidak diubah (info non-blocking,
  sudah cukup). Root cause murni salah pilih API `DocumentFile`, independen
  dari folder mana yang dipilih user -- **berarti bug laten ini sebenarnya
  ada di SEMUA versi sejak cache-by-Uri diperkenalkan (v7.1.5), bukan
  regresi baru v7.5.0/v7.5.1** -- baru kena sekarang krn kombinasi
  cache "dingin" (app dibuka lagi) + tujuan kustom aktif.
- File diubah (2): `util/FileSorter.kt`, `app/build.gradle.kts` (versi).
- **Belum diverifikasi user** (fix baru dikirim sesi ini) -- kalau sesi
  berikutnya user lapor Scan masih crash dgn stack trace SAMA
  (`SingleDocumentFile.listFiles`), cek dulu apakah v7.5.2 ini benar
  ter-install sebelum cari root cause baru. Ada 3 titik `fromSingleUri()`
  lain di `FileSorter.kt` (jalur `undo()`, line ~1211/1217/1282) yang
  SENGAJA TIDAK disentuh -- itu untuk resolusi file/leaf tunggal (exists/
  delete/getParentFile), bukan folder yang di-listFiles()/findFile(), jadi
  bukan bug yang sama.
- Confidence Rating: fix **95%** (match persis stack trace, minimal &
  bertarget) -- belum lewat `./gradlew`/device asli (keterbatasan permanen).
- **Verifikasi ulang (user tanya eksplisit): apakah fix ini bisa mengembalikan
  insiden duplikat folder "(N)" lama (v7.1.x-v7.5.0)?** TIDAK. Fix HANYA ganti
  cara baca-balik `DocumentFile` dari URI cache -- TIDAK menyentuh
  `createDirectory()` maupun logika duplikat sama sekali. 2 lapis proteksi
  duplikat lama tetap utuh & tidak disentuh: (1) resolusi folder rule tetap
  SERIAL sebelum `async{}` fan-out di `resolveSafRuleDestinations` (proteksi
  UTAMA thd race antar-coroutine, bukan cache), (2) self-healing
  `resolveCanonicalRootDirSaf` (konvergen kalau provider SAF sendiri sampai
  bikin >1 folder cocok pola). Faktanya: SEBELUM fix, cache root yang
  berhasil dibaca (`fromSingleUri`) itu sendiri TIDAK memicu duplikat --
  crash baru terjadi 1 langkah setelahnya, saat objek itu dipakai sbg
  `parent` utk `findFile()` folder rule (baris 846) -- artinya crash ini
  SELALU terjadi SEBELUM kode sempat sampai ke `createDirectory()`. Tidak
  pernah ada jalur di mana bug lama ini bisa menghasilkan folder ganda; dia
  cuma bikin app mati lebih dulu. SESUDAH fix, `findFile()` justru jadi
  BERHASIL (bukan crash) -- kalau folder rule sudah ada, langsung KETEMU &
  dipakai ulang (bukan `createDirectory()` baru), jadi fix ini MEMPERKUAT
  anti-duplikat, bukan melemahkannya.

## STATUS PROJECT SEBELUMNYA: v7.5.1 -- Info non-blocking: folder tujuan kustom "Documents" langsung overlap dgn folder crash log -- 2026-08-17
- User konfirmasi eksplisit: folder tujuan kustom aktifnya persis
  "Documents" (bukan subfolder) -- match dgn hipotesis dia sendiri bahwa
  ini overlap dgn `CrashLogger.kt` (`Documents/PromptVault/logs/` via
  MediaStore, subsistem beda dari SAF).
- `SettingsScreen.kt`: `isSafRootDocumentsFolder()` baru, tampilkan 1 baris
  info (bukan warning merah) di kartu Folder Tujuan Kustom kalau kondisi
  ini terdeteksi. TIDAK mengubah `CrashLogger.kt` (spek baku lintas
  project, di luar scope) atau `FileSorter.kt` (mekanisme self-heal
  `resolveCanonicalRootDirSaf` v7.5.0 SUDAH cukup, apapun sumber stale-nya).
- `preflight_check.sh` 13/13 lolos. Confidence: integritas paket 100%,
  perubahan UI info-only murni (tidak sentuh logika scan/data).
- versionCode 90->91, versionName 7.5.0->7.5.1.

## STATUS PROJECT SEBELUMNYA: v7.5.0 -- Auto-buat folder root "PromptVault" di tujuan kustom SAF DIKEMBALIKAN (permintaan langsung user) + lapis anti-duplikat baru -- 2026-08-17
- Trigger: user tanya apakah duplikasi folder root (riwayat v2.19.2 s/d
  v7.1.6 di bawah) berkorelasi dgn fitur "Folder Tujuan Kustom" di
  screenshot Pengaturan -- dikonfirmasi YA (sudah terdokumentasi lengkap,
  bukan penemuan baru), lalu user minta root cause-nya dibalik lagi:
  kembalikan auto-buat root, TAPI jangan sampai duplikat "(N)" terulang.
- **Bukan revert v7.2.0 begitu saja** -- 2 mitigasi lama (v7.1.5 cache-Uri,
  v7.1.6 retry+instrumentasi) TERBUKTI belum cukup dulu utk root (walau
  terbukti cukup utk subfolder rule, DIPERTAHANKAN). Ditambah lapis yang
  BELUM pernah dicoba: `resolveCanonicalRootDirSaf()` di `FileSorter.kt` --
  kalau listing SAF menemukan >1 folder cocok pola
  `PromptVault`/`PromptVault (N)`, app TIDAK pilih random: prioritas nama
  persis tanpa akhiran, fallback paling lama (`lastModified()`), log
  WARNING ke Activity Log, folder lain TIDAK disentuh/dihapus (bukan aksi
  destruktif otomatis). Sejak titik itu SEMUA scan konvergen ke 1 folder
  yang sama (di-cache) -- self-healing, bukan cuma prevention seperti
  percobaan-percobaan sebelumnya. `findOrCreateChildDirSaf` retry juga
  diperkuat (200ms -> 200ms+500ms bertahap).
- 3 file dokumentasi UI (`SettingsScreen.kt`, `PanduanScreen.kt`,
  `OnboardingScreen.kt`) yang sebelumnya (v7.3.0) eksplisit bilang "root
  TIDAK PERNAH auto-dibuat" untuk SAF, disesuaikan -- klaim itu sekarang
  HANYA berlaku utk mode Shizuku (TIDAK disentuh/tidak diminta user kali
  ini, tetap manual root sesuai desain aslinya).
- File diubah (4): `util/FileSorter.kt`, `ui/screens/SettingsScreen.kt`,
  `ui/screens/PanduanScreen.kt`, `ui/screens/OnboardingScreen.kt`,
  `app/build.gradle.kts` (versi). `preflight_check.sh` 13/13 lolos.
- **Batas jujur**: mekanisme deteksi+konvergensi BARU, belum diuji skenario
  provider SAF OEM nyata yang benar-benar bikin duplikat lagi. Desainnya
  defensif by design (tidak mungkin memperburuk drpd insiden lama -- kalau
  gagal pun, app konsisten ke 1 folder + warning, bukan menyebar file diam-
  diam). BELUM lewat `./gradlew`/device asli (keterbatasan permanen
  lingkungan kerja Claude, dicatat konsisten tiap sesi sejak awal project).
  **User WAJIB verifikasi**: (1) build CI hijau, (2) folder tujuan kustom
  BARU (kosong) -> scan -> subfolder "PromptVault" muncul otomatis, (3)
  scan berkali-kali (manual + auto-sort) ke folder SAMA -> pastikan HANYA
  1 folder "PromptVault" yang terisi terus, TIDAK ada "(1)"/"(2)" baru
  muncul -- ini test definitif yang gagal dibuktikan 2 kali di v7.1.x dulu.
- Confidence Rating: integritas paket/ZIP **100%** (preflight 13/13, semua
  protected assets utuh) -- perilaku fungsional anti-duplikat di device OEM
  nyata **~75%** (di luar kendali sandbox, lihat CHANGELOG.md v7.5.0).
- versionCode 89->90, versionName 7.4.0->7.5.0.

## STATUS PROJECT SEBELUMNYA: v7.4.0 -- Panduan User Baru: onboarding dirombak total + layar Panduan persisten baru -- 2026-08-17
- User menyoroti gap: setelah perombakan besar v7.3.0 (Shizuku, sweep-
  select-undo, warning banner), user BARU nyaris tidak punya cara pelajari
  mekanisme app -- "tangani hingga tuntas". Root cause: `OnboardingScreen`
  HANYA tampil SEKALI SEUMUR HIDUP (`onboardingDone` DataStore key) DAN
  isinya basi (4 langkah generik, tidak sebut SAF/Shizuku/konflik/undo sama
  sekali) -- sekali ditekan "Mulai" atau dilupakan, TIDAK ADA jalan balik
  di dalam app.
- **Onboarding REWRITE**: `OnboardingScreen.kt` 4 -> 7 langkah, urut sesuai
  alur pemakaian nyata (selamat datang -> rule -> izin -> ke mana file
  disortir + warning root-folder -> strategi konflik -> auto-sort ->
  undo + pointer ke Panduan). Wizard step-by-step + Crossfade lama TETAP,
  cuma konten diperluas.
- **BARU `PanduanScreen.kt`**: versi REFERENSI satu-halaman (bukan wizard)
  dari materi yang sama + 2 poin troubleshooting cepat, bisa dibuka
  BERKALI-KALI kapan saja TANPA reset status onboarding -- ini penutup
  gap utamanya. `WarningBanner` root-folder di-REUSE persis sama dengan
  SettingsScreen (satu sumber kebenaran).
- **2 entry point baru** (bukan cuma 1, biar tidak terkubur): grouped menu
  Home (`HomeScreen.kt`, antara "Kelola Rule" & "Riwayat Aktivitas", tint
  `colors.tertiary` di-REUSE -- BUKAN aksen ke-5, sistem warna app tetap
  dibatasi 4 aksen) DAN tombol paling atas di `SettingsScreen.kt` (layar
  yang paling sering dibuka user saat setup awal).
- `Navigation.kt`: route baru `Routes.PANDUAN`. `MainActivity.kt` (protected
  asset, edit parsial): import + `composable(Routes.PANDUAN)` + parameter
  `onOpenPanduan` diteruskan ke `HomeScreen(...)`/`SettingsScreen(...)` yang
  SUDAH ADA -- tidak menyentuh logika permission/lifecycle apapun di file
  itu.
- File diubah (6) + 1 baru -- lihat CHANGELOG.md v7.4.0 utk daftar lengkap
  per-file. `preflight_check.sh` 13/13 lolos.
- **SENGAJA tidak disentuh**: item P0/P1/P2 dari
  `PromptVault_real_functional_polish_gap_audit.md` (2026-08-16) -- itu bug
  fungsional FileSorter/undo/worker, di luar scope batch ini (murni gap
  informasi ke user). Jangan campur ke sesi berikutnya tanpa diskusi
  eksplisit ke user dulu, biar tiap batch tetap Atomic Change yang jelas.
- **Batas jujur**: batch ini UI-only + wiring navigasi murni, TIDAK
  menyentuh FileSorter/DataStore/worker/permission sama sekali -- risiko
  regresi fungsional nyaris nol dibanding batch v7.3.0. Tapi tetap BELUM
  lewat `./gradlew`/device asli (keterbatasan lingkungan kerja Claude yang
  konsisten dicatat tiap sesi). **User WAJIB verifikasi**: (1) build CI
  hijau, (2) 7 langkah onboarding tampil benar saat install bersih/data
  app dihapus, (3) "Panduan Penggunaan" di Home & Pengaturan sama-sama
  membuka layar yang sama dan bisa dibuka berkali-kali, (4) navigasi
  back dari Panduan kembali ke layar asal (Home atau Pengaturan) dengan
  benar.
- Confidence Rating: **90%**.
- versionCode 88->89, versionName 7.3.0->7.4.0.

## STATUS PROJECT SEBELUMNYA: v7.3.0 -- 3 permintaan eksplisit user: integrasi Shizuku, sweep-select-to-undo, warning eksplisit root tidak auto-dibuat -- 2026-08-17
- User minta 3 hal SEKALIGUS di 1 sesi (tidak dipecah batch, dikerjakan sbg
  1 Atomic Change krn saling terkait scope "tujuan kustom" & "UX undo"):
  (1) "aplikasi Wajib terintegrasi 100% dengan shizuku", (2) fitur
  sweep-select-to-undo "biar gak ribet buat user", (3) warning sejelas-
  jelasnya bahwa folder root tujuan kustom TIDAK otomatis dibuat app.
- **Integrasi Shizuku**: BARU package `shizuku/` (3 file: `IFileOpsService.aidl`
  kontrak IPC path-absolut, `FileOpsUserService.kt` implementasi Stub yang
  jalan di PROSES SHIZUKU (UID shell/adb atau root), `ShizukuManager.kt`
  singleton lifecycle binder/permission). `FileSorter.scanAndSort()` dapat
  cabang BARU `scanAndSortViaShizuku()` -- dicek PALING AWAL, SALING
  EKSKLUSIF dgn cabang SAF (toggle `useShizuku` di Settings). Arsitektur
  ikut pola yang SUDAH jadi pelajaran permanen project ini: sumber scan
  TETAP SELALU Downloads, Shizuku HANYA tujuan (SAMA seperti SAF, lihat
  restrukturisasi SAF_FINAL_VERDICT_FIX 2026-08-13 di entri lama). Subfolder
  RULE di-resolve SEKALI SERIAL sebelum diproses paralel -- PROAKTIF
  menghindari kelas bug race "folder duplikat" yang PERNAH terjadi nyata
  di SAF (bukan ditemukan lewat insiden baru kali ini, tapi pelajaran lama
  DITERAPKAN LEBIH DULU sebelum bug yang sama sempat terjadi di jalur baru).
  `undo()` dapat cabang baru via prefix palsu `"shizuku://"` di `destUri`
  (pola identik prefix `content://` SAF -- TIDAK perlu skema Room baru,
  DB Schema/DAO protected asset TIDAK disentuh sama sekali).
- **Sweep-select-to-undo**: `ActivityLogScreen.kt` REWRITE PENUH (bukan
  edit parsial -- terlalu banyak state/gesture baru saling terkait utk
  di-patch bagian per bagian dgn aman). Tab Undo: tekan-lama 1 baris ->
  mode seleksi, LALU sapukan jari (drag) ke baris lain utk toggle-pilih
  banyak sekaligus (`detectDragGestures` + `onGloballyPositioned` rekam
  posisi tiap baris) -- checkbox & tap biasa tetap ada sbg alternatif.
  `MainViewModel.undoMultiple()` baru (sekuensial, bukan paralel -- volume
  seleksi manual biasanya kecil), dikonfirmasi lewat `VaultActionSheet`
  yang sama dgn undo tunggal sebelum eksekusi.
- **Warning root tidak auto-dibuat**: komponen `WarningBanner.kt` baru
  (ikon+`colors.error`, bukan sekadar info pasif) dipasang di KEDUA kartu
  tujuan kustom Settings -- SAF (perilaku "root tidak auto-dibuat" SUDAH
  ada sejak v7.2.0 tapi SEBELUMNYA cuma tercatat di dokumentasi teknis,
  TIDAK pernah ditampilkan ke user di UI -- gap ini yang ditutup sekarang)
  dan Shizuku (baru, perilaku SAMA PERSIS diterapkan sejak awal: app
  MENOLAK scan dgn pesan error eksplisit kalau root belum ada, TIDAK PERNAH
  membuatkannya diam-diam).
- File diubah (9) + 4 baru -- lihat CHANGELOG.md v7.3.0 utk daftar lengkap
  per-file. `preflight_check.sh` 13/13 lolos.
- **Batas jujur, WAJIB dibaca sebelum sesi berikutnya klaim "Shizuku
  jalan"**: Shizuku (poin 1) & gestur sapuan custom (poin 2) adalah DUA
  permukaan API yang BELUM PERNAH dipakai project ini SAMA SEKALI sebelum
  batch ini -- beda dari SAF yang setidaknya sudah py 7+ iterasi
  pengalaman nyata (lihat Insiden #7). BELUM lewat `./gradlew`/device
  asli/aplikasi Shizuku sungguhan sama sekali. Kalau CI/build gagal di
  `shizuku/` atau gestur sapuan "kalah" oleh scroll LazyColumn di device
  asli, itu BUKAN tanda ditulis ceroboh -- itu risiko yang SUDAH
  didokumentasikan eksplisit di sini sejak awal, tindak lanjuti dgn fix
  bertarget (kirim log error), BUKAN menghapus fitur tanpa didiskusikan
  dulu ke user (pelajaran sama dgn Insiden #7 SAF).
- Confidence Rating: **70%** (sengaja lebih rendah dari batch biasa --
  lihat rincian lengkap per-poin di CHANGELOG.md v7.3.0). **User WAJIB
  verifikasi**: (1) build CI hijau (prioritas #1 -- dependency Shizuku +
  AIDL codegen = risiko compile paling nyata di batch ini), (2) kartu Mode
  Shizuku di Pengaturan menampilkan status yang benar sesuai kondisi
  Shizuku di HP, (3) sapuan jari di tab Undo Pemindahan benar-benar
  memilih banyak baris tanpa kalah oleh scroll, (4) warning banner tampil
  jelas & mudah dibaca di kedua kartu tujuan kustom (SAF & Shizuku).
- versionCode 87->88, versionName 7.2.0->7.3.0.

## STATUS PROJECT SEBELUMNYA: v7.2.0 -- PERUBAHAN ARSITEKTUR: app berhenti bikin folder root "PromptVault" sendiri di folder tujuan kustom (permintaan langsung user) -- 2026-08-17
- Setelah 2 ronde mitigasi (v7.1.5, v7.1.6) tidak berhasil membuktikan/
  menyingkirkan tuntas duplikat "PromptVault (N)" -- user minta hilangkan
  pemicunya langsung: app TIDAK LAGI `createDirectory("PromptVault")`.
  User bikin & pilih folder root SENDIRI lewat SAF picker; app cuma bikin
  subfolder RULE (Apps vault, dst) di dalamnya -- jalur yang TERBUKTI bersih
  di log Aktivitas user (16/08 11:44-17/08 07:16, APK 7.1.6 terverifikasi).
- `resolveSafRuleDestinations`: `destinationRoot` LANGSUNG dipakai sbg vault
  root, tanpa lapisan `findOrCreateChildDirSaf(..., "PromptVault", ...)`
  lagi. Ini menghapus SATU-SATUNYA titik `createDirectory()` untuk folder
  root di seluruh codebase (bukan cuma menambal lagi).
- **Breaking behavioral change, WAJIB dibaca user**: user dgn folder tujuan
  kustom lama (berisi subfolder "PromptVault") -- scan baru nulis LANGSUNG
  ke root, file lama TIDAK dipindah otomatis. Solusi: arahkan SAF picker ke
  folder "PromptVault" lama itu sendiri kalau mau lanjutin struktur lama.
  Jalur lokal (default Downloads, tanpa custom dest) TIDAK berubah.
- `ActivityLogScreen` tab Undo: label tujuan dibedakan per `destUri`
  (content:// = SAF -> "folder tujuan kustom/<rule>/", path absolut = lokal
  -> tetap "PromptVault/<rule>/"). Entri lama tetap akurat (baca dari
  `destUri` tersimpan).
- Cache-Uri (v7.1.5) & retry+instrumentasi (v7.1.6) DIPERTAHANKAN utk
  subfolder rule -- keduanya terbukti bekerja baik, cacheKey disederhanakan
  jadi nama rule langsung (bukan lagi "PromptVault/<rule>").
- File diubah (3): `util/FileSorter.kt`, `ui/screens/ActivityLogScreen.kt`,
  `app/build.gradle.kts` (versi). `preflight_check.sh` 13/13 lolos.
- Confidence Rating: **80%** (perubahan struktural low-risk krn menghapus
  kode, bukan menambah logika baru -- tapi tetap belum lewat
  `./gradlew`/device asli, lihat CHANGELOG.md v7.2.0 utk daftar verifikasi
  wajib user).
- versionCode 86->87, versionName 7.1.6->7.2.0.

## STATUS PROJECT SEBELUMNYA: v7.1.6 -- Duplikat "PromptVault (N)" MASIH terjadi setelah v7.1.5, retry+instrumentasi (BELUM klaim fix definitif) -- 2026-08-16
- User konfirmasi via chat: sudah update APK v7.1.5, sudah rapikan folder
  lama manual, test ulang -> **duplikat baru muncul LAGI**. Cache-by-Uri
  v7.1.5 TIDAK CUKUP sendirian.
- **Ditulis jujur**: root cause pasti BELUM terkonfirmasi 100% tanpa
  Logcat/device asli (keterbatasan lingkungan kerja Claude yang sudah
  konsisten dicatat tiap sesi -- lihat entri v7.1.4/v7.1.3 dst). Sesi ini
  TIDAK klaim "sudah fix", tapi: (1) retry+delay 200ms sebelum create kalau
  `findFile()` null (mitigasi dugaan staleness), (2) verifikasi nama pasca-
  `createDirectory()` + log `ERROR` eksplisit ke Activity Log kalau provider
  ternyata balikin nama beda (mis. "PromptVault (1)") -- BUKTI KONKRET, bukan
  tebakan, utk sesi berikutnya.
- **Pertanyaan terbuka BELUM terjawab, PRIORITAS sesi berikutnya**: user
  pakai "Folder Tujuan Kustom" (SAF) di Pengaturan, atau default Downloads?
  Kalau default -> `File.mkdirs()` (java.io.File biasa) TIDAK BISA
  auto-suffix "(N)" secara struktural -> seluruh dugaan jalur SAF di
  v7.1.4-v7.1.6 SALAH ALAMAT, sumber duplikat ada di tempat lain yang BELUM
  diperiksa. **JANGAN lanjut tambal jalur SAF lagi sebelum fakta ini
  dikonfirmasi user.**
- File diubah (2): `util/FileSorter.kt`, `app/build.gradle.kts` (versi).
  `preflight_check.sh` 13/13 lolos.
- Confidence Rating: **50%** (sengaja rendah -- iterasi ke-2 utk bug yang
  sama, mitigasi+instrumentasi, BUKAN fix definitif; detail lengkap
  CHANGELOG.md v7.1.6).
- versionCode 85->86, versionName 7.1.5->7.1.6.

## STATUS PROJECT SEBELUMNYA: v7.1.5 -- FIX duplikat folder "PromptVault"/"(N)" berulang, root cause staleness listing SAF antar-scan (BEDA dari race-fix 2026-08-13) -- 2026-08-16
- User lapor (screenshot) 7 folder "PromptVault"/"(1)".."(6)" di tujuan
  kustom, tiap folder isi LENGKAP (9-10 item), tanggal 15-16/08 -- pola BEDA
  dari bug lama (1 item/duplikat, race antar-coroutine, sudah difix
  2026-08-13 via serialisasi `resolveSafRuleDestinations`). Fix lama itu
  TETAP BENAR & DIPERTAHANKAN (menutup race dlm 1 scan) -- celah baru ada di
  luar cakupannya: staleness listing SAF ANTAR-scan (scan tetap serial via
  `scanMutex`, TERVERIFIKASI DI KODE, bukan race baru).
- **Root cause**: `findFile()` (`listFiles()` query) di `findOrCreateChildDirSaf`
  dipanggil ULANG tiap scan. Sebagian provider/OEM listing children bisa
  STALE sesaat pasca `createDirectory()` scan sebelumnya (lag FUSE/index) --
  scan berikutnya "tidak lihat" folder yg sudah ADA secara fisik, bikin baru
  -> provider deteksi tabrakan nama di level filesystem -> auto-suffix.
- **Fix**: cache Uri folder (root "PromptVault" + tiap subfolder rule) di
  `SettingsRepository` (key relatif thd `safTreeUri`, auto-invalidate kalau
  root ganti), `findOrCreateChildDirSaf` coba resolusi LANGSUNG by-Uri
  (`fromSingleUri`+`exists()`) dari cache dulu sebelum fallback ke
  `findFile()`/`createDirectory()` lama. Detail lengkap: CHANGELOG.md v7.1.5.
- File diubah (3): `util/FileSorter.kt`, `data/SettingsRepository.kt`,
  `app/build.gradle.kts` (versi). Tidak ada file baru. `preflight_check.sh`
  13/13 lolos (sempat false-positive kurung tidak seimbang gara² komentar
  "1)"/"2)" numbering -- diganti "Langkah 1:"/"Langkah 2:" biar heuristik
  proyek yg literal-count karakter tidak salah baca).
- **PENTING**: folder duplikat yg SUDAH ADA di device user TIDAK otomatis
  digabung -- fix ini cuma cegah duplikat baru ke depan. User perlu gabung
  manual isi tiap "PromptVault (N)" ke folder asli lalu hapus yg kosong.
- Confidence Rating: **85%** (root cause & fix well-reasoned dari bukti
  kode+screenshot, tapi staleness provider/OEM tidak bisa disimulasikan
  tanpa device asli -- lihat CHANGELOG.md utk detail penuh & item verifikasi
  wajib user).
- versionCode 84->85, versionName 7.1.4->7.1.5.

## STATUS PROJECT SEBELUMNYA: v7.1.4 -- FIX 3 GAP P0 audit eksternal (folder-name traversal, copy parsial, urutan undo SAF), Phase 1/4 -- 2026-08-16
- User upload `PromptVault_real_functional_polish_gap_audit.md` (audit statis
  eksternal baru: 3 P0, 9 P1, 7 P2 -- BEDA dokumen dari `SAF_FINAL_VERDICT_FIX.txt`/
  `SAF_FINAL_LOGIC_AUDIT.md` lama, jangan tertukar). Instruksi eksplisit user:
  **"kerjakan secara bertahap ... jangan greedy"** -- dibaca sebagai: JANGAN
  coba tuntaskan seluruh audit (19 temuan) dalam 1 batch. Dieksekusi: **Phase 1
  PENUH** dari "Priority Fix Order" audit tsb (3 item P0, "Safety/correctness"),
  BUKAN sebagian P0 + sebagian P1 dicampur (supaya batch ini tuntas 1 fase utuh,
  bukan setengah-setengah lintas fase).
- **P0-1 (folder rule tidak divalidasi -> potensi path traversal)**: file BARU
  `util/RuleFolderNameValidator.kt` (`validateRuleFolderName`/`isValidRuleFolderName`/
  `isContainedIn`, top-level pure function, pola sama `mimeTypeForFileName`/
  `GlobMatcher` -- unit-testable tanpa Context, lihat `RuleFolderNameValidatorTest.kt`
  baru). Dipasang di 2 lapis WAJIB: `AddEditRuleScreen.kt` (inline `isError`+
  `supportingText`, Save disabled kalau invalid) DAN `FileSorter.moveFile()`+
  `resolveSafRuleDestinations()` (gerbang terakhir -- rule LAMA yang sudah
  tersimpan sebelum fix ini tetap divalidasi ulang tiap scan, bukan cuma dicegah
  untuk rule baru). Detail lengkap: CHANGELOG.md v7.1.4.
- **P0-2 (`copyThenDelete()` bisa nyisain file tujuan PARSIAL kalau copy gagal
  di tengah jalan)**: fix pola temp-file-lalu-rename (tulis ke `<nama>.tmp_<uuid>`
  dulu, verifikasi, baru rename ke nama final) -- nama final TIDAK PERNAH
  tersentuh sampai transfer tuntas. `copyThenDelete` sekarang `suspend fun`
  (dipanggil dari `moveFile`+`undo()`, keduanya sudah suspend context, aman).
- **P0-3 (urutan `markUndone()` salah di `undoSaf()`/`undoSafDestination()`)**:
  sebelumnya riwayat ditandai "selesai undo" TANPA SYARAT begitu salinan balik
  sukses, TIDAK PEDULI hasil `current.delete()` (hapus salinan lama di tujuan).
  Fix: `markUndone()` HANYA dipanggil kalau delete BENAR-BENAR sukses; gagal ->
  entri riwayat SENGAJA dibiarkan "belum selesai" + WARNING "Undo SEBAGIAN" di
  Activity Log, supaya user tahu ada duplikat & bisa coba undo lagi -- bukan
  silent-mark-done yang menyembunyikan state sebenarnya.
- **SENGAJA TIDAK dikerjakan sesi ini** (Phase 2-4 audit, BUKAN lupa): P1-1
  s.d P1-9 (test matrix FileSorter lengkap, retry worker, kategori skip granular,
  RuleRepository transactional, semantik import, OVERWRITE destruktif tanpa
  histori, dst.) & semua P2 (min/max validation, diagnostics live-refresh, dst.).
  Lihat "Priority Fix Order" di `PromptVault_real_functional_polish_gap_audit.md`
  utk urutan Phase 2-4 kalau user minta lanjut -- JANGAN audit ulang dari nol,
  daftar lengkap 16 temuan sisa sudah ada di dokumen itu (masih relevan sampai
  dieksekusi/di-supersede entri PROJECT_STATE yang lebih baru).
- File diubah (4) + 2 baru: `util/FileSorter.kt`, `ui/screens/AddEditRuleScreen.kt`,
  `app/build.gradle.kts` (versi), `FILE_MANIFEST.txt`; BARU `util/
  RuleFolderNameValidator.kt`, `app/src/test/.../RuleFolderNameValidatorTest.kt`.
  `scripts/preflight_check.sh` 13/13 lolos bersih.
- Confidence Rating: **90%** (3 fix bertarget, masing-masing risiko rendah
  secara individual -- validator murni pure-function baru + reuse pola project
  yang sudah ada, temp-file-rename adalah pola standar well-known, reorder
  markUndone tidak mengubah struktur fungsi lain; turun dari 95%+ semata krn (1)
  BELUM PERNAH lewat `./gradlew` asli/device asli sama sekali (konsisten seluruh
  riwayat project), (2) skenario P0-2/P0-3 (disk penuh, provider SAF menolak
  delete) SULIT disimulasikan tanpa device/provider asli utk diverifikasi
  end-to-end, cuma bisa direview manual jalur logikanya). **User WAJIB
  verifikasi**: (1) isi nama folder rule dengan `/` atau `..` di Tambah/Edit
  Rule -- field harus tampil error merah, tombol Simpan nonaktif, (2) rule
  normal (tanpa karakter aneh) tetap tersimpan & scan seperti biasa (nol
  regresi jalur normal -- PALING PENTING, ini jalur yang dipakai 100% user
  selama ini), (3) build CI hijau.
- versionCode 83->84, versionName 7.1.3->7.1.4.

## STATUS PROJECT SEBELUMNYA: v7.1.3 -- FIX GAP FUNGSIONAL NYATA: POST_NOTIFICATIONS tidak pernah diminta runtime -- 2026-08-16
- User minta audit lebih dalam (edge case DB/permission/migrasi) setelah
  static audit TODO/FIXME kosong. **Item pertama yang dicoba
  (`FileSorter.undo()` dispatcher) FALSE POSITIVE** -- ternyata sudah difix
  di v2.20.1 di level `MainViewModel.undoMove()` (`withContext(Dispatchers.IO)
  { fileSorter.undo(entry) }`), bukan di dalam `FileSorter.undo()` sendiri.
  Catatan v2.16.0 yang jadi acuan awal ("kandidat batch terpisah") sudah
  usang/superseded tapi sempat kebaca tanpa cross-check entri v2.20.1 yang
  lebih baru -- **langsung direvert begitu ketahuan, sebelum sempat
  di-package**. Pelajaran: SELALU cek entri PALING BARU yang menyinggung
  topik yang sama sebelum eksekusi, bukan cuma entri pertama yang ketemu.
- **Audit Room DB**: `AppDatabase.kt` (version=1 dari awal,
  `fallbackToDestructiveMigration` terdokumentasi jelas & disengaja --
  belum pernah ada migrasi asli krn versi belum pernah naik), `MoveHistoryDao`/
  `ActivityLogDao` (trim FIFO 200/500 baris + debounce v2.4.1 sudah
  optimal), `Converters.kt` (enum LogLevel<->String dgn `getOrDefault`
  fallback aman kalau ada rename enum masa depan) -- **semua ditinjau,
  TIDAK ADA bug ditemukan**.
- **Audit permission -- GAP NYATA ditemukan & difix**: `POST_NOTIFICATIONS`
  dideklarasikan `AndroidManifest.xml` sejak Batch §5 (utk notifikasi
  ongoing `AutoSortWorker`, lihat `AutoSortNotification.kt`) TAPI **tidak
  pernah diminta runtime** (grep konfirmasi 0 pemanggilan
  `ActivityResultContracts.RequestPermission()` utk izin ini di seluruh
  kode). `targetSdk=34` (Android 14) -- jauh di atas ambang API 33 tempat
  izin ini WAJIB diminta eksplisit, deklarasi manifest saja tidak cukup.
  **Dampak nyata**: notifikasi "Auto-sort sedang berjalan" -- tujuan UTAMA
  Batch §5 (kasih user visibility scan background) -- kemungkinan TIDAK
  PERNAH tampil di Android 13+ manapun, padahal foreground service-nya
  sendiri tetap jalan diam-diam (bukan crash, tapi kehilangan visibility
  yang jadi alasan fitur itu dibuat).
- **Fix**: `MainActivity.kt` (protected asset, parsial) -- launcher baru
  `notificationPermissionLauncher`, diminta SEKALI (one-shot, flag DataStore
  `notification_permission_asked`, pola sama dgn `onboarding_done`) tepat
  setelah user lolos gate storage permission + onboarding. Hasil grant/deny
  SENGAJA diabaikan -- fitur pelengkap (visibility), BUKAN gate wajib spt
  storage, user yang menolak tidak dipaksa dialog berulang tiap buka app.
- **Audit migrasi**: `LegacyDataMigration.kt` sudah diverifikasi cukup aman
  di v2.20.2 (no-op murni kalau key tidak cocok, guard flag anti-retry-loop)
  -- tidak ada gap baru ditemukan sesi ini.
- File diubah (2): `MainActivity.kt` (parsial), `app/build.gradle.kts`
  (versi). `FILE_MANIFEST.txt` tidak berubah. `preflight_check.sh` 13/13
  lolos bersih.
- Confidence Rating: **92%** (turun dari biasanya krn: (1) BELUM PERNAH
  lewat `./gradlew` asli/device asli spt biasa, (2) API
  `ActivityResultContracts.RequestPermission()` single-permission BELUM
  ADA preseden lain di codebase ini -- yang sudah ada cuma
  `RequestMultiplePermissions()` (2 izin storage legacy) dan
  `OpenDocumentTree()`, jadi pola single-permission ini baru pertama kali
  dipakai project ini, ditulis dari API resmi Android tapi belum ada
  cross-check preseden internal). User WAJIB verifikasi: (1) dialog izin
  notifikasi muncul SEKALI saat pertama kali masuk app (HANYA di Android
  13+, tidak akan muncul di HP Android 12 ke bawah -- itu benar/sesuai
  desain, bukan bug), (2) setelah accept, notifikasi "Auto-sort sedang
  berjalan" benar-benar tampil saat auto-scan jalan di background, (3)
  kalau ditolak, app tetap 100% berfungsi normal (cuma notifikasi itu yang
  tidak tampil).
- versionCode 82->83, versionName 7.1.2->7.1.3.

## STATUS PROJECT SEBELUMNYA: v7.1.2 -- Polish UI lanjutan: highlight GlassPanel diagonal->vertikal + fix Row Undo tanpa CenterVertically -- 2026-08-16
- User tegaskan lagi (v7.1.1 belum cukup): "Toggle/saklar, icon menu, dan
  undo button. Semuanya masih asimetris" -- diaudit ULANG dari nol (bukan
  percaya fix RuleCard v7.1.1 sudah menutup semua), fokus ke 3 elemen
  eksplisit yang disebut.
- **Audit matematis RuleCard/TactileSwitch/GroupedListRow (layout murni)**:
  dihitung ulang semua offset/inset/touch-target satu-satu -- TIDAK ketemu
  bug POSISI/UKURAN (touch target 5 kontrol RuleCard sudah sama 48dp,
  inset thumb switch 3dp presisi sama tiap sisi, kotak ikon GroupedListRow
  30dp+icon 16dp center, indent divider 58dp match posisi teks). Layout
  angka-nya SUDAH simetris sejak v7.1.1 -- tapi user tetap lihat "berat
  sebelah" di 2 elemen ini (toggle & icon menu), jadi akar masalahnya
  BUKAN di angka layout.
- **Akar sebenarnya ditemukan di `GlassPanel.kt`** (primitif BERSAMA yang
  dipakai thumb TactileSwitch, kotak ikon GroupedListRow, pil
  SegmentedControl, VaultCard, dst.): overlay "highlight" pakai
  `Brush.linearGradient(colors=[GlassHighlight, Color.Transparent])` TANPA
  `start`/`end` eksplisit -- default Compose menarik gradient DIAGONAL
  pojok kiri-atas ke pojok kanan-bawah. Di elemen BESAR (VaultCard) efek
  ini nyaris tak kentara, tapi di elemen KECIL bulat/pill (thumb 20dp,
  kotak ikon 30dp) satu pojok jelas terang & pojok seberang gelap polos --
  scan visual manusia langsung baca ini sebagai "tidak simetris" walau
  bounding-box/posisinya presisi center. **Fix**: `Brush.verticalGradient`
  (atas->bawah) -- simetris kiri-kanan, kesan "cahaya dari atas" tetap ada
  (bahasa visual glassmorphism tidak berubah), cuma arahnya diluruskan.
  1 titik ubah di 1 file (`GlassPanel.kt`), otomatis berlaku ke SEMUA
  pemakai primitif ini (toggle & icon menu SEKALIGUS, tanpa sentuh
  masing-masing file) -- termasuk kenapa 2 keluhan user yang kelihatannya
  tidak berhubungan (toggle + icon menu) ternyata 1 akar yang sama.
- **Undo button** (`ActivityLogScreen.kt`, tab "Undo Pemindahan"): BUG
  LAYOUT NYATA ditemukan (beda kelas dari 2 di atas) -- `Row` pembungkus
  Column-teks (3 baris: nama file/tujuan/waktu) + `TextButton("Undo")`
  TIDAK punya `verticalAlignment` (default `Alignment.Top`), BEDA dari
  `Row` tab "Log" di atasnya yang sudah benar pakai `CenterVertically`.
  Karena Column kiri jauh lebih tinggi (3 baris) dari tombol (1 baris),
  tombol nempel RATA ATAS, nyisa ruang kosong di bawahnya -- match persis
  laporan user. Fix: tambah `verticalAlignment = Alignment.CenterVertically`,
  pola sama dgn Row tab Log.
- File diubah (3, non-Atomic): `GlassPanel.kt` (1 brush), `ActivityLogScreen.kt`
  (1 Row), `app/build.gradle.kts` (versi). `FILE_MANIFEST.txt` tidak berubah.
  `preflight_check.sh` 13/13 lolos bersih.
- Confidence Rating: **95%** (2 fix independen berisiko rendah -- 1 ganti
  arah Brush tanpa logika baru, 1 tambah parameter alignment standar Compose
  yang sudah ada polanya persis di file yang sama; turun dari 97%+ semata
  krn tetap BELUM PERNAH lewat `./gradlew` asli/device asli, sandbox tanpa
  Android SDK, DAN karena root-cause GlassPanel diinferensi dari membaca
  perilaku default `Brush.linearGradient` -- bukan dari screenshot baru user
  sesi ini). **User WAJIB verifikasi visual**: (1) thumb switch & kotak ikon
  menu Home sekarang terang merata dari ATAS (bukan lagi nyala di 1 pojok
  doang), (2) tombol "Undo" di tab "Riwayat Aktivitas" -> "Undo Pemindahan"
  sekarang center vertikal sejajar tengah teks di sampingnya, bukan nempel
  atas. Kalau MASIH terasa asimetris setelah ini, kirim screenshot -- akar
  penyebabnya kemungkinan bukan lagi di 3 elemen yang sama, butuh titik
  visual baru untuk diaudit.
- versionCode 81->82, versionName 7.1.1->7.1.2.

## STATUS PROJECT SEBELUMNYA: v7.1.1 -- Polish UI: fix kontras border WCAG 1.4.11 + rapikan baris kontrol asimetris RuleCard -- 2026-08-16
- User kirim 4 screenshot build v7.1.0 NYATA (bukan cuma baca kode) + minta
  fokus 2 hal eksplisit: (1) WCAG utk "layout terdistorsi", (2) khusus polish
  UI/rapikan asimetri -- ditegaskan "no less no more", jadi TIDAK melebar ke
  redesign/ganti hue.
- Audit WCAG numerik (bukan tebakan) ke SEMUA token x tingkat permukaan:
  `GlassBorder`/`HairlineGlass` GAGAL 1.4.11 (1.49-1.55:1, alpha 0.14f) ->
  naik 0.38f (3.00-3.80:1, lulus). `TextMuted` GAGAL 4.5:1 teks normal
  (3.45-3.81:1 di alpha 0.42f, token dead-code tapi tetap diperbaiki
  preventif) -> naik 0.56f (4.81:1 worst-case). `BrassAccent` DICEK TERPISAH
  -- 7.44:1 LULUS AAA, bukan pelanggaran; saturasi rendahnya (47% vs
  Amber/Slate/Rust 77-100%) BUKAN bug WCAG, dan hex-nya dipatok eksplisit
  user sesi sebelumnya -- **ditanyakan ke user via pilihan sebelum
  disentuh**, dijawab fokus ke asimetri layout, jadi hex Brass TIDAK diubah
  sesi ini (keputusan didokumentasikan, bukan diam-diam dilewati).
- Asimetri layout NYATA yang ditemukan (dari screenshot "Kelola Rule"):
  `RuleCard.kt` baris kontrol aksi pakai `Spacer(weight(1f))` di tengah,
  numpuk 2 tombol reorder di kiri vs 3 kontrol lain di kanan, nyisa celah
  kosong lebar di tengah -- diganti `Arrangement.SpaceEvenly`, 5 kontrol
  merata di lebar penuh.
- File diubah (3, non-Atomic): `Color.kt` (2 token alpha), `RuleCard.kt`
  (1 Arrangement), `app/build.gradle.kts` (versi). `FILE_MANIFEST.txt` tidak
  berubah. `preflight_check.sh` 13/13 lolos bersih.
- Confidence Rating: **96%** (fix WCAG murni numerik terverifikasi formula
  relative luminance W3C + 1 fix layout Arrangement straightforward, tidak
  ada logika baru berisiko; turun dari potensi 97%+ semata krn tetap BELUM
  PERNAH lewat `./gradlew` asli/device asli, sandbox tanpa Android SDK).
  User WAJIB verifikasi visual: (1) border kartu/chip kelihatan jelas tapi
  tidak mengganggu di kedua preset tema, (2) baris kontrol RuleCard di
  "Kelola Rule" sekarang seimbang kiri-kanan, bukan berat sebelah.
- versionCode 80->81, versionName 7.1.0->7.1.1.

## STATUS PROJECT SEBELUMNYA: v7.1.0 -- FITUR BARU: toggle tema (Deep Navy+Brass <-> Charcoal+Copper) di Pengaturan -- 2026-08-15
- User upload state repo terkini (`PromptVault-main.zip`, ternyata sudah
  v7.0.1 -- lompat dari v7.0.0). **Penting utk sesi berikutnya**: draft
  Neumorphism `drawBehind`+`Paint.setShadowLayer` yang sempat digarap sesi
  SEBELUM ini TIDAK PERNAH di-deliver/push ke user -- sesi lain sudah ambil
  arah berbeda (balik Glassmorphism total, `GlassPanel.kt`) & sudah
  di-push+CI-hotfix duluan. Draft itu SEPENUHNYA gugur, jangan dilanjutkan
  atau dicari lagi -- state ground-truth SELALU upload/PROJECT_STATE.md
  terbaru, bukan riwayat chat sesi manapun.
- Diminta lanjut sbg "toggle saklar tema custom" -- ambigu, DIKLARIFIKASI
  via pertanyaan pilihan (bukan ditebak): user pilih **switch ON/OFF simpel
  antara 2 preset TETAP**, BUKAN color picker bebas atau banyak preset.
- **Preset ke-2 "Charcoal + Copper" -- BARU dirancang sesi ini** (`#12100E`
  root, H=30 hangat, sengaja beda ARAH hue dari Navy H=225 spy 2 preset
  kerasa beda bukan cuma gelap/terang yg sama; `#C97B4A` aksen). BUKAN
  rekonstruksi Platinum/Ruby v6.0.0 -- itu sudah dihapus total v7.0.0, hex
  persisnya tidak tercatat presisi di mana pun (CHANGELOG/PROJECT_STATE)
  utk direkonstruksi dgn aman, jadi TIDAK dicoba "dikembalikan". WCAG
  dihitung manual sama rigor-nya dgn preset default: teks di atas Charcoal
  ~19:1 (AAA), teks gelap di atas Copper 5,80:1 (AA) -- lulus.
- **Implementasi BENAR-BENAR reaktif, BUKAN switch UI kosong** -- pelajaran
  wajib dihormati dari Insiden `ThemeMode` v2.16.0 (dihapus total krn
  togglenya TIDAK PERNAH benar-benar mengubah apa pun, `PromptVaultTheme`
  hardcode 1 skema, parameter diabaikan). Diverifikasi manual: `Theme.kt`
  `PromptVaultTheme(useAltTheme: Boolean)` SEKARANG memilih 1 dari 2
  `ColorScheme` (`VaultDarkColorsDefault`/`VaultDarkColorsAlt`, struktur
  peran M3 identik, cuma token beda) tiap recomposition -- bukan parameter
  mati.
- **Status/nav bar sistem ikut reaktif** (celah yg TIDAK ada di era
  `ThemeMode` lama krn togglenya memang tidak pernah berfungsi sama sekali):
  `resolveBackgroundColor()` baru (Theme.kt, 1 sumber kebenaran) dipanggil
  dari `SideEffect` BARU di `MainActivity.setContent` -- tiap `useAltTheme`
  berubah (baik krn DataStore baru selesai dimuat ATAU user toggle live),
  `enableEdgeToEdge` dipanggil ULANG dgn warna yg benar. Tanpa ini, chrome
  sistem bisa nyangkut di preset lama walau konten Compose sudah pindah.
- File diubah (6, non-Atomic -- fitur baru murni tambahan, tidak ada 1
  sistem visual lama yg "dipecah"): `SettingsRepository.kt` (DataStore
  boolean, pola identik `intervalMinutesFlow`), `MainViewModel.kt`
  (`StateFlow`+setter, pola identik `scanConcurrency`), `Color.kt` (token
  Charcoal/Copper baru, 5 tingkat elevasi dihitung HSL manual sama seperti
  Navy), `Theme.kt` (2 ColorScheme + `PromptVaultTheme` reaktif +
  `resolveBackgroundColor`), `SettingsScreen.kt` (section "Tema" baru,
  `TactileSwitch`), `MainActivity.kt` (PARSIAL: collect state + SideEffect
  + teruskan param, protected asset lain TIDAK disentuh). `FILE_MANIFEST.txt`
  TIDAK berubah (0 file baru/dihapus).
- `scripts/preflight_check.sh` dijalankan ulang, **13/13 kategori lolos
  bersih** (termasuk #13, kategori hotfix v7.0.1 utk KDoc `*/` prematur --
  KDoc panjang batch ini otomatis tervalidasi tidak mengulang bug yg sama
  yg baru saja menyebabkan CI gagal total di v7.0.1).
- Confidence Rating: **93%** (bukan 95%+ murni krn 2 hal: (1) **BELUM
  PERNAH lewat `./gradlew` asli/device asli** sama sekali -- sandbox tanpa
  Android SDK/jaringan Gradle, TERLEBIH lagi baru saja ada insiden CI gagal
  total v7.0.1 dari sesi lain yg JUGA "lolos preflight lama" sebelum
  ketahuan gagal compile sungguhan, jadi kewaspadaan ekstra wajar; (2) toggle
  reaktif lintas Activity+Compose (`SideEffect`+`enableEdgeToEdge` dipanggil
  ulang) adalah pola yg BELUM PERNAH dipakai project ini sebelumnya --
  logikanya masuk akal & sudah direview manual baris-per-baris, tapi TIDAK
  ada preseden lain di codebase ini utk dibandingkan). User WAJIB
  verifikasi: (1) toggle benar-benar mengganti SELURUH tampilan app, bukan
  cuma 1 layar, (2) status/nav bar ikut berubah warna, (3) tidak ada teks
  tak terbaca di preset manapun, (4) restart app dgn toggle ON -- preset
  harus tetap Charcoal+Copper sejak splash/frame pertama (persistensi
  DataStore).
- versionCode 79->80, versionName 7.0.1->7.1.0.

## STATUS PROJECT SEBELUMNYA: v7.0.1 -- HOTFIX build CI gagal total: KDoc tertutup prematur di TactileTokens.kt -- 2026-08-15
- User upload log CI gagal (`build-failure-log-v7_0_0.zip`): `kspDebugKotlin
  FAILED`, ratusan error `Expecting a top level declaration` mulai
  `TactileTokens.kt:10:27`. Semua error lain di log = 1 CASCADE dari 1 root
  cause tunggal (dikonfirmasi: `awk`/`sort -u` atas seluruh log CI cuma
  menunjuk 1 file, `ui/theme/TactileTokens.kt`).
- **Root cause**: KDoc header yang DITULIS SESI INI (v7.0.0, lihat entri di
  bawah) berisi `(Neu*/Glass*)` -- substring `*/` DI TENGAH kalimat KDoc
  menutup block comment `/** ... */` PREMATUR (Kotlin, spt C/Java, comment
  block tutup di kemunculan PERTAMA `*/`, titik, bukan yang dimaksud
  penulis). Sisa isi KDoc (baris 10 s.d. `*/` yang SEBENARNYA di baris 20)
  ke-parse compiler sbg KODE KOTLIN SUNGGUHAN -> parser Kotlin bingung total
  di situ, berantai jadi ratusan error "Expecting a top level declaration"
  di seluruh sisa file (bukan ratusan bug terpisah, 1 typo tunggal).
- **Kenapa lolos preflight sesi sebelumnya**: kategori #1 (keseimbangan
  kurung `{}()`) TIDAK mendeteksi ini krn jumlah kurung tetap seimbang --
  comment-nesting Kotlin (`/** ... */`) adalah lapisan terpisah dari
  brace-nesting kode, tidak dicek sama sekali oleh heuristik lama.
  `bash -n`/lint statis lain di sandbox JUGA tidak mendeteksi (bukan syntax
  BASH, ini syntax KOTLIN -- perlu compiler Kotlin asli/heuristik khusus).
- **Fix**: `(Neu*/Glass*)` -> `(Neu*, Glass*)` di `TactileTokens.kt`, 1
  baris, makna tidak berubah (cuma pemisah "atau" jadi koma, hindari
  karakter `*/` nyasar).
- **Fix preventif (bukan cuma tambal titik ini)**: `scripts/preflight_check.sh`
  kategori #12 lama (baseColor gradient check, SUDAH OBSOLETE sejak
  parameter `baseColor` dihapus total di v7.0.0 -- kategori itu jadi
  omong-kosong tanpa guna sejak commit sebelumnya, TIDAK ketahuan krn
  kebetulan tetap "lolos" trivial 0 gradient) dipensiunkan resmi jadi no-op
  permanen (didokumentasikan kenapa, bukan dihapus diam-diam). Kategori #13
  BARU: `grep -rnP '\*/\S' "$KT_DIR"` -- deteksi SEMUA kemunculan `*/` yang
  diikuti LANGSUNG karakter bukan-spasi di baris yang sama (comment penutup
  ASLI selalu diikuti akhir baris atau spasi, bukan lanjut teks/kode).
  Dijalankan ulang di seluruh `$KT_DIR`, 0 kemunculan lain ditemukan (bug
  ini SATU-SATUNYA insiden, bukan pola berulang) -- kelas bug ini sekarang
  KEPANTAU OTOMATIS ke depan.
- **Pelajaran utk sesi Claude berikutnya**: KDoc/comment BUKAN "teks bebas
  bahaya" -- karakter `*/` di TENGAH kalimat penjelasan (notasi
  "A*/B*" gaya wildcard, pecahan, atau simbol pembagi apapun yang kebetulan
  diikuti garis miring) bisa menutup block comment secara tidak sengaja.
  Preflight sekarang menjaring ini otomatis (kategori #13) -- TETAP hati-hati
  saat menulis KDoc panjang berisi banyak notasi teknis.
- File diubah (3): `ui/theme/TactileTokens.kt` (fix 1 baris),
  `scripts/preflight_check.sh` (kategori #12 dipensiunkan + #13 baru),
  `app/build.gradle.kts` (versi). **Preflight 13/13 kategori lolos bersih
  setelah fix** (termasuk kategori #13 baru yg langsung diuji thd bug
  aslinya -- dikonfirmasi 0 sisa). Confidence Rating: **97%** (fix titik
  tunggal, sudah divalidasi ulang lewat kategori khusus baru, risiko sisa
  hanya krn TETAP belum lewat `./gradlew` asli -- sandbox tanpa Android
  SDK/jaringan Gradle).

## STATUS PROJECT SEBELUMNYA: v7.0.0 -- Neumorphism DIHAPUS TOTAL, kembali ke Glassmorphism Deep Navy + Brass -- 2026-08-15
- User: gaya visual Neumorphism ("shadow ganda offset-Box" milik `Neumorphic.kt`,
  riwayat Insiden #3/#8/#9/#10 di bawah) dinilai **"ultra buggy"** -- minta
  hapus total & kembali ke Glassmorphism secara eksplisit, dgn 2 hex dipatok:
  `#0B132B` (Deep Navy Blue, 60-70% latar dominan) & `#B5A642` (Brass,
  10-30% aksen tombol utama). Instruksi tegas: **"dilarang keras untuk ngide
  sendiri"** -- TIDAK ADA hue baru ditambahkan di luar 2 hex ini.
- **Keputusan interpretasi (supaya transparan utk sesi berikutnya)**:
  instruksi user membatasi 2 hal SAJA -- warna latar dominan & warna aksen
  tombol UTAMA. Token semantik non-tombol-utama yang SUDAH ADA SEBELUM
  instruksi ini (`AmberGlow`/warning-auto-scan, `RustGlow`/error,
  `SlateGlow`/menu Pengaturan) SENGAJA TIDAK disentuh hex-nya -- ini bukan
  "ngide sendiri" krn bukan hue BARU, cuma token lama yang dipertahankan
  krn di luar cakupan 2 constraint eksplisit user. Kalau user MAKSUDNYA
  ingin ketiga token itu juga diseragamkan ke Navy/Brass murni, tinggal
  bilang -- keputusan ini reversibel & 1 file (`Color.kt`) saja.
- **Root cause arsitektur yang dihapus (kenapa Neumorphism "ultra buggy")**:
  `Neumorphic.kt` lama butuh (1) `baseColor` yang harus PERSIS menyamar dgn
  latar sesungguhnya di belakang tiap komponen (root cause Insiden #9 & #10
  di bawah -- gagal total di atas gradient, `baseColor` statis tidak pernah
  akurat di semua posisi scroll) dan (2) `modifier` pemanggil (`weight()`
  dst.) HARUS dipasang di `Box` pembungkus TERPISAH dari `Surface` konten
  (root cause Insiden #8 -- `weight()` nyasar, segment/kartu kolaps). Kedua
  sumber bug itu MELEKAT PADA DESAIN teknik shadow-ganda-offset-Box itu
  sendiri, bukan bug implementasi yang bisa ditambal titik-per-titik --
  sudah terbukti 3 kali (#8, #9, #10) tambal ulang tetap memunculkan kelas
  bug baru dari akar yang sama.
- **Fix arsitektur (bukan tambal)**: `Neumorphic.kt` **DIHAPUS**, diganti
  `GlassPanel.kt` -- primitif tunggal baru, `Modifier.shadow` standar Compose
  1 lapis (bukan dual-shadow-caster) + border hairline + overlay highlight
  diagonal tipis. `modifier` pemanggil sekarang dipasang LANGSUNG di
  `Surface` (satu-satunya root composable primitif ini, TIDAK ADA Box
  pembungkus tambahan) -- kelas bug Insiden #8 TIDAK MUNGKIN terulang lagi
  krn strukturnya sendiri sudah tidak punya 2 lapis composable terpisah.
  Parameter `baseColor` **DIHAPUS TOTAL** dari `VaultCard`/`GlassPanel` (0
  call site pernah override-nya, dikonfirmasi grep sebelum audit hapus) --
  kelas bug Insiden #9/#10 TIDAK MUNGKIN terulang krn parameternya sendiri
  sudah tidak ada untuk disalahgunakan.
- **Palet (`Color.kt` v7.0.0)**: `AmoledBackground` (nama token TIDAK
  diubah, supaya `MainActivity.kt` -- protected asset -- TIDAK PERLU
  disentuh sama sekali) nilainya jadi Deep Navy `#0B132B` solid.
  `GlassSurface`/`GlassSurfaceElevated`/`GlassSurfaceSheet`/
  `GlassSurfacePressed` -- tint/shade progresif dari hue Navy yang SAMA
  (bukan hue baru, murni variasi terang-gelap utk hierarki elevasi tanpa
  blur asli -- `Modifier.blur` RenderEffect cuma nyata di API 31+, project
  minSdk 26). `BrassAccent` (`#B5A642`) jadi SATU-SATUNYA aksen interaktif
  utama -- `RubyGlow`/`PlatinumAccent`/`PlatinumTint` (blend gradient CTA
  v6.0.0) **DIHAPUS TOTAL**, `primary` DAN `secondary` di `Theme.kt`
  sekarang SAMA-SAMA `BrassAccent` (CTA tidak lagi blend 2 aksen).
- **CTA "Scan Sekarang"**: gradient blend Ruby->Platinum (v6.0.0) DIHAPUS,
  sekarang 1 warna solid Brass, sesuai instruksi eksplisit "aksen tombol
  utama" TUNGGAL (bukan blend/campuran 2 warna seperti sebelumnya).
- **File diubah (13, Atomic Change -- 1 sistem visual kohesif, tidak bisa
  dipecah antar-batch tanpa membuat build gagal di tengah)**: `Neumorphic.kt`
  (DIHAPUS), `GlassPanel.kt` (BARU), `Color.kt`, `Theme.kt`,
  `TactileTokens.kt`, `VaultCard.kt`, `GroupedListRow.kt`, `TactileSwitch.kt`,
  `SegmentedControl.kt`, `EmptyState.kt`, `VaultActionSheet.kt`,
  `HomeScreen.kt`, `res/values/colors.xml`. `FILE_MANIFEST.txt` disesuaikan
  (Neumorphic.kt keluar, GlassPanel.kt masuk, urutan alfabetis, diverifikasi
  cocok 1:1 dgn tree via diff).
- `scripts/preflight_check.sh` dijalankan ulang, 12/12 kategori lolos
  (termasuk #10 well-formedness XML -- sempat gagal 1x krn `--` ganda tidak
  sah di komentar XML `colors.xml`, sudah diperbaiki). **BELUM PERNAH lewat
  `./gradlew` asli / device asli** (sandbox tanpa Android SDK/jaringan Gradle)
  -- hanya lolos preflight statis + review manual menyeluruh tiap file.
  Minta user konfirmasi build APK CI sukses & tampilan Glassmorphism Navy+
  Brass di HP sesuai sebelum dianggap selesai total.
- Confidence Rating: **92%** (bukan 95%+ murni krn keterbatasan verifikasi
  di atas -- bukan krn ada bagian yang diragukan secara desain).
- versionCode/versionName: lihat `app/build.gradle.kts` utk nilai final
  (protected asset, dinaikkan mengikuti pola versi sebelumnya).

## STATUS PROJECT SEBELUMNYA: v2.24.4 -- FIX AKAR Insiden #9 (v2.24.3 TERBUKTI TIDAK CUKUP, Insiden #10): hapus wash gradient HomeScreen -- 2026-08-15
- User kirim 3 screenshot device asli v2.24.3 (App Info konfirmasi versi
  terpasang) + laporan tegas: fix v2.24.3 GAGAL -- "ekor shadow gak jelas"
  masih nongol & sekarang malah kentara HIJAU, minta "Neumorphism real
  dengan accent Platinum+Ruby nge-blend", bukan tambal lagi.
- **Root cause v2.24.3 salah konsep (bukan cuma kurang presisi)**:
  `baseColor` = compositeOver 1 titik gradient statis, padahal gradient
  DIAM di Box terluar sementara `VaultCard`/CTA ada di `Column` yang
  verticalScroll DI ATASNYA -- posisi relatif berubah tiap scroll, TIDAK
  ADA baseColor statis yang akurat di semua posisi. Titik yang dipilih
  (dekat y=0) menyerap porsi besar `colors.surfaceVariant`
  (`GlassSurfaceElevated` 0xFF0D2622, HIJAU tua) -- shadow-caster jadi
  makin kentara beda warna, bukan makin menyatu.
- **Fix akar**: wash gradient `HomeScreen` (fitur lama UI-10, pra-
  Neumorphism v5.0.0, tujuan asli murni anti-"monoton") DIHAPUS TOTAL --
  latar sekarang `colors.background` solid, SAMA PERSIS 12 layar lain di
  app ini yang tidak pernah kena bug kelas ini. `VaultCard`/CTA Scan balik
  ke `baseColor` DEFAULT (tidak perlu compositeOver lagi). Kelas bug ini
  sekarang TIDAK BISA terulang di layar ini, bukan cuma diredam.
- Param `VaultCard.baseColor` (ditambah v2.24.3) TETAP disimpan (tidak
  di-revert) -- tidak salah, cuma tidak dipakai HomeScreen lagi saat ini.
- File diubah (2): `ui/screens/HomeScreen.kt` (hapus gradient + baseColor
  plumbing + import compositeOver), `app/build.gradle.kts` (versi).
  `scripts/preflight_check.sh` TIDAK diubah -- kategori #12 (v2.24.3) tetap
  relevan sbg jaring pengaman, lolos trivial (0 layar gradient tersisa).
  Lolos bersih 12/12. **BELUM PERNAH lewat `./gradlew` asli / device asli.**
  User WAJIB verifikasi ULANG di HP: bersih dari potongan persegi mengintip
  di SEMUA posisi scroll (bukan cuma posisi awal seperti v2.24.3 kemarin).
- versionCode 76->77, versionName 2.24.3->2.24.4.

## STATUS PROJECT SEBELUMNYA: v2.24.3 -- FIX BUG NYATA (screenshot user, Insiden #9): "kartu hantu" mengintip di HomeScreen -- 2026-08-15
- User kirim 2 screenshot HP asli v2.24.2 + instruksi eksplisit "lupakan
  progres sebelum-sebelumnya, fokus perbaiki kerusakan asimetris & cacat
  ulah sesi lain" -- diaudit LANGSUNG dari gejala visual kedua screenshot
  itu (bukan melanjutkan technical debt list sesi lalu, bukan tebakan).
- **Bug**: kartu statistik "Rule aktif/Auto-scan" & tombol "Scan Sekarang"
  di `HomeScreen` sama-sama menampakkan potongan persegi "hantu" mengintip
  di sisi kanan(-bawah) -- ASIMETRIS (cuma satu sisi, bukan shadow tipis
  merata). Root cause: keduanya lewat `NeumorphicSurface` (`Neumorphic.kt`)
  yang butuh `baseColor` = warna LATAR SESUNGGUHNYA supaya badan
  shadow-caster-nya (persegi solid digeser `shadowOffset` ke kanan-bawah)
  menyatu tak terlihat -- keduanya diam-diam pakai default
  `AmoledBackground`, padahal `HomeScreen` (SATU-SATUNYA layar di app ini
  berlatar gradient, `Brush.verticalGradient` surfaceVariant 55%alpha ->
  background, sejak fix UI-10 lama) TIDAK solid AmoledBackground di area
  itu. TIDAK terjadi di 12 pemanggil `VaultCard` lain (semua layar
  berlatar solid, tidak kena kelas bug ini -- diverifikasi via preflight
  kategori #12 baru).
- **Fix**: `VaultCard` sekarang terima param `baseColor` opsional (default
  TETAP `AmoledBackground`, 0 perubahan 12 call site lain). `HomeScreen`
  hitung warna latar efektif di titik gradient teratas
  (`colors.surfaceVariant.copy(alpha=0.55f).compositeOver(colors.background)`
  -- pakai `Color.compositeOver()` bawaan Compose) lalu dioper ke kedua
  pemanggil itu. Tidak sempurna 100% di semua posisi scroll (gradient diam,
  konten scroll di atasnya) tapi hilangkan artefak di posisi normal (kondisi
  screenshot user).
- **Preflight kategori #12 (baru)**: tiap file layar dgn `Brush.*Gradient`
  di latar, cek ADA baseColor dipasang di file yg sama -- heuristik per-file,
  supaya kelas bug ini kepantau otomatis ke depan.
- **Ditinjau & DIBIARKAN (bukan bug)**: screenshot 1 (Kelola Rule) -- FAB
  "+" menumpuk ikon hapus kartu rule #3/6 = perilaku FAB mengambang standar
  Android SAAT BELUM di-scroll penuh (kartu TERAKHIR sudah dpt
  `contentPadding(bottom=88dp)` sejak v2.24.0 #UI-20). Tidak ada kode
  diubah utk gejala ini -- bukan regresi, beda kelas dari Insiden #9.
- File diubah (4): `ui/components/VaultCard.kt`, `ui/screens/HomeScreen.kt`,
  `scripts/preflight_check.sh` (kategori #12), `app/build.gradle.kts`
  (versi). `scripts/preflight_check.sh` lolos bersih 12/12. **BELUM PERNAH
  lewat `./gradlew` asli / device asli.** User WAJIB verifikasi di HP:
  kartu stat & tombol Scan Sekarang tidak lagi ada potongan persegi
  mengintip di kanan/bawah.
- versionCode 75->76, versionName 2.24.2->2.24.3.

## STATUS PROJECT SEBELUMNYA: v2.24.2 -- FIX BUG NYATA (screenshot user): tab "Undo Pemindahan" hilang + "Log" melebar + kotak kosong raksasa di Riwayat Aktivitas -- 2026-08-15
- User kirim 2 screenshot HP asli v2.24.1 dibanding referensi struktur lama
  (3 screenshot v2.20.3): segmented control "Riwayat Aktivitas" cuma
  menampilkan pil "Log" melebar selebar layar, tab "Undo Pemindahan"
  lenyap total, + kotak hijau gelap kosong raksasa di bawahnya.
- **Root cause (`ui/components/Neumorphic.kt`)**: `NeumorphicSurface`
  memasang `modifier` parameter pemanggil (termasuk `RowScope.weight(1f)`
  dari `SegmentedControl.kt` utk segment terpilih) ke `Surface` KONTEN
  beberapa lapis `Box` DI DALAM, bukan ke `Box` TERLUAR yang benar-benar
  jadi anak langsung `Row` pemanggil. `weight()` adalah `ParentDataModifier`
  yang HANYA terbaca Row dari modifier chain anak LANGSUNG-nya -- nyasar ke
  lapisan dalam = `Row` menganggap segment itu TIDAK punya weight sama
  sekali, lebar internalnya (`Text(fillMaxWidth())`) malah mengambil
  SELURUH lebar Row, menyisakan nol ruang utk sibling. **Dicatat sebagai
  Insiden #8** (lihat riwayat insiden kronologis di bawah untuk detail
  lengkap + kelas bug ini).
- **Fix**: `modifier` sekarang dipasang di `Box` terluar (root komposabel,
  sesuai konvensi Compose resmi). `Surface` konten SENGAJA TETAP bukan
  `matchParentSize()` (beda dari shadow-caster) supaya `Box` tetap
  wrap-content tinggi mengikuti `Surface` kalau `modifier` cuma mengunci
  lebar (kasus umum: `fillMaxWidth()`/`weight()` tanpa tinggi eksplisit) --
  0 perubahan visual utk 6 pemanggil `NeumorphicSurface` lain yang sudah
  diverifikasi manual satu-satu (`VaultCard`, `GroupedListRow`,
  `TactileSwitch` x2, `EmptyState`, `VaultActionSheet`, CTA `HomeScreen`).
- **Verifikasi cakupan bug**: `grep -rn "\.weight("` seluruh `ui/` --
  HANYA 1 titik yang mengoper `weight()` sbg `modifier` param ke
  `NeumorphicSurface` (titik bug ini). Semua `weight()` lain dipasang
  LANGSUNG ke elemen native Row/Column/Spacer, tidak lewat wrapper --
  tidak kena kelas bug yang sama, tidak perlu diubah.
- File diubah (2): `ui/components/Neumorphic.kt` (fix inti + javadoc),
  `app/build.gradle.kts` (versi). `scripts/preflight_check.sh` lolos
  bersih 11/11.
- **Investigasi awal SEBELUM bug ini ditemukan** (dicatat supaya sesi
  depan tidak audit ulang dari nol): user awalnya kirim 3 screenshot
  v2.20.3 + klaim umum "layout deformasi akibat merge" TANPA screenshot
  kondisi v2.24.1 saat ini. Audit statis `RuleListScreen`/`ActivityLogScreen`/
  `RuleCard`/`SegmentedControl` terhadap referensi v2.20.3 TIDAK menemukan
  elemen hilang/struktur kacau (RuleCard 2-baris = fix P1 terdokumentasi
  v2.22.0 #UI-03, FAB `contentPadding=88dp` = fix terdokumentasi v2.24.0
  #UI-20, keduanya BUKAN regresi baru) -- diminta klarifikasi ke user via
  `ask_user_input_v0` alih-alih menebak/mengubah kode blind. User lalu
  kirim screenshot KONDISI NYATA v2.24.1 (bukan cuma klaim), barulah bug
  Insiden #8 di atas ketemu lewat pembacaan kode `Neumorphic.kt` yang
  ditunjuk balik dari gejala visual di screenshot itu. **Pelajaran
  proses**: saat klaim "deformasi/regresi" datang tanpa bukti visual
  kondisi SAAT INI, minta screenshot/gejala konkret dulu sebelum menebak
  filenya -- audit statis terhadap referensi lama saja tidak cukup untuk
  menangkap bug runtime-only class ini (`weight()` salah lapis TIDAK
  terlihat sebagai kesalahan dari membaca kode 1 file saja tanpa tahu
  ParentDataModifier semantics -- ketemu justru krn dicari SPESIFIK
  menjelaskan gejala visual "1 pil melebar + 1 hilang + kotak kosong").
- **BELUM PERNAH lewat `./gradlew` asli / device asli.** User WAJIB
  verifikasi di HP: tab "Log"/"Undo Pemindahan" tampil berdampingan
  (bukan 1 melebar penuh), tidak ada kotak kosong, kedua tab bisa di-tap.

## STATUS PROJECT SEBELUMNYA: v2.24.1 -- COMPILE-FIX "--" di colors.xml + rapikan urutan dokumentasi -- 2026-08-15
- User upload `build-failure-log-v2_24_0.zip`: `:app:mergeDebugResources
  FAILED` -- `colors.xml:11` punya `--` di badan komentar `<!-- -->`,
  dilarang keras spec XML 1.0. **Kelas bug identik dengan Insiden `v2.6.0`**
  (2026-08-05, lihat Insiden log di bawah) -- pelajaran lama ("jangan pakai
  `--` di komentar XML manapun") terulang di file `.xml` LAIN yang belum
  pernah kena kasus ini. Fix: ganti `--` jadi koma, nol perubahan logika.
- Validasi ulang SEMUA `res/**/*.xml` + `AndroidManifest.xml` via
  `xml.dom.minidom.parse` (kategori #10 `preflight_check.sh`) -- 0 masalah
  lain, `preflight_check.sh` lolos bersih 11/11.
- **Permintaan eksplisit user: rapikan urutan SEMUA dokumentasi, info
  terbaru wajib selalu di atas.** Ditemukan 2 file tidak strictly
  newest-first: `CHANGELOG.md` (klaster `v2.9.x`-`v2.11.x` ke-append di
  bawah `v2.1.x` yang lebih lama, akibat urutan insert historis, bukan
  urutan versi) dan `PROJECT_STATE.md` (entri Insiden `v2.24.0` nyangkut di
  baris PALING BAWAH file, alih-alih jadi status teratas). Kedua file
  diurutkan ulang murni berdasar versi/tanggal descending -- **0 baris
  konten dihapus atau ditulis ulang isinya, murni reposisi** (diverifikasi
  line-count sebelum/sesudah identik). Entri Insiden `v2.24.0` yang
  sebelumnya nyasar sekarang jadi entri STATUS PROJECT resmi (bukan cuma
  Insiden log) karena memang itu status rilis terkini.
- `README.md`: judul versi basi ("v2.1.4") disamakan ke `versionName`
  aktual.
- File diubah (3, semuanya di luar Batch Limit karena murni fix+reorg
  dokumentasi, bukan fitur baru): `colors.xml`, `app/build.gradle.kts`,
  `README.md`. Reorder tanpa ubah isi: `CHANGELOG.md`, `PROJECT_STATE.md`
  (file ini sendiri).
- **Belum diverifikasi CI hijau** -- WAJIB dicek run Actions berikutnya
  sebelum dianggap final, meski risiko regresi rendah (fix syntax XML murni,
  well-formedness sudah divalidasi lokal).

## STATUS PROJECT SEBELUMNYA: v2.24.0 -- Fix FAB nutup aksi kartu + re-palette Platinum+Ruby -- 2026-08-15
- User kirim screenshot nyata: FAB "+" di "Kelola Rule" nutup ikon Hapus
  kartu terakhir. Root cause: `LazyColumn` tanpa `contentPadding` bawah --
  FAB M3 by design melayang di atas konten, bukan bug Scaffold. Fix: 88dp
  bottom padding. **Pelajaran**: kalau nambah FAB ke Scaffold baru, SELALU
  cek scrollable content di dalamnya punya bottom padding/spacing yang
  cukup -- ini kelas bug yang gampang lolos audit kode statis (kelihatan
  benar di kode, cuma kelihatan salah di screenshot render nyata).
- Re-palette penuh Teal -> Platinum+Ruby (lihat javadoc `Color.kt` &
  CHANGELOG utk detail hex/rasional). Ketemu bonus: `StampGlow` lama vs
  `RustGlow` HAMPIR IDENTIK hex-nya (2 makna beda, warna nyaris sama) --
  otomatis tertutup krn Ruby baru digeser jauh ke hue crimson.
- **Belum diverifikasi**: build CI + tampilan visual asli di device (sandbox
  Termux tidak bisa compile/preview Compose). User perlu install & cek
  kontras teks CTA + kesan "blend" Platinum-Ruby sebelum dianggap matang.

## STATUS PROJECT SEBELUMNYA: v2.23.0 -- Fix 9 temuan P2 audit statis UI v2.21.1 (batch 2/2, PENUTUP audit) -- 2026-08-15
- **User minta "lanjut P2"** -- eksekusi 9 temuan P2 yang di v2.22.0 sengaja
  ditunda (rekomendasi eksplisit audit "sebaiknya masuk batch berikutnya").
- **Audit ulang dulu, bukan asumsi 9 item mentah**: dicek satu-satu ke kode
  AKTUAL (bukan percaya daftar audit txt begitu saja) -- ternyata **2 dari 9
  item SUDAH TERTUTUP** di batch P1 v2.22.0 sebelumnya sbg "sekalian, tidak
  menambah file" (lihat CHANGELOG.md v2.22.0): #UI-14 (uppercase folderName,
  `RuleCard.kt`) & #UI-16 (whitespace alignment `ManifestRow`, `HomeScreen.kt`)
  -- DAN #UI-15 (touch target crash log) ternyata sudah digabung eksekusinya
  dgn #UI-08 di `DiagnosticsScreen.kt` batch yang sama. **6 item TERSISA
  benar-benar dieksekusi** di batch ini: #UI-11, #UI-12, #UI-13, #UI-17
  (diaudit ulang, TIDAK ada gap nyata -- ditutup TANPA ubah kode, lihat
  bawah), #UI-18, #UI-19.
- **#UI-11 (`SettingsScreen.kt`, `friendlySafFolderLabel`)**: sebelumnya
  ambil bagian setelah ':' TERAKHIR di SELURUH string -- root/provider
  (mis. "primary" vs kartu SD) hilang, 2 folder beda storage tapi path akhir
  sama akan tampil identik. Fix: ambil segmen setelah "/tree/" dulu (root:path
  satu kesatuan), tampilkan KEDUANYA `path (root)`.
- **#UI-12 (`SettingsScreen.kt`, export JSON)**: tombol "Salin JSON" + Snackbar
  konfirmasi ditambah (field read-only TETAP ada sbg preview) -- pola identik
  tombol "Salin Log" `ActivityLogScreen.kt` (Insiden #6, `ClipboardManager`+
  `AnnotatedString`).
- **#UI-13 (`RuleRepository.kt`+`MainViewModel.kt`+`SettingsScreen.kt`, status
  import)**: `RuleRepository.importFromJson()` return type `Int` ->
  `ImportOutcome(parseSuccess: Boolean, importedCount: Int)` -- sebelumnya "0"
  ambigu (parse gagal vs JSON valid tapi array kosong). `MainViewModel.
  importRulesJson()` callback `(Int)->Unit` -> `(Boolean, Int)->Unit`, murni
  pass-through. `SettingsScreen` dapat sealed `ImportResultUiState`
  (Success/Warning/Error, warna primary/tertiary/error berbeda). **`MainActivity.
  kt` (Protected Asset) TIDAK PERLU disentuh sama sekali** -- lambda
  `{ text, cb -> viewModel.importRulesJson(text, cb) }` type-infer otomatis
  dari kedua sisi yang sudah cocok, diverifikasi manual sebelum diklaim (bukan
  asumsi).
- **#UI-17 (accessibility `contentDescription=null`, HomeScreen/
  GroupedListRow/ManifestRow)**: diaudit manual sesuai catatan audit sendiri
  ("Prioritas P2 hanya bila TalkBack tidak dapat konteks cukup") -- SEMUA
  icon ber-`contentDescription=null` di 3 lokasi itu decoratif & langsung
  bersebelahan dgn `Text` yang membawa makna sama (ManifestRow icon+label,
  ikon `ErrorOutline` di tombol "Lihat file dilewati", icon `GroupedListRow`
  dlm `Row` yg sama dgn label, chevron affordance redundan dgn state
  clickable). **TIDAK ADA gap nyata ditemukan -- TIDAK ada kode diubah untuk
  item ini**, ditutup sbg "diverifikasi, bukan diasumsikan aman".
- **#UI-18 (`SegmentedControl.kt`)**: `selectedIndex` sekarang di-`coerceIn`
  ke range valid (`effectiveIndex`) sebelum dibandingkan -- sebelumnya index
  di luar range = tidak ada segment terpilih sama sekali (bukan crash,
  hardening murni, belum ada laporan bug aktif).
- **#UI-19 (`SegmentedControl.kt`)**: ditemukan gap NYATA (bukan cuma beda
  gaya) saat audit ulang -- segment TIDAK terpilih sebelumnya nol feedback
  tekan sama sekali (`indication=null`, tanpa scale), beda dgn segment
  terpilih yg otomatis dapat ripple bawaan `NeumorphicSurface(onClick=...)`.
  Fix: `pressScale()` (sudah ada di `PressScale.kt`, reuse bukan bikin baru)
  diterapkan ke segment tidak-terpilih -- pola scale dipilih (bukan ripple)
  supaya konsisten dgn keluarga kontrol neumorphic lain (CTA Home,
  TactileSwitch). **Keputusan eksplisit: 2 keluarga feedback (ripple utk row
  list flat spt GroupedListRow/Diagnostics, scale utk kontrol neumorphic spt
  CTA/Switch/SegmentedControl) DIPERTAHANKAN sbg desain sengaja, BUKAN
  dianggap "inkonsistensi" yg harus diseragamkan jadi 1 pola tunggal** --
  audit sendiri menyebut ini "design system issue" tapi setelah ditelusuri
  akarnya cuma SegmentedControl unselected yg benar2 kosong, bukan seluruh
  app perlu diseragamkan ulang. Kalau user MINTA eksplisit 1 pola feedback
  tunggal utk seluruh app, itu instruksi terpisah -- jangan diasumsikan dari
  audit ini saja.
- File diubah (5): `data/RuleRepository.kt`, `ui/MainViewModel.kt`,
  `ui/screens/SettingsScreen.kt`, `ui/components/SegmentedControl.kt`,
  `app/build.gradle.kts` (versi). `scripts/preflight_check.sh` lolos bersih
  11/11 (langsung, tanpa iterasi fix kali ini). Sempat ketangkap SENDIRI
  sebelum ZIP dipaket: parameter type `onImportRequested` di signature fungsi
  `SettingsScreen` sempat lupa diupdate (`(Int)->Unit` bukan `(Boolean,
  Int)->Unit`) saat body call sudah diubah -- ketahuan via `grep` cross-check
  semua caller/callee `importFromJson`/`importRulesJson`/`onImportRequested`
  sebelum preflight, bukan lolos ke user. Juga 1 unused import (`IconButton`,
  tidak jadi dipakai krn "Salin JSON" akhirnya pakai `OutlinedButton`+`Icon`
  bukan `IconButton` polos) dibersihkan sebelum paket.
- **BELUM PERNAH lewat `./gradlew` asli.** User WAJIB verifikasi visual di HP
  asli: label folder SAF custom (kalau pakai kartu SD/lebih dari 1 storage,
  cek root ikut tampil), tombol "Salin JSON" + Snackbar, import JSON rusak/
  kosong/valid (3 warna beda), segment tidak-terpilih di tab Log/Undo terasa
  mengecil dikit saat ditekan (bukan cuma diam).
- **STATUS AUDIT UI v2.21.1: SEMUA 19 temuan (10 P1 + 9 P2) SEKARANG
  TERTUTUP** (v2.22.0 + v2.23.0 ini) -- baik yang benar2 diubah kodenya
  maupun yang diverifikasi TIDAK ada gap nyata (#UI-17). Sesi berikutnya
  TIDAK PERLU audit ulang dari nol berdasar file audit txt yang sama; kalau
  user laporkan gejala baru atau minta audit sektor lain, itu batch baru.

## STATUS PROJECT SEBELUMNYA: v2.22.0 -- Fix 10 temuan P1 audit statis UI v2.21.1 (batch 1/2) -- 2026-08-15
- **User upload** `PromptVault_v2_21_1_UI_Audit.txt` (audit statis eksternal,
  scope UI/UX + Compose logic + accessibility + responsiveness, 10 P1 + 9 P2,
  0 P0) + minta "debugging UI secara bertahap" -- dibaca sebagai: eksekusi
  SEMUA P1 dulu dalam 1 batch (7 file unik, di dalam Batch Limit 10 file),
  P2 (polish/hardening) DISENGAJAKAN ditunda ke batch berikutnya sesuai
  rekomendasi eksplisit audit ("sebaiknya masuk batch berikutnya").
- **7 file P1 diubah** + `MainActivity.kt` (Protected Asset, edit parsial --
  1 titik teruskan parameter baru) + `app/build.gradle.kts` (versi). Detail
  teknis lengkap tiap 10 temuan: lihat CHANGELOG.md v2.22.0.
- **1 bug NYATA ditemukan & diperbaiki SENDIRI sebelum ZIP dikirim** (bukan
  lolos ke user): draf awal batch ini menambahkan
  `import androidx.compose.foundation.layout.weight` di 3 file (`RuleCard.kt`,
  `DiagnosticsScreen.kt`, `OnboardingScreen.kt`) -- `preflight_check.sh`
  kategori #2 menangkapnya. Root cause: `weight()` adalah member extension
  `RowScope`/`ColumnScope` (otomatis tersedia tanpa import di dalam lambda
  `Row{}`/`Column{}`), BUKAN top-level function seperti `size()`/`padding()`
  -- PERSIS kelas kesalahan yang sama dgn insiden `animateItemPlacement`
  v2.3.7 lama (member scope vs top-level, lihat riwayat insiden di bawah).
  Import salah dihapus, `preflight_check.sh` lolos bersih 11/11 setelah itu.
  **Pelajaran ditambahkan eksplisit**: kalau menulis `Modifier.weight(...)`
  (atau method scope lain: `align`, `matchParentSize`, `animateItemPlacement`)
  di dalam lambda scope yang sesuai, JANGAN tambah import top-level untuk
  method itu -- cek dulu apakah itu member scope (biasanya iya utk API
  layout Compose semacam ini) sebelum menambah import.
- `scripts/preflight_check.sh` lolos bersih 11/11 (setelah 1 iterasi fix di
  atas). **BELUM PERNAH lewat `./gradlew` asli.** User WAJIB verifikasi
  visual di HP asli: Home & Onboarding scroll normal (terutama di font
  scale besar), RuleCard 2-baris tidak pecah/terlalu tinggi di device
  sempit, TactileSwitch tetap terasa sama posisi/ukuran visualnya tapi area
  ketuk lebih nyaman, GroupedListRow & baris crash log Diagnostik sekarang
  ada ripple saat ditekan, SkippedFilesScreen menampilkan pesan beda kalau
  belum pernah scan vs sudah scan 0 skipped.
- versionCode 70->71, versionName 2.21.1->2.22.0.
- **Sisa 9 temuan P2** (copy JSON export, status import lebih eksplisit,
  friendlySafFolderLabel, dll) BELUM dieksekusi -- kandidat batch berikutnya,
  lihat daftar lengkap di `PromptVault_v2_21_1_UI_Audit.txt` bagian P2 atau
  CHANGELOG.md v2.22.0.

## STATUS PROJECT SEBELUMNYA: v2.21.1 -- Merge 3-way cabang v2.21.0 (Neumorphism) + v2.20.3 (fix teknis), TANPA regresi -- 2026-08-14
- **User minta merge paket ZIP `v2_21_0` -> `v2_20_3` tanpa regresi.** Root
  cause: 2 paket = 2 cabang independen yang sama-sama lanjut dari v2.20.1
  (versionCode 67) lalu divergen -- `v2_21_0` (code 68) lompat ke redesign
  Neumorphism TANPA lewat v2.20.2/v2.20.3; `v2_20_3` (code 69) punya fix
  teknis (`SCAN_CONCURRENCY` configurable, migrasi legacy DataStore, fix
  import `decodeFromString`) TAPI TIDAK punya redesign Neumorphism.
- **Cara deteksi**: `diff -rq` penuh kedua paket + grep silang token/simbol
  (`LegacyDataMigration`, `scanConcurrency`, `ElevationCard` dkk) sebelum
  memutuskan strategi merge -- BUKAN asumsi "yang lebih baru menang".
- **Strategi**: base = `v2_20_3` (fix teknis terbaru dipertahankan penuh).
  10 file redesign Neumorphism murni (tidak overlap dgn fix teknis manapun,
  diverifikasi via diff) di-copy utuh dari `v2_21_0`: `ui/components/
  {VaultCard,GroupedListRow,TactileSwitch,VaultActionSheet,EmptyState,
  SegmentedControl}.kt`, `ui/components/Neumorphic.kt` (file baru),
  `ui/screens/HomeScreen.kt`, `ui/theme/{Color,TactileTokens}.kt`. 9 file
  yang overlap (berubah di KEDUA cabang) TETAP pakai versi `v2_20_3` krn versi
  `v2_21_0`-nya justru pre-2.20.2 (lebih lama) -- mengambilnya = regresi:
  `PromptVaultApp.kt`, `data/{RuleRepository,SettingsRepository,
  LegacyDataMigration}.kt`, `ui/MainViewModel.kt`, `MainActivity.kt`,
  `util/FileSorter.kt`, `ui/screens/SettingsScreen.kt`,
  `scripts/preflight_check.sh`, `app/build.gradle.kts`.
- **Token lama dihapus di batch Neumorphism** (`TactileTokens.Elevation
  {Card,Cta,CtaPressed,Icon,Thumb}` -> `Neu*`) -- dicek dgn grep bahwa TIDAK
  ADA file lain di luar 10 file Neumorphism yang masih memakai nama token
  lama sebelum merge dieksekusi (nol dangling reference).
- `scripts/preflight_check.sh` dijalankan ulang setelah merge: **11/11
  lolos bersih** (termasuk kategori #11 decodeFromString yang berasal dari
  cabang `v2_20_3`).
- versionCode 69->70, versionName 2.20.3->2.21.1. **BELUM PERNAH lewat
  `./gradlew` asli** -- WAJIB verifikasi CI + smoke test manual (visual
  Neumorphism di semua komponen redesign + migrasi legacy DataStore +
  setting scan concurrency) sebelum rilis produksi. Risiko regresi: RENDAH
  (merge file-level, bukan merge baris-per-baris/line merge -- tiap file
  utuh dari 1 sumber, tidak ada campur logic 2 sumber dalam 1 file), tapi
  BELUM divalidasi compile asli.

## STATUS SEBELUMNYA: v2.20.3 -- FIX bug compile laten: RuleRepository.kt decodeFromString tanpa import -- 2026-08-14
- **User minta "lanjutkan penyempurnaan secara bertahap"** (bukan item spesifik
  disebut namanya). Diinterpretasi sbg: eksekusi item pending yang SUDAH
  tercatat eksplisit di sesi v2.20.2 sbg "Observasi TIDAK dieksekusi, di luar
  scope 2 item ini" -- itu satu-satunya technical debt/bug konkret yang sudah
  teridentifikasi & menunggu, jadi ini pembacaan paling wajar dari instruksi
  "lanjutkan" tanpa menebak-nebak fitur baru yang tidak diminta.
- **Bug**: `RuleRepository.kt` (`rulesFlow` & `importFromJson`) manggil
  `json.decodeFromString<List<Rule>>(...)` tapi import cuma `encodeToString`
  & `Json` -- fungsi generic reified `decodeFromString<T>()` butuh
  `import kotlinx.serialization.decodeFromString` eksplisit terpisah per
  file, TIDAK otomatis ikut dari `import ...json.Json`. Berpotensi
  `Unresolved reference` compiler asli, belum ketahuan krn project ini
  belum pernah lewat `./gradlew` asli.
- **Fix**: tambah 1 baris import. Nol perubahan logika.
- **Preflight diperkuat (kategori #11, baru)**: grep semua pemakaian
  `.decodeFromString<` di seluruh `app/src/main/java`, pastikan tiap file
  yang makai itu juga punya import pasangannya -- supaya KELAS bug ini (bukan
  cuma titik ini) tertangkap otomatis di masa depan, konsisten pola project
  (tiap insiden nambah kategori preflight, lihat riwayat kategori #8/#9/#10).
- File diubah (3): `data/RuleRepository.kt` (1 baris),
  `scripts/preflight_check.sh` (kategori #11 baru), `app/build.gradle.kts`
  (versi). `scripts/preflight_check.sh` lolos bersih 11/11 (kategori baru
  ikut lolos juga). **BELUM PERNAH lewat `./gradlew` asli.** Risiko regresi:
  SANGAT RENDAH -- murni tambah 1 baris import, tidak ada logika/behavior
  yang berubah sama sekali. User TIDAK perlu verifikasi manual khusus di HP
  (tidak ada perubahan UI/behavior), tapi tetap kandidat pertama yang lolos
  kalau CI akhirnya jalan (`./gradlew` asli pertama kali di project ini).
- versionCode 68->69, versionName 2.20.2->2.20.3.

## STATUS PROJECT SEBELUMNYA: v2.20.2 -- Eksekusi 2 technical debt tercatat: SCAN_CONCURRENCY configurable + migrasi best-effort DataStore lama -- 2026-08-13
- **User minta lanjutkan 2 item pending spesifik** (sudah teridentifikasi
  sesi sebelumnya sbg technical debt #3 & #4 di bawah): migrasi DataStore
  lama -> Room, dan `SCAN_CONCURRENCY` configurable. Instruksi eksplisit
  "kerjakan tanpa regresi".
- **`SCAN_CONCURRENCY`**: konstanta hardcode `FileSorter.SCAN_CONCURRENCY`
  DIHAPUS, sekarang `settingsRepository.getScanConcurrency()` (default TETAP
  6). UI baru "Kecepatan Scan (Lanjutan)" di SettingsScreen, pola FilterChip
  identik interval auto-scan. Rentang `[2,4,6,8,12]` dipilih berdasar alasan
  teknis (bukan profiling nyata, tetap belum ada) -- nol regresi utk user
  yang tidak buka setting ini.
- **Migrasi legacy DataStore->Room** (`LegacyDataMigration.kt`, baru,
  dipanggil sekali dari `PromptVaultApp.onCreate()`, fire-and-forget IO):
  **PENTING untuk sesi berikutnya** -- key literal DataStore era pre-v2.2.0
  (`"activity_log_json"`, `"move_history_json"`) adalah INFERENSI dari
  konvensi penamaan project (pola `{noun}_json`, dicontohkan `RuleRepository`
  `"rules_json"`), BUKAN dikonfirmasi dari source asli (sudah terhapus total
  sejak v2.2.0, snapshot ini tanpa git history). Didesain aman-walau-salah:
  key tidak ketemu = no-op murni, BUKAN korup/crash. Guard flag DataStore
  (`legacy_datastore_migration_done`) + try-catch total + `finally` selalu
  set flag `true` (anti retry-loop). Kalau user melaporkan tab Log/Undo
  TETAP kosong padahal yakin pernah pakai versi sangat lama (>v2.2.0), itu
  sinyal key-nya perlu dikoreksi -- HANYA bisa dipastikan lewat sampel data
  nyata dari user tsb (mis. minta backup/export lama kalau masih ada),
  jangan ditebak ulang tanpa data lagi.
- **Observasi (TIDAK dieksekusi, di luar scope 2 item ini)**: `RuleRepository.kt`
  memanggil `json.decodeFromString<List<Rule>>(...)` tapi TIDAK mengimpor
  `kotlinx.serialization.decodeFromString` (cuma impor `encodeToString` &
  `Json`) -- fungsi generic reified itu biasanya perlu impor eksplisit utk
  resolve, berpotensi compile error laten yang belum ketahuan karena project
  ini belum pernah lewat `./gradlew` asli. Trigger valid: build CI gagal di
  titik ini, atau user eksplisit minta dicek/diperbaiki.
- File diubah (8): `SettingsRepository.kt`, `FileSorter.kt`,
  `SettingsScreen.kt`, `MainViewModel.kt`, `MainActivity.kt`,
  `PromptVaultApp.kt`, `LegacyDataMigration.kt` (baru), `app/build.gradle.kts`.
  `scripts/preflight_check.sh` lolos bersih. **BELUM PERNAH lewat
  `./gradlew` asli**. User WAJIB verifikasi: kartu Kecepatan Scan tersimpan
  & scan tetap normal di semua pilihan; tab Log/Undo setelah update (cek
  apakah data lama muncul lagi atau tetap kosong, laporkan balik hasilnya).

## STATUS PROJECT SEBELUMNYA: v2.20.1 -- FIX TECHNICAL DEBT: undo() jalan di Main thread, bukan IO -- 2026-08-13
- **Konteks**: user minta lanjutkan item "pending tercatat" (bukan testing).
  Audit `PROJECT_STATE.md` menemukan 2 kandidat: (a) `FileSorter.undo()`
  kemungkinan jalan di dispatcher Main (dicatat sejak v2.17.0), (b) tombol
  "Simpan" tanpa snackbar konfirmasi (dicatat sejak v2.4.3/audit awal).
- **Item (b) TERNYATA SUDAH TERTUTUP** -- diverifikasi lewat baca kode
  aktual (bukan percaya catatan lama begitu saja, sesuai pelajaran Insiden
  #6): `RuleSaveFeedback` StateFlow + `LaunchedEffect` di `RuleListScreen.kt`
  SUDAH ada & jalan (ditutup di v2.16.0, komentar `MainActivity.kt` baris
  ~294 juga sudah bilang ini). Entri lama baris ~707 PROJECT_STATE.md soal
  gap ini sudah usang/superseded, TIDAK dieksekusi ulang di batch ini.
- **Item (a) DIEKSEKUSI**: `MainViewModel.undoMove()` sekarang
  `withContext(Dispatchers.IO) { fileSorter.undo(entry) }` -- pola identik
  dgn `checkSafAccessLost()` yang sudah ada di file yang sama. Caller
  (`MainActivity.kt` -> `ActivityLogScreen` -> `rememberCoroutineScope()`)
  default `Dispatchers.Main`, jadi tanpa fix ini SEMUA I/O undo (baca/tulis
  file lokal ATAU `DocumentFile`/`ContentResolver` utk jalur SAF) jalan di
  main thread -- berisiko ANR kalau file besar/provider SAF lambat.
- **Kenapa fix di ViewModel, bukan di `FileSorter.undo()`/`undoSaf()`/
  `undoSafDestination()` langsung**: 1 titik pembungkus di caller cukup
  (identik pola `checkSafAccessLost`), tidak perlu ubah 3 fungsi sekaligus
  di `FileSorter.kt` (Batch Limit: 1 modul, minim invasif). `FileSorter.kt`
  TIDAK disentuh sama sekali di batch ini.
- File diubah (2): `ui/MainViewModel.kt` (1 fungsi), `app/build.gradle.kts`
  (versi). `scripts/preflight_check.sh` lolos bersih. **BELUM PERNAH lewat
  `./gradlew` asli**. User WAJIB verifikasi di HP asli: undo dari
  ActivityLogScreen (baik jalur lokal maupun folder kustom SAF kalau
  dipakai) tetap sukses & UI tidak freeze/lag saat undo file besar.
- versionCode 66->67, versionName 2.20.0->2.20.1.

## STATUS PROJECT SEBELUMNYA: v2.20.0 -- REBRAND PALET "Midnight Blue"->"Transformative Teal" + sistem depth/3D ultra immersive -- 2026-08-13
- **Permintaan user**: ganti palet warna lama -> "Transformative Teal
  (Biru-Hijau Gelap)" + tambah efek depth/3D ultra immersive.
- **Atomic Change (9 file)**: `Color.kt` (repalette total, rename
  `MidnightBlue*`->`Teal*`), `Theme.kt` (wiring colorScheme), `TactileTokens.kt`
  (token elevasi baru: ElevationCard/Cta/CtaPressed/Icon/Thumb), `VaultCard.kt`,
  `HomeScreen.kt` (CTA), `GroupedListRow.kt` (icon box), `TactileSwitch.kt`
  (thumb ON), `colors.xml` (retint splash/launcher-bg + rename accent token),
  `app/build.gradle.kts` (versi). `ic_launcher_foreground.xml` SENGAJA
  tidak disentuh (asumsi: "palet lama" = skema UI Compose, bukan artwork
  ikon launcher yang sudah krem sejak v2.14.0 dan itu keputusan terpisah).
- **PELAJARAN PALING PENTING buat sesi berikutnya**: elevasi/shadow NYATA
  di komponen ber-gradient WAJIB pakai pola "solid-base lalu overlay brush
  terpisah" (Surface/`.background()` solid dulu utk shadow, gradient/tint
  ditumpuk sbg Box terpisah di atas) -- JANGAN PERNAH `Modifier.shadow(...)
  .background(brush)` langsung dirantai ke node yang sama. Itu PERSIS
  penyebab regresi nyata v2.14.0 (CTA Home jadi kotak pucat/glitch di
  banyak GPU/skin), yang waktu itu di-fix dengan MELEPAS shadow total
  (v2.14.1). Sekarang shadow dihidupkan lagi tapi dengan pola aman ini di
  4 tempat sekaligus (VaultCard, CTA, icon GroupedListRow, thumb switch).
  Kalau nanti ada laporan "kotak pucat/putih aneh muncul pas [X]", cek
  DULU apakah [X] melanggar pola ini sebelum menduga penyebab lain.
- **GroupedListRow icon shadow**: keputusan v3.0.1 ("no permanent glow per
  icon") DIGANTI sebagian -- sekarang icon box boleh punya shadow NETRAL
  kecil (bukan `spotColor` berwarna) krn user minta depth eksplisit; bab 18
  (glow BERWARNA dilarang) masih dihormati krn shadow ini netral/abu-abu,
  bukan cahaya menyala berwarna.
- File diubah (9, di luar batas normal 10/modul TAPI ini 1 modul visual
  atomik, precedent sama v2.14.0). `scripts/preflight_check.sh` lolos
  bersih. **BELUM PERNAH lewat `./gradlew` asli / device asli**. User WAJIB
  verifikasi visual di HP asli: CTA "Scan Sekarang" idle & ditekan (paling
  berisiko historis), VaultCard & GroupedListRow saat scroll (tidak boleh
  flicker/pucat), kontras teks di atas TealAccent baru.

## STATUS PROJECT SEBELUMNYA: v2.19.3 -- FIX BUG NYATA (laporan user): file/apk bernama diawali "PromptVault" tidak terdeteksi scan -- 2026-08-13
- **User laporkan**: file/apk bernama persis "PromptVault" (atau apa pun yang
  DIAWALI teks itu, mis. "PromptVault.apk") ditaruh di Downloads, tidak
  pernah terdeteksi sebagai kandidat scan walau rule/pattern cocok.
- **Root cause (ditemukan lewat baca `listCandidateFiles()` di FileSorter.kt)**:
  pengecualian folder output app sendiri pakai `f.absolutePath.startsWith(
  vaultRootDir.absolutePath)` -- STRING-PREFIX match, bukan path-containment.
  `vaultRootDir.absolutePath` = ".../Downloads/PromptVault" TANPA separator
  akhir, jadi path SIBLING file seperti ".../Downloads/PromptVault.apk" juga
  `startsWith(...)` true (nama file "PromptVault.apk" diawali teks
  "PromptVault") walau file itu BUKAN isi folder "PromptVault", cuma
  kebetulan nama depannya sama -- ikut ter-exclude tanpa alasan valid. Bug
  kelas sama ditemukan juga di `cleanupGhostMediaStoreEntries()` (query SQL
  `LIKE '<path>%'`, prefix-match identik).
- **Fix**: tambah `File.separator` di akhir prefix pembanding di KEDUA
  tempat, supaya hanya path yang benar-benar path-DI-DALAM folder
  "PromptVault/" yang cocok, bukan sekadar string yang diawali sama.
- **Pelajaran dicatat**: `String.startsWith(otherPath)` untuk cek
  "apakah path A ada di dalam folder B" SELALU rawan false-positive kalau
  tidak diberi separator akhir eksplisit -- nama file/folder sibling yang
  kebetulan jadi prefix nama folder lain akan ikut ke-match. Cek pola serupa
  (`startsWith` dipakai buat containment path) kalau ada laporan gejala
  "file X tidak terdeteksi" lagi di masa depan.
- File diubah (1 modul): `util/FileSorter.kt` (2 titik fix + doc),
  `app/build.gradle.kts` (versi). `scripts/preflight_check.sh` lolos bersih.
  **BELUM PERNAH lewat `./gradlew` asli**. User DIMINTA konfirmasi di HP
  asli: taruh file/apk bernama diawali "PromptVault" di Downloads, scan,
  pastikan sekarang terdeteksi & terpindah normal sesuai rule.

## STATUS PROJECT SEBELUMNYA: v2.19.2 -- FIX BUG NYATA (laporan user + screenshot): folder "PromptVault" terduplikat (1)/(2)/(3) di tujuan SAF -- 2026-08-13
- User laporkan screenshot file manager: 4 folder di folder tujuan kustom --
  "PromptVault", "PromptVault (1)", "PromptVault (2)", "PromptVault (3)",
  MASING-MASING isi "1 item", tanggal sama. Folder kustom yang dipilih malah
  "ditimpa" (secara efektif: hasil sortir tersebar ke banyak folder
  duplikat alih-alih satu folder "PromptVault" konsisten).
- **Root cause (ditemukan lewat baca ulang [FileSorter.moveFileToSafDestination]
  + [findOrCreateChildDirSaf] setelah audit v2.19.1 SEBELUMNYA MELEWATKAN ini)**:
  `findOrCreateChildDirSaf(destinationRoot, "PromptVault")` dipanggil TERPISAH
  PER-FILE, DI DALAM tiap coroutine paralel (`scanAndSortToDestination` proses
  file kandidat lewat `async` + `Semaphore(SCAN_CONCURRENCY=6)`, arsitektur
  sejak v2.4.0 -- lihat Keputusan Arsitektur #6). `DocumentFile.
  createDirectory()` TIDAK atomik/idempoten seperti `File.mkdirs()` -- kalau
  2+ coroutine memanggil `parent.findFile("PromptVault")` SEBELUM salah satu
  sempat selesai `createDirectory()`, KEDUANYA melihat "belum ada" lalu
  KEDUANYA createDirectory() -> provider TIDAK menolak, malah auto-suffix
  nama biar unik -> hasil PERSIS gejala di screenshot: N folder terpisah,
  masing-masing cuma kebagian file dari coroutine yang menciptakannya
  duluan. Classic TOCTOU race -- `scanMutex` yang sudah ada di [scanAndSort]
  TIDAK mencegah ini (mutex itu cuma menyerialkan ANTAR scan, bukan antar
  file DALAM satu scan yang sengaja diparalelkan).
- **Kenapa lolos audit v2.19.1 sebelumnya**: audit sesi itu baca kode
  `moveFileToSafDestination` baris-per-baris tapi fokus ke *korektnes logika
  per-file* (conflict strategy, verifikasi nama pasca-create, dst) -- TIDAK
  mempertimbangkan bahwa fungsi ini dipanggil PARALEL dari `async{}` di
  caller-nya. Pelajaran: audit SAF ke depan WAJIB eksplisit cek "apakah
  fungsi ini bisa dipanggil concurrent, dan kalau ya, adakah shared-state
  I/O (termasuk pembuatan folder/file baru) yang TOCTOU-race?" -- bukan cuma
  benar secara sekuensial/single-thread.
- **Fix STRUKTURAL (bukan tambal Mutex di titik race)**: folder tujuan SAF
  (root "PromptVault" + subfolder tiap rule aktif) sekarang di-resolve SEKALI,
  SERIAL, di fungsi baru `resolveSafRuleDestinations()` -- dipanggil SEBELUM
  `async{}` mana pun dimulai di `scanAndSortToDestination`. Hasil (`Map<nama
  folder rule, DocumentFile?>`) dibagikan ke semua coroutine paralel sebagai
  data BACA-SAJA. `moveFileToSafDestination()` tidak lagi menerima
  `destinationRoot` mentah dan resolve sendiri -- sekarang menerima `destDir`
  yang SUDAH jadi. `processCandidate()` dapat parameter baru
  `safRuleDestinations`, skip file dengan pesan jelas kalau resolusi folder
  untuk rule terkait gagal (dicek sekali di awal, bukan berulang per file).
- **Efek pada folder duplikat yang SUDAH terlanjur ada** (dari sebelum fix
  ini): TIDAK dibereskan otomatis oleh app -- app tidak (dan sengaja tidak)
  menghapus/menggabung folder yang sudah ada di penyimpanan user tanpa izin
  eksplisit. User perlu gabung manual isi folder "PromptVault (1)/(2)/(3)"
  ke "PromptVault" lewat file manager kalau mau rapi, ATAU biarkan (scan
  berikutnya otomatis konsisten pakai SATU folder "PromptVault" saja berkat
  fix ini, tidak menciptakan folder baru lagi).
- File diubah (2): `util/FileSorter.kt` (fix + fungsi baru
  `resolveSafRuleDestinations`), `app/build.gradle.kts` (versi).
  `scripts/preflight_check.sh` lolos bersih. **BELUM PERNAH lewat `./gradlew`
  asli** -- konsisten seluruh riwayat SAF (Insiden #7 syarat (a) masih belum
  terpenuhi di sandbox ini). User DIMINTA konfirmasi di HP asli: scan lagi ke
  folder kustom yang SAMA, pastikan HANYA "PromptVault" (tanpa akhiran angka)
  yang bertambah isi, tidak ada folder "(4)" baru muncul.

## STATUS PROJECT SEBELUMNYA: v2.19.1 -- DEBUG+POLISH SAF: OVERWRITE tidak lagi asumsi delete() SAF berhasil -- 2026-08-13
- User minta "debugging+polish feature SAF" (audit umum, bukan laporan gejala
  spesifik). Audit manual baris-per-baris SELURUH kode SAF (FileSorter.kt penuh
  + SettingsRepository/MainViewModel/MainActivity/SettingsScreen bagian SAF)
  dilakukan -- BUKAN cuma grep dead-code seperti sesi v2.19.0. Hasil: arsitektur
  v2.19.0 (folder kustom = tujuan, bukan sumber scan) TERVERIFIKASI konsisten
  di semua titik wiring (dispatcher `undo()`, `SafDestinationResolution` 3-state,
  `checkSafAccessLost` reaktif, `@OptIn(ExperimentalLayoutApi::class)` untuk
  `FlowRow` di `SettingsScreen` SUDAH benar sejak awal -- bukan bug baru).
  **1 bug nyata ditemukan** (bukan cuma kosmetik):
- **Bug**: `moveFileToSafDestination()` -- `ConflictStrategy.OVERWRITE` manggil
  `existingAtTarget.delete()` TANPA verifikasi hasil, lalu `createFile()`
  lanjut dengan nama yang sama seolah delete pasti sukses. Provider SAF
  (beda dari `java.io.File` biasa) TERKENAL tidak reliable di riwayat project
  ini (lihat Insiden #4/#6/#7) -- kalau `delete()` diam-diam gagal, provider
  sering auto-suffix nama file baru ("target (1).ext") alih-alih menimpa:
  user pikir sudah "Overwrite", padahal file lama MASIH ADA + file baru
  bernama beda dari yang diminta rule.
- **Fix**: cek hasil `delete()` eksplisit -- gagal -> log ERROR + `MoveOutcome.
  FAILED` (bukan lanjut diam-diam dengan asumsi sukses). Konsisten dengan pola
  "jangan percaya boolean provider begitu saja" yang sudah jadi PELAJARAN
  PERMANEN project ini sejak Insiden #6.
- **SENGAJA TIDAK disentuh**: `moveFile()` (jalur lokal java.io.File) punya
  gap yang SAMA PERSIS (`destFile.delete()` juga tidak diverifikasi) -- TIDAK
  difix di batch ini karena scope eksplisit user adalah "feature SAF", dan
  risiko delete() gagal jauh lebih rendah di filesystem lokal milik app
  sendiri drpd provider SAF pihak ketiga/OEM. Dicatat sebagai kandidat batch
  terpisah kalau user minta audit jalur lokal juga.
- 2 file diubah: `util/FileSorter.kt` (fix), `app/build.gradle.kts` (versi).
  `scripts/preflight_check.sh` lolos bersih. **BELUM PERNAH lewat `./gradlew`
  asli** (konsisten dengan seluruh riwayat SAF -- Insiden #7 syarat (a) masih
  belum terpenuhi di sandbox ini).

## STATUS PROJECT SEBELUMNYA: v2.19.0 -- SAF DIRESTRUKTURISASI: TUJUAN, BUKAN SUMBER SCAN -- 2026-08-13
- User upload `SAF_FINAL_VERDICT_FIX.txt` (spec/verdict eksternal): ROOT CAUSE
  seluruh siklus SAF v2.17.0-v2.18.1 adalah **salah menafsirkan requirement**,
  bukan sekadar bug API. SAF yang benar = mekanisme akses ke folder TUJUAN
  kustom yang dipilih user, BUKAN sumber scan alternatif. Instruksi user:
  "folder scan file untuk dipindahkan tetap hardcode 'download'" -- menegaskan
  sumber scan harus SELALU Downloads, sesuai spec.
- **Restrukturisasi (bukan tambal)**: `scanAndSort()` sekarang SATU sumber
  scan ([listCandidateFiles], Downloads, tidak berubah dari awal project) +
  cabang TUJUAN tunggal (Downloads/PromptVault/ lokal ATAU folder kustom SAF/
  PromptVault/ lewat DocumentFile). Dihapus total: `scanAndSortSafLocked()`,
  `listCandidateFilesSaf()`, `processCandidateSaf()`, `isLikelyStillWritingSaf()`
  -- semua sisa arsitektur "SAF sebagai scanner" yang salah. `SafRootResolution`
  -> `SafDestinationResolution`, `resolveSafRoot()` -> `resolveSafDestinationRoot()`
  (rename, bukan cuma kosmetik -- linimasa insiden ini AKAR masalahnya adalah
  penamaan/konsep yang ambigu). `moveFileSaf()` -> `moveFileToSafDestination()`
  (sumber jadi `File` lokal, bukan lagi `DocumentFile`).
- **Efek samping positif**: `previewPatternMatches()` (sumber bug v2.18.1)
  disederhanakan total -- tidak ada lagi cabang SAF sama sekali, karena sumber
  scan sekarang SELALU satu-satunya (Downloads). Kelas bug "preview vs scan
  lihat folder beda" jadi STRUKTURAL tidak mungkin terulang, bukan cuma
  ditambal ulang.
- **Kompatibilitas mundur riwayat undo**: entri `MoveHistoryEntry` LAMA (dibuat
  sebelum restrukturisasi ini, format sumber+tujuan sama-sama `content://`)
  TETAP bisa di-undo lewat `undoSaf()` (logika lama, tidak diubah). Entri BARU
  (tujuan `content://`, sumber path lokal) lewat `undoSafDestination()` baru.
  Dispatcher `undo()` membedakan lewat `originalParentUri`, bukan skema DB baru.
- UI `SettingsScreen.kt` ("Folder Kustom" -> "Folder Tujuan Kustom") & doc
  comment `SettingsRepository`/`MainViewModel` diperbaiki -- sebelumnya
  eksplisit menyebut "pindai folder pilihanmu sendiri" (bahasa SUMBER),
  sekarang "file tetap dipindai dari Downloads, folder ini cuma tujuan".
  `MainActivity.kt` (picker wiring) TIDAK disentuh -- murni pilih-URI, tidak
  peduli peran sumber/tujuan.
- File diubah (5): `util/FileSorter.kt` (restrukturisasi inti),
  `ui/screens/SettingsScreen.kt`, `data/SettingsRepository.kt` (doc),
  `ui/MainViewModel.kt` (doc), `app/build.gradle.kts` (versi).
- `scripts/preflight_check.sh` lolos bersih (2 iterasi -- iterasi 1 masih
  menyisakan import `CancellationException` tak terpakai setelah
  `processCandidateSaf` dihapus, dibersihkan di iterasi 2). **BELUM PERNAH
  lewat `./gradlew` asli** -- CI run berikutnya WAJIB dicek, konsisten dengan
  seluruh riwayat SAF sebelumnya (Insiden #7 syarat (c): blind tapi disiplin).
- **Pelajaran proses dicatat**: ini SIKLUS KEDUA "misinterpretasi requirement"
  untuk SAF di project ini (yang pertama: Insiden #7 lama, arsitektur "Zip
  Sorter" independen). Kalau SAF diminta lagi di masa depan dan sesi itu ragu
  soal peran (sumber vs tujuan vs lainnya), TANYA eksplisit ke user SEBELUM
  nulis kode -- jangan asumsikan dari nama fitur ("SAF") saja.

## STATUS PROJECT SEBELUMNYA: v2.18.1 -- FIX BUG NYATA: PREVIEW vs SCAN LIHAT FOLDER BEDA -- 2026-08-13
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
  3. **[DIEKSEKUSI di v2.20.2, 2026-08-13]** Data lama di DataStore
     (ActivityLog/MoveHistory) dari sebelum migrasi Room v2.2.0 -- sebelumnya
     "disepakati tidak urgent" (lihat "Keputusan arsitektur utama" #1) dan
     TIDAK dieksekusi di batch v2.16.0 ini karena alasan (a)/(b) di bawah
     masih berlaku saat itu. Dieksekusi ulang di v2.20.2 atas instruksi
     eksplisit user "kerjakan tanpa regresi" -- BUKAN karena alasan (b) sudah
     terselesaikan (key literal DataStore lama TETAP tidak terverifikasi,
     tidak ada git history di snapshot ini), tapi karena didesain aman-walau-
     tebakan-salah (no-op murni kalau key tidak cocok, bukan migrasi
     destruktif). Lihat `LegacyDataMigration.kt` + CHANGELOG v2.20.2 untuk
     detail lengkap & peringatan soal ketidakpastian nama key.
     ~~(a) app belum pernah rilis publik luas...~~
     ~~(b) skema DataStore key lama kemungkinan sudah berubah bentuk...~~
     ~~Kalau user based di masa depan benar melaporkan data hilang...~~
  4. **[DIEKSEKUSI di v2.20.2, 2026-08-13]** `SCAN_CONCURRENCY = 6`
     (FileSorter, v2.4.0) sekarang configurable dari kartu "Kecepatan Scan
     (Lanjutan)" di Pengaturan (`SettingsRepository.ALLOWED_SCAN_CONCURRENCY`
     = `[2,4,6,8,12]`, default TETAP 6). Dieksekusi atas instruksi eksplisit
     user, BUKAN karena data profiling nyata akhirnya tersedia (memang belum
     ada, tidak diklaim ada) -- yang berubah cuma "tidak configurable" jadi
     "configurable", jadi kalau nanti trigger asli (user laporkan scan
     lambat) benar terjadi, user bisa coba sendiri tanpa nunggu rilis baru.
     ~~TIDAK diubah sekarang karena tidak ada data profiling...~~
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

### [2026-08-15] Insiden #8 -- NeumorphicSurface: `modifier` (termasuk `weight()`) dipasang di lapisan Compose yang salah
- **Gejala (laporan user + 2 screenshot HP asli v2.24.1)**: layar "Riwayat
  Aktivitas" -- pil "Log" melebar SELEBAR LAYAR, tab "Undo Pemindahan"
  lenyap total, + kotak hijau gelap kosong raksasa di bawah pil sebelum
  daftar log muncul.
- **Root cause**: `NeumorphicSurface` (`ui/components/Neumorphic.kt`,
  primitif tunggal Neumorphism sejak v5.0.0/v2.21.0) menerima parameter
  `modifier: Modifier` dari pemanggil, tapi memasangnya ke `Surface` KONTEN
  yang posisinya BEBERAPA LAPIS `Box` DI DALAM -- BUKAN ke `Box` TERLUAR
  yang benar-benar jadi elemen anak-langsung dari composable pemanggil
  (mis. `Row` di `SegmentedControl.kt`). Ini "bekerja" (tidak kelihatan
  salah) untuk modifier UKURAN biasa (`size()`, `fillMaxWidth()`,
  `padding()`, `offset()`, `scale()`) karena `Box` tanpa modifier tetap
  bisa wrap-content mengikuti ukuran `Surface` di dalamnya. TAPI FATAL
  untuk `ParentDataModifier` seperti `RowScope.weight()`/`ColumnScope
  .weight()` -- parent data itu HANYA dibaca `Row`/`Column` dari modifier
  chain milik ANAK LANGSUNG-nya (di sini: `Box` terluar `NeumorphicSurface`),
  BUKAN dari node manapun yang lebih dalam. `SegmentedControl.kt` memanggil
  `NeumorphicSurface(modifier = Modifier.weight(1f), ...)` untuk segment
  YANG SEDANG TERPILIH -- `weight()` itu nyasar ke `Surface` dalam,
  `Row` menganggap child ini TIDAK berbobot sama sekali, lalu `Text
  (Modifier.fillMaxWidth())` di dalamnya mewarisi lebar penuh Row yang
  tersedia (karena child "tak berbobot" diukur dgn constraint longgar) --
  menyisakan NOL ruang untuk sibling segment lain yang weight-nya justru
  valid. Match PERSIS gejala di screenshot.
- **Kenapa lolos audit statis sebelumnya**: `preflight_check.sh` kategori
  #2 mengecek IMPORT member-scope yang salah (pola insiden lama
  `animateItemPlacement`/v2.3.7 & `weight` import top-level/v2.22.0) --
  BUKAN mengecek APAKAH `weight()` yang sudah correct-diimport dipasang di
  LAPISAN Compose yang tepat saat dioper lewat parameter `modifier` sebuah
  wrapper composable. Ini kelas kesalahan BERBEDA (semantik layout, bukan
  resolusi import) yang preflight statis TIDAK dirancang menangkapnya --
  hanya kelihatan lewat reasoning manual tentang bagaimana Row membaca
  parent data, dipicu justru oleh gejala visual di screenshot user.
- **Fix**: `modifier` parameter sekarang dipasang LANGSUNG di `Box`
  terluar `NeumorphicSurface` (root komposabel -- sesuai konvensi resmi
  Compose "selalu pasang parameter modifier di elemen root"). `Surface`
  konten SENGAJA TETAP BUKAN `matchParentSize()` (beda perlakuan dari 2
  shadow-caster Box yang MEMANG `matchParentSize()`) -- ia anak `Box`
  biasa yang mewarisi constraints yang sama dari induknya, supaya kasus
  umum (`modifier` cuma mengunci LEBAR, mis. `fillMaxWidth()`/`weight()`,
  TANPA tinggi eksplisit) `Box` tetap bisa wrap-content TINGGI mengikuti
  konten `Surface` asli -- PERSIS perilaku lama untuk 6 pemanggil lain
  (`VaultCard`, `GroupedListRow`, `TactileSwitch` x2, `EmptyState`,
  `VaultActionSheet`, CTA `HomeScreen`), semua sudah pakai modifier ukuran
  biasa, bukan `weight()` -- diverifikasi manual satu-satu (bukan asumsi)
  sebelum ZIP dipaket, 0 perubahan visual utk mereka.
- **PELAJARAN PERMANEN untuk sesi Claude berikutnya**: composable wrapper
  APA PUN yang menerima parameter `modifier: Modifier` WAJIB memasang
  modifier itu di elemen ROOT/terluar yang composable itu kembalikan --
  BUKAN di elemen anak yang lebih dalam -- meskipun secara visual "kelihatan
  benar" untuk modifier ukuran biasa (fillMaxWidth/size/padding/dst).
  Modifier `ParentDataModifier` (`weight()` di Row/Column, & sejenisnya)
  HANYA berfungsi kalau nempel di anak LANGSUNG dari Row/Column/layout
  scope terkait -- kalau ada composable wrapper lain di codebase ini yang
  ditambahkan ke depan dan menerima `modifier` dari pemanggil, cek dulu
  modifier itu benar-benar jatuh di root, terutama kalau composable itu
  nanti dipakai di dalam `Row`/`Column` dengan `weight()`.
- **BELUM dikonfirmasi user** -- tunggu screenshot/laporan HP asli
  setelah update berikutnya.

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

### [2026-08-07] v2.9.1 -- Viewer crash log ditambah di Diagnostik
- Lanjutan v2.9.0. `DiagnosticsScreen` sekarang punya card "Crash Log" (list
  + tap-to-view AlertDialog). Menutup gap "belum ada UI viewer" yang dicatat
  di entri sebelumnya.
- CI belum dikonfirmasi hijau untuk versi ini.

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
