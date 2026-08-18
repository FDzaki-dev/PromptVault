package com.elprompter.promptvault.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.Row
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.elprompter.promptvault.R
import com.elprompter.promptvault.data.Rule
import com.elprompter.promptvault.data.SaveRuleCheck
import com.elprompter.promptvault.ui.components.VaultActionSheet
import com.elprompter.promptvault.ui.components.VaultCard
import com.elprompter.promptvault.ui.components.VaultTopBar
import com.elprompter.promptvault.util.PatternPreviewResult
import com.elprompter.promptvault.util.validateRuleFolderName
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.UUID

@Composable
fun AddEditRuleScreen(
    existingRule: Rule?,
    onCheckBeforeSave: suspend (Rule) -> SaveRuleCheck,
    onPreviewPattern: suspend (String, String) -> PatternPreviewResult,
    onSave: (Rule, removeDuplicateRuleId: String?) -> Unit,
    onCancel: () -> Unit
) {
    var folderName by remember { mutableStateOf(existingRule?.folderName ?: "") }
    var pattern by remember { mutableStateOf(existingRule?.pattern ?: "") }
    var excludePattern by remember { mutableStateOf(existingRule?.excludePattern ?: "") }
    var minSizeKbText by remember { mutableStateOf(existingRule?.minSizeKb?.toString() ?: "") }
    var maxSizeKbText by remember { mutableStateOf(existingRule?.maxSizeKb?.toString() ?: "") }
    var pendingCheck by remember { mutableStateOf<SaveRuleCheck?>(null) }
    var pendingRule by remember { mutableStateOf<Rule?>(null) }
    var preview by remember { mutableStateOf<PatternPreviewResult?>(null) }

    val scope = rememberCoroutineScope()

    // [Fix P0-1 + P2-2, audit gap 2026-08-16 -- PromptVault_real_functional_polish_gap_audit.md]
    // SEBELUMNYA hanya `isNotBlank()` dicek di sini -- nama folder tidak
    // valid (mengandung "/"/"\"/".."/karakter provider-unsafe) baru ketahuan
    // BELAKANGAN saat file benar-benar dipindahkan (FileSorter.moveFile),
    // bukan saat rule disimpan. Validator yang sama ([validateRuleFolderName])
    // dipakai di sini DAN di FileSorter -- lihat KDoc lengkap di
    // RuleFolderNameValidator.kt kenapa dua lapis ini sama-sama wajib.
    val folderNameError = if (folderName.isBlank()) null else validateRuleFolderName(folderName)

    // Uji pattern secara live ke isi Downloads saat ini (debounce 400ms biar tidak
    // scan folder di tiap ketikan huruf). Ini yang menjawab keluhan "gak jelas kenapa
    // dilewati" -- user langsung lihat cocok/tidaknya SEBELUM menyimpan rule.
    LaunchedEffect(pattern, excludePattern) {
        if (pattern.isBlank()) {
            preview = null
        } else {
            delay(400)
            preview = onPreviewPattern(pattern.trim(), excludePattern.trim())
        }
    }

    Scaffold(
        topBar = {
            VaultTopBar(title = if (existingRule == null) stringResource(R.string.rule_edit_title_add) else stringResource(R.string.rule_edit_title_edit), onBack = onCancel)
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = folderName,
                onValueChange = { folderName = it },
                label = { Text(stringResource(R.string.rule_edit_folder_label)) },
                isError = folderNameError != null,
                supportingText = folderNameError?.let { error -> { Text(error) } },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = pattern,
                onValueChange = { pattern = it },
                label = { Text(stringResource(R.string.rule_edit_pattern_label)) },
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                stringResource(R.string.rule_edit_pattern_hint),
                style = MaterialTheme.typography.bodySmall
            )

            OutlinedTextField(
                value = excludePattern,
                onValueChange = { excludePattern = it },
                label = { Text(stringResource(R.string.rule_edit_exclude_label)) },
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                stringResource(R.string.rule_edit_exclude_hint),
                style = MaterialTheme.typography.bodySmall
            )

            Text(stringResource(R.string.rule_edit_size_filter_title), style = MaterialTheme.typography.titleSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = minSizeKbText,
                    onValueChange = { minSizeKbText = it.filter { c -> c.isDigit() } },
                    label = { Text(stringResource(R.string.rule_edit_min_kb_label)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = maxSizeKbText,
                    onValueChange = { maxSizeKbText = it.filter { c -> c.isDigit() } },
                    label = { Text(stringResource(R.string.rule_edit_max_kb_label)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
            }
            Text(
                stringResource(R.string.rule_edit_size_hint),
                style = MaterialTheme.typography.bodySmall
            )

            // Live preview: bukti langsung pattern ini akan kena file yang mana di Downloads.
            preview?.let { p ->
                VaultCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            stringResource(R.string.rule_edit_preview_summary, p.matchedFileNames.size, p.totalCandidateFiles),
                            style = MaterialTheme.typography.titleSmall
                        )
                        if (p.matchedFileNames.isEmpty() && p.totalCandidateFiles > 0) {
                            Text(
                                stringResource(R.string.rule_edit_preview_empty),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        LazyColumn(modifier = Modifier.heightIn(max = 180.dp)) {
                            items(p.matchedFileNames.take(10)) { name ->
                                Text(stringResource(R.string.rule_edit_preview_bullet, name), style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        if (p.matchedFileNames.size > 10) {
                            Text(stringResource(R.string.rule_edit_preview_more, p.matchedFileNames.size - 10), style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }

            Button(
                onClick = {
                    val rule = Rule(
                        id = existingRule?.id ?: UUID.randomUUID().toString(),
                        folderName = folderName.trim(),
                        pattern = pattern.trim(),
                        excludePattern = excludePattern.trim(),
                        minSizeKb = minSizeKbText.toLongOrNull(),
                        maxSizeKb = maxSizeKbText.toLongOrNull()
                    )
                    scope.launch {
                        val check = onCheckBeforeSave(rule)
                        if (check is SaveRuleCheck.Ok) {
                            onSave(rule, null)
                        } else {
                            pendingCheck = check
                            pendingRule = rule
                        }
                    }
                },
                enabled = folderName.isNotBlank() && folderNameError == null && pattern.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary, contentColor = MaterialTheme.colorScheme.onSecondary),
                modifier = Modifier.fillMaxWidth()
            ) { Text(stringResource(R.string.action_save)) }
        }
    }

    val check = pendingCheck
    val rule = pendingRule
    if (check != null && rule != null) {
        val message = when (check) {
            is SaveRuleCheck.DuplicatePattern ->
                // Konfirmasi sebelum menimpa pattern yang sama (fitur lengkap).
                stringResource(R.string.rule_edit_confirm_duplicate, rule.pattern, check.existing.folderName)
            is SaveRuleCheck.OverlapsWithOthers ->
                // Peringatan rule tumpang tindih sebelum disimpan (fitur lengkap).
                stringResource(R.string.rule_edit_confirm_overlap, check.overlapping.joinToString { it.folderName })
            SaveRuleCheck.Ok -> ""
        }
        VaultActionSheet(
            title = stringResource(R.string.rule_edit_confirm_title),
            message = message,
            confirmLabel = stringResource(R.string.rule_edit_confirm_button),
            onConfirm = {
                // Batch [duplicate-fix]: untuk DuplicatePattern, "Tetap Simpan" harus
                // benar-benar menimpa (hapus rule lama, id-nya beda dari rule baru).
                // Untuk OverlapsWithOthers, kedua rule memang dimaksud tetap
                // hidup berdampingan (prioritas urutan yang menentukan pemenang),
                // jadi tidak ada yang dihapus.
                val removeId = (check as? SaveRuleCheck.DuplicatePattern)?.existing?.id
                onSave(rule, removeId)
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
