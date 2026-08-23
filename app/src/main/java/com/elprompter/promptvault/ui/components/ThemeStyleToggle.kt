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
import com.elprompter.promptvault.ui.theme.TactileTokens
import androidx.compose.ui.unit.dp

/**
 * [Pending Queue, roadmap tab "Tampilan"] KERANGKA SAJA -- picker visual 2
 * gaya tema kustom (Glassmorphism / Neumorphism) yang DISEBUTKAN user secara
 * eksplisit, TAPI belum ada engine render di baliknya. `selected`/`onSelect`
 * murni state lokal di `HomeScreen` (`remember`) -- TIDAK ditulis ke
 * DataStore/Prefs, TIDAK memengaruhi `Theme.kt`/`Color.kt`/komponen manapun.
 * Pilihan apa pun yang ditekan di sini TIDAK mengubah tampilan app SAMA
 * SEKALI saat ini -- sengaja, sesuai instruksi "jangan kerjakan isinya
 * dulu, cukup kerangkanya saja". Badge "Segera hadir" menandai ini eksplisit
 * ke user supaya tidak dikira bug kalau pilihan tidak berefek.
 *
 * Menyusul (di luar scope batch ini, PENDING QUEUE): definisi token warna
 * per-gaya, mekanisme switch runtime di `Theme.kt` (kemungkinan
 * `CompositionLocal` baru), persistensi pilihan (DataStore, pola sama
 * dengan preferensi lain di `SettingsScreen.kt`).
 */
enum class ThemeStyleOption { GLASSMORPHISM, NEUMORPHISM }

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
        // Badge "Segera hadir" -- lihat KDoc atas: toggle ini murni kerangka,
        // pilihan TIDAK mengubah tampilan app sama sekali saat ini.
        Text(
            stringResource(R.string.theme_toggle_coming_soon),
            style = MaterialTheme.typography.labelMedium,
            color = colors.tertiary
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
