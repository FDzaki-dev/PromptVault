package com.elprompter.promptvault.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.elprompter.promptvault.ui.theme.GlassBorder
import com.elprompter.promptvault.ui.theme.GlassHighlight
import com.elprompter.promptvault.ui.theme.GlassShadow
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
 * OFF: track "tenggelam" ke dalam glass (fill lebih gelap dari `GlassSurface`
 * sekitarnya + TANPA shadow terangkat) -- terasa recessed, bukan cuma abu-abu.
 * ON: track terangkat tipis dengan tint aksen (accentColor, default
 * `colors.primary`/Midnight Blue accent) + glow lokal SATU tempat saja (thumb),
 * sesuai bab 18 (glow untuk selected state itu diizinkan, bukan dekorasi
 * tersebar). ON/OFF tidak bergantung HANYA pada kedalaman -- posisi thumb
 * kiri/kanan + warna track tetap jadi penanda kedua (bab 21 Accessibility).
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

    Box(
        modifier = modifier
            .size(width = TrackWidth, height = TrackHeight)
            .toggleable(
                value = checked,
                interactionSource = interactionSource,
                indication = null,
                role = Role.Switch,
                onValueChange = onCheckedChange
            )
            .background(trackColor, RoundedCornerShape(50))
            .border(
                width = 1.dp,
                // Bevel recessed saat OFF (garis atas lebih gelap dari bawah,
                // kebalikan highlight normal bab 8) -- saat ON, border tenang
                // mengikuti hairline glass biasa supaya aksennya tidak dobel.
                color = if (checked) GlassBorder else GlassShadow.copy(alpha = 0.4f),
                shape = RoundedCornerShape(50)
            ),
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .offset(x = 3.dp + thumbOffset)
                .size(ThumbSize)
                .scale(thumbScale)
                .then(
                    if (checked) Modifier.shadow(3.dp, RoundedCornerShape(50), spotColor = accentColor)
                    else Modifier
                )
                .background(
                    Brush.radialGradient(colors = listOf(GlassHighlight, GlassSurfaceElevated)),
                    RoundedCornerShape(50)
                )
        )
    }
}
