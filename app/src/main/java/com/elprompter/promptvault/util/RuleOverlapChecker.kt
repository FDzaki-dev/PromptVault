package com.elprompter.promptvault.util

import com.elprompter.promptvault.data.Rule

/**
 * TODO #3: rule tumpang tindih sebelumnya ditangani diam-diam (ambil rule pertama
 * yang cocok tanpa peringatan). Sekarang kita deteksi & tampilkan peringatan ke user,
 * baik saat menyimpan rule baru maupun saat scan menemukan file yang cocok >1 rule.
 */
object RuleOverlapChecker {

    fun findOverlaps(candidate: Rule, others: List<Rule>): List<Rule> =
        others.filter { it.enabled && GlobMatcher.patternsCanOverlap(candidate.pattern, it.pattern) }

    /** Untuk satu nama file nyata, kembalikan semua rule aktif yang cocok (dipakai saat scan). */
    fun matchingRules(fileName: String, rules: List<Rule>): List<Rule> =
        rules.filter { it.enabled && GlobMatcher.matches(fileName, it.pattern) }
}
