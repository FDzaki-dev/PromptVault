package com.elprompter.promptvault.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.Row
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.elprompter.promptvault.R
import com.elprompter.promptvault.data.Rule
import com.elprompter.promptvault.data.SaveRuleCheck
import com.elprompter.promptvault.ui.components.TactileSwitch
import com.elprompter.promptvault.ui.components.VaultActionSheet
import com.elprompter.promptvault.ui.components.VaultCard
import com.elprompter.promptvault.ui.components.VaultTopBar
import com.elprompter.promptvault.util.PatternPreviewResult
import com.elprompter.promptvault.util.validateRuleFolderName
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * [next pending, 2026-08-21 -- "tambahkan preset cepat khusus tab tambah
 * rule, biar user awam ada gambaran gimana mekanisme rule sortir file yang
 * benar"] 6 preset umum, HANYA tampil saat TAMBAH rule baru (bukan edit --
 * lihat gate `existingRule == null` di composable). Tap = isi `folderName`+
 * `pattern` OTOMATIS (tetap bisa diedit manual sebelum Simpan) -- exclude
 * pattern & filter ukuran SENGAJA tidak disentuh preset, biar tidak menimpa
 * apa pun yang sudah diisi user di 2 field itu.
 *
 * Nilai edukasi (tujuan utama fitur ini, bukan cuma shortcut): begitu
 * ditap, user LANGSUNG lihat pattern CSV multi-ekstensi (mis.
 * "*.jpg, *.jpeg, *.png") di field pattern yang SAMA dgn yang mereka akan
 * ketik manual, DAN live preview di bawah (sudah ada, [onPreviewPattern])
 * otomatis jalan tunjukkan file Downloads mana yang benar-benar cocok --
 * loop lengkap "pattern -> folder -> bukti file cocok" tanpa perlu
 * dijelaskan lewat teks panjang di Onboarding/Panduan.
 *
 * "Screenshot" SENGAJA beda gaya dari 5 preset lain (prefix nama file,
 * BUKAN cuma ekstensi) -- supaya user awam lihat 2 gaya pattern yang valid
 * (ekstensi generik vs prefix nama spesifik), bukan cuma satu pola yang
 * bisa disalahpahami sbg "cara satu-satunya".
 */
private data class RulePreset(@androidx.annotation.StringRes val labelRes: Int, val folder: String, val pattern: String)

