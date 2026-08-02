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
import androidx.compose.ui.unit.dp
import com.elprompter.promptvault.data.Rule
import com.elprompter.promptvault.data.SaveRuleCheck
import com.elprompter.promptvault.ui.components.VaultActionSheet
import com.elprompter.promptvault.ui.components.VaultCard
import com.elprompter.promptvault.ui.components.VaultTopBar
import com.elprompter.promptvault.util.PatternPreviewResult
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.UUID

@Composable
fun AddEditRuleScreen(
    existingRule: Rule?,
    onCheckBeforeSave: suspend (Rule) -> SaveRuleCheck,
    onPreviewPattern: (String, String) -> PatternPreviewResult,
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
            VaultTopBar(title = if (existingRule == null) "Tambah Rule" else "Edit Rule", onBack = onCancel)
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

            OutlinedTextField(
                value = excludePattern,
                onValueChange = { excludePattern = it },
                label = { Text("Kecualikan pattern ini (opsional)") },
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                "File yang cocok pattern utama TAPI juga cocok pattern kecuali ini tidak akan dipindahkan. " +
                    "Contoh: pattern *.zip, kecualikan backup_*.zip.",
                style = MaterialTheme.typography.bodySmall
            )

            Text("Filter Ukuran File (opsional)", style = MaterialTheme.typography.titleSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = minSizeKbText,
                    onValueChange = { minSizeKbText = it.filter { c -> c.isDigit() } },
                    label = { Text("Min (KB)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = maxSizeKbText,
                    onValueChange = { maxSizeKbText = it.filter { c -> c.isDigit() } },
                    label = { Text("Maks (KB)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
            }
            Text(
                "Kosongkan kalau tidak perlu batasan ukuran. Contoh pakai: hindari file kosong (min 1 KB) " +
                    "atau hindari file raksasa yang bikin auto-scan lama (maks 50000 KB).",
                style = MaterialTheme.typography.bodySmall
            )

            // Live preview: bukti langsung pattern ini akan kena file yang mana di Downloads.
            preview?.let { p ->
                VaultCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            "${p.matchedFileNames.size} dari ${p.totalCandidateFiles} file ZIP/TXT di Downloads cocok pattern ini",
                            style = MaterialTheme.typography.titleSmall
                        )
                        if (p.matchedFileNames.isEmpty() && p.totalCandidateFiles > 0) {
                            Text(
                                "Tidak ada yang cocok. Cek lagi ejaan/format pattern-nya, atau buka Diagnostik untuk lihat nama file asli di Downloads.",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        LazyColumn(modifier = Modifier.heightIn(max = 180.dp)) {
                            items(p.matchedFileNames.take(10)) { name ->
                                Text("• $name", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        if (p.matchedFileNames.size > 10) {
                            Text("+ ${p.matchedFileNames.size - 10} file lainnya", style = MaterialTheme.typography.labelSmall)
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
                enabled = folderName.isNotBlank() && pattern.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary, contentColor = MaterialTheme.colorScheme.onSecondary),
                modifier = Modifier.fillMaxWidth()
            ) { Text("Simpan") }
        }
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
        VaultActionSheet(
            title = "Perlu konfirmasi",
            message = message,
            confirmLabel = "Tetap Simpan",
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
