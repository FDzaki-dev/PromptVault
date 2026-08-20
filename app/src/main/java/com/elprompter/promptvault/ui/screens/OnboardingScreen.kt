package com.elprompter.promptvault.ui.screens

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.elprompter.promptvault.R

private data class OnboardingStep(val icon: ImageVector, val title: String, val body: String)

/**
 * [Rombak total, batch "Panduan User Baru" 2026-08-17] Sebelumnya cuma 4
 * langkah generik yang berhenti di level "app ini merapikan file + auto-scan
 * jalan sendiri" -- SAMA SEKALI tidak menyebut konsep yang sudah jadi bagian
 * inti app sejak v7.2.0/v7.3.0: folder tujuan kustom (SAF & Shizuku) TIDAK
 * PERNAH dibuat otomatis, 3 strategi konflik nama file, dan Undo lewat
 * Riwayat Aktivitas. Root cause gap "user baru minim informasi": onboarding
 * ini HANYA tampil SEKALI SEUMUR HIDUP (gated `onboardingDone`, lihat
 * MainActivity.kt) -- kalau isinya basi, user baru cuma dapat kesan pertama
 * yang salah dan tidak akan otomatis dikoreksi lagi nanti.
 *
 * Sekarang 7 langkah, urut sesuai ALUR NYATA pemakaian pertama kali (bukan
 * cuma daftar fitur random), dan langkah terakhir SECARA EKSPLISIT
 * memberitahu bahwa seluruh isi ini bisa dibuka ULANG kapan saja lewat
 * "Panduan Penggunaan" di menu Home -- supaya info tidak hilang begitu user
 * menekan "Mulai" sekali dan lupa detailnya.
 *
 * [Roadmap Fase 1.3 batch 6/N, 2026-08-20] Diubah dari `private val` top-level
 * jadi `@Composable private fun` -- `stringResource()` butuh scope composable,
 * tidak bisa dipanggil di inisialisasi `val` top-level biasa. Dipanggil sekali
 * di awal body `OnboardingScreen`, bukan tiap Crossfade re-render (list re-build
 * murah, cuma 7 item, tidak perlu `remember`).
 */
@Composable
private fun onboardingSteps(): List<OnboardingStep> = listOf(
    OnboardingStep(
        Icons.Filled.Archive,
        stringResource(R.string.onboarding_step1_title),
        stringResource(R.string.onboarding_step1_body)
    ),
    OnboardingStep(
        Icons.Filled.FolderOpen,
        stringResource(R.string.onboarding_step2_title),
        stringResource(R.string.onboarding_step2_body)
    ),
    OnboardingStep(
        Icons.Filled.Lock,
        stringResource(R.string.onboarding_step3_title),
        stringResource(R.string.onboarding_step3_body)
    ),
    OnboardingStep(
        Icons.Filled.HelpOutline,
        stringResource(R.string.onboarding_step4_title),
        stringResource(R.string.onboarding_step4_body)
    ),
    OnboardingStep(
        Icons.Filled.CompareArrows,
        stringResource(R.string.onboarding_step5_title),
        stringResource(R.string.onboarding_step5_body)
    ),
    OnboardingStep(
        Icons.Filled.Schedule,
        stringResource(R.string.onboarding_step6_title),
        stringResource(R.string.onboarding_step6_body)
    ),
    OnboardingStep(
        Icons.Filled.History,
        stringResource(R.string.onboarding_step7_title),
        stringResource(R.string.onboarding_step7_body)
    )
)

@Composable
fun OnboardingScreen(onFinished: () -> Unit) {
    var index by remember { mutableStateOf(0) }
    val colors = MaterialTheme.colorScheme
    val steps = onboardingSteps()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // UI-02 fix: area konten dipisah dari area tombol & diberi
        // verticalScroll + weight(1f) -- di font scale/display size besar
        // atau device layar pendek, konten sekarang scroll, tombol bawah
        // tetap di area aman, tidak lagi bertabrakan/terpotong.
        Column(
            modifier = Modifier
                .weight(1f, fill = false)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            ProgressDots(total = steps.size, current = index)

            Crossfade(targetState = index, label = "onboardingStep", animationSpec = tween(220)) { stepIndex ->
                val currentStep = steps[stepIndex]
                Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .background(colors.primary, RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(currentStep.icon, contentDescription = null, tint = colors.onPrimary, modifier = Modifier.size(36.dp))
                    }

                    Text(stringResource(R.string.onboarding_progress_fmt, stepIndex + 1, steps.size), style = MaterialTheme.typography.labelLarge, color = colors.onSurfaceVariant)
                    Text(currentStep.title, style = MaterialTheme.typography.headlineSmall, color = colors.onBackground)
                    Text(currentStep.body, style = MaterialTheme.typography.bodyLarge, color = colors.onBackground)
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(
                onClick = { if (index < steps.lastIndex) index++ else onFinished() },
                colors = ButtonDefaults.buttonColors(containerColor = colors.secondary, contentColor = colors.onSecondary),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (index < steps.lastIndex) stringResource(R.string.onboarding_btn_next) else stringResource(R.string.onboarding_btn_start))
            }
            if (index > 0) {
                OutlinedButton(
                    onClick = { index-- },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.primary),
                    modifier = Modifier.fillMaxWidth()
                ) { Text(stringResource(R.string.onboarding_btn_back)) }
            }
        }
    }
}

@Composable
private fun ProgressDots(total: Int, current: Int) {
    val colors = MaterialTheme.colorScheme
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(total) { i ->
            Box(
                modifier = Modifier
                    .height(4.dp)
                    .width(if (i == current) 24.dp else 12.dp)
                    .background(
                        if (i == current) colors.primary else colors.outline,
                        RoundedCornerShape(2.dp)
                    )
            )
        }
    }
}
