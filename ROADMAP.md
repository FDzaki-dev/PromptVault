# Roadmap PromptVault -- Menuju 100% Fungsionalitas & Polish

> Moto: **low-risk, high-value dulu**. Tiap item diberi skor Risiko/Nilai +
> estimasi jumlah file (proxy kompleksitas batch, patokan
> "Batch Limit: maks 10 file/1 modul" di alur kerja standar). Urutan fase =
> urutan pengerjaan yang disarankan, BUKAN urutan prioritas rasa/opini --
> murni rasio nilai:risiko dari tertinggi ke terendah.
>
> Status saat ini: **v8.0.0**, dark-only, Material 3 murni. Baseline app
> sudah solid -- fitur inti (auto-sort, rule pattern, undo, conflict
> strategy, export/import rule JSON, notifikasi hasil auto-scan, Shizuku
> integration, crash logger) semua sudah 100% fungsional & terdokumentasi di
> `PROJECT_STATE.md`/`CHANGELOG.md`. Roadmap ini HANYA berisi gap nyata yang
> tersisa, bukan daftar fitur ulang yang sudah ada.

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

### 1.2 Audit aksesibilitas TalkBack menyeluruh (progress: 1/4 batch)
- **Risiko: Rendah** (umumnya nambah `contentDescription`/`Role`, jarang ubah logika) · **Nilai: Tinggi** (app belum pernah diaudit end-to-end pakai screen reader asli, walau sudah ada perbaikan titik seperti hitbox `TactileSwitch` 48dp)
- Estimasi: 1 batch per grup layar (Home, RuleList/AddEdit, Settings,
  ActivityLog/MoveHistory) -- 4 batch kecil, bukan 1 batch besar
- Fokus: setiap ikon aksi tanpa label wajib `contentDescription`, urutan
  fokus logis, dan target sentuh minimum 48dp konsisten (pola yang sudah
  dipakai di `TactileSwitch` diterapkan menyeluruh)
- ~~Batch 1/4 -- grup Home~~ ✅ SELESAI v8.2.0/v8.2.1 (`GroupedListRow.kt`
  role Button, CTA scan contentDescription dinamis, `ManifestRow` merge
  semantics). Detail lengkap: `CHANGELOG.md` v8.2.0/v8.2.1.
- ~~Batch 2/4 -- grup RuleList/AddEdit~~ ✅ SELESAI v8.3.0
  (`TactileSwitch.kt` param contentDescription opsional, `RuleCard.kt`
  label per-item di 5 kontrol, `AddEditRuleScreen.kt` semantics error()
  utk field folder). Detail lengkap: `CHANGELOG.md` v8.3.0.
- ~~Batch 3/4 -- grup Settings~~ ✅ SELESAI v8.4.0 (`SettingsScreen.kt`
  contentDescription switch Mode Shizuku; sisanya sudah diaudit &
  compliant). Detail lengkap: `CHANGELOG.md` v8.4.0.
- **Lanjut sesi berikutnya**: Batch 4/4 (TERAKHIR) -- grup
  ActivityLog/MoveHistory.

### 1.3 String UI: audit hardcode vs `strings.xml`
- **Risiko: Rendah** (mechanical, tidak ubah perilaku) · **Nilai: Sedang** (prasyarat WAJIB kalau kelak mau lokalisasi -- lihat Fase 3.3 -- tapi berdiri sendiri juga berguna: memisahkan teks dari logika bikin maintenance lebih rapi)
- Estimasi: bertahap per layar, beberapa batch kecil
- **Catatan low-risk**: kerjakan HANYA ekstraksi string, JANGAN sekalian
  terjemahkan -- itu scope Fase 3.3 terpisah

### 1.4 Statistik ringkas di Home (jumlah file tersortir minggu ini/bulan ini)
- **Risiko: Rendah** (baca data yang sudah ada di `ActivityLogRepository`/`MoveHistoryRepository`, murni tampilan baru) · **Nilai: Sedang-Tinggi** (user langsung lihat app "bekerja" tanpa buka Riwayat Aktivitas manual)
- Estimasi: ~2-3 file (`HomeScreen.kt` + 1 komponen kartu baru + query
  agregasi di repository terkait)

---

## Fase 2 -- Medium-Risk / High-Value (kerjakan setelah Fase 1 stabil)

### 2.1 Pencarian & filter di Riwayat Aktivitas + daftar Rule
- **Risiko: Sedang** (state UI baru + interaksi list, tapi tidak sentuh data layer) · **Nilai: Tinggi** (skala nyata: user dgn puluhan rule/ratusan log entry akan makin butuh ini)
- Estimasi: ~4-6 file (2 screen + kemungkinan 1 util filter kecil)

### 2.2 Notifikasi hasil auto-scan lebih kaya (ringkasan per-rule, bukan cuma total)
- **Risiko: Sedang** (`AutoSortNotification.kt` sudah ada, ini perluasan bukan bikin baru dari nol -- tapi tetap perlu hati-hati soal panjang teks notifikasi Android & battery/Doze) · **Nilai: Sedang**
- Estimasi: ~2-3 file

### 2.3 Halaman "Statistik" penuh (grafik tren, bukan cuma angka ringkas Home)
- **Risiko: Sedang** (screen baru + agregasi data time-series, area baru yang belum pernah diaudit `preflight_check.sh` kategori 5/7) · **Nilai: Sedang**
- Estimasi: ~5-8 file
- **Prasyarat**: kerjakan SETELAH 1.4 (statistik ringkas Home) terbukti stabil
  di device asli user -- jangan lompat langsung ke versi penuh

---

## Fase 3 -- Higher-Risk / Scope Besar (butuh keputusan eksplisit user per item, JANGAN dikerjakan default)

> Item di fase ini TIDAK otomatis dikerjakan walau ada di roadmap --
> masing-masing butuh konfirmasi eksplisit di sesi terpisah karena scope/
> risiko melewati ambang "low-risk" (biasanya >1 modul, atau perlu izin
> Android baru, atau permanen mengunci arah desain).

| Item | Risiko | Nilai | Kenapa berisiko |
|---|---|---|---|
| **3.1 Home screen widget** (trigger scan cepat dari luar app) | Tinggi | Sedang | API `AppWidgetProvider` terpisah total dari Compose, tidak bisa diverifikasi visual sama sekali tanpa device asli, gagal-diam sulit dideteksi |
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
