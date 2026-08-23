# Changelog PromptVault

Semua versi dan alasan perubahannya, biar sesi Claude berikutnya (atau kamu)
punya konteks penuh tanpa perlu scroll chat lama.

## v8.28.1 (2026-08-23) — Border keemasan Neumorphism dikembalikan

Border sempat hilang tanpa sengaja saat emergency fix v8.28.0 (fokus
buang wrapper Box, tidak sadar `border` juga ikut tidak diteruskan).
Fix: `NeumorphTokens.GoldBorderColor` (pakai `Tertiary`, 0 hue baru) +
`GoldBorderWidth` (1.5dp), diwire ke kedua `Surface()` cabang
Neumorphism di `TactileSurface.kt` (hormati `border` caller eksplisit
kalau ada).

Preflight: 14/14 kategori PASS. versionCode 165→166, versionName
8.28.0→8.28.1.

## v8.28.0 (2026-08-23) — Fix regresi v8.27.0: tab "Tampilan" hilang + kartu blank
Root cause: wrapper `Box` (shadow-caster offset custom v8.27.0) bikin
`modifier` caller (mis. `Modifier.weight(1f)` di `SegmentedControl.kt`)
nempel di cucu Row, bukan anak langsung -- weight diabaikan, layout Row
rusak. Fix: wrapper `Box` + shadow offset custom dihapus total, balik ke
SATU `Surface(shadowElevation=)` polos (pola sama Glass/Material3). Fill
gradient tint (v8.27.0) tidak diubah, tetap sumber utama kesan timbul/cekung.

## v8.27.0 (2026-08-23) — Maksimalkan timbul/cekung Neumorphism (tanpa glow/blooming) + fix tone
Drop-shadow asli 1 sisi (`Surface.shadowElevation` default color, ikut bentuk
kartu) + tint gradient fill (Primary brand, bukan putih polos) -- 0 teknik
baru, 100% reuse primitif yang sudah stabil (shadow default + sheen brush
Glass). Border v8.26.0 dihapus (neumorphism otentik tidak pakai outline).
WCAG worst-case 5.13:1 AA.

## v8.26.0 (2026-08-23) — REVERT DARURAT: teknik shadow Neumorphism bikin seluruh UI washed-out

Laporan user (screenshot 2x + referensi rupa benar): Beranda dan Tampilan
sama-sama pudar total, hampir semua elemen nyaris tak terlihat. Root cause:
teknik "shadow ganda genuine" (v8.25.4, drawBehind+nativeCanvas+
setShadowLayer+gradient brush custom) — percobaan ke-4 efek "timbul"
Neumorphism, kali ini bukan cuma kurang kelihatan tapi merusak kontras di
SELURUH app (VaultTheme.style global → semua TactileSurface kena).

**Fix = revert total**: cabang NEUMORPHISM sekarang `Surface` M3 baku +
`BorderStroke` solid (1.5dp, putih 0.35f) — pola sama yang sudah stabil di
Material3 Murni. `NeumorphTokens.kt` ditulis ulang total, semua fungsi
brush/shadow custom dihapus, tinggal 2 token border.

2 file diubah. Confidence 85% — pakai API paling sederhana & terbukti
stabil, tapi belum bisa dites visual nyata.

**Pelajaran**: 4 percobaan efek timbul custom (v8.23.2→v8.23.6→v8.25.3→
v8.25.4) semua gagal dengan cara berbeda-beda. Kalau dicoba lagi:
verifikasi screenshot tiap iterasi, atau terima border-only sebagai batas
aman permanen.

versionCode 162->163, versionName 8.25.5->8.26.0.

## v8.25.5 (2026-08-23) — Fix compile error: 2 import hilang (ripple, toArgb)

Build failure log v8.25.4: compileDebugKotlin+compileReleaseKotlin FAILED,
6 error unik, semua "Unresolved reference" — real missing-import error,
bukan bug parser seperti batch sebelumnya.

`TactileSurface.kt`: `androidx.compose.material3.ripple.ripple` butuh
Material3 lebih baru dari compose-bom 2024.06.00 — ganti ke
`androidx.compose.material.ripple.rememberRipple()` (API lama, stabil).

`NeumorphTokens.kt`: 4x `.toArgb()` tanpa
`import androidx.compose.ui.graphics.toArgb` — ditambahkan.

2 file diubah. Confidence 90% — fix berdasar pesan compiler asli+presisi.

versionCode 161->162, versionName 8.25.4->8.25.5.

## v8.25.4 (2026-08-22) — GANTI TEKNIK KE-4: border bevel → dual soft-shadow genuine

User kirim palet+spec CSS literal (base #181a20, convex/concave dual
`box-shadow` eksak hex+offset+blur, accent neon 3-stop). Root cause
v8.25.3 (bevel border) masih "belum sempurna": garis tegas beda teknik
dari soft-shadow neumorphism genuine (2 shadow blur yg melebur ke
background, bukan garis presisi).

Fix: `Paint.setShadowLayer` dual-offset dual-warna (native Android,
terjemahan persis CSS box-shadow). `NeumorphTokens.kt` rewrite penuh
ke-4x (token 1:1 dari spec + `convexShadow()`/`concaveShadow()`
extension). `TactileSurface.kt` cabang Neumorphism: `Surface` M3 dilepas
(cuma solid Color) → `Box` + shadow drawBehind (layer bawah) +
clip+background brush (layer atas) + clickable+ripple manual.

Accent neon (pink-oranye-emas) DISEDIAKAN sbg token tapi TIDAK dipasang
ke elemen mana pun — brand accent app ini biru periwinkle konsisten
lintas seluruh app, ganti hue = rebrand global di luar scope. Dial
gauge/bottom-nav di image referensi murni mood-board gaya, bukan
permintaan fitur (app ini file-sorter, tidak relevan).

File diubah (2). Preflight 14/14 PASS langsung. versionCode 160→161,
versionName 8.25.3→8.25.4. **Paling kritis diverifikasi**: API native
`setShadowLayer` via Compose `drawIntoCanvas` belum ada preseden compile
di project ini — cek CI hijau dulu sebelum device test.

## v8.25.3 (2026-08-22) — GANTI TEKNIK: fill tint -> border bevel (Neumorphism)

Root cause final: fill wash (v8.24.0-v8.25.2) PASTI dibatasi alpha
rendah krn menimpa teks (WCAG) -- makanya selalu "flat"/tak kebaca.
Ganti teknik ke garis bingkai (border) 2dp diagonal terang->gelap di
TEPI shape saja -- area itu tidak pernah ditempati teks, jadi alpha bisa
0.65 (0 batas WCAG) & hasilnya jauh lebih terlihat sbg cekung/timbul.

## v8.25.2 (2026-08-22) — REVERT: glow-blob Neumorphism dihapus, fokus cekung+timbul

Teknik "glow blob luar kartu" (v8.25.0) bikin app "uncanny" (halo blur
acak vs referensi soft-UI genuine). Dihapus total -- kedalaman sekarang
murni dari tint gradient DI DALAM fill (highlight 0.16 = batas WCAG AA
presisi, shade 0.42, area gradient dilebarkan). Struktur `TactileSurface`
disederhanakan, konsisten dgn branch Glass/Material3.

## v8.25.1 (2026-08-22) — FIX build failure: regresi bug KDoc `[vX.Y.Z]`

