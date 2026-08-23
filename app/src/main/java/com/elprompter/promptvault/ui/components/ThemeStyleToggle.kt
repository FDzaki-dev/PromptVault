package com.elprompter.promptvault.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.Icon
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
 * [v8.23.2] Toggle gaya visual LIVE -- `selected`/`onSelect` sekarang
 * ditulis ke `SettingsRepository` (DataStore) lewat `MainViewModel.setThemeStyle`
 * dan dibaca via `MainViewModel.themeStyle`/`VaultTheme.style` (`Theme.kt`),
 * bukan lagi state lokal `remember` (v8.23.0, kerangka). Memilih opsi di
 * sini LANGSUNG mengubah tampilan `TactileSurface` app-wide (lihat
 * `TactileSurface.kt`/`GlassTokens.kt`/`NeumorphTokens.kt`). Badge "Segera
 * hadir" DIHAPUS (sudah tidak akurat -- pilihan sekarang berefek nyata).
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
        ThemeStyleOptionRow(
            label = stringResource(R.string.theme_toggle_glassmorphism),
            isSelected = selected == ThemeStyleOption.GLASSMORPHISM,
            onClick = { onSelect(ThemeStyleOption.GLASSMORPHISM) }
        )
        ThemeStyleOptionRow(
            label = stringResource(R.string.theme_toggle_neumorphism),
            isSelected = selected == ThemeStyleOption.NEUMORPHISM,
            onClick = { onSelect(ThemeStyleOption.NEUMORPHISM) }
        )
    }
}

@Composable
private fun ThemeStyleOptionRow(label: String, isSelected: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    TactileSurface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = if (isSelected) colors.primaryContainer else colors.surfaceContainer,
        elevation = if (isSelected) TactileTokens.TactileElevationControl else 0.dp,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                if (isSelected) Icons.Filled.Check else Icons.Filled.RadioButtonUnchecked,
                contentDescription = null,
                tint = if (isSelected) colors.onPrimaryContainer else colors.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
            Text(
                label,
                style = MaterialTheme.typography.bodyLarge,
                color = if (isSelected) colors.onPrimaryContainer else colors.onSurface
            )
        }
    }
}
