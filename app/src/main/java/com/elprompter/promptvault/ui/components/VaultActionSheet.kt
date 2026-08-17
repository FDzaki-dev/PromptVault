package com.elprompter.promptvault.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp

/**
 * Pengganti AlertDialog kotak di tengah layar -- muncul dari bawah sebagai
 * action sheet. Untuk konfirmasi aksi (hapus, undo, dsb) yang butuh
 * perhatian penuh tapi tetap terasa ringan, bukan modal yang "mengunci" layar.
 * containerColor pakai surfaceVariant (lapisan "terangkat") supaya sheet
 * terasa mengambang di atas layar, terutama kontras jelas di dark mode.
 *
 * v8.0.0 -- Glassmorphism -> Material 3 murni: grabber pill tetap bentuk
 * cekung di tengah-atas ([TactileSurface] `recessed = true`, isi
 * `colorScheme.surfaceContainerLowest`, peran M3 baku) -- pola drag-handle
 * standar, sekarang lewat primitif Surface M3 yang sama dengan seluruh app.
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
        // tonalElevation=0 -- sheet tetap terbaca sbg permukaan neumorphic
        // tenang di atas AMOLED, bukan panel Material solid dgn tonal tint.
        // Catatan: ModalBottomSheet pada material3-bom 2024.06.00 tidak
        // memiliki parameter shadowElevation (hanya Surface yang punya).
        tonalElevation = 0.dp
    ) {
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            TactileSurface(
                modifier = Modifier
                    .padding(top = 10.dp, bottom = 4.dp)
                    .size(width = 36.dp, height = 4.dp),
                shape = RoundedCornerShape(50),
                color = colors.surfaceContainerLowest,
                recessed = true,
                content = {}
            )
        }
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
