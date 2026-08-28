package com.elprompter.promptvault.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.elprompter.promptvault.ui.theme.TactileTokens

/**
 * Permukaan kartu utama app. Struktur wrap-content dipertahankan (kartu
 * TIDAK BOLEH merebut sisa tinggi layar, lihat Insiden #3 lama di
 * PROJECT_STATE.md) -- tidak berubah oleh batch ini.
 *
 * v8.0.0 — Glassmorphism -> Material 3 murni: `GlassPanel` diganti
 * `TactileSurface` (lihat `TactileSurface.kt`). `color` sekarang
 * `colorScheme.surfaceContainer` (peran M3 BAKU utk permukaan kartu
 * "naik" 1 tingkat dari root), menggantikan token literal `GlassSurface`.
 *
 * v8.30.0 — "Stacked Cards Effect" diaktifkan di sini (`stackedCards =
 * true`), permintaan eksplisit user, khusus tema Neumorphism (opt-in di
 * `TactileSurface`, 0 dampak ke gaya Glass/Material3 Murni atau komponen
 * lain -- lihat javadoc lengkap `NeumorphTokens.kt`). VaultCard dipilih
 * krn kartu PALING besar/dominan di app -- efek tumpukan paling masuk
 * akal & terlihat di sini, bukan di kotak ikon kecil/kontrol.
 *
 * v8.36.1 — Diganti ke `stackedCardsTopLeft = true` (varian kiri-atas/
 * 3-lapis, sebelumnya HANYA dipasang manual di kartu manifest Home lewat
 * `TactileSurface` langsung -- lihat log batch sebelumnya). User ditanya
 * eksplisit lewat pilihan (bukan diasumsikan sepihak) krn efek baru ini
 * butuh inset 28dp/kartu yang bakal melebarkan jarak antar-item di 2
 * `LazyColumn` rapat (`RuleListScreen` via `RuleCard`, `ActivityLogScreen`
 * langsung, keduanya `spacedBy(4.dp)`) -- user PILIH "semua VaultCard,
 * termasuk 2 list rapat, jarak antar-item bakal melebar" (bukan opsi
 * "kecuali list rapat"), jadi trade-off itu DIKETAHUI & DITERIMA, bukan
 * regresi tak disadari. `stackedCards` (varian lama, kanan-bawah/1-lapis)
 * TIDAK dihapus dari `NeumorphTokens.kt`/`TactileSurface.kt` -- ditinggal
 * sbg kode tak terpakai (0 caller lagi) drpd dihapus paksa, jaga blast
 * radius batch ini seminimal mungkin (murni ganti 1 baris parameter di
 * sini, 0 file lain disentuh selain `HomeScreen.kt` yg baliknya ke
 * `VaultCard()` polos, lihat log batch).
 */
@Composable
fun VaultCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    TactileSurface(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainer,
        elevation = TactileTokens.TactileElevationCard,
        stackedCardsTopLeft = true,
        content = content
    )
}
