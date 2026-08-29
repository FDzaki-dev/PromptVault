# PROJECT_STATE.md -- Sortify (repo/folder/package tetap PromptVault, lihat README.md)
> WAJIB dibaca Claude di awal SETIAP sesi baru, sebelum melanjutkan kerja apa
> pun. Jangan hapus riwayat insiden di bawah walau sudah lama/sudah fix --
> ini log kronologis permanen, bukan changelog fitur (itu ada di CHANGELOG.md).

> ⚠️ **STATUS: DISCONTINUED (2026-08-29)**, atas keputusan eksplisit user --
> Fase 1 & 2 `ROADMAP.md` 100% selesai, JANGAN mulai kerjaan baru di sesi
> manapun tanpa user secara eksplisit minta. **Kekecualian: Fase 3 di
> `ROADMAP.md` TETAP dianggap "welcome"** -- kalau user sebut salah satu
> item Fase 3 by name, itu instruksi eksplisit yang sah, lanjutkan seperti
> proyek aktif biasa (bukan proyek diarsipkan/mati permanen, cuma paused).

## 📌 ATURAN WAJIB SESI (PINNED -- jangan pernah turun/terkubur log baru)
1. **DILARANG bump versi manual dalam bentuk apa pun.** `versionCode` &
   `versionName` di `app/build.gradle.kts` WAJIB 100% otomatis, diturunkan
   langsung dari `GITHUB_RUN_NUMBER` (env var bawaan tiap job GitHub
   Actions) -- BUKAN git tag, BUKAN dihitung manual, BUKAN diketik sesi
   mana pun. Detail implementasi: lihat log governance di bawah (2026-08-27).
> Rule ini LOCKED atas instruksi eksplisit user 2026-08-27: "Gak ada
> perubahan oleh sesi lain lagi, berlaku sekarang!!" -- sesi mana pun
> (termasuk sesi ini) DILARANG mengubah/mencabut/melonggarkan rule #1 di
> atas tanpa instruksi eksplisit baru dari user. Rule pinned lama (wajib
> bump manual tiap sesi) SUDAH DICABUT TOTAL & digantikan kebalikannya
> persis -- lihat log di bawah. Section ini PERMANEN di baris teratas file
> ini, tidak ikut aturan descending log biasa -- entri log baru tetap
> disisipkan di bawah section ini, BUKAN di atasnya.
2. **Setiap balasan sesi WAJIB tampilkan versionName/batch file + ringkasan
   singkat kerjaan, tepat SEBELUM `[SCRIPT: DAILY UPDATE]` / `[SCRIPT:
   INITIAL SETUP]`.**
> Rule ini LOCKED atas instruksi eksplisit user 2026-08-27: "setiap sesi
> wajib menampilkan versionName/batch file mereka beserta 'summary' tentang
> apa yang dikerjakannya. Tepat berada sebelum: '[SCRIPT: DAILY UPDATE]'!!"
> -- berlaku PERMANEN mulai sesi ini utk SEMUA sesi berikutnya, sesi mana
> pun DILARANG mencabut/melonggarkan tanpa instruksi eksplisit baru user.

## [STATUS] Project dilabeli DISCONTINUED, Fase 3 tetap welcome (2026-08-29)
- **Instruksi user, verbatim**: "labeli project dengan discontinued, tapi
  fase 3 tetap welcome jika mau dieksekusi sewaktu-waktu" -- dijawab
  setelah ditawari 4 pilihan Fase 3 (`ask_user_input_v0`), user pilih
  TIDAK satupun, minta status label sebagai gantinya.
- **Interpretasi**: ini status marker/dokumentasi, BUKAN "hapus/arsipkan
  project" -- app tetap 100% fungsional & sudah stabil (Fase 1 & 2
  selesai semua). Bedanya cuma: sesi Claude ke depan JANGAN mulai kerjaan
  baru tanpa diminta eksplisit, KECUALI Fase 3 (`ROADMAP.md`) yang memang
  dari awal sudah butuh sebut-nama-eksplisit -- jadi status ini secara
  praktik TIDAK mengubah perilaku sesi terhadap Fase 3 sama sekali, cuma
  menegaskan ulang + menambah label visibilitas di README/PROJECT_STATE.
- **Fix (docs-only, task mikro, Fast-Track -- tanpa audit kode)**: badge
  status ditambah di 3 tempat yang saling sinkron: (1) `README.md` --
  blockquote baru paling atas, sebelum catatan rebranding, (2)
  `PROJECT_STATE.md` (file ini) -- ditambah ke notice wajib-baca paling
  atas supaya sesi manapun langsung lihat sebelum kerja apapun, (3)
  `ROADMAP.md` -- baris status diganti dari nomor versi lama (`v8.20.0`,
  sudah usang sejak governance auto-versioning) jadi label discontinued +
  penegasan Fase 3 tetap sama seperti sebelumnya.
- **TIDAK diubah**: `ROADMAP.md` isi tabel Fase 1/2/3 itu sendiri (0 item
  dicoret/ditambah), `CHANGELOG.md` sinkron sbg entri terpisah di bawah.
  0 kode disentuh sama sekali batch ini.
- File diubah (3 docs, VIP-adjacent, di luar limit): `README.md`,
  `PROJECT_STATE.md`, `ROADMAP.md`. `CHANGELOG.md` disinkron entri
  terpisah (VIP).

## [REBRAND] Nama tampilan PromptVault -> Sortify (2026-08-29)
- **Instruksi user, verbatim**: "Saya mau rebranding nama project jadi
  lebih simpel, memorable, dan konteks aplikasi langsung tersampaikan
  (kosmetik, user facing, rename file project GitHub dan sesi only. Zero
  touch bagian vital dan yang sudah lama stable!!)". Nama baru belum
  ditentukan user -- diajukan 4 kandidat lewat `ask_user_input_v0`
  (Sortify/AutoSort/Tidyload/DropSort), user pilih **Sortify**.
