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
import com.elprompter.promptvault.ui.theme.NeuBorder
import com.elprompter.promptvault.ui.theme.NeuHighlight
import com.elprompter.promptvault.ui.theme.NeuShadowDark
import com.elprompter.promptvault.ui.theme.TactileTokens
import com.elprompter.promptvault.ui.theme.TitaniumSurfaceRaised
import com.elprompter.promptvault.ui.theme.TitaniumSurfaceRecessed

private val TrackWidth = 46.dp
private val TrackHeight = 26.dp
private val ThumbSize = 20.dp
private val ThumbTravel = TrackWidth - ThumbSize - 6.dp // 3dp inset tiap sisi

/**
 * Switch tactile -- v4.0.0 recolor total ke "Dark Titanium Neumorphism".
 *
 * OFF: track TENGGELAM (fill TitaniumSurfaceRecessed, gelap ke arah kiri-atas
 * -- kebalikan permukaan terangkat -- persis prinsip inset neumorphism di
 * `Neumorphic.kt`) -- terasa dicukil ke dalam logam, bukan cuma abu-abu flat.
 * ON: track terangkat tipis dgn tint accentColor (default Zamrud/primary,
 * "sedikit sentuhan" sesuai instruksi) + glow lokal SATU tempat (thumb) --
 * ON/OFF tetap punya penanda kedua di luar warna (posisi thumb kiri/kanan).
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
        targetValue = if (checked) accentColor.copy(alpha = 0.85f) else TitaniumSurfaceRecessed,
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
                // kebalikan highlight normal) -- saat ON, border tenang
                // mengikuti hairline neu biasa supaya aksennya tidak dobel.
                color = if (checked) NeuBorder else NeuShadowDark.copy(alpha = 0.4f),
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
                    Brush.radialGradient(colors = listOf(NeuHighlight, TitaniumSurfaceRaised)),
                    RoundedCornerShape(50)
                )
        )
    }
}
