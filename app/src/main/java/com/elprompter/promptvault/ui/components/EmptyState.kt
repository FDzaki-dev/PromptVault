package com.elprompter.promptvault.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.elprompter.promptvault.ui.theme.TactileTokens

/**
 * Empty state bersama, dipakai di semua layar list (Kelola Rule, Riwayat
 * Aktivitas, Undo Pemindahan, File Dilewati).
 *
 * v2.3.7: sebelumnya tiap layar punya Text() polos sendiri-sendiri tanpa
 * ikon, dengan padding-top yang tidak konsisten satu sama lain (12dp, 16dp,
 * dst). Sekarang satu komponen: ikon bulat bertema warna aksen layar
 * tersebut -- konsisten dengan "sistem warna 4-aksen" yang sudah jadi
 * standar (lihat PROJECT_STATE.md) -- judul singkat, dan pesan pendukung.
 * Dipanggil lewat Crossfade di tiap layar supaya transisi kosong<->berisi
 * halus, bukan potongan tiba-tiba.
 *
 * v8.0.0 -- Glassmorphism -> Material 3 murni: lingkaran ikon sekarang
 * [TactileSurface] timbul kecil ([TactileTokens.TactileElevationControl],
 * Surface M3 baku), konsisten dengan kotak ikon `GroupedListRow` & thumb
 * `TactileSwitch`. Overlay gradient tint diganti fill solid alpha rendah
 * (sama pola dgn `GroupedListRow`). Warna aksen per layar (`accentColor`/
 * `accentContainerColor`) TIDAK berubah.
 */
@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    accentContainerColor: Color = MaterialTheme.colorScheme.primaryContainer
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 40.dp, bottom = 16.dp, start = 24.dp, end = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        TactileSurface(
            modifier = Modifier.size(56.dp),
            shape = RoundedCornerShape(16.dp),
            color = accentContainerColor,
            elevation = TactileTokens.TactileElevationControl
        ) {
            Box(
                modifier = Modifier.background(accentColor.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(28.dp))
            }
        }
        Text(
            title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )
        Text(
            message,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}
