package com.elprompter.promptvault.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * [Fitur baru 2026-08-17, permintaan eksplisit user] Banner peringatan
 * bersama -- dipakai di kartu "Folder Tujuan Kustom (SAF)" DAN kartu "Mode
 * Shizuku" (SettingsScreen) untuk pesan yang SAMA PENTINGNYA di kedua
 * tempat: **aplikasi ini TIDAK PERNAH membuat folder root tujuan kustom
 * secara otomatis -- user WAJIB membuatnya sendiri lebih dulu lewat file
 * manager**. Dibuat komponen terpisah (bukan Text() inline di 2 tempat)
 * supaya warna/ikon/urgensi visualnya konsisten & gampang dipakai ulang
 * kalau ada jalur tujuan kustom baru lagi di masa depan.
 *
 * Warna SENGAJA pakai `colors.error` (bukan `colors.tertiary`/warning
 * biasa) -- ini bukan sekadar info, tapi konsekuensi nyata yang bisa bikin
 * scan gagal total kalau diabaikan (lihat pesan error eksplisit di
 * FileSorter.resolveSafDestinationRoot / scanAndSortViaShizuku).
 *
 * (v8.29.0 -- sempurnakan UI/UX) Sebelumnya `Modifier.background()` polos,
 * BYPASS `TactileSurface` -- 1 SATU-SATUNYA permukaan berisi konten yang
 * TIDAK ikut gaya tema aktif (Glass/Neumorphism/Material3 Murni), jadi
 * tetap flat & tidak konsisten sama seluruh kartu lain di sekitarnya
 * begitu user ganti tema. Diganti `TactileSurface` (fill `errorContainer`,
 * sama urgensi merah, tapi sekarang ikut treatment tema aktif) -- teks/ikon
 * TETAP `colors.error` (bukan `onErrorContainer`, mempertahankan urgensi
 * visual merah asli). WCAG diverifikasi ulang utk pasangan BARU ini
 * (`error` vs `errorContainer`, beda dari pasangan `onErrorContainer` vs
 * `errorContainer` yang sudah ada di Color.kt): 4.75:1 (AA, formula W3C,
 * ambang 4.5:1 -- lulus, margin tipis tapi valid).
 */
@Composable
fun WarningBanner(message: String, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    TactileSurface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.small,
        color = colors.errorContainer
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                Icons.Filled.WarningAmber,
                contentDescription = null,
                tint = colors.error,
                modifier = Modifier.padding(end = 8.dp)
            )
            Text(message, style = MaterialTheme.typography.bodySmall, color = colors.error)
        }
    }
}
