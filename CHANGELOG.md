# Changelog PromptVault

Semua versi dan alasan perubahannya, biar sesi Claude berikutnya (atau kamu)
punya konteks penuh tanpa perlu scroll chat lama.

## v2.18.1 -- Fix bug NYATA: preview Rule cek Downloads, scan cek folder kustom (2026-08-13)
User klarifikasi laporan sebelumnya: "saat bikin rule semua file yang cocok
MUNCUL di preview, tapi saat discan malah 'tidak ada file cocok'." Ini BUKAN
salah paham user, BUKAN juga soal ekstensi (sudah dibenerin di v2.18.0) --
ini **bug nyata terpisah**, baru ketahuan lewat testing asli user (tidak
kena di audit eksternal maupun review kode sebelumnya).

**Root cause**: `FileSorter.previewPatternMatches()` (dipanggil layar
Tambah/Edit Rule, live tiap 400ms debounce ketik pattern) HARDCODE selalu
cek `downloadsDir` (java.io.File biasa) -- SAMA SEKALI TIDAK PEDULI folder
kustom SAF sudah dipilih user atau belum. Sementara `scanAndSort()` yang
sesungguhnya SUDAH BENAR (sejak fix P0 audit SAF v2.17.1) mengarah ke folder
kustom kalau dikonfigurasi. Akibatnya: preview & scan mengecek DUA folder
BERBEDA TOTAL -- preview "cocok" (dari isi Downloads, mungkin file lama/tak
relevan yang kebetulan cocok pattern luas), scan asli "tidak ada" (dari
folder kustom yang sungguhan berisi file user, tapi preview tidak pernah
melihatnya).

**Fix**: `previewPatternMatches()` diubah jadi `suspend fun`, reuse
`resolveSafRoot()` PERSIS SAMA seperti `scanAndSort()` -- satu logika
pemilihan sumber untuk preview & scan sungguhan, supaya kelas bug ini tidak
bisa terulang lewat cabang logika kedua yang independen. Folder
"PromptVault" di jalur SAF preview dicari lewat `findFile()` (baca-saja,
TIDAK dibuat) -- preview tidak boleh bikin folder muncul sebagai efek
samping ngetik pattern sebelum rule disimpan. `listCandidateFilesSaf()`
parameter `vaultRootDoc` jadi nullable untuk mendukung ini (scan
sungguhan tetap pakai versi yang sudah dibuat lewat
`findOrCreateChildDirSaf`, tidak berubah perilakunya).

**Rantai perubahan signature** (suspend menjalar ke atas, WAJIB semua
diubah bersamaan supaya kompil): `FileSorter.previewPatternMatches` ->
`MainViewModel.previewPattern` -> parameter `onPreviewPattern` di
`AddEditRuleScreen` (tipe lambda `suspend (String,String)->...`).
Pemanggilnya sudah di dalam `LaunchedEffect` (coroutine), jadi tidak perlu
ubah titik panggil di UI.

File diubah (3, 1 modul "preview/scan parity"): `util/FileSorter.kt`,
`ui/MainViewModel.kt`, `ui/screens/AddEditRuleScreen.kt`.
`scripts/preflight_check.sh` lolos bersih. **BELUM PERNAH lewat `./gradlew`
asli** -- CI run berikutnya WAJIB dicek.

## v2.18.0 -- Dukung SEMUA ekstensi file, bukan cuma ZIP/TXT (2026-08-13)
User laporan "sudah pilih folder custom untuk pindahkan file project, tapi
selalu 'tidak ada file cocok'" -- root cause: scanner HARDCODE hanya terima
`.zip`/`.txt` sejak awal project, sementara file project user "campuran"
(bukan zip/txt). Dikonfirmasi ke user: mau app diperluas dukung SEMUA
ekstensi (bukan whitelist tertentu) -- dieksekusi sebagai 1 batch atomic.

**Perubahan inti** (`util/FileSorter.kt`): `listCandidateFiles()` (Downloads)
dan `listCandidateFilesSaf()` (folder kustom) SEBELUMNYA filter ekstensi
`.zip`/`.txt` duluan sebelum pattern rule sempat dicek sama sekali -- filter
itu DIHAPUS TOTAL. Sekarang [GlobMatcher] (pattern glob per Rule, mis.
`*.kt`, `*` untuk semua) SATU-SATUNYA penentu file mana yang cocok. Guard
lain (file sementara/partial, exclude folder `PromptVault` sendiri,
non-rekursif top-level-only) TIDAK berubah -- tidak terkait ekstensi.

**`mimeTypeForFileName()` diperluas**: dari 2 entri (zip/txt) jadi ~15 tipe
umum (pdf, jpg/png/gif/webp, mp4, mp3, json, xml, md, csv, apk, doc/docx,
kt/java/gradle/kts/py/js/html/css -> text/plain). `application/octet-stream`
TETAP fallback untuk ekstensi apa pun di luar tabel -- SAF `createFile()`
tetap sukses untuk ekstensi manapun, tabel ini cuma soal fidelity MIME,
BUKAN syarat "ekstensi itu didukung". Test `MimeTypeForFileNameTest`
diupdate (assertion lama `.pdf -> octet-stream` sudah tidak berlaku,
diganti ekstensi genuinely-tidak-terdaftar `.xyz123` + test baru untuk tipe
yang baru ditambah).

**String UI** ("ZIP/TXT", "ZIP & TXT") digenerickan di 5 layar/file supaya
tidak lagi menyesatkan: `HomeScreen.kt`, `OnboardingScreen.kt`,
`DiagnosticsScreen.kt`, `AddEditRuleScreen.kt`, `MainActivity.kt` (teks izin
storage), plus 2 pesan log di `FileSorter.kt`.

**Batasan yang SENGAJA tidak diubah** (di luar scope user, dicatat supaya
tidak dianggap lupa): scan tetap non-rekursif (cuma level teratas folder,
sama seperti sebelumnya) -- kalau file project ada di sub-folder, tetap
tidak ke-scan. Kalau ini juga perlu, itu batch terpisah (perubahan lebih
besar: rekursi + risiko match folder itu sendiri).

File diubah (9, 1 modul "matching engine"): `util/FileSorter.kt`,
`ui/MainViewModel.kt`, `ui/screens/HomeScreen.kt`,
`ui/screens/OnboardingScreen.kt`, `ui/screens/DiagnosticsScreen.kt`,
`ui/screens/AddEditRuleScreen.kt`, `MainActivity.kt`,
`test/.../MimeTypeForFileNameTest.kt`. `scripts/preflight_check.sh` lolos
bersih. **BELUM PERNAH lewat `./gradlew` asli** -- CI run berikutnya WAJIB
dicek (pola sama seperti v2.17.0/v2.17.1).

## v2.17.1 -- Fix 2 bug P0 fatal SAF (audit eksternal) (2026-08-13)
User upload `SAF_FINAL_LOGIC_AUDIT.md` (audit eksternal atas SAF v2.17.0):
2 P0 fatal + 6 P1 + 3 P2. Batch ini HANYA eksekusi 2 P0 (atomic change, scope
sengaja dipersempit -- P1/P2 belum dikerjakan, lihat PROJECT_STATE.md).

**P0 #1 -- "validasi permission saat startup" belum ada**: akuisisi
`takePersistableUriPermission` di `MainViewModel.setSafTreeUri()` sendiri
sudah benar (dicek ulang, tidak ada bug di situ), tapi TIDAK ADA validasi
proaktif -- akses hilang (dicabut dari luar app, folder dihapus/dipindah)
baru ketahuan diam-diam saat scan berikutnya gagal. Fix: `FileSorter`
expose `checkSafAccessLost(): Boolean` (I/O check tanpa scan), dipanggil
`MainViewModel` reaktif setiap kali `safTreeUri` berubah (StateFlow replay
nilai terakhir ke collector baru = otomatis mencakup startup, tanpa init
block terpisah yang gampang lupa dipanggil). `SettingsScreen` tampilkan
warning merah langsung di kartu "Folder Kustom" kalau `safAccessLost=true`.

**P0 #2 -- silent fallback ke Downloads saat SAF rusak**: `resolveSafRoot()`
lama collapse "belum diset" DAN "sudah diset tapi rusak/izin dicabut" jadi
`DocumentFile?` yang sama-sama `null` -> scan fallback diam-diam ke
Downloads di KEDUA kasus. User bisa mengira file masuk folder kustom
padahal sebenarnya masuk Downloads (atau sebaliknya, salah kira gagal
padahal cuma pindah jalur). Fix: `resolveSafRoot()` return sealed class
`SafRootResolution` (`Active` / `NotConfigured` / `AccessLost(reason)`).
`scanAndSort()` HANYA fallback ke Downloads kalau `NotConfigured`;
`AccessLost` menghentikan scan + log `LogLevel.ERROR` eksplisit +
`ScanResult.safAccessLost=true` (field baru, default `false` -- tidak
mengubah signature caller lain seperti `AutoSortWorker`). `MainViewModel.
runManualScan()` prioritaskan pesan `safAccessLost` di atas pesan Downloads
generik.

File diubah (4, 1 modul "SAF"): `util/FileSorter.kt`, `ui/MainViewModel.kt`,
`ui/screens/SettingsScreen.kt`, `MainActivity.kt` (thread param baru).
`scripts/preflight_check.sh` lolos bersih. **BELUM PERNAH lewat `./gradlew`
asli** -- sama seperti v2.17.0, CI run pertama WAJIB dicek.

Sisa scope audit (6 P1 + 3 P2, BELUM dieksekusi, kandidat batch terpisah):
P1 -- race re-entrancy folder ganti saat scan jalan, tidak ada retry/backoff
saat provider transient error, `DocumentFile.findFile()` linear-scan tiap
file (potensi lambat di folder besar), copy-then-verify tidak cek ukuran
byte-exact, tidak ada UI loading state saat validasi SAF jalan, nama file
hasil `createFile()` SAF tidak divalidasi ulang match rule (celah rename
provider). P2 -- 3 item kosmetik/minor, lihat `SAF_FINAL_LOGIC_AUDIT.md`
asli (tidak disalin ke repo, hanya referensi upload user) untuk detail.

## v2.17.0 -- SAF ditulis ulang: Folder Kustom (2026-08-12)
User minta fitur SAF ditambahkan lagi, eksplisit "penuh dedikasi bukan asal
jadi". SESUAI PROSEDUR yang didokumentasikan sendiri di `PROJECT_STATE.md`
("Insiden #7"): dibaca dulu SELURUH riwayat SAF sebelum menulis kode apa pun,
lalu dikonfirmasi ke user karena sandbox sesi ini TIDAK punya akses
Gradle/emulator/device asli (syarat "a" gugur). User pilih lanjut. Dieksekusi
di bawah **syarat (c)**: blind (tanpa compiler asli), TAPI disiplin -- reuse
persis path/arsitektur legacy (v2.7.0-v2.12.0, dari catatan penghapusan
v2.13.0), BUKAN modul independen baru seperti "Zip Sorter" dulu (scope itu
SENGAJA tidak diulang batch ini), dan SETIAP bug dari Insiden #4/#6/#7
diaudit satu-satu supaya tidak terulang dengan cara yang sama:

- **Bug CI v2.8.0** (fungsi SAF suspend polos manggil `async{}` tanpa
  CoroutineScope receiver): dihindari dari desain awal --
  `scanAndSortSafLocked()` punya `withContext(Dispatchers.IO){}` MILIKNYA
  SENDIRI, struktur 1:1 sama dengan `scanAndSortLocked()` yang sudah terbukti
  kompil, dipanggil dari `scanAndSort()` yang jadi TITIK CABANG TUNGGAL
  (SAF vs Downloads) -- signature tidak berubah, jadi `AutoSortWorker.kt`
  otomatis dapat dukungan SAF tanpa disentuh sama sekali.
- **Bug #2 v2.10.0** (mime type dipercaya dari provider sumber): mime type
  SEKARANG SELALU dari ekstensi nama file (`mimeTypeForFileName()`, fungsi
  murni top-level supaya unit-testable TANPA Context Android -- lihat
  `MimeTypeForFileNameTest.kt`, SATU-SATUNYA bagian fitur ini yang benar-benar
  tereksekusi, bukan cuma dibaca, sebelum sampai ke CI asli). Nama aktual
  pasca-`createFile()` diverifikasi, WARNING dicatat kalau provider ubah nama.
- **Bug #1 v2.10.0** (`releasePersistableUriPermission()` didokumentasikan
  tapi tidak pernah dipanggil): sekarang BENAR-BENAR dipanggil dari
  `MainViewModel.setSafTreeUri()` (lepas folder lama SETELAH folder baru
  sukses tersimpan, urutan sengaja) & `clearSafTreeUri()`.
- **Insiden #4/#6** (boolean DocumentFile jadi trust-gate tanpa sinyal
  nyata): Dual Stability Guard versi SAF SENGAJA cuma 2/3 sinyal
  (`lastModified()` + kestabilan ukuran, TANPA file-lock check -- tidak ada
  API konsisten untuk itu di `content://` lintas provider/OEM), dicatat
  eksplisit sebagai known limitation, BUKAN diklaim setara versi Downloads.
- **DocumentsContract.moveDocument() SENGAJA TIDAK dipakai** (dukungan tidak
  konsisten antar provider/OEM, alasan sama seperti poin di atas) -- pakai
  copy-lalu-delete lewat ContentResolver, konsisten dgn `copyThenDelete` lama.
- **DB Schema/DAO (protected asset) TIDAK disentuh sama sekali** -- URI SAF
  disimpan di kolom `String` polos yang sudah ada (`originalParentUri`/
  `destUri` di `MoveHistoryEntry`), dibedakan dari path File biasa lewat
  prefix `"content://"` saat undo. Nol migrasi Room.
- **Ditemukan & diperbaiki SELAMA batch ini** (bukan bug lama): draf pertama
  `processCandidateSaf()` membungkus SELURUH badan (termasuk `delay(1 detik)`
  di `isLikelyStillWritingSaf`) dalam `catch (e: Exception)` polos -- akan
  menelan `CancellationException` kalau scan dibatalkan tepat di jendela itu.
  Diperbaiki: `catch (e: CancellationException) { throw e }` eksplisit
  sebelum `catch (e: Exception)`.

File diubah/ditambah:
- `util/FileSorter.kt`: `mimeTypeForFileName()` (top-level baru),
  `isTempOrPartialName()` (diekstrak dari `isTempOrPartialFile()`, direuse),
  `resolveSafRoot()`, `scanAndSortSafLocked()`, `listCandidateFilesSaf()`,
  `isLikelyStillWritingSaf()`, `findOrCreateChildDirSaf()`,
  `copyDocumentBytes()`, `processCandidateSaf()`, `moveFileSaf()`,
  `undoSaf()` (baru semua); `scanAndSort()`/`undo()` (cabang baru, minimal).
- `data/SettingsRepository.kt`: `safTreeUriFlow`/`getSafTreeUri()`/
  `setSafTreeUri()`/`clearSafTreeUri()` (DataStore key baru, tanpa skema DB).
- `ui/MainViewModel.kt`: `safTreeUri` StateFlow, `setSafTreeUri()`,
  `clearSafTreeUri()`, `releaseSafPermission()`.
- `MainActivity.kt` (protected, edit parsial): `safTreePickerLauncher`
  (`ActivityResultContracts.OpenDocumentTree`), param `onPickSafFolder`
  di-thread lewat `PromptVaultRoot` -> `Routes.SETTINGS`.
- `ui/screens/SettingsScreen.kt`: card "Folder Kustom (Opsional)" + 3 param
  (`safTreeUri`, `onPickSafFolder`, `onClearSafFolder`) + `friendlySafFolderLabel()`.
- `app/build.gradle.kts` (protected, edit parsial): dependency
  `androidx.documentfile:documentfile:1.0.1` ditambah balik; versionCode 58,
  versionName 2.17.0.
- `app/src/test/java/.../MimeTypeForFileNameTest.kt` (baru, 5 test case).
- **SENGAJA TIDAK disentuh**: `ui/screens/DiagnosticsScreen.kt` (card "Zip
  Sorter" dulu HANYA untuk modul Zip Sorter yang independen -- di luar scope
  batch ini by design, lihat syarat (c) di atas), `data/db/**` (lihat poin DB
  Schema di atas), `AndroidManifest.xml` (SAF tidak butuh permission apa pun
  di manifest -- justru itu alasan utama SAF ada).