- **Audit scope SEBELUM eksekusi** (grep menyeluruh semua occurrence
  "PromptVault" di project, bukan tebak): literal "PromptVault" dipakai di
  2 peran BERBEDA yang harus dipisah:
  1. **Brand/display name** (aman diganti, "kosmetik" beneran) -- `app_name`,
     teks onboarding/notifikasi/widget/panduan/settings-desc yang menyebut
     app sebagai SUBJEK kalimat ("PromptVault memindai...", "PromptVault
     butuh izin...").
  2. **Kontrak nama folder/identitas fungsional** (BUKAN kosmetik, ini
     "vital & sudah lama stable" persis yang diminta di-zero-touch) --
     ditemukan di: `FileSorter.kt` (`SAF_ROOT_FOLDER_NAME`,
     `SAF_ROOT_CACHE_KEY`, `File(downloadsDir, "PromptVault")`, root folder
     default), `BackupManager.kt` (`ROOT_FOLDER_NAME`, komentar eksplisit
     "literal PromptVault sudah jadi kontrak"), `UpdateRepository.kt`
     (`REPO` -- dipakai buat query GitHub Releases API, HARUS match nama
     repo asli), `CrashLogger.kt` (`RELATIVE_DIR =
     "Documents/PromptVault/logs/"`), class/file Kotlin `PromptVaultApp.kt`
     / `PromptVaultRoot` / `PromptVaultShapes`, `applicationId`/`namespace`
     (`com.elprompter.promptvault`), dan repo GitHub asli
     (`github.com/FDzaki-dev/PromptVault`).
- **Keputusan scope (fully derivable dari instruksi user sendiri, bukan
  interpretasi bebas)**: kategori 1 DIGANTI, kategori 2 ZERO TOUCH TOTAL.
  Kalau kategori 2 ikut diubah: `BackupManager` gagal kenali backup lama
  user (restore rusak), `UpdateRepository` gagal cek update (query ke repo
  yang nggak ada), dan ratusan import Kotlin di seluruh project harus
  diubah (bukan lagi "kosmetik", jadi refactor struktural masif -- persis
  yang DILARANG instruksi user).
- **strings.xml -- 14 dari 22 occurrence diganti** (list lengkap: `app_name`,
  `auto_sort_notif_text`, `settings_restore_message`,
  `settings_interval_section_desc`, `settings_conflict_section_desc`,
  `settings_update_section_desc`, `pandu_intro`, `pandu_section1_body`,
  `pandu_warning_shizuku`, `onboarding_step1_title`,
  `onboarding_step3_body`, `onboarding_step6_body`,
  `permission_gate_message`, `widget_scan_label`). Dieksekusi via script
  Python match-by-string-name (bukan blind find-replace teks) supaya tidak
  ada resiko kena string yang salah kategori. Diverifikasi count sebelum/
  sesudah: 14 baru "Sortify", 8 sisa masih "PromptVault" persis prediksi.
- **8 string TIDAK diubah** (kategori 2, folder-path-tied):
  `rule_edit_hold_back_zip_hint`, `settings_saf_section_desc`,
  `settings_saf_documents_warning`, `settings_shizuku_path_warning`,
  `diag_crashlog_desc`, `pandu_section3_body`, `onboarding_step4_body`,
  `activitylog_dest_local_fmt` -- semua menyebut "PromptVault" sebagai NAMA
  FOLDER ASLI yang beneran dibuat app (match `SAF_ROOT_FOLDER_NAME` dkk di
  atas). Ganti teksnya doang tanpa ganti foldernya beneran = app "bohong"
  ke user soal nama folder yang sebenarnya dibuat.
- **settings.gradle.kts**: `rootProject.name` "PromptVault" -> "Sortify" --
  cuma label kosmetik Gradle (muncul di log build/Android Studio project
  tree), 0 pengaruh ke `applicationId`/output APK/path apa pun.
- **Docs (VIP, di luar limit Micro-Batch)**: judul H1 `README.md` /
  `PROJECT_STATE.md` (file ini) / `PROJECT_STATE_ARCHIVE.md` / `ROADMAP.md`
  / `TROUBLESHOOTING.md` / `FILE_MANIFEST.txt` / `CHANGELOG.md` diganti ke
  "Sortify" (+ catatan singkat "dulu/tetap PromptVault" di masing-masing
  supaya tidak ambigu). `README.md` dapat 1 blok catatan rebranding
  eksplisit di atas (kenapa repo/package tetap PromptVault). `MAINTENANCE.md`
  bagian onboarding-cepat diupdate supaya sesi depan kenali KEDUA nama
  ("lanjutkan project Sortify" ATAU "...PromptVault" = project yang sama).
  **0 riwayat log historis** (badan `CHANGELOG.md`/`PROJECT_STATE.md`/
  `PROJECT_STATE_ARCHIVE.md` yang sudah ada) ditulis ulang -- itu catatan
  kronologis permanen tentang apa yang BENERAN terjadi saat itu (masih sah
  disebut "PromptVault" karena itu memang namanya waktu itu).
- **Penamaan ZIP & script Termux ke depan**: TETAP pakai prefix
  `PromptVault` (bukan `Sortify`) -- folder lokal `~/projects/`, remote
  git `origin`, dan `[NamaFolderProyek]`/`[NamaFileAplikasi]` di script
  Termux immutable semuanya masih merujuk `PromptVault` (repo asli, zero
  touch). Ganti prefix ZIP tanpa ganti repo asli would break
  `find ~/projects -iname "..."` di script Daily Update (bikin folder BARU
  kosong alih-alih update yang lama -- persis skenario "vital yang jangan
  disentuh"). Kalau user mau repo GitHub-nya juga di-rename fisik
  (`gh repo rename`), itu keputusan terpisah yang belum diminta sesi ini.
- File diubah TOTAL (2 kode + 8 dokumentasi VIP, 0 file baru/dihapus,
  `FILE_MANIFEST.txt` isinya sama cuma judul berubah): `strings.xml`,
  `settings.gradle.kts`, `README.md`, `PROJECT_STATE.md`,
  `PROJECT_STATE_ARCHIVE.md`, `MAINTENANCE.md`, `ROADMAP.md`,
  `TROUBLESHOOTING.md`, `FILE_MANIFEST.txt`, `CHANGELOG.md`.
- **Batas jujur**: BELUM PERNAH lewat `./gradlew`/device asli seperti biasa
  -- ini murni edit resource-string/teks (XML value + markdown), risiko
  compile-break MENDEKATI NOL (tidak ada perubahan tipe/struktur Kotlin
  sama sekali), tapi **user WAJIB cek visual di app asli**: (1) nama app
  di launcher/app switcher, (2) widget scan label, (3) teks onboarding
  7 langkah (step 1 & step 3 & step 6 kena), (4) notifikasi auto-sort
  saat scan jalan, (5) beberapa teks di Pengaturan (interval/conflict/
  update section desc) & Panduan Penggunaan.

## [DOCS] Arsipkan riwayat batch usang + sinkronisasi README.md & CHANGELOG.md (2026-08-29, sesi VIP Docs)
- **Trigger**: instruksi eksplisit user, "Arsipkan dokumentasi yang sudah
  stale, lalu sinkronisasi readme.md!!" -- masuk kategori VIP Docs Sync
  (kebal limit/larangan-sentuh, wajib PROJECT_STATE.md/README.md/
  CHANGELOG.md disinkron bersamaan). **0 file kode disentuh** batch ini.
- **Ditemukan sebelum eksekusi (Hard Reset check)**: ZIP user berisi 2
  salinan project -- root (unnested, 2026-08-29, LEBIH BARU) DAN folder
  duplikat `PromptVault/` bersarang di dalamnya (2026-08-27, LEBIH LAMA,
  854 baris `PROJECT_STATE.md` & 252 baris `CHANGELOG.md` lebih pendek).
  Sesuai Hard Reset ("ZIP User = Source of Truth", "DILARANG merger file
  stale/usang"): folder `PromptVault/` bersarang itu DIABAIKAN total,
  root-level dipakai sbg satu-satunya Source of Truth. Kemungkinan sisa
  artefak dari cara user bikin ZIP di Termux -- cek folder kerja
  `~/projects/PromptVault` kalau muncul lagi sesi depan.
- **Arsip baru: `PROJECT_STATE_ARCHIVE.md`** (file baru, VIP-exempt dari
  limit). Isi (dipindah VERBATIM, 0 diringkas/dibuang):
  1. 21 entri batch bergaya versi manual `v8.35.6` (2026-08-27) turun
     sampai `Insiden #6` (2026-08-06) -- seluruh era pra-governance
     auto-versioning, 5623 baris.
  2. Section lama "Versi/batch terakhir yang selesai" (berhenti total di
     versionCode 42/versionName 2.8.0, 2026-08-05 -- SANGAT usang, 3+
     bulan ketinggalan dari kerja aktual project), 243 baris.
- **`PROJECT_STATE.md` ini sendiri** (dipangkas dari 7520 -> jauh lebih
  ringkas, TANPA kehilangan histori -- semua ada di arsip): section
  "Versi/batch terakhir yang selesai" yang usang DIGANTI section baru
  **"Status Terkini"** (lihat di bawah "Keputusan arsitektur utama"),
  isinya akurat per hari ini (bukan nyangkut di 2026-08-05). Section
  PERMANEN lain (`Riwayat insiden kronologis`, `Struktur package/modul`,
  `Keputusan arsitektur utama`, rules PINNED) **TIDAK disentuh sama
  sekali** -- masih 100% isi asli, sesuai instruksi "jangan hapus riwayat
  insiden".
- **`README.md` disinkronkan**: judul hardcode `v7.5.2` (usang parah --
  itu versi dari 2026-08-17, SEBELUM v8.x rebrand total, SEBELUM
  governance auto-versioning) dibuang, diganti deskripsi tanpa nomor versi
  statis (karena versionName SEKARANG otomatis per build CI, `1.0.<run>`,
  nomor statis apapun di README pasti stale lagi sesi berikutnya). Section
  fitur ditambah ringkasan 4 gaya tema (Material3 baseline, Cupertino,
  Neumorphism "Blade Runner", Glassmorphism) + baris dokumentasi baru utk
  `PROJECT_STATE_ARCHIVE.md`.
- **`CHANGELOG.md` disinkronkan**: ditemukan GAP -- batch TERBARU
  `PROJECT_STATE.md` ("colorScheme & aksen ke-4, MENUTUP 4/4 Glass") tidak
  punya entri di `CHANGELOG.md` sama sekali (padahal ini PERSIS kelas bug
  yang baru difix batch "[FIX][CI] Root cause GitHub Release notes stale"
  di atas -- entri hilang dari sini = Release notes GitHub bakal nyangkut
  di judul batch sebelumnya lagi). Ditambahkan 1 entri baru di paling atas
  `CHANGELOG.md`, konsisten dgn 3 entri sebelumnya yg sudah di-backfill.
- **Pending Queue**: kosong (tidak berubah dari batch sebelumnya -- 4/4
  Glass, 4/4 Neumorphism, 3/3 Cupertino tuntas, Material3 sengaja
  baseline). Batch ini murni dokumentasi/housekeeping, 0 fitur/fix baru.

## [UI][GLASSMORPHISM] colorScheme & aksen ke-4 -- MENUTUP 4/4 sumbu Glass (2026-08-29, lanjutan sesi Glass)
- **Trigger**: "Lanjut kerjakan 2 sumbu Glass yang nyisa (colorScheme &
  aksen ke-4)." Sumbu terakhir dari 4 (typography & shape sudah lebih
  dulu selesai di batch2 sebelumnya).
- **Hard Reset**: sesi ini mulai dari ZIP baru user (`PromptVault-main.zip`,
  bukan lanjutan container lama) -- diverifikasi dulu isinya IDENTIK
  hasil batch shape sebelumnya (`GlassShapes`/2-4 progres) sebelum edit,
  0 riwayat hilang.
- **Konflik dgn catatan lama, pola SAMA PERSIS 2 sumbu sebelumnya**:
  javadoc `GlassTokens.kt` v8.23.0 syarat #1 jg melarang sentuh
  "warna dasar/hue" -- disupersede (3/3, MENUTUP semua bagian syarat #1
  yg pernah dilarang). **Syarat #2 ("calm, gak boleh warm") SENGAJA TIDAK
  ikut disupersede** -- palet baru 100% hue dingin, 0 warna hangat.
- **Color.kt** (+14 val): `GlassIce`/`GlassFrost`/`GlassPrism` (primary/
  secondary/tertiary, trio+container+on, H199/H172/H255 -- semua dingin)
  + `GlassGlacier`/`GlassGlacierContainer` (aksen ke-4 "slate", H155
  glacier mint -- pola 2-field spt `CupertinoIndigo`/`NeoMagenta`, tanpa
  on-variant). Kontras dihitung lewat skrip Python (formula luminance
  WCAG) -- worst-case vs `SurfaceContainerHighest`: 7.06-8.79:1, semua
  On*/OnXContainer 8.04-9.23:1. SEMUA lulus AAA (>=7:1), margin besar.
- **Theme.kt** (parsial): `GlassColors` (colorScheme baru) + `GlassExtra`
  (aksen ke-4), pola identik `CupertinoColors`/`NeumorphismColors` di
  atasnya. `colorScheme`/`extraColors` di `PromptVaultTheme` jadi 4-cabang
  penuh (GLASSMORPHISM dipisah dari `else`) -- **MATERIAL3 sekarang
  SATU-SATUNYA gaya yg masih pakai `PromptVaultColors`/`VaultExtra` via
  `else`, 4/4 gaya lain (Cupertino/Neumorphism/Glass) semua sudah py
  identitas warna sendiri.**
- **MENUTUP 4/4**: dengan batch ini SEMUA 4 sumbu identitas visual Glass
  (tipografi, shape, warna, aksen ke-4) sudah lengkap kondisional per
  `themeStyle` -- pola sama spt Neumorphism/Blade Runner sebelumnya sudah
  menutup 4/4-nya duluan.
- File diubah (3, PAS batas Micro-Batch): `ui/theme/Color.kt` (+14 val),
  `ui/theme/Theme.kt` (parsial), `ui/theme/GlassTokens.kt` (parsial, +1
  paragraf supersede, 0 logic diubah).
- **Batas jujur**: sama spt semua batch restyling sebelumnya, BELUM lewat
  `./gradlew`/device asli -- verifikasi sebatas kurung seimbang (manual +
  perlu re-run `scripts/preflight_check.sh` di Termux) + kontras
  dihitung terpisah via skrip Python (bukan dari Compose runtime
  sungguhan). Hasil visual "kesan kaca beku dingin" tetap perlu
  diverifikasi user di HP.
- **Pending Queue**: kosong -- 4/4 sumbu Glass tuntas, tidak ada sumbu
  identitas visual tersisa utk gaya manapun (Cupertino 3/3, Neumorphism
  4/4, Glass 4/4; Material3 sengaja tetap baseline/tidak restyling).

## [UI][GLASSMORPHISM] Shape "frosted-glass corner" -- pisah dari MATERIAL3, lanjutan lengkapi Glass 2/4 (2026-08-29, sesi baru lanjutan)
- **Trigger**: user ditanya "Glassmorphism lanjut dilengkapi juga?" (3
  opsi tombol), user pilih **"Ya, shape dulu (frosted-glass corner)"**.
- **Konflik dgn catatan lama, sama pola spt typography batch sebelumnya**:
  javadoc `GlassTokens.kt` v8.23.0 jg mendaftar "shape radius ... SEMUA
  TIDAK DISENTUH" sbg bagian syarat #1 "Glassmorphism murni". Disupersede
  lagi utk sumbu ini (User Inst TERBARU > catatan lama), dicatat sbg
  paragraf baru terpisah di `GlassTokens.kt` (bukan menimpa paragraf
  supersede typography sebelumnya).
- **Shapes.kt** (+1 val): `GlassShapes` baru, keluarga `RoundedCornerShape`
  (sama spt Cupertino, Compose stok 0 dukung squircle asli) TAPI radius
  PALING besar/lembut dari SEMUA 4 gaya (10/14/20/28/36dp -- lebih besar
  dari CupertinoShapes 8/12/18/24/32) -- kesan "sudut kaca beku" sesuai
  hint eksplisit user "frosted-glass corner".
- **Theme.kt** (parsial): `shapes` di `PromptVaultTheme` jadi 4-cabang
  (GLASSMORPHISM -> `GlassShapes`, dipisah dari `else`). MATERIAL3 sekarang
  satu2nya pemakai `PromptVaultShapes` via `else`. `colorScheme`/
  `extraColors` GLASSMORPHISM MASIH belum berubah (scope sesi ini baru
  shape).
- **Progres Glass 2/4**: typography (batch sebelumnya) + shape (batch ini)
  sudah dipisah dari MATERIAL3. colorScheme & aksen ke-4 MASIH numpang
  `else` bareng MATERIAL3 -- sisa 2/4 kalau user mau lanjut lagi.
- File diubah (3, PAS batas Micro-Batch): `ui/theme/Shapes.kt` (+1 val),
  `ui/theme/Theme.kt` (parsial), `ui/theme/GlassTokens.kt` (parsial,
  +1 paragraf supersede, 0 logic diubah).
- Preflight `scripts/preflight_check.sh` ✅ 14/14 (kurung diverifikasi
  manual dulu per file SEBELUM run preflight, belajar dari insiden bug
  paren batch sebelumnya -- 0 insiden serupa kali ini).
- **Catatan teknis sesi**: container/filesystem kerja Claude sempat RESET
  di antara giliran (`/home/claude` kosong lagi stlh user jeda utk jawab
  pertanyaan tombol) -- source of truth dipulihkan dgn re-extract ZIP
  TERAKHIR yg sudah dikirim (`/mnt/user-data/outputs/PromptVault-1.0.224.zip`,
  masih persisten), BUKAN minta user upload ulang. Diverifikasi isinya
  (`GlassTypography`/`NeoMagenta` masih ada) sebelum lanjut edit -- 0
  histori batch sebelumnya yg hilang.
- **Batas jujur**: sama spt batch2 sebelumnya, BELUM lewat `./gradlew`/
  device asli. Hasil visual "kesan frosted-glass" tetap perlu diverifikasi
  user di HP.
- **Pending Queue**: kosong (2/4 sisa NANTI kalau diminta lanjut, bukan
  otomatis dikerjakan skrng).

## [UI][GLASSMORPHISM] Perkuat typography Glassmorphism murni -- pisah dari MATERIAL3 (2026-08-29, sesi baru)
- **Trigger**: instruksi baru user, "perkuat typography Glassmorphism
  murni". Sebelum batch ini GLASSMORPHISM & MATERIAL3 100% berbagi
  `PromptVaultTypography` yg sama persis (nebeng cabang `else`), 0
  dibedakan.
- **Konflik dgn catatan lama ditemukan & di-supersede (bukan diabaikan
  diam2)**: javadoc `GlassTokens.kt` v8.23.0 pernah eksplisit mendaftar
  "typography ... SEMUA TIDAK DISENTUH" sbg salah satu dari 3 syarat
  "Glassmorphism murni" hasil instruksi user WAKTU ITU. Sesuai hirarki
  resmi (User Inst TERBARU > Core Protocol > riwayat lama), instruksi
  sesi ini membalik syarat tsb utk 1 aspek (typography) -- SUDAH
  diberitahukan ke user singkat di chat sebelum eksekusi. `GlassTokens.kt`
  diberi 1 paragraf SUPERSEDE baru (bukan menghapus paragraf lama -- arsip
  riwayat kenapa aturan lama pernah ada tetap utuh).
- **Type.kt** (+1 val): `GlassTypography` baru. Role
  display/headline/titleLarge/labelLarge (hirarki tertinggi, paling butuh
  berdiri tegas di atas fill translucent blur) -> weight naik 1 tingkat
  (Normal -> Medium/SemiBold) + letter-spacing positif halus (+0.1..
  +0.15sp) -- kesan "elegan/ringan/cahaya menembus kaca". Role
  titleMedium/titleSmall/label(Medium/Small)/body* -> 100% REUSE
  `PromptVaultTypography` apa adanya (SENGAJA tidak ikut diperkuat, jaga
  kesan "kaca lapang/airy" di teks isi). Ukuran/line-height semua role
  identik M3 baku (parity footprint).
- **Theme.kt** (parsial): `typography` di `PromptVaultTheme` jadi 4-cabang
  (GLASSMORPHISM -> `GlassTypography` baru dipisah dari `else`).
  MATERIAL3 sekarang satu2nya pemakai `PromptVaultTypography` via `else`.
  `colorScheme`/`shapes`/`extraColors` GLASSMORPHISM TIDAK ikut berubah --
  scope sengaja cuma typography.
- **Bug ditemukan sendiri & diperbaiki sebelum sempat dikirim**: proses
  `str_replace` pertama utk nyisip `GlassTypography` di `Type.kt`
  TIDAK SENGAJA ikut menghapus baris pembuka javadoc `CupertinoTypography`
  (`/**` + fragmen kalimat pembuka "Skala 'iOS-ish' (angka") krn baris tsb
  kepakai sbg anchor akhir `old_str`, tapi tidak ikut ditulis ulang di
  `new_str`. Efeknya: teks lanjutan javadoc lama jadi floating TANPA
  `/**` pembuka (bakal gagal compile, dianggap kode bukan komentar) + 1
  kurung `)` orphan (unmatched). **Ketahuan LANGSUNG lewat
  `scripts/preflight_check.sh` item #1** (kurung tidak seimbang, delta
  tepat 0/-1 di `Type.kt`) sebelum sempat di-package/dikirim ke user --
  persis fungsi preflight yg dimaksudkan. Diperbaiki dgn nyisip ulang 2
  baris yg hilang; re-run preflight penuh (14/14 ✅) sebelum lanjut.
  Dicatat scr jujur di sini krn ini nyaris jadi ZIP rusak terkirim.
- File diubah (3, PAS batas Micro-Batch): `ui/theme/Type.kt` (+1 val),
  `ui/theme/Theme.kt` (parsial), `ui/theme/GlassTokens.kt` (parsial,
  +1 paragraf supersede, 0 logic diubah).
- **Batas jujur**: BELUM lewat `./gradlew`/device asli -- sanity check
  terbatas pada preflight statis (kurung, YAML, XML, dll) + review manual,
  bukan compile Gradle sungguhan. Hasil visual "kesan diperkuat" tetap
  perlu diverifikasi user di HP.
- **Pending Queue**: kosong.

## [UI][NEUMORPHISM] Aksen ke-4 "Pengaturan" -> Neon Magenta (BR2049), menutup 4/4 sumbu Blade Runner (2026-08-29, lanjutan Blade Runner)
- **Trigger**: lanjutan langsung dari entri "Root cause GitHub Release
  notes stale" di bawah -- user diajukan 4 opsi warna via tombol
  (`ask_user_input_v0`): Neon Magenta / Neon Violet / reuse Amber / reuse
  Teal. User pilih **Neon Magenta** ("khas neon BR2049").
