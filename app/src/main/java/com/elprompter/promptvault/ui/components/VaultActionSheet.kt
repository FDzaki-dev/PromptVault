package com.elprompter.promptvault.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
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
import com.elprompter.promptvault.ui.theme.CardPaper
import com.elprompter.promptvault.ui.theme.Kraft
import com.elprompter.promptvault.ui.theme.Pine
import com.elprompter.promptvault.ui.theme.Stamp

/**
 * Pengganti AlertDialog kotak di tengah layar -- muncul dari bawah seperti
 * action sheet iOS. Untuk konfirmasi aksi (hapus, undo, dsb) yang butuh
 * perhatian penuh tapi tetap terasa ringan, bukan modal yang "mengunci" layar.
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
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Kraft
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(message, style = MaterialTheme.typography.bodyMedium)

            Button(
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onConfirm()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isDestructive) Stamp else Pine,
                    contentColor = CardPaper
                ),
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            ) { Text(confirmLabel) }

            OutlinedButton(
                onClick = onDismiss,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Pine),
                modifier = Modifier.fillMaxWidth()
            ) { Text(dismissLabel) }
        }
    }
}
