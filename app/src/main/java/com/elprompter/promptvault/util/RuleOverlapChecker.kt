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
        rules.filter { rule ->
            rule.enabled &&
                GlobMatcher.matches(fileName, rule.pattern) &&
                !isExcluded(fileName, rule)
        }

    /** True kalau file dikecualikan secara eksplisit oleh excludePattern rule ini. */
    fun isExcluded(fileName: String, rule: Rule): Boolean =
        rule.excludePattern.isNotBlank() && GlobMatcher.matches(fileName, rule.excludePattern)
}
