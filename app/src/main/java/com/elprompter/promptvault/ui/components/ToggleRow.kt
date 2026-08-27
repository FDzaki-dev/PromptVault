package com.elprompter.promptvault.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp

/**
 * [Refactor rapi-rapi, 2026-08-27, TANPA ubah behavior] Ekstraksi murni dari
 * pola `Row(Arrangement.SpaceBetween) { Text(weight=1f) ; TactileSwitch }`
 * yang sebelumnya diketik ulang identik di 3 titik terpisah
 * (`AddEditRuleScreen.kt` toggle hold-back-zip, `SettingsScreen.kt` toggle
 * Auto-Sort & Mode Shizuku -- lihat fix row-overlap v8.35.1 yang menambal
 * ke-3 titik ini satu-satu). Bukan komponen baru secara visual/perilaku --
 * hasil render & interaksi 100% identik dgn kode inline sebelumnya di
 * ketiga pemanggil, cuma dipindah ke 1 tempat supaya tidak diketik ulang.
 *
 * SENGAJA TIDAK dipakai utk `ThemeStyleSwitchRow` (`ThemeStyleToggle.kt`) --
 * itu pola visual BERBEDA (dibungkus `TactileSurface` berwarna/elevasi
 * sesuai state `checked`, dipakai utk pilihan mutually-exclusive), memaksa
 * reuse di situ akan MENGUBAH tampilannya -- di luar scope task ini.
 *
 * @param style default `titleSmall` sesuai pemanggil pertama (hold-back-zip);
 *   2 pemanggil lain (`SettingsScreen.kt`) pass `titleMedium` eksplisit,
 *   TIDAK diseragamkan diam-diam -- ukuran font asli masing-masing dipertahankan
 *   persis (di luar scope "tanpa ubah behavior").
 * @param accentColor default sama persis dgn default `TactileSwitch` sendiri
 *   (`MaterialTheme.colorScheme.primary`) -- pemanggil yang sebelumnya pass
 *   `colors.primary` eksplisit menghasilkan nilai IDENTIK, aman dihilangkan.
 */
@Composable
fun ToggleRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.titleSmall,
    accentColor: Color = MaterialTheme.colorScheme.primary
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            style = style,
            modifier = Modifier.weight(1f).padding(end = 12.dp)
        )
        TactileSwitch(checked = checked, onCheckedChange = onCheckedChange, accentColor = accentColor)
    }
}
