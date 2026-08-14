package com.elprompter.promptvault.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.elprompter.promptvault.ui.theme.GlassSurfaceElevated
import com.elprompter.promptvault.ui.theme.GlassSurfacePressed
import com.elprompter.promptvault.ui.theme.NeuHighlight
import com.elprompter.promptvault.ui.theme.TactileTokens

private val TrackWidth = 46.dp
private val TrackHeight = 26.dp
private val ThumbSize = 20.dp
private val ThumbTravel = TrackWidth - ThumbSize - 6.dp // 3dp inset tiap sisi

/**
 * Switch tactile bab 12 spesifikasi -- pengganti `Switch` Material3 polos.
 *
 * v5.0.0 -- Redesign Glassmorphism -> Neumorphism. OFF: track sekarang benar
 * BENAR tenggelam (NeumorphicSurface `pressed = true` -- overlay gradien
 * diagonal terbalik, bukan cuma border gelap tunggal seperti versi glass
 * lama) di dalam wadah [GlassSurfacePressed]. ON: track jadi tint aksen
 * SOLID (bukan lagi indikator "recessed vs flat", tapi warna) + thumb-nya
 * yang sekarang dapat efek TIMBUL neumorphic (dual-shadow kecil via
 * `NeumorphicSurface`, [TactileTokens.NeuElevationThumb]/
 * [TactileTokens.NeuOffsetThumb]) menggantikan `Modifier.shadow(spotColor=
 * accentColor)` tunggal berwarna lama -- konsisten dgn seluruh app yang kini
 * pakai shadow ganda terarah, bukan glow satu warna. ON/OFF TIDAK bergantung
 * HANYA pada kedalaman -- posisi thumb kiri/kanan + warna track tetap jadi
 * penanda kedua (bab 21 Accessibility), sama seperti sebelumnya.
 */
@Composable
fun TactileSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    accentColor: Color = androidx.compose.material3.MaterialTheme.colorScheme.primary
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()

    val trackColor by animateColorAsState(
        targetValue = if (checked) accentColor.copy(alpha = 0.9f) else GlassSurfacePressed,
        animationSpec = tween(TactileTokens.PressAnimationMillis),
        label = "switchTrack"
    )
    val thumbOffset by animateDpAsState(
        targetValue = if (checked) ThumbTravel else 0.dp,
        animationSpec = tween(TactileTokens.PressAnimationMillis),
        label = "switchThumbOffset"
    )
    val thumbScale by animateFloatAsState(
        targetValue = if (pressed) TactileTokens.PressScale else 1f,
        animationSpec = tween(TactileTokens.PressAnimationMillis),
        label = "switchThumbScale"
    )

    // UI-04 fix: hitbox (toggleable) sekarang TERPISAH dari ukuran visual
    // track (46x26dp). Sebelumnya toggleable dipasang LANGSUNG di
    // NeumorphicSurface bertrack 46x26dp -- area sentuh fisik/logis jadi
    // lebih kecil dari touch target Android yang nyaman. Sekarang wrapper
    // Box luar diberi minimum 48dp x 48dp, toggleable dipasang di wrapper
    // itu, track visual TETAP 46x26dp (tidak berubah tampilannya) di-center
    // di dalam wrapper.
    Box(
        modifier = modifier
            .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
            .toggleable(
                value = checked,
                interactionSource = interactionSource,
                indication = null,
                role = Role.Switch,
                onValueChange = onCheckedChange
            ),
        contentAlignment = Alignment.Center
    ) {
        // Track: NeumorphicSurface `pressed = true` permanen -- track SELALU
        // terbaca sbg "wadah cekung" (baik ON maupun OFF), warna isi
        // (trackColor) yang berubah utk sinyal status, bukan kedalamannya
        // (konsisten dgn desain neumorphism umum: track selalu inset, thumb
        // yang mengapung di atasnya).
        NeumorphicSurface(
            modifier = Modifier.size(width = TrackWidth, height = TrackHeight),
            shape = RoundedCornerShape(50),
            color = trackColor,
            pressed = true
        ) {
            Box(contentAlignment = Alignment.CenterStart) {
                // Thumb: NeumorphicSurface timbul kecil saat ON (dual-shadow),
                // flat tanpa shadow saat OFF (thumb "diam" di dasar cekungan,
                // belum "terangkat" -- shadow ganda cuma relevan kalau ada tint
                // aksen utk dipantulkan).
                NeumorphicSurface(
                    modifier = Modifier
                        .offset(x = 3.dp + thumbOffset)
                        .size(ThumbSize)
                        .scale(thumbScale),
                    shape = RoundedCornerShape(50),
                    color = GlassSurfaceElevated,
                    baseColor = trackColor,
                    elevation = if (checked) TactileTokens.NeuElevationThumb else 0.dp,
                    shadowOffset = TactileTokens.NeuOffsetThumb
                ) {
                    Box(
                        modifier = Modifier
                            .background(
                                Brush.radialGradient(colors = listOf(NeuHighlight, Color.Transparent)),
                                RoundedCornerShape(50)
                            )
                    )
                }
            }
        }
    }
}