Rewrite total `NeumorphTokens.kt` di v8.25.0 reintroduce bug KSP lama
(v8.23.5): tag `[v8.25.0]` di block comment tersandung parser referensi
KDoc. Fix: `(v8.25.0)`. + guard permanen baru di `preflight_check.sh`
(check #14) supaya kelas bug ini ketangkep otomatis ke depan.

## v8.25.0 (2026-08-23) — Root cause fix: shadow Neumorphism diganti total (Modifier.shadow -> Brush.radialGradient)
User bandingkan langsung dgn referensi desain asli -- kartu masih terlihat
flat walau v8.23.6 (naikkan alpha) + v8.24.0 (fill 3-lapis). Root cause:
`Modifier.shadow` (renderer Android View bawaan) tidak reliable render glow
warna terang di device fisik. Diganti `Brush.radialGradient` murni Compose
(3-stop falloff) + blob discale 1.7x & offset 16dp (naik dari 7dp) supaya
"meleber" jauh lebih luas & jelas kebaca. Fill 3-lapis v8.24.0 tidak disentuh.

## v8.24.0 (2026-08-23) — Neumorphism: fill "puffy" 3-lapis, shadow tidak disentuh
Fill Surface Neumorphism dulu 1 lapis flat polos -- ditambah 2 brush gradient
dekoratif (terang kiri-atas, gelap kanan-bawah) DI ATAS fill dasar, DI
BELAKANG content(), total 3 lapis per kartu -- kesan cembung/3D otentik ala
Neumorphism, bukan cuma shadow di tepi. Shadow ganda existing (v8.23.2/
v8.23.6) 0 baris diubah sesuai permintaan eksplisit. Alpha (0.14 terang/0.22
gelap) diverifikasi WCAG worst-case AA (4.84:1), margin sengaja disisakan.

## v8.23.6 (2026-08-23) — Fix: Neumorphism sama Material3 gak kelihatan beda

Laporan user (screenshot): toggle Neumorphism vs Material 3 Murni terlihat
identik. Root cause: kode branch sudah benar, tapi alpha shadow
`NeumorphTokens` (putih 0.06f/hitam 0.45f) jauh terlalu tipis — shadow cuma
kelihatan sebagai bleed tipis di luar konten, nyaris 0% kebaca di layar
nyata.

Fix: alpha putih 0.06f→0.35f, hitam 0.45f→0.70f, blur 12dp→18dp, offset
6dp→7dp. Teknik (offset Box + Modifier.shadow) tidak diubah, cuma kontras
dinaikkan.

1 file diubah: `ui/theme/NeumorphTokens.kt`. Confidence 70% — belum bisa
dites visual nyata di sesi ini.

versionCode 154->155, versionName 8.23.5->8.23.6.

## v8.23.5 (2026-08-23) — Fix root cause: bug parser KDoc KSP di tag [vX.Y.Z]

Build failure log v8.23.4: 6x "Closing bracket expected" di 4 file, semua
di baris komentar. Cek manual bracket per file — semua seimbang. Root cause
asli: KSP mem-parse `[...]` di KDoc sebagai referensi bernama, tersandung di
"." dalam tag `[v8.23.2]` (digit tidak valid sebagai lanjutan identifier
setelah titik). Tag `//` dengan pola sama di file lain tidak kena — KDoc
reference-parsing cuma aktif di `/** */`.

Fix: 6 tag `[v8.23.2]`/`[v8.23.4]` diganti `(v8.23.2)`/`(v8.23.4)` (kurung
biasa, bukan siku). 4 file diubah: SettingsRepository.kt, MainViewModel.kt,
ThemeStyleToggle.kt, Theme.kt.

Confidence 90% — pertama kali fix berdasar pesan compiler asli+posisi kolom
presisi, bukan tebakan.

versionCode 153->154, versionName 8.23.4->8.23.5.

## v8.23.4 (2026-08-23) — Saklar ON/OFF (bukan radio) + tema ke-3 (Material 3 Murni)

`ThemeStyleOption` tambah `MATERIAL3` (flat/opaque, restorasi perilaku
`TactileSurface` v8.0.0 pre-Glassmorphism). `ThemeStyleToggle.kt` ganti
UI dari radio-row jadi `TactileSwitch` per baris -- tetap mutually
exclusive (state tunggal dari DataStore), menyalakan 1 otomatis matikan
lainnya, menekan yang sedang ON di-ignore (cegah 0 gaya aktif).

Default tetap GLASSMORPHISM, 0 perubahan token warna/WCAG di batch ini.

Preflight: 13/13 kategori PASS. versionCode 152→153, versionName
8.23.3→8.23.4.

## v8.23.3 (2026-08-23) — FIX REGRESI: centering rusak akibat Glassmorphism

`Box` pembungkus sheen highlight (v8.23.1) tidak diberi
`propagateMinConstraints = true` -- memutus tight-constraint
(fillMaxWidth dll) yang tadinya mengalir otomatis lewat M3 `Surface`.
Efek: "Scan Sekarang" & ikon menu balik ke wrap-content, terlihat
nempel kiri-atas (bukan bug `contentAlignment`, tapi Box-nya sendiri
jadi sekecil kontennya).

Bug KEDUA ditemukan saat audit (belum sempat dilaporkan): cabang
Neumorphism (v8.23.2) 0 child anchor untuk ukuran Box (semua
`matchParentSize`), resiko collapse tinggi 0. Keduanya diperbaiki di
`TactileSurface.kt` -- 0 file caller disentuh.

Preflight: 13/13 kategori PASS. versionCode 151→152, versionName
8.23.2→8.23.3.

## v8.23.2 (2026-08-22) — Glassmorphism batch 2/N: toggle "Tampilan" LIVE + Neumorphism diimplementasikan penuh

`ThemeStyleOption` dipindah ke `SettingsRepository` (persisten DataStore,
default GLASSMORPHISM). `LocalThemeStyle` CompositionLocal baru di
`Theme.kt`, dikoleksi di `MainActivity.kt` & diteruskan ke seluruh app.

Neumorphism BARU diimplementasikan penuh (bukan placeholder):
`NeumorphTokens.kt` baru + `TactileSurface.kt` cabang total -- fill
opaque, sepasang shadow terarah (terang kiri-atas/gelap kanan-bawah),
0 border/sheen (beda total dari Glass, bukan varian dari itu).

Kedua gaya diaudit ulang 3 syarat user: murni (0 campur token), calm
(shadow netral, hue dasar tidak disentuh), WCAG (Neumorphism opaque =
kontras persis angka yang sudah diverifikasi).

Badge "Segera hadir" dihapus (sudah tidak akurat). `HomeScreen`/
`MainViewModel`/`MainActivity` diwire penuh (bukan lagi `remember` lokal).

Preflight: 13/13 kategori PASS. versionCode 150→151, versionName
8.23.1→8.23.2.

## v8.23.1 (2026-08-22) — Glassmorphism batch 1/N: engine visual di TactileSurface

File baru `GlassTokens.kt` (fill translucent alpha, border glass-edge,
sheen gradient). `TactileSurface.kt` diubah -- signature publik 0
berubah, semua call site otomatis dapat wajah glass. Hue/base color
TIDAK disentuh (tetap H222 calm, Color.kt v8.0.0 utuh).

Audit WCAG: backdrop app selalu sama-gelap-atau-lebih-gelap dari
surface manapun → translucency cuma bisa geser LEBIH gelap, kontras
teks selalu >= worst-case opaque yang sudah diverifikasi (termasuk
kasus terketat: label `SegmentedControl` di atas fill recessed).

⚠️ Glass berlaku global, BELUM diwire ke toggle `ThemeStyleToggle`
(scaffold v8.23.0, masih "Segera hadir" & 0 efek) -- butuh keputusan
user menyusul. Robolectric/test infra tetap dihapus permanen (v8.22.21).

Preflight: 13/13 kategori PASS. versionCode 149→150, versionName
8.23.0→8.23.1.

## v8.23.0 (2026-08-22) — Fitur baru (kerangka): tab "Tampilan" di Beranda, picker Glassmorphism/Neumorphism
Tab kedua di Beranda (`SegmentedControl` existing) berisi `ThemeStyleToggle`
baru -- 2 opsi selectable, badge "Segera hadir". KERANGKA MURNI sesuai
instruksi eksplisit: state cuma `remember` lokal, 0 wiring ke Theme.kt/
engine render, menekan opsi tidak mengubah tampilan app. Bug `--` di
komentar XML (kelas berulang ke-5) ketangkap preflight sebelum packaging.

## v8.22.21 (2026-08-22) — Rollback: Robolectric OOM terulang, sudah 2x mitigasi gagal
`testDebugUnitTest` exit 10 (OOM silent) identik v8.22.14 walau maxParallelForks=1
+ maxHeapSize=2048m sudah aktif jalan (baru bisa dites nyata sekarang). Sesuai
kontingensi tertulis v8.22.16: Robolectric + dependency terkait dihapus lagi,
`BootSurvivalWorkManagerTest.kt` dihapus (100% unreferenced). Reboot-survival
end-to-end jadi gap test terdokumentasi, bukan dites.

## v8.22.20 (2026-08-22) — Compile-fix: `unitTests.all{}` implicit receiver Kotlin DSL
`maxParallelForks`/`maxHeapSize` di `app/build.gradle.kts` diakses tanpa `it.`
prefix di dalam `all(Action<Test>)` -- Kotlin DSL tidak treat itu sbg implicit
receiver. Fix: `it.maxParallelForks` / `it.maxHeapSize`. 2 baris.

## v8.22.19 (2026-08-22) — Fix race condition di logging v8.22.18 sendiri

Log gagal v8.22.18 masih tanpa `decode-keystore.log`, padahal itu tepat
yang perlu dilihat. Root cause: `exec > >(tee file) 2>&1` (ditambahkan
v8.22.18) menjalankan tee di subshell background — kalau step exit cepat
(`exit 1` segera setelah baris exec), shell utama bisa selesai sebelum tee
sempat menulis apa pun ke disk. Race condition di fix sendiri, bukan bug
keystore.

**Fix**: ganti ke `{ block; } 2>&1 | tee file` (tee foreground, pipeline
biasa) di 4 step terkait — pola yang sudah terbukti reliable di "Compile,
test, build" sejak awal.

1 file diubah: `.github/workflows/build.yml`. Isi step-step itu sendiri
tidak diubah — belum ada bukti nyata sumber masalah aslinya.

versionCode 145->146, versionName 8.22.18->8.22.19.

## v8.22.18 (2026-08-22) — CI merah lagi, tapi fix v8.22.17 terbukti jalan (gap diagnostik, bukan bug baru)

Log gagal v8.22.17 masih tanpa `build-all.log` — tapi bukti kali ini beda:
`documentationLink` sekarang `docs.gradle.org/8.9/...` (bukan 9.7.0 lagi),
`totalProblemCount: 0`. **Fix v8.22.17 sukses** — step wrapper generation
jalan bersih pakai Gradle 8.9 yang benar.

Kegagalan sesungguhnya ada di step SETELAH itu (kemungkinan besar "Decode
keystore") — tapi tidak ada cara melihat errornya karena cuma step
"Compile, test, build" yang merekam output ke file sebelum ini.

**Fix batch ini**: semua step sekarang rekam stdout+stderr sendiri via
`exec > >(tee <nama>.log) 2>&1`, upload-on-failure ambil `*.log` (glob).
Logic step-step itu sendiri TIDAK diubah — belum ada bukti itu sumber
masalahnya.

1 file diubah: `.github/workflows/build.yml`. Kalau masih merah, log yang
di-upload sekarang akan berisi pesan error ASLI, bukan tebakan.

versionCode 144->145, versionName 8.22.17->8.22.18.

## v8.22.17 (2026-08-22) — Fix root cause CI: pin gradle-version (bukan bug Robolectric)

Build failure log v8.22.16 TIDAK punya `build-all.log` sama sekali — gagal
sebelum step compile/test/build sempat jalan. Fix Robolectric v8.22.16 belum
sempat diuji sama sekali.

**Root cause**: `configuration-cache-report.html` sisa di artifact menunjuk
ke docs Gradle 9.7.0 — runner image sudah bergeser ke Gradle ambien 9.7.0,
tidak kompatibel dengan AGP 8.5.2. Step "Setup Gradle" sebelumnya tanpa
`gradle-version:`, jadi step "Generate pinned Gradle Wrapper (8.9)" terpaksa
pakai Gradle ambien 9.7.0 untuk configure project dulu — crash sebelum
sempat generate wrapper 8.9.

**Fix**: `gradle-version: '8.9'` eksplisit di `setup-gradle@v3` — action
provision sendiri binary 8.9, tidak bergantung image runner. Permanen, bukan
tambal sekali kejadian.

1 file diubah: `.github/workflows/build.yml`. Confidence 70% — kalau lolos,
Robolectric v8.22.16 baru akan teruji pertama kali.

versionCode 143->144, versionName 8.22.16->8.22.17.

## v8.22.16 (2026-08-22) — Re-add Robolectric dengan fix OOM eksplisit

Lanjutan v8.22.15: reboot-survival test ditambah lagi, kali ini dengan 2
lapis mitigasi OOM (bukan cuma revert):

1. `testOptions.unitTests.all { maxParallelForks = 1; maxHeapSize = "2048m" }`
   di `app/build.gradle.kts` — eksplisit, bukan default Gradle.
2. CI (`build.yml`) sekarang cuma jalankan `testDebugUnitTest`, drop
   `testReleaseUnitTest` — project tidak punya test source set per-buildType,
   0 coverage hilang, cuma hilangkan 2 JVM Robolectric paralel.

Dependency sama persis dengan v8.22.14 (versi bukan penyebab OOM).
`worker/BootSurvivalWorkManagerTest.kt` ditulis ulang dari spesifikasi 4
skenario di log v8.22.14 (file lama sudah dihapus di rollback, tidak sempat
dibaca isinya) — reboot ON→ENQUEUED, reboot OFF→tidak ada worker aktif,
reboot ON→OFF→ON→konsisten, `AutoSortWorker.doWork()` nyata dieksekusi saat
OFF.

**Belum bisa diverifikasi compile/run nyata di sesi ini.** Confidence 75%.
**Pantau CI ekstra ketat** — kalau masih merah, opsi berikut: matikan
`org.gradle.parallel` global, atau terima reboot-survival end-to-end sebagai
gap test yang didokumentasikan.

versionCode 142->143, versionName 8.22.15->8.22.16.

## v8.22.15 (2026-08-22) — ROLLBACK: CI merah, Robolectric crash

CI benar-benar merah persis seperti diperingatkan di v8.22.14: `testReleaseUnitTest`
+ `testDebugUnitTest` gagal identik, "Gradle Test Executor... exit value 10",
0 assertion/stack trace — sinyal klasik Robolectric OOM (2 varian jalan
paralel via `org.gradle.parallel=true`, masing2 spawn JVM Robolectric berat
di atas runner CI terbatas).

**Rollback persis sesuai kontingensi yang sudah ditulis di v8.22.14**: hapus
4 dependency `testImplementation` Robolectric/androidx.test/work-testing +
block `testOptions.unitTests.isIncludeAndroidResources` di
`app/build.gradle.kts`, hapus `worker/BootSurvivalWorkManagerTest.kt`.
Kembali ke behavior v8.22.13. Tidak ada fitur lain yang kena — scope batch
v8.22.14 murni test infra.

Pending: kalau reboot-survival test diinginkan lagi, perlu `maxParallelForks`/heap
eksplisit di `testOptions.unitTests.all{}`, atau CI cuma jalankan 1 varian.

versionCode 141->142, versionName 8.22.14->8.22.15.

## v8.22.14 (2026-08-22) — Pending queue P2 #5-lanjutan: setup Robolectric + reboot survival test ⚠️

**RISIKO**: mengubah `app/build.gradle.kts` (dependency test baru) TANPA
verifikasi gradle/network di sesi ini. CI menjalankan test SEBELUM build
release DALAM SATU perintah — test gagal = release APK ikut gagal.
Confidence 80%. **Pantau run CI pertama ekstra ketat.**

Dependency baru (`testImplementation` saja, 0 pengaruh APK rilis):
`org.robolectric:robolectric:4.13`, `androidx.test:core:1.6.1`,
`androidx.test.ext:junit:1.2.1`, `androidx.work:work-testing:2.9.1`
(sinkron versi dgn `work-runtime-ktx`). + `testOptions.unitTests.isIncludeAndroidResources = true`.

Test baru `BootSurvivalWorkManagerTest.kt` (4 test, WorkManager ASLI via
Robolectric): reboot ON→ENQUEUED, reboot OFF→tidak ada worker aktif,
reboot berulang→state akhir konsisten, `AutoSortWorker.doWork()`
benar2 dieksekusi (bukan pure-logic gate) saat OFF→`Result.success()`.

Reboot disimulasikan panggil `WorkScheduler.rescheduleFromSavedSettings()`
langsung (badan kerja `BootCompletedReceiver`), bukan lewat
`onReceive()`/`goAsync()` (proteksi lifecycle proses OS, bukan logic
yang perlu diuji).

Preflight: 13/13 (cek statis, bukan `./gradlew test` beneran).
versionCode 140→141, versionName 8.22.13→8.22.14.

**Pending queue: KOSONG** — semua item audit 2026-08-22 tuntas.

## v8.22.13 (2026-08-22) — Pending queue P3 #6: Diagnostics bedakan toggle/WorkManager/next-run

`readWorkStatus()` (`DiagnosticsScreen.kt`) sekarang tampilkan 3 baris
terpisah: toggle Auto-Sort dari `SettingsRepository` (sumber kebenaran
Pengaturan, dibaca langsung), state WorkManager apa adanya, dan
`nextScheduleTimeMillis` (androidx.work 2.9.1) diformat jadi tanggal
atau "tidak diketahui". Fungsi jadi `suspend fun` (baca DataStore).

6 string baru, 1 string lama dihapus (`diag_status_fmt`, 100%
unreferenced setelah restrukturisasi). Scan logic/scheduling/worker
lain tidak disentuh.

Preflight: 13/13 kategori PASS. versionCode 139→140, versionName
8.22.12→8.22.13.

**Sisa pending**: P2 #5-lanjutan (setup Robolectric, reboot survival test).

## v8.22.12 (2026-08-22) — Pending queue P2 #5 (partial): pure-logic test lifecycle Auto-Sort

File baru `worker/AutoSortLifecycleLogic.kt` (5 fungsi pure) + rewire
`AutoSortWorker`/`WorkScheduler`/`AutoSortNotification` -- 0 perubahan
perilaku. Test baru `AutoSortLifecycleLogicTest.kt` (9 test JVM murni,
0 Robolectric): gate ON/OFF periodik, manual selalu jalan, scheduler
schedule/cancel, judul notif ongoing+hasil x manual/periodik.

Reboot survival end-to-end & eksekusi `doWork()` nyata TIDAK tercakup
(butuh Robolectric/work-testing, infra belum ada) -- diturunkan jadi
item pending baru yang lebih sempit. Diagnostics (P3 #6) tetap pending.

Preflight: 13/13 kategori PASS. versionCode 138→139, versionName
8.22.11→8.22.12.

## v8.22.11 (2026-08-22) — FIX: wording notifikasi Manual Scan (audit P2 #4)

`runScanAndReport` sekarang bawa parameter `isManual` -- notifikasi
widget/manual scan pakai title generik "Scan berjalan"/"Scan selesai",
auto-scan periodik tetap "Auto-sort berjalan"/"Auto-sort selesai". 2
item audit lain (test lifecycle, Diagnostics) masuk pending queue.

## v8.22.10 (2026-08-22) — FIX: validasi invariant Import Rule (audit P2 #3)

`RuleRepository.importFromJson` dulu percaya begitu saja isi JSON --
sekarang tiap rule difilter lewat `isValidImportedRule` (pakai
`validateRuleFolderName` yang sama dgn jalur manual) sebelum di-persist.
Rule invalid (folder traversal, pattern kosong, min>max) di-skip diam2.
+9 unit test baru. 3 item audit lain masuk pending queue.

## v8.22.9 (2026-08-22) — FIX: rename extensionless trailing dot (audit P1 #2)

`nextAvailableFileName` sekarang skip titik saat ekstensi kosong --
`README` (conflict) -> `README_1`, bukan `README_1.`. Test lama yang
mengunci bug ini diganti mengunci perilaku benar. 4 item audit lain
masuk pending queue.

## v8.22.8 (2026-08-22) — FIX: race condition scheduler ON/OFF (audit P1 #1)

`WorkScheduler` diserialkan pakai `Mutex` + baca DataStore FRESH di
dalam critical section (`syncFromSavedSettings`) -- toggle OFF user
tidak lagi bisa tertimpa coroutine startup/reboot yang telat jalan.
5 item audit lain (rename, validasi import rule, wording notif manual
scan, test lifecycle, diagnostics) masuk pending queue.

## v8.22.7 (2026-08-22) — Governance: proyek DISCONTINUED sampai bug baru

0 perubahan kode. Status proyek dibekukan atas permintaan user; lihat
`PROJECT_STATE.md` (section pinned teratas) untuk detail.

## v8.22.6 (2026-08-22) — FIX: dialog update gak informatif (cuma link)

Root cause: `generate_release_notes: true` di CI kosong isi krn repo push
langsung ke `main` (bukan alur PR). Fix: body Release sekarang diambil
dari section teratas `CHANGELOG.md` via `awk`, bukan auto-generate lagi.

## v8.22.5 (2026-08-22) — Tutup pending queue #2: chip preset stringResource

`RulePreset.label: String` (6 literal Kotlin) → `labelRes: @StringRes Int`
+ 6 string resource baru di `strings.xml`. Standar 100% stringResource
Fase 1.3 kembali penuh. Pending queue v8.15.0/v8.22.1: 0 item tersisa.

## v8.22.4 (2026-08-22) — FIX: sudut widget mismatch/ganggu vs system clip

Laporan user: sudut widget "ganggu banget" setelah fix resize v8.22.3.
Root cause: shape kita gambar radius+stroke sendiri (20dp + border warna
keras) DI DALAM area yang di Android 12+ sudah di-clip sistem dgn radius
berbeda — 2 rounding tidak match, dipertegas stroke solid.

Fix: stroke dihapus total, radius fallback API 26-30 turun ke 16dp. Baru
`drawable-v31/widget_scan_background.xml` — radius PERSIS ikut
`@android:dimen/system_app_widget_background_radius` (resmi API 31+),
0 mismatch krn sumber angka sama dgn launcher. 2 file terpisah krn dimen
sistem itu tidak exist di API<31 (qualifier `-v31/` cara resmi Android).

File diubah (1) + 1 baru. Preflight 13/13 PASS (1 iterasi fix `--` di
komentar, kelas bug berulang sama v8.5.0b/v8.6.0/v8.22.3). versionCode
130→131, versionName 8.22.3→8.22.4.

## v8.22.3 (2026-08-22) — FIX: widget di-resize jadi kotak kosong raksasa

Laporan user (screenshot): widget yang di-resize besar jadi kotak hitam
raksasa, icon+teks numpuk kiri-atas, sisa ruang kosong total. Fungsional
v8.22.2 (dynamic summary) sudah benar — ini murni cacat visual.

Root cause: `resizeMode` tanpa batas atas + konten RemoteViews fixed-size
tidak bisa scale. Fix: `maxResizeWidth="250dp"`/`maxResizeHeight="110dp"`
(API 31+, aman diabaikan di bawahnya) mengunci widget selalu ukuran
kartu shortcut ringkas — tidak bisa lagi dibesarkan jadi kotak kosong.
`gravity="center"` (2 sumbu) di layout sbg pelengkap utk device API<31.

File diubah (2): `widget_scan_info.xml`, `widget_scan.xml`. Preflight
13/13 PASS (1 iterasi fix `--` di komentar baru, kelas bug berulang sama
persis v8.5.0b/v8.6.0). versionCode 129→130, versionName 8.22.2→8.22.3.

## v8.22.2 (2026-08-22) — Tutup Pending Queue #1: widget dynamic scan summary

Widget dulu 100% stateless (teks aksi tidak pernah berubah walau scan
selesai). Sekarang `runScanAndReport` push ringkasan "N file • HH:mm" ke
`SettingsRepository` (key baru, pola persis `autoSortEnabledFlow`) +
langsung ke RemoteViews semua widget terpasang via `ScanWidgetProvider.
notifyScanCompleted` (no-op kalau tidak ada widget). `onUpdate` baca ulang
dari persistensi (`runBlocking`, DataStore lokal) supaya ringkasan
bertahan lintas resize/reboot. Builder RemoteViews di-refactor jadi 1
fungsi `buildWidgetViews` dipakai ulang kedua jalur update — wiring klik
tidak pernah beda.

File diubah (3) + 1 string baru: `data/SettingsRepository.kt`,
`worker/ScanExecution.kt`, `widget/ScanWidgetProvider.kt`, `strings.xml`.
Tidak disentuh: FileSorter, notifikasi sistem, `widget_scan.xml` layout.

Preflight 13/13 PASS. versionCode 128→129, versionName 8.22.1→8.22.2.
Pending Queue #2 (chip preset literal vs stringResource) tetap tertunda.

## v8.22.1 (2026-08-21) — Audit Polish: fix widget resize/distorsi (temuan lama belum tertutup)

"Audit polished 100%" — sweep menyeluruh (FILE_MANIFEST.txt vs disk 100%
akurat ✅, 0 hardcode Text() tersisa di screens ✅, 0 TODO aktif selain
catatan Fase 0 permanen ✅). 1 temuan konkret ditutup batch ini (sisanya
di-log sbg Pending Queue, lihat PROJECT_STATE.md — batas 1 task/batch):

**Widget resize/distorsi** (laporan user lama, screenshot resize — SEMPAT
disangka tertutup oleh fix "beta testing" v8.21.2, TERNYATA TIDAK): 2
TextView di `widget_scan.xml` masih `wrap_content` tanpa `maxLines`/
`ellipsize` sama sekali, dan layout horizontal v8.21.2 (icon+teks) malah
MENGURANGI ruang teks yang tersisa. Fix: `maxLines="1"`+`ellipsize="end"`
di KEDUA TextView, padding root 14dp->10dp. 1 file (`widget_scan.xml`).
`ScanWidgetProvider.kt` tidak disentuh (murni layout).

**Pending Queue (BELUM dikerjakan, ditunda batch berikutnya)**:
1. Widget masih 100% stateless — teks TIDAK PERNAH berubah walau scan
   selesai (laporan user lama, poin ke-3, juga belum tertutup).
2. Chip preset di `AddEditRuleScreen.kt` (v8.22.0) pakai label hardcoded
   Kotlin (`RulePreset("Gambar", ...)`), BUKAN `stringResource` — regresi
   kecil dari standar "100% stringResource" (Fase 1.3, SELESAI v8.16.2).

`preflight_check.sh` 13/13 lolos. Confidence Rating: **90%** (fix layout
sempit & mekanis). versionCode 127->128, versionName 8.22.0->8.22.1.

## v8.22.0 (2026-08-21) — Preset Cepat di tab Tambah Rule (edukasi user awam)

Instruksi eksplisit: "tambahkan preset cepat khusus tab tambah rule. biar
user awam ada gambaran gimana mekanisme rule sortir file yang benar".

**Fitur baru**: 6 chip preset (Gambar, PDF, Video, Arsip ZIP/RAR, Dokumen
Office, Screenshot) di atas form — HANYA tampil saat tambah rule baru (gate
`existingRule == null`, tidak muncul saat edit). Tap = isi `folderName`+
`pattern` otomatis, tetap bisa diedit manual, TIDAK menimpa exclude
pattern/filter ukuran yang sudah diisi user.

**Nilai edukasi (tujuan utama)**: pattern preset pakai CSV multi-ekstensi
(mis. `*.jpg, *.jpeg, *.png, *.webp, *.heic` — `GlobMatcher.matchesAny`
sudah mendukung ini dari awal, TIDAK ada perubahan di util/GlobMatcher.kt).
Begitu ditap, live preview yang SUDAH ADA (`onPreviewPattern`) langsung
jalan tunjukkan file Downloads mana yang cocok — user awam lihat loop
lengkap "pattern -> folder -> bukti file cocok" tanpa baca teks panjang.
Preset "Screenshot" SENGAJA beda gaya (prefix nama file `Screenshot_*.png`,
bukan cuma ekstensi) — menunjukkan 2 gaya pattern valid, bukan cuma satu
pola yang bisa disalahpahami user sbg satu-satunya cara.

**File diubah (2)**: `AddEditRuleScreen.kt` (+`RulePreset` data class,
`FlowRow`+`AssistChip`, pola sama dgn chip di SettingsScreen.kt), `strings.xml`
(+2 string). **Tidak disentuh**: FileSorter, GlobMatcher, Rule Engine,
validasi nama folder — preset cuma mengisi field yang sudah ada, nol logic
baru di layer data.

`preflight_check.sh` 13/13 lolos. Confidence Rating: **90%** (UI-only,
reuse pattern matcher & live preview yang sudah teruji). versionCode
126->127, versionName 8.21.3->8.22.0.

## v8.21.3 (2026-08-21) — FIX WAJIB: Auto-Sort OFF ikut blokir widget "Scan Sekarang"

Bug: `ScanWidgetProvider` enqueue `AutoSortWorker` untuk trigger manual --
padahal `AutoSortWorker` SENGAJA punya gate `autoSortEnabled` (v8.21.1).
Akibatnya "Auto-Sort OFF" ikut memblokir tombol widget, padahal manual scan
harus selalu jalan terlepas status auto-sort.

**Fix (pisahkan entry point, BUKAN sorting engine)**:
- BARU `worker/ScanExecution.kt`: extract-function `runScanAndReport()` --
  badan kerja scan+lapor (FileSorter/notifikasi/error-handling) PERSIS SAMA
  dgn isi lama `AutoSortWorker.doWork()`, dipakai ULANG oleh 2 worker di
  bawah. Nol logic sorting yang diduplikasi/diubah.
- `AutoSortWorker.kt`: gate `autoSortEnabled` DIPERTAHANKAN, badan kerja
  sekarang panggil `runScanAndReport()`.
- BARU `worker/ManualScanWorker.kt`: entry point manual (widget), TIDAK
  mengecek `autoSortEnabled` sama sekali, panggil `runScanAndReport()` yang
  SAMA.
- `widget/ScanWidgetProvider.kt`: enqueue `ManualScanWorker` (bukan
  `AutoSortWorker` lagi).

**Target final tercapai**: `AUTO: WorkManager -> AutoSortWorker -> gate ->
FileSorter` | `MANUAL: Widget -> ManualScanWorker -> FileSorter`.

**Tidak disentuh** (sesuai scope eksplisit): FileSorter, SAF, Shizuku, Rule
Engine, `WorkScheduler` (periodic tetap jadwalkan `AutoSortWorker`, tidak
berubah), sorting logic.

Test manual WAJIB di device (tidak bisa diverifikasi otomatis di sesi ini):
Auto-Sort OFF -> AutoSortWorker no-op; Auto-Sort OFF -> tap widget tetap
scan; Auto-Sort ON -> AutoSortWorker scan normal; tap widget -> notifikasi
hasil tetap muncul (lewat `runScanAndReport` yang sama).

`preflight_check.sh` 13/13 lolos. Confidence Rating: **88%** (logic gate
sederhana & straightforward, tapi WorkManager execution tetap butuh
verifikasi device asli). versionCode 125->126, versionName 8.21.2->8.21.3.

## v8.21.1 (2026-08-21) — FITUR: Auto-Sort ON/OFF benar-benar fungsional

Sumber: instruksi eksplisit user. Toggle master switch baru untuk scheduler
background: `SettingsRepository.autoSortEnabled` (default `true`, backward
compat). `WorkScheduler.rescheduleFromSavedSettings()` jadi titik pusat
ON→schedule/OFF→cancel — otomatis benarkan `PromptVaultApp`/`BootCompletedReceiver`
tanpa mengubah keduanya. `AutoSortWorker` dapat defensive gate untuk stale
worker. `setIntervalMinutes()` tidak lagi schedule diam-diam saat OFF. UI
toggle di SettingsScreen (reuse `TactileSwitch`), indikator Home tampil OFF.
`FileSorter.scanAndSort()` tidak disentuh — manual scan selalu jalan. Test
otomatis TIDAK ditambahkan (butuh Robolectric/mockk, tidak ada di project) —
dicatat sebagai technical debt. versionCode/versionName tetap 123/8.21.0.

## v8.21.2 (2026-08-21) — Fix cacat widget "vibes beta testing"

Instruksi langsung user. 3 cacat konkret diperbaiki: (1) `previewImage`
generik → tambah `previewLayout` (API 31+) supaya widget picker tampilkan
bentuk widget sebenarnya; (2) tidak ada feedback tap → background dibungkus
`<ripple>`; (3) tidak ada identitas visual → tambah icon app + layout
horizontal. `ScanWidgetProvider.kt` tidak disentuh (logic sudah benar).
3 file XML.

## v8.21.1 (2026-08-21) — Verifikasi build + lanjutan Audit UX 100%

Fitur Auto-Sort ON/OFF (batch sebelumnya) TERKONFIRMASI bekerja di device
asli via screenshot user. Lanjutan pending queue audit UX: OnboardingScreen
dikonfirmasi 0 TextField (N/A, bukan bug); predictive back gesture sudah OK
(manifest + nav-compose 2.7.7 native support, 0 BackHandler custom); kontras
disabled-state sudah OK (0 override eksplisit, default Material 3 dipakai).
Sisa pending queue (durasi animasi, landscape/tablet) — scope besar,
ditunda batch berikutnya. 0 file kode diubah, murni audit + version bump.

## v8.21.0 (2026-08-21) — Roadmap Fase 3.1: Widget Home Screen "Scan Sekarang"
User pilih eksplisit lewat 4 opsi Fase 3 (widget/cloud/lokalisasi/multi-profil).

**Fitur baru**: widget home screen 1-tap "Scan Sekarang" — trigger scan
Downloads tanpa buka app. SENGAJA stateless (tidak coba tampilkan hasil scan
langsung di widget) — hasil tetap lewat notifikasi sistem yang sudah ada
(`AutoSortNotification.resultNotification`), widget cukup Toast instan
konfirmasi tap diterima. Ini yang menjaga risiko tetap rendah dibanding
estimasi awal ROADMAP.md ("Tinggi" — gagal-diam sulit dideteksi tanpa device).

**File baru (4)**: `widget/ScanWidgetProvider.kt` (AppWidgetProvider,
`.enqueue()` biasa ke `AutoSortWorker` — class WorkManager yang SAMA PERSIS
dipakai auto-scan periodik, nol logic scan baru, aman dari race lewat
`scanMutex` statis existing di FileSorter), `res/layout/widget_scan.xml`
(RemoteViews, bukan Compose — platform tidak mendukung Compose di proses
widget), `res/xml/widget_scan_info.xml` (AppWidgetProviderInfo,
`updatePeriodMillis=0` — stateless, tidak perlu auto-refresh berkala),
`res/drawable/widget_scan_background.xml` (reuse warna `colors.xml` yang
sama dgn Compose `Color.kt`, biar tidak "beda app" di home screen).

**File diubah (3)**: `strings.xml` (+4 string, ikut konvensi 100%
stringResource sejak Fase 1.3), `AndroidManifest.xml` (protected asset, edit
parsial — 1 `<receiver>` baru), `app/build.gradle.kts` (versi).

**Nol dependency baru** — `AppWidgetProvider`/`RemoteViews` bagian framework
Android, konsisten prinsip yang sama dipakai `StatisticsScreen.kt` v8.20.0
(Canvas hand-rolled, bukan library chart).

**Insiden minor sesi ini**: 5 file XML baru awalnya GAGAL
`preflight_check.sh` kategori 10 (well-formedness) — komentar XML pakai "--"
(konvensi pemisah kalimat project ini), padahal spec XML MELARANG "--" di
dalam isi comment `<!-- -->` di mana saja, bukan cuma di ujung. Fix: semua
"--" di komentar XML diganti em dash "—". Kotlin/Markdown TIDAK kena aturan
ini (comment `/** */`/`//` bebas), jadi bug ini spesifik file XML baru saja.
**Pelajaran dicatat di sini supaya sesi berikutnya tidak mengulang** kalau
nambah file XML baru + komentar panjang gaya project ini.

`ROADMAP.md` diupdate: Fase 3.1 dicoret ✅ SELESAI. `preflight_check.sh`
13/13 lolos (setelah fix XML di atas). Confidence Rating: **80%** (lebih
rendah dari batch UI-only biasa — widget adalah surface Android BARU yang
sama sekali belum pernah diverifikasi visual/fungsional di sesi manapun,
device asli WAJIB sebelum dianggap benar-benar beres, sesuai peringatan
ROADMAP.md sendiri utk item ini).
versionCode 122->123, versionName 8.20.1->8.21.0.

## v8.20.1 (2026-08-21) — Fix cacat UI: chart Tren 14 Hari cuma tampil 4 batang
Laporan user + screenshot: layar Statistik judulnya "Tren 14 hari terakhir"
tapi cuma 4 batang terlihat di kanvas kosong -- kelihatan RUSAK ke user
beginner. Root cause: `TrendBarChart` di StatisticsScreen.kt gambar
`barHeight=0px` utk hari count=0 (0px = tidak terlihat), padahal data selalu
14 bucket lengkap (lihat `computeStatisticsData`). Fix: hari count=0 tetap
digambar stub pendek warna redup (bukan tinggi 0) -- 14 batang SELALU
terlihat, beda visual jelas 0-aktivitas vs ada-aktivitas. 1 file diubah
(StatisticsScreen.kt). preflight_check.sh lolos. Confidence: 92%.
versionCode 121->122, versionName 8.20.0->8.20.1.

## v8.20.0 (2026-08-21) — Roadmap Fase 2.3: halaman Statistik penuh

"Lanjutkan" setelah Fase 1-2.2 tuntas → ditanya dulu (prasyarat 2.3 belum
bisa dikonfirmasi Claude tanpa device asli) → user pilih lanjut, anggap 1.4
stabil.

- `StatisticsScreen.kt` baru: total sepanjang riwayat, grafik tren batang
  14 hari, breakdown per-rule — semua `Canvas` hand-rolled, nol library
  chart baru.
- Sumber data `MoveHistoryRepository`, pola sama `computeHomeStats()`
  v8.17.0 & `resultNotification` v8.19.0 (3 fitur share 1 sumber). Caveat
  cap 200 entri ditampilkan eksplisit di layar (caption), bukan cuma
  komentar kode.
- `MainViewModel.kt`: `StatisticsData` + `statisticsData` StateFlow +
  `computeStatisticsData()` murni.
- Protected Assets disentuh parsial: `Navigation.kt` (+1 route),
  `MainActivity.kt` (+1 composable, +1 param `HomeScreen`).
- Preflight 1 iterasi: kategori 5 false-positive dari komentar yang
  menyebut "LazyColumn"+"verticalScroll" sekaligus (menjelaskan kenapa
  tidak dipakai) — di-reword, bukan bug kode nyata.
- File: 6 diubah + 1 baru (`StatisticsScreen.kt`), 8 string baru.
- Preflight 13/13 PASS. **Belum lewat device asli** — user verifikasi: menu
  Statistik muncul di Home, grafik proporsional, breakdown per-rule urut
  desc, caption cap 200 kebaca.
- versionCode 120→121, versionName 8.19.0→8.20.0.

## v8.19.0 (2026-08-21) — Roadmap Fase 2.2: notifikasi hasil auto-scan per-rule

"Lanjutkan progress!!" tanpa area spesifik → dicek `ROADMAP.md`, item
berikutnya setelah 2.1 adalah 2.2: notifikasi hasil auto-scan diperkaya
ringkasan per-rule (sebelumnya tidak ada notifikasi hasil sama sekali,
cuma notifikasi ongoing "sedang berjalan").

- Breakdown per-rule diambil dari `MoveHistoryRepository` pasca-scan (pola
  sama `computeHomeStats()` v8.17.0) — `FileSorter.kt`/`ScanResult`/
  `MoveHistoryDao.kt` (Protected) nol disentuh, risiko regresi di jalur
  scan inti = nol.
- `AutoSortNotification.resultNotification()` baru, ID notifikasi terpisah
  (1002) dari ongoing (1001), `BigTextStyle` breakdown per-folder sort
  DESC by count.
- `AutoSortWorker.kt`: notif hasil HANYA kalau `filesMoved > 0` (cegah
  notification fatigue tiap siklus 240 menit kalau nihil). Try-catch
  terpisah termasuk `SecurityException` (POST_NOTIFICATIONS dicabut) —
  kegagalan notif tidak menggagalkan hasil scan yang sudah sukses.
- Caveat: breakdown per-rule derived by time-window
  (`timestampMillis >= scanStartMillis`), bukan dihitung langsung di titik
  pindah — praktis tidak meleset karena `scanMutex` mencegah 2 scan
  beriringan, dicatat apa adanya.
- File diubah (3): `AutoSortNotification.kt`, `AutoSortWorker.kt`,
  `strings.xml`. `ROADMAP.md` Fase 2.2 dicoret selesai.
- Preflight 13/13 PASS. **Belum lewat device asli** — user verifikasi:
  notifikasi hasil muncul terpisah dari ongoing, breakdown per-rule masuk
  akal, 0-file tidak memicu notifikasi.
- versionCode 119→120, versionName 8.18.0→8.19.0.

## v8.18.0 (2026-08-21) — Roadmap Fase 2.1: pencarian di Riwayat Aktivitas

Cross-check dulu ke kode: `RuleListScreen.kt` ternyata sudah punya search
lengkap (v2.24.0 lama) — cakupan nyata cuma `ActivityLogScreen.kt`. 1
search field shared 2 tab (filter `message` di Log, `fileName` di Undo),
disembunyikan saat mode seleksi-sapuan aktif, empty-state beda "data
kosong" vs "tidak ditemukan". Sweep-select & batch undo tidak terpengaruh
(tetap pakai `undoableHistory` param penuh).

`ROADMAP.md` Fase 2.1 dicoret selesai.

Preflight: 13/13 kategori PASS. versionCode 118→119, versionName 8.17.0→8.18.0.

## v8.17.0 (2026-08-21) — Roadmap Fase 1.4: statistik ringkas Home

Kartu ringkasan Home dapat 2 baris baru: jumlah file tersortir minggu
ini/bulan ini. Sumber `MoveHistoryRepository` (record per-file bersih),
dihitung regardless status undo. Caveat: repository di-cap 200 entri
(existing, utk fitur Undo) — bisa under-count kalau pemindahan bulan ini
melebihi 200 sebelum akhir bulan. Batas minggu/bulan pakai kalender via
`java.util.Calendar`, konsisten pola tanggal existing app.

`ROADMAP.md` Fase 1.4 dicoret selesai.

Preflight: 13/13 kategori PASS. versionCode 117→118, versionName 8.16.2→8.17.0.

## v8.16.2 (2026-08-21) — Roadmap Fase 1.3 batch 8/N (PENUTUP): ekstraksi string `MainActivity.kt`

Lanjutan `ROADMAP.md` Fase 1.3 — dicek ulang ke kode aktual dulu (bukan
percaya `ROADMAP.md` mentah yang belum sempat di-update): 7 screen lain
yang tercatat "sisa" ternyata sudah 100% `stringResource` (klaim v8.16.1
akurat). `MainActivity.kt`/`PermissionGate` (dialog izin runtime)
satu-satunya sisa nyata — 5 string dipindah ke `strings.xml` (prefix
`permission_gate_*`). Murni ekstraksi, nol perubahan perilaku.

Insiden minor sendiri, ketangkap preflight (kelas bug sama v8.5.0b/v8.6.0):
`--` di komentar XML baru, diganti `;`.

**Roadmap Fase 1.3 SEKARANG 100% SELESAI** — seluruh app (9 screen +
`MainActivity.kt`) sudah `stringResource` penuh, `ROADMAP.md` diupdate.

Preflight: 13/13 kategori PASS. versionCode 116→117, versionName 8.16.1→8.16.2.

## v8.16.1 (2026-08-21) — Audit UX area baru: hardcoded string/i18n

`SkippedFilesScreen.kt` satu-satunya screen terlewat migrasi
stringResource — 6 string dipindah ke `strings.xml`. Screen lain sudah
100% migrasi sebelumnya.

## v8.16.0 (2026-08-21) — Audit UX area baru: state restoration

Ketikan user di `AddEditRuleScreen.kt` (5 field) sebelumnya `remember`
biasa — hilang saat rotasi layar/process death. Fix: `rememberSaveable`.
Satu-satunya form input teks signifikan di app; state transient di
screen lain tidak terdampak.

## v8.15.6 (2026-08-21) — Tutup pending queue #3: disabled-contrast & animasi N/A, portrait lock

Kontras disabled-state & konsistensi durasi animasi diverifikasi N/A
(sudah sesuai/konsisten). Landscape/tablet: belum ada adaptive layout —
fix pragmatis `android:screenOrientation="portrait"` dikunci di
MainActivity untuk cegah layout pecah, adaptive tablet jadi roadmap
terpisah. Pending queue audit UX v8.15.0 kini tuntas.

## v8.15.5 (2026-08-21) — Aktifkan predictive back gesture

`targetSdk 34` tapi `android:enableOnBackInvokedCallback` belum
dideklarasikan (default false) — animasi preview swipe-back sistem
(Android 13+) tidak pernah jalan. Fix: 1 baris di `AndroidManifest.xml`.

## v8.15.4 (2026-08-21) — Tampilkan potongan release notes di Pembaruan

Layar Pengaturan > Pembaruan Aplikasi sebelumnya cuma bandingkan nomor
versi. `releaseNotes` (body GitHub Releases API) sudah ada di model tapi
belum pernah dirender — sekarang ditampilkan (maks 4 baris + ellipsis) +
tombol "Lihat rilis lengkap" buka halaman rilis di browser.

## v8.15.3 (2026-08-21) — Audit UX 100%, batch 3: OnboardingScreen verified N/A

Verifikasi manual `OnboardingScreen.kt` (pending queue v8.15.0 #2): murni
carousel info, 0 TextField — dikonfirmasi bukan bug. Tidak ada perubahan
kode.

## v8.15.2 (2026-08-21) — Pin aturan wajib sesi ke repo

Instruksi user: "Abadikan di repository". 2 rule (bump versi manual wajib
tiap sesi; box skrip commit tampil di atas heading "Update Harian:") kini
tertulis permanen di section pinned teratas `PROJECT_STATE.md`, bukan
cuma diingat via chat. Tidak ada perubahan kode.

## v8.15.1 (2026-08-21) — Audit UX 100%, batch 2: guard double-tap "Simpan"

Lanjutan pending queue v8.15.0. `AddEditRuleScreen.kt`: tombol "Simpan"
sekarang guard `isSaving` (pola sama seperti `undoInFlight` di
`ActivityLogScreen.kt`) supaya tap cepat 2x saat `onCheckBeforeSave`
(suspend) masih berjalan tidak memicu 2 proses cek/simpan bertumpuk.
Belum lewat gradlew/device asli — verifikasi manual diperlukan.

## v8.15.0 (2026-08-21) — Audit UX 100%, batch 1: fix nama file tanpa maxLines

Audit lintas semua layar+komponen. 1 bug nyata diperbaiki: nama file
mentah (`entry.fileName`/`info.fileName`/`entry.displayName`) tampil
tanpa `maxLines` di 3 layar -> word-break paksa di tengah token (bukti:
"AudioPlayer-v1.0.34-release-run146.apk" pecah 2 baris di screenshot
referensi sesi ini) + tinggi row tidak konsisten. Fix: `maxLines=1` +
`TextOverflow.Ellipsis` di `ActivityLogScreen.kt`, `SkippedFilesScreen.kt`,
`DiagnosticsScreen.kt` (4 titik). Pending queue (belum dikerjakan, batch
berikutnya): guard double-tap submit `AddEditRuleScreen.kt`, verifikasi
manual form `OnboardingScreen.kt`. Detail lengkap: `PROJECT_STATE.md`.

## v8.14.0 (2026-08-21) — Eksperimen percepatan kompilasi CI, batch 2

CI: 3 invocation `./gradlew` terpisah digabung jadi 1 step (config-cache
cuma configure project sekali, bukan per-invocation). Keystore decode
dipindah ke atas step gabungan (assembleRelease butuh signing sejak
awal). Log gabung ke `build-all.log`. `gradle.properties`: +
`ksp.incremental=true` (eksplisit, sudah default true, tidak ubah
perilaku). Detail + risiko: `PROJECT_STATE.md`.

**Update 2026-08-21**: CI CONFIRMED hijau (run #120, 6m23s, Success, 1
artifact) -- turun ~52s dari baseline v8.13.0 (7m15s). Tidak ada rollback
diperlukan.

## v8.13.0 (2026-08-21) — Eksperimen percepatan kompilasi CI

`gradle.properties`: `parallel`+`caching`+`vfs.watch` (stabil) +
`configuration-cache` (EKSPERIMENTAL, `problems=warn` supaya `System.getenv`
di `signingConfigs` cuma jadi warning bukan gagal build) + heap
2048m->3072m. Target: 3 invocation `./gradlew` terpisah per job CI
(compile->test->assembleRelease) saling reuse config cache. K2 compiler
SENGAJA tidak diaktifkan (belum stabil utk Compose di Kotlin 1.9.24).
Rollback kalau CI gagal: hapus 2 baris `configuration-cache*` saja.
Detail lengkap + alasan tiap baris: `PROJECT_STATE.md`.

**Update 2026-08-21**: CI CONFIRMED hijau oleh user (run #118, 7m15s,
Success, 1 artifact) -- tidak ada rollback diperlukan.

## v8.12.0 (2026-08-20) — Fitur: UI input PAT GitHub (opsional, hindari rate-limit updater)

Menyambungkan titik ekstensi yang sudah disiapkan sejak v8.5.0
(`UpdateRepository.checkLatestRelease()`/`downloadApk()` sudah terima
parameter `githubToken: String? = null`) — **0 baris `UpdateRepository.kt`
diubah** batch ini, murni penyimpanan + UI.

`SettingsRepository`: key DataStore baru `github_pat_token` (string,
nullable), pola identik `shizukuDestPathKey` (flow + get/set/clear).
`MainViewModel`: `StateFlow<String?> githubToken` + `setGithubToken()`/
`clearGithubToken()`, pola `stateIn` manual identik `shizukuDestPath`;
`checkForUpdate()`/`downloadUpdate()` diteruskan `githubToken.value`.
`MainActivity.kt` (protected asset, parsial): 1 `collectAsStateWithLifecycle`
+ 3 param baru diteruskan ke `SettingsScreen`.

`SettingsScreen.kt`: `OutlinedTextField` masked (toggle show/hide via ikon
`VpnKey`) di kartu "Pembaruan Aplikasi" (bawah deskripsi, atas tombol cek
update) + hint rate-limit + tombol Simpan (`action_save`, reuse) & Hapus
(`action_delete`, reuse, hanya muncul kalau token sudah tersimpan) — Simpan
disabled kalau input kosong/sama dgn tersimpan.

Preflight: 13/13 kategori PASS. versionCode 104→105, versionName 8.11.0→8.12.0.

**Belum diverifikasi CI hijau.**

## v8.11.0 (2026-08-20) — Roadmap Fase 1.3 (batch 7/N): ekstraksi string `ActivityLogScreen.kt`

26 string resource baru (`activitylog_*` + 1 `action_undo` generik)
menggantikan literal Kotlin di `ActivityLogScreen.kt` — MURNI ekstraksi,
nilai teks tetap sama persis.

Pola CAMPURAN (layar ini paling kompleks strukturnya sejauh Fase 1.3):
teks di scope composable langsung (topBar/actions lambda, `EmptyState`,
`items{}`, `pendingUndo?.let{}`/`pendingBatchUndo?.let{}` yang jalan
langsung di body composable) pakai `stringResource()` biasa. Teks di
dalam `onClick`/`onConfirm`/`scope.launch{}` (lambda non-composable) pakai
`context.getString()` — `val context = LocalContext.current` ditambah di
awal, pola identik `SettingsScreen.kt` v8.6.0.

`action_undo` REUSE 2x (label tombol `TextButton` per-baris & `confirmLabel`
`VaultActionSheet` konfirmasi tunggal) — teks identik "Undo", 1 sumber
kebenaran, konsisten pola `action_save`/`action_edit`/`action_delete`.

1 literal sempat terlewat di audit pertama (`SegmentedControl` tab
"Log"/"Undo Pemindahan") — ketangkap lewat re-grep manual sebelum ZIP
dipaket, ditambah sbg `activitylog_tab_log`/`activitylog_tab_undo`.

XML escaping: kutip literal nama file `\"%1$s\"` (preseden `pandu_section6_body`),
`&` di sweep hint → `&amp;`.

Preflight: 13/13 kategori PASS. versionCode 103→104, versionName 8.10.0→8.11.0.

**Belum diverifikasi CI hijau** — WAJIB dicek run Actions berikutnya.

**Sisa Fase 1.3**: `SkippedFilesScreen.kt`, `MainActivity.kt` (dialog
izin/error) — urutan bebas.

## v8.10.0 (2026-08-20) — Roadmap Fase 1.3 (batch 6/N): ekstraksi string `OnboardingScreen.kt`

18 string resource baru (`onboarding_*`) menggantikan literal Kotlin di
`OnboardingScreen.kt` — MURNI ekstraksi, nilai teks tetap sama persis.

Pola berbeda dari batch lain: `steps` sebelumnya `private val` top-level
(bukan di dalam scope composable) — `stringResource()` TIDAK BISA dipanggil
di situ. Diubah jadi `@Composable private fun onboardingSteps()`, dipanggil
sekali di awal body `OnboardingScreen` (list re-build murah, 7 item, tidak
perlu `remember`). Ini kandidat pola dipakai lagi kalau ada layar Fase 1.3
lain yang juga punya data top-level berisi teks (bukan cuma fungsi
non-composable seperti `DiagnosticsScreen`, tapi `val` murni).

XML escaping: `<nama rule>` → `&lt;nama rule&gt;` (preseden `pandu_section3_body`),
`&` → `&amp;` (2 titik), kutip literal `"PromptVault"`/`"Riwayat Aktivitas & Undo"`/
`"Panduan Penggunaan"` → `\"..\"` (preseden `pandu_section6_body`, bukan `&quot;`).
Tidak ada reuse string — semua teks Onboarding beda kalimat dari `pandu_*`
walau topiknya sama (Onboarding = ringkas per-langkah, Panduan = referensi
lengkap), sesuai keputusan v7.4.0 kedua konten SENGAJA berbeda gaya.

Preflight: 13/13 kategori PASS. versionCode 102→103, versionName 8.9.0→8.10.0.

**Belum diverifikasi CI hijau** — WAJIB dicek run Actions berikutnya.

**Sisa Fase 1.3**: `ActivityLogScreen.kt`, `SkippedFilesScreen.kt`,
`MainActivity.kt` (dialog izin/error) — urutan bebas.

## v8.9.0 (2026-08-19) — Roadmap Fase 1.3 (batch 5/N): ekstraksi string `HomeScreen.kt`

7 string resource baru (`home_*`) + 5 REUSE string yang sudah ada
menggantikan literal Kotlin di `HomeScreen.kt`.

Reuse (bukan duplikat): `"PromptVault"`→`app_name`, `"Kelola Rule"`→
`rule_list_title`, `"Panduan Penggunaan"`→`pandu_title`, `"Pengaturan"`→
`settings_title`, `"Diagnostik"`→`diag_title` — semuanya identik persis
dengan title layar tujuan navigasi masing-masing, jadi 1 sumber kebenaran.
`"Riwayat Aktivitas & Undo"` TIDAK direuse (beda dari title internal
`ActivityLogScreen` yang belum diverifikasi) — dibuat `home_menu_riwayat` baru.

`GroupedList.rows` adalah `List<@Composable () -> Unit>` (diverifikasi ke
source sebelum menulis kode) — `stringResource()` valid di dalam tiap
lambda karena lambda itu sendiri `@Composable`.

2 literal sengaja tidak diubah: `"$ruleCount"` (interpolasi dinamis) &
`"ctaScale"` (tag internal animasi, bukan teks user-facing).

Preflight: 13/13 kategori PASS. versionCode 101→102, versionName 8.8.0→8.9.0.

**Sisa Fase 1.3**: `OnboardingScreen.kt`, `ActivityLogScreen.kt`,
`SkippedFilesScreen.kt`, `MainActivity.kt`.

## v8.8.0 (2026-08-19) — Roadmap Fase 1.3 (batch 4/N): ekstraksi string `PanduanScreen.kt`

18 string resource baru (`pandu_*`) menggantikan literal Kotlin di
`PanduanScreen.kt` — MURNI ekstraksi, nilai teks tetap sama persis.

Beda karakter dari batch lain (dicatat sejak v8.3.0: paragraf besar,
batch tersendiri) — 100% dalam scope `@Composable`, tanpa fungsi
non-composable/callback, jadi pola paling sederhana sejauh ini:
`stringResource(R.string.pandu_*)` langsung di parameter.

XML entity escaping: `<nama rule>` → `&lt;nama rule&gt;`, 4 titik `&` →
`&amp;` (preseden `settings_shizuku_section_desc`). Divalidasi
`xml.dom.minidom.parse` sebelum preflight — 0 pelanggaran.

Preflight: 13/13 kategori PASS. versionCode 100→101, versionName 8.7.0→8.8.0.

**Sisa Fase 1.3**: `HomeScreen.kt`, `OnboardingScreen.kt`,
`ActivityLogScreen.kt`, `SkippedFilesScreen.kt`, `MainActivity.kt`.

## v8.7.0 (2026-08-19) — Roadmap Fase 1.3 (batch 3/N): ekstraksi string `DiagnosticsScreen.kt`

Lanjutan item ketiga `ROADMAP.md` setelah `SettingsScreen.kt` (v8.6.0).
Cakupan: `DiagnosticsScreen.kt` (25 string resource baru, prefix `diag_*`) —
MURNI ekstraksi, nilai teks tetap sama persis, tidak ada perubahan perilaku.

**Pola non-composable berbeda dari v8.6.0**: `readWorkStatus()` top-level
`private fun` (dipanggil dari `LaunchedEffect`, bukan callback lambda UI) —
3 string (none/fmt/error) di-resolve via `stringResource()` di badan
composable SEBELUM `LaunchedEffect` dimulai, diteruskan sbg parameter
fungsi biasa. Lebih sederhana dari `context.getString()` untuk kasus ini.

Fmt 3-parameter (`diag_crashlog_item_fmt`, `diag_status_fmt`) divalidasi
manual urutan `%1$s/%2$s/%3$d` cocok argumen `stringResource()`.

Preflight: 13/13 kategori PASS. `strings.xml` divalidasi
`xml.dom.minidom.parse` sebelum preflight — 0 pelanggaran, 0 `--` baru.
versionCode 99→100, versionName 8.6.0→8.7.0.

**Sisa Fase 1.3**: `PanduanScreen.kt`, `HomeScreen.kt`, `OnboardingScreen.kt`,
`ActivityLogScreen.kt`, `SkippedFilesScreen.kt`, `MainActivity.kt`.

## v8.6.0 (2026-08-19) — Roadmap Fase 1.3 (batch 2/N): ekstraksi string `SettingsScreen.kt`

Lanjutan item ketiga `ROADMAP.md` setelah cluster "Kelola Rule" (v8.3.0).
Cakupan: `SettingsScreen.kt` (layar terbesar dari sisa daftar, estimasi awal
~22 literal — realisasinya 70 string resource baru karena estimasi awal
tidak menghitung 6 status Shizuku + seluruh kartu updater v8.5.0 yang belum
ada saat roadmap ditulis). MURNI ekstraksi, nilai string tetap sama persis,
tidak ada perubahan perilaku.

**Pola non-composable-lambda** (3 titik, konsisten catatan teknis v8.3.0):
snackbar "disalin ke clipboard" jalan di dalam `scope.launch{}` dan hasil
import (`onImportRequested` callback) BUKAN scope `@Composable` — dipakai
`context.getString()` (`LocalContext.current`, val baru `context` di scope
fungsi) untuk pesan sukses import (butuh count dinamis saat callback jalan),
sedangkan 2 pesan gagal/kosong `import` di-resolve LEBIH AWAL sebagai
`stringResource()` di badan composable lalu ditangkap closure-nya (lebih
sederhana daripada `context.getString()` karena isinya statis).

**Insiden minor sendiri, langsung ketangkap**: komentar XML baru sempat
pakai `--` lagi (kelas bug berulang yang sama persis dengan v8.5.0b) —
ketahuan preflight kategori #10 SEBELUM commit, diganti `;`. Tidak
menyentuh string *content* (`--` di isi `<string>` aman, restriksi XML
cuma berlaku di dalam `<!-- -->`).

Preflight: 13/13 kategori PASS. versionCode 98→99, versionName 8.5.0→8.6.0.

**Sisa Fase 1.3** (independen, urutan bebas): `DiagnosticsScreen.kt` (~15),
`PanduanScreen.kt`, `HomeScreen.kt`, `OnboardingScreen.kt`,
`ActivityLogScreen.kt`, `SkippedFilesScreen.kt`, `MainActivity.kt`.

## v8.5.0c (2026-08-19) — COMPILE-FIX: `this@MainActivity` unresolved di `PromptVaultRoot`

CI run berikutnya: manifest fix (v8.5.0b) terkonfirmasi lolos, tapi
`compileDebugKotlin FAILED` — `Unresolved reference: @MainActivity` di
`MainActivity.kt:416`. `installApk(this@MainActivity, ...)` dipanggil dari
`PromptVaultRoot()`, fungsi `@Composable` top-level yang BUKAN member class
`MainActivity` — label itu tidak eksis di scope tersebut. Fix: pakai
`context` lokal (`LocalContext.current`, sudah ada di scope) — `installApk()`
memang cuma butuh `Context`. 1 baris, nol perubahan logika lain.
versionCode/versionName TIDAK naik (tetap 98/8.5.0). Belum diverifikasi CI
hijau.

## v8.5.0b (2026-08-19) — COMPILE-FIX: `"--"` di komentar `AndroidManifest.xml`

CI run #108 FAILED: `SAXParseException: "--" is not permitted within comments`,
baris 21 `AndroidManifest.xml` — komentar baru fitur in-app updater (poin
INTERNET/REQUEST_INSTALL_PACKAGES) pakai `--` sebagai pemisah kalimat.
Fix: ganti `--` jadi `;`, tidak ada perubahan logika/permission. Validasi:
`xml.dom.minidom.parse` (0 pelanggaran) + `preflight_check.sh` 13/13 kategori
PASS. versionCode/versionName TIDAK naik (tetap 98/8.5.0) — compile-fix murni.
Belum diverifikasi CI hijau — cek run Actions berikutnya.

## v8.5.0 (2026-08-19) — FITUR BARU: In-app Updater ("Release Downloader Spec")

Menutup gap audit rule preferensi: fitur auto-updater/downloader belum ada
sama sekali sebelumnya. Ditambahkan sesuai spesifikasi wajib proyek —
streaming chunk-by-chunk ke disk (Okio sink, BUKAN `readBytes()` penuh ke
RAM), timeout eksplisit (connect 15s, read 20s), `followRedirects(true)`
(asset GitHub Release selalu redirect 302 ke CDN S3), header
`Authorization: Bearer <token>` (opsional, repo publik) + `Accept:
application/octet-stream` saat request biner.

**Alur**: `UpdateRepository.checkLatestRelease()` — `GET
/repos/FDzaki-dev/PromptVault/releases/latest` (GitHub API), bandingkan
`tag_name` vs versionName terpasang (parsing numerik per-segmen, bukan
string compare polos — "8.10.0" > "8.9.0" ditangani benar), cari asset
pertama berakhiran `.apk`. `downloadApk()` — download ke
`cacheDir/updates/` via file sementara `.part` (baru di-rename ke final
SETELAH sukses penuh, gagal di tengah jalan tidak meninggalkan APK
"valid" palsu), progres dipancarkan tiap chunk 8 KB via callback.

**UI**: kartu baru "Pembaruan Aplikasi" di `SettingsScreen` — cek versi,
progress bar unduhan (determinate kalau server kirim Content-Length,
indeterminate kalau tidak), tombol "Pasang Sekarang" memicu dialog
installer sistem lewat `FileProvider` yang sudah dideklarasikan di
manifest sejak awal tapi belum ada pemakai nyata.

**Manifest**: tambah `INTERNET` (wajib utk network call) dan
`REQUEST_INSTALL_PACKAGES` (wajib Android 8+ utk memicu instal APK dari
luar Play Store).

**Versi**: 8.4.0 (97) → 8.5.0 (98). File baru: `update/UpdateModels.kt`,
`update/UpdateRepository.kt`. Bukan regresi — nol perubahan behavior utk
user yang tidak membuka kartu baru ini.

**Housekeeping terpisah, batch sama**: `FILE_MANIFEST.txt` diregenerasi
penuh dari isi disk sebenarnya — memperbaiki desync lama (`data/
BackupManager.kt` sempat tidak tercatat di manifest, ditemukan lewat audit
rule preferensi, BUKAN perubahan kode/fungsi apapun pada file itu sendiri).

## v8.4.0 (2026-08-18) — FITUR BARU: "Selamatkan Uninstall" — deteksi & restore config lama dari folder tujuan kustom SAF

Permintaan eksplisit user: kalau app tidak sengaja ter-uninstall lalu
diinstal ulang, dan user memilih folder tujuan kustom SAF yang SAMA (masih
berisi banyak file lama), app HARUS mendeteksi root folder yang sudah
pernah dibuat sebelumnya (bukan bikin duplikat baru) DAN menawarkan restore
config yang ikut hilang saat uninstall (rule, log, riwayat pemindahan, dsb).

**Bagian anti-duplikat folder**: SUDAH matang sejak v7.5.0/v8.x lewat
`FileSorter.resolveCanonicalRootDirSaf()` (self-healing regex+cache) —
TIDAK diulang/ditulis ulang di sini, sesuai pelajaran permanen Insiden #7.
Batch ini murni menambah lapisan BARU di atasnya: cermin/manifest config.

**Desain**: file JSON tersembunyi `.promptvault_config_backup.json`
(konstanta `VaultConfigBackup.BACKUP_FILE_NAME`) ditulis di root vault
"PromptVault" yang sama — berisi rule (string JSON, reuse
`RuleRepository.exportAsJson()`/`importFromJson()` yang sudah ada, bukan
skema baru), setting relevan (interval/conflict strategy/scan concurrency),
serta snapshot log aktivitas & riwayat pemindahan (masing-masing dibatasi
200 entri terbaru). BARU `util/VaultConfigBackup.kt` — murni I/O
serialisasi + 2 fungsi pure logic (`isPayloadWorthOffering`/`countRules`,
di-unit-test di `VaultConfigBackupTest.kt`).

**Tulis (opportunistic, best-effort)**: `FileSorter.syncConfigBackupToSaf()`
dipanggil dari 2 titik — (1) `MainViewModel` reaktif tiap rule berubah
(`rules` StateFlow, `drop(1)` supaya emisi awal tidak memicu sync palsu),
kalau folder SAF aktif; (2) `scanAndSortToDestination()` sendiri, setelah
tiap scan sukses ke tujuan SAF. Root vault TIDAK PERNAH dibuat lebih awal
cuma gara-gara sinkronisasi backup — dipakai varian peek-only
(`peekCanonicalRootDirSaf`, TIDAK pernah `createDirectory()`), root
"asli" tetap HANYA dibuat lewat jalur normal (`resolveCanonicalRootDirSaf`,
dipanggil scan). Kegagalan tulis SELALU ditelan diam-diam (try-catch) —
bukan gerbang yang boleh menggagalkan scan/simpan rule utama.

**Baca & tawarkan (SEKALI, dipicu picker)**: `MainViewModel.setSafTreeUri()`
memanggil `detectVaultRestoreOffer()` PERSIS SEKALI segera setelah URI baru
tersimpan (BUKAN reaktif berulang tiap buka Pengaturan). Kalau
`FileSorter.peekVaultBackup()` menemukan backup non-kosong, dialog
`VaultActionSheet` (reuse komponen konfirmasi standar app, BUKAN
`AlertDialog` baru) muncul di `SettingsScreen` menampilkan ringkasan
(jumlah rule/log/riwayat + tanggal backup) dengan 2 pilihan: "Pulihkan
Konfigurasi Lama" atau "Mulai Kosong Saja".

**Restore**: `FileSorter.applyVaultRestore()` — rule lewat
`RuleRepository.importFromJson()` yang sudah ada (merge by-id, di instalasi
baru = full restore otomatis); log & riwayat lewat `restoreEntries()` baru
di `ActivityLogRepository`/`MoveHistoryRepository` (insert via
`OnConflictStrategy.REPLACE` yang SUDAH ADA di DAO — dedupe otomatis
by-id, aman dipanggil 2x). Riwayat pemindahan (`MoveHistoryEntry`) SENGAJA
tetap direstore walau `destUri`/`originalParentUri` SAF berpotensi stale
pasca-reinstall — `FileSorter.undo()` SUDAH punya lapis try-catch/
verifikasi yang matang dari riwayat pengerasan berulang (v7.1.4 P0-3,
v7.1.9 OVERWRITE-delete, dst.), jadi kegagalan undo pada entri lama tetap
gagal DENGAN AMAN (hasil eksplisit, bukan crash), bukan risiko baru.

**Scope SENGAJA terbatas mode SAF saja** (bukan Shizuku) — Shizuku pakai
path manual, tidak ada titik "pilih folder" alami untuk memicu deteksi.

File diubah (6) + 2 baru: `util/FileSorter.kt` (6 fungsi baru + 1 hook di
`scanAndSortToDestination`), `data/ActivityLogRepository.kt`/
`data/MoveHistoryRepository.kt` (`restoreEntries` + mapper), `ui/MainViewModel.kt`
(StateFlow tawaran + detect/confirm/dismiss + hook reaktif), `ui/screens/SettingsScreen.kt`
(3 param baru + dialog `VaultActionSheet`), `MainActivity.kt` (protected
asset, edit parsial: 1 `collectAsStateWithLifecycle` + 3 param diteruskan),
`app/build.gradle.kts` (versi). BARU: `util/VaultConfigBackup.kt`,
`app/src/test/.../util/VaultConfigBackupTest.kt`.

Preflight check: 13/13 kategori PASS. **BELUM PERNAH lewat `./gradlew`
asli/device asli** (konsisten seluruh riwayat project) — risiko tambahan
di batch ini: (1) I/O `DocumentFile.createFile`/`openOutputStream` untuk
file JSON BELUM pernah dipakai project ini sebelumnya (SAF sebelumnya
hanya untuk memindahkan file user, bukan menulis manifest app sendiri),
(2) reactive `rules.drop(1).collect{}` di `MainViewModel` adalah pola baru
(StateFlow custom, bukan `Flow.combine`/`debounce` — sengaja dihindari
karena butuh anotasi eksperimental yang belum ada preseden di project ini).
**User WAJIB verifikasi**: (1) build CI hijau, (2) pilih folder SAF baru
(kosong) → isi rule → cek file `.promptvault_config_backup.json` muncul
di root "PromptVault" setelah scan pertama, (3) uninstall app (atau clear
data) → install ulang → pilih folder SAF yang SAMA → dialog "Konfigurasi
Lama Ditemukan" muncul dengan angka yang benar → konfirmasi → rule/log
lama kembali muncul di Kelola Rule/Riwayat Aktivitas, (4) pilih "Mulai
Kosong Saja" → pastikan TIDAK ada data yang berubah & root folder tetap
dipakai (bukan folder baru "(1)").

Confidence Rating: **80%** (arsitektur reuse jalur SAF yang sudah matang +
DAO conflict-replace yang sudah ada, tapi 2 permukaan I/O di atas baru
pertama kali dipakai project ini — lihat poin risiko di atas — turun dari
90%+ standar batch reuse-berat sampai lolos verifikasi CI/device pertama).

## v8.3.0-ci (2026-08-18) — FIX: Stale Run Guard di build.yml (anti-desync "Latest" release)

Debug session: user lapor "GitHub Release macet, masih app lama padahal fitur
baru selesai compile". Investigasi `gh run`/`gh release list` nemuin akar
masalah: re-run job Actions dari commit LAMA (v8.2.0, run 3209) sempat
jalan setelah v8.3.0 & v8.4.0 sudah publish -- `softprops/action-gh-release`
nandain v8.2.0 sebagai "Latest" (job hijau/sukses, bukan gagal, jadi gak
kelihatan di tab Actions).

**Fix**: step "Stale run guard (anti-desync)" ditambah di `build.yml` persis
setelah Checkout. Bandingkan `git ls-remote origin refs/heads/main` vs
`$GITHUB_SHA` -- kalau beda (stale re-run), `exit 1` sebelum compile/publish.

Tidak ada perubahan app (versionCode/versionName tetap 96/8.3.0). File
tersentuh: `.github/workflows/build.yml` saja (protected asset, edit
parsial 1 step). Immediate action user: `gh release edit v8.3.0 --latest`.

## v8.3.0 (2026-08-18) — Roadmap Fase 1.3 (batch 1/N): ekstraksi string cluster "Kelola Rule"

Item ketiga `ROADMAP.md`, batch pertama dari beberapa (sesuai catatan
roadmap "bertahap per layar"). Cakupan: `AddEditRuleScreen.kt`,
`RuleListScreen.kt`, `RuleCard.kt` -- MURNI ekstraksi (bukan lokalisasi,
nilai string tetap Bahasa Indonesia), 35 string resource baru di
`strings.xml` + 3 generik lintas-layar (`action_save`/`action_edit`/
`action_delete`).

**Detail teknis penting** (relevan utk batch string lanjutan di layar lain):
`stringResource()` HANYA valid dipanggil langsung di badan fungsi
`@Composable` atau di dalam lambda yang JUGA `@Composable` (mis. `label = {
Text(...) }` milik `OutlinedTextField`, atau argumen `title`/`message` yang
dievaluasi inline saat memanggil composable lain). **TIDAK valid** di dalam
lambda yang dieksekusi belakangan/non-composable: `LaunchedEffect { }`,
`onClick = { }`, `onConfirm = { }`. Untuk 2 kasus itu (snackbar di
`LaunchedEffect` & di `onConfirm` hapus rule, `RuleListScreen.kt`), dipakai
`LocalContext.current.getString(R.string.xxx, args)` sebagai gantinya --
`Context.getString()` fungsi biasa, bukan `@Composable`, jadi aman dipanggil
di mana saja asal ada referensi `Context`.

String dengan data dinamis pakai format positional Android baku (`%1$s`/
`%2$d`, dicek tipe argumen cocok Int/String) -- termasuk 3 kasus tricky:
`rule_edit_preview_summary` (2 argumen Int), `rule_edit_confirm_duplicate`
(2 argumen String dari 2 rule berbeda), `rule_list_delete_message` (2
argumen String dari 1 rule yang sama). XML comment sempat 1x salah (`--`
literal di dalam `<!-- -->`, terlarang di spec XML) -- ketahuan lewat
validasi `python3 -c "xml.dom.minidom.parse(...)"` sebelum commit, sudah
diperbaiki (diganti em dash `—`).

**Layar lain (Settings 22 literal, Diagnostics 15, Panduan 9 paragraf besar,
Home/Onboarding/ActivityLog/SkippedFiles/MainActivity) BELUM disentuh** --
menyusul di batch 1.3 berikutnya, urutan bebas (independen satu sama lain,
tidak ada dependency antar layar).

Preflight check: 12/13 kategori PASS otomatis (kategori 10 -- well-formedness
XML -- eksplisit mengonfirmasi `strings.xml` valid setelah fix di atas),
kategori #7 identik daftar baseline. versionCode 95→96, versionName
8.2.0→8.3.0.

## v8.2.0 (2026-08-18) — Roadmap Fase 1.2: audit aksesibilitas TalkBack menyeluruh

Item kedua `ROADMAP.md`. Audit MENYELURUH (bukan cuma 1 layar) atas semua
`Icon`/`IconButton`/elemen clickable custom di app -- 1 file diubah, karena
gap nyata yang ditemukan HANYA ada di 1 komponen (efisien, sesuai moto
low-risk/high-value: tidak menyentuh yang sudah benar).

**Diaudit (9 layar + semua komponen bersama)**: `HomeScreen`,
`RuleListScreen`, `AddEditRuleScreen`, `SettingsScreen`, `ActivityLogScreen`,
`SkippedFilesScreen`, `DiagnosticsScreen`, `OnboardingScreen`,
`PanduanScreen`, `MainActivity`, + `RuleCard`/`GroupedListRow`/`EmptyState`/
`WarningBanner`/`VaultTopBar`/`TactileSwitch`.

**Temuan**: label `contentDescription` SUDAH benar di semua 17 titik Icon/
IconButton yang diperiksa (ikon aksi standalone sudah berlabel jelas --
"Kembali", "Naikkan prioritas", "Salin Log", dst; `contentDescription = null`
HANYA dipakai pada ikon dekoratif yang selalu berdampingan teks, pola yang
sudah benar). Target sentuh 48dp juga sudah terpenuhi di semua tempat KECUALI
1: **`SegmentedControl`** (dipakai tab "Log"/"Undo Pemindahan" di
`ActivityLogScreen`).

**2 fix di `SegmentedControl.kt`** (murni aditif, TIDAK menyentuh
`onClick`/`interactionSource`/`pressScale` yang sudah teruji):
1. Tidak ada semantics `selected`/`Role.Tab` sama sekali -- screen reader
   cuma baca label polos tanpa tahu tab mana yang aktif. Ditambah
   `Modifier.semantics { role = Role.Tab; selected = ... }` per segmen +
   `Modifier.selectableGroup()` di Row induk.
2. Tinggi target sentuh ~38dp (padding vertikal 9dp), di bawah standar
   Android 48dp. Padding dinaikkan ke 14dp (14+14+lineHeight20dp=48dp).

Preflight check: 12/13 kategori PASS otomatis, kategori #7 identik daftar
baseline (0 entri baru). versionCode 94→95, versionName 8.1.0→8.2.0.

## v8.1.0 (2026-08-18) — Roadmap Fase 1.1: unit test untuk logika inti FileSorter

Item pertama `ROADMAP.md` (low-risk/high-value). 4 fungsi PURE diekstrak
MURNI (perilaku 100% identik, no-op refactor, bukan rewrite) dari method
private `FileSorter` jadi top-level function -- pola sama persis dgn
`mimeTypeForFileName` yang sudah ada (unit-testable tanpa
Context/Robolectric):
- `isTempOrPartialName` (dulu private instance method)
- `explainNoMatchByName` (dulu private instance method)
- `buildPreviewResult` (dulu private instance method)
- `nextAvailableFileName` -- **satu-satunya yang genuinely baru** (bukan
  cuma pindah lokasi): loop rename-saat-konflik di `moveFile` dulu inline
  baca `File.exists()` langsung (tidak bisa diuji terisolasi), sekarang
  predikat `exists` di-inject caller -- produksi tetap pakai `File.exists()`
  asli, test pakai `Set<String>` palsu. Bug-for-bug parity dipertahankan
  SENGAJA (nama file tanpa ekstensi tetap hasilkan trailing dot
  `"nama_1."`, bukan "diperbaiki" diam-diam -- fix perilaku itu di luar
  scope batch ini).

`FileSorterPureLogicTest.kt` baru (12 test case): temp-marker detection,
alasan no-match (exclude/size/no-match), preview pattern match+exclude,
rename-conflict counter (0/1/banyak konflik + kasus extensionless).

**Tidak disentuh** (di luar cakupan "pure logic"): `scanAndSort`,
`moveFile` (isi lain), semua jalur SAF/Shizuku -- masih butuh device asli/CI
seperti sebelumnya, lihat `MAINTENANCE.md`.

Preflight check: 12/13 kategori PASS otomatis, kategori #7 identik daftar
baseline (0 entri baru). versionCode 93→94, versionName 8.0.0→8.1.0.

## v8.0.0 (2026-08-18) — Rombak Total Tema: Material 3 Murni

Permintaan eksplisit user: "Rombak total theme aplikasi jadi default
Material 3 murni, pendekatan Premium Tactile experience, base warna calm
bukan warm, tetap sesuai standar WCAG." Atomic change, 19 file tersentuh
(1 file baru, 1 dihapus) -- batch limit 10 file dilampaui dgn justifikasi:
seluruh perubahan saling terikat erat, kompilasi gagal kalau parsial.

**Dihapus total:**
- `GlassPanel.kt` (primitif Glassmorphism: border kaca, gradient highlight,
  shadow warna kustom) → diganti `TactileSurface.kt` (Surface M3 baku,
  `tonalElevation`+`shadowElevation`)
- Toggle preset ganda `useAltTheme` (Deep Navy+Brass / Charcoal+Copper,
  v7.1.0) beserta seluruh infra-nya (`SettingsRepository` key/flow/setter,
  `MainViewModel` StateFlow/setter, seksi "Tema" di `SettingsScreen`,
  `SideEffect` reaktif di `MainActivity`) -- "default M3 murni" = SATU
  ColorScheme baku, bukan 2 preset kustom untuk dipilih
- Semua token literal `Glass*`/`Brass*`/`Copper*`/`Charcoal*`/
  `AmoledBackground`

**Diganti/ditambah:**
- `Color.kt`: 1 skema tonal M3 (seed biru calm H222 utk primary/neutral,
  amber H42 KHUSUS warning, merah H8 utk error). Semua pasangan teks/ikon
  diverifikasi WCAG manual (relative luminance W3C) -- lihat komentar
  kontras per grup warna di file
- `Shapes.kt`: skala corner radius baku M3 (4/8/12/16/28dp), dari skala
  kustom 8/12/16/20/28dp ("kesan iOS") sebelumnya
- `Type.kt`: 15-style type scale baku M3, dari gaya kustom "Apple large
  title" sebelumnya
- `TactileTokens.kt`: token elevasi `Glass*` → `Tactile*`, dikalibrasi ke
  nilai M3 Elevation Level asli (1dp/3dp/6dp; 6dp = elevasi default FAB
  M3 baku, dipakai utk CTA)
- `colors.xml`: `pv_amoled_background` & `pv_brass_accent`→`pv_primary_accent`
  diupdate ke hex baru selaras `Color.kt`

**Dark-only tidak diubah** (keputusan v3.0.0 dipertahankan -- user minta
rombak warna/tema, bukan Light mode baru).

versionCode 92→93, versionName 7.5.2→8.0.0. Preflight check: 12/13 kategori
lulus otomatis, 1 kategori (#7, review manual fungsi lokal) diperiksa manual
-- tidak ada temuan baru dari batch ini.

## v7.5.2b -- Docs-only: README/TROUBLESHOOTING ketinggalan selama saga SAF & duplikasi (2026-08-17)
User minta lanjut beresin dokumen Markdown yang terbengkalai (fokus dev
sesi-sesi sebelumnya habis di krisis SAF/duplikasi & crash). Tidak ada
perubahan kode/logic.

- `README.md`: header versi basi (v7.4.0) -> v7.5.2.
- `TROUBLESHOOTING.md`: 0 entri soal SAF/duplikasi folder & crash app
  meski itu saga terbesar project (v2.19.2-v7.5.2) -- ditambah §5 (folder
  "PromptVault (1)" duplikat, cara resolusi + kondisi "Documents" overlap)
  dan §6 (app crash saat Scan, cara pakai Diagnostik/crash log internal
  ketimbang minta Logcat manual).
- `preflight_check.sh` 13/13 lolos. versionCode/versionName TIDAK berubah
  (docs-only, no behavior change -- akhiran "b" cuma penanda batch di
  changelog, bukan versi rilis baru).

## v7.5.2 -- FIX crash pertama produksi: UnsupportedOperationException saat scan ke tujuan kustom SAF (2026-08-17)
User lapor crash pertama sepanjang project (log `crash_20260817_174626_f7fac68a.txt`,
device Infinix X6855, Android 16) -- terjadi persis saat tekan Scan setelah
edit rule. Stack trace: `SingleDocumentFile.listFiles` -> `DocumentFile.findFile`
-> `FileSorter.findOrCreateChildDirSaf:846`.

**Root cause**: `findOrCreateChildDirSaf()` dan `resolveCanonicalRootDirSaf()`
merekonstruksi folder ter-cache pakai `DocumentFile.fromSingleUri()`. Fungsi
itu SELALU menghasilkan `SingleDocumentFile`, yang `listFiles()`-nya
unconditionally `throw UnsupportedOperationException` (hardcoded di androidx,
BUKAN bergantung apakah URI-nya folder asli atau bukan). Objek "cached" itu
lalu dipakai lagi sbg `parent` di panggilan `findOrCreateChildDirSaf`
berikutnya (subfolder rule di bawah root vault) -> `parent.findFile(name)` ->
crash. Cache-by-Uri (v7.1.5) sendiri sudah benar secara desain, tinggal salah
pilih fungsi rekonstruksi.

**Fix (1 file, 2 titik)**: `FileSorter.kt` -- `fromSingleUri()` diganti
`DocumentFile.fromTreeUri()` di kedua titik rekonstruksi cache. URI yang
di-cache SELALU berasal dari `.uri` child `TreeDocumentFile` (mengandung
segmen `/tree/`), jadi `fromTreeUri()` membangun ulang `TreeDocumentFile`
yang benar -- `listFiles()`/`findFile()`/`createDirectory()` tetap normal.

**Terkait v7.5.1**: user memang pakai folder tujuan kustom = "Documents"
(overlap dgn `Documents/PromptVault/logs/` milik `CrashLogger.kt`, sudah
diberi info non-blocking di v7.5.1) -- overlap ITU SENDIRI bukan penyebab
crash ini (2 subsistem storage beda, tidak saling panggil `listFiles()` satu
sama lain), tapi kemungkinan memperbesar peluang cache folder sempat "dingin"/
dibaca ulang (app dibuka lagi setelah lama) sehingga jalur `fromSingleUri()`
yang buggy ini lebih sering terpakai dibanding kalau selalu createDirectory()
baru. Root cause sebenarnya murni salah pilih API DocumentFile, independen
dari lokasi folder yang dipilih user.

**Batas jujur**: belum lewat `./gradlew`/device asli (keterbatasan permanen
lingkungan Claude). Fix ini defensif & spesifik match stack trace yang
diberikan user (bukan tebakan luas) -- **user WAJIB verifikasi**: edit rule
lalu tekan Scan berkali-kali ke folder tujuan kustom yang sama, pastikan
tidak crash lagi & subfolder rule tetap konsisten (tidak duplikat).

versionCode 91->92, versionName 7.5.1->7.5.2.

## v7.5.1 -- Info non-blocking: folder tujuan kustom = "Documents" langsung overlap dgn folder crash log (2026-08-17)
User (setelah diskusi root cause v7.5.0) tanya & KONFIRMASI: folder tujuan
kustom yang dia pakai persis "Documents" (root storage utama), bukan
subfolder. Ini overlap PERSIS dengan `CrashLogger.kt` yang nulis ke
`Documents/PromptVault/logs/` lewat MediaStore -- subsistem storage BEDA
dari SAF yang dipakai FileSorter buat folder tujuan kustom.

**Perubahan (1 file)**:
- `ui/screens/SettingsScreen.kt`: BARU `isSafRootDocumentsFolder()` (reuse
  pola parsing `friendlySafFolderLabel`) -- deteksi kalau `safTreeUri`
  persis root "Documents" storage utama. Kalau `true`, tampilkan 1 baris
  info (`colors.tertiary`, BUKAN error/warning merah -- ini bukan masalah
  yang wajib ditindak) di kartu Folder Tujuan Kustom: menjelaskan overlap
  dgn crash log + `resolveCanonicalRootDirSaf` (v7.5.0) sudah menangani
  otomatis kalau bentrok, plus opsi pisah total (pilih subfolder).
- TIDAK mengubah `CrashLogger.kt` (path `Documents/PromptVault/logs/`
  adalah spek baku project ini utk SEMUA app, bukan bug lokal PromptVault
  -- ubah sepihak berisiko inkonsistensi lintas project) maupun logika
  `FileSorter.kt` (mekanisme self-heal v7.5.0 sudah cukup menangani
  skenario overlap ini, apapun subsistem yang bikin stale-nya).

`preflight_check.sh` 13/13 lolos. Confidence: integritas paket 100%; ini
murni penambahan UI info non-blocking (tidak menyentuh alur data/logika
scan), risiko regresi minimal.

## v7.5.0 -- Auto-buat folder root "PromptVault" di tujuan kustom SAF DIKEMBALIKAN + lapis anti-duplikat baru (2026-08-17)
User minta balik: fitur auto-buat root "PromptVault" (dihapus v7.2.0 karena
duplikat "(N)" berulang) dikembalikan, DENGAN syarat duplikat tidak boleh
terulang. Bukan sekadar revert v7.2.0 -- ditambah lapis proteksi yang BELUM
pernah dicoba di v7.1.5/v7.1.6 dulu.

**Perubahan (4 file, Atomic Change -- logika + SEMUA salinan dokumentasi
UI yang menyebut "root tidak auto-dibuat" saling terkait, harus konsisten
sekaligus)**:
- `util/FileSorter.kt`:
  - BARU `resolveCanonicalRootDirSaf()`: dipanggil sekali di awal
    `resolveSafRuleDestinations()`. Cache-by-Uri dulu (cepat) -> kalau
    kosong, LIST & cocokkan children ke regex `^PromptVault(\s\(\d+\))?$`
    -> 0 hasil = buat baru (lewat `findOrCreateChildDirSaf` yang sudah ada),
    1 hasil = pakai itu, **>1 hasil = pilih kanonik (prioritas nama persis
    "PromptVault", fallback `lastModified()` paling awal), log WARNING
    eksplisit ke Activity Log, folder lain TIDAK disentuh/dihapus**. Ini
    lapis SELF-HEALING baru: kalaupun provider SAF sempat "nakal" bikin
    folder ganda (di luar kendali app), scan berikutnya tetap konvergen ke
    SATU folder, tidak makin pecah.
  - `findOrCreateChildDirSaf()`: retry diperkuat dari 1x200ms jadi
    200ms+500ms bertahap -- window race paling rawan adalah saat root
    BELUM PERNAH ada (cache kosong), jadi retry lebih sabar di titik itu.
  - `resolveSafRuleDestinations()`: `vaultRootDoc` sekarang hasil
    `resolveCanonicalRootDirSaf()`, bukan `destinationRoot` mentah lagi.
- `ui/screens/SettingsScreen.kt`: `WarningBanner` "TIDAK PERNAH membuat
  folder root" di kartu Folder Tujuan Kustom (SAF) DIHAPUS, diganti 1
  kalimat info di deskripsi kartu: subfolder "PromptVault" dibuat otomatis.
  Kartu Shizuku TIDAK diubah -- mode Shizuku TETAP tidak auto-buat root
  (di luar scope permintaan user kali ini).
- `ui/screens/PanduanScreen.kt` & `ui/screens/OnboardingScreen.kt`: teks
  yang dulu bilang "KEDUA opsi lanjutan (SAF & Shizuku) tidak auto-buat
  root" dipecah -- SAF sekarang disebut auto-buat, warning "root harus
  sudah ada" dipersempit khusus untuk Shizuku saja.
- `app/build.gradle.kts`: versionCode 89->90, versionName 7.4.0->7.5.0.

**Batas jujur**: mekanisme deteksi+konvergensi (poin 5 di atas) BARU, belum
pernah diuji skenario nyata provider SAF OEM yang benar-benar bikin
duplikat lagi -- desainnya defensif (tidak bisa memperburuk keadaan: kalau
providernya nakal sekali, app langsung konsisten pakai 1 folder & warning
ke user, bukan diam-diam menyebar file ke banyak folder seperti insiden
lama). `preflight_check.sh` 13/13 lolos. Belum lewat `./gradlew`/device asli.
**User WAJIB verifikasi**: (1) build CI hijau, (2) pilih folder tujuan
kustom BARU (kosong) -> scan -> subfolder "PromptVault" muncul otomatis
tanpa perlu dibuat manual, (3) scan berkali-kali (manual & tunggu
auto-sort) ke folder yang SAMA -> pastikan HANYA SATU folder "PromptVault"
yang bertambah isi, tidak ada "(1)"/"(2)" baru.
Confidence Rating (integritas paket/struktur ZIP): **100%** -- semua
protected assets utuh, preflight 13/13. Confidence Rating (perilaku
fungsional fix anti-duplikat di device OEM nyata, di luar kendali sandbox
ini): **~75%** -- desain lebih kuat dari 2 percobaan sebelumnya, tapi
sifat provider SAF pihak ketiga tetap tidak bisa dipastikan 100% tanpa
device asli.

## v7.4.0 -- Panduan User Baru: onboarding dirombak total + layar Panduan persisten baru (2026-08-17)
User menyoroti: setelah rentetan perombakan besar (Shizuku, sweep-select-undo,
warning banner root-folder, dst di v7.3.0), user BARU nyaris tidak punya cara
mempelajari mekanisme app secara utuh -- gap ini harus ditutup tuntas.

**Root cause**: satu-satunya penjelasan mekanisme app adalah `OnboardingScreen`
yang HANYA tampil SEKALI SEUMUR HIDUP (gated `onboardingDone` di DataStore),
isinya pun basi (4 langkah generik, sama sekali tidak menyebut SAF vs
Shizuku, strategi konflik nama file, atau Undo) -- sudah tertinggal jauh dari
fitur aktual app sejak v7.2.0/v7.3.0. Sekali user menekan "Mulai" atau
lupa detailnya, tidak ada jalan balik di dalam app.

**Perubahan (6 file diubah + 1 file baru, Atomic Change -- onboarding,
entry point baru di 2 layar, dan wiring navigasi di MainActivity saling
tergantung, tidak bisa dipecah batch tanpa membuat build gagal di tengah)**:
- REWRITE `ui/screens/OnboardingScreen.kt`: 4 -> 7 langkah, urut sesuai alur
  pemakaian nyata (selamat datang -> rule -> izin -> ke mana file disortir
  + warning root-folder tidak auto-dibuat -> strategi konflik nama ->
  auto-sort & notifikasi -> undo + pointer ke Panduan). Struktur wizard
  step-by-step & animasi Crossfade lama DIPERTAHANKAN, cuma konten yang
  diperluas.
- BARU `ui/screens/PanduanScreen.kt`: versi REFERENSI (satu halaman scroll,
  bukan wizard) dari materi onboarding yang sama, plus 2 poin troubleshooting
  cepat (file tidak masuk folder yang diharapkan, dst). Bisa dibuka BERKALI-KALI
  kapan saja tanpa reset status onboarding -- inilah penutup gap utamanya.
  WarningBanner root-folder di-reuse persis sama dengan yang di SettingsScreen
  (satu sumber kebenaran, bukan teks duplikat yang bisa saling kontradiksi).
- `ui/Navigation.kt`: route baru `Routes.PANDUAN`.
- `ui/screens/HomeScreen.kt`: param baru `onOpenPanduan`, 1 `GroupedListRow`
  baru "Panduan Penggunaan" di grouped menu (antara "Kelola Rule" dan
  "Riwayat Aktivitas") -- tint SENGAJA reuse `colors.tertiary` (Amber),
  BUKAN aksen ke-5 baru (sistem warna app dibatasi 4 aksen, lihat Color.kt).
- `ui/screens/SettingsScreen.kt`: param baru `onOpenPanduan`, tombol "Buka
  Panduan Penggunaan" di paling atas Column (sebelum kartu Interval
  Auto-Scan) -- entry point kedua, krn Pengaturan adalah layar paling sering
  dibuka user saat setup awal (SAF/Shizuku/konflik).
- `MainActivity.kt` (protected asset, edit parsial): import `PanduanScreen`,
  `composable(Routes.PANDUAN)` baru, `onOpenPanduan` diteruskan ke pemanggilan
  `HomeScreen(...)` dan `SettingsScreen(...)` yang sudah ada. Tidak ada
  logika permission/lifecycle yang disentuh.
- `app/build.gradle.kts`: versi saja.

**Yang SENGAJA tidak diubah**: tidak menyentuh P0/P1/P2 dari audit fungsional
(`PromptVault_real_functional_polish_gap_audit.md`, 2026-08-16) -- itu bug
fungsional di FileSorter/undo/worker, scope batch ini murni gap informasi ke
user, tidak dicampur supaya masing-masing tetap Atomic Change yang jelas.

`preflight_check.sh` 13/13 lolos (kategori 1 keseimbangan kurung, 3 delegate
import, 4 import duplikat, 6 warna literal, semua bersih di file baru/diubah).
Confidence Rating: **90%** (perubahan UI-only + wiring navigasi murni,
tidak menyentuh FileSorter/DataStore/worker sama sekali -- risiko regresi
fungsional nyaris nol; belum lewat `./gradlew`/device asli seperti biasa,
lihat batasan jujur standar di tiap entri PROJECT_STATE.md).
versionCode 88->89, versionName 7.3.0->7.4.0.

## v7.3.0 -- 3 permintaan eksplisit user: integrasi Shizuku, sweep-select-to-undo, warning eksplisit root tidak auto-dibuat (2026-08-17)
User minta 3 hal sekaligus di 1 sesi: (1) integrasi Shizuku "100%" bukan
tempelan, (2) fitur pilih-banyak-sapu-jari buat undo massal biar "gak
ribet", (3) peringatan sejelas-jelasnya bahwa folder root tujuan kustom
TIDAK otomatis dibuat aplikasi.

**1. Integrasi Shizuku (fitur baru, menyentuh 9 file + 4 file baru, Atomic
Change -- 1 fitur kohesif, tidak bisa dipecah tanpa membuat build gagal
di tengah)**:
- BARU: `shizuku/IFileOpsService.aidl` (kontrak IPC: exists/isDirectory/
  mkdirs/moveFile/deleteFile/fileLength/ping/destroy, semua path filesystem
  absolut polos bukan content:// URI), `shizuku/FileOpsUserService.kt`
  (implementasi Stub yang JALAN DI PROSES SHIZUKU, UID shell/adb atau root
  tergantung backend aktif user -- `moveFile` pakai pola temp-file-lalu-rename
  yang SAMA dengan `copyThenDelete` fix P0-2 2026-08-16, supaya kelas bug
  "file parsial nyangkut" yang sudah pernah diperbaiki di jalur lokal TIDAK
  terulang di jalur privileged baru ini), `shizuku/ShizukuManager.kt`
  (singleton lifecycle binder/permission/service, StateFlow status:
  NOT_INSTALLED/NOT_RUNNING/PERMISSION_DENIED/BINDING/READY/ERROR).
- `app/build.gradle.kts`: `dev.rikka.shizuku:api:13.1.5` +
  `dev.rikka.shizuku:provider:13.1.5`, `buildFeatures.aidl = true`.
- `AndroidManifest.xml`: deklarasi manual `<provider android:name="rikka.shizuku.ShizukuProvider">`
  (authorities pakai applicationId, WAJIB dideklarasikan app-side, tidak
  otomatis dari manifest library).
- `PromptVaultApp.kt`: `ShizukuManager.init(this)` sekali di `onCreate()`
  (daftar listener binder, TIDAK minta izin otomatis -- izin diminta
  eksplisit lewat tombol user di Pengaturan, konsisten prinsip "minta izin
  saat relevan" yang sudah dipakai utk POST_NOTIFICATIONS).
- `SettingsRepository.kt`: `shizukuDestPathKey` (path absolut) +
  `useShizukuKey` (toggle) -- SALING EKSKLUSIF dengan `safTreeUriKey` by
  design, bukan campur/prioritas implisit.
- `FileSorter.kt`: cabang BARU `scanAndSortViaShizuku()` dicek PALING AWAL
  di `scanAndSort()` (sebelum cabang SAF) kalau `useShizuku=true`. Konsisten
  dengan ARSITEKTUR yang sudah jadi pelajaran permanen: sumber scan TETAP
  SELALU Downloads, Shizuku HANYA tujuan. Subfolder RULE di-resolve SEKALI
  SERIAL sebelum pemrosesan paralel (`resolveShizukuRuleDestinations` --
  PROAKTIF menghindari kelas bug race "folder duplikat" yang sudah pernah
  terjadi di SAF 2026-08-13, BUKAN ditemukan lewat insiden baru). Root
  folder TIDAK PERNAH dibuat -- HANYA divalidasi ADA lewat IPC, scan
  DIHENTIKAN + pesan error eksplisit kalau tidak ada (persis pola
  AccessLost SAF: tidak pernah silent-fallback ke Downloads). `undo()`
  dapat cabang baru dicek PALING AWAL: `destUri` berprefix palsu
  `"shizuku://"` (bukan skema URI asli, penanda saja, pola identik prefix
  `content://` utk SAF -- TIDAK butuh kolom/skema Room baru).
- `MainViewModel.kt`: expose `shizukuStatus`/`shizukuDestPath`/`useShizuku`
  StateFlow + `requestShizukuPermission()`/`refreshShizukuStatus()`/
  `setShizukuDestPath()`/`setUseShizuku()`.
- `SettingsScreen.kt`: kartu baru "Mode Shizuku (Lanjutan)" -- toggle,
  status berwarna, tombol "Minta Izin Shizuku"/"Cek Ulang Status", input
  path absolut (hanya tampil kalau toggle ON), + `WarningBanner` (lihat
  poin 3 di bawah).
- **Batas jujur (WAJIB dibaca sebelum klaim "Shizuku jalan")**: kode ini
  BELUM PERNAH lewat `./gradlew`/device asli/aplikasi Shizuku sungguhan --
  sandbox sesi ini tidak punya Android SDK/Gradle/Shizuku terpasang,
  konsisten dgn seluruh riwayat kode SAF project ini (lihat Insiden #7,
  PROJECT_STATE.md). Ditulis seketat mungkin dari permukaan API publik
  `dev.rikka.shizuku:api` yang terdokumentasi, TAPI kalau CI/build gagal di
  file `shizuku/`, itu BUKAN tanda ditulis ceroboh -- itu justru risiko yang
  sudah didokumentasikan eksplisit di sini sejak awal.

**2. Sweep-select-to-undo (fitur baru, `ActivityLogScreen.kt` -- rewrite
penuh file, `MainViewModel.kt`, `MainActivity.kt`)**:
- Tab "Undo Pemindahan" sekarang punya mode seleksi-banyak: tekan-lama 1
  baris -> masuk mode seleksi, LALU sapukan jari ke baris lain (drag,
  `detectDragGestures` di atas `Box` pembungkus LazyColumn, posisi tiap
  baris direkam via `onGloballyPositioned`+`boundsInWindow()`) utk
  toggle-pilih banyak baris SEKALIGUS tanpa tap satu-satu -- pola familiar
  ala Gmail/Files/Galeri. Checkbox per baris + tap biasa tetap berfungsi
  sbg alternatif non-sapuan. Top bar berubah jadi "N dipilih" + tombol
  "Undo Terpilih (N)" + tombol Batal.
- `MainViewModel.undoMultiple()` baru -- undo BANYAK entri sekuensial (bukan
  paralel, volume biasanya kecil dari seleksi manual), return
  (sukses, gagal), dipakai lewat konfirmasi `VaultActionSheet` yang sama
  persis dgn undo tunggal (1 langkah konfirmasi terakhir sebelum eksekusi
  batch, bukan langsung jalan).
- Hint 1 baris ditampilkan di atas list Undo (saat TIDAK dalam mode
  seleksi) supaya fitur ini tidak perlu ditemukan sendiri oleh user.
- **Batas jujur**: gestur sapuan lintas-elemen custom seperti ini juga
  BELUM PERNAH diuji device asli (sandbox tanpa Compose preview/emulator).

**3. Warning eksplisit "root tidak auto-dibuat" (`WarningBanner.kt` baru,
`SettingsScreen.kt`)**:
- Komponen `WarningBanner` baru (ikon + warna `colors.error`, bukan sekadar
  info) dipakai di KEDUA kartu tujuan kustom -- SAF (sudah ada sejak
  v7.2.0, tapi SEBELUMNYA cuma tercatat di dokumentasi teknis
  PROJECT_STATE.md/CHANGELOG.md, TIDAK ditampilkan di UI) dan Shizuku
  (baru). Pesan eksplisit: aplikasi TIDAK PERNAH membuat folder root
  tujuan kustom secara otomatis -- user WAJIB membuatnya sendiri lewat file
  manager LEBIH DULU. Bukan cuma teks pasif -- `FileSorter` (kedua jalur)
  MENOLAK scan dengan pesan error eksplisit kalau root belum ada, bukan
  membuatkannya diam-diam (perilaku ini SUDAH ada sejak v7.2.0 utk SAF,
  DITERAPKAN SAMA PERSIS ke jalur Shizuku baru).

File diubah (9) + 4 baru: `app/build.gradle.kts`, `AndroidManifest.xml`,
`PromptVaultApp.kt`, `data/SettingsRepository.kt`, `util/FileSorter.kt`,
`ui/MainViewModel.kt`, `MainActivity.kt`, `ui/screens/SettingsScreen.kt`,
`ui/screens/ActivityLogScreen.kt` (rewrite penuh); BARU
`shizuku/IFileOpsService.aidl`, `shizuku/FileOpsUserService.kt`,
`shizuku/ShizukuManager.kt`, `ui/components/WarningBanner.kt`.
`scripts/preflight_check.sh` 13/13 lolos.

Confidence Rating: **70%** -- SENGAJA lebih rendah dari batch-batch biasa.
Perubahan dokumentasi/UI-teks (poin 3) & pola undo batch sekuensial (poin
2, arsitekturnya straightforward) confidence tinggi; TAPI integrasi Shizuku
(poin 1) & gestur sapuan custom (poin 2) adalah 2 permukaan API yang BELUM
PERNAH dipakai di project ini SAMA SEKALI sebelumnya (beda dari SAF yang
setidaknya sudah py 7+ iterasi pengalaman) -- keduanya BELUM lewat
`./gradlew`/device asli. **User WAJIB verifikasi**: (1) build CI hijau
(prioritas #1 -- dependency Shizuku & AIDL codegen adalah risiko compile
paling nyata di batch ini), (2) kartu Mode Shizuku muncul & status
berubah sesuai kondisi Shizuku di HP (belum pasang/sudah pasang tapi belum
izin/siap), (3) sapuan jari di tab Undo benar-benar memilih banyak baris,
tidak "kalah" oleh scroll LazyColumn, (4) warning banner tampil jelas di
kedua kartu tujuan kustom.
versionCode 87->88, versionName 7.2.0->7.3.0.

## v7.2.0 -- PERUBAHAN ARSITEKTUR: app BERHENTI bikin folder root "PromptVault" sendiri di folder tujuan kustom (2026-08-17)
Setelah 2 ronde mitigasi (v7.1.5 cache-Uri, v7.1.6 retry+instrumentasi) TIDAK
berhasil membuktikan/menyingkirkan tuntas laporan duplikat "PromptVault (N)"
di folder tujuan kustom SAF -- **PERMINTAAN LANGSUNG USER**: hilangkan
pemicunya sepenuhnya, bukan tambal lagi. Terverifikasi lewat grep: SELURUH
codebase cuma punya 1 titik panggilan `createDirectory()`, yaitu di dalam
`findOrCreateChildDirSaf` yang dipanggil `resolveSafRuleDestinations` utk
folder root "PromptVault". Log Aktivitas user (16/08 11:44 - 17/08 07:16,
APK 7.1.6 terverifikasi terpasang) menunjukkan JALUR SUBFOLDER RULE (Apps
vault, Markdown vault, dst -- juga lewat `findOrCreateChildDirSaf`, fungsi
YANG SAMA) 0 masalah -- artinya fungsinya sendiri sudah cukup solid, TAPI
spesifik pemanggilan folder ROOT tetap jadi sumber ketidakpastian yang tidak
kunjung terbukti tuntas tanpa akses device langsung.

**Perubahan**: `resolveSafRuleDestinations` TIDAK LAGI memanggil
`findOrCreateChildDirSaf(destinationRoot, "PromptVault", ...)`.
`destinationRoot` (folder yang user pilih SENDIRI lewat SAF picker -- user
BIKIN & PILIH folder itu manual, mis. beri nama "PromptVault" sendiri kalau
mau) SEKARANG LANGSUNG dipakai sbg vault root. App HANYA membuat subfolder
RULE (Apps vault, dst) langsung di dalamnya -- jalur yang SUDAH TERBUKTI
bersih di log. Efek samping: konsumsi 1x lookup SAF lebih sedikit per scan
(1 folder lebih dikit yang perlu di-resolve), scan custom-destination
SEDIKIT lebih cepat.

**Konsekuensi behavioral (PENTING, WAJIB dibaca user)**:
- User yang sebelumnya sudah pakai folder tujuan kustom dgn subfolder
  "PromptVault" DI DALAMNYA: scan BARU nulis LANGSUNG ke root (rule folder
  langsung di bawah folder yang dipilih), TANPA subfolder "PromptVault" lagi.
  File LAMA di subfolder "PromptVault" lama TETAP DI SANA, TIDAK dipindah
  otomatis (di luar scope Strict Delete & Repack Guard -- ini file hasil
  sortir user, bukan file proyek). Kalau mau lanjutin struktur lama: arahkan
  SAF picker LANGSUNG ke folder "PromptVault" lama itu sendiri (bukan
  parent-nya) sbg Folder Tujuan Kustom yang baru.
- Jalur LOKAL (tanpa folder tujuan kustom, default Downloads) TIDAK berubah
  -- masih `Downloads/PromptVault/<rule>/` seperti biasa (jalur ini
  struktural TIDAK BISA kena bug "(N)" -- `File.mkdirs()` biasa, bukan SAF).
- `ActivityLogScreen` (tab Undo): label "Ke: ..." SEBELUMNYA hardcode
  "PromptVault/<rule>/" utk SEMUA entri -- sekarang dibedakan per `destUri`
  (`content://` = SAF -> "folder tujuan kustom/<rule>/", path absolut =
  lokal -> tetap "PromptVault/<rule>/"). Entri UNDO LAMA (sebelum update ini)
  tetap akurat krn dibaca dari `destUri` yang TERSIMPAN, bukan di-generate ulang.

**Cache Uri (v7.1.5) & retry+instrumentasi (v7.1.6) TETAP DIPERTAHANKAN**
utk subfolder rule -- keduanya sudah TERBUKTI bekerja baik di log, tidak ada
alasan dicabut. `cacheKey` subfolder disederhanakan dari `"PromptVault/<rule>"`
jadi `"<rule>"` langsung (entri cache lama dgn key format "PromptVault/..."
jadi orphan, harmless -- tidak pernah di-lookup lagi, tidak dibersihkan
otomatis, tidak berdampak fungsional).

File diubah (2): `util/FileSorter.kt`, `ui/screens/ActivityLogScreen.kt`,
`app/build.gradle.kts` (versi). `preflight_check.sh` 13/13 lolos.
Confidence Rating: **80%** -- perubahan strukturalnya SENDIRI straightforward
& low-risk (menghapus kode, bukan menambah logika baru rawan bug), TAPI tetap
BELUM lewat `./gradlew`/device asli spt biasa (batasan lingkungan kerja
Claude, sudah konsisten dicatat tiap sesi). **User WAJIB verifikasi**: (1)
scan ke folder tujuan kustom BARU (rule folder langsung di root, tanpa
subfolder "PromptVault") berjalan normal, (2) tab Undo tampilkan label
tujuan yang benar, (3) build CI hijau.
versionCode 86->87, versionName 7.1.6->7.2.0 (MINOR bump krn breaking
behavioral change, bukan cuma bugfix).

## v7.1.6 -- Duplikat "PromptVault (N)" MASIH terjadi setelah v7.1.5 -- retry+INSTRUMENTASI, JUJUR: root cause belum 100% terkonfirmasi (2026-08-16)
User konfirmasi: sudah update ke v7.1.5 (cache Uri), sudah rapikan folder lama,
test ulang -- **duplikat baru MUNCUL LAGI**. Artinya cache-by-Uri v7.1.5 SAJA
TIDAK CUKUP. Ditulis jujur di sini: tanpa Logcat/device asli, root cause
pastinya BELUM bisa dipastikan 100% -- dugaan terkuat (listing SAF stale) bisa
jadi juga bikin query `exists()` LANGSUNG on cache-hit false-negative, bukan
cuma `findFile()` yang dulu diasumsikan satu²nya titik lemah.

**Perubahan sesi ini (mitigasi + instrumentasi, BUKAN klaim "sudah pasti fix")**:
1. `findOrCreateChildDirSaf`: retry 1x + `delay(200)` sebelum menyerah &
   membuat folder baru kalau `findFile()` pertama null (jaga² staleness sesaat).
2. **Verifikasi nama pasca-`createDirectory()`**: kalau nama hasil TIDAK
   PERSIS sama dgn yang diminta (mis. provider balikin "PromptVault (1)"),
   dicatat `LogLevel.ERROR` ke Activity Log dgn detail lengkap (cacheKey, nama
   diminta, nama aktual) -- ini BUKTI KONKRET kalau kejadian lagi, bukan
   asumsi. Folder baru (nama match) dicatat `LogLevel.INFO` juga, supaya
   pola waktu kejadian kelihatan di log.
3. **User WAJIB reproduce sambil buka layar Log Aktivitas** (atau screenshot
   Log Aktivitas setelah kejadian) -- baris ERROR barunya PERSIS nunjukkin
   penyebabnya provider auto-suffix beneran atau bukan. Tanpa ini, sesi
   berikutnya masih akan nebak buta lagi.
4. **Pertanyaan terbuka yang BELUM terjawab**: apakah user pakai "Folder
   Tujuan Kustom" (SAF, di Pengaturan) atau default Downloads? Kalau default
   (java.io.File biasa) -- `File.mkdirs()` TIDAK PERNAH auto-suffix "(N)"
   secara struktural, artinya seluruh dugaan SAF di v7.1.4/v7.1.5/v7.1.6 SALAH
   ALAMAT dan sumber duplikat ada di TEMPAT LAIN yang belum diperiksa (custom
   destination path lokal, atau bukan dari app sama sekali). WAJIB dikonfirmasi
   sebelum sesi berikutnya lanjut fix lebih jauh -- jangan tambal lagi di jalur
   SAF kalau ternyata jalur yang dipakai bukan SAF.
File diubah (2): `util/FileSorter.kt`, `app/build.gradle.kts` (versi).
`preflight_check.sh` 13/13 lolos.
Confidence Rating: **50%** -- SENGAJA rendah, ini iterasi mitigasi+instrumentasi
kedua utk bug yang sama, BUKAN fix definitif. Jangan overclaim ke user.
versionCode 85->86, versionName 7.1.5->7.1.6.

## v7.1.5 -- FIX duplikat folder "PromptVault"/"(N)" BERULANG (beda root cause dari fix v-sebelumnya) (2026-08-16)
User lapor screenshot BARU: 7 folder "PromptVault"/"PromptVault (1)".."(6)" di
folder tujuan kustom, masing-masing ISI LENGKAP (9-10 item = semua subfolder
rule) -- BEDA POLA dari bug lama (v2.4.1-era, 1 item per duplikat, sudah
"selesai" difix 2026-08-13 dgn serialisasi `resolveSafRuleDestinations`
SEBELUM parallel processing). Fix lama itu TETAP BENAR & TETAP DIPERTAHANKAN
(menutup race ANTAR-coroutine dalam 1 scan) -- tapi tidak menutup celah lain.

**Root cause BARU**: `DocumentFile.findFile(name)` di `findOrCreateChildDirSaf`
query listing (`listFiles()`) provider SAF ULANG setiap scan. `scanMutex`
sudah pastikan scan SERIAL (tidak overlap) -- tapi pada sebagian provider/OEM,
listing children bisa STALE sesaat setelah `createDirectory()` scan
sebelumnya (lag FUSE/index). Scan berikutnya (mis. AutoSortWorker periodik,
menit² kemudian) query listing, tidak lihat folder yang SUDAH ADA secara
fisik -> `createDirectory()` dipanggil lagi -> provider deteksi tabrakan nama
di level FILESYSTEM (bukan di listing yang stale) -> auto-suffix "(1)", dst.
Pola "tiap folder isinya SET LENGKAP" cocok: tiap duplikat = 1 scan yang gagal
kenali root lama, bukan 1 file terpecah spt bug lama.

**Fix**: `SettingsRepository` (parsial, 2 fungsi+1 key baru: `getCachedFolderUri`/
`setCachedFolderUri`/`safFolderCacheKey`) -- cache Uri folder hasil resolve,
key = path relatif thd `safTreeUri` saat ini (auto-invalidate di
`setSafTreeUri`/`clearSafTreeUri` kalau root berubah). `FileSorter.
findOrCreateChildDirSaf` (jadi `suspend`, +param `cacheKey`) -- coba resolusi
LANGSUNG by-Uri (`DocumentFile.fromSingleUri` + `exists()`) dari cache DULU,
baru fallback ke `findFile()`/`createDirectory()` seperti semula kalau cache
kosong/basi. Resolusi by-Uri jauh lebih tahan stale drpd query listing
by-nama krn menyasar 1 dokumen spesifik, bukan cursor children penuh.
File diubah (2): `util/FileSorter.kt`, `data/SettingsRepository.kt`,
`app/build.gradle.kts` (versi). Tidak ada file baru -> `FILE_MANIFEST.txt`
tidak berubah. `scripts/preflight_check.sh` 13/13 lolos bersih.

**PENTING -- folder duplikat YANG SUDAH TERLANJUR ADA di device user TIDAK
otomatis digabung/dihapus oleh fix ini** (fix ini cuma cegah duplikat BARU
ke depan). User perlu gabung manual: pindahkan isi tiap "PromptVault (N)" ke
"PromptVault" asli via file manager, lalu hapus folder "(N)" yang sudah
kosong -- app sengaja tidak melakukan ini otomatis (lihat Strict Delete &
Repack Guard, ini file HASIL SORTIR user, bukan file proyek).

Confidence Rating: **85%** (root cause & fix logikanya sesuai bukti kuat di
kode+screenshot, tapi turun dari standar 90%+ krn: (1) BELUM PERNAH lewat
`./gradlew`/device asli spt biasa, (2) staleness listing SAF adalah perilaku
provider/OEM yang TIDAK BISA disimulasikan/diverifikasi tanpa device asli --
cache-by-Uri adalah mitigasi terbaik yang bisa ditulis dari kode, TAPI kalau
provider tertentu bahkan menolak `fromSingleUri` yang valid dlm kasus langka,
fallback ke jalur lama tetap jalan (tidak regresi), namun skenario itu sendiri
belum bisa dites end-to-end). **User WAJIB verifikasi**: (1) scan manual +
auto-sort periodik berulang kali (bbrp jam) TIDAK lagi bikin folder
"PromptVault (N)" baru, (2) rule folder normal tetap ke-scan & file tetap
sampai ke folder yang benar (nol regresi jalur normal), (3) build CI hijau.
versionCode 84->85, versionName 7.1.4->7.1.5.

## v7.1.4 -- FIX 3 GAP P0 dari audit eksternal (folder-name traversal, copy parsial, urutan undo SAF) -- Phase 1/4 (2026-08-16)
User upload `PromptVault_real_functional_polish_gap_audit.md` (audit statis
eksternal: 3 P0, 9 P1, 7 P2). Instruksi eksplisit: kerjakan BERTAHAP, jangan
sekaligus (`"kerjakan secara bertahap ... jangan greedy"`). Batch ini =
**Phase 1 penuh dari Priority Fix Order audit (item 1-3)** -- SEMUA 3 temuan
P0, BUKAN sebagian dari P1/P2 (yang sengaja ditunda ke batch berikutnya,
lihat daftar lengkap di bagian bawah audit md tsb, sisa Phase 2-4).

**P0-1 (folder rule tidak divalidasi, potensi path traversal)**: `rule.
folderName` sebelumnya cuma dicek `isNotBlank()` di UI lalu dipakai LANGSUNG
di `File(destDir, rule.folderName)` -- nama berisi `/`, `\`, atau `..` bisa
membuat destinasi KELUAR dari folder `PromptVault` yang dimaksud. Fix: file
baru `util/RuleFolderNameValidator.kt` (fungsi top-level murni, pola sama
`mimeTypeForFileName`/`GlobMatcher`, unit-testable tanpa Context) --
`validateRuleFolderName()` (tolak blank/`.`/`..`/karakter provider-unsafe
`/\:*?"<>|`+kontrol) dipasang di DUA lapis wajib: (1) `AddEditRuleScreen.kt`
inline (`isError`+`supportingText` di field, Save disabled kalau invalid --
sekalian menutup P2-2 "folder-name validation UX" dari audit yang sama), (2)
`FileSorter.moveFile()` & `resolveSafRuleDestinations()` sebagai GERBANG
TERAKHIR (WAJIB tetap ada supaya rule LAMA yang tersimpan sebelum fix ini
tetap aman dipakai scan, bukan cuma dicegah untuk rule baru). Ditambah
`isContainedIn()` -- pertahanan lapis-kedua canonical-path SETELAH `File`
tujuan dibangun, defense-in-depth sesuai required-fix audit.

**P0-2 (`copyThenDelete()` bisa nyisain file tujuan parsial)**: sebelumnya
`src.copyTo(dest, overwrite=false)` menulis LANGSUNG ke nama final -- copy
gagal di tengah jalan (disk penuh/I/O error) bisa nyisain file korup di nama
final, catch block cuma `return false` tanpa membersihkan. Fix: tulis ke
file sementara (`<nama>.tmp_<uuid>`, folder tujuan sama) dulu, verifikasi
selesai, BARU rename ke nama final; gagal di titik manapun -> file sementara
dihapus, nama final TIDAK PERNAH tersentuh sampai transfer tuntas. `src`
cuma dihapus SETELAH tujuan final terkonfirmasi lengkap; kegagalan hapus
sumber tetap tidak menggagalkan fungsi (perilaku lama dipertahankan,
konsisten filosofi `moveFileToSafDestination`) tapi sekarang DILOG sbg
WARNING (sebelumnya didiamkan total).

**P0-3 (urutan `markUndone()` salah di `undoSaf()`/`undoSafDestination()`)**:
sebelumnya `moveHistoryRepository.markUndone(entry.id)` dipanggil TANPA
SYARAT begitu salinan balik sukses, TIDAK PEDULI `current.delete()` (hapus
salinan lama di tujuan) berhasil atau tidak -- kalau provider SAF menolak
delete, riwayat SUDAH TERLANJUR ditandai "selesai di-undo" padahal 2 salinan
(lama + hasil restore) masih ada sekaligus, dan UI tidak lagi menawarkan
cara menindaklanjuti. Fix: `markUndone()` HANYA dipanggil kalau `delete()`
BENAR-BENAR sukses; kalau gagal, entri riwayat SENGAJA dibiarkan "belum
selesai" (state-machine, bukan silent-mark-done) + WARNING eksplisit "Undo
SEBAGIAN" di Activity Log supaya user tahu ada duplikat & entri tetap bisa
dicoba lagi. Pola sama diterapkan di kedua fungsi (legacy `undoSaf` & format
baru `undoSafDestination`).

**Sengaja TIDAK dikerjakan di batch ini** (di luar scope Phase 1, kandidat
Phase 2-4 sesuai Priority Fix Order di audit md): P1-1 (unit test FileSorter
lengkap -- baru ditambah 1 test file utk validator baru, BUKAN matrix
lengkap yang diminta P1-1), P1-2 (klasifikasi retry worker), P1-3 (kategori
skip lebih granular), P1-4 (promosi kegagalan resolusi folder SAF jadi scan
error eksplisit), P1-5/P1-6 (RuleRepository transactional + semantik
import), P1-8/P1-9 (OVERWRITE destruktif tanpa histori), semua P2. Ini
keputusan SADAR mengikuti instruksi user "bertahap, jangan greedy" -- BUKAN
lupa/terlewat, lihat "Priority Fix Order" di audit md untuk urutan Phase
2-4 lengkap kalau mau lanjut.

File diubah (4) + 2 baru: `util/FileSorter.kt`, `ui/screens/
AddEditRuleScreen.kt`, `app/build.gradle.kts` (versi), `FILE_MANIFEST.txt`;
BARU `util/RuleFolderNameValidator.kt` + `app/src/test/.../
RuleFolderNameValidatorTest.kt`. `scripts/preflight_check.sh` 13/13 lolos
bersih. **BELUM PERNAH lewat `./gradlew` asli/device asli** (konsisten
seluruh riwayat project). User WAJIB verifikasi: (1) coba isi nama folder
rule dengan `/` atau `..` di Tambah/Edit Rule -- field harus tampil error
merah & tombol Simpan nonaktif, (2) rule normal (tanpa karakter aneh) tetap
bisa disimpan & scan seperti biasa (nol regresi jalur normal), (3) build
CI hijau.

## v7.1.3 -- FIX GAP FUNGSIONAL NYATA: POST_NOTIFICATIONS tidak pernah diminta runtime (2026-08-16)
User minta audit lebih dalam (edge case DB/permission/migrasi), setelah item
pertama yang saya coba (`FileSorter.undo()` dispatcher) ternyata FALSE
POSITIVE -- sudah difix di v2.20.1, langsung direvert begitu ketahuan
(lihat commit sebelumnya utk detail koreksi).

**Audit DB (Room)**: `AppDatabase.kt` (version=1, fallbackToDestructiveMigration
terdokumentasi jelas & disengaja), `MoveHistoryDao`/`ActivityLogDao` (trim
FIFO 200/500 baris, debounce trim v2.4.1 sudah optimal), `Converters.kt`
(enum LogLevel<->String dgn fallback aman) -- **semua OK, tidak ada bug**.

**Audit permission -- GAP NYATA ditemukan**: `POST_NOTIFICATIONS` dideklarasikan
di `AndroidManifest.xml` sejak Batch §5 (utk notifikasi ongoing
`AutoSortWorker`, lihat `AutoSortNotification.kt`) TAPI **tidak pernah
diminta runtime** di kode manapun (`grep` konfirmasi 0 pemanggilan
`ActivityResultContracts.RequestPermission()` utk izin ini). App ini
`targetSdk=34` (Android 14), jauh di atas ambang API 33 tempat
`POST_NOTIFICATIONS` WAJIB diminta eksplisit -- deklarasi manifest SAJA
tidak cukup. **Dampak nyata**: notifikasi "Auto-sort sedang berjalan" --
tujuan UTAMA Batch §5 (kasih user visibility scan background) -- kemungkinan
TIDAK PERNAH tampil di HP Android 13+ manapun, padahal foreground service-nya
sendiri tetap jalan diam-diam (jadi bukan crash/gagal fungsi, tapi user
kehilangan visibility yang justru jadi alasan fitur ini dibuat).

**Fix**: `MainActivity.kt` (protected asset, parsial) -- launcher baru
`notificationPermissionLauncher`, diminta SEKALI (one-shot, flag DataStore
`notification_permission_asked`) tepat setelah user lolos gate storage
permission + onboarding (titik paling wajar "minta izin saat relevan").
Hasil grant/deny SENGAJA diabaikan -- ini fitur pelengkap (visibility),
BUKAN gate wajib spt storage, user yang menolak tidak dipaksa dialog
berulang.

**Audit migrasi**: `LegacyDataMigration.kt` sudah didesain aman-walau-tebakan-
salah (no-op murni kalau key tidak cocok, guard flag anti-retry) -- sudah
diverifikasi cukup di v2.20.2, tidak ada gap baru ditemukan sesi ini.

File diubah (2): `MainActivity.kt` (parsial), `app/build.gradle.kts` (versi).
`FILE_MANIFEST.txt` tidak berubah. `preflight_check.sh` 13/13 lolos bersih.
**BELUM PERNAH lewat `./gradlew` asli/device asli.** User WAJIB verifikasi:
(1) dialog izin notifikasi muncul SEKALI saat pertama masuk app (Android
13+ saja), (2) setelah accept, notifikasi "Auto-sort sedang berjalan"
benar-benar tampil saat auto-scan jalan di background.

## v7.1.2 -- Polish UI lanjutan: highlight GlassPanel diagonal->vertikal + fix Row Undo (2026-08-16)
User laporkan v7.1.1 belum cukup: toggle/saklar, kotak ikon menu, dan tombol
Undo masih terlihat asimetris.

**Root cause #1 (toggle + icon menu, 1 akar sama):** `GlassPanel.kt` overlay
highlight pakai `Brush.linearGradient(...)` tanpa `start`/`end` -- default
Compose menarik gradient DIAGONAL pojok kiri-atas ke kanan-bawah. Di elemen
kecil bulat (thumb switch 20dp, kotak ikon 30dp) ini kelihatan jelas sebagai
"satu pojok terang, pojok seberang gelap" walau posisi/ukuran elemen itu
sendiri sudah presisi center (diverifikasi ulang matematis). Fix: ganti ke
`Brush.verticalGradient` (atas->bawah, simetris kiri-kanan). 1 titik ubah,
otomatis berlaku ke semua pemakai primitif (thumb switch, kotak ikon menu,
pil SegmentedControl, VaultCard, dst.).

**Root cause #2 (tombol Undo, beda kelas):** `ActivityLogScreen.kt` tab
"Undo Pemindahan" -- `Row` pembungkus Column-teks (3 baris) + tombol "Undo"
tidak punya `verticalAlignment` (default Top). Column teks lebih tinggi dari
tombol -> tombol nempel rata atas, nyisa ruang kosong di bawah. Fix: tambah
`verticalAlignment = Alignment.CenterVertically`, sama seperti Row tab "Log"
di atasnya yang sudah benar.

File diubah (3): `ui/components/GlassPanel.kt`, `ui/screens/ActivityLogScreen.kt`,
`app/build.gradle.kts` (versi). `scripts/preflight_check.sh` 13/13 lolos
bersih. **BELUM PERNAH lewat `./gradlew` asli/device asli.** User WAJIB
verifikasi visual: thumb switch & kotak ikon Home terang merata dari atas
(bukan 1 pojok), tombol Undo center vertikal sejajar teks di sampingnya.
versionCode 81->82, versionName 7.1.1->7.1.2.

## v7.1.1 -- Polish UI: fix kontras border WCAG 1.4.11 + rapikan baris kontrol asimetris RuleCard (2026-08-16)
User kirim 4 screenshot build v7.1.0 nyata + minta 2 hal spesifik ("fokus
kerjakan yang sekarang", "no less no more"): (1) kembalikan layout
terdistorsi wajib WCAG, (2) khusus polish UI & rapikan layout asimetris --
BUKAN redesign ulang / ganti hue yang sudah dipatok user.

**Audit WCAG (formula relative luminance W3C, semua kombinasi token x
tingkat permukaan dihitung ulang, bukan tebakan):**
- `GlassBorder`/`HairlineGlass` (border 1dp semua `GlassPanel`, termasuk
  tepi `VaultCard` & FilterChip unselected di Pengaturan): alpha 0.14f cuma
  1.49-1.55:1 di 4 tingkat permukaan -- GAGAL ambang WCAG 1.4.11 (non-text/
  batas komponen, syarat 3:1). Naik ke 0.38f -> 3.00-3.80:1 (worst-case
  GlassSurfaceSheet 3.00:1), lulus di semua tingkat.
- `TextMuted` (alpha 0.42f): 3.45-3.81:1 di 5 tingkat permukaan -- GAGAL
  ambang teks normal 4.5:1. Token ini TIDAK ada pemanggil aktif (diverifikasi
  grep, dead code) tapi tetap diperbaiki ke 0.56f (4.81:1 worst-case) supaya
  aman dipakai kapan pun ke depan, bukan jebakan WCAG tertunda.
- `BrassAccent` (#B5A642, ikon "Kelola Rule"): dicek terpisah -- kontras
  7.44:1 (LULUS AAA), BUKAN pelanggaran WCAG. Saturasinya (47%) memang jauh
  di bawah 3 ikon lain (Amber/Slate/Rust, 77-100%) shg keliatan pudar
  berdampingan, tapi hex ini dipatok eksplisit user sesi sebelumnya
  ("dilarang keras ngide sendiri") -- DITANYAKAN ke user, dijawab fokus ke
  layout asimetris, jadi hex Brass TIDAK disentuh sesi ini.

**Fix asimetri layout (bukan soal warna):** `RuleCard.kt` baris kontrol aksi
(naik/turun prioritas, switch, edit, hapus) SEBELUMNYA `Arrangement.spacedBy(4.dp)`
+ 1 `Spacer(weight(1f))` disisipkan di TENGAH -- efeknya 2 tombol reorder
numpuk rapat di ujung kiri, 3 kontrol lain (switch/edit/hapus) numpuk rapat
di ujung kanan, nyisain 1 celah kosong lebar PERSIS di tengah (baris terlihat
asimetris/berat sebelah, bukan proporsional -- lihat screenshot user, layar
"Kelola Rule"). Diganti `Arrangement.SpaceEvenly` tanpa Spacer manual --
jarak antar SEMUA 5 kontrol sekarang merata di lebar penuh baris.

File diubah (3): `Color.kt` (2 token alpha), `RuleCard.kt` (1 Arrangement),
`app/build.gradle.kts` (versi). Tidak ada file baru/dihapus, `FILE_MANIFEST.txt`
tidak berubah. `preflight_check.sh` 13/13 kategori lolos bersih. Confidence
Rating: **96%** (2 fix WCAG murni numerik/terverifikasi formula, 1 fix layout
straightforward Arrangement swap, TIDAK ada logika baru berisiko -- turun
dari 97%+ semata krn seperti biasa BELUM PERNAH lewat `./gradlew` asli/device
asli, sandbox tanpa Android SDK). User WAJIB verifikasi visual: (1) border
kartu/chip kelihatan jelas tapi tidak mengganggu, (2) baris kontrol RuleCard
di "Kelola Rule" sekarang seimbang kiri-kanan.

## v7.1.0 -- FITUR BARU: toggle tema (Deep Navy+Brass <-> Charcoal+Copper) di Pengaturan (2026-08-15)
User upload state repo terkini (`PromptVault-main.zip`, sudah v7.0.1 -- lompat
dari v7.0.0 Glassmorphism-Navy-Brass, jadi konteks kerja sesi ini BUKAN
lanjutan draft Neumorphism `drawBehind`+`setShadowLayer` yang sempat digarap
sesi sebelumnya & TIDAK PERNAH sempat di-deliver/push -- itu draft SEPENUHNYA
DITINGGALKAN, sesi lain sudah ambil arah berbeda [balik Glassmorphism] &
sudah di-push+CI-hotfix duluan). Minta lanjut sbg "toggle saklar tema custom".

**Klarifikasi (ditanya via pilihan, dijawab user)**: toggle SIMPEL ON/OFF
antara 2 preset TETAP -- BUKAN color picker bebas, BUKAN banyak preset.

**Preset ke-2 ("Charcoal + Copper") -- BARU dirancang sesi ini**, BUKAN
rekonstruksi Platinum/Ruby v6.0.0 lama (sudah dihapus total di v7.0.0, hex
persisnya tidak tercatat presisi di mana pun utk direkonstruksi dgn aman).
Root `#12100E` (charcoal HANGAT, H=30 -- sengaja beda arah hue dari Navy
H=225 spy 2 preset kerasa beda, bukan cuma gelap/terang yg sama), aksen
`#C97B4A` (Copper). WCAG dihitung manual (formula relative luminance W3C):
teks di atas Charcoal ~19:1 (AAA), teks gelap di atas Copper 5,80:1 (AA) --
LULUS di kedua pasangan, sama rigor-nya dgn preset default.

**Implementasi (BUKAN switch UI kosong -- pelajaran wajib dari Insiden
`ThemeMode` v2.16.0 yang dihapus krn togglenya tidak pernah benar-benar
mengubah apa pun)**:
- `SettingsRepository`: `useAltThemeFlow`/`getUseAltTheme`/`setUseAltTheme`
  (DataStore boolean, pola identik `intervalMinutesFlow` dkk).
- `MainViewModel`: `useAltTheme: StateFlow<Boolean>` + `setUseAltTheme()`,
  pola identik `scanConcurrency`/`setScanConcurrency`.
- `Color.kt`: token `Charcoal*`/`Copper*` baru (5 tingkat elevasi Charcoal
  dihitung HSL->RGB manual, pola sama persis Navy). Semantik Amber/Rust/Slate
  TIDAK berubah di preset ke-2 (konsisten prinsip v7.0.0: di luar scope
  toggle "warna dominan+aksen tombol").
- `Theme.kt`: `VaultDarkColorsDefault`/`VaultDarkColorsAlt` (2 `ColorScheme`
  struktur-identik, cuma token beda) + `PromptVaultTheme(useAltTheme: Boolean)`
  BENAR-BENAR `@Composable` yg memilih skema reaktif tiap recomposition --
  BUKAN parameter mati spt `ThemeMode` lama. `resolveBackgroundColor()` baru,
  1 sumber kebenaran warna bg dipakai bareng status/nav bar + Compose.
- `SettingsScreen.kt`: section "Tema" baru (`TactileSwitch` + label preset
  aktif), param `useAltTheme`/`onUseAltThemeChanged`.
- `MainActivity.kt` (protected asset, edit PARSIAL): `setContent` collect
  `viewModel.useAltTheme`, teruskan ke `PromptVaultTheme(useAltTheme=...)`.
  `SideEffect` BARU memanggil ulang `enableEdgeToEdge` pakai
  `resolveBackgroundColor(useAltTheme)` tiap state berubah -- TANPA ini,
  status/nav bar (disetel sekali di `onCreate` SEBELUM state DataStore
  termuat) bisa "nyangkut" di preset lama walau konten Compose sudah pindah.

File diubah (6): `SettingsRepository.kt`, `MainViewModel.kt`, `Color.kt`,
`Theme.kt`, `SettingsScreen.kt`, `MainActivity.kt` (parsial). Tidak ada file
baru/dihapus -- `FILE_MANIFEST.txt` TIDAK perlu berubah. `preflight_check.sh`
13/13 kategori lolos bersih (termasuk #13, kategori dari hotfix v7.0.1, jadi
KDoc panjang batch ini otomatis tervalidasi tidak mengulang bug yang sama).
**BELUM PERNAH lewat `./gradlew` asli / device asli.** User WAJIB
verifikasi: (1) toggle di Pengaturan benar-benar mengganti seluruh tampilan
app (bukan cuma 1 layar), (2) status/nav bar ikut berubah warna, (3) tidak
ada teks tak terbaca di preset manapun.

## v7.0.1 -- HOTFIX build CI gagal total: KDoc tertutup prematur di TactileTokens.kt (2026-08-15)
User upload log CI gagal (`build-failure-log-v7_0_0.zip`, `kspDebugKotlin FAILED`,
ratusan error "Expecting a top level declaration" mulai `TactileTokens.kt:10`).

**Root cause**: KDoc header v7.0.0 di `TactileTokens.kt` menulis
`(Neu*/Glass*)` -- substring `*/` DI TENGAH kalimat menutup block comment
`/** ... */` lebih awal dari yang dimaksud. Sisa isi KDoc (baris 10 s.d.
`*/` sebenarnya di baris 20) ke-parse compiler sbg KODE KOTLIN SUNGGUHAN,
bukan lagi komentar -> parser bingung total, berantai jadi ratusan error
"Expecting a top level declaration" di seluruh sisa file. Kelas bug ini
TIDAK kelihatan dari preflight kategori #1 (hitung kurung `{}()`) krn
jumlah kurung tetap seimbang -- comment-nesting bukan brace-nesting.

**Fix**: `(Neu*/Glass*)` -> `(Neu*, Glass*)` di `TactileTokens.kt` (1 baris,
makna tidak berubah). Preflight `scripts/preflight_check.sh` kategori #12
lama (baseColor gradient, sudah obsolete sejak `baseColor` dihapus v7.0.0)
dipensiunkan jadi no-op permanen; kategori #13 BARU ditambahkan: deteksi
`*/` yang diikuti langsung karakter bukan-spasi di baris yang sama (comment
penutup ASLI selalu diikuti akhir baris/spasi) -- kelas bug ini sekarang
KEPANTAU OTOMATIS ke depan, bukan cuma ditambal titik ini.

File diubah (2): `ui/theme/TactileTokens.kt`, `scripts/preflight_check.sh`
(kategori #12 dipensiunkan + #13 baru). `app/build.gradle.kts` (versi).

## v7.0.0 -- Neumorphism DIHAPUS TOTAL, kembali ke Glassmorphism Deep Navy + Brass (2026-08-15)
User: gaya visual Neumorphism ("shadow ganda offset-Box", riwayat Insiden
#3/#8/#9/#10) dinilai **"ultra buggy"** -- minta hapus total & kembali ke
Glassmorphism secara eksplisit, dengan 2 hex dipatok: `#0B132B` (Deep Navy
Blue, 60-70% latar dominan) & `#B5A642` (Brass, 10-30% aksen tombol utama).
Instruksi tegas: **"dilarang keras untuk ngide sendiri"** -- TIDAK ADA hue
baru ditambahkan di luar 2 hex ini.

**Perubahan arsitektur (Atomic Change, 13 file -- lihat Impact Report sesi
ini utk justifikasi melebihi batas 10 file/batch):**
- `Neumorphic.kt` **DIHAPUS**, digantikan `GlassPanel.kt` (primitif baru,
  jauh lebih sederhana): `Modifier.shadow` standar 1 lapis + border hairline
  + overlay highlight diagonal tipis, TANPA teknik shadow-caster offset-Box
  ganda. Ini menghilangkan 2 SUMBER BUG STRUKTURAL sekaligus (bukan cuma
  ditambal): (1) parameter `baseColor` yang harus "menyamar" dgn latar
  (root cause Insiden #9 & #10, gagal total di atas gradient) -- SUDAH TIDAK
  ADA lagi krn shadow standar valid di atas latar apapun; (2) `modifier`
  pemanggil yang harus dipasang di `Box` pembungkus terpisah dari `Surface`
  (root cause Insiden #8, `weight()` nyasar) -- SUDAH TIDAK ADA lagi krn
  `modifier` sekarang dipasang LANGSUNG di `Surface` (satu-satunya root
  composable primitif ini, tidak ada Box tambahan).
- `Color.kt`: palet direstrukturisasi total ke Deep Navy (`AmoledBackground`,
  nama token TIDAK diubah -- lihat di bawah) + Brass (`BrassAccent`, baru).
  `RubyGlow`/`PlatinumAccent`/`PlatinumTint` (blend gradient CTA v6.0.0)
  **DIHAPUS**. Token semantik lama (`AmberGlow`/`RustGlow`/`SlateGlow`) TIDAK
  diubah hex-nya -- di luar cakupan 2 constraint user ("latar dominan" +
  "aksen tombol utama"), dipertahankan apa adanya.
- `Theme.kt`: `primary` & `secondary` SEKARANG SAMA-SAMA `BrassAccent` (CTA
  tidak lagi blend 2 aksen). `SortedStamp` (pakai `colors.secondary`)
  otomatis jadi stempel Brass -- cocok tematik ("stempel kuningan"), bukan
  penambahan warna baru.
- `TactileTokens.kt`: token `Neu*` (elevasi+offset shadow ganda,
  `NeuPressedDarkAlpha`/`NeuPressedLightAlpha`) DIHAPUS, digantikan
  `Glass*` (1 nilai elevasi per komponen, bukan pasangan).
- `VaultCard.kt`, `GroupedListRow.kt`, `TactileSwitch.kt`,
  `SegmentedControl.kt`, `EmptyState.kt`, `VaultActionSheet.kt`,
  `HomeScreen.kt`: seluruh pemanggilan `NeumorphicSurface` diganti
  `GlassPanel`. Parameter `baseColor` di `VaultCard` **DIHAPUS** (0 call
  site pernah override, dikonfirmasi grep sebelum audit hapus) -- API lebih
  sederhana, kelas bug baseColor tidak mungkin terulang krn parameternya
  sendiri sudah tidak ada.
- CTA "Scan Sekarang" (`HomeScreen.kt`): gradient blend Ruby->Platinum
  (v6.0.0) **DIHAPUS TOTAL**, sekarang 1 warna solid Brass (`colors.primary`)
  sesuai instruksi eksplisit "aksen tombol utama" tunggal, bukan blend.
- `colors.xml`: `pv_amoled_background` diisi hex Deep Navy (nama TIDAK
  diubah -- `MainActivity.kt`, protected asset, jadi TIDAK PERLU disentuh
  sama sekali, status bar/nav bar/splash otomatis ikut Deep Navy tanpa edit
  manual). `pv_platinum_accent` -> `pv_brass_accent`.

**Keputusan interpretasi (dicatat supaya transparan, lihat PROJECT_STATE.md
untuk detail penuh)**: instruksi user membatasi 2 hal SAJA -- warna latar
dominan & warna aksen tombol utama. Warna semantik non-tombol-utama (error/
warning/menu Pengaturan) sengaja TIDAK disentuh krn di luar 2 constraint
itu & sudah ada sebelum instruksi ini (bukan penambahan baru).

## v2.24.4 -- FIX AKAR Insiden #9 (v2.24.3 TERBUKTI TIDAK CUKUP di device asli): hapus wash gradient, HomeScreen kembali latar solid (2026-08-15)
User kirim 3 screenshot device asli v2.24.3 (termasuk App Info yg konfirmasi
versi terpasang) + laporan tegas: fix v2.24.3 GAGAL -- "ekor shadow" masih
nongol & sekarang malah kentara HIJAU, "Neumorphism real" yg diminta belum
tercapai.

**Analisis ulang (Insiden #10)**: v2.24.3 mencoba `baseColor` =
`colors.surfaceVariant.copy(alpha=0.55f).compositeOver(colors.background)`
(estimasi warna gradient di titik y=0) -- PENDEKATAN INI SALAH SECARA
KONSEP, bukan cuma kurang presisi: `Brush.verticalGradient` DIAM di Box
terluar (fillMaxSize, tidak scroll), sedangkan `VaultCard`/CTA ada di dalam
`Column` yang verticalScroll DI ATAS Box itu -- posisi relatif kartu vs
gradient BERUBAH terus setiap discroll, jadi TIDAK ADA satu `baseColor`
statis yang bisa akurat di semua posisi. Warna hasil composite yg dipilih
(dekat titik teratas gradient, alpha 0.55 penuh) menyerap porsi besar
`colors.surfaceVariant` (`GlassSurfaceElevated`, `0xFF0D2622` -- HIJAU tua),
jadi bukannya menyamar makin baik, badan shadow-caster malah makin kentara
beda warna dari sekitarnya yang sebagian besar sudah meluruh ke
`AmoledBackground` (near-black). **Kesimpulan**: teknik shadow neumorphism
di `Neumorphic.kt` (badan shadow-caster diisi solid `baseColor` supaya
"menyatu" dgn latar) secara DESAIN cuma valid di atas latar SOLID SERAGAM --
bukan gradient apapun, presisi berapa pun perhitungannya.

**Fix (akar, bukan tambal lagi)**: wash gradient di `HomeScreen` (fitur lama
`UI-10`, ditambahkan SEBELUM redesign Neumorphism v5.0.0 ada, tujuannya
dulu murni mengatasi keluhan "monoton") **DIHAPUS TOTAL** -- latar
`HomeScreen` sekarang `colors.background` solid, IDENTIK dgn 12 layar lain
di app ini (yang semua sudah solid AmoledBackground sejak awal & TIDAK
PERNAH kena kelas bug ini). `VaultCard`/CTA Scan kembali pakai `baseColor`
DEFAULT (tidak perlu compositeOver/parameter tambahan lagi) -- kelas bug ini
sekarang TIDAK BISA terulang di layar ini sama sekali, bukan cuma diredam.
Variasi visual "anti-monoton" yang dulu jadi alasan wash gradient itu ada
SEKARANG sudah cukup terwakili oleh gradient CTA Platinum->Ruby + shadow
ganda timbul neumorphism itu sendiri -- SESUAI permintaan eksplisit user
"Neumorphism real dengan accent Platinum+Ruby nge-blend" (bukan flat +
tempelan wash gradient yang justru bentrok teknis dgn neumorphism-nya).

`VaultCard.kt` param `baseColor` (ditambah v2.24.3) TETAP disimpan (tidak
di-revert) -- tidak salah/berbahaya, cuma jadi tidak dipakai lagi di
`HomeScreen` saat ini, tetap berguna kalau suatu saat ada layar solid non-
default butuh baseColor custom.

File diubah (3): `ui/screens/HomeScreen.kt` (hapus gradient, hapus
`homeCardBaseColor`/2 pemanggilan baseColor, hapus import `compositeOver`
yg jadi tidak terpakai), `app/build.gradle.kts` (versi).
`scripts/preflight_check.sh` TIDAK diubah lagi -- kategori #12 (v2.24.3)
TETAP relevan sbg jaring pengaman ke depan (sekarang lolos trivial krn 0
layar bergradient tersisa, bukan krn kategorinya dihapus). Lolos bersih
12/12. **BELUM PERNAH lewat `./gradlew` asli / device asli.** User WAJIB
verifikasi ulang di HP: kartu stat & tombol Scan Sekarang BENAR-BENAR bersih
dari potongan persegi mengintip di kanan/bawah, di SEMUA posisi scroll
(bukan cuma posisi awal seperti pengecekan v2.24.3 kemarin).

## v2.24.3 -- FIX bug visual nyata (screenshot user): "kartu hantu" mengintip di HomeScreen (2026-08-15)
User kirim 2 screenshot HP asli v2.24.2 + instruksi "fokus perbaiki kerusakan
asimetris & cacat ulah sesi lain" -- diaudit langsung dari gejala visual
kedua screenshot (bukan tebakan), BUKAN melanjutkan technical debt list sesi
sebelumnya (instruksi eksplisit: lupakan progres sebelumnya).

**Bug (Insiden #9)**: kartu statistik "Rule aktif/Auto-scan" & tombol "Scan
Sekarang" di `HomeScreen` sama-sama menampakkan potongan persegi "hantu" di
sisi kanan(-bawah), tidak simetris dgn sisi kiri. Root cause: keduanya lewat
`NeumorphicSurface` (lihat `Neumorphic.kt`) yang butuh `baseColor` = warna
LATAR SESUNGGUHNYA supaya badan shadow-caster-nya (persegi solid yg digeser
`shadowOffset` ke kanan-bawah) menyatu tak terlihat dgn sekitarnya -- kedua
pemanggil ini diam-diam pakai default `AmoledBackground`, padahal `HomeScreen`
(SATU-SATUNYA layar di app ini berlatar gradient, `Brush.verticalGradient`
`colors.surfaceVariant` 55% alpha -> `colors.background`, ditambahkan sejak
fix UI-10) TIDAK berlatar solid AmoledBackground di area itu -- badan
shadow-caster jadi nongol sbg persegi salah warna, gejala visual "kartu
hantu" mengintip persis sisi geser shadow (kanan-bawah = asimetris, bukan
simetris di semua sisi seperti seharusnya cuma shadow tipis biasa). TIDAK
terjadi di 12 pemanggil `VaultCard` lain (semua berlatar solid, tidak kena
kelas bug ini).

**Fix**: `VaultCard` sekarang menerima parameter `baseColor` opsional
(default TETAP `AmoledBackground`, 0 perubahan di 12 call site lain).
`HomeScreen` menghitung warna latar EFEKTIF di titik gradient teratas
(`colors.surfaceVariant.copy(alpha=0.55f).compositeOver(colors.background)`,
pakai `Color.compositeOver()` bawaan Compose, bukan pendekatan manual) lalu
mengoper itu ke `VaultCard` (kartu stat) & `NeumorphicSurface` CTA (tombol
Scan Sekarang) -- 2 satu-satunya pemanggil di layar bergradient itu. Tidak
sempurna 100% di semua posisi scroll (background gradient diam, konten
scroll di atasnya), tapi menghilangkan artefak di posisi normal/tanpa-scroll
(persis kondisi screenshot user) & jauh lebih dekat drpd default polos.

**Preflight ditambah kategori #12** (`scripts/preflight_check.sh`): tiap
file layar dgn `Brush.*Gradient` di latar, cek ADA pemanggilan
`baseColor` di file yg sama -- heuristik per-file (bukan presisi per-baris),
supaya kelas bug ini (baseColor default meleset dari latar non-solid)
kepantau otomatis di masa depan, bukan cuma titik ini.

**Ditinjau & DIBIARKAN (bukan bug)**: screenshot 1 (Kelola Rule) menampilkan
FAB "+" menumpuk ikon hapus kartu rule #3 dari 6 -- ini perilaku FAB
mengambang standar Android SAAT BELUM di-scroll penuh (kartu TERAKHIR sudah
diberi `contentPadding(bottom=88dp)` sejak v2.24.0 #UI-20 supaya tetap bisa
di-tap penuh setelah discroll -- itu fix yg relevan, sudah ada). Tidak ada
kode yang diubah utk gejala ini -- bukan regresi, bukan kelas bug yang sama
dgn Insiden #9.

File diubah (4): `ui/components/VaultCard.kt` (param `baseColor`),
`ui/screens/HomeScreen.kt` (hitung & pasang `homeCardBaseColor`),
`scripts/preflight_check.sh` (kategori #12 baru), `app/build.gradle.kts`
(versi). `scripts/preflight_check.sh` lolos bersih 12/12. **BELUM PERNAH
lewat `./gradlew` asli / device asli.** User WAJIB verifikasi di HP: kartu
stat & tombol Scan Sekarang di layar utama tidak lagi ada potongan persegi
mengintip di sisi kanan/bawah.

## v2.24.2 -- FIX BUG NYATA (laporan user + screenshot): tab "Undo Pemindahan" hilang, "Log" melebar + kotak kosong raksasa di Riwayat Aktivitas (2026-08-15)

User kirim 2 screenshot HP asli v2.24.1 (dibandingkan ke screenshot v2.20.3
sebagai referensi struktur lama): layar "Riwayat Aktivitas" -- segmented
control cuma menampilkan pil "Log" MELEBAR SELEBAR LAYAR, tab "Undo
Pemindahan" hilang total, DAN ada kotak hijau gelap kosong raksasa di
bawahnya sebelum daftar log mulai muncul.

**Root cause (`ui/components/Neumorphic.kt`, `NeumorphicSurface`)**:
`SegmentedControl.kt` memanggil `NeumorphicSurface(modifier =
Modifier.weight(1f), ...)` untuk segment yang SEDANG TERPILIH (di dalam
`Row` tab Log/Undo). `NeumorphicSurface` SEBELUMNYA memasang `modifier`
parameter ini ke `Surface` KONTEN yang letaknya beberapa lapis `Box` DI
DALAM, bukan ke `Box` TERLUAR yang benar-benar jadi anak langsung `Row`
pemanggil. `RowScope.weight()` adalah `ParentDataModifier` -- HANYA
terbaca oleh `Row` kalau nempel di modifier chain anak LANGSUNG-nya. Karena
`weight()` di sini nyasar ke `Surface` (bukan `Box` terluar), `Row` sama
sekali TIDAK menganggap segment itu "punya weight" -- diperlakukan sebagai
child tak-berbobot yang lebar internalnya (lewat `Text(Modifier
.fillMaxWidth())` di dalam) malah mengambil SELURUH lebar `Row` yang
tersedia, menyisakan NOL ruang untuk segment "Undo Pemindahan" (yang
weight-nya justru valid tapi kehabisan sisa ruang) -- match PERSIS gejala
di screenshot (satu pil melebar penuh, satunya lenyap).

**Ini KELAS BUG BARU yang belum pernah tercatat di project ini** -- mirip
semangatnya dengan Insiden #3 lama (modifier di lapisan wrapper yang salah
menyebabkan ukuran kacau), tapi soal `ParentDataModifier`/`weight()`,
bukan soal `fillMaxSize()`. Dicatat sebagai **Insiden #8** di
`PROJECT_STATE.md`.

**Fix**: `modifier` parameter sekarang dipasang LANGSUNG di `Box` terluar
`NeumorphicSurface` (root komposabel sesuai konvensi resmi Compose), bukan
lagi di `Surface` konten. `Surface` konten SENGAJA TETAP bukan
`matchParentSize()` (beda dari shadow-caster) -- ia anak `Box` biasa yang
mewarisi constraints yang sama, supaya kasus umum (`modifier` cuma
mengunci lebar, mis. `fillMaxWidth()`/`weight()`, tanpa tinggi eksplisit)
`Box` TETAP wrap-content tinggi mengikuti `Surface` persis seperti
sebelumnya -- **0 perubahan visual utk 6 pemanggil lain** (`VaultCard`,
`GroupedListRow`, `TactileSwitch` x2, `EmptyState`, `VaultActionSheet`,
CTA `HomeScreen`), semua sudah pakai modifier ukuran biasa (size/
fillMaxWidth/padding/offset/scale, bukan `weight()`) -- diverifikasi
manual satu-satu (lihat grep cross-check di bawah) sebelum ZIP dipaket.

**Verifikasi sebelum paket**: `grep -rn "\.weight("` di seluruh `ui/` --
HANYA 1 titik di seluruh codebase yang mengoper `weight()` sebagai
`modifier` PARAMETER ke dalam `NeumorphicSurface` (persis titik bug ini,
`SegmentedControl.kt` segment terpilih); semua pemakaian `weight()`
lainnya (`GroupedListRow`, `RuleCard` Spacer, `ActivityLogScreen` x2,
`AddEditRuleScreen` x2, `DiagnosticsScreen`, `OnboardingScreen`) dipasang
LANGSUNG ke elemen native (`Row`/`Column`/`Spacer`), tidak lewat wrapper
apa pun -- tidak kena kelas bug yang sama. `scripts/preflight_check.sh`
lolos bersih 11/11.

File diubah (2): `ui/components/Neumorphic.kt` (fix inti + javadoc
diperbarui), `app/build.gradle.kts` (versi).

**BELUM PERNAH lewat `./gradlew` asli / device asli.** User WAJIB
verifikasi di HP: buka "Riwayat Aktivitas", pastikan pil "Log" dan "Undo
Pemindahan" tampil BERDAMPINGAN (bukan satu melebar penuh), tidak ada
kotak kosong di bawahnya, dan kedua tab tetap bisa di-tap normal. Kalau
masih ada gejala serupa (elemen melebar tak wajar / hilang) di komponen
LAIN yang juga pakai `NeumorphicSurface` + `weight()` di masa depan,
kemungkinan pola bug yang sama -- cek dulu `modifier` param-nya dipasang
di `Box` terluar, bukan lapisan dalam.

## v2.24.1 -- COMPILE-FIX: `"--"` di komentar `colors.xml` + rapikan urutan seluruh dokumentasi (2026-08-15)

User upload `build-failure-log-v2_24_0.zip`. `:app:mergeDebugResources FAILED`
-- `SAXParseException: The string "--" is not permitted within comments`,
`colors.xml:11`. Root cause: komentar riwayat re-palette token `pv_platinum_
accent` (v6.0.0) memakai `--` sebagai pemisah kalimat di badan `<!-- -->` --
PERSIS kelas bug yang sama dengan Insiden `v2.6.0` (2026-08-05,
`AndroidManifest.xml`) yang sudah didokumentasikan sebagai pelajaran
permanen di `PROJECT_STATE.md`, tapi terulang lagi di file `.xml` lain.

**Fix**: ganti `--` jadi koma di komentar `colors.xml`, tidak ada perubahan
logika/warna. Scan ulang SEMUA `res/**/*.xml` + `AndroidManifest.xml` pakai
`xml.dom.minidom.parse` (kategori #10 `preflight_check.sh`) -- 0 pelanggaran
lain ditemukan.

**Dokumentasi dirapikan (permintaan eksplisit user)**: `CHANGELOG.md` dan
`PROJECT_STATE.md` diaudit ulang urutannya -- beberapa entri lama ternyata
tidak strictly newest-first (mis. `v2.11.x`/`v2.9.x` di `CHANGELOG.md`
sempat ke-append di bawah entri `v2.1.x` yang lebih lama; entri Insiden
`v2.24.0` di `PROJECT_STATE.md` sempat nyangkut di baris PALING BAWAH file
alih-alih jadi status teratas). Semua entri diurutkan ulang murni
berdasarkan versi/tanggal (descending), TIDAK ADA teks/riwayat yang dihapus
atau ditulis ulang isinya -- hanya reposisi. `README.md` (versi di judul
sempat basi, "v2.1.4") disamakan ke `versionName` aktual.

File diubah (3): `app/src/main/res/values/colors.xml` (fix bug), `app/
build.gradle.kts` (versi), `README.md` (sinkron versi judul). Reorder murni
(0 perubahan isi kalimat) di `CHANGELOG.md` & `PROJECT_STATE.md`.

**Belum diverifikasi CI** -- fix ini murni syntax XML (well-formedness
sudah divalidasi lokal via parser), risiko regresi sangat rendah, tapi tetap
WAJIB dicek build hijau di Actions sebelum dianggap final.

## v2.24.0 -- Debug + polish: fix bug FAB nutup aksi kartu, re-palette Platinum+Ruby (2026-08-15)
User kirim 3 screenshot: (1) riwayat aktivitas normal, (2) layar "Kelola
Rule" -- tombol "+" (FAB tambah rule) MENUTUPI ikon Hapus di kartu rule
terakhir ("FileManager project"), (3) Home normal. Diminta debug bug UI +
redesign palet jadi "Platinum + Ruby blend" premium, "anti gagal".

- **Bug nyata #UI-20 (`RuleListScreen.kt`)**: `LazyColumn` daftar rule TIDAK
  punya `contentPadding` bawah -- `Scaffold` M3 TIDAK otomatis menghindarkan
  `floatingActionButton` dari konten (FAB by design melayang DI ATAS,
  content harus kasih padding sendiri). Akibatnya kartu rule terakhir
  (khususnya tombol Edit/Hapus) ketutup fisik & optik oleh FAB "+" --
  PERSIS match screenshot #2 user. Fix: `contentPadding = PaddingValues
  (bottom = 88.dp)` (56dp tinggi FAB M3 + margin aman).
- **Bug laten #UI-21 (`Color.kt`, ditemukan saat audit warna sebelum
  re-palette)**: token lama `StampGlow` (`#FF6E52`, badge sukses) vs
  `RustGlow` (`#FF6B5C`, error) HAMPIR IDENTIK hex-nya -- 2 makna semantik
  beda (sukses vs error) nyaris tak terbedakan mata. Tertutup otomatis oleh
  re-palette di bawah (Ruby baru digeser jauh ke hue crimson).
- **Re-palette v6.0.0 (`Color.kt`, `Theme.kt`)**: "Transformative Teal" ->
  "Platinum + Ruby" (permintaan eksplisit). `TealAccent*` -> `PlatinumAccent*`
  (primary, silver-platinum dingin `#DCE2E9`), `StampGlow*` -> `RubyGlow*`
  (secondary, crimson jenuh `#E23A55`). `AmberGlow`/`RustGlow`/`SlateGlow`
  TIDAK diubah nilainya (sudah cukup beda hue). `onSecondary` sekarang
  `RubyOn` baru (terang) -- BUKAN reuse `PlatinumAccentOn` (gelap) seperti
  pola lama, krn Ruby cukup jenuh/gelap-value shg teks terang kontrasnya
  lebih baik (a11y).
- **CTA "Scan Sekarang" (`HomeScreen.kt`)**: gradient diganti dari (Stamp ->
  Amber) jadi (Ruby -> Platinum) dgn `colorStops` TIDAK merata (0f/0.65f/1f)
  -- 65% area tengah (tempat label teks) tetap solid Ruby demi kontras teks
  aman, 35% sisi kanan "meleleh" ke Platinum terang utk kesan blend premium
  nyata (bukan cuma 2 warna solid berdampingan).
- **`colors.xml`**: `pv_teal_accent` -> `pv_platinum_accent` (`#DCE2E9`) --
  disamakan walau saat ini tidak direferensikan XML lain, supaya tidak jadi
  sisa hex basi.
- Verifikasi sebelum ZIP: `grep -rn "TealAccent\|StampGlow\|TealTint"` di
  seluruh `app/src/main/java` & `res` -- 0 referensi kode aktif tersisa
  (hanya komentar historis penjelas rename). `grep -rn "0xFF"` di seluruh
  `ui/` di luar `Color.kt` -- 0 hasil (tidak ada hex hardcode lain yang
  perlu ikut diubah, semua komponen sudah theme-aware sejak lama).
- File diubah (6): `ui/theme/Color.kt`, `ui/theme/Theme.kt`,
  `ui/screens/HomeScreen.kt`, `ui/screens/RuleListScreen.kt`,
  `ui/components/VaultCard.kt` (komentar saja), `app/build.gradle.kts`
  (versi) + `res/values/colors.xml`. `AndroidManifest.xml`/`MainActivity.kt`
  (Protected Assets) **TIDAK disentuh** -- keduanya cuma referensi
  `AmoledBackground` yang nilainya tidak berubah.
- **Belum diverifikasi**: build CI + tampilan nyata di device (Termux tidak
  attach ke compiler Android/preview Compose) -- kontras Ruby/teks & efek
  blend CTA perlu dicek visual oleh user pasca-install.

## v2.23.0 -- Fix 9 temuan P2 dari audit statis UI v2.21.1 (batch 2/2, PENUTUP audit) (2026-08-15)
Lanjutan v2.22.0 (P1). Audit ulang ke kode aktual dulu: 2 dari 9 item P2
ternyata SUDAH tertutup co-located di batch P1 (#UI-14, #UI-16), dan #UI-15
sudah tergabung eksekusinya dengan #UI-08. 6 item sisanya dieksekusi di sini.

- **#UI-11 (`SettingsScreen.kt`)**: `friendlySafFolderLabel()` sebelumnya
  cuma ambil bagian setelah ':' TERAKHIR di seluruh string URI -- root/
  provider (mis. "primary" vs id kartu SD) hilang dari label, 2 folder beda
  storage dgn path akhir sama tampil identik/ambigu. Fix: ambil segmen
  setelah "/tree/" dulu, tampilkan `path (root)` -- root & path relatif
  sama-sama terlihat.
- **#UI-12 (`SettingsScreen.kt`)**: tombol "Salin JSON" + Snackbar
  konfirmasi ditambah di kartu Export (field read-only preview tetap ada).
  Pola identik "Salin Log" `ActivityLogScreen.kt` (`ClipboardManager` +
  `AnnotatedString`, Insiden #6).
- **#UI-13 (`RuleRepository.kt`, `MainViewModel.kt`, `SettingsScreen.kt`)**:
  `RuleRepository.importFromJson()` return `Int` -> data class
  `ImportOutcome(parseSuccess: Boolean, importedCount: Int)` -- sebelumnya
  "0 rule diimpor" ambigu (parse gagal total vs JSON valid tapi array
  kosong). `MainViewModel.importRulesJson()` callback jadi `(Boolean, Int)
  -> Unit` (pass-through murni). `SettingsScreen` dapat sealed private
  `ImportResultUiState` (Success/Warning/Error) dgn warna beda
  (primary/tertiary/error). `MainActivity.kt` (Protected Asset) **TIDAK
  disentuh** -- lambda wiring `{ text, cb -> viewModel.importRulesJson(text,
  cb) }` type-infer otomatis cocok di kedua sisi, diverifikasi via grep
  cross-reference semua caller sebelum diklaim aman.
- **#UI-14, #UI-16**: sudah tertutup di v2.22.0 (co-located dgn batch P1,
  lihat entri di bawah) -- tidak ada perubahan lagi di sini.
- **#UI-15**: sudah tertutup di v2.22.0 (digabung eksekusinya dgn #UI-08 di
  `DiagnosticsScreen.kt`) -- tidak ada perubahan lagi di sini.
- **#UI-17 (audit, `HomeScreen.kt`/`GroupedListRow.kt`/`ManifestRow()`)**:
  diaudit manual ulang sesuai catatan audit sendiri ("prioritas P2 hanya
  bila TalkBack tidak dapat konteks cukup"). Semua icon `contentDescription
  = null` di 3 lokasi itu decoratif, bersebelahan langsung dgn `Text` yang
  membawa makna sama, atau chevron affordance yang redundan dgn state
  clickable Row. **TIDAK ada gap nyata -- nol perubahan kode untuk item
  ini**, ditutup sbg "diverifikasi", bukan "diasumsikan aman".
- **#UI-18 (`SegmentedControl.kt`)**: `selectedIndex` di-`coerceIn(0,
  options.lastIndex)` sebelum dibandingkan per-segment -- sebelumnya index
  di luar range = tidak ada segment yang terlihat terpilih (hardening,
  belum ada laporan bug aktif).
- **#UI-19 (`SegmentedControl.kt`)**: audit ulang menemukan gap NYATA (bukan
  cuma beda gaya) -- segment TIDAK terpilih sebelumnya nol feedback tekan
  (`indication = null`, tanpa scale), beda dgn segment terpilih yang
  otomatis dapat ripple bawaan `NeumorphicSurface(onClick=...)`. Fix: reuse
  `pressScale()` (sudah ada di `PressScale.kt`) di segment tidak-terpilih --
  scale dipilih (bukan ripple) supaya konsisten dgn keluarga kontrol
  neumorphic lain (CTA Home, TactileSwitch). Keputusan eksplisit: 2 keluarga
  feedback (ripple utk row list flat, scale utk kontrol neumorphic)
  DIPERTAHANKAN sbg desain sengaja, bukan diseragamkan paksa jadi 1 pola.
- File diubah (5): `data/RuleRepository.kt`, `ui/MainViewModel.kt`,
  `ui/screens/SettingsScreen.kt`, `ui/components/SegmentedControl.kt`,
  `app/build.gradle.kts` (versi). `scripts/preflight_check.sh` lolos bersih
  11/11. 1 typo ketangkap sendiri sebelum ZIP dipaket (parameter type
  `onImportRequested` di signature `SettingsScreen` sempat telat diupdate
  dari `(Int)->Unit`, ketahuan via grep cross-reference semua caller/callee
  terkait import, bukan lolos ke user) + 1 unused import (`IconButton`)
  dibersihkan. **BELUM PERNAH lewat `./gradlew` asli.**
- versionCode 71->72, versionName 2.22.0->2.23.0.
- **STATUS: audit UI v2.21.1 (19 temuan: 10 P1 + 9 P2) SEKARANG TERTUTUP
  SEMUA** antara v2.22.0 dan v2.23.0 ini.

## v2.22.0 -- Fix 10 temuan P1 dari audit statis UI v2.21.1 (batch 1/2, semua P1) (2026-08-15)
User upload `PromptVault_v2_21_1_UI_Audit.txt` (audit statis UI eksternal,
10 temuan P1 + 9 P2, 0 P0). Batch ini menuntaskan SEMUA 10 P1 (P2 menyusul
batch terpisah sesuai catatan audit "sebaiknya masuk batch berikutnya").

- **#UI-01 (HomeScreen.kt)**: Column body Home sekarang `verticalScroll` --
  sebelumnya `fillMaxSize()` tanpa scroll berisiko menu bawah terdorong
  keluar viewport di layar pendek/landscape/font scale besar.
- **#UI-02 (OnboardingScreen.kt)**: area konten step (`ProgressDots` +
  ikon+teks) dipisah dari area tombol lewat `Modifier.weight(1f, fill =
  false).verticalScroll(...)` -- tombol tetap di area aman bawah, konten
  scroll kalau diperlukan.
- **#UI-03 (RuleCard.kt)**: redesign 2-baris -- metadata (nama folder/
  pattern/exclude/warning) di baris atas dgn `maxLines`+`TextOverflow.
  Ellipsis` terencana, kontrol aksi (reorder/switch/edit/delete) di baris
  bawah, masing-masing `sizeIn(minWidth/minHeight = 48.dp)`. Sebelumnya
  semua kontrol dipaksa dalam 1 Row sempit.
- **#UI-04 (TactileSwitch.kt)**: hitbox `toggleable` dipindah ke `Box`
  pembungkus `sizeIn(minWidth/minHeight = 48.dp)`, track visual TETAP
  46x26dp (tampilan tidak berubah) di-center di dalamnya -- sebelumnya
  toggleable dipasang langsung di track 46x26dp, area sentuh lebih kecil
  dari target nyaman.
- **#UI-05 (GroupedListRow.kt)**: `selectable(..., indication = null)`
  diganti `clickable(indication = LocalIndication.current, ...)` -- row
  sekarang punya ripple/pressed state, sebelumnya tap tidak memberi sinyal
  visual sama sekali.
- **#UI-06 & #UI-07 (OnboardingScreen.kt)**: teks step "Buat rule" sekarang
  eksplisit menyebut default Downloads/PromptVault/ DAN opsi folder tujuan
  kustom SAF (sebelumnya cuma menyebut Downloads/PromptVault/, membentuk
  ekspektasi salah pasca fitur SAF diperluas). Teks step "Izin penyimpanan"
  diganti jadi generik ("izin penyimpanan yang sesuai versi Android kamu"),
  sebelumnya klaim "izin akses semua file" tidak akurat untuk semua API yang
  didukung (minSdk 26 dgn cabang izin API lama).
- **#UI-08 & #UI-15 (DiagnosticsScreen.kt)**: entry crash log dibungkus
  `Row` `sizeIn(minHeight = 48.dp)` + `clickable(indication = LocalIndication.
  current, ...)` + ikon chevron sbg affordance -- sebelumnya `clickable`
  langsung di `Text` tanpa indication & tanpa touch target eksplisit.
- **#UI-09 (SkippedFilesScreen.kt)**: parameter baru `hasScannedBefore:
  Boolean` (diisi caller dari `lastScanSummary != null` di `MainActivity.
  kt`) -- empty state sekarang beda pesan untuk "belum pernah scan" vs
  "sudah scan, 0 skipped", sebelumnya digabung jadi 1 pesan ambigu.
- **#UI-10 (HomeScreen.kt)**: `Brush.verticalGradient(..., endY = 900f)`
  hardcode pixel absolut dihapus, dibiarkan default (`Float.
  POSITIVE_INFINITY`, resolve otomatis ke tinggi area gambar sesungguhnya
  saat draw) -- sebelumnya distribusi gradient tidak proporsional lintas
  device/densitas berbeda.
- **Sekalian (P2 ringan, co-located, tidak menambah file)**: #UI-14
  (`RuleCard.kt`, `rule.folderName.uppercase()` dihapus, casing asli
  dipertahankan) & #UI-16 (`HomeScreen.kt` `ManifestRow`, alignment
  berbasis whitespace string diganti `Row`+`Spacer` eksplisit).

File diubah (8): `ui/screens/{HomeScreen,OnboardingScreen,
DiagnosticsScreen,SkippedFilesScreen}.kt`, `ui/components/{RuleCard,
TactileSwitch,GroupedListRow}.kt`, `MainActivity.kt` (Protected Asset, edit
parsial -- 1 titik: teruskan `hasScannedBefore` ke `SkippedFilesScreen`),
`app/build.gradle.kts` (versi). `scripts/preflight_check.sh` lolos bersih
11/11 (1 iterasi fix: `import androidx.compose.foundation.layout.weight`
sempat ditambahkan salah di 3 file -- `weight()` itu member extension
`RowScope`/`ColumnScope`, BUKAN top-level function, tidak butuh import
eksplisit di dalam lambda `Row{}`/`Column{}` -- persis kelas bug yang sama
dgn insiden `animateItemPlacement` v2.3.7 lama; dikoreksi sebelum ZIP
di-package, bukan lolos ke user). **BELUM PERNAH lewat `./gradlew` asli.**
Sisa 9 temuan P2 dari audit (copy JSON export, status import lebih
eksplisit, dll) BELUM dieksekusi di batch ini -- kandidat batch berikutnya.

## v2.21.1 -- Merge 3-way: redesign Neumorphism (cabang v2.21.0) + fix teknis (cabang v2.20.3), TANPA regresi (2026-08-14)
User minta merge hasil & dokumentasi dari paket `v2_21_0` ke `v2_20_3` tanpa
regresi. Root cause: kedua paket ZIP adalah 2 cabang independen yang sama-sama
lanjut dari v2.20.1 (versionCode 67) tapi divergen --
`v2_21_0` (versionCode 68) lompat langsung ke redesign Neumorphism TANPA
melewati v2.20.2/v2.20.3, sedangkan `v2_20_3` (versionCode 69) berisi 2 fix
teknis (`SCAN_CONCURRENCY` configurable + migrasi legacy DataStore + fix
import `decodeFromString`) tapi TIDAK punya redesign Neumorphism.

**Verifikasi sebelum merge** (`diff -rq` penuh + grep silang): dipastikan 9
file redesign Neumorphism (`VaultCard`, `GroupedListRow`, `TactileSwitch`,
`VaultActionSheet`, `EmptyState`, `SegmentedControl`, `HomeScreen`, `Color.kt`,
`TactileTokens.kt`) + 1 file baru (`Neumorphic.kt`) MURNI perubahan visual --
tidak menyentuh/menghapus apa pun yang berkaitan dengan `SCAN_CONCURRENCY`
atau `LegacyDataMigration`. Token lama `TactileTokens.Elevation{Card,Cta,
CtaPressed,Icon,Thumb}` DIHAPUS & diganti `Neu*` di batch Neumorphism --
dicek tidak ada file lain (`SettingsScreen.kt`, dll) yang masih memakai nama
token lama sebelum merge (aman, tidak ada dangling reference).

**Strategi merge**: base diambil dari `v2_20_3` (punya seluruh fix teknis
terbaru), lalu 10 file redesign Neumorphism di atas di-copy utuh dari
`v2_21_0` menimpa versi lama. 9 file lain yang tadinya SAMA-SAMA berubah di
kedua cabang (`PromptVaultApp.kt`, `RuleRepository.kt`,
`SettingsRepository.kt`, `MainViewModel.kt`, `MainActivity.kt`,
`FileSorter.kt`, `SettingsScreen.kt`, `scripts/preflight_check.sh`,
`app/build.gradle.kts`) TETAP pakai versi `v2_20_3` (base) karena
`v2_21_0` untuk file-file itu justru versi LEBIH LAMA (pre-2.20.2) --
mengambilnya akan jadi regresi (hilang `LegacyDataMigration`, hilang
`SCAN_CONCURRENCY` configurable, hilang import `decodeFromString`).

File diubah (10, semua neumorphism, dari cabang `v2_21_0`):
`ui/components/{VaultCard,GroupedListRow,TactileSwitch,VaultActionSheet,
EmptyState,SegmentedControl,Neumorphic(baru)}.kt`, `ui/screens/HomeScreen.kt`,
`ui/theme/{Color,TactileTokens}.kt`. File dipertahankan dari `v2_20_3` (9,
supaya nol regresi): `PromptVaultApp.kt`, `data/{RuleRepository,
SettingsRepository,LegacyDataMigration}.kt`, `ui/MainViewModel.kt`,
`MainActivity.kt`, `util/FileSorter.kt`, `ui/screens/SettingsScreen.kt`,
`scripts/preflight_check.sh`. `scripts/preflight_check.sh` lolos bersih
(11/11) setelah merge. **BELUM PERNAH lewat `./gradlew` asli** -- WAJIB
verifikasi CI + smoke test manual (redesign Neumorphism + migrasi legacy +
scan concurrency setting) di HP asli sebelum rilis produksi.

versionCode 69->70, versionName 2.20.3->2.21.1 (2.21.x krn base fitur
Neumorphism v2.21.0 + patch merge; bukan 2.20.4 krn perubahan visual besar,
bukan cuma technical debt).

## v2.20.3 -- FIX bug compile laten nyata: `RuleRepository.kt` decodeFromString tanpa import (2026-08-14)
Eksekusi item yang sebelumnya dicatat sbg "Observasi TIDAK dieksekusi" di
v2.20.2 -- user minta lanjutkan penyempurnaan bertahap (trigger valid sesuai
catatan lama: instruksi eksplisit lanjut, bukan tebakan proaktif).

**Bug**: `RuleRepository.kt` memanggil `json.decodeFromString<List<Rule>>(...)`
(2 titik: `rulesFlow` & `importFromJson`) tapi cuma mengimpor
`kotlinx.serialization.encodeToString` & `kotlinx.serialization.json.Json` --
`decodeFromString<T>()` reified generic BUTUH import eksplisit terpisah
(`kotlinx.serialization.decodeFromString`), tidak otomatis ikut dari import
`Json`. Berpotensi `Unresolved reference` di compiler asli -- belum pernah
ketahuan krn project ini belum pernah lewat `./gradlew` asli.

**Fix**: tambah 1 baris import di `RuleRepository.kt`. Tidak ada perubahan
logika lain.

**Preflight ditambah kategori #11** (`scripts/preflight_check.sh`): grep semua
file yang pakai `.decodeFromString<` lalu pastikan pasangan importnya ada --
supaya kelas bug ini (generic reified tanpa import) tidak lolos lagi ke kode
manapun di masa depan, bukan cuma fix titik ini.

File diubah (2): `data/RuleRepository.kt` (1 baris import),
`scripts/preflight_check.sh` (kategori #11 baru), `app/build.gradle.kts`
(versi). `scripts/preflight_check.sh` lolos bersih (11/11). **BELUM PERNAH
lewat `./gradlew` asli.** Risiko regresi: sangat rendah (murni tambah import,
tidak ada logika disentuh).

## v2.20.2 -- Eksekusi 2 technical debt tercatat: SCAN_CONCURRENCY configurable + migrasi best-effort DataStore lama (2026-08-13)
User minta lanjutkan 2 item pending spesifik (bukan testing), atas instruksi
eksplisit "kerjakan tanpa regresi".

**1. `SCAN_CONCURRENCY` configurable** (`SettingsRepository.kt`,
`FileSorter.kt`, `SettingsScreen.kt`, `MainViewModel.kt`, `MainActivity.kt`):
konstanta hardcode `6` (v2.4.0, "asumsi teknis AI belum divalidasi
profiling") sekarang bisa diatur dari kartu baru "Kecepatan Scan (Lanjutan)"
di Pengaturan, pilihan `[2, 4, 6, 8, 12]`. Default TETAP 6 -- nol dampak
untuk siapa pun yang tidak membuka setting ini. Pola FilterChip meniru
persis interval auto-scan yang sudah ada, rentang 2..12 dipilih berdasar
alasan teknis (di bawah 2 nyaris menghilangkan manfaat paralelisme; di atas
12 berisiko terlalu banyak file handle bersamaan di HP kelas bawah), bukan
data profiling baru (memang belum ada, tidak diklaim ada).

**2. Migrasi best-effort DataStore lama -> Room** (`LegacyDataMigration.kt`,
baru; dipanggil sekali dari `PromptVaultApp.onCreate()`): item ini SEBELUMNYA
sengaja tidak dieksekusi karena berisiko jadi "migrasi buta" (lihat catatan
lama di PROJECT_STATE.md). Setelah diperiksa ulang: `ActivityLogEntry`/
`MoveHistoryEntry` (domain model) sudah `@Serializable` sejak awal dengan
bentuk field IDENTIK ke entity Room sekarang -- jadi BENTUK JSON kemungkinan
besar benar. Yang TIDAK bisa diverifikasi: nama key literal DataStore era
pre-v2.2.0 (kode lamanya sudah terhapus total sejak migrasi Room, snapshot
project ini juga tanpa riwayat git). Key yang dipakai (`"activity_log_json"`,
`"move_history_json"`) adalah INFERENSI dari konvensi penamaan konsisten
project ini (pola `{noun}_json`, lihat `RuleRepository` -- `"rules_json"`),
BUKAN nilai terkonfirmasi. **Didesain aman walau tebakan key salah**: kalau
key tidak ketemu/JSON tidak valid, hasilnya nol baris termigrasi (no-op),
persis situasi status quo sejak v2.2.0 -- BUKAN crash atau data korup.
Guard flag di DataStore memastikan proses ini jalan sekali seumur install,
di-set `true` di blok `finally` APAPUN hasilnya (termasuk gagal) supaya
tidak retry-loop tiap app dibuka kalau data memang tidak kompatibel.
Dibungkus try-catch total (fail-safe, non-kritis).

**Observasi TIDAK dieksekusi (di luar scope 2 item ini)**: `RuleRepository.kt`
memanggil `json.decodeFromString<List<Rule>>(...)` tapi hanya mengimpor
`kotlinx.serialization.encodeToString`/`Json`, TANPA impor
`kotlinx.serialization.decodeFromString` -- fungsi generic reified itu
biasanya butuh impor eksplisit itu utk resolve. Berpotensi compile error
laten yang belum pernah ketahuan karena project ini belum pernah lewat
`./gradlew` asli di sandbox manapun. TIDAK diperbaiki di batch ini (di luar
2 item yang diminta) -- dicatat di sini + PROJECT_STATE.md supaya tidak
lupa, valid dicek/dikerjakan kalau build CI berikutnya gagal di titik ini
atau user eksplisit minta.

File diubah (8): `SettingsRepository.kt`, `FileSorter.kt`,
`SettingsScreen.kt`, `MainViewModel.kt`, `MainActivity.kt`,
`PromptVaultApp.kt`, `LegacyDataMigration.kt` (baru), `app/build.gradle.kts`
(versi). Preflight lolos bersih. **BELUM PERNAH lewat `./gradlew` asli** --
user WAJIB verifikasi: (1) kartu "Kecepatan Scan" di Pengaturan tersimpan &
scan tetap jalan normal di tiap pilihan, (2) tab Log/Undo setelah update --
kalau user ini memang punya riwayat sangat lama, cek apakah data lama
muncul lagi (indikasi tebakan key BENAR) atau tetap kosong (indikasi
tebakan key salah, laporkan balik supaya bisa dikoreksi dgn data nyata).

versionCode 67->68, versionName 2.20.1->2.20.2.

## v2.20.1 -- Fix technical debt: undo() jalan di Main thread, bukan IO (2026-08-13)
User minta lanjutkan item pending tercatat (bukan testing). Ditemukan 2
kandidat di `PROJECT_STATE.md`: dispatcher `undo()` & snackbar "Simpan".

**Snackbar "Simpan"**: dicek ulang ke kode aktual -- TERNYATA sudah
diimplementasi sejak v2.16.0 (`RuleSaveFeedback` + `RuleListScreen`).
Catatan lama yang bilang ini masih gap sudah usang, tidak dieksekusi ulang.

**Dispatcher `undo()` (yang genuinely dieksekusi)**: `MainViewModel.undoMove()`
dibungkus `withContext(Dispatchers.IO) { fileSorter.undo(entry) }` -- pola
identik `checkSafAccessLost()` di file yang sama. Sebelumnya, karena caller
(`ActivityLogScreen` via `rememberCoroutineScope()`) default ke
`Dispatchers.Main`, seluruh I/O undo (baca/tulis file lokal maupun
`DocumentFile`/`ContentResolver` untuk folder kustom SAF) jalan di main
thread -- berisiko ANR di file besar/provider SAF lambat. Fix di 1 titik
(ViewModel), `FileSorter.kt` (3 fungsi undo di dalamnya) TIDAK disentuh.

File diubah (2): `ui/MainViewModel.kt`, `app/build.gradle.kts` (versi).
`scripts/preflight_check.sh` lolos bersih. **BELUM PERNAH lewat `./gradlew`
asli** -- perlu verifikasi CI + undo manual di HP asli (lokal & SAF).

## v2.20.0 -- Rebrand palet total "Midnight Blue" -> "Transformative Teal" + sistem depth/3D ultra immersive (2026-08-13)
Permintaan eksplisit user: ganti seluruh palet warna lama + tambah efek
depth/3D immersive. Ini Atomic Change (9 file, di luar batas normal 10
file/1 modul, tapi 1 layer visual yang saling terkait -- sama persis
precedent v2.14.0 yang juga menyentuh 9 file untuk redesign total).

**Palet** (`Color.kt`): seluruh token `MidnightBlue*` (indigo dingin)
di-rename + di-re-hex jadi `Teal*` (biru-hijau). `TealGradientAlpha`
dinaikkan dari 0.06f -> 0.10f (permintaan "ultra immersive" butuh tint
lebih terasa, tetap ambient bukan warna dominan). `SlateGlow` (aksen
"Pengaturan") digeser dari keluarga biru murni ke indigo-periwinkle
supaya tidak mirip lagi dengan TealAccent baru (jaga Keputusan Arsitektur
#3: 4 aksen menu harus beda hue). `AmoledBackground`/`pv_amoled_background`
(colors.xml, dipakai splash + launcher icon background) ikut di-retint;
`pv_midnight_blue_accent` -> `pv_teal_accent`. `ic_launcher_foreground.xml`
(vektor "kartu index/manifest" krem/rust) SENGAJA TIDAK disentuh -- itu
keputusan desain terpisah dari v2.14.0 (ikon tetap krem di atas background
AMOLED), bukan bagian dari sistem token warna UI ini; asumsi diambil kalau
user MAKSUD "palet warna lama" itu skema UI Compose yang sudah beberapa
kali diiterasi (Midnight Blue), bukan artwork launcher icon.

**Depth/3D** (token baru `TactileTokens.ElevationCard/Cta/CtaPressed/Icon/
Thumb`): `VaultCard`, CTA "Scan Sekarang" (`HomeScreen.kt`), kotak ikon
`GroupedListRow`, dan thumb `TactileSwitch` (saat ON) sekarang punya
elevasi bayangan NYATA (v3.0.0 semua flat, `shadowElevation=0.dp`).

**Kenapa BUKAN sekadar `Modifier.shadow(...).background(brush)` langsung**
(ini bagian paling penting untuk sesi berikutnya): kombinasi itu PERSIS
yang menyebabkan regresi nyata di v2.14.0 (CTA Home jadi kotak pucat/
glitch di banyak GPU/skin), yang di-fix v2.14.1 dengan MELEPAS shadow
total dari CTA. Supaya elevasi nyata bisa dihidupkan lagi TANPA mengulang
bug yang sama, dipakai pola baru konsisten di semua 4 tempat: `Surface`
(atau `Modifier.shadow` untuk thumb switch yang bukan Surface) dengan
`color`/`.background()` dasar SOLID (bukan `Color.Transparent` + brush
langsung di node yang sama) -- shadow RenderNode menggambar bayangan dari
warna solid yang aman, lalu gradient/tint (Teal ambient, Stamp->Amber,
radial highlight) ditumpuk sebagai layer Box TERPISAH di atas/di dalam,
tidak pernah di-chain ke node yang sama dengan shadow. Kalau di masa
depan mau tambah elevasi ke komponen ber-gradient lain, WAJIB pakai pola
solid-base-lalu-overlay ini, jangan `Modifier.shadow` langsung ke Brush.

GroupedListRow icon box: v3.0.1 sebelumnya SENGAJA melarang shadow
berwarna tint di situ (dianggap "glow permanen", pelanggaran bab 18).
v4.0.0 ini MENGGANTI keputusan itu -- ikon sekarang dapat shadow NETRAL
kecil (`ElevationIcon = 3.dp`, warna default Material3, bukan `spotColor`
tint) supaya secara semantik ini "bayangan terangkat", bukan "cahaya
menyala" -- jadi tidak melanggar semangat bab 18 (glow BERWARNA dilarang,
shadow netral kecil untuk depth diizinkan berdasarkan instruksi baru).

1 file diubah: `app/build.gradle.kts` (versi). Preflight lolos bersih.
**BELUM PERNAH lewat `./gradlew` asli / device asli** -- konsisten seluruh
riwayat project. User WAJIB verifikasi visual di HP asli sebelum anggap
selesai, terutama: (1) CTA "Scan Sekarang" idle & saat ditekan (paling
berisiko -- riwayat regresi persis di titik ini), (2) VaultCard & icon
GroupedListRow tidak flicker/pucat saat scroll, (3) kontras teks di atas
TealAccent baru masih terbaca jelas.

versionCode 65->66, versionName 2.19.3->2.20.0.

## v2.19.3 -- Fix bug NYATA: file/apk bernama diawali "PromptVault" tidak pernah terdeteksi scan (2026-08-13)
User laporkan: file/apk dengan nama persis "PromptVault" (atau apa pun yang
DIAWALI teks itu, mis. "PromptVault.apk", "PromptVault-release.apk") yang
ditaruh langsung di Downloads tidak pernah ke-detect sebagai kandidat scan,
walau rule/pattern-nya cocok.

**Root cause**: `listCandidateFiles()` mengecualikan folder output app
sendiri lewat `f.absolutePath.startsWith(vaultRootDir.absolutePath)` --
ini STRING-PREFIX match, bukan path-containment check. Karena
`vaultRootDir.absolutePath` = ".../Downloads/PromptVault" (tanpa separator
akhir), path file SIBLING seperti ".../Downloads/PromptVault.apk" juga
lolos `startsWith(...)` itu (string "PromptVault.apk" diawali "PromptVault"),
padahal file itu bukan isi folder "PromptVault", cuma kebetulan namanya
diawali teks yang sama -- jadi ikut ter-exclude dari kandidat scan tanpa
alasan valid. Bug kelas sama juga ditemukan di `cleanupGhostMediaStoreEntries()`
(query `LIKE '<path>%'` punya masalah prefix-match identik).

**Fix**: tambah `File.separator` di akhir prefix pembanding pada kedua
tempat (`listCandidateFiles` & `cleanupGhostMediaStoreEntries`), supaya
hanya path yang BENAR-BENAR di dalam folder "PromptVault/" yang cocok,
bukan sekadar diawali string yang sama.

1 file diubah: `util/FileSorter.kt` (2 titik fix + doc), `app/build.gradle.kts`
(versi). `scripts/preflight_check.sh` lolos bersih. **BELUM PERNAH lewat
`./gradlew` asli** -- konsisten seluruh riwayat project (sandbox tanpa
Gradle/device asli). User DIMINTA konfirmasi di HP asli: taruh file/apk
bernama diawali "PromptVault" di Downloads, buat rule yang cocok, scan,
pastikan file itu SEKARANG terdeteksi & terpindah normal.

versionCode 64->65, versionName 2.19.2->2.19.3.

## v2.19.2 -- Fix bug NYATA: folder "PromptVault" terduplikat (1)/(2)/(3) di tujuan SAF (2026-08-13)
User laporkan screenshot: folder tujuan kustom berisi 4 folder --
"PromptVault", "PromptVault (1)", "PromptVault (2)", "PromptVault (3)",
masing-masing cuma 1 item, tanggal sama.

**Root cause**: `findOrCreateChildDirSaf(destinationRoot, "PromptVault")`
dipanggil terpisah PER-FILE, di dalam tiap coroutine paralel
(`scanAndSortToDestination` memproses file kandidat lewat `async` +
`Semaphore(SCAN_CONCURRENCY=6)`, arsitektur performa sejak v2.4.0).
`DocumentFile.createDirectory()` tidak atomik/idempoten seperti
`File.mkdirs()` -- 2+ coroutine bisa sama-sama melihat "folder belum ada"
sebelum salah satu selesai membuatnya, lalu keduanya createDirectory() ->
provider SAF tidak menolak, malah auto-suffix nama biar tetap unik -> N
folder terpisah, masing-masing cuma kebagian file dari coroutine yang
menciptakannya duluan. Classic TOCTOU race; `scanMutex` yang sudah ada TIDAK
mencegah ini (cuma menyerialkan antar scan, bukan antar file dalam satu
scan yang sengaja diparalelkan).

**Fix struktural**: folder tujuan SAF (root "PromptVault" + subfolder tiap
rule aktif) sekarang di-resolve SEKALI, SERIAL, lewat fungsi baru
`resolveSafRuleDestinations()` -- dipanggil SEBELUM `async{}` mana pun
dimulai. Hasilnya (`Map<namaFolderRule, DocumentFile?>`) dibagikan ke semua
coroutine paralel sebagai data baca-saja, jadi tidak ada lagi 2 coroutine
yang bisa balapan menciptakan folder yang sama. `moveFileToSafDestination()`
& `processCandidate()` disesuaikan untuk menerima folder yang sudah
di-resolve, bukan resolve sendiri lagi.

**Folder duplikat yang SUDAH ada** (dari sebelum fix ini) TIDAK dibereskan
otomatis -- perlu digabung manual oleh user lewat file manager kalau mau
rapi. Scan berikutnya SUDAH konsisten pakai satu folder "PromptVault" saja.

2 file diubah: `util/FileSorter.kt`, `app/build.gradle.kts`.
`scripts/preflight_check.sh` lolos bersih. **BELUM PERNAH lewat `./gradlew`
asli.**

versionCode 63->64, versionName 2.19.1->2.19.2.

## v2.19.1 -- Debug+polish SAF: overwrite ke folder kustom sekarang verifikasi delete() (2026-08-13)
User minta audit umum "debugging+polish feature SAF". Audit manual menyeluruh
seluruh kode SAF (bukan cuma cek dead-code) -- arsitektur v2.19.0 terverifikasi
konsisten, semua wiring (dispatcher undo, resolusi 3-state, opt-in Compose
experimental) sudah benar. 1 bug nyata ditemukan & diperbaiki:

**Bug**: `FileSorter.moveFileToSafDestination()` -- strategi konflik OVERWRITE
memanggil `existingAtTarget.delete()` tanpa cek hasil, lalu langsung lanjut
`createFile()` dengan nama sama seolah pasti berhasil dihapus. Provider SAF
kalau delete diam-diam gagal, umumnya auto-suffix nama file baru ("target
(1).ext") alih-alih menimpa -- hasil: file lama tetap ada, file baru bernama
beda dari yang diminta rule, tapi app melaporkan sukses seolah overwrite
normal.

**Fix**: hasil `delete()` sekarang dicek eksplisit. Gagal -> log
`LogLevel.ERROR` + return `MoveOutcome.FAILED`, tidak lagi diam-diam lanjut.
Pola ini konsisten dengan pelajaran permanen project ini (Insiden #6): jangan
percaya method boolean provider DocumentFile tanpa verifikasi.

**Di luar scope (dicatat, tidak dieksekusi)**: `moveFile()` (jalur lokal
java.io.File, strategi OVERWRITE) punya gap identik (`destFile.delete()`
juga tidak diverifikasi) -- tidak diubah karena user secara eksplisit minta
scope "feature SAF", dan risiko delete() gagal jauh lebih rendah di
filesystem lokal milik app sendiri dibanding provider SAF pihak ketiga.

1 file logika (`util/FileSorter.kt`) + `app/build.gradle.kts` (versi).
`scripts/preflight_check.sh` lolos bersih. **BELUM PERNAH lewat `./gradlew`
asli** -- sandbox sesi ini tetap tanpa akses Gradle/device asli (Insiden #7
syarat (a) belum terpenuhi).

versionCode 62->63, versionName 2.19.0->2.19.1.

## v2.19.0 -- SAF direstrukturisasi total: folder kustom = TUJUAN, bukan sumber scan (2026-08-13)
User upload `SAF_FINAL_VERDICT_FIX.txt` -- spec/verdict yang menyimpulkan
seluruh siklus SAF v2.17.0-v2.18.1 salah menafsirkan requirement dari awal.
Instruksi user singkat "folder scan file untuk dipindahkan tetap hardcode
'download'" menegaskan bagian yang benar dari spec itu: sumber scan harus
SELALU Downloads.

**Root cause (dari dokumen)**: SAF = mekanisme akses ke folder TUJUAN
penyimpanan kustom yang dipilih user, BUKAN sumber scan alternatif. Implementasi
sejak v2.17.0 memperlakukan folder kustom sebagai scanner mandiri (dipindai
SENDIRI, terpisah dari Downloads) -- arsitektur yang salah dari awal, bukan
bug detail yang bisa ditambal.

**Restrukturisasi** (`util/FileSorter.kt`):
- `scanAndSort()` sekarang SATU sumber scan ([listCandidateFiles], Downloads,
  tidak pernah berubah sejak awal project) + SATU cabang TUJUAN (lokal atau
  SAF), bukan lagi dua scanner independen.
- **Dihapus total**: `scanAndSortSafLocked()`, `listCandidateFilesSaf()`,
  `processCandidateSaf()`, `isLikelyStillWritingSaf()` -- semua fungsi yang
  memperlakukan DocumentFile/folder kustom sebagai SUMBER kandidat scan.
- **Rename** (bukan kosmetik -- akar masalah adalah konsep yang ambigu):
  `SafRootResolution` -> `SafDestinationResolution`, `resolveSafRoot()` ->
  `resolveSafDestinationRoot()`. Perilaku/validasi (persistable permission,
  cek exists/isDirectory, SecurityException) TIDAK berubah -- itu sudah benar.
- **Baru**: `moveFileToSafDestination(file: File, ...)` menggantikan
  `moveFileSaf(doc: DocumentFile, ...)` -- sumbernya sekarang SELALU
  `java.io.File` lokal, disalin ke `DocumentFile` tujuan lewat
  ContentResolver (bukan lagi DocumentFile-ke-DocumentFile).
- `processCandidate()` dapat parameter baru `destinationRoot: DocumentFile?`
  -- satu-satunya titik cabang tersisa antara tujuan lokal ([moveFile]) vs
  tujuan SAF ([moveFileToSafDestination]); sumbernya (`file: File`, dari
  Downloads) sama untuk keduanya.
- `previewPatternMatches()` disederhanakan drastis: TIDAK ADA LAGI cabang SAF
  sama sekali (sumber scan cuma satu sekarang). Ini efek samping positif --
  kelas bug "preview vs scan lihat folder beda" (v2.18.1) jadi STRUKTURAL
  tidak mungkin terulang, bukan cuma disinkronkan ulang.

**Kompatibilitas mundur untuk undo**: `MoveHistoryEntry` yang SUDAH tersimpan
di Room dari SEBELUM update ini (format lama: sumber & tujuan sama-sama URI
`content://`) tetap bisa di-undo lewat `undoSaf()` (logika lama, tidak
disentuh sama sekali). Entri BARU (tujuan `content://`, sumber path lokal
biasa) lewat `undoSafDestination()` yang baru ditambahkan. `undo()` membedakan
dua format ini lewat `originalParentUri` (bukan skema/kolom DB baru).

**UI**: `SettingsScreen.kt` -- "Folder Kustom (Opsional)" jadi "Folder Tujuan
Kustom (Opsional)", teks deskripsi diubah dari "Pindai folder pilihanmu
sendiri..." (bahasa SUMBER, salah) jadi "File tetap DIPINDAI dari Downloads
seperti biasa. Folder ini cuma menentukan KE MANA hasil sortir disimpan..."
(bahasa TUJUAN, benar). Doc comment `SettingsRepository.safTreeUriFlow` &
`MainViewModel.safTreeUri`/`clearSafTreeUri` diperjelas senada. `MainActivity.kt`
(picker wiring, `ActivityResultContracts.OpenDocumentTree()`) TIDAK disentuh
sama sekali -- murni memilih URI, tidak peduli peran sumber/tujuan.

`scripts/preflight_check.sh` lolos bersih (2 iterasi -- iterasi 1 sempat
menyisakan import `CancellationException` tak terpakai setelah
`processCandidateSaf` dihapus, karena fungsi itu dulu satu-satunya pemakai
guard cancellation-safe; dibersihkan di iterasi 2 setelah dikonfirmasi jalur
baru tidak butuh guard yang sama -- lihat komentar `isTempOrPartialName` &
komentar di atas `processCandidate`). **BELUM PERNAH lewat `./gradlew` asli**
-- CI run berikutnya WAJIB dicek, konsisten dengan seluruh riwayat SAF
sebelumnya (Insiden #7 syarat (c): blind tapi disiplin, bukan berarti prosedur
gagal kalau CI merah -- lanjutkan dengan fix normal).

versionCode 61->62, versionName 2.18.1->2.19.0.

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
