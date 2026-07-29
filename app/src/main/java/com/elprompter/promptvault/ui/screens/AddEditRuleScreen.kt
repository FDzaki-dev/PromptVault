package com.elprompter.promptvault.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.elprompter.promptvault.data.Rule
import com.elprompter.promptvault.data.SaveRuleCheck
import com.elprompter.promptvault.ui.components.ConfirmDialog
import kotlinx.coroutines.launch
import java.util.UUID

@Composable
fun AddEditRuleScreen(
    existingRule: Rule?,
    onCheckBeforeSave: suspend (Rule) -> SaveRuleCheck,
    onSave: (Rule) -> Unit,
    onCancel: () -> Unit
) {
    var folderName by remember { mutableStateOf(existingRule?.folderName ?: "") }
    var pattern by remember { mutableStateOf(existingRule?.pattern ?: "") }
    var pendingCheck by remember { mutableStateOf<SaveRuleCheck?>(null) }
    var pendingRule by remember { mutableStateOf<Rule?>(null) }

    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(if (existingRule == null) "Tambah Rule" else "Edit Rule", style = MaterialTheme.typography.headlineSmall)

        OutlinedTextField(
            value = folderName,
            onValueChange = { folderName = it },
            label = { Text("Nama folder tujuan") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = pattern,
            onValueChange = { pattern = it },
            label = { Text("Pattern (mis. invoice_*.zip)") },
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            "Gunakan * untuk banyak karakter dan ? untuk satu karakter. Contoh: *.txt, laporan_*.zip",
            style = MaterialTheme.typography.bodySmall
        )

        Button(
            onClick = {
                val rule = Rule(
                    id = existingRule?.id ?: UUID.randomUUID().toString(),
                    folderName = folderName.trim(),
                    pattern = pattern.trim()
                )
                scope.launch {
                    val check = onCheckBeforeSave(rule)
                    if (check is SaveRuleCheck.Ok) {
                        onSave(rule)
                    } else {
                        pendingCheck = check
                        pendingRule = rule
                    }
                }
            },
            enabled = folderName.isNotBlank() && pattern.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
        ) { Text("Simpan") }

        Button(onClick = onCancel, modifier = Modifier.fillMaxWidth()) { Text("Batal") }
    }

    val check = pendingCheck
    val rule = pendingRule
    if (check != null && rule != null) {
        val message = when (check) {
            is SaveRuleCheck.DuplicatePattern ->
                // TODO #9: konfirmasi sebelum menimpa pattern yang sama, tidak lagi diam-diam.
                "Pattern \"${rule.pattern}\" sudah dipakai rule \"${check.existing.folderName}\". Timpa rule tersebut?"
            is SaveRuleCheck.OverlapsWithOthers ->
                // TODO #3: peringatan rule tumpang tindih sebelum disimpan.
                "Pattern ini bisa tumpang tindih dengan: ${check.overlapping.joinToString { it.folderName }}. " +
                    "File yang cocok di keduanya akan memakai rule yang lebih dulu terdaftar. Tetap simpan?"
            SaveRuleCheck.Ok -> ""
        }
        ConfirmDialog(
            title = "Perlu konfirmasi",
            message = message,
            confirmLabel = "Tetap Simpan",
            onConfirm = {
                onSave(rule)
                pendingCheck = null
                pendingRule = null
            },
            onDismiss = {
                pendingCheck = null
                pendingRule = null
            }
        )
    }
}
