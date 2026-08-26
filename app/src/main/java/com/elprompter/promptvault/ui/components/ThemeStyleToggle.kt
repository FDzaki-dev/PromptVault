package com.elprompter.promptvault.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.elprompter.promptvault.R
import com.elprompter.promptvault.data.ThemeStyleOption
import com.elprompter.promptvault.ui.theme.TactileTokens
import androidx.compose.ui.unit.dp

/**
 * (v8.23.4) UI diganti dari radio-row (checkmark, cuma 1 bisa aktif via
 * tap-select) jadi SAKLAR ON/OFF (`TactileSwitch`) per baris -- diminta
 * eksplisit user. Tetap mutually exclusive (cuma 1 gaya aktif dalam satu
 * waktu, sesuai sifat `TactileSurface` yang cuma bisa render 1 gaya per
 * panggilan) -- switch OPSI LAIN otomatis OFF saat 1 dinyalakan (state
 * `selected` tunggal dari `MainViewModel.themeStyle`, BUKAN 3 boolean
 * independen). Menekan switch yang SEDANG ON tidak melakukan apa-apa
 * (`onCheckedChange` cuma diteruskan saat `checked=true` -- mencegah
 * "0 gaya aktif" yang tidak valid).
 *
 * 4 opsi (v8.31.1, sebelumnya 3; nama gaya ke-4 di-rename HYBRID->CUPERTINO
 * di v8.31.4): Glassmorphism, Neumorphism, Material 3 Murni, Cupertino
 * (menuju identitas Cupertino penuh, dikerjakan bertahap, lihat
 * `CupertinoTokens.kt`).
 */
@Composable
fun ThemeStyleToggle(
    selected: ThemeStyleOption,
    onSelect: (ThemeStyleOption) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            stringResource(R.string.theme_toggle_title),
            style = MaterialTheme.typography.titleMedium,
            color = colors.onBackground
        )
        Text(
            stringResource(R.string.theme_toggle_description),
            style = MaterialTheme.typography.bodySmall,
            color = colors.onSurfaceVariant
        )
        ThemeStyleSwitchRow(
            label = stringResource(R.string.theme_toggle_glassmorphism),
            checked = selected == ThemeStyleOption.GLASSMORPHISM,
            onCheckedChange = { if (it) onSelect(ThemeStyleOption.GLASSMORPHISM) }
        )
        ThemeStyleSwitchRow(
            label = stringResource(R.string.theme_toggle_neumorphism),
            checked = selected == ThemeStyleOption.NEUMORPHISM,
            onCheckedChange = { if (it) onSelect(ThemeStyleOption.NEUMORPHISM) }
        )
        ThemeStyleSwitchRow(
            label = stringResource(R.string.theme_toggle_material3),
            checked = selected == ThemeStyleOption.MATERIAL3,
            onCheckedChange = { if (it) onSelect(ThemeStyleOption.MATERIAL3) }
        )
        ThemeStyleSwitchRow(
            label = stringResource(R.string.theme_toggle_cupertino),
            checked = selected == ThemeStyleOption.CUPERTINO,
            onCheckedChange = { if (it) onSelect(ThemeStyleOption.CUPERTINO) }
        )
    }
}

@Composable
private fun ThemeStyleSwitchRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    val colors = MaterialTheme.colorScheme
    TactileSurface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = if (checked) colors.primaryContainer else colors.surfaceContainer,
        elevation = if (checked) TactileTokens.TactileElevationControl else 0.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                label,
                style = MaterialTheme.typography.bodyLarge,
                color = if (checked) colors.onPrimaryContainer else colors.onSurface
            )
            TactileSwitch(checked = checked, onCheckedChange = onCheckedChange)
        }
    }
}
