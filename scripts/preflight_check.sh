#!/usr/bin/env bash
# ============================================================================
# PromptVault -- Preflight Check
# ============================================================================
# WAJIB dijalankan (dari root repo) SEBELUM mem-package ZIP apapun untuk
# dikirim ke user, di SESI CLAUDE MANAPUN dan KAPANPUN.
#
# Kenapa ini ada: Claude tidak punya Android SDK/Gradle di lingkungan kerjanya
# (sandbox tanpa akses jaringan), jadi kompilasi Kotlin asli TIDAK BISA
# diverifikasi lokal. Satu-satunya jaring pengaman sebelum kode sampai ke CI
# (yang biayanya waktu tunggu + jatah menit Actions + bolak-balik upload log)
# adalah audit statis ini. Setiap baris di sini mewakili bug NYATA yang
# pernah lolos dan bikin build gagal -- lihat CHANGELOG.md untuk versi mana.
#
# Cara pakai: bash scripts/preflight_check.sh
# Exit code 0 = aman untuk di-zip. Exit code 1 = ADA yang harus dibenerin dulu.
# ============================================================================

set -uo pipefail
cd "$(dirname "$0")/.."

FAIL=0
KT_DIR="app/src/main/java/com/elprompter/promptvault"

fail() { echo "❌ $1"; FAIL=1; }
ok()   { echo "✅ $1"; }

