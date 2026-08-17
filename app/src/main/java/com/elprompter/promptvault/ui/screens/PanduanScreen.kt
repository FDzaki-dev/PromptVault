package com.elprompter.promptvault.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.elprompter.promptvault.ui.components.VaultCard
import com.elprompter.promptvault.ui.components.VaultTopBar
import com.elprompter.promptvault.ui.components.WarningBanner

/**
 * [Fitur baru, batch "Panduan User Baru" 2026-08-17]
 *
 * Root cause yang ditutup: satu-satunya penjelasan mekanisme app sebelumnya
 * adalah [OnboardingScreen] yang HANYA tampil SEKALI SEUMUR HIDUP (gated
 * `onboardingDone` di DataStore, lihat MainActivity.kt) -- setelah itu, user
 * baru yang lupa detail (atau meng-uninstall+install ulang di HP yang beda)
 * tidak punya jalan balik selain baca CHANGELOG.md/PROJECT_STATE.md di GitHub
 * (dokumen teknis untuk sesi Claude, BUKAN untuk end-user). Layar ini adalah
 * versi REFERENSI (bukan wizard step-per-step) dari materi onboarding yang
 * SAMA, plus beberapa poin troubleshooting cepat -- bisa dibuka berkali-kali
 * lewat menu Home ATAU dari kartu di Pengaturan, kapan saja, tanpa reset
 * status onboarding.
 *
 * Konten SENGAJA dijaga konsisten dengan [OnboardingScreen] & penjelasan
 * inline di [SettingsScreen] (WarningBanner root-folder yang sama persis
 * dipakai ulang di sini) -- supaya tidak ada 2 sumber kebenaran yang bisa
 * saling kontradiksi soal perilaku app yang sama.
 */
@Composable
fun PanduanScreen(onBack: () -> Unit) {
    val colors = MaterialTheme.colorScheme

    Scaffold(
        topBar = { VaultTopBar(title = "Panduan Penggunaan", onBack = onBack) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                "Ringkasan cara kerja PromptVault. Bisa dibuka lagi kapan saja dari sini -- " +
                    "tidak perlu mengulang onboarding.",
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant
            )

            PanduanSection(
                title = "1. Cara kerja dasar",
                body = "PromptVault memindai folder Downloads, lalu mencocokkan tiap file ke " +
                    "\"rule\" yang kamu buat (pattern nama file + folder tujuan). File yang cocok " +
                    "langsung dipindah -- bukan disalin, file aslinya hilang dari Downloads " +
                    "(tapi bisa di-undo, lihat poin 6). Kalau beberapa rule sama-sama bisa cocok " +
                    "ke satu file, rule PALING ATAS di daftar \"Kelola Rule\" yang menang. Urutan " +
                    "rule bisa diubah lewat tombol naik/turun di layar itu."
            )

            PanduanSection(
                title = "2. Checklist setup pertama kali",
                body = "(1) Beri izin penyimpanan saat diminta. (2) Buka \"Kelola Rule\" -> " +
                    "\"Tambah Rule\", isi pattern (mis. *.pdf) dan nama folder tujuan -- ada live " +
                    "preview file mana saja di Downloads yang cocok SEBELUM disimpan. (3) Tekan " +
                    "\"Scan Sekarang\" di Home untuk uji coba manual pertama kali, atau tunggu " +
                    "auto-sort jalan sendiri sesuai interval di Pengaturan."
            )

            PanduanSection(
                title = "3. Ke mana file disortir?",
                body = "Default: Downloads/PromptVault/<nama rule>/, dibuat otomatis, tidak perlu " +
                    "setup apapun. Dua opsi lanjutan di Pengaturan kalau kamu butuh tujuan lain: " +
                    "\"Folder Tujuan Kustom\" (SAF, termasuk kartu SD -- subfolder \"PromptVault\" " +
                    "dibuat otomatis juga di dalamnya) atau \"Mode Shizuku\" (butuh aplikasi Shizuku " +
                    "terpasang & jalan)."
            )
            WarningBanner(
                "Khusus Mode Shizuku: folder ROOT tujuan HARUS SUDAH ADA secara fisik SEBELUM " +
                    "diisi di Pengaturan -- buat dulu sendiri lewat file manager. PromptVault TIDAK " +
                    "PERNAH membuat folder root lewat Shizuku sendiri. Kalau folder belum ada, scan " +
                    "akan GAGAL dengan pesan error yang jelas, bukan membuatkannya diam-diam. (Folder " +
                    "Tujuan Kustom via SAF di atas beda -- root-nya DIBUAT OTOMATIS.)"
            )

            PanduanSection(
                title = "4. Kalau nama file sudah ada di tujuan",
                body = "Diatur di Pengaturan -> \"Kalau Nama File Sudah Ada di Tujuan\", 3 pilihan: " +
                    "\"Ganti nama otomatis\" (default, paling aman -- file baru dapat nama lain, " +
                    "tidak ada yang hilang), \"Lewati\" (file baru tidak dipindah, file lama di " +
                    "tujuan tidak disentuh), \"Timpa\" (file lama di tujuan DIHAPUS PERMANEN dan " +
                    "TIDAK BISA di-undo -- pilih ini hanya kalau kamu yakin)."
            )

            PanduanSection(
                title = "5. Auto-sort & notifikasi latar belakang",
                body = "Setelah ada rule aktif, app memindai Downloads sendiri di latar belakang " +
                    "sesuai interval di Pengaturan (minimal 15 menit, batas sistem Android, tidak " +
                    "bisa lebih cepat). Notifikasi kecil muncul saat auto-sort sedang berjalan. " +
                    "Kalau ingin tahu kapan terakhir auto-sort jalan atau kenapa gagal, buka " +
                    "\"Diagnostik\" dari Home."
            )

            PanduanSection(
                title = "6. Salah pindah? Undo lewat Riwayat Aktivitas",
                body = "Buka \"Riwayat Aktivitas & Undo\" dari Home, pindah ke tab Undo. Tekan satu " +
                    "baris untuk undo satu file, atau tekan-lama satu baris lalu sapukan jari ke " +
                    "baris lain untuk memilih banyak file sekaligus sebelum undo massal " +
                    "(checkbox manual & tap biasa tetap tersedia sebagai alternatif kalau sapuan " +
                    "jari kurang nyaman di HP kamu). Undo TIDAK tersedia untuk file yang " +
                    "ditimpa lewat strategi \"Timpa\" di poin 4 -- file lamanya sudah terhapus " +
                    "permanen sebelum undo sempat dijalankan."
            )

            PanduanSection(
                title = "7. File tidak masuk ke folder yang diharapkan?",
                body = "Buka \"Detail File Dilewati\" (muncul di Home setelah scan kalau ada file " +
                    "yang dilewati) untuk lihat alasan spesifik per file -- tidak cocok pattern, " +
                    "kena pattern exclude, di luar batas ukuran, diduga masih ditulis (file baru " +
                    "banget), atau konflik nama dengan strategi \"Lewati\". Untuk cek pattern rule " +
                    "kamu terhadap nama file asli di Downloads, buka \"Diagnostik\" dari Home."
            )

            Text(
                "Panduan ini bisa dibuka lagi kapan saja lewat menu Home atau kartu di Pengaturan. " +
                    "Konten mengikuti versi app yang sedang kamu pakai.",
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun PanduanSection(title: String, body: String) {
    VaultCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
            Text(body, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
