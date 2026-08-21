package com.elprompter.promptvault.ui.screens

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.elprompter.promptvault.R
import com.elprompter.promptvault.ui.MainViewModel
import com.elprompter.promptvault.ui.components.EmptyState
import com.elprompter.promptvault.ui.components.VaultCard
import com.elprompter.promptvault.ui.components.VaultTopBar

/**
 * [Roadmap Fase 2.3, 2026-08-21] Statistik penuh -- lanjutan Fase 1.4
 * (angka ringkas Home). Grafik tren 14 hari + total per-rule sepanjang
 * riwayat tersimpan. Sumber data & caveat cap 200 entri: lihat KDoc
 * [MainViewModel.statisticsData]/[MainViewModel.StatisticsData].
 *
 * SENGAJA tanpa library chart eksternal -- grafik batang hand-rolled pakai
 * `Canvas` polos (konsisten dgn filosofi "low-risk" project ini: nol
 * dependency baru = nol risiko kompatibilitas versi/lisensi yang belum
 * pernah diaudit `preflight_check.sh`). Label sumbu-X SENGAJA cuma 3 titik
 * (awal/tengah/akhir, bukan 14 label penuh) -- 14 label sempit di layar HP
 * beresiko tumpang tindih/terpotong, 3 titik cukup memberi konteks rentang
 * waktu tanpa risiko itu.
 *
 * Layout `Column` + `verticalScroll` polos, BUKAN lazy-list -- jumlah rule
 * realistis kecil (puluhan, bukan ratusan), list per-rule pendek aman
 * di-render langsung tanpa lazy loading, sekaligus menghindari kombinasi
 * scroll-di-dalam-scroll yang preflight kategori 5 tandai berisiko.
 */
@Composable
fun StatisticsScreen(
    data: MainViewModel.StatisticsData,
    onBack: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    Scaffold(
        topBar = { VaultTopBar(title = stringResource(R.string.statistics_title), onBack = onBack) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Crossfade(targetState = data.totalAllTime == 0, label = "statisticsEmptyState", animationSpec = tween(220)) { isEmpty ->
                if (isEmpty) {
                    EmptyState(
                        icon = Icons.Filled.BarChart,
                        title = stringResource(R.string.statistics_empty_title),
                        message = stringResource(R.string.statistics_empty_message),
                        accentColor = colors.primary,
                        accentContainerColor = colors.primaryContainer
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        VaultCard(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    data.totalAllTime.toString(),
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = colors.onSurface
                                )
                                Text(
                                    stringResource(R.string.statistics_total_label),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.onSurfaceVariant
                                )
                            }
                        }

                        VaultCard(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    stringResource(R.string.statistics_trend_title),
                                    style = MaterialTheme.typography.titleSmall,
                                    color = colors.onSurface
                                )
                                TrendBarChart(
                                    data = data.dailyTrend,
                                    barColor = colors.primary,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(100.dp)
                                        .padding(top = 12.dp, bottom = 4.dp)
                                )
                                if (data.dailyTrend.isNotEmpty()) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        val mid = data.dailyTrend[data.dailyTrend.size / 2]
                                        Text(data.dailyTrend.first().dayLabel, style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
                                        Text(mid.dayLabel, style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
                                        Text(data.dailyTrend.last().dayLabel, style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
                                    }
                                }
                            }
                        }

                        if (data.perRule.isNotEmpty()) {
                            VaultCard(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Text(
                                        stringResource(R.string.statistics_per_rule_title),
                                        style = MaterialTheme.typography.titleSmall,
                                        color = colors.onSurface
                                    )
                                    val maxCount = data.perRule.first().count.coerceAtLeast(1)
                                    data.perRule.forEach { bucket ->
                                        RuleBar(folderName = bucket.folderName, count = bucket.count, maxCount = maxCount, barColor = colors.tertiary)
                                    }
                                }
                            }
                        }

                        Text(
                            stringResource(R.string.statistics_cap_caveat),
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Grafik batang hand-rolled, lihat javadoc [StatisticsScreen] kenapa tanpa library.
 *
 * [Fix cacat UI, laporan user + screenshot 2026-08-21] Sebelumnya bar count=0
 * digambar dgn barHeight=0px -- drawRoundRect ukuran nol = TIDAK TERLIHAT SAMA
 * SEKALI. Judul bilang "Tren 14 hari terakhir" (selalu 14 bucket, lihat
 * computeStatisticsData di MainViewModel.kt) tapi kalau cuma 4 hari yg punya
 * aktivitas, user cuma lihat 4 batang mengambang di tengah kanvas kosong --
 * kelihatan seperti chart RUSAK/belum sepenuhnya termuat, bukan "10 hari
 * lainnya memang 0 aktivitas". Fix: hari dgn count=0 tetap digambar sbg stub
 * pendek warna redup (bukan tinggi 0) -- 14 batang SELALU terlihat, beda
 * visual jelas antara "0 aktivitas" vs "ada aktivitas".
 */
@Composable
private fun TrendBarChart(data: List<MainViewModel.StatisticsData.DayBucket>, barColor: Color, modifier: Modifier = Modifier) {
    val maxCount = (data.maxOfOrNull { it.count } ?: 0).coerceAtLeast(1)
    val emptyStubColor = barColor.copy(alpha = 0.22f)
    Canvas(modifier = modifier) {
        if (data.isEmpty()) return@Canvas
        val spacing = 4.dp.toPx()
        val barWidth = (size.width - spacing * (data.size - 1)) / data.size
        val stubHeight = 3.dp.toPx()
        data.forEachIndexed { index, bucket ->
            val ratio = bucket.count.toFloat() / maxCount.toFloat()
            val barHeight = (size.height * ratio).coerceAtLeast(stubHeight)
            val x = index * (barWidth + spacing)
            drawRoundRect(
                color = if (bucket.count > 0) barColor else emptyStubColor,
                topLeft = Offset(x, size.height - barHeight),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
            )
        }
    }
}

/** Satu baris breakdown per-rule: label + bar proporsional + angka, pola sederhana tanpa Canvas. */
@Composable
private fun RuleBar(folderName: String, count: Int, maxCount: Int, barColor: Color) {
    val colors = MaterialTheme.colorScheme
    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(folderName, style = MaterialTheme.typography.bodyMedium, color = colors.onSurface)
            Text(count.toString(), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
        }
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .padding(top = 4.dp)
        ) {
            val ratio = count.toFloat() / maxCount.toFloat()
            drawRoundRect(
                color = colors.surfaceContainerHighest,
                size = size,
                cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
            )
            drawRoundRect(
                color = barColor,
                size = Size(size.width * ratio, size.height),
                cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
            )
        }
    }
}
