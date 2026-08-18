package com.elprompter.promptvault.ui.screens

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.elprompter.promptvault.ui.components.EmptyState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.elprompter.promptvault.R
import com.elprompter.promptvault.data.Rule
import com.elprompter.promptvault.ui.MainViewModel
import com.elprompter.promptvault.ui.components.VaultActionSheet
import com.elprompter.promptvault.ui.components.RuleCard
import com.elprompter.promptvault.ui.components.VaultTopBar
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RuleListScreen(
    rules: List<Rule>,
    overlappingRuleIds: Set<String>,
    onToggleEnabled: (Rule, Boolean) -> Unit,
    onMoveUp: (Rule) -> Unit,
    onMoveDown: (Rule) -> Unit,
    onEditRule: (Rule) -> Unit,
    onDeleteRule: (Rule) -> Unit,
    onAddRule: () -> Unit,
    ruleSaveFeedback: MainViewModel.RuleSaveFeedback?,
    onRuleSaveFeedbackConsumed: () -> Unit,
    onBack: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    var pendingDelete by remember { mutableStateOf<Rule?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val colors = MaterialTheme.colorScheme
    // [Roadmap Fase 1.3, 2026-08-18] `stringResource()` HANYA valid di
    // konteks composable langsung -- `LaunchedEffect`/`onConfirm` di bawah
    // adalah lambda yang dieksekusi BELAKANGAN (bukan saat komposisi), jadi
    // WAJIB pakai `context.getString(...)` (fungsi biasa, bukan @Composable)
    // utk string dinamis di dalamnya, bukan `stringResource()`.
    val context = LocalContext.current

    // v2.16.0 -- technical debt closure: konfirmasi sukses simpan rule (baru
    // ATAU edit) dari AddEditRuleScreen. Dikonsumsi di SINI (bukan di layar
    // form itu sendiri) karena form sudah di-pop dari back stack sebelum
    // Snackbar sempat tampil -- lihat javadoc MainViewModel.RuleSaveFeedback.
    LaunchedEffect(ruleSaveFeedback?.eventId) {
        val feedback = ruleSaveFeedback ?: return@LaunchedEffect
        onRuleSaveFeedbackConsumed()
        snackbarHostState.showSnackbar(context.getString(R.string.rule_list_saved_snackbar, feedback.folderName))
    }

    val filtered = remember(rules, query) {
        if (query.isBlank()) rules
        else rules.filter {
            it.folderName.contains(query, ignoreCase = true) || it.pattern.contains(query, ignoreCase = true)
        }
    }
    // Reorder prioritas cuma bermakna kalau daftar tidak lagi difilter pencarian.
    val reorderEnabled = query.isBlank()

    Scaffold(
        topBar = { VaultTopBar(title = stringResource(R.string.rule_list_title), onBack = onBack) },
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                Snackbar(snackbarData = data, containerColor = colors.primary, contentColor = colors.onPrimary)
            }
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddRule, containerColor = colors.primary, contentColor = colors.onPrimary) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.rule_list_add_cd))
            }
        }
    ) { padding ->
        Column(modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(16.dp)) {
            Text(
                stringResource(R.string.rule_list_priority_hint),
                style = MaterialTheme.typography.bodySmall
            )
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text(stringResource(R.string.rule_list_search_label)) },
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
            )

            Crossfade(targetState = filtered.isEmpty(), label = "ruleListEmptyState", animationSpec = tween(220)) { isEmpty ->
                if (isEmpty) {
                    if (rules.isEmpty()) {
                        EmptyState(
                            icon = Icons.Filled.PlaylistAdd,
                            title = stringResource(R.string.rule_list_empty_title),
                            message = stringResource(R.string.rule_list_empty_message),
                            accentColor = colors.primary,
                            accentContainerColor = colors.primaryContainer
                        )
                    } else {
                        EmptyState(
                            icon = Icons.Filled.SearchOff,
                            title = stringResource(R.string.rule_list_not_found_title),
                            message = stringResource(R.string.rule_list_not_found_message, query),
                            accentColor = colors.primary,
                            accentContainerColor = colors.primaryContainer
                        )
                    }
                } else {
                    // v2.24.0 fix (#UI-20): LazyColumn sebelumnya TANPA contentPadding
                    // bawah -- item terakhir (khususnya tombol Edit/Hapus di
                    // RuleCard) ketutup FloatingActionButton "+" yang MELAYANG di
                    // atas konten (Scaffold TIDAK otomatis menghindarkan FAB dari
                    // content padding kecuali diberi manual, ini bukan bug Scaffold
                    // tapi kelalaian pemanggil). 88dp = tinggi standar FAB M3 (56dp)
                    // + margin aman supaya baris aksi kartu terakhir tetap bisa
                    // di-tap penuh & tidak ketutup optik.
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 88.dp)
                    ) {
                        items(filtered, key = { it.id }) { rule ->
                            val globalIndex = rules.indexOfFirst { it.id == rule.id }
                            RuleCard(
                                modifier = androidx.compose.ui.Modifier.animateItemPlacement(),
                                rule = rule,
                                priority = globalIndex + 1,
                                hasOverlapWarning = overlappingRuleIds.contains(rule.id),
                                canMoveUp = reorderEnabled && globalIndex > 0,
                                canMoveDown = reorderEnabled && globalIndex < rules.lastIndex,
                                onToggleEnabled = { onToggleEnabled(rule, it) },
                                onMoveUp = { onMoveUp(rule) },
                                onMoveDown = { onMoveDown(rule) },
                                onEdit = { onEditRule(rule) },
                                onDelete = { pendingDelete = rule }
                            )
                        }
                    }
                }
            }
        }
    }

    pendingDelete?.let { rule ->
        VaultActionSheet(
            title = stringResource(R.string.rule_list_delete_title),
            message = stringResource(R.string.rule_list_delete_message, rule.folderName, rule.pattern),
            confirmLabel = stringResource(R.string.action_delete),
            isDestructive = true,
            onConfirm = {
                onDeleteRule(rule)
                pendingDelete = null
                scope.launch { snackbarHostState.showSnackbar(context.getString(R.string.rule_list_deleted_snackbar, rule.folderName)) }
            },
            onDismiss = { pendingDelete = null }
        )
    }
}
