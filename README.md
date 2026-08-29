# Sortify

> **Rebranding 2026-08-27**: app ini sebelumnya bernama **PromptVault**.
> Nama tampilan (launcher, notifikasi, teks dalam app) sudah pindah ke
> **Sortify**. Nama repo GitHub, `applicationId`, nama folder default di
> Downloads/SAF, dan nama file/class Kotlin **SENGAJA TETAP** `PromptVault`
> -- itu semua kontrak fungsional lama (lihat `PROJECT_STATE.md` untuk audit
> lengkap scope rename), ganti itu = breaking change, bukan kosmetik.

App Android offline untuk merapikan otomatis file (ekstensi apa saja) di folder
Downloads ke folder tujuan, berdasarkan rule pattern yang kamu buat sendiri.

Repo publik: https://github.com/FDzaki-dev/PromptVault (nama repo tetap
`PromptVault`, lihat catatan rebranding di atas)

> **Versi**: `versionName` APK 100% otomatis dari nomor run GitHub Actions
> (`1.0.<run_number>`), BUKAN nomor semantik manual -- cek tab **Rilis**
> repo GitHub untuk versi terpasang terbaru. (README ini sengaja tidak
> hardcode nomor versi statis, karena pasti stale lagi tiap build CI baru.)

## Fitur utama
- Auto-sort file Downloads (atau folder kustom via SAF) berdasarkan rule
  pattern buatan sendiri, manual atau otomatis (WorkManager).
- **4 gaya tampilan** (pilih di tab "Tampilan", layar Home):
  Material3 (baseline default), Cupertino (iOS-look), Neumorphism
  "Blade Runner 2049" (Teal & Amber + Neon Magenta), dan Glassmorphism
  (kaca beku, dingin). Masing-masing punya identitas visual sendiri
  (warna/shape/typography/aksen) -- detail lengkap tiap sumbu ada di
  `PROJECT_STATE.md`.
- Undo pemindahan, Riwayat Aktivitas, Diagnostik, Crash Logger.

## Untuk user baru (bukan developer)

App-nya sendiri sudah punya panduan lengkap di dalam:
- **Saat pertama kali buka app**: onboarding 7 langkah otomatis muncul,
  menjelaskan cara kerja dasar, izin, ke mana file disortir, strategi
  konflik nama file, auto-sort, dan undo.
- **Kapan saja setelahnya**: buka menu **"Panduan Penggunaan"** dari layar
  Home (atau tombol di atas layar Pengaturan) untuk baca ulang semuanya
  tanpa perlu install ulang app.

Dokumen di bawah ini (CHANGELOG/PROJECT_STATE/dst) ditujukan untuk sesi
Claude yang melanjutkan pengembangan, bukan untuk end-user.

## Dokumentasi

- `CHANGELOG.md` -- riwayat lengkap tiap versi & alasan perubahan (baca dulu buat tahu state terkini)
- `PROJECT_STATE.md` -- **WAJIB dibaca Claude di sesi manapun sebelum lanjut kerja**: batch/versi terkini, status terkini, keputusan arsitektur, dan riwayat insiden kronologis (termasuk bug yang sudah di-fix, untuk konteks jangka panjang)
- `PROJECT_STATE_ARCHIVE.md` -- riwayat batch/versi LAMA (era pra-governance auto-versioning, sebelum 2026-08-27) yang sudah diarsipkan dari `PROJECT_STATE.md` supaya file itu tetap ringkas; baca hanya kalau butuh detail teknis batch lama
- `FILE_MANIFEST.txt` -- daftar file yang di-track, dipakai buat percepat diff di sesi berikutnya
- `MAINTENANCE.md` -- **wajib dibaca Claude di sesi manapun sebelum lanjut kerja**: cara onboarding cepat, audit wajib sebelum ship, gotcha CI
- `TROUBLESHOOTING.md` -- panduan kalau build gagal atau app tidak berperilaku benar
- `scripts/preflight_check.sh` -- jalankan ini sebelum package ZIP apapun: `bash scripts/preflight_check.sh`

## Untuk melanjutkan project ini di sesi Claude baru

Cukup bilang ke Claude: *"lanjutkan project Sortify (repo GitHub: PromptVault),
ini repo-nya: https://github.com/FDzaki-dev/PromptVault"* -- Claude bisa langsung
`web_fetch` `CHANGELOG.md`/`MAINTENANCE.md` dari situ buat dapat konteks
penuh tanpa perlu upload ZIP ulang. Detail lengkap ada di `MAINTENANCE.md`.
