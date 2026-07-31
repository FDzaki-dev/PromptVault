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
if [ "$FAIL" -eq 0 ]; then
  echo "🟢 SEMUA AMAN -- boleh lanjut package ZIP."
else
  echo "🔴 ADA YANG HARUS DIPERBAIKI DULU sebelum package ZIP. Lihat ❌ di atas."
fi
exit $FAIL
