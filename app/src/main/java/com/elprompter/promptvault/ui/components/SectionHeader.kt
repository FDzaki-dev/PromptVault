package com.elprompter.promptvault.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * [Refactor rapi-rapi, 2026-08-27, TANPA ubah behavior -- lanjutan ToggleRow]
 * Ekstraksi murni dari pola `Text(title, titleMedium)` diikuti
 * `Text(desc, bodySmall)` yang sebelumnya diketik ulang identik di 8 titik
 * terpisah (`SettingsScreen.kt` x6: interval/konflik/konkurensi/SAF/backup/
 * import, `DiagnosticsScreen.kt` x2: downloads/crashlog). Bukan komponen baru
 * secara visual -- hasil render 100% identik dgn kode inline sebelumnya di
 * seluruh pemanggil, cuma dipindah ke 1 tempat supaya tidak diketik ulang.
 *
 * PENTING soal `spacing`: title & desc di kode asli BUKAN dibungkus Column
 * sendiri -- keduanya langsung jadi child dari Column pemanggil, jadi jarak
 * title-desc sebelumnya ikut `verticalArrangement` Column pemanggil (BEDA-BEDA
 * tiap tempat: 16.dp di 3 section teratas Settings yang langsung anak Column
 * terluar, 8.dp di section dalam `VaultCard`, 0.dp -- default Column tanpa
 * `verticalArrangement` -- di kartu Downloads Diagnostics). Supaya hasil
 * akhir IDENTIK matematis, `spacing` WAJIB dipass eksplisit sesuai nilai asli
 * tiap pemanggil (diverifikasi satu-satu, lihat tabel di PROJECT_STATE.md),
 * BUKAN diseragamkan/ditebak -- default 8.dp cuma dipilih krn itu nilai yang
 * paling sering muncul (4 dari 8 pemanggil), bukan berarti "aman diabaikan".
 *
 * SENGAJA TIDAK dipakai utk section title yang punya `Icon` di sampingnya
 * (mis. "Cek Pembaruan" di `SettingsScreen.kt`, title-nya di dalam `Row`
 * bareng `Icon`) -- pola visual beda, reuse di situ butuh param tambahan yg
 * di luar scope batch minimal ini.
 */
@Composable
fun SectionHeader(
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    spacing: Dp = 8.dp
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(spacing)) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Text(description, style = MaterialTheme.typography.bodySmall)
    }
}
