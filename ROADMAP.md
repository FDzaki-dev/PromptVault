# Roadmap Sortify -- Menuju 100% Fungsionalitas & Polish

> Moto: **low-risk, high-value dulu**. Tiap item diberi skor Risiko/Nilai +
> estimasi jumlah file (proxy kompleksitas batch, patokan
> "Batch Limit: maks 10 file/1 modul" di alur kerja standar). Urutan fase =
> urutan pengerjaan yang disarankan, BUKAN urutan prioritas rasa/opini --
> murni rasio nilai:risiko dari tertinggi ke terendah.
>
> Status saat ini: **v8.20.0**, dark-only, Material 3 murni. Baseline app
> sudah solid -- fitur inti (auto-sort, rule pattern, undo, conflict
> strategy, export/import rule JSON, notifikasi hasil auto-scan per-rule,
> statistik penuh, Shizuku integration, crash logger) semua sudah 100%
> fungsional & terdokumentasi di `PROJECT_STATE.md`/`CHANGELOG.md`. **Fase 1
> & 2 SEMUA SELESAI** -- sisa roadmap tinggal Fase 3 (butuh item disebut
> eksplisit oleh user, JANGAN dikerjakan default).

---

## Fase 0 -- Gap Permanen (bukan bisa "diselesaikan", cuma bisa dimitigasi)

| Item | Risiko | Nilai | Kenapa permanen |
|---|---|---|---|
| Verifikasi kompilasi/perilaku nyata di device asli | - | Tinggi | Lingkungan kerja Claude sandbox tanpa Android SDK/device -- **selalu** butuh CI hijau + laporan user asli sebelum klaim "beres". Sudah dicatat jujur di `MAINTENANCE.md`, tidak berubah oleh roadmap ini. |

**Mitigasi berkelanjutan** (bukan sekali selesai): tiap batch WAJIB lewat
`scripts/preflight_check.sh` dulu (sudah standar), dan tiap klaim fix HARUS
menunggu konfirmasi CI/user sebelum ditutup di `PROJECT_STATE.md` -- pola ini
sudah berjalan, roadmap ini tidak menambah proses baru di sini.

---

## Fase 1 -- Low-Risk / High-Value (kerjakan duluan)

### ~~1.1 Unit test untuk `FileSorter.kt` (logika inti pemindahan file)~~ ✅ SELESAI v8.1.0
- Lihat `CHANGELOG.md` v8.1.0 untuk detail. 4 fungsi pure diekstrak
  (`isTempOrPartialName`, `explainNoMatchByName`, `buildPreviewResult`,
  `nextAvailableFileName`) + `FileSorterPureLogicTest.kt` (12 test case).

### ~~1.2 Audit aksesibilitas TalkBack menyeluruh~~ ✅ SELESAI v8.2.0
- Lihat `CHANGELOG.md` v8.2.0. Audit 9 layar + semua komponen bersama --
  gap nyata cuma di `SegmentedControl` (semantics `selected`/`Role.Tab` +
  target sentuh 38dp→48dp), sisanya sudah compliant. Selesai 1 batch
  (bukan 4 seperti estimasi awal di bawah).

### ~~1.3 String UI: audit hardcode vs `strings.xml`~~ ✅ SELESAI v8.16.2
- Batch 1: cluster "Kelola Rule" (v8.3.0). Batch 2: `SettingsScreen.kt`
  (v8.6.0). Batch 3-7: sisa 6 screen (`DiagnosticsScreen.kt`,
  `PanduanScreen.kt`, `HomeScreen.kt`, `OnboardingScreen.kt`,
  `ActivityLogScreen.kt`, `SkippedFilesScreen.kt` -- yang terakhir via
  audit UX batch 7, v8.16.1). Batch 8/N (PENUTUP): `MainActivity.kt`/
  `PermissionGate`, v8.16.2.
- Seluruh app (9 screen + `MainActivity.kt`) sekarang 100% `stringResource`,
  nol literal UI hardcode tersisa. Prasyarat Fase 3.3 (lokalisasi EN)
  terpenuhi kalau suatu saat diminta eksplisit.

### ~~1.4 Statistik ringkas di Home (jumlah file tersortir minggu ini/bulan ini)~~ ✅ SELESAI v8.17.0
- 2 baris baru di kartu ringkasan Home. Sumber `MoveHistoryRepository`.
  Caveat: cap 200 entri (existing, fitur Undo) bisa under-count "bulan
  ini" kalau pemindahan bulan berjalan >200 sebelum akhir bulan --
  diterima sbg trade-off, bukan bug baru. Lihat `CHANGELOG.md` v8.17.0.

---

## Fase 2 -- Medium-Risk / High-Value (kerjakan setelah Fase 1 stabil)

