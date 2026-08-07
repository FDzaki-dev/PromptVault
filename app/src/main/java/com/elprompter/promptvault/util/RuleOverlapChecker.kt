package com.elprompter.promptvault.util

import com.elprompter.promptvault.data.Rule

/**
 * Rule tumpang tindih dideteksi & ditampilkan sebagai peringatan ke user (fitur
 * lengkap), baik saat menyimpan rule baru maupun saat scan menemukan file yang
 * cocok >1 rule.
 */
object RuleOverlapChecker {

    fun findOverlaps(candidate: Rule, others: List<Rule>): List<Rule> =
        others.filter { it.enabled && patternListsCanOverlap(candidate.pattern, it.pattern) }

    private fun patternListsCanOverlap(csvA: String, csvB: String): Boolean {
        val a = GlobMatcher.splitPatterns(csvA)
        val b = GlobMatcher.splitPatterns(csvB)
        return a.any { pa -> b.any { pb -> GlobMatcher.patternsCanOverlap(pa, pb) } }
    }

    /** Untuk satu nama file nyata, kembalikan semua rule aktif yang cocok (dipakai saat scan). */
    fun matchingRules(fileName: String, fileSizeKb: Long, rules: List<Rule>): List<Rule> =
        rules.filter { rule ->
            rule.enabled &&
                GlobMatcher.matchesAny(fileName, rule.pattern) &&
                !isExcluded(fileName, rule) &&
                matchesSizeConstraint(fileSizeKb, rule)
        }

    /** True kalau file dikecualikan secara eksplisit oleh excludePattern rule ini (boleh multi-pattern). */
    fun isExcluded(fileName: String, rule: Rule): Boolean =
        rule.excludePattern.isNotBlank() && GlobMatcher.matchesAny(fileName, rule.excludePattern)

    /** True kalau ukuran file berada dalam batas min/max rule (null = tidak ada batas). */
    fun matchesSizeConstraint(fileSizeKb: Long, rule: Rule): Boolean {
        val minOk = rule.minSizeKb?.let { fileSizeKb >= it } ?: true
        val maxOk = rule.maxSizeKb?.let { fileSizeKb <= it } ?: true
        return minOk && maxOk
    }
}
