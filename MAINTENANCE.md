# Catatan Perawatan (untuk Claude di sesi berikutnya)

Project ini dibangun lewat prompt-driven development: 72Faki tidak menulis
kode manual, semua lewat Claude yang mem-package ZIP. Karena tidak ada
Android SDK/Gradle di lingkungan kerja Claude (sandbox tanpa akses jaringan),
**kompilasi asli TIDAK BISA diverifikasi secara lokal oleh Claude** -- error
baru ketahuan setelah push ke GitHub Actions. Makanya disiplin di bawah ini
penting, bukan opsional.

## Cara tercepat "onboarding" di sesi Claude yang baru

Repo ini **publik** di `https://github.com/FDzaki-dev/PromptVault` (nama
repo TETAP `PromptVault` -- app-nya sendiri di-rebrand jadi **Sortify**
2026-08-27, lihat README.md). Kalau sesi baru dimulai dan user bilang
"lanjutkan project Sortify" ATAU "lanjutkan project PromptVault" (keduanya
merujuk project yang SAMA), cara PALING HEMAT WAKTU & TOKEN untuk dapat
konteks penuh:

1. `web_fetch` langsung 4 file ini (tidak perlu minta user upload ZIP dulu):
   - `https://raw.githubusercontent.com/FDzaki-dev/PromptVault/main/PROJECT_STATE.md` (paling penting -- keputusan arsitektur & riwayat insiden)
   - `https://raw.githubusercontent.com/FDzaki-dev/PromptVault/main/README.md`
   - `https://raw.githubusercontent.com/FDzaki-dev/PromptVault/main/CHANGELOG.md`
   - `https://raw.githubusercontent.com/FDzaki-dev/PromptVault/main/MAINTENANCE.md` (file ini sendiri, versi terbaru)
2. Kalau perlu lihat source code aktual (bukan cuma dokumentasi), fetch juga
   file .kt spesifik yang relevan lewat URL raw yang sama polanya, atau minta
   user upload ZIP terbaru kalau butuh keseluruhan project sekaligus.
3. **Jangan** asumsikan versi/state dari memori percakapan lama -- selalu
   cross-check ke `CHANGELOG.md` (entri paling atas = versi terkini) karena
   itu satu-satunya sumber kebenaran yang persisten lintas sesi.

Kalau `web_fetch` gagal (repo di-private-kan, URL berubah, dll), fallback:
minta user upload ZIP project terbaru dari `~/projects/PromptVault` atau
jalankan `cd ~/projects/PromptVault && git log --oneline -5` buat konfirmasi
versi HEAD saat ini.

## WAJIB sebelum kirim ZIP apapun

Jalankan **satu script** ini dari root repo (bukan lagi command manual
satu-satu -- semua audit sudah dikonsolidasi di sini):

```bash
bash scripts/preflight_check.sh
```

Exit code 0 = aman untuk di-zip. Exit code 1 = ada yang harus dibenerin dulu,
lihat baris bertanda ❌. Kategori #7 (fungsi lokal nested) selalu tampil
sebagai daftar untuk **direview manual** (bukan auto-fail), karena deteksi
otomatis "fungsi ini manggil @Composable atau tidak" tidak reliable lewat
grep semata.

Kalau nanti ketemu pola bug BARU dari log CI yang belum ke-cover script ini,
tambahkan kategori baru di `scripts/preflight_check.sh` -- jangan cuma catat
di dokumen ini saja, supaya sesi Claude berikutnya otomatis ikut kecek tanpa
perlu baca histori penambahan satu-satu.

## Versi & commit

`versionName`/`versionCode` di `app/build.gradle.kts` adalah SATU-SATUNYA
sumber kebenaran untuk versi. Nama ZIP yang dikirim ke user dan nama artifact
APK di CI SELALU diekstrak otomatis dari situ (`grep -oP 'versionName = "\K[^"]+'`),
tidak pernah diketik manual, tidak pernah ditempeli commit hash acak.

Lihat `CHANGELOG.md` untuk riwayat lengkap tiap versi -- entri paling atas
selalu versi terkini.

> Gotcha Gradle-version-pin & `| tee`+`pipefail` di CI dipindah jadi komentar
> inline langsung di step terkait `.github/workflows/build.yml` ("Setup
> Gradle" & "Decode keystore") -- sesi yang menyentuh file itu otomatis lihat
> aturannya persis di tempat yang relevan, tidak perlu disalin lagi di sini.

## Item kategori 7 preflight yang sudah diverifikasi aman (jangan re-cek manual tiap sesi)

- `Navigation.kt:8 addEditRule(...)` -- fungsi murni bikin string route, tidak
  panggil API Compose apapun. Aman.
- `MainViewModel.kt` (semua fungsi yang muncul) -- method biasa di dalam
  `ViewModel` class, bukan lambda/local function di dalam `@Composable`. Aman.
- `SettingsScreen.kt:61 chipColors(...)` -- SUDAH punya `@Composable` persis
  di baris sebelumnya (60). Aman, false-positive grep biasa (grep konteksnya
  hanya lihat baris definisi fungsi, bukan baris anotasi di atasnya).

Kalau daftar fungsi yang muncul di kategori 7 PERSIS sama seperti di atas
(nama & lokasi baris identik atau bergeser wajar karena edit lain), tidak
perlu re-review manual satu-satu -- cukup pastikan tidak ada ENTRI BARU yang
belum ada di daftar ini.
