# Catatan Perawatan (untuk Claude di sesi berikutnya)

Project ini dibangun lewat prompt-driven development: 72Faki tidak menulis
kode manual, semua lewat Claude yang mem-package ZIP. Karena tidak ada
Android SDK/Gradle di lingkungan kerja Claude (sandbox tanpa akses jaringan),
**kompilasi asli TIDAK BISA diverifikasi secara lokal oleh Claude** -- error
baru ketahuan setelah push ke GitHub Actions. Makanya disiplin di bawah ini
penting, bukan opsional.

## WAJIB sebelum kirim ZIP apapun

Jalankan audit statis ini (lewat bash_tool) sebelum mem-package ZIP:

```bash
# 1. Keseimbangan kurung di semua file .kt
find . -name "*.kt" | xargs -I{} sh -c 'python3 -c "
content = open(\"{}\").read()
b = content.count(\"{\") - content.count(\"}\")
p = content.count(\"(\") - content.count(\")\")
if b != 0 or p != 0: print(\"MISMATCH {}\", b, p)
"'

# 2. Import weight/align/matchParentSize yang salah (harus TIDAK ADA hasil)
grep -rn "^import androidx.compose.foundation.layout.weight$\|^import androidx.compose.foundation.layout.align$\|^import androidx.compose.foundation.layout.matchParentSize$" app/src/main/java/

# 3. 'var x by ...' harus punya getValue+setValue; 'val x by ...' cukup getValue
#    (cek manual tiap file yang pakai delegate 'by')
grep -rl "by remember\|by mutableStateOf\|collectAsState\|by .*Flow" app/src/main/java/com/elprompter/promptvault/

# 4. Import duplikat per file (harus TIDAK ADA hasil)
for f in $(find app/src -name "*.kt"); do
  dups=$(grep "^import " "$f" | sort | uniq -d)
  [ -n "$dups" ] && echo "DUP $f: $dups"
done

# 5. LazyColumn di dalam Column yang verticalScroll HARUS punya heightIn(max=...)
#    (kalau tidak, crash runtime "infinite height" di HP, bukan error compile)

# 6. Sejak app punya dark mode (v2.1.0): JANGAN referensi warna literal
#    (Pine/Stamp/Kraft/CardPaper/Ink/InkFaint/HairlineInk/Amber) langsung di
#    layar/komponen manapun KECUALI di ui/theme/Theme.kt sendiri. Semua warna
#    di layar/komponen HARUS lewat MaterialTheme.colorScheme.* supaya otomatis
#    ikut ganti terang/gelap. Cek dengan:
grep -rn "= Pine\b\|= Stamp\b\|= Kraft\b\|= CardPaper\b\|= Ink\b\|= InkFaint\b\|= HairlineInk\b\|= Amber\b" app/src/main/java/com/elprompter/promptvault/ui/ app/src/main/java/com/elprompter/promptvault/MainActivity.kt
# (hasil kosong = aman; kalau ada, itu di luar Theme.kt = bug)
```

## Soal versi Gradle di CI (penting, pernah bikin build gagal tanpa pesan jelas)

Runner GitHub Actions kadang sudah menyediakan Gradle versi sangat baru
(pernah ketemu 9.6.1) yang TIDAK KOMPATIBEL dengan AGP 8.5.2 yang dipakai
project ini. Kalau versi Gradle tidak dikunci, build bisa gagal dengan log
yang isinya cuma laporan deprecation warning yang tidak menjelaskan apa-apa
-- error compile aslinya bahkan bisa tidak ke-capture kalau workflow tidak
menyimpan output konsol asli.

Sejak v2.1.1, workflow CI sudah:
1. Generate Gradle Wrapper terkunci ke versi 8.9 (`gradle wrapper --gradle-version 8.9`)
   di awal job, lalu semua langkah berikutnya pakai `./gradlew` (bukan `gradle` polos).
2. Redirect output tiap langkah penting (`compileDebugKotlin`, test, `assembleRelease`)
   ke file log via `tee`, supaya kalau gagal, isi errornya (baris `e: file:///...`)
   betulan ke-capture di artifact `build-failure-log-vX.X.X`, bukan cuma laporan
   deprecation warning generik.

Kalau ke depan mau upgrade AGP/Gradle, pastikan cek tabel kompatibilitas resmi:
https://developer.android.com/build/releases/gradle-plugin#compatibility

## PENTING: `| tee` di CI WAJIB didahului `set -o pipefail`

Kalau step `run:` di workflow pakai pola `perintah 2>&1 | tee file.log`, TANPA
`set -euo pipefail` di awal, bash akan melaporkan exit code dari `tee` (yang
nyaris selalu 0/sukses), BUKAN dari perintah sebelumnya. Ini bikin step yang
sebenarnya GAGAL dianggap SUKSES oleh GitHub Actions, dan workflow lanjut ke
step berikutnya seolah tidak terjadi apa-apa -- baru gagal ambigu di step lain
yang bergantung pada output step sebelumnya (pernah kejadian: `assembleRelease`
gagal diam-diam, baru ketahuan pas step rename APK bilang file tidak ada).
Setiap kali menambah step baru yang pakai `| tee`, SELALU pastikan
`set -euo pipefail` ada di baris pertama block `run: |` itu.

Daftar ini akan bertambah setiap kali ada bug baru yang ketahuan dari log CI --
lihat riwayat commit `MAINTENANCE.md` untuk histori penambahan.

## Struktur proyek

- `app/src/main/java/.../data/` -- model + repository (DataStore-backed)
- `app/src/main/java/.../util/` -- logika murni (glob matcher, file sorter, dll),
  ini bagian yang PALING gampang di-unit-test (lihat `app/src/test/`)
- `app/src/main/java/.../worker/` -- WorkManager auto-scan + boot receiver
- `app/src/main/java/.../ui/` -- Compose screens, komponen, tema
- `.github/workflows/build.yml` -- CI: compile-check dulu (cepat), baru test,
  baru assembleRelease (lambat). Kalau gagal, log otomatis jadi artifact.

## Versi & commit

`versionName`/`versionCode` di `app/build.gradle.kts` adalah SATU-SATUNYA
sumber kebenaran untuk versi. Nama ZIP yang dikirim ke user dan nama artifact
APK di CI SELALU diekstrak otomatis dari situ (`grep -oP 'versionName = "\K[^"]+'`),
tidak pernah diketik manual, tidak pernah ditempeli commit hash acak.

Lihat `CHANGELOG.md` untuk riwayat lengkap tiap versi.