echo "== 1. Keseimbangan kurung di semua file .kt =="
MISMATCH=$(find app -name "*.kt" | xargs -I{} python3 -c "
content = open('{}').read()
b = content.count('{') - content.count('}')
p = content.count('(') - content.count(')')
if b != 0 or p != 0: print('{}', b, p)
" 2>/dev/null)
if [ -n "$MISMATCH" ]; then fail "Kurung tidak seimbang:"; echo "$MISMATCH"; else ok "Semua file seimbang"; fi

echo ""
echo "== 2. Import member-scope yang salah (weight/align/matchParentSize) =="
BAD_IMPORT=$(grep -rn "^import androidx.compose.foundation.layout.weight$\|^import androidx.compose.foundation.layout.align$\|^import androidx.compose.foundation.layout.matchParentSize$" app/src/main/java/ 2>/dev/null)
if [ -n "$BAD_IMPORT" ]; then fail "Import berbahaya ditemukan:"; echo "$BAD_IMPORT"; else ok "Aman"; fi

echo ""
echo "== 3. Delegate 'by' tanpa getValue/setValue =="
DELEGATE_ISSUE=0
for f in $(grep -rl "by remember\|by mutableStateOf\|collectAsState\|by .*Flow\|animateColorAsState\|animateFloatAsState\|collectIsPressedAsState" "$KT_DIR" 2>/dev/null); do
  if ! grep -q "import androidx.compose.runtime.getValue" "$f"; then
    fail "MISSING getValue: $f"; DELEGATE_ISSUE=1
  fi
  if grep -q "var .* by " "$f" && ! grep -q "import androidx.compose.runtime.setValue" "$f"; then
    fail "MISSING setValue: $f"; DELEGATE_ISSUE=1
  fi
done
[ "$DELEGATE_ISSUE" -eq 0 ] && ok "Semua delegate lengkap importnya"

echo ""
echo "== 4. Import duplikat per file =="
DUP_ISSUE=0
for f in $(find app/src -name "*.kt"); do
  dups=$(grep "^import " "$f" | sort | uniq -d)
  if [ -n "$dups" ]; then fail "Duplikat di $f: $dups"; DUP_ISSUE=1; fi
done
[ "$DUP_ISSUE" -eq 0 ] && ok "Tidak ada import duplikat"

echo ""
echo "== 5. LazyColumn di dalam verticalScroll tanpa heightIn =="
SCROLL_ISSUE=0
for f in $(grep -rl "verticalScroll" "$KT_DIR" 2>/dev/null); do
  if grep -q "LazyColumn\|LazyRow" "$f" && ! grep -q "heightIn" "$f"; then
    fail "PERIKSA MANUAL (LazyColumn dlm verticalScroll tanpa heightIn): $f"; SCROLL_ISSUE=1
  fi
done
[ "$SCROLL_ISSUE" -eq 0 ] && ok "Aman"

echo ""
echo "== 6. Warna literal bocor keluar dari Theme.kt (harus lewat MaterialTheme.colorScheme) =="
COLOR_LEAK=$(grep -rn "= Pine\b\|= Stamp\b\|= Kraft\b\|= CardPaper\b\|= Ink\b\|= InkFaint\b\|= HairlineInk\b\|= Amber\b" "$KT_DIR/ui/" app/src/main/java/com/elprompter/promptvault/MainActivity.kt 2>/dev/null | grep -v "ui/theme/Theme.kt")
if [ -n "$COLOR_LEAK" ]; then fail "Warna literal bocor:"; echo "$COLOR_LEAK"; else ok "Bersih, semua theme-aware"; fi

echo ""
echo "== 7. Fungsi lokal (nested) -- REVIEW MANUAL WAJIB =="
echo "   (pastikan yang manggil FilterChipDefaults/ButtonDefaults/MaterialTheme/dll punya @Composable di baris sebelumnya)"
grep -rn "^    fun \|^        fun " "$KT_DIR/ui/" 2>/dev/null || echo "   (tidak ada fungsi lokal ditemukan)"

echo ""
echo "== 8. Validitas YAML workflow CI =="
if python3 -c "import yaml; yaml.safe_load(open('.github/workflows/build.yml'))" 2>/dev/null; then
  ok "YAML valid"
else
  fail "YAML build.yml tidak valid / python3-yaml tidak tersedia (cek manual)"
fi

echo ""
echo "== 9. CI wajib publish APK ke GitHub Release (bukan cuma Actions Artifact) =="
# [ditambahkan 2026-08-04] Sebelum ini, workflow cuma pakai upload-artifact
# (login-only, expired 90 hari, tidak muncul di sidebar Releases repo) --
# lolos dari preflight lama karena tidak ada kategori yang mengecek ini.
if grep -q "softprops/action-gh-release\|actions/create-release\|gh release " .github/workflows/build.yml 2>/dev/null; then
  ok "Ada step publish ke GitHub Release"
else
  fail "TIDAK ada step publish ke GitHub Release di build.yml -- APK cuma jadi Actions Artifact, melanggar GitHub Release Rule"
fi

echo ""
echo "== 10. Well-formedness XML (semua res/*.xml + AndroidManifest.xml) =="
# [ditambahkan 2026-08-05, insiden v2.6.0] sebelumnya tidak ada kategori yang
# validasi XML well-formed -- lolos preflight lama, baru ketahuan pas CI gagal
# gara-gara "--" di dalam komentar <!-- -->.
XML_ISSUE=0
for f in $(find app/src/main -name "*.xml"); do
  if ! python3 -c "import xml.dom.minidom as m; m.parse('$f')" 2>/tmp/xmlerr; then
    fail "XML tidak valid: $f -- $(cat /tmp/xmlerr | tail -1)"; XML_ISSUE=1
  fi
done
[ "$XML_ISSUE" -eq 0 ] && ok "Semua XML well-formed"

echo ""
echo "== 11. decodeFromString<T> reified tanpa import eksplisit =="
# [ditambahkan 2026-08-13, fix RuleRepository.kt v2.20.3] json.decodeFromString<T>()
# adalah extension function generic reified -- perlu
# 'import kotlinx.serialization.decodeFromString' eksplisit per file, TIDAK
# otomatis ikut dari 'import kotlinx.serialization.json.Json'. Lolos preflight
# lama karena tidak ada kategori yang cek pasangan pemakaian/import ini.
DECODE_ISSUE=0
for f in $(grep -rl "\.decodeFromString<" "$KT_DIR" 2>/dev/null); do
  if ! grep -q "^import kotlinx.serialization.decodeFromString$" "$f"; then
    fail "MISSING import kotlinx.serialization.decodeFromString: $f"; DECODE_ISSUE=1
  fi
done
[ "$DECODE_ISSUE" -eq 0 ] && ok "Semua decodeFromString<T> punya import lengkap"


echo "== 12. [OBSOLETE sejak v7.0.0, dipertahankan sbg no-op utk histori] baseColor gradient check =="
# [ditambahkan 2026-08-15, fix v2.24.3, Insiden #9; DIPENSIUNKAN v7.0.0]
# Category ini dulu mengecek parameter `baseColor` (teknik shadow-caster
# Neumorphism lama, `Neumorphic.kt`) yang harus menyamar dgn latar gradient.
# v7.0.0 menghapus TOTAL `Neumorphic.kt` & parameter `baseColor` (diganti
# `GlassPanel.kt`, shadow standar Compose yg valid di atas latar apapun) --
# jadi heuristik lama SUDAH TIDAK RELEVAN lagi (tidak ada lagi baseColor utk
# dicek). Dibiarkan sbg dokumentasi historis, no-op permanen. Lihat
# CHANGELOG/PROJECT_STATE v2.24.4 utk detail insiden aslinya.
ok "Dilewati (parameter baseColor sudah tidak ada sejak v7.0.0)"

echo ""
echo "== 13. KDoc/block comment ('/** ... */') tertutup PREMATUR di tengah isi =="
# [ditambahkan 2026-08-15, fix v7.0.0 build failure] Root cause NYATA: KDoc
# di `TactileTokens.kt` berisi teks "Neu*/Glass*" -- substring "*/" di
# TENGAH kalimat menutup block comment lebih awal dari yang dimaksud, sisa
# isi KDoc (s.d. `*/` yang SEBENARNYA di baris lain) ke-parse sbg kode
# Kotlin sungguhan -> "Expecting a top level declaration" berantai (CI log
# `build-failure-log-v7_0_0.zip`). Kelas bug ini TIDAK kelihatan dari
# hitungan kurung `{`/`}`/`(`/`)` biasa (kategori #1) -- perlu pengecekan
# khusus: cari "*/" yang diikuti LANGSUNG karakter bukan-spasi di baris yang
# sama (comment penutup ASLI selalu diikuti akhir baris/spasi, bukan lanjut
# teks/kode).
BROKEN_COMMENT=$(grep -rnP '\*/\S' "$KT_DIR" 2>/dev/null)
if [ -n "$BROKEN_COMMENT" ]; then
  fail "Block comment tertutup prematur (ada '*/' di tengah kalimat KDoc):"
  echo "$BROKEN_COMMENT"
else
  ok "Tidak ada '*/' yang menutup comment secara tidak sengaja"
fi

echo ""
if [ "$FAIL" -eq 0 ]; then
  echo "🟢 SEMUA AMAN -- boleh lanjut package ZIP."
else
  echo "🔴 ADA YANG HARUS DIPERBAIKI DULU sebelum package ZIP. Lihat ❌ di atas."
fi
exit $FAIL
