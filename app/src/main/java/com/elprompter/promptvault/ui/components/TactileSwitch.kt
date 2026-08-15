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
import com.elprompter.promptvault.ui.theme.GlassHighlight
import com.elprompter.promptvault.ui.theme.GlassSurfaceElevated
import com.elprompter.promptvault.ui.theme.GlassSurfacePressed
import com.elprompter.promptvault.ui.theme.TactileTokens

private val TrackWidth = 46.dp
private val TrackHeight = 26.dp
private val ThumbSize = 20.dp
private val ThumbTravel = TrackWidth - ThumbSize - 6.dp // 3dp inset tiap sisi

/**
 * Switch tactile bab 12 spesifikasi -- pengganti `Switch` Material3 polos.
 *
 * v7.0.0 -- Neumorphism -> Glassmorphism (KEMBALI): track pakai `GlassPanel`
 * `recessed = true` (shadow+highlight dimatikan, warna [GlassSurfacePressed]
 * gelap yang membawa kesan "slot" tenggelam -- tanpa overlay gradien
 * diagonal terbalik seperti sistem lama, cukup warna solid + tanpa shadow).
 * Thumb ON pakai `GlassPanel` timbul kecil ([TactileTokens.GlassElevationThumb],
 * border+highlight+shadow tunggal standar) menggantikan dual-shadow lama.
 * ON/OFF TIDAK bergantung HANYA pada kedalaman -- posisi thumb kiri/kanan +
 * warna track tetap jadi penanda kedua (Accessibility), tidak berubah.
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
    // track (46x26dp). Sebelumnya toggleable dipasang LANGSUNG di panel
    // bertrack 46x26dp -- area sentuh fisik/logis jadi lebih kecil dari
    // touch target Android yang nyaman. Sekarang wrapper
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
        // Track: GlassPanel `recessed = true` permanen -- track SELALU
        // terbaca sbg "wadah cekung" (baik ON maupun OFF), warna isi
        // (trackColor) yang berubah utk sinyal status, bukan kedalamannya.
        GlassPanel(
            modifier = Modifier.size(width = TrackWidth, height = TrackHeight),
            shape = RoundedCornerShape(50),
            color = trackColor,
            recessed = true
        ) {
            Box(contentAlignment = Alignment.CenterStart) {
                // Thumb: GlassPanel timbul kecil saat ON (shadow+highlight+
                // border standar), recessed (tanpa shadow) saat OFF -- thumb
                // "diam" di dasar slot, belum "terangkat".
                GlassPanel(
                    modifier = Modifier
                        .offset(x = 3.dp + thumbOffset)
                        .size(ThumbSize)
                        .scale(thumbScale),
                    shape = RoundedCornerShape(50),
                    color = GlassSurfaceElevated,
                    elevation = TactileTokens.GlassElevationThumb,
                    recessed = !checked
                ) {
                    Box(
                        modifier = Modifier
                            .background(
                                Brush.radialGradient(colors = listOf(GlassHighlight, Color.Transparent)),
                                RoundedCornerShape(50)
                            )
                    )
                }
            }
        }
    }
}