Verifikasi yang BENAR-BENAR dijalankan sebelum ZIP ini dikirim (bukan cuma
diklaim): `scripts/preflight_check.sh` -- awalnya FAIL (1 paren tak seimbang,
typo di komentar dokumentasi `copyDocumentBytes()`, sudah diperbaiki), lolos
bersih di run kedua. Ditambah review manual baris-per-baris untuk tipe/
nullability/signature (kelas kesalahan yang preflight TIDAK bisa deteksi,
sesuai catatan root-cause Insiden #7) -- lihat detail di section "STATUS SAF"
di bawah. **BELUM PERNAH lewat `./gradlew` asli** -- confidence report ada di
pesan chat, JANGAN dianggap 100% pasti kompil di percobaan pertama.

Temuan sampingan (DICATAT, TIDAK DIEKSEKUSI -- di luar scope "tambah fitur
SAF"): `FileSorter.undo()` (kedua jalur, File maupun SAF) tidak punya
`withContext(Dispatchers.IO)` sendiri, kemungkinan berjalan di dispatcher
pemanggil (Main, lewat `rememberCoroutineScope()` di `ActivityLogScreen`) --
karakteristik ini SUDAH ADA sebelum batch ini (bukan regresi baru), sengaja
TIDAK diperbaiki di sini supaya tetap 1 scope per batch.

## v2.16.1 -- Hotfix: build gagal, `shadowElevation` bukan param ModalBottomSheet (2026-08-09)
CI gagal 2x berturut-turut (attempt sebelumnya) di `compileDebugKotlin`:
`VaultActionSheet.kt:53: Cannot find a parameter with this name: shadowElevation`.
Root cause: `ModalBottomSheet` pada `compose-bom = 2024.06.00` (material3 1.2.x)
TIDAK punya parameter `shadowElevation` -- itu cuma ada di `Surface` (dipakai
benar di `VaultCard.kt`). Fix: ganti jadi `tonalElevation = 0.dp` di
`VaultActionSheet.kt` (efek visual sama -- flat, tanpa elevation Material
default), komentar diperbarui biar sesi depan tidak salah tiru pola ini lagi
ke composable lain yang bukan `Surface`.
- File diubah: `VaultActionSheet.kt` (1 baris param), `app/build.gradle.kts`
  (versionCode 57, versionName 2.16.1).

## v2.16.0 -- Technical debt audit & atomic closure (2026-08-09)
User minta daftar SEMUA technical debt kode/fitur (bukan testing) yang belum
kesampaian sejak awal project, dieksekusi jadi 1 batch atomic. Audit penuh
`PROJECT_STATE.md` (semua insiden & roadmap) + grep TODO/FIXME/known-limitation
di seluruh kode. Hasil & keputusan per item -- lihat pesan chat untuk tabel
lengkap "dieksekusi vs tidak + alasan". Yang DIEKSEKUSI di batch ini (2 item,
saling terkait erat -> 1 atomic change, bukan digabung asal-asalan):

1. **Opsi tema "Terang"/"Ikuti Sistem" DIHAPUS TOTAL** (dead feature sejak
   v2.14.0 -- `PromptVaultTheme` sudah hardcode 1 skema gelap, opsi di
   Pengaturan tidak lagi mengubah apapun, UI berbohong ke user). Bukan
   diimplementasi ulang (kontradiksi spesifikasi desain "dark mode adalah
   satu-satunya mode"), tapi dicabut sampai akar:
   - `data/SettingsRepository.kt`: `enum ThemeMode`, `themeModeFlow`,
     `setThemeMode()`, key DataStore `theme_mode` dihapus.
   - `ui/theme/Theme.kt`: `PromptVaultTheme(darkTheme: Boolean)` ->
     `PromptVaultTheme()` (parameter mati dibuang, bukan cuma diabaikan).
   - `ui/screens/SettingsScreen.kt`: section "Tampilan" (FlowRow 3 FilterChip)
     dihapus, param `currentThemeMode`/`onThemeModeSelected` dibuang.
   - `ui/MainViewModel.kt`: `themeMode` StateFlow + `setThemeMode()` dihapus.
   - `MainActivity.kt`: `effectiveDark`/`isSystemInDarkTheme()`/wiring
     `ThemeMode` di 2 composable (root theme + Routes.SETTINGS) dihapus.
2. **Konfirmasi sukses simpan rule ditambahkan** (gap didokumentasikan sejak
   audit v2.4.3, sengaja dibiarkan waktu itu -- "GAP dicatat, SENGAJA belum
   difix"). Tombol "Simpan" di AddEditRuleScreen sebelumnya nol sinyal sukses
   selain navigasi balik implisit.
   - Pola one-shot SAMA seperti `ScanFeedback` (v2.4.4, eventId bukan isi
     teks) -- **disimpan di ViewModel, dikonsumsi di RuleListScreen**
     (layar TUJUAN setelah pop), BUKAN di AddEditRuleScreen sendiri, karena
     form itu langsung di-dispose sesaat setelah simpan (persis kelas bug
     Snackbar Home v2.4.4 kalau state-nya lokal ke composable yang dibuang).
   - `ui/MainViewModel.kt`: `RuleSaveFeedback` data class baru +
     `ruleSaveFeedback` StateFlow + `consumeRuleSaveFeedback()`. `saveRule()`
     dapat parameter `announce: Boolean = false` -- default false supaya
     toggle enable/disable Switch (yang juga lewat `saveRule()`) TIDAK ikut
     memicu Snackbar "disimpan" tiap digeser (noise, bukan feedback berguna).
   - `ui/screens/RuleListScreen.kt`: param `ruleSaveFeedback` +
     `onRuleSaveFeedbackConsumed`, `LaunchedEffect` tampilkan Snackbar
     `Rule "X" disimpan`.
   - `MainActivity.kt`: Routes.RULES collect+wire `ruleSaveFeedback`;
     Routes.ADD_EDIT_RULE panggil `saveRule(rule, id, announce = true)`.
- File diubah (6): `data/SettingsRepository.kt`, `ui/theme/Theme.kt`,
  `ui/screens/SettingsScreen.kt`, `ui/MainViewModel.kt`, `MainActivity.kt`,
  `ui/screens/RuleListScreen.kt`. Tidak ada file baru/dihapus.
- Item debt YANG TIDAK dieksekusi (dicatat, bukan diabaikan) -- lihat
  PROJECT_STATE.md bagian "Technical debt audit 2026-08-09" untuk alasan
  masing-masing: migrasi data DataStore lama pre-v2.2.0 ke Room,
  `SCAN_CONCURRENCY=6` hardcoded/belum diprofilkan, §6 CI dependency-lock
  (terhambat struktural, sama seperti sebelumnya).
- versionCode 55->56, versionName 2.15.0->2.16.0.
- **Belum ada konfirmasi CI/device dari user untuk versi ini.**

## v2.15.0 -- Audit kepatuhan 100% ke spesifikasi tema (gap closure) (2026-08-09)
User minta tema di-override ulang "100% sesuai markdown, jangan ada celah
setitik pun". Audit ulang file-per-file v2.14.1 vs spesifikasi menemukan 3
pelanggaran nyata + 2 penyesuaian token nilai persis:
- **Pelanggaran bab 18 (Glow System) di `GroupedListRow.kt`**: SETIAP baris
  menu Home (4 ikon sekaligus) pakai `Modifier.shadow()` berwarna tint
  sebagai glow permanen -- persis masuk daftar "Forbidden: Every icon" di
  spesifikasi. Diganti kotak ikon glass datar (fill tint alpha rendah +
  border rambut `GlassBorder`, tanpa shadow berwarna sama sekali).
- **Pelanggaran bab 12 (Tactile Switch) di `RuleCard.kt`**: toggle enable
  rule masih `Switch` Material3 polos, bukan kontrol tactile recessed sesuai
  spesifikasi. Komponen baru `TactileSwitch.kt` dibuat (track recessed saat
  OFF, terangkat + tint aksen + glow lokal SATU titik di thumb saat ON,
  posisi thumb sebagai penanda kedua di luar warna/kedalaman -- bab 21
  Accessibility), dipakai gantikan `Switch` di `RuleCard`.
- **Gap bab 7/8 (Frosted Glass / Glass Edge) di `VaultActionSheet.kt`**: sheet
  konfirmasi (hapus/undo) sebelumnya panel flat `surfaceVariant` tanpa tepi
  glass sama sekali. Ditambah highlight rambut 1dp di top (bukan border
  penuh -- sesuai arah cahaya bab 9) + `shadowElevation=0` supaya tidak jatuh
  ke shadow Material default yang tidak sesuai bahasa visual glass.
- **Presisi token di `Color.kt`**: `MidnightBlueGradientAlpha` 0.08f->0.06f
  (persis nilai `MidnightBlueAmbientAlpha` di spesifikasi bab 6, bukan
  didekati). Tambah `TextMuted` (0xFF737E8C) yang sebelumnya belum ada
  padahal disebut eksplisit di bab 16.
- **TIDAK diubah** (di luar cakupan lapisan visual/tema, risiko blast-radius
  lebih besar dari 1 batch tema): opsi "Terang"/"Ikuti Sistem" yang belum
  fungsional di `SettingsScreen.kt` -- ini keputusan fitur/UX (`ThemeMode`,
  `SettingsRepository`, wiring `MainActivity`), bukan pelanggaran dokumen
  desain visual yang jadi acuan batch ini. Tetap tercatat sebagai known
  limitation di PROJECT_STATE.md, belum dihapus.
- File diubah (5) + 1 file baru: `ui/theme/Color.kt`,
  `ui/components/GroupedListRow.kt`, `ui/components/RuleCard.kt`,
  `ui/components/VaultActionSheet.kt`, `app/build.gradle.kts` (versi), +
  BARU `ui/components/TactileSwitch.kt`.
- Nol perubahan logika bisnis/data layer -- murni lapisan visual/komponen,
  sama seperti batch v2.14.0.
- versionCode 54->55, versionName 2.14.1->2.15.0.
- **Belum ada konfirmasi CI/device dari user untuk versi ini.**

## v2.14.1 -- Fix regresi: CTA "Scan Sekarang" pucat/glitch (2026-08-08)
`tactilePress()` (shadow 4dp saat idle) di CTA Home menyebabkan kotak pucat
translusen di beberapa device (fallback render shadow di atas gradient
custom). Revert CTA ke `.pressScale()` polos. Lihat PROJECT_STATE.md.
- `ui/screens/HomeScreen.kt`: ganti `tactilePress` -> `pressScale`.
- versionCode 53->54, versionName 2.14.0->2.14.1.

## v2.14.0 -- Tema visual diganti total: AMOLED Glassmorphism Hybrid + Midnight Blue Gradient (2026-08-08)
User upload spesifikasi desain dan minta tema default ditimpa sampai bersih,
100% sesuai isi dokumen. Lihat PROJECT_STATE.md untuk detail & known
limitation (toggle "Terang" di Pengaturan kini tidak berfungsi, dark AMOLED
adalah satu-satunya mode).
- `ui/theme/Color.kt`: hapus total palet terang & obsidian lama, ganti token
  baru (`AmoledBackground`, `GlassSurface[Elevated/Sheet/Pressed]`,
  `MidnightBlueTint`/`MidnightBlueAccent[Container/On]`, `TextPrimary/
  Secondary`, `GlassHighlight/Border/Shadow`, aksen semantik `StampGlow`/
  `AmberGlow`/`RustGlow`/`SlateGlow` ditata ulang warnanya).
- `ui/theme/Theme.kt`: satu `darkColorScheme` saja (tidak ada lagi
  `lightColorScheme`), `darkTheme` param diabaikan secara sengaja.
- `ui/theme/TactileTokens.kt` (baru): elevasi/skala/durasi tactile terpusat.
- `ui/components/VaultCard.kt`: gradient glass 3-stop (Elevated -> tint
  Midnight Blue alpha 0.08 -> Surface) + border rambut translusen.
- `ui/components/PressScale.kt`: tambah `tactilePress()` (skala + elevasi
  turun ke 0 saat ditekan), dipakai di CTA "Scan Sekarang" (`HomeScreen.kt`).
- `values/colors.xml`, `values/themes.xml`: parent tema non-light, splash
  screen AMOLED.
- `mipmap-anydpi-v26/ic_launcher*.xml`: background ikon AMOLED (ganti dari
  `pv_pine`).
- `MainActivity.kt` (partial): status/nav bar scrim gelap permanen
  (`SystemBarStyle.dark`), bukan lagi `SystemBarStyle.auto`.
- versionCode 52->53, versionName 2.13.0->2.14.0.

## v2.13.0 -- SAF dihapus total ke akar, atas permintaan eksplisit user (2026-08-08)
User minta: hapus SEMUA fitur terkait SAF sampai bersih ke akarnya, dan HANYA
diterapkan kembali kalau root cause kesalahan fatal sudah benar-benar
dipahami. Lihat PROJECT_STATE.md bagian "Insiden #7" untuk analisis lengkap
kenapa jawabannya, untuk sekarang, TIDAK diterapkan kembali.
- **Dihapus total** (§1 roadmap backend "SAF/Scoped Storage" + modul "Zip
  Sorter" v2.12.0, keduanya berbasis SAF/`DocumentFile`):
  - `util/FileSorter.kt`: `resolveSafRoot`, `scanAndSortSafLocked`,
    `listCandidateFilesSaf`, `isLikelyStillWritingSaf`, `processCandidateSaf`,
    `mimeTypeForFileName`, `findOrCreateChildDirSaf`, `copyDocumentBytes`,
    `moveFileSaf`, `undoSaf` -- `scanAndSortLocked()`/`undo()` kembali ke
    jalur java.io.File/Downloads murni, sama seperti sebelum v2.7.0.
  - `data/SettingsRepository.kt`: `safTreeUriKey`/`safTreeUriFlow`/get-set.
  - `ui/MainViewModel.kt`: `safTreeUri`, `setSafTreeUri`, `clearSafTreeUri`,
    `releaseSafPermission`.
  - `MainActivity.kt`: `safTreePickerLauncher`, `zipSorterViewModel`, param
    `onPickSafFolder`, route `Routes.ZIP_SORTER` + composable-nya.
  - `ui/screens/SettingsScreen.kt`: card UI "Folder Kustom (Opsional)" +
    3 parameter terkait.
  - `ui/screens/DiagnosticsScreen.kt`: card "Zip Sorter (modul terpisah)" +
    param `onOpenZipSorter`.
  - File dihapus utuh: `ui/ZipSorterViewModel.kt`,
    `ui/screens/ZipSorterScreen.kt`, seluruh folder `zipsorter/` (model,
    repository, util, worker -- 4 file).
  - `app/build.gradle.kts`: dependency `androidx.documentfile:documentfile`
    dibuang (sudah tidak dipakai kode manapun).
- **TIDAK dihapus/berubah**: rule engine utama (`data/Rule*`, UI Kelola
  Rule), Room DB (log & history), worker auto-sort, crash logger, tema. Semua
  ini 100% independen dari SAF, nol risiko regresi dari batch ini.
- **Perilaku user-facing setelah update**: aplikasi HANYA memindai/memindah
  file di Downloads (java.io.File + `MANAGE_EXTERNAL_STORAGE`), persis
  seperti v2.6.0 ke bawah. Tidak ada lagi opsi "Folder Kustom" di Pengaturan,
  tidak ada lagi menu "Zip Sorter" di Diagnostik. Kalau ada user yang sempat
  set folder kustom, settingnya otomatis diabaikan (key dihapus dari
  DataStore) -- scan kembali ke Downloads tanpa perlu aksi user apapun.
- versionCode 51->52, versionName 2.12.0->2.13.0 (perubahan perilaku runtime
  nyata, bukan cuma compile-fix -- wajib bump per kebijakan CHANGELOG v2.8.1).
- **Belum diverifikasi CI/device** -- ini murni operasi hapus/subtraksi kode
  (bukan fitur baru), risiko regresi jalur Downloads/java.io.File rendah
  karena jalur itu sendiri sudah stabil sejak v2.4.0 dan TIDAK disentuh sama
  sekali di batch ini (SAF selalu jadi cabang paralel opsional, dihapus
  cabangnya saja).

## v2.12.1 -- COMPILE-FIX: `zipSorterViewModel` Unresolved reference di NavHost (2026-08-07)
User upload `build-failure-log-v2_12_0.zip`. `:app:compileDebugKotlin FAILED`
-- 4x "Unresolved reference: zipSorterViewModel" di `MainActivity.kt` (baris
composable ZIP_SORTER).
- **Root cause (kesalahan Claude)**: `NavHost`/semua `composable{}` route
  ternyata TIDAK berada langsung di dalam class `MainActivity` -- sudah lama
  diekstrak ke fungsi top-level `private fun PromptVaultRoot(viewModel:
  MainViewModel, ...)` (dipanggil dari `setContent{}`). Properti activity
  `zipSorterViewModel` yang ditambah batch v2.12.0 TIDAK otomatis kebawa ke
  scope fungsi terpisah itu -- beda dengan `viewModel` (`MainViewModel`) yang
  memang sudah jadi parameter resmi `PromptVaultRoot` sejak awal.
- **Fix**: tambah parameter `zipSorterViewModel: ZipSorterViewModel` ke
  `PromptVaultRoot`, teruskan dari `setContent{}` (`zipSorterViewModel =
  zipSorterViewModel`). Tidak ada perubahan logika lain -- 1 file,
  `MainActivity.kt`, murni compile-fix.
- **Pelajaran**: kalau nambah state/viewModel baru yang dipakai di dalam
  `composable{}`, WAJIB cek dulu apakah `NavHost` ada di scope class Activity
  atau sudah diekstrak ke fungsi Composable terpisah -- jangan asumsikan
  properti activity otomatis in-scope.
- versionCode/versionName TETAP 51/2.12.0 (compile-fix, belum pernah publish
  sukses di versi ini).

## v2.12.0 -- Fitur baru: modul Zip Sorter (engine kategori file + auto-extract ZIP) (2026-08-07)
User upload dokumen boilerplate "Android File & Zip Auto-Sorter Core Engine"
(package asli `com.example.filesorter`) dan minta diterapkan ke PromptVault
dengan package disesuaikan + contoh ViewModel/Screen SAF. Diimplementasi
sebagai modul **terisolasi total** `zipsorter/` (model, util, repository,
worker) -- TIDAK menyentuh/menggantikan rule engine utama (`util/FileSorter.kt`)
supaya risiko nol terhadap fitur existing yang sudah SELESAI/STABLE.

**File baru (6, 1 modul, dalam Batch Lock):**
- `zipsorter/model/FileSortModels.kt` -- FileCategory/SortState/SortConfig.
- `zipsorter/util/ZipFileUriHelper.kt` -- helper SAF (unique filename, folder).
- `zipsorter/repository/ZipSorterRepository.kt` -- scan+extract+move via DocumentFile.
- `zipsorter/worker/ZipSortWorker.kt` -- CoroutineWorker (opsional, belum dijadwalkan).
- `ui/ZipSorterViewModel.kt` -- AndroidViewModel, state via StateFlow.
- `ui/screens/ZipSorterScreen.kt` -- contoh Compose screen + `ActivityResultContracts.OpenDocumentTree()`.

**Bug di dokumen sumber user, DIPERBAIKI saat diadaptasi (bukan disalin mentah):**
1. `ZipSortWorker` (draft: `FileSortWorker`) -- `override async suspend fun doWork()`
   BUKAN syntax Kotlin valid (`async` bukan keyword deklarasi fungsi). Fix: `override suspend fun doWork()`.
2. `ZipSorterRepository`: gerbang `doc.isFile` berisiko false-negative sama
   persis dengan insiden #6 di `FileSorter.kt` lama (MIME kosong/salah dari
   provider). Fix: pola `!doc.isDirectory` (positive check via MIME_TYPE_DIR
   yang lebih reliable), konsisten dengan pelajaran permanen di PROJECT_STATE.md.
3. `extractZip()` deklarasi return `Boolean` tapi tidak pernah return `false`
   saat exception (exception cuma di-print, fungsi lanjut seolah sukses).
   Fix: dibungkus try-catch, return `false` eksplisit di jalur gagal.
4. `getUniqueTargetFile()` (util lama) berpotensi infinite-loop/create-lalu-cek-ulang
   pada file yang baru saja dibuatnya sendiri. Ditulis ulang jadi lurus:
   cek nama tersedia dulu (loop counter), baru `createFile()` sekali di akhir.
5. `processFolder()` tidak menangani folder target invalid/kosong dengan rapi
   (bisa div/0 di perhitungan progress `totalFiles`). Fix: early-return
   `SortState.Error`/`SortState.Success(0,0)` eksplisit.

**Wiring (parsial, protected assets, minimal):**
- `ui/Navigation.kt`: tambah `Routes.ZIP_SORTER`.
- `MainActivity.kt`: tambah `zipSorterViewModel` + 1 `composable(Routes.ZIP_SORTER)`.
- `ui/screens/DiagnosticsScreen.kt`: tambah 1 `VaultCard` + tombol "Buka Zip Sorter"
  (param `onOpenZipSorter` default no-op, tidak mengubah pemanggilan lama manapun).
- `app/build.gradle.kts`: TIDAK ada dependency baru -- `androidx.documentfile:documentfile:1.0.1`
  & `androidx.work:work-runtime-ktx` sudah ada dari batch §1/§5 lama. AndroidManifest.xml
  TIDAK diubah -- SAF `OpenDocumentTree` tidak butuh permission tambahan.

**Keterbatasan sengaja / belum dikerjakan (di luar scope, jangan diasumsikan bug):**
- `ZipSortWorker` dibuat tapi belum didaftarkan ke WorkManager/scheduler manapun --
  murni disediakan sebagai komponen siap pakai kalau user mau jadwalkan nanti.
- Modul ini TIDAK terhubung ke `Rule`/`ActivityLogRepository`/`MoveHistoryRepository`
  milik engine utama -- kategori & log-nya independen, by design (isolasi modul).
- **BELUM diverifikasi runtime** (sandbox tanpa Gradle/device) -- WAJIB build CI +
  test manual (pilih folder, taruh file campuran+ZIP, cek hasil grouping & extract)
  sebelum dianggap matang.

## v2.8.3 -- Fix: listCandidateFilesSaf gagal detect file, MIME false-negative (2026-08-06)
Kelas bug SAMA dgn v2.8.1 (`resolveSafRoot`). `listCandidateFilesSaf()`
syaratkan `doc.isFile` -- query MIME type provider, false-negatif kalau MIME
kosong/salah (umum tergantung asal file). Hasil: sebagian file di folder
custom ke-skip TOTAL dari deteksi scan (acak per file, tergantung MIME-nya).
Fix: syarat ganti ke `!doc.isDirectory` (MIME_TYPE_DIR jauh lebih konsisten
diisi provider drpd MIME detail). versionCode 44->45. PELAJARAN: audit semua
pemakaian method boolean DocumentFile (isFile/canRead/canWrite/exists) di
FileSorter.kt -- kelas bug ini bisa berulang di tempat lain yg belum ketauan.

## v2.8.2 -- Fitur: tombol Salin Log (2026-08-06)
`ActivityLogScreen` tab "Log" dapat tombol copy di top bar -- nyalin semua
entri log ke clipboard (`[timestamp] LEVEL: pesan`), biar user bisa cepat
ekstrak & kirim log ERROR utk diagnosa (mis. SAF gagal vs folder custom
emang kosong) tanpa perlu ADB/Logcat. versionCode 43->44.

## v2.8.1 -- Bump versi + fix GitHub Release "stuck" (2026-08-06)
Tag release ikut `versionName`; 2 hotfix sebelumnya (return@coroutineScope,
resolveSafRoot) tidak naikkan versi -> tag sama `v2.8.0` -> Release cuma
di-UPDATE in-place, tampilan tidak berubah, user kira stuck/gagal publish.
Bump versionCode 42->43, versionName ->2.8.1 supaya dapat tag & entri
Release baru yang jelas -- termasuk membawa fix resolveSafRoot (SAF gagal
baca folder custom, silent fallback) ke build yang bisa diverifikasi.

## Fix runtime: SAF gagal baca folder custom (2026-08-06)
Verifikasi runtime pertama §1 Fase 2 GAGAL: folder custom dipilih, file
ditaruh, scan tetap bilang "tidak ada file cocok" tanpa error. Root cause:
`resolveSafRoot()` pakai `doc.exists()`/`doc.canRead()` sbg gerbang --
keduanya false-negative dikenal luas di banyak `DocumentProvider` (SD card
dll). Gerbang gagal -> `return null` diam-diam -> fallback ke Downloads,
scan folder yang salah tanpa jejak. Fix: gerbang jadi `doc.isDirectory` +
`doc.listFiles()` sbg probe akses nyata di try-catch, dan tambah log ERROR
eksplisit di ActivityLog untuk kedua jalur gagal (sebelumnya 100% silent).
versionCode/versionName tetap 42/2.8.0. BELUM dikonfirmasi user pasca-fix.

## Compile-fix v2.8.0 ronde 2 (2026-08-06)
CI FAILED lagi setelah fix ronde 1 (di bawah): `FileSorter.kt:357` &
`:364` -- 2 baris early-return (`rules.isEmpty()` / `candidateFiles.isEmpty()`)
masih pakai `return` polos di dalam `coroutineScope{}`. Koreksi klaim ronde 1:
`coroutineScope`'s `block` param di kotlinx.coroutines itu `crossinline`,
BUKAN sekadar `inline` -- jadi non-local `return` polos DILARANG compiler
("'return' is not allowed here"), hanya `return@coroutineScope` (labeled)
yang valid. Baris terakhir fungsi (:394) sudah benar; 2 baris awal
kelewatan. Fix: `return` -> `return@coroutineScope` di kedua baris. Tidak
ada perubahan logika. versionCode/versionName tetap 42/2.8.0.

## Compile-fix v2.8.0 ronde 1 (2026-08-06)
CI gagal di `scanAndSortSafLocked()`: fungsi ini `private suspend fun` biasa
(bukan `withContext{}`/`coroutineScope{}`), jadi `async{}`/`awaitAll()` di
dalamnya tidak punya receiver `CoroutineScope` -> unresolved reference.
Beda dari `scanAndSortLocked()` legacy yang sudah dibungkus
`withContext(Dispatchers.IO){}`. Fix: bungkus body dengan `coroutineScope{}`
+ import `kotlinx.coroutines.coroutineScope`, `return` awal jadi
`return@coroutineScope` di ujung fungsi. Tidak ada perubahan logika/behavior,
versionCode tetap 42 / versionName tetap 2.8.0 (Fase 2 SAF masih BELUM
dikonfirmasi runtime, jadi tidak layak naik versi baru dari compile-fix
murni).

## v2.8.0 -- §1 Roadmap backend Fase 2/2 (HYBRID): FileSorter pakai DocumentFile
Lanjutan v2.7.0 (Fase 1, dikonfirmasi jalan di HP asli: picker muncul, izin
persist lintas restart app). §1 SEKARANG FUNGSIONAL PENUH: kalau user pilih
folder kustom lewat SAF, `FileSorter` scan/move/undo di folder ITU lewat
`DocumentFile`, bukan lagi cuma nyimpen URI yang dormant.

**Desain**: `scanAndSortLocked()` panggil `resolveSafRoot()` di awal --
kalau ada URI SAF tersimpan DAN masih valid (`exists()`, `canRead()`,
`isDirectory`), delegasikan SELURUH scan ke `scanAndSortSafLocked()` (jalur
baru, DocumentFile). Kalau tidak ada/tidak valid, lanjut ke jalur Downloads/
java.io.File PERSIS SEPERTI SEBELUM Fase 2 ada -- fallback otomatis & diam-
diam, tidak pernah error ke user hanya gara-gara SAF gagal.

**Kenapa jalur SAF DIDUPLIKASI (bukan digabung generic)**: supaya jalur
Downloads/java.io.File yang sudah stabil sejak v2.4.0 nol risiko regresi --
tidak ada satu baris pun logikanya yang disentuh. Fungsi baru (semua
suffix `Saf`): `scanAndSortSafLocked`, `listCandidateFilesSaf`,
`isLikelyStillWritingSaf`, `processCandidateSaf`, `moveFileSaf`, `undoSaf`,
plus helper `findOrCreateChildDirSaf` & `copyDocumentBytes`.

**Kejutan baik**: `MoveHistoryEntity`/`MoveHistoryEntry` (Protected: DB
Schema/DAO) TIDAK PERLU diubah sama sekali -- `originalParentUri`/`destUri`
sudah bertipe `String` generik dari awal (komentar di source-nya bahkan
sudah menyebut "SAF / MediaStore" sebagai kemungkinan). Jadi entry SAF
tinggal menyimpan `content://...` di situ, dan `undo()` publik jadi
dispatcher: `destUri.startsWith("content://")` -> `undoSaf()`, selain itu
-> `undoLegacy()` (isi asli `undo()`, cuma diganti nama).

**Keterbatasan yang SENGAJA diterima batch ini** (didokumentasikan sebagai
trade-off, bukan bug yang lolos tanpa sadar):
1. `previewPatternMatches()` & `listDownloadsCandidateFileNames()` (layar
   Tambah/Edit Rule & Diagnostik) TETAP baca Downloads/java.io.File walau
   mode SAF aktif -- keduanya fungsi non-suspend dipanggil sinkron dari UI;
   baca setting SAF butuh suspend. Kalau dibutuhkan, ini batch terpisah.
2. Dual Stability Guard versi SAF cuma 2 dari 3 sinyal legacy (age +
   size-delta, TANPA file-lock check) -- SAF/`ParcelFileDescriptor` tidak
   punya padanan `RandomAccessFile.tryLock()` yang konsisten lintas
   document provider.
3. §2 (cleanup ghost MediaStore) tidak dipanggil di jalur SAF -- query
   berbasis path java.io.File, tidak relevan untuk `content://`.
4. Move & undo pakai copy-lalu-hapus (konsisten dengan fallback
   `copyThenDelete` yang sudah dipakai jalur legacy), BUKAN
   `DocumentsContract.moveDocument` -- lebih portable lintas provider.

**Perubahan**: `FileSorter.kt` (perubahan besar, lihat di atas) +
`build.gradle.kts` (dependency baru `androidx.documentfile:documentfile:1.0.1`,
Protected File, edit parsial/tambah 1 baris). 2 file, tapi **DIDEKLARASIKAN
SEBAGAI ATOMIC CHANGE** (migrasi arsitektur inti) sesuai Batch Lock rule --
tidak dipecah lebih lanjut karena scan/move/undo untuk 1 mode (SAF) memang
satu kesatuan fungsional yang tidak masuk akal displit ke commit terpisah.

**Verifikasi runtime TIDAK BISA dilakukan sama sekali** (sandbox tanpa
Android SDK/device) -- **INI BATCH PALING BERISIKO SEJAUH INI**, ditandai
eksplisit di sini karena `FileSorter` adalah fungsi inti aplikasi. Preflight
statis lolos 10/10 (kurung seimbang, import benar, XML valid), TAPI itu
TIDAK sama dengan kompilasi Kotlin sukses apalagi behavior benar di
runtime. **Checklist WAJIB di HP asli sebelum dianggap stabil**:
1. Mode Downloads (TANPA pilih folder SAF) -- scan/move/undo masih persis
   seperti sebelumnya (regression check jalur legacy).
2. Mode SAF aktif: taruh file ZIP/TXT di folder kustom, scan manual,
   pastikan file pindah ke `<folder kustom>/PromptVault/<rule>/`.
3. Undo pada entry SAF -- file balik ke folder kustom asal.
4. Auto-sort background (`AutoSortWorker`) dengan SAF aktif -- pastikan
   tidak crash (worker jalan di proses terpisah dari UI, akses
   `DocumentFile`/`ContentResolver` harus tetap valid di context Application).
5. Konflik nama file (RENAME/SKIP/OVERWRITE) di folder SAF.

## v2.7.0 -- §1 Roadmap backend Fase 1/2 (HYBRID): SAF folder picker (dormant, belum dipakai FileSorter)
Keputusan user 2026-08-05: §1 SAF terlalu berisiko untuk full-replace (tidak
bisa di-compile-test di sandbox, nyentuh hampir semua fungsi inti). Approach
yang dipilih: **HYBRID** -- SAF cuma opsi TAMBAHAN, MANAGE_EXTERNAL_STORAGE
tetap jadi mekanisme default/fallback. Dipecah jadi 2 fase supaya risiko
kecil per batch (bukan satu rombakan raksasa):

- **Fase 1 (batch ini, v2.7.0)**: infrastruktur SAF picker + penyimpanan URI
  saja. TIDAK ADA perubahan ke `FileSorter.kt` sama sekali -- scan/move/undo
  TETAP 100% java.io.File seperti sekarang, untuk SEMUA user (termasuk yang
  memilih folder SAF). URI yang dipilih baru DISIMPAN, belum DIPAKAI.
- **Fase 2 (batch berikutnya, terpisah)**: `FileSorter` baca URI tersimpan --
  kalau ada & masih valid, pakai `DocumentFile` untuk scan/move/undo di folder
  itu; kalau tidak ada/tidak valid lagi, fallback ke Downloads/java.io.File
  seperti sekarang. Ini bagian paling berisiko, sengaja dipisah supaya Fase 1
  bisa dikonfirmasi jalan dulu (picker muncul, izin persist, URI tersimpan)
  sebelum logic scan/move/undo yang jauh lebih kompleks disentuh.

**Kenapa dipecah begini**: kalau Fase 1+2 digabung sekaligus dan ada bug di
manapun, tidak jelas apakah masalahnya di UI picker atau di logic scan --
menyulitkan debug tanpa akses Gradle/device. Dengan Fase 1 terisolasi (tidak
menyentuh FileSorter), risiko regresi ke fitur sortir yang sudah stabil = 0.

**Perubahan**:
- `SettingsRepository.kt`: field baru `safTreeUriFlow`/`getSafTreeUri()`/
  `setSafTreeUri()` (DataStore, nullable String, simpan `Uri.toString()`).
- `MainViewModel.kt`: expose `safTreeUri: StateFlow<String?>` +
  `setSafTreeUri()`/`clearSafTreeUri()`.
- `MainActivity.kt` (Protected File, edit parsial): launcher baru
  `ActivityResultContracts.OpenDocumentTree()` -- panggil
  `contentResolver.takePersistableUriPermission()` SEBELUM simpan URI (wajib,
  kalau tidak izin hilang begitu proses app mati). `PromptVaultRoot` dapat 1
  parameter baru `onPickSafFolder`.
- `SettingsScreen.kt`: section baru "Folder Kustom (Opsional)" -- tampilkan
  folder terpilih (decode nama dari URI) atau "Belum ada folder kustom
  dipilih (pakai Downloads)", tombol Pilih/Ganti Folder & Hapus.

File yang diubah: `SettingsRepository.kt`, `MainViewModel.kt`,
`MainActivity.kt` (parsial), `SettingsScreen.kt`. 4 file, dalam Batch Lock.
Tidak ada perubahan ke `AndroidManifest.xml` -- `OpenDocumentTree()` adalah
system picker, tidak butuh permission tambahan di manifest.

**Verifikasi runtime TIDAK BISA dilakukan** (sandbox tanpa Android
SDK/device). Preflight statis lolos 10/10. **Yang PALING PENTING
dikonfirmasi di HP asli sebelum lanjut Fase 2**: (1) tombol "Pilih Folder"
di Pengaturan memunculkan picker sistem, (2) setelah pilih folder & buka
app lagi (restart proses), nama folder masih muncul di layar Pengaturan
(bukti persisted permission benar-benar tersimpan lintas proses), (3) tombol
"Hapus, Kembali ke Downloads" mengosongkan lagi. Kalau salah satu gagal,
JANGAN lanjut ke Fase 2 sebelum ini beres -- Fase 2 bergantung total ke
fondasi ini benar.

## v2.6.0 -- §5 Roadmap backend selesai: Coroutine lifecycle & Foreground Service
Lanjutan dari v2.5.0 (§2 selesai). Urutan roadmap: §2 -> §5 -> §1 (§6 tetap
diskip, butuh akses Gradle nyata).

**Masalah**: `AutoSortWorker` (CoroutineWorker periodic via WorkManager) jalan
murni sebagai background worker biasa. Di Android 12+ ada batasan eksekusi
background yang lebih agresif -- worker yang jalan lama (scan ratusan file,
tiap kandidat ada stability-check 1 detik, lihat v2.4.0) beresiko
dijeda/dibunuh OS di device yang agresif membatasi baterai (device utama
user, Infinix custom ROM XOS, termasuk kategori ini), tanpa notifikasi
apapun ke user kenapa auto-sort kadang tidak tuntas.

**Audit coroutine lifecycle**: `scanAndSortLocked()` sudah `withContext
(Dispatchers.IO)` + `async`/`awaitAll()` (v2.4.0) -- ini sudah cooperative
cancellation gratis lewat structured concurrency kotlinx.coroutines: kalau
WorkManager memanggil `onStopped()` (constraints tidak lagi terpenuhi / OS
minta stop), `CoroutineWorker` otomatis cancel coroutine `doWork()`, dan
seluruh child coroutine di `scanAndSortLocked()` ikut ter-cancel otomatis di
titik suspensinya masing-masing (delay stability-check, I/O). **Tidak ada
bug ditemukan di sektor ini** -- tidak perlu perubahan kode untuk bagian
lifecycle-nya, murni foreground service yang jadi gap nyata.

**Fix (foreground service)**:
- File baru `worker/AutoSortNotification.kt`: notification channel
  (`IMPORTANCE_LOW`, tanpa suara, `setShowBadge(false)`) + builder
  `ForegroundInfo` (pakai `FOREGROUND_SERVICE_TYPE_DATA_SYNC` di API 29+,
  constructor 2-argumen tanpa type di API < 29).
- `AutoSortWorker.doWork()`: panggil `setForeground(...)` SEBELUM
  `sorter.scanAndSort()`. Dibungkus try-catch best-effort -- kalau OS
  menolak promosi foreground (skenario tak terduga), auto-sort TETAP lanjut
  sebagai background worker biasa, tidak menggagalkan seluruh proses.
- `PromptVaultApp.onCreate()`: panggil `AutoSortNotification.ensureChannel()`
  sekali di awal proses (idempoten) supaya channel sudah ada sebelum worker
  pertama kali butuh `setForeground()`.
- `AndroidManifest.xml` (Protected File, edit parsial): tambah permission
  `FOREGROUND_SERVICE_DATA_SYNC` (wajib sejak targetSdk 34 untuk service
  type dataSync), dan override eksplisit
  `<service android:name="androidx.work.impl.foreground.SystemForegroundService"
  android:foregroundServiceType="dataSync" tools:node="merge" />` supaya
  type-nya PASTI ter-set, bukan bergantung default manifest merge WorkManager.
- `strings.xml`: 4 string baru (nama+deskripsi channel, judul+teks
  notifikasi).

File yang diubah: `AutoSortWorker.kt`, `PromptVaultApp.kt`,
`AndroidManifest.xml`, `strings.xml` (edit parsial) + 1 file baru
(`AutoSortNotification.kt`). 5 file, dalam Batch Lock (1 modul: `worker/`
+ pendukungnya).

**Verifikasi runtime TIDAK BISA dilakukan** (sandbox tanpa Android
SDK/device). Preflight statis lolos 9/9. Yang PALING PENTING dikonfirmasi
di HP asli: (1) notifikasi "Auto-sort berjalan" benar-benar muncul saat
scan otomatis jalan (bukan cuma manual "Scan Sekarang" yang tidak lewat
worker ini), (2) tidak ada crash/`ForegroundServiceStartNotAllowedException`
di Logcat/crash log app saat worker jalan di background murni (app tidak
dibuka user sebelumnya) -- ini skenario paling beresiko untuk restriksi
Android 12+, walau secara teori dokumentasi resmi Android WorkManager +
`setForeground()` termasuk jalur yang diizinkan.

**[COMPILE-FIX 2026-08-05]** CI build v2.6.0 gagal: `processDebugMainManifest`
error `SAXParseException` -- `The string "--" is not permitted within
comments` di `AndroidManifest.xml` baris 14. Sebab: 2 komentar penjelasan
yang ditambahkan di batch §5 (izin `FOREGROUND_SERVICE_DATA_SYNC` & override
`<service>`) memakai `--` sebagai pemisah kalimat di dalam teks komentar --
valid di komentar Kotlin (`//`) tapi XML MELARANG KERAS substring `--` di
badan komentar `<!-- -->` (aturan spec XML 1.0, bukan quirk Android). Fix:
ganti `--` di kedua komentar itu jadi koma biasa, tidak ada perubahan logika
apapun. Preflight statis sebelumnya LOLOS karena kategori #8 cuma cek
validitas YAML workflow, bukan well-formedness XML manifest -- gap ini
dicatat untuk preflight_check.sh ke depan (lihat PROJECT_STATE.md).
`versionCode`/`versionName` TIDAK dibumping (build sebelumnya tidak pernah
sukses jadi APK, sama seperti pola compile-fix v2.5.0).

## v2.5.0 -- §2 Roadmap backend selesai: MediaStore ghost-file cleanup
User eksplisit minta tuntaskan SEMUA item roadmap backend yang sebelumnya
sengaja DIJEDA (§1 SAF, §2 MediaStore, §5 Foreground Service, §6 CI
dependency lock). Spec asli "BACKEND & CI/CD EXECUTABLE SPECIFICATION"
TIDAK ada teksnya di repo (cuma ringkasan 1 baris di PROJECT_STATE.md) --
user konfirmasi desain ulang dari standar Android + konteks app. Dieksekusi
BERTAHAP per item (bukan sekaligus, sesuai Batch Lock -- 4 item ini masing-
masing atomic change terpisah, beda area arsitektur). §2 duluan karena
paling mandiri & risikonya paling terukur secara statis (murni logic Kotlin,
tidak butuh Gradle/network buat verifikasi seperti §6, tidak seberat §1/§5).

**Masalah**: app pindah file lewat `java.io.File.renameTo()`/copy langsung
(BUKAN SAF/MediaStore API -- keputusan arsitektur #2 yang sudah ada di
PROJECT_STATE.md). Efek samping: index MediaStore (dipakai file manager
bawaan, app lain yang baca lewat Content Provider bukan filesystem langsung)
TIDAK otomatis update. Dua gejala: (a) file yang baru dipindah baru muncul
di app lain setelah reboot/scan manual, (b) entri "hantu" nyangkut di
MediaStore menunjuk ke path lama yang filenya sudah tidak ada.

- **Fix (a)**: `MediaScannerConnection.scanFile()` dipanggil untuk path lama
  + baru setiap kali `moveFile()` atau `undo()` sukses -- MediaStore langsung
  disuruh re-index kedua lokasi. Non-fatal by design: kalau scanFile gagal,
  pemindahan filenya SENDIRI tetap dianggap sukses (jangan sampai indexing
  kosmetik menggagalkan hasil nyata).
- **Fix (b)**: fungsi baru `cleanupGhostMediaStoreEntries()` -- query
  `MediaStore.Files` untuk baris di bawah `Downloads/PromptVault/`, cek tiap
  baris apakah file fisiknya masih ada (`File(path).exists()`), hapus baris
  yang tidak ada filenya. Dipanggil SEKALI per scan (bukan per file, jaga
  performa sesuai prinsip v2.4.0/v2.4.1), non-fatal kalau query/delete gagal.
  Pakai `MediaStore.Files.FileColumns.DATA` (deprecated API 29+ tapi tetap
  jalan untuk app dengan `MANAGE_EXTERNAL_STORAGE`, yang app ini sudah
  pakai) -- dipilih daripada migrasi ke SAF karena itu §1 yang terpisah.

File yang diubah: `FileSorter.kt` (Core File, edit parsial -- cuma tambah
2 blok kecil + 1 fungsi baru, tidak ubah logika match/rule/conflict yang
sudah ada). 1 file, jauh di bawah Batch Lock.

**Verifikasi runtime TIDAK BISA dilakukan** (sandbox tanpa Android SDK,
tidak ada device buat lihat MediaStore beneran ke-update). Preflight statis
lolos 9/9, tapi ini murni jaring pengaman sintaks -- TIDAK bisa konfirmasi
`MediaScannerConnection`/`ContentResolver.query` benar-benar berperilaku
seperti didokumentasikan di device asli. **Tolong konfirmasi setelah build:
pindahkan 1 file, cek muncul di file manager LAIN (bukan PromptVault)
tanpa reboot.**

**[COMPILE-FIX 2026-08-04]** CI build v2.5.0 gagal: `compileDebugKotlin`
error di `FileSorter.kt:272` -- "Suspend function 'add' should be called
only from a coroutine or another suspend function". Sebab: fungsi baru
`cleanupGhostMediaStoreEntries()` di atas ke-deklarasi `private fun` biasa
(lupa `suspend`), padahal isinya manggil `activityLogRepository.add()`
(suspend). Fix: tambah keyword `suspend` di deklarasinya -- pemanggilnya
(`scanAndSortLocked`) sudah suspend context, jadi aman, tidak ada
pemanggil lain yang perlu disesuaikan. HANYA 1 baris di `FileSorter.kt`
diubah, tidak ada logika lain disentuh. Static scan ulang seluruh fungsi
di file ini (semua pemanggilan `activityLogRepository.add`/
`moveHistoryRepository.*`) -- tidak ada mismatch suspend lain ditemukan.

## v2.4.4 -- Fix bug regresi v2.4.3: Snackbar hasil scan muncul berulang
User laporkan gejala nyata: tiap habis pencet "Lihat detail file yang
dilewati" (navigasi ke SkippedFilesScreen) lalu balik, Snackbar hasil scan
yang di v2.4.3 muncul lagi -- padahal scan baru tidak dijalankan.

**Root cause**: `scanFeedback` di-model StateFlow yang isinya menempel terus
di ViewModel (survive navigasi, sesuai tujuan awal), TAPI `LaunchedEffect
(scanFeedback?.eventId)` yang menampilkannya cuma "one-shot" selama
`HomeScreen` itu sendiri tetap hidup di composition. Navigation Compose
men-dispose `HomeScreen` saat pindah layar dan MEMBUAT ULANG saat balik --
instance baru itu tidak tahu event `eventId` yang sama sudah pernah
ditampilkan sebelumnya, jadi efeknya jalan lagi. Ini regresi yang lolos dari
preflight statis karena preflight tidak (dan tidak bisa) mendeteksi bug
siklus-hidup Compose seperti ini -- murni ketahuan dari laporan gejala nyata
user, sesuai proses yang benar.

- **Fix**: state "sudah dikonsumsi" dipindah ke ViewModel lewat
  `MainViewModel.consumeScanFeedback()` (set `_scanFeedback.value = null`),
  dipanggil dari `HomeScreen` SEBELUM `snackbarHostState.showSnackbar(...)`
  (bukan sesudah -- `showSnackbar` suspend sampai dismiss/timeout, kalau
  konsumsi ditunda ke situ ada celah waktu user sempat gonta-ganti layar
  sebelum Snackbar pertama kelar & re-trigger tetap bisa kejadian).
- **Efek samping yang ikut difix**: karena `scanFeedback` sekarang di-null-
  kan LEBIH AWAL dari sebelumnya, warna Snackbar (merah utk error) yang tadinya
  dibaca langsung dari `scanFeedback?.isError` bisa balik ke warna normal
  padahal Snackbar error masih tampil di layar. Ditambah `activeIsError`
  (local state di HomeScreen, di-snapshot di awal LaunchedEffect) supaya
  warna tetap konsisten selama durasi tampil Snackbar itu.

File yang diubah: `MainViewModel.kt`, `HomeScreen.kt`, `MainActivity.kt`
(wiring 1 param baru `onScanFeedbackConsumed`). 3 file, dalam Batch Lock.

**Verifikasi runtime TIDAK BISA dilakukan** (sandbox tanpa Gradle) --
preflight lolos 9/9, tapi tunggu konfirmasi kamu: pencet Scan Sekarang ->
buka Lihat Detail File yang Dilewati -> balik -> Snackbar TIDAK boleh
muncul lagi.

## v2.4.3 -- Audit sektor "feedback interaksi": Snackbar + haptic hasil Scan Sekarang
User minta audit tuntas khusus sektor feedback: apa yang diharapkan user saat
berinteraksi dengan app. Audit statis menyisir semua 8 screen + MainViewModel
untuk pola Snackbar/Toast/haptic. Temuan: RuleListScreen (hapus rule) dan
ActivityLogScreen (undo) SUDAH punya Snackbar. Tapi aksi PALING SERING dipakai
di app ini -- tombol "Scan Sekarang" di HomeScreen -- cuma update teks pasif
`lastScanSummary` di dalam card. Kalau hasil scan kali ini teksnya sama persis
dengan scan sebelumnya (mis. "Tidak ada file cocok" dua kali berturut), user
TIDAK dapat sinyal apapun bahwa scan barusan benar-benar jalan -- terasa
seperti tombol tidak merespons.

- **Fix**: `MainViewModel` sekarang expose `scanFeedback: StateFlow<ScanFeedback?>`
  terpisah dari `lastScanSummary`, dibedakan lewat `eventId` (timestamp) supaya
  tetap trigger ulang walau teks pesan identik dengan sebelumnya.
- **HomeScreen**: tambah `SnackbarHost` + haptic yang bereaksi ke `scanFeedback`
  -- `HapticFeedbackType.TextHandleMove` (halus) untuk hasil normal,
  `HapticFeedbackType.LongPress` (sama seperti pola konfirmasi destruktif di
  `VaultActionSheet`) untuk kasus error (folder Downloads tidak terbaca / izin
  bermasalah), plus warna Snackbar ikut berubah jadi `colors.error` saat error.
- **Urutan sinyal**: `_scanFeedback` sengaja di-emit PALING TERAKHIR di
  `runManualScan()`, setelah `isScanning` balik ke false -- supaya user selalu
  lihat: spinner hilang dulu, baru Snackbar muncul (bukan bertabrakan).
- **Tidak diubah** (dicek, dianggap cukup): highlight FilterChip di
  SettingsScreen sudah jadi feedback visual yang wajar untuk pilihan tema/
  interval/conflict strategy; import rule di SettingsScreen sudah punya teks
  hasil persisten di layar. Tombol "Simpan" di AddEditRuleScreen tidak diubah
  karena feedback-nya sudah implisit lewat navigasi kembali ke list (rule baru
  langsung terlihat) -- di luar scope batch ini, dicatat sebagai kandidat
  audit lanjutan di PROJECT_STATE.md kalau user minta lain kali.

File yang diubah: `MainViewModel.kt`, `HomeScreen.kt`, `MainActivity.kt` (wiring
1 baris param), `app/build.gradle.kts` (version bump). 4 file, dalam batas
Batch Lock.

**Verifikasi runtime TIDAK BISA dilakukan** (sandbox tanpa Android SDK/Gradle)
-- preflight_check.sh (kurung seimbang, delegate `by`, import, dst) LOLOS
100%, tapi tetap cuma jaring pengaman statis. Tunggu konfirmasi kamu setelah
build asli bahwa Snackbar + getaran muncul benar setelah tap "Scan Sekarang".

## [Dokumentasi] v2.4.2 dinyatakan STABLE RELEASE (2026-08-04)
Tidak ada perubahan kode/build. User konfirmasi APK sudah muncul di sidebar
Releases dan minta project dinyatakan "selesai" secara resmi. Lihat
PROJECT_STATE.md bagian "STATUS PROJECT: SELESAI / STABLE" untuk kriteria
Definition of Done dan aturan permanen sesi berikutnya (stop audit proaktif).

## v2.4.2 -- Fix CI: APK sekarang publish ke GitHub Release (bukan cuma Actions Artifact)
User cek repo, APK signed tidak muncul di sidebar Releases seperti seharusnya
(sesuai aturan proyek "GitHub Release Rule"). Audit `.github/workflows/build.yml`
konfirmasi: workflow SELAMA INI cuma pakai `actions/upload-artifact@v4` --
Actions Artifact biasa (butuh login GitHub, expired 90 hari, tidak muncul di
Releases repo), TIDAK PERNAH benar-benar publish ke GitHub Release. Ini gap
yang lolos sejak workflow pertama kali dibuat, tidak ketahuan karena preflight
lama tidak punya kategori yang mengecek ini.

- **Fix**: tambah `permissions: contents: write` di level workflow (wajib
  untuk action bisa buat Release), dan step baru `softprops/action-gh-release@v2`
  setelah step upload-artifact (artifact lama TETAP dipertahankan sebagai
  fallback, tidak dihapus) -- tag dibuat otomatis `v<versionName>`, APK
  ter-attach ke Release itu. Kalau tag versi yang sama sudah ada (re-run),
  action ini UPDATE release yang sama, bukan gagal/duplikat.
- **Preflight**: ditambah kategori #9 -- cek `build.yml` mengandung step
  publish Release (`softprops/action-gh-release`/`actions/create-release`/
  `gh release`), FAIL kalau tidak ada. Supaya gap seperti ini tidak lolos
  lagi ke rilis berikutnya.

File yang diubah: `.github/workflows/build.yml`, `scripts/preflight_check.sh`
(2 file, dalam batas Batch Lock). Tidak ada perubahan kode aplikasi.

**Verifikasi runtime TIDAK BISA dilakukan** (sandbox tanpa akses GitHub
Actions) -- step `softprops/action-gh-release@v2` adalah action pihak
ketiga yang umum dipakai & terdokumentasi, tapi tetap tunggu konfirmasi user
setelah CI jalan bahwa Release + APK benar-benar muncul di sidebar repo.

## v2.4.1 -- Kurangi write-contention SQLite saat scan paralel (lanjutan optimasi v2.4.0)
User konfirmasi v2.4.0 (scan paralel + IO dispatcher) sudah terasa cepat di
HP asli, lalu diminta lanjut fokus performa. Audit lanjutan ke jalur yang
DIPANGGIL selama scan (bukan scan-nya sendiri) menemukan bottleneck baru:

- **`ActivityLogRepository.add()` dan `MoveHistoryRepository.record()`
  memanggil `dao.trimToMax()` di SETIAP insert, tanpa terkecuali.**
  `trimToMax()` adalah `DELETE ... WHERE id NOT IN (SELECT id ... ORDER BY
  timestampMillis DESC LIMIT :maxEntries)` -- query yang scan+sort seluruh
  tabel tiap kali dipanggil. Selama scan v2.4.0 memproses banyak file
  paralel lewat `Semaphore(6)`, tiap kandidat file bisa memicu 1+ log line
  (skip reason / overlap warning / hasil pindah) -- untuk scan 300 file,
  ini berarti ratusan `trimToMax()` beruntun, masing-masing memperebutkan
  write-lock SQLite (Room menyerialkan write transaction secara internal).
  Efeknya: paralelisme yang baru ditambahkan di `FileSorter` sebagian
  "dimakan lagi" oleh serialisasi di sisi database.
- **Fix**: trim sekarang berkala, bukan tiap insert -- `AtomicInteger`
  per-instance repository, trim dijalankan tiap kelipatan
  `TRIM_CHECK_INTERVAL = 20` insert (aman dipanggil concurrent lintas
  coroutine tanpa Mutex tambahan). Konsekuensinya tabel boleh melebihi
  `MAX_ENTRIES` sampai maksimal 19 baris ekstra di antara dua trim -- tidak
  terlihat user (log/riwayat undo tetap tampil normal, cuma retensi
  membulat ke kelipatan 20), jauh lebih murah daripada trim tiap baris.

File yang diubah: `ActivityLogRepository.kt`, `MoveHistoryRepository.kt`
(2 file, dalam batas Batch Lock). Tidak ada perubahan API publik/schema,
`FileSorter`/`MainViewModel`/`AutoSortWorker` tidak perlu disentuh.

**Verifikasi runtime TIDAK BISA dilakukan** (sandbox tanpa Android
SDK/Gradle) -- murni audit statis + reasoning tentang pola concurrency
Room/SQLite. Confidence tinggi (perubahan idiomatik, isolated ke 2
repository), tapi tetap tunggu konfirmasi user setelah build CI sukses.

## v2.4.0 -- Overhaul performa scan (fokus 100% permintaan user, bukan finishing biasa)
User laporan: app "kewalahan" scan bahkan cuma dengan ratusan file di
Downloads (100-500), gejalanya freeze/lag lama, force close, DAN auto-sort
background jadi lambat/telat -- ketiganya sekaligus. Audit `FileSorter.kt`
menemukan 3 akar masalah independen yang saling memperparah:

1. **Semua I/O jalan di Main thread**: `scanAndSort()` dipanggil dari
   `MainViewModel.runManualScan()` lewat `viewModelScope.launch` (default ke
   Main dispatcher). Karena fungsi ini TIDAK PERNAH pindah dispatcher, setiap
   `File.listFiles()`, buka `RandomAccessFile` untuk cek lock, `renameTo()`,
   `copyTo()` -- semua I/O blocking sinkron -- dulunya jalan LANGSUNG di UI
   thread. Ini penyebab utama freeze & force-close (ANR). Fix: seluruh isi
   `scanAndSortLocked()` sekarang dibungkus `withContext(Dispatchers.IO)`.
2. **Urutan pengecekan terbalik**: fungsi `isLikelyStillWriting()` (delay 1
   detik + buka file handle untuk cek lock) dulu dijalankan untuk SEMUA file
   kandidat ZIP/TXT di Downloads, TERMASUK file yang tidak cocok rule
   manapun dan tidak akan pernah dipindah. Fix: cek rule match (murah,
   in-memory) sekarang jalan duluan; stability check cuma untuk file yang
   memang akan dipindah.
3. **Scan sekuensial, satu file per satu file**: `for (file in
   candidateFiles)` berarti 300 file yang lolos ke stability check = ~300
   detik (1 detik delay/file berturutan, tanpa paralelisme sama sekali).
   Fix: tiap kandidat sekarang diproses lewat `async` + `Semaphore` (batas 6
   proses bersamaan -- **asumsi teknis AI**, lihat PROJECT_STATE.md), hasil
   digabung lewat `awaitAll()` lalu diagregasi SEKUENSIAL di luar coroutine
   paralel (bukan mutable var dibagi lintas coroutine) supaya tidak perlu
   Mutex tambahan untuk `moved`/`skipped`/`overlapWarnings`.

**Tidak ada perubahan behavior/UI yang terlihat user** (pesan Log, alasan
skip, hasil scan tetap identik) -- murni performa. `ActivityLogRepository`
& `MoveHistoryRepository` sudah Room-backed sejak v2.2.0 sehingga aman
dipanggil concurrent tanpa perubahan tambahan di kedua file itu.

File yang diubah: `FileSorter.kt` saja (1 file, dalam batas Batch Lock).
`MainViewModel.kt`/`AutoSortWorker.kt` TIDAK perlu disentuh karena
`scanAndSort()` tetap `suspend fun` dengan signature sama persis --
`withContext(Dispatchers.IO)` bekerja transparan dari dispatcher pemanggil
manapun.

**Verifikasi runtime TIDAK BISA dilakukan** (sandbox Claude tanpa Android
SDK/Gradle) -- ini murni audit statis + reasoning tentang model concurrency
Kotlin coroutines. Confidence tinggi karena perubahan idiomatik & didukung
langsung oleh dokumentasi resmi kotlinx.coroutines, tapi tetap tunggu
konfirmasi user setelah build CI sukses DAN scan beberapa ratus file di HP
asli terasa jauh lebih cepat.

## v2.3.9 -- Konsistensi visual: padding seragam + animasi Onboarding
Lanjutan batch finishing (bagian "konsistensi visual" yang tadinya belum
disentuh). User konfirmasi CI v2.3.8 sukses build, lalu diaudit ulang sisi
konsistensi visual -- ditemukan 2 hal nyata:

- **Padding luar layar tidak konsisten**: separuh layar pakai `16.dp`,
  separuh `20.dp` untuk konteks yang sama (padding utama konten di bawah
  TopBar). Distandarkan ke **16dp** di semua layar (`HomeScreen`,
  `AddEditRuleScreen`, `DiagnosticsScreen`, `SettingsScreen` yang tadinya
  20dp/20dp horizontal, disamakan dengan `RuleListScreen`,
  `ActivityLogScreen`, `SkippedFilesScreen` yang sudah 16dp) -- 16dp dipilih
  karena itu keyline margin standar Material Design untuk layar ponsel
  (bukan tablet), pas untuk target device (Infinix Android 15/16).
  `OnboardingScreen` sengaja TETAP 24dp (konteks beda -- full-bleed flow
  sekali jalan, bukan layar dengan TopBar seperti yang lain).
- **`OnboardingScreen` belum ikut dapat animasi transisi state** padahal
  semua layar lain sekarang punya `Crossfade` -- step 1->2->3->4 tadinya
  potong instan. Ditambah `Crossfade` (220ms) yang membungkus ikon + judul
  + body tiap step, konsisten dengan pola yang sudah dipakai di 4 layar
  lain sejak v2.3.7.

Tidak ada perubahan logika/fitur. Murni kosmetik, resiko rendah (tidak
menyentuh modul yang sudah diaudit logic sebelumnya).

## v2.3.8 -- Fix build gagal dari v2.3.7 (animateItemPlacement + alias ikon salah)
⚠️ **v2.3.7 GAGAL BUILD di CI.** User upload log build gagal, dua bug nyata
ditemukan -- keduanya murni kesalahan Claude sendiri karena tidak ada akses
Gradle/jaringan di sandbox untuk verifikasi kompilasi asli sebelum v2.3.7
dikirim (sudah diperingatkan di catatan v2.3.7, dan benar terjadi).

- **`Modifier.animateItemPlacement()` di 3 file (`RuleListScreen`,
  `ActivityLogScreen`, `SkippedFilesScreen`) gagal kompilasi total:**
  1. Import `androidx.compose.foundation.lazy.animateItemPlacement` yang
     dipakai kemarin SALAH -- fungsi ini bukan top-level function yang bisa
     di-import, melainkan member extension dari `LazyItemScope` (otomatis
     tersedia di dalam lambda `items { }`, tanpa perlu import sama sekali).
     Error: "Unresolved reference: animateItemPlacement" di baris import.
  2. Fungsi ini juga ber-anotasi `@ExperimentalFoundationApi` -- tanpa
     opt-in eksplisit, Kotlin menolaknya sebagai ERROR (bukan cuma
     warning). Fix: hapus import yang salah, tambah
     `@OptIn(ExperimentalFoundationApi::class)` di ketiga fungsi Composable
     yang memakainya.
- **Alias ikon `Rule` di `RuleListScreen` (fix tabrakan nama versi
  sebelumnya) ternyata salah juga:** `Icons.Filled.Rule` adalah *extension
  property*, jadi alias `import ... as RuleIcon` tetap butuh receiver-nya
  (`Icons.Filled.RuleIcon`), bukan `RuleIcon` polos -- ini yang bikin error
  "receiver type mismatch". Daripada bergantung ke alias yang gampang salah
  lagi ke depan, ikon empty-state di `RuleListScreen` diganti total ke
  `Icons.Filled.PlaylistAdd` (tidak collide dengan `data.Rule`, tidak perlu
  alias sama sekali).
- Tidak ada perubahan fitur/UI baru di rilis ini -- murni perbaikan supaya
  v2.3.7 (fix izin legacy + empty state + animasi) benar-benar bisa di-build.

## v2.3.7 -- Finishing batch: fix izin legacy (Android 8-10) + UI polish pertama
User konfirmasi regresi Home v2.3.1 sudah normal, lalu minta lanjut tahap
"finishing": audit menyeluruh + robustness + polish UI, digabung satu ZIP.

**Robustness -- izin runtime Android 8-10 (API 26-29), sebelumnya sengaja
ditunda, sekarang dibenerin atas pilihan user:**
- `hasManageStoragePermission()` di `MainActivity.kt` sebelumnya hardcode
  `true` untuk seluruh rentang SDK di bawah 30, padahal `minSdk = 26`.
  Sekarang benar-benar mengecek `ContextCompat.checkSelfPermission()` untuk
  `READ_EXTERNAL_STORAGE` (dan `WRITE_EXTERNAL_STORAGE` khusus API <= 28,
  sesuai `maxSdkVersion` di manifest).
- Ditambah `ActivityResultLauncher` (`RequestMultiplePermissions`) di
  `MainActivity` supaya device API 26-29 langsung dapat dialog izin sistem
  saat menekan "Buka Pengaturan Izin", bukan cuma dilempar ke halaman
  Setelan Aplikasi umum seperti sebelumnya.
- `PermissionGate` dapat tombol fallback tambahan "Izin ditolak permanen?
  Buka Pengaturan Aplikasi" (hanya tampil di API < 30) untuk kasus user
  sudah pernah menolak dialog izin dan Android tidak akan menampilkannya
  otomatis lagi.
- Ditambah auto-recheck status izin lewat `DisposableEffect` +
  `Lifecycle.Event.ON_RESUME`, supaya begitu user balik dari Setelan
  (baik alur API 30+ maupun API 26-29), status langsung ke-refresh tanpa
  wajib pencet tombol "cek ulang" manual (tombolnya tetap ada sebagai
  fallback).

**UI polish -- empty state konsisten:**
- Komponen baru `ui/components/EmptyState.kt`: ikon bulat bertema warna
  aksen + judul + pesan, menggantikan `Text()` polos yang sebelumnya gaya
  beda-beda di tiap layar (padding top tidak konsisten, tanpa ikon).
- Dipasang di `RuleListScreen` (accent hijau/primary, 2 varian pesan: rule
  kosong total vs hasil pencarian kosong), `ActivityLogScreen` tab Log &
  tab Undo (accent amber/tertiary), `SkippedFilesScreen` (accent
  stamp/secondary).

**UI polish -- animasi:**
- `Crossfade` (220ms) untuk transisi kosong<->berisi konten di keempat
  layar di atas, supaya tidak lagi potong instan saat state berubah.
- `Modifier.animateItemPlacement()` di semua `LazyColumn` (daftar rule,
  log aktivitas, riwayat undo, file dilewati) supaya item yang
  bertambah/berkurang/pindah posisi (mis. reorder prioritas rule)
  beranimasi halus, bukan lompat tiba-tiba.
- Transisi fade+slide (`enterTransition`/`exitTransition`/
  `popEnterTransition`/`popExitTransition`, ~200ms) di `NavHost` untuk
  semua perpindahan layar -- sebelumnya navigasi antar layar potong instan
  tanpa animasi sama sekali, kontras dengan bagian lain app yang sudah
  banyak animasi kecil (press-scale, segmented control).

**Perubahan arsitektur kecil:**
- `RuleCard` (`ui/components/RuleCard.kt`) sekarang menerima parameter
  `modifier: Modifier = Modifier` (diteruskan ke `VaultCard` internal).
  Sebelumnya tidak ada -- wajib ditambah supaya `RuleListScreen` bisa
  memasang `animateItemPlacement()` langsung ke card yang sebenarnya,
  bukan ke wrapper kosong. Konvensi ini sekarang jadi standar untuk
  komponen list-item baru ke depannya (lihat catatan di
  `PROJECT_STATE.md`).

**Catatan verifikasi:** perubahan sudah lolos `preflight_check.sh` +
review manual menyeluruh (cross-check semua call-site yang terdampak
perubahan signature `RuleCard`/`PromptVaultRoot`/`hasManageStoragePermission`).
Belum di-build & dijalankan nyata di device pada sesi ini (tidak ada akses
jaringan Gradle di sandbox Claude) -- mohon konfirmasi hasil build CI +
tampilan di HP setelah update.

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

## v2.11.1 -- Bersihkan label TODO basi (docs-only, no behavior change)
- Diaudit ulang: TODO #1 (Undo), #2 (interval), #3 (overlap warning), #7
  (export), #9 (konfirmasi duplikat) SEMUA sudah lengkap fungsional di kode --
  labelnya cuma lupa dihapus, berisiko menyesatkan sesi Claude berikutnya
  kalau dikira masih ada kerjaan tersisa. Diganti jadi komentar deskriptif
  biasa (bukan TODO).
- TODO #4 & #5 (`DiagnosticsScreen.kt`) DIBIARKAN -- itu memang masih
  pending nyata: PromptVault belum pernah diuji di HP fisik oleh Claude.
- Tidak ada perubahan behavior/logic apapun di batch ini.

## v2.11.0 -- Fix bug UNDO (hasil palsu + pesan hardcode Downloads)
- **Konteks**: fitur UNDO di tab "Undo Pemindahan" sebenarnya SUDAH lengkap
  fungsional (list, konfirmasi, panggil `FileSorter.undo()`) -- komentar
  "TODO #1" di kode sudah basi/tidak akurat, dihapus.
- **Bug nyata yang ditemukan**: `MainViewModel.undoMove()` sebelumnya
  fire-and-forget (`viewModelScope.launch { fileSorter.undo(entry) }`, hasil
  `Boolean` dibuang). `ActivityLogScreen` menampilkan snackbar "berhasil
  dikembalikan ke Downloads" SELALU, tanpa peduli undo aslinya sukses atau
  gagal -- dan teksnya hardcode "Downloads" padahal tujuan bisa folder SAF
  kustom (lihat v2.10.0).
- **Fix**: `undoMove()` jadi `suspend fun` mengembalikan `Boolean` asli;
  `ActivityLogScreen.onUndo` diubah jadi `suspend (MoveHistoryEntry) ->
  Boolean`; snackbar sekarang JUJUR (beda pesan sukses/gagal) dan pesan tidak
  lagi hardcode "Downloads". Tambah guard `undoInFlight` supaya tombol Undo
  tidak bisa di-tap dobel selagi proses berjalan.
- **Belum diverifikasi build CI di sesi ini.**

## v2.10.0 -- Debugging & pematangan SAF (2 bug nyata diperbaiki)
- **Bug #1 (kebocoran izin persisted, FATAL jangka panjang)**: komentar lama di
  `clearSafTreeUri()` bilang pelepasan `releasePersistableUriPermission()`
  "dilakukan di pemanggil (MainActivity)" -- ternyata TIDAK PERNAH benar-benar
  dipanggil di mana pun. Tiap kali user ganti/hapus folder kustom, izin lama
  menumpuk selamanya. Android membatasi jumlah persisted URI permission per
  app (~128) -- kalau limit tercapai, `takePersistableUriPermission()`
  berikutnya lempar `SecurityException` dan fitur folder kustom berhenti bisa
  dipakai TANPA pesan error jelas ke user (silently fallback ke Downloads
  lewat catch block yang sudah ada). **Fix**: `MainViewModel.setSafTreeUri()`
  & `clearSafTreeUri()` sekarang eksplisit `releasePersistableUriPermission()`
  ke URI lama sebelum ganti/hapus (best-effort, gagal-pun diabaikan aman).
- **Bug #2 (mime type tidak reliable, sama kelas insiden #4/#6)**:
  `moveFileSaf`/`undoSaf` sebelumnya pakai `doc.type`/`current.type` (MIME dari
  provider SUMBER) buat `createFile()` di TUJUAN. Provider SAF beda-beda
  (Google Drive, SD card, dll) kadang isi MIME_TYPE generik/salah, dan
  beberapa provider tujuan menambah/ubah ekstensi otomatis sesuai mime saat
  `createFile()` -- berisiko nama dobel-ekstensi (`laporan.txt.txt`) TANPA
  exception apapun. **Fix**: MIME sekarang diturunkan dari ekstensi nama file
  sendiri (`mimeTypeForFileName()`, zip/txt eksplisit, bukan dipercaya dari
  provider), plus verifikasi pasca-`createFile()`: kalau nama aktual di
  storage != nama yang diminta, dicatat WARNING ke Activity Log (bukan
  di-assume berhasil diam-diam).
- **Belum diverifikasi build CI di sesi ini.**

## v2.9.1 -- Viewer crash log di Diagnostik
- **Fitur baru**: `DiagnosticsScreen` sekarang tampilkan daftar crash log
  tersimpan (10 terbaru, total count di judul), pakai `CrashLogger.listLogs()`
  (baru, query MediaStore) berjalan di `Dispatchers.IO`.
- Ketuk satu log -> `AlertDialog` isi lengkap stack trace via
  `CrashLogger.readLog()` (baru), juga di IO thread, fail-safe (query/baca
  dibungkus try-catch, list kosong kalau gagal -- tidak crash layar sendiri).
- Selaras `MAINTENANCE.md`/instruksi baku: "prioritaskan baca crash log
  sebelum minta Logcat/ADB" -- sekarang bisa langsung dari dalam app, tidak
  perlu file manager/adb pull lagi.
- **Belum diverifikasi build CI di sesi ini.**

## v2.9.0 -- Crash Logger bawaan (MediaStore, tanpa permission legacy)
- **Fitur baru**: `util/CrashLogger.kt` -- uncaught exception handler global,
  dipasang paling awal di `PromptVaultApp.onCreate()` (sebelum apapun lain).
  Setiap crash otomatis ditulis ke
  `Documents/PromptVault/logs/crash_<yyyyMMdd_HHmmss>_<uuid8>.txt` lewat
  `MediaStore.Files` (API 29+, scoped storage resmi) -- TIDAK butuh
  `WRITE_EXTERNAL_STORAGE`.
- Metadata lengkap per log: App Version+versionCode, OS (release+SDK),
  Device (manufacturer+model), Thread, timestamp, full stack trace.
- **Fail-safe**: penulisan log dibungkus try-catch penuh; kalau logger
  sendiri gagal, tidak menutupi crash asli -- handler default sistem tetap
  SELALU dipanggil lewat blok `finally`.
- **FIFO retention**: query `MediaStore` untuk file `crash_*.txt` di folder
  log, urut `DATE_ADDED ASC`, hapus yang tertua kalau total > 50.
- Belum ada UI viewer di app (di luar scope batch ini) -- untuk debugging,
  tarik file log langsung dari `Documents/PromptVault/logs/` (file manager
  atau `adb pull`). Prioritaskan baca file ini sebelum minta Logcat/ADB.
- **Belum diverifikasi build CI di sesi ini** -- `preflight_check.sh` lolos
  bersih + review manual, tapi tunggu konfirmasi APK CI sukses.

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
