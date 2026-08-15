package com.elprompter.promptvault.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.elprompter.promptvault.ui.theme.GlassBorder
import com.elprompter.promptvault.ui.theme.GlassHighlight
import com.elprompter.promptvault.ui.theme.GlassShadow
import com.elprompter.promptvault.ui.theme.TactileTokens

/**
 * v7.0.0 — Primitif tunggal Glassmorphism, MENGGANTIKAN `Neumorphic.kt`
 * (dihapus total, permintaan eksplisit user: "ultra buggy" -- riwayat
 * Insiden #3/#8/#9/#10 di PROJECT_STATE.md, semua berasal dari teknik
 * shadow ganda offset-Box `NeumorphicSurface`).
 *
 * ## Kenapa lebih sederhana & tidak rawan bug kelas yang sama
 * `Neumorphic.kt` lama butuh 2 `Box` TAMBAHAN (shadow-caster) yang harus
 * `baseColor`-nya cocok PERSIS dgn latar sesungguhnya di belakang komponen
 * (root cause Insiden #9 & #10 -- gagal total di atas gradient/latar
 * campuran), DAN `modifier` pemanggil harus dipasang di `Box` pembungkus
 * TERPISAH dari `Surface` konten (root cause Insiden #8 -- `weight()`
 * nyasar). Primitif ini TIDAK punya salah satu dari 2 sumber bug itu:
 * - Tidak ada shadow-caster/baseColor sama sekali -- shadow di sini murni
 *   `Modifier.shadow` BAWAAN Compose, digambar Android runtime sendiri,
 *   valid di atas latar APAPUN (solid atau bukan).
 * - `modifier` pemanggil (`fillMaxWidth()`/`weight(1f)`/dst) dipasang
 *   LANGSUNG di `Surface` -- primitif ini TIDAK membungkusnya lagi dengan
 *   `Box` tambahan, jadi `Surface` inilah anak LANGSUNG Row/Column
 *   pemanggil & `ParentDataModifier` seperti `weight()` selalu terbaca
 *   benar sejak awal (kelas bug Insiden #8 terstruktur tidak mungkin
 *   terulang, bukan cuma ditambal).
 *
 * ## Bahasa visual Glassmorphism
 * - `color` panel: tint Deep Navy semi-solid (lihat `Color.kt`) -- badan
 *   kaca, bukan tembus pandang penuh (tidak ada blur asli di minSdk 26).
 * - `border`: hairline putih-alpha rendah ([GlassBorder]) -- "tepi kaca"
 *   khas glassmorphism, DIHAPUS di era Neumorphism v5.0.0, sekarang
 *   dikembalikan.
 * - `highlight`: overlay gradient diagonal tipis putih->transparan di
 *   pojok kiri-atas (kesan cahaya memantul di permukaan kaca). Dimatikan
 *   otomatis kalau `recessed = true`.
 * - `elevation`: SATU shadow standar Compose (bukan dual/offset), pakai
 *   warna [GlassShadow] (hitam-alpha netral) supaya terasa "melayang"
 *   ringan tanpa glow berwarna.
 *
 * @param recessed permukaan "tenggelam" (track switch/segmented control OFF,
 *   grabber pill sheet) -- shadow & highlight dimatikan, `color` pemanggil
 *   (biasanya token `GlassSurfacePressed`, lebih gelap) yang membawa kesan
 *   cekung, bukan lagi overlay gradien terbalik seperti sistem lama.
 */
@Composable
fun GlassPanel(
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.medium,
    color: Color = MaterialTheme.colorScheme.surface,
    elevation: Dp = TactileTokens.GlassElevationCard,
    recessed: Boolean = false,
    border: BorderStroke? = BorderStroke(1.dp, GlassBorder),
    onClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    content: @Composable () -> Unit
) {
    val effectiveElevation = if (recessed) 0.dp else elevation
    val shadowedModifier = if (effectiveElevation > 0.dp) {
        modifier.shadow(
            elevation = effectiveElevation,
            shape = shape,
            clip = false,
            ambientColor = GlassShadow,
            spotColor = GlassShadow
        )
    } else {
        modifier
    }

    val innerContent: @Composable () -> Unit = {
        Box {
            if (!recessed) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(
                            brush = Brush.linearGradient(colors = listOf(GlassHighlight, Color.Transparent)),
                            shape = shape
                        )
                )
            }
            content()
        }
    }

    if (onClick != null) {
        Surface(
            onClick = onClick,
            enabled = enabled,
            modifier = shadowedModifier,
            shape = shape,
            color = color,
            border = border,
            tonalElevation = 0.dp,
            shadowElevation = 0.dp,
            interactionSource = interactionSource,
            content = innerContent
        )
    } else {
        Surface(
            modifier = shadowedModifier,
            shape = shape,
            color = color,
            border = border,
            tonalElevation = 0.dp,
            shadowElevation = 0.dp,
            content = innerContent
        )
    }
}
