package com.elprompter.promptvault.util

/**
 * Pencocokan glob sederhana (mendukung * dan ?) tanpa dependency eksternal,
 * agar cocok dipakai baik di runtime app maupun di unit test JVM murni.
 */
object GlobMatcher {

    fun matches(fileName: String, pattern: String): Boolean {
        val regex = globToRegex(pattern)
        return regex.matches(fileName)
    }

    fun globToRegex(pattern: String): Regex {
        val sb = StringBuilder("(?i)")
        for (c in pattern) {
            when (c) {
                '*' -> sb.append(".*")
                '?' -> sb.append(".")
                '.', '(', ')', '+', '|', '^', '$', '@', '%', '{', '}', '[', ']', '\\' ->
                    sb.append('\\').append(c)
                else -> sb.append(c)
            }
        }
        return Regex(sb.toString())
    }

    /**
     * True jika dua pattern glob secara teoretis bisa cocok dengan nama file yang sama.
     * Dipakai untuk deteksi rule tumpang tindih (TODO #3).
     * Pendekatan praktis: bandingkan lewat sejumlah nama file sampel yang dihasilkan
     * dari kedua pattern, plus pengecekan literal-vs-literal langsung.
     */
    fun patternsCanOverlap(patternA: String, patternB: String): Boolean {
        if (patternA.equals(patternB, ignoreCase = true)) return true

        val sampleFromA = sampleFileNameFor(patternA)
        val sampleFromB = sampleFileNameFor(patternB)

        val aMatchesOwnSample = matches(sampleFromA, patternA)
        val bMatchesOwnSample = matches(sampleFromB, patternB)

        val crossMatch =
            (aMatchesOwnSample && matches(sampleFromA, patternB)) ||
            (bMatchesOwnSample && matches(sampleFromB, patternA))

        if (crossMatch) return true

        // Kasus umum: salah satu pattern adalah wildcard murni untuk ekstensi yang sama,
        // mis. "*.zip" vs "report_*.zip" -> overlap.
        val extA = extensionOf(patternA)
        val extB = extensionOf(patternB)
        if (extA != null && extA.equals(extB, ignoreCase = true)) {
            if (patternA == "*.$extA" || patternB == "*.$extB") return true
        }
        return false
    }

    private fun sampleFileNameFor(pattern: String): String =
        pattern.replace("*", "sample").replace("?", "x")

    private fun extensionOf(pattern: String): String? =
        pattern.substringAfterLast('.', missingDelimiterValue = "").ifBlank { null }
}