- **Color.kt** (+1 blok, 2 val): `NeoMagenta` (0xFFEAA9C9, hue H330) +
  `NeoMagentaContainer` (0xFF64304A) -- pola PERSIS `CupertinoIndigo`
  (aksen ke-4, HANYA base+container, TANPA on-variant krn
  `VaultExtraColors` cuma 2 field). Kontras: NeoMagenta vs
  SurfaceContainerHighest = 6.84:1 (sejajar Teal 6.20/TealDeep 6.60/Amber
  7.30 punya `NeoAmber` dkk); NeoMagentaContainer vs AppBackground = 1.90:1
  (disamakan presisi ke NeoTealContainer/NeoAmberContainer).
- **Theme.kt** (parsial): `NeumorphismExtra = VaultExtraColors(slate =
  NeoMagenta, slateContainer = NeoMagentaContainer)` -- val baru, pola
  PERSIS `CupertinoExtra`. `LocalVaultExtraColors provides` di
  `PromptVaultTheme` diubah dari ternary `isCupertino` jadi `when`
  3-cabang, pola PERSIS `colorScheme`/`typography`/`shapes`. Var lokal
  `isCupertino` (sudah 0 pemakai lagi) DIHAPUS. Javadoc lama ("aksen ke-4
  TETAP TIDAK berubah utk NEUMORPHISM") diperbarui in-place, bukan
  dihapus.
- **MENUTUP 4/4**: dengan batch ini, SEMUA 4 sumbu identitas visual
  Neumorphism (warna, shape, tipografi, aksen ke-4) sudah lengkap
  kondisional per `themeStyle` -- rangkaian permintaan bertahap sepanjang
  sesi hari ini ("kombinasi warna" -> "shape/typography" -> "aksen ke-4")
  selesai penuh.
- File diubah (2, di bawah batas Micro-Batch 3): `ui/theme/Color.kt`
  (+1 blok), `ui/theme/Theme.kt` (parsial). Preflight `scripts/
  preflight_check.sh` ✅ (14/14, termasuk cek #6 "0 hex leak" -- 2 literal
  baru tetap di dalam `ui/theme`).
- **Docs (VIP, gabung 1 zip)**: `CHANGELOG.md` dapat 1 entri baru
  (di atas segalanya) sesuai fix proses di entri "Root cause..." di bawah
  -- supaya Release CI berikutnya (v1.0.226) langsung akurat, TIDAK
  mengulang insiden stale spt v1.0.225.
- **Batas jujur**: sama seperti batch sebelumnya, BELUM lewat
  `./gradlew`/device asli -- kalibrasi kontras HANYA dihitung via skrip
  Python (rumus luminance relatif WCAG persis), bukan alat aksesibilitas
  Android sungguhan. Hasil visual final tetap perlu diverifikasi user di
  HP.
- **Pending Queue**: kosong.

## [FIX][CI] Root cause GitHub Release notes stale (nampilin batch lama) -- `CHANGELOG.md` tidak ikut ter-update (2026-08-29)
- **Gejala dilaporkan user** (screenshot halaman GitHub Releases): tag
  `v1.0.225` (baru, dibuild 2 menit lalu, commit `8af3d32`) tapi body
  release yang tampil masih judul "[UI] Stacked Cards Effect ...
  (2026-08-28)" -- deskripsi batch LAMA, bukan batch yang barusan di-push.
- **Root cause ditemukan** (baca `.github/workflows/build.yml`, step
  "Extract release notes from CHANGELOG.md", baris ~231): `body_path`
  release DIISI dari `awk '/^## /{n++} n==1' CHANGELOG.md` -- ekstrak
  section `## ` PALING ATAS di **`CHANGELOG.md`**, BUKAN dari
  `PROJECT_STATE.md`. Ini 2 file BEDA dengan tujuan beda (`README.md`
  sudah bilang: `CHANGELOG.md` = "riwayat lengkap tiap versi" (dikonsumsi
  CI), `PROJECT_STATE.md` = log kronologis dev-session mentah). **3 batch
  Neumorphism/Blade Runner berturut-turut** (skema warna 2026-08-29,
  rebalance Amber 2026-08-29, shape+typography 2026-08-29 sesi lalu) SEMUA
  cuma nulis ke `PROJECT_STATE.md` (sesuai rule CHAT FORMAT "Log wajib ke
  PROJECT_STATE.md") -- **0 satupun ikut nambah entri di `CHANGELOG.md`**,
  jadi section teratas file itu tetap section lama tgl 2026-08-28, dan CI
  terus-menerus publish body itu yang STALE ke tiap release baru walau
  build-nya sendiri sukses & APK-nya benar berisi kode terbaru (APK-nya
  TIDAK stale, cuma teks deskripsi release-nya yang salah/usang).
- **Bukan bug di workflow YAML** -- logic `awk` di CI 100% jalan sesuai
  desain aslinya (fix 2026-08-22, lihat komentar di file itu sendiri).
  Gap-nya murni proses: rule "Log wajib ke PROJECT_STATE.md" (CHAT FORMAT)
  ternyata TIDAK otomatis mencakup `CHANGELOG.md`, padahal CI diam-diam
  bergantung penuh ke file itu utk isi Release.
- **Fix** (diterapkan batch berikutnya, digabung 1 zip dgn kerjaan aksen
  ke-4): backfill 3 entri `CHANGELOG.md` yang kelewat (skema warna, rebalance
  Amber, shape+typography) + entri baru batch aksen ke-4, SEMUA disisipkan
  di ATAS section "Stacked Cards Effect ... (2026-08-28)" yang lama (format
  singkat ala `CHANGELOG.md`, BUKAN disalin mentah dari narasi panjang
  `PROJECT_STATE.md`).
- **Praktik ke depan** (dicatat di sini, BUKAN rule PINNED baru -- itu
  wewenang eksplisit user, lihat rule #1/#2 di atas): tiap batch yang
  benar-benar di-`git push` ke `main` (bakal ke-trigger CI) sebaiknya JUGA
  dapat 1 entri baru singkat di `CHANGELOG.md` (bukan cuma
  `PROJECT_STATE.md`), supaya `body_path` Release CI berikutnya selalu
  ikut versi commit yang sedang di-build, bukan republish entri lama.

## [UI][NEUMORPHISM] Shape + Typography ala Blade Runner, menyusul skema warna (2026-08-29, lanjutan Blade Runner)
- **Trigger**: user eksplisit minta "terapkan shape/typography ala Blade
  Runner, seperti pada tema warnanya" -- menyusul `NeumorphismColors`
  ("Teal & Amber Blade Runner", 2 entri di bawah) yang sudah lebih dulu
  ada tapi saat itu SENGAJA scope dipersempit cuma warna (lihat javadoc
  lama `Theme.kt`, sekarang diperbarui bukan dihapus).
- **Shape** (`ui/theme/Shapes.kt`, +1 val `NeumorphismShapes`): keluarga
  `CutCornerShape` (sudut potong lurus/chamfer) -- kebalikan arah
  `CupertinoShapes` (bulat/lembut). Nilai dp per tingkat SENGAJA disamakan
  PERSIS `PromptVaultShapes` (4/8/12/16/28, skala M3 baku) -- "parity
  footprint", 0 resiko ukuran komponen berubah di layar manapun saat
  pindah gaya, cuma keluarga potongannya yang beda.
- **Typography** (`ui/theme/Type.kt`, +1 val `NeumorphismTypography`):
  role display/headline/titleLarge/label* pakai `CodeFont` (monospace,
  reuse token lama, 0 font pihak ketiga baru) + weight lebih tebal
  (Bold/SemiBold) + letter-spacing POSITIF lebar -- kesan "signage
  neon/HUD terminal" khas kota Blade Runner. Role titleMedium/titleSmall/
  body* TETAP `Sans` (`FontFamily.Default`) demi keterbacaan teks isi
  (nama file, deskripsi rule). Semua ukuran/line-height 100% reuse
  `PromptVaultTypography` (0 diketik ulang) -- hirarki M3 otomatis aman.
- **Wiring** (`ui/theme/Theme.kt`, parsial): `typography`/`shapes` di
  `PromptVaultTheme` diubah dari ternary boolean `isCupertino` jadi
  `when (themeStyle)` 3-cabang, pola PERSIS `colorScheme` yang sudah lebih
  dulu 3-cabang -- NEUMORPHISM -> `NeumorphismShapes`/`NeumorphismTypography`,
  CUPERTINO tetap `Cupertino*`, sisanya (GLASSMORPHISM/MATERIAL3) tetap
  `PromptVault*`, 0 berubah. Javadoc lama di atas fungsi (yang bilang
  "shapes/typography TIDAK ikut berubah utk NEUMORPHISM") diperbarui
  in-place (bukan dihapus) supaya tidak jadi info usang.
- **TIDAK disentuh**: `LocalVaultExtraColors` (aksen ke-4 "Pengaturan")
  TETAP `VaultExtra` -- di luar permintaan sesi ini (cuma shape+tipografi).
  `NeumorphismColors`/`NeumorphTokens` (fill/border/stacked-cards) 0
  berubah -- identitas warna Blade Runner yang sudah ada sebelumnya
  tidak disentuh, sesi ini murni menyusulkan 2 sumbu visual yang tadinya
  ditunda.
- File diubah (3, PAS batas Micro-Batch): `ui/theme/Shapes.kt` (+1 val),
  `ui/theme/Type.kt` (+1 val), `ui/theme/Theme.kt` (parsial: 2 blok
  ternary -> `when`, 1 javadoc diperbarui). `FILE_MANIFEST.txt`/
  `versionCode`/`versionName` TIDAK disentuh (Rule PINNED #1).
- **Batas jujur**: BELUM PERNAH lewat `./gradlew`/device asli -- sanity
  check sesi ini terbatas pada pemeriksaan sintaks (kurung/brace seimbang)
  di 3 file, bukan compile Gradle sungguhan. Hasil visual final (apakah
  "kesan Blade Runner" pada shape/tipografi sudah sesuai ekspektasi user)
  tetap perlu diverifikasi user di HP.
- **Pending Queue**: kosong -- 3 file di batch ini sudah menuntaskan
  permintaan sesi ini secara utuh, 0 sisa pekerjaan menggantung.

## [UI][NEUMORPHISM] Rebalance porsi Amber -- CTA "Scan Sekarang" (2026-08-29, lanjutan Blade Runner)
- **Lanjutan entri di bawah** (skema warna Blade Runner) -- user lapor via
  screenshot kedua: "Ternyata lebih dominan warna teal daripada amber nya"
  di gaya Neumorphism. Ditanya balik cara rebalance (3 opsi via
  `ask_user_input_v0`), user pilih: "Ya, kasih amber porsi lebih besar
  (mis. tombol 'Scan Sekarang' jadi amber)".
- **Root cause dominasi teal**: bukan soal hue/saturasi warna (Amber
  malah kontras LEBIH tinggi drpd Teal, 7.30:1 vs 6.20:1, lihat entri
  bawah) -- murni soal SEBARAN PEMAKAIAN. `colors.primary` (teal) dipakai
  di elemen BESAR (segmented control "Beranda"/"Tampilan", kartu ikon
  "Kelola Rule"/"Statistik", CTA "Scan Sekarang"), sedangkan
  `colors.tertiary` (amber) cuma di ikon KECIL (clock/history/help,
  16-20dp) -- jumlah pemakaian di `HomeScreen.kt` sebenarnya cukup
  seimbang (5 primary vs 5 tertiary), tapi BOBOT VISUAL (luas
  permukaan/elemen filled besar) jomplang ke teal.
- **Fix** (`ui/screens/HomeScreen.kt`, 1 file): tombol CTA "Scan Sekarang"
  (elemen paling menonjol di Home, filled penuh lebar layar) -- SEBELUMNYA
  `color = colors.primary` (teal) DI SEMUA GAYA, sekarang KHUSUS
  Neumorphism (`VaultTheme.style == ThemeStyleOption.NEUMORPHISM`) pakai
  `colors.tertiary`/`colors.onTertiary` (amber). 3 gaya lain
  (Glass/M3/Cupertino) TETAP `colors.primary`/`colors.onPrimary`, 0
  berubah -- scope asli tetap "khusus Neumorphism only".
- **TIDAK disentuh** (1 batch = 1 perubahan paling berdampak, bukan
  rombak semua elemen sekaligus): segmented control, ikon "Kelola Rule"/
  "Statistik" TETAP teal -- kalau user masih merasa kurang seimbang
  setelah lihat hasil ini, bisa lanjut minta elemen lain menyusul.
- File diubah (1, dalam batas Micro-Batch): `ui/screens/HomeScreen.kt`
  (parsial: 1 import `ThemeStyleOption` + blok CTA). `FILE_MANIFEST.txt`/
  `versionCode`/`versionName` TIDAK disentuh (Rule PINNED #1).
- **Batas jujur**: BELUM PERNAH lewat `./gradlew`/device asli -- hasil
  visual final (apakah proporsi ini sudah "cukup seimbang" menurut mata
  user) tetap perlu diverifikasi user di HP, bukan cuma diasumsikan dari
  kalkulasi ini.
- **User WAJIB verifikasi di HP**: (1) build CI hijau, (2) gaya
  Neumorphism -> tombol "Scan Sekarang" HARUS amber (bukan teal lagi),
  teks/spinner di dalamnya HARUS tetap terbaca jelas, (3) elemen lain
  (segmented control, ikon "Kelola Rule"/"Statistik") TETAP teal seperti
  sebelumnya, (4) 3 gaya lain (Glass/M3/Cupertino) -> tombol "Scan
  Sekarang" HARUS TETAP warna primary biasa (0 regresi).

## [UI][NEUMORPHISM] Skema warna baru "Teal & Amber (Blade Runner)" -- khusus gaya Neumorphism (2026-08-29)
- **Instruksi eksplisit user**: "Terapkan kombinasi warna: Teal & Amber
  (Blade Runner). khusus theme Neumorphism only!!" -- lampiran screenshot
  Home (gaya Neumorphism aktif, kartu stacked-cards). Scope EKSPLISIT
  cuma 1 gaya (Neumorphism) -- Glassmorphism/Material3/Cupertino WAJIB 0
  berubah.
- **Pola yang diikuti**: SAMA PERSIS precedent restyling Cupertino murni
  (2026-08-27/28, lihat log di bawah) -- skema warna kondisional per
  `themeStyle`, neutral/background/surface/error/outline 100% REUSE token
  lama (0 token baru di situ), cuma slot AKSEN (primary/secondary/
  tertiary) yang diganti. `PromptVaultTheme()` (`Theme.kt`) diubah dari
  cabang boolean `isCupertino` jadi `when(themeStyle)` 3-cabang utk
  `colorScheme` SAJA -- `shapes`/`typography`/`LocalVaultExtraColors`
  TIDAK ikut kondisional baru (TETAP sama dgn Glass/M3 utk Neumorphism,
  user cuma minta "kombinasi warna", bukan rombak shape/tipografi/aksen
  ke-4 spt restyling Cupertino).
- **Palet baru** (`Color.kt`, blok `NeoTeal`/`NeoTealDeep`/`NeoAmber` + 4
  varian on/container tiap satu, methodology WCAG identik seluruh file --
  dicari via script kalkulasi kontras Python, BUKAN tebakan visual):
  - Primary = Teal H187 (`#4BC2D2`, cyan-teal terang) -- kontras vs
    `SurfaceContainerHighest` 6.20:1.
  - Secondary = Teal-hijau H172 (`#7FC5BC`, varian lebih teduh) -- 6.60:1.
  - Tertiary = Amber/oranye H32 (`#EAB980`, lebih hangat & jenuh dari
    amber-warning M3 baku H42) -- 7.30:1.
  - Semua pasangan on*/container* 7.31-8.00:1 (AAA). Container vs
    `AppBackground` disamakan presisi ke ~1.90:1 (setara tier existing
    PrimaryContainer 1.82:1) -- dicari eksplisit krn hue teal secara
    persepsi jauh LEBIH TERANG dari hue biru pada L yang sama (bobot
    channel hijau formula WCAG jauh lebih besar), jadi TIDAK bisa reuse
    L mentah dari resep [Primary]/[Tertiary] lama, harus dihitung ulang.
  - Error/outline/neutral: 100% REUSE (`ErrorRed`/`Outline`/dst), 0 token
    baru. Aksen ke-4 "Pengaturan" (`VaultExtraColors.slate`): TETAP
    `SettingsAccent` (indigo) lama, SENGAJA tidak diganti -- scope
    diminta cuma "Teal & Amber" 2 warna, bukan rombak 4 slot spt
    Cupertino; user bisa minta lanjutan kalau mau aksen ke-4 disesuaikan.
- **Titik krusial yg DITELUSURI (bukan cuma ganti `ColorScheme`)**:
  `NeumorphTokens.kt` (`FillHighlightTint`, tint gradient kiri-atas yg
  bikin kartu Neumorphism "kerasa ikatan warna brand" -- fitur eksplisit
  dari v8.27.0 lama) SEBELUMNYA HARDCODE ke `Primary` (biru lama), BUKAN
  baca `MaterialTheme.colorScheme.primary` dinamis -- kalau tidak
  ditelusuri & ikut diganti, kartu Neumorphism akan TETAP kerasa biru
  walau tombol/teks sekitarnya sudah teal (identitas setengah-setengah).
  Diverifikasi via grep: `NeumorphTokens.*` HANYA dikonsumsi di
  `TactileSurface.kt` di dalam cabang
  `if (style == ThemeStyleOption.NEUMORPHISM)` -- 100% eksklusif
  Neumorphism, aman diganti jadi literal `NeoTeal` langsung (TIDAK perlu
  diubah jadi `@Composable`/dinamis, toh cuma pernah dibaca dlm konteks
  itu). WCAG blend alpha=0.20 (nilai alpha TIDAK diubah) dihitung ULANG:
  composite (51,78,88) -> 5.12:1 vs `TextSecondary` (AA, nyaris identik
  margin nilai lama 5.13:1).
- **TIDAK disentuh** (Zero-Unnecessary-Refactor + hormati keputusan
  eksplisit user sebelumnya): `NeumorphTokens.Platinum`/`borderBrush()`
  (border diagonal kartu) -- SENGAJA netral/blend, keputusan eksplisit
  user v8.28.4 ("lebih cocok pakai tone warna yang nyaru"), BUKAN bagian
  identitas 2-warna yang diminta sesi ini, jadi TETAP dibiarkan;
  `CupertinoColors`/`PromptVaultColors` (Glass/M3/Cupertino) 0 baris
  berubah; `versionCode`/`versionName` di `app/build.gradle.kts` TIDAK
  disentuh sama sekali (Rule PINNED #1 di atas -- 100% otomatis
  `GITHUB_RUN_NUMBER`, bukan sesi ini yang menentukan angkanya).
- File diubah (3, dalam batas Micro-Batch): `ui/theme/Color.kt` (parsial,
  1 blok baru + 3x4 token warna), `ui/theme/Theme.kt` (parsial, 1
  `ColorScheme` baru + `when` 3-cabang di `PromptVaultTheme()`),
  `ui/theme/NeumorphTokens.kt` (parsial, 1 baris `FillHighlightTint` +
  KDoc). `FILE_MANIFEST.txt` TIDAK berubah (0 file baru/dihapus).
- **Batas jujur**: seperti seluruh riwayat project, **BELUM PERNAH lewat
  `./gradlew`/device asli** -- kalkulasi WCAG diverifikasi via script
  Python terpisah (bukan cuma eyeball), tapi hasil visual sesungguhnya
  (apakah kombinasi Teal+Amber ini benar "kerasa Blade Runner" di layar
  asli, bukan cuma di atas kertas kontras) BELUM diverifikasi user.
- **User WAJIB verifikasi di HP**: (1) build CI hijau, (2) di
  Pengaturan/Tampilan pilih gaya "Neumorphism" -> Beranda & semua layar
  lain HARUS terlihat teal (tombol "Scan Sekarang", ikon, switch aktif)
  + amber (ikon warning/highlight) menggantikan biru+amber-redup lama,
  (3) kartu (VaultCard/manifest Beranda dll) tint kiri-atas HARUS terasa
  teal, BUKAN biru lagi, (4) ganti ke gaya Glassmorphism/Material3/
  Cupertino -> HARUS TETAP seperti sebelumnya, 0 perubahan (Teal & Amber
  CUMA utk Neumorphism), (5) teks tetap terbaca jelas di semua kombinasi
  (WCAG sudah dihitung tapi verifikasi visual asli tetap final call).

## [UI] Stacked Cards Effect kiri-atas/3-lapis -- diperluas ke SEMUA VaultCard (2026-08-28, lanjutan batch sebelumnya)
- **Konteks**: batch sebelumnya (lihat entri di bawah) sengaja SCOPE efek
  baru cuma ke 1 kartu (manifest Home) krn efek itu butuh inset 28dp/kartu
  yang bakal melebarkan jarak antar-item di 2 `LazyColumn` rapat
  (`RuleListScreen`, `ActivityLogScreen`, `spacedBy(4.dp)`). User lapor
  screenshot: "kenapa cuman 1 yang dapat" -- ditanya balik via pilihan
  (bukan diasumsikan), user PILIH eksplisit: **"Semua VaultCard, termasuk 2
  list rapat (jarak antar-item bakal melebar)"** -- trade-off DIKETAHUI &
  DITERIMA user sendiri, bukan asumsi sepihak Claude.
- **`ui/components/VaultCard.kt`** (parsial, 1 baris param): `stackedCards
  = true` -> `stackedCardsTopLeft = true`. Krn `VaultCard` dipakai >10
  layar, perubahan 1 baris ini otomatis menjalar ke SEMUA caller-nya (efek
  lama kanan-bawah/1-lapis sekarang tidak dipakai di mana pun lagi).
- **`ui/screens/HomeScreen.kt`** (parsial): kartu manifest dibalik dari
  pemanggilan `TactileSurface` langsung (workaround khusus batch
  sebelumnya) balik ke `VaultCard()` polos -- sekarang `VaultCard` sendiri
  sudah bawa efek yang sama, workaround itu jadi tidak perlu lagi. Import
  `VaultCard` dikembalikan.
- **`ui/theme/NeumorphTokens.kt` / `ui/components/TactileSurface.kt`**: 0
  disentuh batch ini -- token/fungsi `stackedCards()`/`StackedCardOffset`/
  `StackedCardColors` (varian lama) SENGAJA DIBIARKAN ada sbg kode tak
  terpakai (0 caller lagi setelah `VaultCard.kt` pindah ke
  `stackedCardsTopLeft`), TIDAK dihapus paksa -- alasan: infrastruktur
  varian baru (`stackedCardsTopLeft`/`StackedCardOffsetTopLeft`/dst) sudah
  siap dari batch sebelumnya, batch ini murni soal WHO memanggilnya, bukan
  soal implementasi ulang.
- **Dampak visual DIKETAHUI user**: `RuleListScreen` (list Kelola Rule) &
  tab Undo `ActivityLogScreen` sekarang jarak antar-kartunya lebih renggang
  drpd sebelumnya (tiap kartu +28dp ruang atas & lebih sempit di kiri utk
  lapis yang mengintip) -- INI SENGAJA, bukan bug, sesuai pilihan user.
- **Belum diverifikasi build CI/device nyata** (sandbox sesi ini tanpa
  akses jaringan Gradle) -- preflight_check.sh lolos bersih (14/14).
- File diubah (2, dalam batas Micro-Batch): `ui/components/VaultCard.kt`
  (parsial, 1 baris param), `ui/screens/HomeScreen.kt` (parsial, revert 1
  call site + import).

## [UI] Stacked Cards Effect -- arah dibalik ke kiri-atas, 3 lapis, khusus kartu manifest Home (2026-08-28)
- **Instruksi user**: screenshot kartu manifest/statistik Home (tema
  Neumorphism) + teks "Ubah arah stacked card effect agar menghadap ke kiri
  atas dengan 3 layer bertumpuk, tanpa offset/truncated (tambahkan
  inset/sejenisnya) gak boleh terlihat nyaru sama sekali!!".
- **Keputusan arsitektur (WAJIB dibaca sebelum sentuh fitur ini lagi)**:
  efek "stacked cards" lama (`NeumorphTokens.stackedCards()` /
  `TactileSurface(stackedCards=...)`) dipakai BERSAMA lewat `VaultCard` di
  >10 layar -- termasuk 2 `LazyColumn` RAPAT (`RuleListScreen` via
  `RuleCard`, `ActivityLogScreen` langsung, keduanya `Arrangement.
  spacedBy(4.dp)`). Nimpa token/fungsi itu langsung dgn versi 3-lapis +
  inset besar (perlu ~28dp ruang kosong per kartu utk "tanpa terpotong")
  bakal bikin KEDUA list itu mendadak sangat renggang -- regresi nyata di
  layar yang bahkan tidak disinggung user. Fix: dibuat opt-in KEDUA yang
  100% TERPISAH (`stackedCardsTopLeft`), token lama 0 disentuh/0 berubah
  nilai, dipasang HANYA di 1 titik: kartu manifest/statistik `HomeScreen.kt`
  (kartu yang difoto user) -- dipanggil via `TactileSurface(...)` LANGSUNG
  (bukan lewat `VaultCard`, yang tetap pakai efek lama utk semua caller lain
  apa adanya) supaya efek baru tidak menjalar ke `VaultCard.kt` sama sekali.
- **`ui/theme/NeumorphTokens.kt`** (parsial, HANYA nambah, 0 baris lama
  dihapus/diubah nilainya): token/fungsi baru `StackedCardOffsetTopLeft`
  (8dp/lapis), `StackedCardInsetTopLeft` (28dp = 3x8dp + margin aman 4dp),
  `StackedCardColorsTopLeft` (3 warna reuse existing, menaik terang makin
  jauh dari kartu: `SurfaceContainerHigh` -> `SurfaceContainerHighest` ->
  `Outline`, supaya lapis PALING JAUH -- paling terpapar ke `AppBackground`
  polos -- paling kontras & tidak "nyaru"), `Modifier.stackedCardsTopLeft()`
  (geometri identik `stackedCards()` lama, cuma tanda offset dibalik jadi
  `Offset(-shift, -shift)`).
- **`ui/components/TactileSurface.kt`** (parsial): parameter opt-in baru
  `stackedCardsTopLeft: Boolean = false` (default 0 dampak). Cabang
  NEUMORPHISM: kalau `true` (dan `!recessed`), `Modifier.padding(top =
  StackedCardInsetTopLeft, start = StackedCardInsetTopLeft)` ditempel
  SEBELUM `.stackedCardsTopLeft()` di modifier chain yang SAMA dgn
  `Surface` (pola aman identik `stackedCards()` lama -- BUKAN `Box`
  pembungkus baru, tidak mengulang regresi `weight()`/`align()` v8.28.0)
  -- padding ini yang bikin lapis kiri-atas gambar DI DALAM ruang yang
  sudah dialokasikan node itu sendiri ke parent, bukan bocor ke sibling
  atau kepotong tepi layar ("tanpa offset/truncated" sesuai instruksi).
  Cabang `stackedCards` lama (dan cabang MATERIAL3/CUPERTINO/Glass) 0
  diubah.
- **`ui/screens/HomeScreen.kt`** (parsial): 1 pemanggilan `VaultCard(...)`
  (kartu manifest/statistik, satu-satunya `VaultCard` di file ini) diganti
  `TactileSurface(...)` langsung dgn `shape`/`color`/`elevation` DISALIN
  PERSIS dari `VaultCard.kt` (supaya visual identik di luar arah
  stacked-card) + `stackedCardsTopLeft = true`. Import `VaultCard` yang jadi
  tidak terpakai ikut dihapus. Isi `content` (4x `ManifestRow` + kondisional
  `lastScanSummary`/`hasSkippedFiles`) 0 diubah sama sekali.
- **`ui/components/VaultCard.kt`**: **0 disentuh sama sekali** (dicek ulang
  post-batch, diff kosong) -- semua caller lain (`RuleCard`, `GroupedListRow`,
  `AddEditRuleScreen`, `ActivityLogScreen`, `StatisticsScreen`,
  `SkippedFilesScreen`, `PanduanScreen`, `DiagnosticsScreen`,
  `SettingsScreen`) TIDAK terdampak batch ini, efek stacked-card mereka
  tetap arah kanan-bawah 1-lapis spt sebelumnya.
- **Belum diverifikasi build CI/device nyata** (sandbox sesi ini tanpa
  akses jaringan Gradle, tidak bisa kompilasi lokal) -- hanya review manual
  menyeluruh tiap file yang diubah (brace balance dicek, semua simbol baru
  ditelusuri sampai ke definisinya). Minta user konfirmasi build APK CI
  sukses & tampilan kartu Home di HP sesuai sebelum dianggap selesai total.
- File diubah (3, dalam batas Micro-Batch): `ui/theme/NeumorphTokens.kt`
  (parsial, additive), `ui/components/TactileSurface.kt` (parsial),
  `ui/screens/HomeScreen.kt` (parsial, 1 call site).

## [INSIDEN+FIX] CI merah lagi -- fix "Read app version" (sesi lalu) ternyata TIDAK PERNAH sampai ke repo asli (2026-08-28)
- **Laporan user**: upload ZIP log Actions asli (`logs_89757911596.zip`),
  pesan "?!!". Isi: job `build` gagal PERSIS di step **"Read app version"**,
  exit code 1 -- `assembleRelease` TIDAK PERNAH jalan (`app/build/outputs/
  apk/release/` tidak ada, dikonfirmasi step diagnostik).
- **Root cause (dari log asli)**: baris `VERSION=$(grep -oP 'versionName =
  "\K[^"]+' app/build.gradle.kts)` di `.github/workflows/build.yml` -- SAMA
  PERSIS root cause yg SUDAH didiagnosis & "difix" sesi sebelumnya (lihat
  entri `[INSIDEN+FIX] CI merah total pasca-governance versioning otomatis`
  di bawah). Bedanya: fix itu **TIDAK PERNAH benar2 sampai ke repo**.
- **Kenapa fix lama hilang (ditemukan lewat audit sandbox sesi ini, bukan
  tebakan)**: `.github/workflows/*` & `.gitignore` adalah dotfile -- SEMUA
  skrip Termux [DAILY UPDATE] SENGAJA mengecualikannya dari langkah
  bersih-bersih (`! -name '.*'`), justru supaya file ini AMAN kalau ZIP sesi
  mana pun tidak menyertakannya (tetap utuh, tidak ikut kehapus). Konsekuensi
  sisi lain: kalau sebuah ZIP KEBETULAN menyertakan `.github/...` versi LAMA
  (mis. sandbox Claude sesi tsb "mewarisi" file basi dari sesi sebelumnya,
  BUKAN dari ZIP sumber user yg diupload sesi itu), `unzip -o` bakal
  MENIMPA file yg sudah benar di repo asli dengan versi basi itu -- silent
  regression, 0 error yg kelihatan saat push krn `git commit`/`push` tetap
  sukses (isinya cuma "berubah balik" ke versi lama, bukan gagal).
- **Diverifikasi di sandbox sesi ini**: ekstraksi ULANG bersih ZIP sumber
  sesi SEBELUMNYA (`PromptVault_v1_0_218.zip`) ke folder kosong baru
  konfirmasi ZIP itu **TIDAK PERNAH berisi** `.github/` atau `.gitignore`
  sama sekali -- tapi folder kerja sandbox sesi tsb PUNYA keduanya (basi,
  regex lama). ZIP hasil sesi lalu (`PromptVault_v1_0_219.zip`, batch
  Cupertino warna sistem) dizip dari folder kerja yg sudah tercemar itu --
  artinya kemungkinan besar ZIP tsb ikut membawa `.github/workflows/
  build.yml` basi & menimpa balik fix yg sudah ada di repo user saat
  di-push via [DAILY UPDATE]. **Insiden murni proses/tooling sandbox, bukan
  kesalahan di kode aplikasi Cupertino batch itu sendiri** (`Color.kt`/
  `Theme.kt`/`CupertinoTokens.kt` batch itu tidak tersentuh isu ini).
- **Fix (ulang, sesi ini)**: `.github/workflows/build.yml` -- baris sama
  diganti `VERSION="1.0.$GITHUB_RUN_NUMBER"` (identik formula fix sesi
  lalu, lihat entri di bawah utk alasan lengkap kenapa formula ini yg
  dipilih). 16 step lain 0 disentuh.
- **Pencegahan ke depan (WAJIB dipatuhi sesi manapun)**: sebelum
  packaging ZIP akhir, sesi WAJIB verifikasi working folder BUKAN hasil
  `unzip -o` ke folder yang sudah terisi dari sesi lain sebelumnya (cek
  `.github`/dotfile lain TIDAK muncul kalau ZIP sumber sesi ini memang
  tidak menyertakannya) -- kalau ada dotfile "misterius" yg tidak berasal
  dari ZIP sumber sesi ini, JANGAN ikut di-zip ke output kecuali memang
  sengaja mau menimpa (spt insiden fix `.github` di batch ini).
- File diubah (1): `.github/workflows/build.yml` (parsial, 1 baris, protected
  file -- restorasi fix yg sudah pernah dapat izin eksplisit user, bukan
  perubahan baru).
- **CHANGELOG.md**: entri baru ditambah paling atas batch ini juga.

## [CUPERTINO] Warna sistem iOS -- tutup pending item TERAKHIR (ke-3 dari 3), restyling Cupertino murni SELESAI (2026-08-28)
- **Instruksi user**: "lanjut fase terakhir penyempurnaan theme Cupertino
  style murni!!" -- cocok PERSIS dgn 1 item tersisa di log batch di bawah
  ("warna sistem iOS, `systemBlue` dst"), jadi TIDAK perlu audit generik
  ulang dari nol, langsung ke item itu. Tapi item ini sendiri (per catatan
  batch sebelumnya) butuh 1 langkah audit dulu sebelum eksekusi: cek
  apakah ada kode yg hardcode `Color(0x...)` di luar package `ui/theme`
  yang bisa BYPASS swap kondisional (beda dari shapes/typography yg
  amannya sudah pasti krn cuma 1 titik pakai `MaterialTheme.shapes`/
  `.typography`). Hasil grep: **0 hasil** -- seluruh app 100% konsumsi
  warna lewat `MaterialTheme.colorScheme.*`/`VaultTheme.extraColors`, jadi
  full-swap kondisional aman, pola identik shapes/typography.
- **Sumber hue**: nilai publik resmi Apple HIG (dark appearance) --
  systemBlue #0A84FF, systemTeal #64D2FF, systemOrange #FF9F0A, systemRed
  #FF453A, systemIndigo #5E5CE6. **Tone (S/L) TIDAK dipakai mentah**:
  dihitung ulang, systemBlue mentah cuma 3.58:1 vs
  `SurfaceContainerHighest` (app ini), GAGAL syarat WCAG >=4.5:1 yang
  sudah di-lock sejak v8.0.0 (nilai Apple dikalibrasi utk `systemBackground`
  iOS asli yg jauh lebih gelap dari surface app ini). Tone di-re-derive
  per hue Apple dgn pola IDENTIK cara `Primary`/`Tertiary`/dst diturunkan
  dulu (S/L pastel dark-scheme M3) -- kontras final semua lulus AA
  (5.24-7.33:1 foreground, 6.75-8.37:1 on-fill/on-container). Detail
  perhitungan lengkap tiap warna: javadoc di `Color.kt`.
- **`Color.kt`**: 18 val baru (`CupertinoBlue`/`OnBlue`/`BlueContainer`/
  `OnBlueContainer` dst utk Blue/Teal/Orange/Red, + `CupertinoIndigo`/
  `IndigoContainer` utk aksen ke-4) -- `PromptVaultColors` (skema M3 calm
  lama, dipakai 3 gaya lain) 0 baris berubah.
- **`Theme.kt`**: `CupertinoColors` (`ColorScheme` baru, `darkColorScheme`)
  -- primary/secondary/tertiary/error diisi 4 warna sistem Apple di atas,
  SEMUA slot neutral/surface/background 100% REUSE token
  `PromptVaultColors` (0 token baru di sisi neutral -- "warna sistem" cuma
  soal aksen, bukan rombak background). `CupertinoExtra` (varian kedua
  `VaultExtraColors`, `slate` = `CupertinoIndigo`). `colorScheme` &
  `LocalVaultExtraColors` di `PromptVaultTheme` KINI kondisional per
  `themeStyle` -- pola identik PERSIS baris `shapes`/`typography` yg sudah
  ada (var lokal `isCupertino` ditambah biar 3 baris kondisional tidak
  duplikasi pemanggilan `==` yang sama 3x).
- **`CupertinoTokens.kt`**: javadoc "Belum dikerjakan" diperbarui -- 0 item
  wajib tersisa dari checklist restyling awal (typography, custom dialog,
  warna sistem -- SEMUA 3 sudah tertutup). Dicatat eksplisit: penghalusan
  lanjutan ke depan sifatnya iteratif/opsional (pola sama Neumorphism/
  Glassmorphism), BUKAN checklist wajib baru.
- **TIDAK disentuh** (Zero-Unnecessary-Refactor): `PromptVaultColors`
  (0 baris berubah -- 3 gaya lain 100% identik visualnya spt sebelum batch
  ini), semua composable yg MEMAKAI `MaterialTheme.colorScheme.*`/
  `VaultTheme.extraColors.slate` (`VaultActionSheet`, `WarningBanner`,
  `RuleCard`, `SegmentedControl`, `VaultTopBar`, `GroupedListRow`, dst) --
  otomatis ikut berubah lewat `MaterialTheme`/`CompositionLocalProvider`
  global, 0 titik pemanggilan manual yang perlu disentuh satu-satu.
  `NeumorphTokens.kt` (referensi `Primary`/`Tertiary` mentah di situ
  SENGAJA tetap brand-blue lama, khusus gaya Neumorphism, di luar scope
  Cupertino -- dicek, tidak collide).
- **Verifikasi statis**: keseimbangan `{}`/`()` ketiga file kode SEIMBANG
  (`Color.kt` 0/0 & 115/115; `Theme.kt` 6/6 & 52/52; `CupertinoTokens.kt`
  1/1 & 24/24).
- File diubah (3, pas batas Micro-Batch): `ui/theme/Color.kt` (parsial,
  tambah 18 val baru), `ui/theme/Theme.kt` (parsial, tambah `CupertinoColors`/
  `CupertinoExtra` + 2 baris kondisional), `ui/theme/CupertinoTokens.kt`
  (parsial, javadoc only, 0 logic).
- **CHANGELOG.md**: entri baru ditambah paling atas batch ini juga.
- **Pending Queue (tidak berubah)**: `FileSorter.kt` refactor (item lama,
  tidak terkait Cupertino, lihat log lebih bawah) -- SATU-SATUNYA item
  pending tersisa di project ini sekarang; restyling Cupertino murni tahap
  awal (3/3 item) TUNTAS.

## [CUPERTINO] Typography scale iOS-ish -- tutup pending item ke-2 dari 3, tinggal "warna sistem" (2026-08-27)
- **Instruksi user**: "sempurnakan Cupertino style murni!!" -- generik lagi,
  diaudit dulu (bukan Fast-Track). 2 item pending tersisa di javadoc
  `CupertinoTokens.kt` sblm batch ini: typography scale iOS-ish, warna
  sistem (`systemBlue` dst). Dipilih **typography** krn ada preseden teknik
  identik yg tinggal direplikasi: `shapes` di `Theme.kt` SUDAH kondisional
  per `themeStyle` sejak v8.31.2 (`CupertinoShapes` vs `PromptVaultShapes`)
  -- typography tinggal pola yang SAMA PERSIS, scope 1 batch jelas & aman.
  Warna sistem TIDAK dipilih krn butuh audit tiap titik pemakaian warna
  lintas app dulu (brand-new kerja, bukan replikasi pola) -- di luar 1 batch.
- **`Type.kt`**: `CupertinoTypography` (BARU, val ke-2 di file yg sama --
  BUKAN file baru, jadi `FILE_MANIFEST.txt` TIDAK perlu diubah) memetakan
  15 role `Typography(...)` M3 ke skala HIG Apple (size class Large --
  angka publik dari dokumentasi resmi Apple, bukan reverse-engineer aset
  berlisensi apa pun): Large Title 34sp Bold s/d Caption 2 11sp Medium.
  Tracking HIG SENGAJA dipertahankan NON-linear (beda dari M3 yg linear
  makin positif ke size kecil): POSITIF tipis (+0.35..+0.38sp) di size
  besar 20-34sp, NEGATIF (-0.24..-0.41sp) di size "workhorse" 15-17sp
  (Headline/Body/Callout -- inilah kesan rapat khas SF Pro), balik ke
  ~0/+0.07sp di size kecil (Footnote/Caption). Hirarki ukuran PER ROLE M3
  (mis. `headlineLarge >= headlineMedium >= headlineSmall`) tetap dijaga
  non-menurun walau beberapa role SENGAJA reuse size HIG yang sama (HIG
  cuma py 11 role, M3 py 15 slot) -- 0 asumsi call site lain yang jebol.
  `FontWeight.SemiBold` dipakai di Headline/nav-title-tier (3 role: `title
  Large`, `headlineMedium`, `headlineSmall`), `Bold` di `displayLarge`
  (match gaya "besar-tebal" javadoc lama `Type.kt` sblm v8.0.0), sisanya
  `Normal`/`Medium` (label tier, konvensi M3 role label = Medium).
- **`Theme.kt`**: `typography = if (themeStyle == CUPERTINO) CupertinoTypography
  else PromptVaultTypography` -- 1 baris baru persis di sebelah baris
  `shapes` yang sudah ada (pola identik, 0 refactor lain di fungsi ini).
- **`CupertinoTokens.kt`**: javadoc "Belum dikerjakan" diperbarui -- 2 item
  ditutup dipindah ke daftar "Progres murni" (typography batch ini, custom
  dialog dari batch SEBELUMNYA yang sempat kelewat update di javadoc ini),
  tinggal 1 item tersisa ("warna sistem").
- **TIDAK disentuh** (Zero-Unnecessary-Refactor): `PromptVaultTypography`
  (0 baris berubah -- 3 gaya lain 100% identik visualnya spt sebelum batch
  ini), `CupertinoShapes`/`PromptVaultShapes` (dipakai ulang apa adanya,
  cuma dibaca sbg referensi pola), semua composable yg MEMAKAI
  `MaterialTheme.typography.*` (Text, dst di seluruh app) -- otomatis ikut
  berubah lewat `MaterialTheme` global, 0 titik pemanggilan manual yang
  perlu disentuh satu-satu.
- **Verifikasi statis**: keseimbangan `{}`/`()` ketiga file kode SEIMBANG
  (`Type.kt` 0/0 & 62/62 -- file ini emang 0 kurung kurawal, semua fungsi
  top-level val; `Theme.kt` 6/6 & 35/35; `CupertinoTokens.kt` 1/1 & 20/20).
- File diubah (3, pas batas Micro-Batch): `ui/theme/Type.kt` (parsial,
  tambah val baru), `ui/theme/Theme.kt` (parsial, 1 baris), `ui/theme/
  CupertinoTokens.kt` (parsial, javadoc only, 0 logic).
- **CHANGELOG.md**: entri baru ditambah paling atas batch ini juga (lihat
  file terpisah) -- pelajaran dari insiden sesi sebelumnya (lihat entri
  `[FIX] Release notes GitHub nyangkut...` di bawah) LANGSUNG diterapkan,
  bukan cuma dicatat.
- **Pending Queue (tidak berubah)**: warna sistem iOS (`systemBlue` dst) --
  1 item tersisa dari 3 item awal restyling Cupertino murni; `FileSorter.kt`
  refactor (item lama, tidak terkait Cupertino, lihat log lebih bawah).

## [FIX] Release notes GitHub nyangkut di entri lama walau versionName sudah naik (2026-08-27)
- **Laporan user**: tab GitHub Release stale, "display angka sudah up-to-date,
  namun informasi yang ditampilkan malah dari run yang sudah lewat/stale".
- **Root cause (dikonfirmasi via kode, bukan tebakan)**: CI (`.github/
  workflows/build.yml` step "Extract release notes from CHANGELOG.md")
  selalu ambil SECTION `## ` TERATAS `CHANGELOG.md` via `awk` -- logic ini
  sendiri benar & tidak disentuh. Masalahnya: sesi SEBELUM ini (entri
  `[CUPERTINO] Dialog crash log...` di bawah, lihat posisinya PALING ATAS
  di log file ini -> kerjaan PALING BARU) sudah menulis kode `VaultAlertDialog.kt`
  penuh (dipakai `DiagnosticsScreen.kt`, terdaftar `FILE_MANIFEST.txt`) TAPI
  lupa menambah entri baru di `CHANGELOG.md` -- top section-nya masih
  "Aksi ke-3 Simpan Perubahan" (kerjaan 1 sesi LEBIH LAMA). Akibatnya:
  `versionName` (dari `GITHUB_RUN_NUMBER`, otomatis) tetap naik tiap push
  ("angka sudah up-to-date"), tapi body Release yg di-extract awk selalu
  itu-itu saja / basi ("informasi ... dari run yang sudah lewat") sampai
  ada entri baru ditambah manual -- BUKAN bug caching/CDN GitHub, BUKAN
  bug di `UpdateRepository.kt` (network client sudah dikonfirmasi 0 cache).
- **Fix**: `CHANGELOG.md` ditambah 1 entri baru PALING ATAS (`[CUPERTINO]
  Dialog crash log ikut sistem tema...`) merangkum kerjaan `VaultAlertDialog`
  yg kodenya sudah ada tapi belum tercatat -- murni dokumentasi retroaktif,
  0 baris kode aplikasi disentuh.
- **TIDAK disentuh** (Zero-Unnecessary-Refactor): `build.yml` (logic awk
  sudah benar), `UpdateRepository.kt`/`VaultAlertDialog.kt`/`VaultActionSheet.kt`
  (sudah benar sejak ditulis, dikonfirmasi via pembacaan kode langsung).
- **Pengingat proses ke depan**: SETIAP sesi yg mengubah kode WAJIB tambah
  entri baru di `CHANGELOG.md` (bukan cuma `PROJECT_STATE.md`) di batch yang
  SAMA -- kalau lupa, gejala persis ini (release notes basi) akan berulang
  lagi di push berikutnya.
- File diubah (1): `CHANGELOG.md` (1 entri baru).

## [CUPERTINO] Dialog crash log ikut sistem tema -- tutup pending item "custom dialog non-actionsheet" (2026-08-27)
- **Instruksi user**: "lanjut sempurnakan theme Cupertino style murni!!" --
  tanpa target spesifik, jadi diaudit dulu (bukan Fast-Track, scope
  "sempurnakan theme" bukan tweak 1 variabel). `CupertinoTokens.kt`
  (javadoc-nya sendiri) mencatat 3 item belum dikerjakan: typography
  scale iOS-ish, warna sistem (`systemBlue` dst), **custom dialog
  non-actionsheet**. Grep `AlertDialog\|ModalBottomSheet\|Dialog` menyeluruh
  ke seluruh `ui/screens/*.kt`/`ui/components/*.kt` -- SATU-SATUNYA titik
  ketemu: `DiagnosticsScreen.kt` (penampil isi crash log), pakai
  `androidx.compose.material3.AlertDialog` MENTAH, 0 sentuhan sistem tema
  sama sekali -- kelas bug SAMA PERSIS dgn `WarningBanner` sebelum v8.29.0
  ("SATU-SATUNYA permukaan berisi konten yang bypass total sistem tema").
  Item ini dipilih (bukan 2 item lain) krn paling konkret/scoped & sudah
  ditandai eksplisit sbg pending -- 2 item lain (typography scale, warna
  sistem) jauh lebih luas cakupannya (lintas app, bukan 1 titik bypass),
  di luar 1 batch aman.
- **Fix**: BARU `ui/components/VaultAlertDialog.kt` -- pengganti `AlertDialog`
  M3 polos, wadah utamanya [TactileSurface] (BUKAN implementasi baru dari
  nol) -- otomatis dapat treatment gaya aktif (translucent+sheen Glass /
  shadow+border Neumorphism / flat elevasi M3 murni / flat+hairline+radius
  besar Cupertino) TANPA logic tambahan, persis kartu lain di app. Shape
  `MaterialTheme.shapes.extraLarge` (SAMA dgn default `AlertDialogDefaults`
  M3 yang digantikan -- 3 gaya non-Cupertino 0 berubah visual dari radius,
  Cupertino otomatis dapat radius besar lewat `CupertinoShapes` yang SUDAH
  dipasang kondisional di `Theme.kt`, 0 override manual perlu di komponen
  baru ini). Tombol tunggal tetap `TextButton` (konvensi M3 baku aksi
  non-destruktif, sama seperti sebelumnya) -- 0 percabangan gaya diperlukan
  di situ, beda dgn `VaultActionSheet` yang py aksi destruktif/multi-tombol.
  Khusus Cupertino: `HorizontalDivider` hairline dipisah SEBELUM tombol
  (pola sama `VaultActionSheet`) -- signature `UIAlertController` asli.
- **`DiagnosticsScreen.kt`**: `AlertDialog(...)` diganti `VaultAlertDialog(...)`
  -- title/dismissLabel/onDismissRequest sama persis nilainya, isi (Column
  scrollable `heightIn(max=400.dp)` + `Text` log content) dipindah APA
  ADANYA ke slot `content`. Import `AlertDialog`/`TextButton` yang jadi
  unused ikut dihapus (grep dikonfirmasi 0 pemakaian tersisa SEBELUM
  dihapus), import `VaultAlertDialog` ditambah (pola sama
  `SectionHeader`/`VaultCard` yang sudah eksplisit di file yang sama,
  BUKAN fully-qualified inline spt `VaultTopBar`/`Scaffold` -- konsisten
  komponen yang di-reuse lintas file).
- **TIDAK disentuh** (Zero-Unnecessary-Refactor + di luar scope aman):
  `VaultActionSheet.kt` (pola actionsheet Cupertino sudah benar sejak
  v8.31.3, tidak relevan di sini), `CupertinoTokens.kt`/`CupertinoShapes`
  (dipakai ulang apa adanya, 0 token baru), 2 item pending lain (typography
  iOS-ish, warna sistem) -- TETAP di pending, scope lebih besar dari 1 batch.
- File diubah (3, pas batas Micro-Batch): BARU `ui/components/
  VaultAlertDialog.kt`, `ui/screens/DiagnosticsScreen.kt` (parsial: 2 baris
  import dihapus + 1 baris import baru + 1 blok dialog diganti),
  `FILE_MANIFEST.txt` (1 baris baru, posisi alfabetis antara
  `VaultActionSheet.kt` & `VaultCard.kt`).
- **Verifikasi statis**: (1) keseimbangan `{}`/`()` kedua file kode
  (VaultAlertDialog.kt 6/6 & 24/24, DiagnosticsScreen.kt 44/44 & 148/148),
  (2) grep ulang 0 sisa `AlertDialog`/`TextButton` di `DiagnosticsScreen.kt`
  setelah import dihapus, (3) `scripts/preflight_check.sh` 14/14 kategori
  PASS (kategori #7 review manual: `VaultAlertDialog.kt` 0 muncul di daftar
  -- tidak ada pola fungsi lokal mencurigakan).
- **⏳ PENDING QUEUE (2 item, dari `CupertinoTokens.kt`, TETAP belum
  dikerjakan -- scope lebih besar dari 1 batch aman)**:
  1. Typography scale iOS-ish (font Cupertino-style, lintas app -- saat
     ini semua gaya SAMA PERSIS `PromptVaultTypography` M3 baku, belum ada
     percabangan per `themeStyle` spt `shapes` sudah py).
  2. Warna sistem ala iOS (`systemBlue` dst) -- saat ini Cupertino reuse
     100% palet M3 `colorScheme` yang sama dgn 3 gaya lain, 0 hue khusus
     Cupertino.
- **Batas jujur**: seperti seluruh riwayat project, **BELUM PERNAH lewat
  `./gradlew`/device asli** -- `Dialog` composable primitif (`androidx.
  compose.ui.window.Dialog`) BARU PERTAMA KALI dipakai LANGSUNG di seluruh
  riwayat project ini (grep sebelum implementasi: 0 hasil, sebelumnya
  semua dialog lewat `AlertDialog`/`ModalBottomSheet` M3) -- API standar
  Compose foundation, risiko rendah, tapi genuinely belum ada preseden
  internal utk dibandingkan.
- **User WAJIB verifikasi di HP**: (1) build CI hijau, (2) Diagnostik ->
  buka salah satu crash log (kalau ada) -> dialog HARUS tetap muncul &
  bisa ditutup ("Tutup"), isi log tetap scrollable & terbaca, (3) ganti
  gaya tema ke Cupertino (Pengaturan -> Tampilan) -> buka dialog log LAGI
  -> HARUS terlihat flat (0 shadow), hairline tipis sebelum tombol
  "Tutup", sudut lebih membulat dari 3 gaya lain, (4) ganti ke 3 gaya lain
  (Glass/Neumorphism/Material3 Murni) -> dialog HARUS terlihat 100% sama
  seperti sebelum batch ini (0 regresi visual -- radius/warna/tombol
  identik, cuma sekarang lewat `TactileSurface` bukan `AlertDialog` M3).

## [FITUR] Aksi ke-3 "Simpan Perubahan" di sheet "Buang Perubahan?", gaya iOS (2026-08-27)
- **Instruksi user, eksplisit** (dgn screenshot sheet "Buang Perubahan?"
  existing): "tambahkan action ke-3 'simpan perubahan'. like iOS style!!" --
  BUKAN task kosmetik (beda dari 2 batch refactor sebelumnya di hari yang
  sama) -- ini nambah SATU jalur aksi baru (bisa trigger simpan rule dari
  dalam sheet discard), jadi diaudit spt fitur baru biasa (bukan Fast-Track).
- **Kenapa lewat `VaultActionSheet` (komponen bersama, dipakai 6 titik)
  bukan cuma di `AddEditRuleScreen.kt` saja**: param aksi ke-3 dibuat
  OPSIONAL (`neutralLabel`/`onNeutral`, default `null`) tepat supaya 5
  pemanggil lain (`ActivityLogScreen.kt` x2, `RuleListScreen.kt`,
  `SettingsScreen.kt`, + 1 sheet lain di `AddEditRuleScreen.kt` sendiri utk
  konfirmasi duplikat/overlap) **TIDAK PERLU diubah SAMA SEKALI** & render
  100% IDENTIK spt sebelumnya (grep diverifikasi: default `null` -> branch
  `if (neutralLabel != null && onNeutral != null)` tidak pernah masuk ->
  0 divider/tombol tambahan). Cuma sheet "Buang Perubahan?" yang pass
  keduanya non-null.
- **Urutan 3 aksi ikut konvensi Apple HIG persis** (bukan ditaruh
  sembarang): [destructive paling atas] -> [aksi netral/regular] -> [cancel
  paling bawah] -- pola yang sama dipakai iOS native sendiri di alert
  "ada perubahan belum disimpan" (mis. Notes: Delete Draft/Save Draft/
  Cancel; Mail: Delete Draft/Save Draft/Cancel). Di sini: "Buang" (merah,
  `colors.error`, TETAP di posisi sama spt sebelumnya) -> "Simpan Perubahan"
  (BARU, `colors.primary`, biar kebeda jelas dari 2 aksi lain tanpa nambah
  token warna baru) -> "Batal" (`colors.onSurfaceVariant`, TETAP di baris
  paling bawah spt sebelumnya, gaya Cupertino: `TextButton` + hairline
  divider; gaya Material lain: `OutlinedButton`, keduanya konsisten dgn 2
  tombol lama yang sudah ada).
- **Wiring logic "Simpan Perubahan" (bukan cuma tombol kosong)**: body
  `onClick` tombol "Simpan" utama (build `Rule` dari field form + `isSaving`
  guard + `onCheckBeforeSave` + cabang Ok/duplicate-overlap) diekstrak MURNI
  (0 logic diubah, cuma dipindah) jadi `val performSave: () -> Unit` di atas
  `Scaffold` -- dipanggil ulang dari `onNeutral` sheet discard
  (`showDiscardConfirm = false` dulu baru `performSave()`, urutan ini
  PENTING supaya sheet discard tertutup SEBELUM sheet duplikat/overlap
  berpotensi muncul menggantikannya lewat `pendingCheck`/`pendingRule` --
  mencegah 2 sheet numpuk bareng, kelas bug yang sama persis dgn insiden
  "dialog overlap" yang sudah pernah ditambal sebelumnya di file ini).
  `onSave(rule, null)` pada jalur Ok TETAP motor navigasi pop-back-stack yang
  sama (lihat `MainActivity.kt` composable `ADD_EDIT_RULE`) -- 0 perubahan
  di titik itu.
- **Guard form-tidak-valid**: `isFormValidToSave` (val baru, sama persis
  kondisi `enabled` tombol Simpan sebelumnya: folder tidak blank + tidak ada
  error validasi + pattern tidak blank) dipakai gating -- kalau form
  SEDANG tidak valid saat sheet discard muncul, `neutralLabel`/`onNeutral`
  dipass `null` (aksi disembunyikan TOTAL dari sheet, bukan ditampilkan tapi
  diam-diam gagal kalau ditap) -- konsisten dgn tombol Simpan utama yang
  disabled pada kondisi sama.
- **TIDAK disentuh**: sheet konfirmasi duplikat/overlap ("Tetap Simpan?", di
  `AddEditRuleScreen.kt` juga) -- SENGAJA tetap 2 aksi, aksi ke-3 di situ
  tidak masuk akal (sudah dalam proses simpan, bukan discard). 5 pemanggil
  `VaultActionSheet` lain -- lihat penjelasan param opsional di atas.
- File diubah (3, pas batas Micro-Batch): `ui/components/VaultActionSheet.kt`
  (parsial: 2 param baru + 2 blok render bersyarat, Cupertino & Material),
  `ui/screens/AddEditRuleScreen.kt` (parsial: extract `performSave`+
  `isFormValidToSave`, wiring sheet discard), `res/values/strings.xml`
  (1 string baru: `rule_edit_discard_save`).
- **Verifikasi statis**: (1) keseimbangan `{}`/`()` per file (semua balance),
  (2) grep ulang 5 pemanggil `VaultActionSheet` lain -- 0 yang pass
  `neutralLabel`/`onNeutral`, aman default `null`, (3) `scripts/
  preflight_check.sh` 14/14 kategori PASS.
- **Batas jujur**: seperti seluruh riwayat project, **BELUM PERNAH lewat
  `./gradlew`/device asli** -- terutama flow "tap Simpan Perubahan saat
  pattern ternyata duplikat/overlap dgn rule lain" (2 sheet berurutan,
  bukan cuma path Ok) BELUM pernah dicoba nyata di device, cuma diverifikasi
  lewat baca-ulang urutan state (`showDiscardConfirm`/`pendingCheck`/
  `pendingRule`) di atas.
- **User WAJIB verifikasi di HP**: (1) build CI hijau, (2) buka Edit/Tambah
  Rule, ubah field APA PUN, coba kembali (tombol panah ATAU gesture back) --
  sheet "Buang Perubahan?" HARUS tampil 3 aksi (Buang/Simpan Perubahan/
  Batal) saat form valid, HANYA 2 aksi (Buang/Batal, TANPA "Simpan
  Perubahan") saat form TIDAK valid (mis. nama folder/pattern dikosongkan
  lagi), (3) tap "Simpan Perubahan" HARUS benar-benar menyimpan & kembali ke
  daftar rule (bukan cuma nutup sheet), (4) 5 sheet lain di app (hapus rule,
  undo, restore vault, konfirmasi duplikat/overlap) HARUS tetap 2 aksi spt
  sebelumnya, 0 perubahan visual.

## [REFACTOR] Ekstrak `SectionHeader` -- konsolidasi 8 pasangan Text title+desc duplikat, tanpa ubah behavior (2026-08-27)
- **Instruksi user, eksplisit**: "lanjutkan progress refactor *kosmetik only!!" --
  melanjutkan batch `ToggleRow` sesi sebelumnya, scope dibatasi user sendiri ke
  "kosmetik" (murni UI, 0 logic) -- jadi **item #1 Pending Queue (`FileSorter.kt`,
  2086 baris) TETAP TIDAK disentuh** sesi ini: itu pemecahan file/struktur kode
  backend (SAF/Shizuku), bukan kosmetik, dan sudah ditandai "TUNGGU instruksi
  eksplisit" -- instruksi user kali ini justru mempersempit ke kosmetik, BUKAN
  mengizinkan item berisiko itu.
- **Audit dilakukan** (grep `style = MaterialTheme.typography` di semua
  `ui/screens/*.kt`): ditemukan pola identik `Text(title, titleMedium)`
  langsung diikuti `Text(desc, bodySmall)` di **8 titik** across 2 file --
  `SettingsScreen.kt` (interval, konflik, konkurensi, SAF, backup, import) &
  `DiagnosticsScreen.kt` (downloads, crashlog) -- kandidat ekstraksi paling
  aman yang tersisa: 0 state/logic/coroutine/I-O disentuh, murni struktur UI,
  sama persis semangatnya dgn `ToggleRow` batch sebelumnya.
- **Kompleksitas tersembunyi yang WAJIB ditangani (beda dgn `ToggleRow`)**:
  title & desc di kode asli bukan dibungkus Column sendiri -- keduanya
  langsung jadi child Column PEMANGGIL, jadi jarak title-desc ikut
  `verticalArrangement` Column pemanggil yang **beda-beda per titik** (16.dp
  di 3 section teratas Settings yg langsung anak Column terluar
  `spacedBy(16.dp)`, 8.dp di section dalam `VaultCard` yg Column-nya
  `spacedBy(8.dp)`, 0.dp -- default Column TANPA `verticalArrangement` --
  khusus kartu Downloads Diagnostics). Kalau diseragamkan begitu saja, jarak
  visual title-desc di beberapa tempat akan BERUBAH (regresi kosmetik,
  ironis krn task-nya sendiri kosmetik-only) -- jadi `SectionHeader` dibuat
  dgn param `spacing: Dp = 8.dp` (default = nilai paling sering muncul, 4/8
  pemanggil) & 4 pemanggil sisanya pass `spacing` eksplisit sesuai jarak
  ASLI masing-masing, diverifikasi satu-satu (bukan diseragamkan/ditebak):

  | Pemanggil | Section | Jarak title-desc asli | Setelah refactor |
  |---|---|---|---|
  | `SettingsScreen.kt` | Interval | 16.dp (anak Column terluar) | `spacing = 16.dp` eksplisit |
  | `SettingsScreen.kt` | Konflik | 16.dp (anak Column terluar) | `spacing = 16.dp` eksplisit |
  | `SettingsScreen.kt` | Konkurensi | 16.dp (anak Column terluar) | `spacing = 16.dp` eksplisit |
  | `SettingsScreen.kt` | SAF (dlm `VaultCard`) | 8.dp | pakai default, 0 param tambahan |
  | `SettingsScreen.kt` | Backup (dlm `VaultCard`) | 8.dp | pakai default, 0 param tambahan |
  | `SettingsScreen.kt` | Import (dlm `VaultCard`) | 8.dp | pakai default, 0 param tambahan |
  | `DiagnosticsScreen.kt` | Downloads (dlm `VaultCard`, Column TANPA `verticalArrangement`) | 0.dp | `spacing = 0.dp` eksplisit |
  | `DiagnosticsScreen.kt` | Crashlog (dlm `VaultCard`, `spacedBy(8.dp)`) | 8.dp | pakai default, 0 param tambahan |

- **TIDAK disentuh** (Zero-Unnecessary-Refactor + di luar scope aman): section
  "Cek Pembaruan" (`SettingsScreen.kt`, baris title punya `Icon` di dalam
  `Row` bareng `Text` -- pola beda, bukan `Text` title polos); "Auto-Sort" &
  "Mode Shizuku" (title-nya `ToggleRow`, bukan `Text` -- sudah komponen hasil
  batch sebelumnya, tidak relevan di sini); `diag_worker_status_title` (baris
  ke-2 bukan desc, isinya `statusText` dinamis style `bodyMedium` bukan
  `bodySmall` -- pola beda); `diag_manual_verify_title` (baris ke-2 dst bukan
  1 desc melainkan 5 baris langkah terpisah -- pola beda). `FileSorter.kt` --
  tetap di Pending Queue, lihat catatan instruksi user di atas.
- File diubah (3, pas batas Micro-Batch): `ui/components/SectionHeader.kt`
  (baru), `ui/screens/SettingsScreen.kt` (parsial: 1 import baru, 6 blok
  `Text`+`Text`->`SectionHeader`), `ui/screens/DiagnosticsScreen.kt` (parsial:
  1 import baru, 2 blok `Text`+`Text`->`SectionHeader`). `FILE_MANIFEST.txt`
  diperbarui (1 file baru, posisi alfabetis antara `RuleCard.kt` &
  `SegmentedControl.kt`).
- **Verifikasi statis** (pengganti `./gradlew` yang tidak tersedia): (1)
  keseimbangan `{}`/`()` dicek per file (semua balance), (2) `scripts/
  preflight_check.sh` 14/14 kategori PASS, (3) grep ulang pasca-edit
  memastikan 8 pola lama sudah tergantikan & 4 pola yang SENGAJA tidak
  disentuh (update/shizuku/autosort/worker-status/manual-verify) masih utuh
  seperti semula.
- **Batas jujur**: seperti seluruh riwayat project, **BELUM PERNAH lewat
  `./gradlew`/device asli** -- ekstraksi ini risiko serendah mungkin by
  design (0 logic/state disentuh, constraint Compose murni + tabel verifikasi
  `spacing` per-titik di atas), tapi tetap belum ada konfirmasi compile/render
  asli.
- **Pending Queue (tidak berubah dari sesi sebelumnya, belum dikerjakan)**:
  1. **`FileSorter.kt` (2086 baris)** -- TETAP tunggu instruksi eksplisit user
     (bukan kosmetik, file inti SAF/Shizuku berisiko tinggi).
  2. **Audit unused import project-wide** -- baru dicek 4 file total
     (`AddEditRuleScreen.kt`, `SettingsScreen.kt` batch lalu; `SettingsScreen.kt`,
     `DiagnosticsScreen.kt` batch ini -- tidak ada import yg jadi unused krn
     `Text`/`MaterialTheme` masih dipakai luas di kedua file), 65 file `.kt`
     lain BELUM diaudit.
  3. **Desync label versi `CHANGELOG.md` vs `PROJECT_STATE.md`** -- lihat
     detail lengkap di entri batch `ToggleRow` di bawah, masih belum
     diperbaiki (di luar scope, TIDAK ditulis ulang tanpa instruksi eksplisit).
- **User WAJIB verifikasi di HP**: (1) build CI hijau, (2) layar Pengaturan
  (section Interval/Konflik/Konkurensi/SAF/Backup/Import) & Diagnostik
  (section Downloads/Riwayat Crash) -- tampilan & jarak title-desc HARUS
  identik 1:1 dgn sebelumnya (0 perubahan visual).

## [REFACTOR] Ekstrak `ToggleRow` -- konsolidasi 3 Row switch duplikat, tanpa ubah behavior (2026-08-27)
- **Instruksi user, eksplisit**: "refactor rapi-rapi tanpa mengubah
  behavior!!" -- tanpa scope spesifik, jadi diaudit dulu (bukan Fast-Track --
  scope lintas-file, meski hasil akhirnya sengaja dipilih yang kecil/aman).
- **Audit dilakukan** (bukan langsung eksekusi tebakan): (1) survei baris per
  file `.kt` (69 file) -- `FileSorter.kt` (2086 baris) jauh terbesar,
  kandidat pemecahan file paling jelas, TAPI ini persis file inti SAF/
  Shizuku/"latest zip held back" yang user sendiri pernah tandai prioritas
  dilindungi dari regresi (lihat sesi rollback R8) -- restrukturisasi file
  sebesar itu TANPA `./gradlew` utk verifikasi ulang adalah risiko yang TIDAK
  sepadan dgn instruksi "tanpa ubah behavior", DITUNDA (bukan dikerjakan
  blind), dicatat di Pending Queue di bawah, BUKAN dieksekusi sesi ini. (2)
  Duplikasi pola `Row(SpaceBetween){Text(weight=1f);TactileSwitch}` --
  SUDAH diidentifikasi lengkap sejak fix row-overlap v8.35.1 (3 titik: 1x
  `AddEditRuleScreen.kt`, 2x `SettingsScreen.kt`, ditambal SATU-SATU saat
  itu krn task waktu itu adalah bugfix darurat, bukan refactor) -- kandidat
  PALING AMAN utk batch refactor ini: ekstraksi murni, bukan re-desain,
  hasil render 100% identik secara matematis (constraint Compose sama
  persis), dan SUDAH terverifikasi benar isinya sejak v8.35.1.
- **Kenapa dipilih ini, bukan yang lain**: ekstraksi composable adalah
  refactor PALING RENDAH RISIKO yang ada (0 perubahan logic/state/coroutine/
  I/O, murni struktur UI) -- cocok dgn STABILITY WINS & keterbatasan sandbox
  ini (0 akses `./gradlew`/device, jadi "tanpa ubah behavior" HARUS
  dibuktikan lewat kesamaan kode statis, bukan run test).
- **Implementasi** (`ui/components/ToggleRow.kt`, baru): `@Composable fun
  ToggleRow(label, checked, onCheckedChange, modifier, style, accentColor)`
  -- badan fungsi = SALINAN PERSIS `Row{Text;TactileSwitch}` dari 3 pemanggil
  asli, cuma parameter yang beda (`label`/`checked`/`onCheckedChange`)
  dijadikan argumen. Default `style = titleSmall` (pemanggil pertama,
  hold-back-zip) & `accentColor = MaterialTheme.colorScheme.primary` (default
  `TactileSwitch` sendiri, IDENTIK dgn `colors.primary` yang sebelumnya
  dipass eksplisit di `SettingsScreen.kt`) -- diverifikasi SATU-SATU per
  pemanggil sebelum edit (lihat tabel kesesuaian di bawah), BUKAN
  diseragamkan/ditebak.

  | Pemanggil | style asli | accentColor asli | Setelah refactor |
  |---|---|---|---|
  | `AddEditRuleScreen.kt` (hold-back-zip) | `titleSmall` | (default `TactileSwitch`) | pakai default `ToggleRow`, 0 param tambahan -- IDENTIK |
  | `SettingsScreen.kt` (Auto-Sort) | `titleMedium` | `colors.primary` (eksplisit) | `style`+`accentColor` dipass eksplisit -- IDENTIK |
  | `SettingsScreen.kt` (Shizuku) | `titleMedium` | `colors.primary` (eksplisit) | `style`+`accentColor` dipass eksplisit -- IDENTIK |

- **TIDAK disentuh** (Zero-Unnecessary-Refactor + di luar scope aman):
  `ThemeStyleSwitchRow` (`ThemeStyleToggle.kt`) -- pola visual BEDA (kartu
  `TactileSurface` berwarna/elevasi sesuai state, dipakai utk pilihan
  mutually-exclusive) -- reuse `ToggleRow` di situ akan MENGUBAH tampilan,
  bukan cuma refactor struktur. `RuleCard.kt` -- pola beda (`SpaceEvenly`,
  ikon+switch tanpa `Text`, sudah dikonfirmasi TIDAK relevan sejak audit
  v8.35.1). `TactileSwitch.kt` sendiri -- komponennya benar, tidak disentuh.
  `FileSorter.kt` -- lihat Pending Queue.
- File diubah (3, pas batas Micro-Batch): `ui/components/ToggleRow.kt`
  (baru), `ui/screens/AddEditRuleScreen.kt` (parsial: 1 import ganti,
  1 blok Row->ToggleRow, import `TactileSwitch` dihapus krn sudah tak
  terpakai), `ui/screens/SettingsScreen.kt` (parsial: 1 import ganti, 2
  blok Row->ToggleRow, import `TactileSwitch` dihapus). `FILE_MANIFEST.txt`
  diperbarui (1 file baru, posisi alfabetis antara `ThemeStyleToggle.kt` &
  `VaultActionSheet.kt`).
- **Verifikasi statis** (pengganti `./gradlew` yang tidak tersedia): (1)
  keseimbangan `{}`/`()` dicek per file (semua balance, lihat log sesi),
  (2) `TactileSwitch` dikonfirmasi 0 pemakaian tersisa di 2 file pemanggil
  SEBELUM importnya dihapus (grep, bukan asumsi), (3) `scripts/
  preflight_check.sh` 14/14 kategori PASS.
- **Batas jujur**: seperti seluruh riwayat project, **BELUM PERNAH lewat
  `./gradlew`/device asli** -- ekstraksi ini risiko serendah mungkin by
  design (constraint Compose murni, 0 logic/state disentuh), tapi tetap
  belum ada konfirmasi compile/render asli.
- **Pending Queue (refactor lanjutan, TIDAK dikerjakan sesi ini)**:
  1. **`FileSorter.kt` (2086 baris)** -- kandidat pemecahan file paling
     jelas (mis. pisah loop legacy/SAF/Shizuku ke file terpisah), TAPI
     risiko lebih tinggi (file inti SAF, ditandai prioritas proteksi user
     sendiri) -- TUNGGU instruksi eksplisit user sebelum dikerjakan, jangan
     default.
  2. **Audit unused import project-wide** -- baru dicek 2 file (`AddEditRuleScreen.kt`,
     `SettingsScreen.kt`) sbg efek samping batch ini, 67 file `.kt` lain
     BELUM diaudit.
  3. **[DITEMUKAN, BUKAN DIPERBAIKI] Desync label versi `CHANGELOG.md` vs
     `PROJECT_STATE.md`**: `CHANGELOG.md` punya entri "v8.35.1 -- FIX:
     in-app updater..." SEDANGKAN `PROJECT_STATE.md` sudah pakai label
     "v8.35.1" lebih dulu utk topik BEDA ("FIX row overlap: judul teks
     nabrak/nutupin TactileSwitch") -- 2 topik beda nomor sama, kemungkinan
     `CHANGELOG.md` sempat tidak disinkron per-batch di beberapa sesi lalu.
     TIDAK diperbaiki sesi ini (di luar scope batch, & riwayat log historis
     sebaiknya tidak ditulis ulang tanpa instruksi eksplisit) -- entri BARU
     mulai sesi ini di kedua file memakai format `[KATEGORI] Judul (tanggal)`
     (bukan nomor versi manual) persis mengikuti 3 entri terbaru
     `PROJECT_STATE.md` (governance/incident/updater-fix) -- otomatis
     menghindari tabrakan nomor lagi ke depan krn tidak ada lagi nomor yang
     diketik manusia.
- **User WAJIB verifikasi di HP**: (1) build CI hijau, (2) toggle "Tahan
  versi .zip terbaru" (Tambah/Edit Rule), Auto-Sort & Mode Shizuku
  (Pengaturan) -- tampilan & interaksi HARUS identik 1:1 dgn sebelumnya (0
  perubahan visual: ukuran teks, warna switch, jarak, wrap behavior saat
  teks panjang -- SEMUA harus sama persis spt v8.35.1).

## [INSIDEN+FIX] In-app updater kira app usang = sudah versi terbaru (2026-08-27)
- **Gejala nyata dari user**: fitur cek update dalam app melaporkan "sudah
  versi terbaru" padahal APK yang terpasang usang.
- **Root cause** (ditemukan dari source, bukan tebakan -- lihat
  `UpdateRepository.kt` fungsi `isNewerVersion`): efek samping LANGSUNG
  dari governance versioning di atas. Fungsi lama bandingkan versi via
  TUPLE POSISIONAL penuh (major dulu, baru minor, baru patch -- didesain
  utk skema semantic lama `8.x.y`). Skema versionName sekarang PERMANEN
  `1.0.<run_number>` (major di-reset ke 1) -- app lama yang masih terpasang
  dgn versionName skema lama (mis. `8.35.0`) dibandingkan lawan rilis baru
  (mis. `1.0.209`) KALAH di posisi PERTAMA (`1 < 8`) -> `isNewerVersion`
  return `false` SELAMANYA, app kira dirinya paling baru padahal paling usang.
- **Fix** (`UpdateRepository.kt`, parsial, 1 fungsi): bandingkan SEGMEN
  TERAKHIR version string saja (run number -- satu-satunya angka yg pernah
  berubah di skema baru, PINNED naik terus tiap job Actions), bukan seluruh
  tuple posisional. Kebal thd app skema lama yang masih beredar DAN thd
  perubahan skema apa pun di masa depan.
- File diubah (1): `UpdateRepository.kt` (parsial, fungsi `isNewerVersion`).

## [GOVERNANCE] Versioning 100% otomatis dari GitHub -- manual bump DIHAPUS PERMANEN (2026-08-27)
- **Instruksi eksplisit user**: singkirkan semua rule lama soal versioning,
  gantikan dgn: tidak boleh ada lagi bump versi manual, `versionCode` &
  `versionName` wajib otomatis langsung dari GitHub, rule ini tidak boleh
  diubah sesi lain, berlaku seketika.
- **`app/build.gradle.kts`** (protected asset, edit PARSIAL, izin eksplisit
  user utk task ini): hardcode `versionCode = 197` / `versionName =
  "8.35.6"` diganti baca `System.getenv("GITHUB_RUN_NUMBER")` saat fase
  konfigurasi -- pola IDENTIK dgn `signingConfigs` yg sudah lama baca
  `System.getenv(...)` langsung di file yang sama (0 pola baru).
  - `versionCode = githubRunNumber ?: 1` -- integer run number Actions,
    terjamin naik terus tiap job baru (syarat wajib Android utk
    versionCode), 0 kemungkinan lupa/duplikat krn tak lagi disentuh manusia.
  - `versionName = githubRunNumber?.let { "1.0.$it" } ?: "1.0.0-dev"` --
    fallback HANYA kepakai kalau build jalan DI LUAR GitHub Actions (mis.
    lint/test lokal Termux tanpa env var ini); APK release nyata SELALU
    lewat workflow CI shg selalu dapat run number asli.
  - **Asumsi eksplisit** (boleh dikoreksi user kalau format tak sesuai
    selera): skema lama `major.minor.patch` manual (mis. `8.35.6`) tidak
    bisa dipertahankan krn user minta nol campur tangan manusia --
    `1.0.<run_number>` dipilih supaya tetap berbentuk "versi" familiar
    tanpa ada komponen yg perlu diketik manusia kapan pun.
  - **`.github/workflows/build.yml` TIDAK disentuh sama sekali** --
    `GITHUB_RUN_NUMBER` adalah env var bawaan default tiap job Actions
    (auto-tersedia ke semua step `run:` termasuk `./gradlew`), 0 perubahan
    workflow diperlukan.
- **Efek samping yang perlu diketahui user**: mulai sekarang versionName
  APK TIDAK LAGI berbentuk `8.x.y` semantic seperti histori sebelumnya --
  jadi `1.0.<nomor_run_actions>` (mis. `1.0.412`), dan HANYA terisi run
  number asli saat build lolos lewat GitHub Actions (build lokal/Termux
  tanpa CI tetap tampil fallback `1.0.0-dev`, sesuai desain baru ini).

## [INSIDEN+FIX] CI merah total pasca-governance versioning otomatis (2026-08-27)
- **Gejala nyata dari user** (2 log Actions asli diberikan): job `build`
  gagal di step **"Read app version"**, exit code 1, `read-version.log`
  yang ke-upload 0 byte kosong total. Compile/test/`assembleRelease`
  **TIDAK PERNAH JALAN** (diagnostik `List release output` konfirmasi
  folder `app/build/outputs/apk/release/` tidak ada sama sekali).
- **Root cause** (dari log asli, bukan tebakan): step itu berisi
  `VERSION=$(grep -oP 'versionName = "\K[^"]+' app/build.gradle.kts)` --
  regex ini expect literal `versionName = "x.y.z"` di source text. Batch
  governance sebelumnya (entry di atas) mengubah baris itu jadi ekspresi
  Kotlin (`githubRunNumber?.let {...}`), jadi 0 match -> grep exit 1 ->
  `set -euo pipefail` abort step ini SEBELUM baris `echo` sempat jalan --
  konsisten & menjelaskan kenapa `read-version.log` yang lebih dulu
  diupload user juga kosong total.
- **`.github/workflows/build.yml`** (protected asset, edit PARSIAL, izin
  eksplisit user + **isi lengkap file diberikan user sendiri** utk task
  ini -- BUKAN direkonstruksi/ditebak): HANYA step "Read app version" yang
  diubah, 16 step lain disalin 100% identik byte-per-byte (diverifikasi
  `diff` sebelum commit). `VERSION=$(grep ...)` diganti
  `VERSION="1.0.$GITHUB_RUN_NUMBER"` -- formula IDENTIK persis dgn
  fallback di `app/build.gradle.kts` (`"1.0.$githubRunNumber"`), keduanya
  jalan di run GitHub Actions yang SAMA jadi nilainya PASTI sama. 0 step
  lain perlu disentuh -- semua step downstream (`Rename APK`,
  `Upload APK artifact`, `Publish GitHub Release`, `Force-flag Latest`,
  `Upload build log on failure`) cuma pakai `steps.version.outputs.version`
  sbg string opaque, tidak peduli cara hitungnya.
- **Kenapa harus 1:1 sama persis dgn `app/build.gradle.kts`** (bukan
  format bebas): `UpdateRepository.kt`/`UpdateModels.kt` (fitur in-app
  update-checker yg sudah ada) membandingkan versionName APK terpasang vs
  tag GitHub Release terbaru -- kalau formatnya beda dikit saja, fitur cek
  update bisa salah baca/gagal match.



> **[Lompatan histori]** Batch journal di atas berhenti di 2026-08-27
(governance auto-versioning). Batch versi manual `v8.35.6` ke bawah s/d
`Insiden #6` (2026-08-06) sudah diarsipkan ke `PROJECT_STATE_ARCHIVE.md`
per sesi 2026-08-29 -- 0 histori hilang, cuma dipindah.


## Status Terkini (menggantikan "Versi/batch terakhir yang selesai" lama yang nyangkut di 2026-08-05 -- lihat PROJECT_STATE_ARCHIVE.md utk isi lama itu)
- **Versioning**: 100% otomatis dari `GITHUB_RUN_NUMBER` sejak governance
  2026-08-27 (lihat batch `[GOVERNANCE]` di `PROJECT_STATE_ARCHIVE.md`).
  Format `versionName` = `1.0.<nomor_run_Actions>` (mis. `1.0.412`),
  fallback `1.0.0-dev` HANYA kalau build di luar CI (lokal/Termux tanpa
  GitHub Actions). Tidak ada lagi skema semantik manual `8.x.y` -- DILARANG
  ditulis manual di file manapun (termasuk README) krn pasti stale lagi.
- **Sistem tema (tab "Tampilan" di Home, 4 pilihan)**:
  - **Material3** -- baseline asli project, sengaja TIDAK di-restyle
    (dipakai sbg pembanding "default").
  - **Cupertino** (iOS-look) -- 3/3 sumbu identitas SELESAI: typography,
    dialog/actionsheet ikut tema, warna sistem iOS.
  - **Neumorphism "Blade Runner 2049"** (Teal & Amber) -- 4/4 sumbu
    SELESAI: skema warna, shape (cut-corner), typography, aksen ke-4
    (Neon Magenta di ikon Pengaturan).
  - **Glassmorphism** -- 4/4 sumbu SELESAI: typography, shape
    (frosted-glass corner), colorScheme (Ice/Frost/Prism, dingin), aksen
    ke-4 (Glacier).
  - Detail teknis tiap sumbu ada di batch-batch terkait di atas (batch
    2026-08-27 s/d 2026-08-29) atau di arsip utk batch lebih lama.
- **Robustness/fitur inti** (Crash Logger MediaStore, Release Downloader
  chunk streaming, CI Stale Run Guard, roadmap backend §1-§5 lama) --
  histori implementasi lengkap ada tersebar di `PROJECT_STATE_ARCHIVE.md`
  & `CHANGELOG.md`; TIDAK diaudit ulang di batch dokumentasi ini (di luar
  scope "arsipkan + sinkron README", dan Fast-Track/VIP Docs melarang
  audit full project tanpa trigger eksplisit).
- **Batch terakhir yang menyentuh KODE**: "colorScheme & aksen ke-4,
  MENUTUP 4/4 sumbu Glass" (2026-08-29, lihat entri paling atas setelah
  batch dokumentasi ini). Batch dokumentasi ini sendiri (arsip + sync)
  murni non-kode.

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

**Riwayat batch versi manual (`v8.35.6` s/d `Insiden #6`, era pra-governance
sebelum 2026-08-27) sudah diarsipkan ke `PROJECT_STATE_ARCHIVE.md`** (sesi
2026-08-29) -- baca file itu HANYA kalau butuh detail teknis batch lama;
tidak wajib dibaca utk onboarding/kerja aktif sesi baru.
