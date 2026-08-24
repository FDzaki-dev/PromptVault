package com.elprompter.promptvault.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.elprompter.promptvault.ui.theme.TactileTokens

private val TrackWidth = 46.dp
private val TrackHeight = 26.dp
private val ThumbSize = 20.dp
private val ThumbTravel = TrackWidth - ThumbSize - 6.dp // 3dp inset tiap sisi

/**
 * Switch tactile -- pengganti `Switch` Material3 polos.
 *
 * v8.0.0 -- Glassmorphism -> Material 3 murni: track & thumb sekarang
 * `TactileSurface` (Surface M3 baku, tonal+shadow elevation), warna dari
 * `colorScheme.surfaceContainerLowest`/`surfaceContainerHigh` (peran M3
 * baku), overlay radial-gradient "kilau kaca" pada thumb DIHAPUS (bahasa
 * visual Glassmorphism, bukan M3). ON/OFF TIDAK bergantung HANYA pada
 * kedalaman -- posisi thumb kiri/kanan + warna track tetap jadi penanda
 * kedua (Accessibility), tidak berubah.
 *
 * v8.30.6 -- [Pending Queue item #1, permintaan eksplisit user] Track
 * SEBELUMNYA `recessed = true` PERMANEN (v8.0.0: "track SELALU cekung baik
 * ON/OFF, warna isi yang bertugas sinyal status, bukan kedalaman"). User
 * eksplisit minta efek tenggelam-timbul ikut ON/OFF: "track cekung saat
 * OFF, thumb timbul saat ON". Fix: track ikut `recessed = !checked` --
 * PERSIS pola yang SUDAH dipakai thumb sejak awal (lihat di bawah, 0
 * berubah di situ) -- sekarang KEDUA permukaan sama-sama membalik
 * kedalaman serentak: OFF = track+thumb cekung ("tertekan/mati"), ON =
 * track+thumb timbul ("terangkat/hidup"), sinyal tactile yang jauh lebih
 * tegas drpd cuma 1 dari 2 permukaan yang berubah. Warna (poin kedua
 * Accessibility) + posisi thumb TETAP tidak berubah -- 0 dampak ke
 * penanda non-visual/kontras.
 */
@Composable
fun TactileSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    accentColor: Color = MaterialTheme.colorScheme.primary
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val colors = MaterialTheme.colorScheme

    val trackColor by animateColorAsState(
        targetValue = if (checked) accentColor.copy(alpha = 0.9f) else colors.surfaceContainerLowest,
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

    // UI-04 fix: hitbox (toggleable) TERPISAH dari ukuran visual track
    // (46x26dp) -- wrapper luar diberi minimum 48dp x 48dp, toggleable
    // dipasang di wrapper itu, track visual TETAP 46x26dp di-center.
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
        // Track: [v8.30.6] `recessed = !checked` -- cekung saat OFF, timbul
        // saat ON (SEBELUMNYA `true` permanen, lihat javadoc di atas).
        // `trackColor` (warna) TETAP jadi penanda status utama/Accessibility,
        // kedalaman kini penguat visual TAMBAHAN, bukan pengganti.
        TactileSurface(
            modifier = Modifier.size(width = TrackWidth, height = TrackHeight),
            shape = RoundedCornerShape(50),
            color = trackColor,
            recessed = !checked
        ) {
            Box(contentAlignment = Alignment.CenterStart) {
                // Thumb: TactileSurface timbul saat ON (tonal+shadow elevation
                // M3 baku), recessed saat OFF -- pola `recessed = !checked` ini
                // yang JADI ACUAN track di atas ikut sejak v8.30.6 (sebelumnya
                // cuma thumb yang begini, track statis).
                TactileSurface(
                    modifier = Modifier
                        .offset(x = 3.dp + thumbOffset)
                        .size(ThumbSize)
                        .scale(thumbScale),
                    shape = RoundedCornerShape(50),
                    color = colors.surfaceContainerHigh,
                    elevation = TactileTokens.TactileElevationThumb,
                    recessed = !checked,
                    content = {}
                )
            }
        }
    }
}
