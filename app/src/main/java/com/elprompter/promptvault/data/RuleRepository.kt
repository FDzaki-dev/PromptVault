package com.elprompter.promptvault.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Hasil pengecekan sebelum menyimpan rule baru/edit.
 */
sealed class SaveRuleCheck {
    object Ok : SaveRuleCheck()
    data class DuplicatePattern(val existing: Rule) : SaveRuleCheck()      // fitur lengkap, dipakai AddEditRuleScreen
    data class OverlapsWithOthers(val overlapping: List<Rule>) : SaveRuleCheck() // fitur lengkap, dipakai AddEditRuleScreen
}

class RuleRepository(private val context: Context) {

    private val key = stringPreferencesKey("rules_json")
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }

    val rulesFlow: Flow<List<Rule>> = context.promptVaultDataStore.data.map { prefs ->
        val raw = prefs[key] ?: "[]"
        runCatching { json.decodeFromString<List<Rule>>(raw) }.getOrDefault(emptyList())
    }

    suspend fun getRules(): List<Rule> = rulesFlow.first()

    /** Cek duplikat pattern & overlap sebelum benar-benar menyimpan (dipakai UI utk konfirmasi). */
    suspend fun checkBeforeSave(candidate: Rule): SaveRuleCheck {
        val rules = getRules().filter { it.id != candidate.id }
        val duplicate = rules.firstOrNull { it.pattern.equals(candidate.pattern, ignoreCase = true) }
        if (duplicate != null) return SaveRuleCheck.DuplicatePattern(duplicate)

        val overlaps = com.elprompter.promptvault.util.RuleOverlapChecker.findOverlaps(candidate, rules)
        if (overlaps.isNotEmpty()) return SaveRuleCheck.OverlapsWithOthers(overlaps)

        return SaveRuleCheck.Ok
    }

    suspend fun upsertRule(rule: Rule) {
        val current = getRules().toMutableList()
        val idx = current.indexOfFirst { it.id == rule.id }
        if (idx >= 0) current[idx] = rule else current.add(rule)
        persist(current)
    }

    /**
     * Batch [duplicate-fix]: dipakai saat user menekan "Tetap Simpan" pada
     * dialog konfirmasi DUPLICATE PATTERN. Sebelumnya alur ini cuma memanggil
     * [upsertRule] biasa dengan id BARU (untuk rule baru) -- padahal dialognya
     * bilang "Timpa rule tersebut?". Hasilnya: rule lama TIDAK terhapus, malah
     * jadi 2 rule dengan pattern identik. [removeRuleId] adalah id rule LAMA
     * yang harus hilang supaya "Timpa" benar-benar berarti timpa satu rule,
     * bukan tambah rule baru di sebelah yang lama.
     * Dilakukan dalam SATU baca-ubah-simpan (bukan panggil deleteRule() lalu
     * upsertRule() terpisah) supaya tidak ada celah race antara dua operasi
     * DataStore yang independen.
     */
    suspend fun upsertRule(rule: Rule, removeRuleId: String?) {
        val current = getRules().toMutableList()
        if (removeRuleId != null) current.removeAll { it.id == removeRuleId }
        val idx = current.indexOfFirst { it.id == rule.id }
        if (idx >= 0) current[idx] = rule else current.add(rule)
        persist(current)
    }

    suspend fun deleteRule(ruleId: String) {
        val current = getRules().filterNot { it.id == ruleId }
        persist(current)
    }

    /**
     * Urutan list di storage INI YANG MENENTUKAN prioritas rule saat file cocok
     * lebih dari satu rule (rule dengan index lebih kecil menang). Sebelumnya
     * urutan ini "tersembunyi" (cuma urutan penyimpanan apa adanya) -- sekarang
     * user bisa mengatur naik/turun secara eksplisit lewat dua fungsi ini.
     */
    suspend fun moveRuleUp(ruleId: String) {
        val current = getRules().toMutableList()
        val idx = current.indexOfFirst { it.id == ruleId }
        if (idx > 0) {
            val tmp = current[idx - 1]
            current[idx - 1] = current[idx]
            current[idx] = tmp
            persist(current)
        }
    }

    suspend fun moveRuleDown(ruleId: String) {
        val current = getRules().toMutableList()
        val idx = current.indexOfFirst { it.id == ruleId }
        if (idx in 0 until current.lastIndex) {
            val tmp = current[idx + 1]
            current[idx + 1] = current[idx]
            current[idx] = tmp
            persist(current)
        }
    }

    suspend fun findAllOverlaps(): Map<Rule, List<Rule>> {
        val rules = getRules()
        val result = mutableMapOf<Rule, List<Rule>>()
        for (r in rules) {
            val overlaps = com.elprompter.promptvault.util.RuleOverlapChecker.findOverlaps(r, rules.filter { it.id != r.id })
            if (overlaps.isNotEmpty()) result[r] = overlaps
        }
        return result
    }

    private suspend fun persist(rules: List<Rule>) {
        context.promptVaultDataStore.edit { prefs ->
            prefs[key] = json.encodeToString(rules)
        }
    }

    /** Ekspor semua rule sebagai teks JSON (fitur lengkap -- backup/export via Pengaturan). */
    suspend fun exportAsJson(): String = json.encodeToString(getRules())

    /**
     * Impor rule dari teks JSON hasil export. Menggabungkan (merge) berdasarkan id.
     *
     * [Fix audit P2 #UI-13, 2026-08-15] Sebelumnya cuma `Int` (jumlah rule
     * ter-import) -- tidak bisa dibedakan "0 karena JSON tidak valid/parse
     * gagal" vs "0 karena JSON valid tapi array kosong". Sekarang return
     * [ImportOutcome] eksplisit: [ImportOutcome.parseSuccess] = false HANYA
     * kalau `jsonText` gagal di-decode sama sekali (format salah), TETAP true
     * walau hasil array kosong (itu bukan error, cuma tidak ada yang diimpor).
     */
    data class ImportOutcome(val parseSuccess: Boolean, val importedCount: Int)

    suspend fun importFromJson(jsonText: String): ImportOutcome {
        val parseResult = runCatching { json.decodeFromString<List<Rule>>(jsonText) }
        val imported = parseResult.getOrNull()
            ?: return ImportOutcome(parseSuccess = false, importedCount = 0)
        if (imported.isEmpty()) return ImportOutcome(parseSuccess = true, importedCount = 0)
        val current = getRules().associateBy { it.id }.toMutableMap()
        imported.forEach { current[it.id] = it }
        persist(current.values.toList())
        return ImportOutcome(parseSuccess = true, importedCount = imported.size)
    }
}
