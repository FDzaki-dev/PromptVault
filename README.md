# PromptVault v7.4.0

App Android offline untuk merapikan otomatis file (ekstensi apa saja) di folder
Downloads ke folder tujuan, berdasarkan rule pattern yang kamu buat sendiri.

Repo publik: https://github.com/FDzaki-dev/PromptVault

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
- `PROJECT_STATE.md` -- **WAJIB dibaca Claude di sesi manapun sebelum lanjut kerja**: batch/versi terakhir, keputusan arsitektur, dan riwayat insiden kronologis (termasuk bug yang sudah di-fix, untuk konteks jangka panjang)
- `FILE_MANIFEST.txt` -- daftar file yang di-track, dipakai buat percepat diff di sesi berikutnya
- `MAINTENANCE.md` -- **wajib dibaca Claude di sesi manapun sebelum lanjut kerja**: cara onboarding cepat, audit wajib sebelum ship, gotcha CI
- `TROUBLESHOOTING.md` -- panduan kalau build gagal atau app tidak berperilaku benar
- `scripts/preflight_check.sh` -- jalankan ini sebelum package ZIP apapun: `bash scripts/preflight_check.sh`

## Untuk melanjutkan project ini di sesi Claude baru

Cukup bilang ke Claude: *"lanjutkan project PromptVault, ini repo-nya:
https://github.com/FDzaki-dev/PromptVault"* -- Claude bisa langsung
`web_fetch` `CHANGELOG.md`/`MAINTENANCE.md` dari situ buat dapat konteks
penuh tanpa perlu upload ZIP ulang. Detail lengkap ada di `MAINTENANCE.md`.
