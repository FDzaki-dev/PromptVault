package com.elprompter.promptvault.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.elprompter.promptvault.ui.theme.GlassBorder

/**
 * Pengganti AlertDialog kotak di tengah layar -- muncul dari bawah seperti
 * action sheet iOS. Untuk konfirmasi aksi (hapus, undo, dsb) yang butuh
 * perhatian penuh tapi tetap terasa ringan, bukan modal yang "mengunci" layar.
 * containerColor pakai surfaceVariant (lapisan "terangkat") supaya sheet
 * terasa mengambang di atas layar, terutama kontras jelas di dark mode.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultActionSheet(
    title: String,
    message: String,
    confirmLabel: String = "Lanjutkan",
    dismissLabel: String = "Batal",
    isDestructive: Boolean = false,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    val haptics = LocalHapticFeedback.current
    val colors = MaterialTheme.colorScheme
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.surfaceVariant,
        // shadowElevation=0 + border rambut manual di bawah (bukan tonal
        // elevation Material default) -- sheet tetap terbaca sebagai lapisan
        // glass tipis di atas AMOLED (bab 7), bukan panel Material solid.
        shadowElevation = 0.dp
    ) {
        // Highlight rambut tunggal di TOP (bab 8/9: arah cahaya kiri-atas ->
        // kanan-bawah, "reflected light" bukan garis outline penuh) -- satu
        // isyarat kecil supaya sheet tetap terbaca sebagai lapisan glass
        // yang mengambang, bukan panel Material solid tanpa tepi sama sekali.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(GlassBorder)
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(top = 12.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = colors.onSurface)
            Text(message, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)

            Button(
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onConfirm()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isDestructive) colors.error else colors.primary,
                    contentColor = if (isDestructive) colors.onError else colors.onPrimary
                ),
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            ) { Text(confirmLabel) }

            OutlinedButton(
                onClick = onDismiss,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.primary),
                modifier = Modifier.fillMaxWidth()
            ) { Text(dismissLabel) }
        }
    }
}
