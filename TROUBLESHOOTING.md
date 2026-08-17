# Troubleshooting PromptVault

Panduan cepat kalau ada masalah build/CI atau perilaku app yang aneh.

## 1. Build gagal di GitHub Actions

**Cara tercepat dapat detail errornya:**
1. Buka tab **Actions** di repo GitHub → klik run yang gagal (tanda ❌).
2. Kalau langkah **"Compile check (fail fast)"** yang merah: itu murni error
   sintaks/tipe Kotlin, belum menyentuh signing/APK sama sekali. Scroll ke
   baris yang diawali `e:` -- itu lokasi file + baris + pesan error persisnya.
3. Kalau ada artifact bernama `build-failure-log-vX.X.X` di halaman run itu,
   unduh & kirim ke Claude -- otomatis ter-generate begitu ada langkah yang
   gagal, jadi tidak perlu screenshot manual.

**Cara kirim ke Claude:** upload file log (txt/zip) itu langsung ke chat,
tulis "build gagal, ini lognya" atau sejenisnya. Claude akan baca error
`e: file:///...` -- itu bagian paling penting, bukan stack trace Gradle yang
panjang di bawahnya (itu cuma detail internal Gradle, boleh diabaikan).

## 2. Pola bug yang PERNAH terjadi di project ini (biar tidak terulang)

Ini bukan teori, ini kejadian nyata yang sudah pernah bikin build v1.9.0 gagal:

| Gejala | Penyebab | Fix |
|---|---|---|
| `Cannot access 'weight': it is internal` | Ada baris `import androidx.compose.foundation.layout.weight` di file. `weight` itu member `RowScope`/`ColumnScope`, BUKAN top-level function -- jangan pernah diimpor manual. | Hapus baris import-nya. `Modifier.weight(1f)` otomatis jalan di dalam `Row{}`/`Column{}` tanpa import apapun. |
| `Type 'State<X>' has no method 'getValue'` | Pakai `val x by someState` atau `by remember { mutableStateOf(...) }` tapi lupa `import androidx.compose.runtime.getValue` (dan `setValue` kalau `var`). | Tambah kedua import itu. |
| `Overload resolution ambiguity` pada `.background(...)` | Biasanya efek DOMINO dari error `getValue` di atas -- tipe jadi tidak jelas, Kotlin bingung pilih overload mana. | Selesaikan dulu error `getValue`-nya, error ini biasanya ikut hilang. |

## 3. Termux / git

Kalau `git push` gagal atau ada konflik struktur folder, minta Claude kasih
perintah diagnostik+perbaikan dalam satu paste (sudah jadi standar respons).

## 4. App jalan tapi rule tidak memindahkan file

1. Buka **Diagnostik** di app -- lihat daftar nama file ASLI di Downloads,
   bandingkan langsung dengan pattern rule kamu.
2. Buka **Tambah/Edit Rule** -- live preview di bawah field pattern langsung
   menunjukkan file mana yang cocok SEBELUM disimpan.
3. Setelah scan, kalau ada file dilewati, buka **Detail File Dilewati** di
   Home -- setiap file dikasih alasan spesifik (tidak cocok pattern / kena
   exclude / di luar batas ukuran / diduga masih ditulis / konflik nama).

## 5. Folder Tujuan Kustom (SAF) bikin folder "PromptVault (1)" duplikat

Riwayat panjang (v2.19.2 s/d v7.5.0, detail lengkap di `PROJECT_STATE.md`).
Ringkas:
- **Kalau masih pakai versi < 7.5.0**: update dulu ke versi terbaru --
  duplikat lama disebabkan race antar-coroutine + listing SAF stale, sudah
  ditutup total sejak v7.5.0 (resolusi folder serial + self-healing
  `resolveCanonicalRootDirSaf`).
- **Kalau sudah di versi terbaru tapi masih lihat folder "(1)"/"(2)" lama**:
  itu SISA dari sebelum update, app TIDAK menghapusnya otomatis (aksi
  destruktif di luar scope). Gabungkan isinya manual lewat file manager,
  lalu biarkan 1 folder "PromptVault" saja -- scan berikutnya akan konsisten
  pakai folder itu terus (dicek & di-log otomatis di Log Aktivitas kalau app
  masih menemukan >1).
- **Folder tujuan kustom persis "Documents"**: folder root "PromptVault" app
  akan fisik sama persis dengan `Documents/PromptVault/` yang juga dipakai
  crash logger internal (lihat #6 di bawah) -- bukan bug, cuma 2 subsistem
  beda numpuk di lokasi sama. App kasih info non-blocking soal ini di kartu
  Folder Tujuan Kustom (Pengaturan). Kalau mau pisah total, pilih SUBFOLDER
  di dalam Documents, bukan Documents-nya langsung.

## 6. App crash saat tekan Scan

Sejak v7.5.2, crash pertama sepanjang project (`UnsupportedOperationException`
di `FileSorter.findOrCreateChildDirSaf`) sudah di-fix -- update ke versi
terbaru dulu kalau masih kena ini.

Kalau app masih crash setelah update:
1. Buka **Diagnostik** di app -- daftar crash log tersimpan otomatis
   (`Documents/PromptVault/logs/crash_*.txt`, MediaStore, tidak butuh izin
   storage legacy), tanpa perlu ADB/Logcat.
2. Upload file crash log itu langsung ke chat Claude -- lebih akurat
   dibanding deskripsi gejala, dan JADI PRIORITAS pertama sebelum Claude
   minta Logcat manual (`adb logcat`).
3. Retention otomatis FIFO maks 50 log -- kalau butuh log lama yang sudah
   kehapus, tidak bisa dipulihkan, screenshot/salin isinya dulu kalau perlu
   disimpan lebih lama.