private val rulePresets = listOf(
    RulePreset(R.string.rule_edit_preset_gambar, "Gambar", "*.jpg, *.jpeg, *.png, *.webp, *.heic"),
    RulePreset(R.string.rule_edit_preset_pdf, "PDF", "*.pdf"),
    RulePreset(R.string.rule_edit_preset_video, "Video", "*.mp4, *.mkv, *.mov, *.3gp"),
    RulePreset(R.string.rule_edit_preset_arsip, "Arsip", "*.zip, *.rar, *.7z"),
    RulePreset(R.string.rule_edit_preset_dokumen, "Dokumen", "*.doc, *.docx, *.xls, *.xlsx, *.ppt, *.pptx"),
    RulePreset(R.string.rule_edit_preset_screenshot, "Screenshot", "Screenshot_*.png, Screenshot_*.jpg")
)

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AddEditRuleScreen(
    existingRule: Rule?,
    onCheckBeforeSave: suspend (Rule) -> SaveRuleCheck,
    onPreviewPattern: suspend (String, String) -> PatternPreviewResult,
    onSave: (Rule, removeDuplicateRuleId: String?) -> Unit,
    onCancel: () -> Unit
) {
    // Audit UX 100% batch 6 (area baru: state restoration): field ketikan
    // user sebelumnya `remember` biasa -- HILANG total saat rotasi layar
    // atau process death (app di-background lalu dibunuh sistem, umum di
    // Android saat RAM rendah). `rememberSaveable` menyimpan ke
    // savedInstanceState (Bundle), bertahan lewat keduanya. `pendingCheck`/
    // `pendingRule`/`preview` TETAP `remember` biasa -- objek kompleks
    // (Rule/SaveRuleCheck/PatternPreviewResult) tidak otomatis Bundle-safe
    // tanpa Parcelize tambahan, dan isinya derived/transient (bisa dihitung
    // ulang dari field di atas), bukan ketikan user yang butuh diselamatkan.
    var folderName by rememberSaveable { mutableStateOf(existingRule?.folderName ?: "") }
    var pattern by rememberSaveable { mutableStateOf(existingRule?.pattern ?: "") }
    var excludePattern by rememberSaveable { mutableStateOf(existingRule?.excludePattern ?: "") }
    var minSizeKbText by rememberSaveable { mutableStateOf(existingRule?.minSizeKb?.toString() ?: "") }
    var maxSizeKbText by rememberSaveable { mutableStateOf(existingRule?.maxSizeKb?.toString() ?: "") }
    // [Batch 2, 2026-08-26] Toggle per-rule "tahan versi .zip terbaru" --
    // lihat KDoc lengkap di Rule.holdBackLatestZip/FileSorter.
    // computeLatestZipHeldBack(). rememberSaveable, pola SAMA PERSIS dgn
    // field ketikan lain di atas (bertahan lewat rotasi/process death).
    var holdBackLatestZip by rememberSaveable { mutableStateOf(existingRule?.holdBackLatestZip ?: false) }
    var pendingCheck by remember { mutableStateOf<SaveRuleCheck?>(null) }
    var pendingRule by remember { mutableStateOf<Rule?>(null) }
    var preview by remember { mutableStateOf<PatternPreviewResult?>(null) }
    // Pending queue v8.15.0: guard double-tap "Simpan" -- pola sama seperti
    // undoInFlight di ActivityLogScreen.kt. Tanpa ini, tap cepat 2x sebelum
    // onCheckBeforeSave (suspend) selesai bisa trigger 2 proses cek/simpan
    // bertumpuk.
    var isSaving by remember { mutableStateOf(false) }

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
            if (existingRule == null) {
                Text(stringResource(R.string.rule_edit_preset_title), style = MaterialTheme.typography.titleSmall)
                Text(
                    stringResource(R.string.rule_edit_preset_hint),
                    style = MaterialTheme.typography.bodySmall
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    rulePresets.forEach { preset ->
                        AssistChip(
                            onClick = {
                                folderName = preset.folder
                                pattern = preset.pattern
                            },
                            label = { Text(stringResource(preset.labelRes)) }
                        )
                    }
                }
            }

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

            // [Batch 2, 2026-08-26] Selalu tampil (bukan kondisional cek
            // substring ".zip" di pattern) -- keputusan sengaja: pattern
            // bisa multi-ekstensi CSV (mis. "*.zip, *.rar") dan user bisa
            // ubah pattern belakangan tanpa toggle ini hilang/reset diam-
            // diam. Hint text di bawah sudah jelaskan syarat aktualnya
            // (scope .zip+SAF saja) drpd disembunyikan/dikondisikan UI.
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    stringResource(R.string.rule_edit_hold_back_zip_title),
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f).padding(end = 12.dp)
                )
                TactileSwitch(checked = holdBackLatestZip, onCheckedChange = { holdBackLatestZip = it })
            }
            Text(
                stringResource(R.string.rule_edit_hold_back_zip_hint),
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
                        maxSizeKb = maxSizeKbText.toLongOrNull(),
                        holdBackLatestZip = holdBackLatestZip
                    )
                    isSaving = true
                    scope.launch {
                        // [Fix bug lanjutan, laporan user 2026-08-27] checkBeforeSave
                        // (duplicate pattern + overlap) HANYA bergantung pada field
                        // `pattern` -- lihat RuleOverlapChecker.findOverlaps() &
                        // RuleRepository.checkBeforeSave(), keduanya TIDAK PERNAH baca
                        // holdBackLatestZip/excludePattern/minSizeKb/maxSizeKb/
                        // folderName/enabled. Kalau user edit rule LAMA tanpa ubah
                        // pattern sama sekali (mis. cuma nyalakan toggle "Tahan versi
                        // .zip terbaru"), kondisi tumpang tindih/duplikat SUDAH PERSIS
                        // sama dgn saat rule ini terakhir disimpan -- minta konfirmasi
                        // ULANG di sini cuma friksi, dan kalau user tidak sadar HARUS
                        // tap "Tetap Simpan" (bukan cuma baca lalu keluar/back), SELURUH
                        // perubahan form (termasuk toggle) hilang diam-diam tanpa
                        // pernah tersimpan -- persis root cause laporan bug user.
                        // Overlap yang SUDAH ada TETAP kelihatan lewat badge peringatan
                        // di RuleListScreen (hasOverlapWarning), jadi user tidak
                        // kehilangan info apa pun dgn skip ini.
                        val patternUnchanged = existingRule != null && existingRule.pattern == rule.pattern
                        val check = if (patternUnchanged) SaveRuleCheck.Ok else onCheckBeforeSave(rule)
                        isSaving = false
                        if (check is SaveRuleCheck.Ok) {
                            onSave(rule, null)
                        } else {
                            pendingCheck = check
                            pendingRule = rule
                        }
                    }
                },
                enabled = folderName.isNotBlank() && folderNameError == null && pattern.isNotBlank() && !isSaving,
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
