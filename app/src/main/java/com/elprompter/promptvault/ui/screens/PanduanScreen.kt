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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.elprompter.promptvault.R
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
        topBar = { VaultTopBar(title = stringResource(R.string.pandu_title), onBack = onBack) }
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
                stringResource(R.string.pandu_intro),
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant
            )

            PanduanSection(
                title = stringResource(R.string.pandu_section1_title),
                body = stringResource(R.string.pandu_section1_body)
            )

            PanduanSection(
                title = stringResource(R.string.pandu_section2_title),
                body = stringResource(R.string.pandu_section2_body)
            )

            PanduanSection(
                title = stringResource(R.string.pandu_section3_title),
                body = stringResource(R.string.pandu_section3_body)
            )
            WarningBanner(stringResource(R.string.pandu_warning_shizuku))

            PanduanSection(
                title = stringResource(R.string.pandu_section4_title),
                body = stringResource(R.string.pandu_section4_body)
            )

            PanduanSection(
                title = stringResource(R.string.pandu_section5_title),
                body = stringResource(R.string.pandu_section5_body)
            )

            PanduanSection(
                title = stringResource(R.string.pandu_section6_title),
                body = stringResource(R.string.pandu_section6_body)
            )

            PanduanSection(
                title = stringResource(R.string.pandu_section7_title),
                body = stringResource(R.string.pandu_section7_body)
            )

            Text(
                stringResource(R.string.pandu_footer),
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
