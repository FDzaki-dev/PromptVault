package com.elprompter.promptvault.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.elprompter.promptvault.data.ThemeStyleOption
import com.elprompter.promptvault.ui.theme.CupertinoTokens
import com.elprompter.promptvault.ui.theme.VaultTheme

/**
 * (v8.31.5, lanjut sempurnakan Cupertino -- item pending "custom dialog
 * non-actionsheet" dari [CupertinoTokens]) Pengganti `AlertDialog` M3 polos
 * -- grep menyeluruh dikonfirmasi SATU-SATUNYA titik pakai di seluruh app
 * (`DiagnosticsScreen.kt`, penampil isi crash log) yang MASIH bypass total
 * sistem tema, kelas bug SAMA PERSIS dgn `WarningBanner` sebelum v8.29.0.
 *
 * [TactileSurface] jadi wadah -- OTOMATIS dapat treatment gaya aktif
 * (translucent+sheen Glass / shadow+border Neumorphism / flat elevasi M3
 * murni / flat+hairline+radius besar Cupertino) tanpa logic baru di sini,
 * persis kartu lain di app. `shape = MaterialTheme.shapes.extraLarge`
 * (SAMA dgn shape default `AlertDialogDefaults` M3 yang digantikan --
 * gaya non-Cupertino jadi 0 berubah visual dari radius, cuma dapat
 * border/elevasi konsisten tema; Cupertino otomatis dapat radius besar
 * lewat [com.elprompter.promptvault.ui.theme.CupertinoShapes] yang sudah
 * dipasang kondisional di `Theme.kt`, 0 override manual perlu di sini).
 *
 * Tombol tunggal (`TextButton`, konvensi M3 baku utk aksi dialog
 * non-destruktif) -- 0 percabangan gaya diperlukan di situ, beda dgn
 * [VaultActionSheet] yang py aksi destruktif/multi-tombol. Khusus
 * Cupertino: `HorizontalDivider` hairline dipisah SEBELUM tombol --
 * signature `UIAlertController` asli (pemisah tipis, bukan spasi kosong).
 */
@Composable
fun VaultAlertDialog(
    title: String,
    dismissLabel: String,
    onDismissRequest: () -> Unit,
    content: @Composable () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val isCupertino = VaultTheme.style == ThemeStyleOption.CUPERTINO
    Dialog(onDismissRequest = onDismissRequest) {
        TactileSurface(
            shape = MaterialTheme.shapes.extraLarge,
            color = colors.surfaceContainerHigh
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(title, style = MaterialTheme.typography.titleSmall, color = colors.onSurface)
                content()
                if (isCupertino) {
                    HorizontalDivider(
                        thickness = CupertinoTokens.HairlineWidth,
                        color = CupertinoTokens.hairlineColor()
                    )
                }
                TextButton(
                    onClick = onDismissRequest,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text(
                        dismissLabel,
                        color = colors.primary,
                        fontWeight = if (isCupertino) FontWeight.SemiBold else FontWeight.Normal
                    )
                }
            }
        }
    }
}
