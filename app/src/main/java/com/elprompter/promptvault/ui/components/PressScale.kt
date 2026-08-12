package com.elprompter.promptvault.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import com.elprompter.promptvault.ui.theme.TactileTokens

/**
 * Efek "tekan mengecil sedikit" (bab 6 spesifikasi: press -> sink, tanpa
 * bounce berlebihan). Dipakai di baris menu Home dan tombol CTA utama.
 */
@Composable
fun Modifier.pressScale(interactionSource: MutableInteractionSource): Modifier {
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        if (pressed) TactileTokens.PressScale else 1f,
        animationSpec = tween(TactileTokens.PressAnimationMillis),
        label = "pressScale"
    )
    return this.scale(scale)
}

/**
 * Versi lengkap bab 6: skala mengecil DAN elevasi turun ke 0 saat ditekan,
 * supaya kontrol terasa benar-benar "tenggelam", bukan cuma mengecil.
 * Dipakai untuk kontrol tactile yang lebih menonjol (CTA utama).
 */
@Composable
fun Modifier.tactilePress(
    interactionSource: MutableInteractionSource,
    shape: Shape = RoundedCornerShape(TactileTokens.ControlCornerRadius),
    raisedElevation: Dp = TactileTokens.ElevationRaised
): Modifier {
    val pressed by interactionSource.collectIsPressedAsState()
    val elevation by animateDpAsState(
        if (pressed) TactileTokens.ElevationPressed else raisedElevation,
        animationSpec = tween(TactileTokens.PressAnimationMillis),
        label = "tactileElevation"
    )
    val scale by animateFloatAsState(
        if (pressed) TactileTokens.PressScale else 1f,
        animationSpec = tween(TactileTokens.PressAnimationMillis),
        label = "tactileScale"
    )
    return this
        .scale(scale)
        .shadow(elevation = elevation, shape = shape)
}
