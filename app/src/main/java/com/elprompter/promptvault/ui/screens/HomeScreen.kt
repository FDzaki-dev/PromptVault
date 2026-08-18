package com.elprompter.promptvault.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Rule
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.elprompter.promptvault.ui.MainViewModel
import com.elprompter.promptvault.ui.components.GroupedList
import com.elprompter.promptvault.ui.components.GroupedListRow
import com.elprompter.promptvault.ui.components.TactileSurface
import com.elprompter.promptvault.ui.components.VaultCard
import com.elprompter.promptvault.ui.theme.TactileTokens
import com.elprompter.promptvault.ui.theme.VaultTheme

@Composable
fun HomeScreen(
    ruleCount: Int,
    intervalMinutes: Int,
    isScanning: Boolean,
    lastScanSummary: String?,
    hasSkippedFiles: Boolean,
    scanFeedback: MainViewModel.ScanFeedback?,
    onScanFeedbackConsumed: () -> Unit,
    onScanNow: () -> Unit,
    onOpenRules: () -> Unit,
    onOpenLog: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenDiagnostics: () -> Unit,
    onOpenSkippedFiles: () -> Unit,
    // [Fitur baru, batch "Panduan User Baru" 2026-08-17] Jalan pintas paling
    // discoverable ke PanduanScreen -- ditaruh di grouped menu Home (bukan
    // cuma di Pengaturan) supaya user baru yang belum pernah buka Pengaturan
    // sama sekali tetap gampang menemukan referensi lengkap kapan saja.
    onOpenPanduan: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    // [v8.0.0] Riwayat panjang Insiden #9/#10 (baseColor vs wash gradient,
    // lihat PROJECT_STATE.md) sepenuhnya MOOT: `TactileSurface` (Surface M3
    // baku, pengganti `GlassPanel`, lihat `TactileSurface.kt`) tidak pakai
    // teknik shadow-caster sama sekali -- `shadowElevation`/`tonalElevation`
    // Compose asli, valid di atas latar apapun. Latar layar ini tetap solid
    // `colors.background` (konsisten dgn seluruh app).
    val haptics = LocalHapticFeedback.current
    val snackbarHostState = remember { SnackbarHostState() }
    // Warna Snackbar yang SEDANG tayang di-snapshot terpisah dari scanFeedback
    // -- supaya begitu onScanFeedbackConsumed() men-null-kan scanFeedback di
    // ViewModel (lihat komentar di LaunchedEffect di bawah), warna Snackbar
    // yang masih tampil di layar tidak ikut berubah balik ke warna normal.
    var activeIsError by remember { mutableStateOf(false) }

    // Keyed ke eventId (bukan teks pesan) -- supaya scan kedua dengan hasil
    // teks identik ("Tidak ada file cocok" dua kali berturut) tetap memicu
    // Snackbar + haptic baru, bukan cuma diam karena StateFlow value sama.
    LaunchedEffect(scanFeedback?.eventId) {
        val feedback = scanFeedback ?: return@LaunchedEffect
        activeIsError = feedback.isError
        haptics.performHapticFeedback(
            if (feedback.isError) HapticFeedbackType.LongPress else HapticFeedbackType.TextHandleMove
        )
        // Konsumsi SEBELUM showSnackbar (bukan sesudah) -- showSnackbar itu
        // suspend dan baru return setelah dismiss/timeout. Kalau di-null-kan
        // sesudahnya, ada jendela waktu di mana user sempat navigasi
        // pergi-pulang SEBELUM Snackbar pertama selesai, dan re-trigger tetap
        // bisa kejadian. Konsumsi lebih awal menutup celah itu.
        onScanFeedbackConsumed()
        snackbarHostState.showSnackbar(feedback.message)
    }

    Scaffold(
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = if (activeIsError) colors.error else colors.primary,
                    contentColor = if (activeIsError) colors.onError else colors.onPrimary
                )
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.background)
                // [v8.0.0] Latar tetap solid `colors.background` (biru-calm
                // gelap, lihat Color.kt) -- konsisten dgn seluruh layar app,
                // bukan wash gradient/tekstur.
        ) {
        // UI-01 fix: Column body Home sekarang scrollable (verticalScroll).
        // Sebelumnya fillMaxSize() tanpa scroll -- di layar pendek/landscape/
        // font scale besar, menu bawah bisa terdorong keluar viewport. CTA &
        // grouped menu tetap sebagai item normal di dalam Column yang sama,
        // hirarki visual tidak berubah.
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .padding(top = 12.dp, bottom = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("PromptVault", style = MaterialTheme.typography.headlineMedium, color = colors.onBackground)
            Text("Rapikan otomatis file di Downloads kamu sesuai rule.", style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)

            VaultCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    ManifestRow(icon = Icons.Filled.Rule, tint = colors.primary, label = "Rule aktif", value = "$ruleCount")
                    ManifestRow(icon = Icons.Filled.Schedule, tint = colors.tertiary, label = "Auto-scan", value = "tiap $intervalMinutes menit")
                    if (lastScanSummary != null) {
                        Text(lastScanSummary, style = MaterialTheme.typography.bodySmall, color = colors.onSurface)
                    }
                    if (hasSkippedFiles) {
                        TextButton(onClick = onOpenSkippedFiles, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Filled.ErrorOutline, contentDescription = null, tint = colors.secondary, modifier = Modifier.size(16.dp))
                            Text(" Lihat detail file yang dilewati", color = colors.secondary, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }

            // v8.0.0 -- Glassmorphism -> Material 3 murni: `TactileSurface`
            // dgn tonal+shadow elevation M3 baku ([TactileTokens.TactileElevationCta],
            // == elevasi default FAB M3) saat idle, `recessed = scanPressed`
            // saat ditekan (elevasi hilang, terasa "masuk"). CTA warna solid
            // `colors.primary` (biru calm, lihat Theme.kt).
            val scanInteraction = remember { MutableInteractionSource() }
            val scanPressed by scanInteraction.collectIsPressedAsState()
            val ctaScale by animateFloatAsState(
                targetValue = if (scanPressed) TactileTokens.PressScale else 1f,
                animationSpec = tween(TactileTokens.PressAnimationMillis),
                label = "ctaScale"
            )
            TactileSurface(
                onClick = onScanNow,
                enabled = !isScanning,
                interactionSource = scanInteraction,
                modifier = Modifier
                    .fillMaxWidth()
                    .scale(ctaScale),
                shape = MaterialTheme.shapes.large,
                color = colors.primary,
                elevation = TactileTokens.TactileElevationCta,
                recessed = scanPressed
            ) {
                Box(
                    modifier = Modifier.padding(vertical = 15.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (isScanning) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = colors.onPrimary)
                    } else {
                        Text("Scan Sekarang", color = colors.onPrimary, style = MaterialTheme.typography.titleMedium)
                    }
                }
            }

            val extraColors = VaultTheme.extraColors
            GroupedList(
                rows = listOf(
                    { GroupedListRow(Icons.Filled.Rule, "Kelola Rule", colors.primary, onOpenRules) },
                    // [Fitur baru, batch "Panduan User Baru" 2026-08-17] Tint
                    // pakai colors.tertiary (Amber) -- SENGAJA reuse aksen yang
                    // sama dgn "Riwayat Aktivitas" di bawah, BUKAN aksen ke-5
                    // baru. Sistem warna app dibatasi 4 aksen (primary/
                    // tertiary/error/slate, lihat Color.kt & EmptyState.kt),
                    // menambah aksen ke-5 melanggar standar itu.
                    { GroupedListRow(Icons.Filled.HelpOutline, "Panduan Penggunaan", colors.tertiary, onOpenPanduan) },
                    { GroupedListRow(Icons.Filled.History, "Riwayat Aktivitas & Undo", colors.tertiary, onOpenLog) },
                    { GroupedListRow(Icons.Filled.Settings, "Pengaturan", extraColors.slate, onOpenSettings) },
                    { GroupedListRow(Icons.Filled.BugReport, "Diagnostik", colors.error, onOpenDiagnostics) }
                )
            )
        }
        }
    }
}

@Composable
private fun ManifestRow(icon: androidx.compose.ui.graphics.vector.ImageVector, tint: androidx.compose.ui.graphics.Color, label: String, value: String) {
    // UI-16 fix: alignment label/value sebelumnya mengandalkan spasi literal
    // di dalam string ("  $label   $value") -- rawan tidak konsisten lintas
    // font/locale/rendering. Sekarang pakai Row + Spacer eksplisit berbasis
    // layout, bukan whitespace.
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(16.dp))
        androidx.compose.foundation.layout.Spacer(modifier = Modifier.width(8.dp))
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        androidx.compose.foundation.layout.Spacer(modifier = Modifier.width(6.dp))
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