### ~~2.1 Pencarian & filter di Riwayat Aktivitas + daftar Rule~~ ✅ SELESAI v8.18.0
- `RuleListScreen.kt` ternyata sudah selesai duluan (v2.24.0). Cakupan
  nyata batch ini cuma `ActivityLogScreen.kt` (1 search field, 2 tab,
  disembunyikan saat mode seleksi-sapuan). Lihat `CHANGELOG.md` v8.18.0.

### ~~2.2 Notifikasi hasil auto-scan lebih kaya (ringkasan per-rule, bukan cuma total)~~ ✅ SELESAI v8.19.0
- Notifikasi HASIL baru (`AutoSortNotification.resultNotification`),
  terpisah dari notifikasi ongoing yang sudah ada. Breakdown per-rule
  diambil dari `MoveHistoryRepository` (pola sama dgn `computeHomeStats()`
  v8.17.0) -- BUKAN nambah parameter baru ke `FileSorter`/`ScanResult`,
  jadi 3 loop pemindahan (legacy/SAF/Shizuku) di `FileSorter.kt` sama
  sekali tidak disentuh (risiko lebih rendah dari estimasi awal). Hanya
  muncul kalau ada file benar-benar dipindah (0 file = skip, cegah
  notification fatigue tiap siklus auto-scan). Lihat `CHANGELOG.md` v8.19.0.

### ~~2.3 Halaman "Statistik" penuh (grafik tren, bukan cuma angka ringkas Home)~~ ✅ SELESAI v8.20.0
- `StatisticsScreen.kt` baru: kartu total sepanjang riwayat, grafik tren
  batang 14 hari (Canvas hand-rolled, TANPA library chart baru), breakdown
  per-rule (bar proporsional + angka). Sumber data `MoveHistoryRepository`
  (pola sama `computeHomeStats()`/`resultNotification` v8.17.0/v8.19.0) --
  caveat cap 200 entri sama, ditampilkan eksplisit di layar (bukan
  disembunyikan). Lihat `CHANGELOG.md` v8.20.0.

---

## Fase 3 -- Higher-Risk / Scope Besar (butuh keputusan eksplisit user per item, JANGAN dikerjakan default)

> Item di fase ini TIDAK otomatis dikerjakan walau ada di roadmap --
> masing-masing butuh konfirmasi eksplisit di sesi terpisah karena scope/
> risiko melewati ambang "low-risk" (biasanya >1 modul, atau perlu izin
> Android baru, atau permanen mengunci arah desain).

| Item | Risiko | Nilai | Kenapa berisiko |
|---|---|---|---|
| ~~**3.1 Home screen widget** (trigger scan cepat dari luar app)~~ | Tinggi | Sedang | ✅ SELESAI v8.21.0 -- widget stateless 1-tap, reuse `AutoSortWorker` apa adanya, hasil tetap lewat notifikasi existing. Lihat `CHANGELOG.md`/`PROJECT_STATE.md` v8.21.0. Verifikasi device asli MASIH tertunda user (lihat batas jujur di log). |
| **3.2 Tujuan pemindahan ke cloud storage** (Google Drive dll, bukan cuma folder lokal/SAF) | Tinggi | Sedang | Butuh OAuth + API pihak ketiga baru, model izin baru, ubah asumsi inti `FileSorter` (saat ini 100% berbasis SAF lokal) |
| **3.3 Lokalisasi multi-bahasa (EN toggle)** | Sedang-Tinggi | Rendah-Sedang (app saat ini Bahasa Indonesia penuh, target user belum jelas butuh EN) | Menyentuh SEMUA layar sekaligus (>1 modul, melebihi batch limit jauh), butuh 1.3 selesai dulu sbg prasyarat |
| **3.4 Multi-profile / lebih dari 1 set rule aktif bergantian** | Tinggi | Rendah-Sedang | Ubah model data inti (`Rule`, `SettingsRepository`) -- migrasi DataStore, risiko regresi ke semua fitur existing |
| **3.5 Light mode asli (ikut sistem)** | Sedang | Rendah (belum ada permintaan user, app sengaja dark-only sejak v3.0.0) | **Tidak direncanakan default** -- parkir di sini murni sbg catatan kalau suatu saat diminta eksplisit, lihat `PROJECT_STATE.md` v8.0.0 soal keputusan ini |

---

## Cara pakai roadmap ini di sesi berikutnya

1. Kerjakan **satu item per sesi/batch**, urut dari Fase 1 ke bawah.
2. Sebelum mulai item baru: cross-check `PROJECT_STATE.md` -- kalau ternyata
   sudah lebih dulu selesai sesi lain, coret dari sini & catat di
   `CHANGELOG.md` seperti biasa.
3. Fase 3 **JANGAN** dikerjakan tanpa user secara eksplisit menyebut item
   itu by name di prompt -- ini bukan larangan permanen, cuma penanda
   "tanya dulu, jangan asumsi".
4. Update file ini (coret item selesai / tambah temuan baru) sebagai
   bagian dari tiap batch yang menutup satu item roadmap.
