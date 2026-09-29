package com.example.domain.engine

import java.util.Calendar

data class NumberingRule(
    val prefix: String = "KA",
    val includeYear: Boolean = true,
    val includeMonth: Boolean = false,
    val digits: Int = 3,
    val separator: String = "-"
)

object QuotationNumberGenerator {

    fun generate(rule: NumberingRule, sequence: Int): String {
        val cal = Calendar.getInstance()
        val year = cal.get(Calendar.YEAR)
        val month = cal.get(Calendar.MONTH) + 1

        val parts = mutableListOf<String>()
        if (rule.prefix.isNotBlank()) {
            parts.add(rule.prefix.trim().uppercase())
        }

        if (rule.includeYear) {
            parts.add(year.toString())
        }

        if (rule.includeMonth) {
            parts.add(String.format("%02d", month))
        }

        val seqStr = String.format("%0${rule.digits}d", sequence)
        parts.add(seqStr)

        return parts.joinToString(rule.separator)
    }

    fun formatWithRevision(baseNumber: String, revision: Int): String {
        return if (revision <= 0) {
            baseNumber
        } else {
            val revStr = String.format("%02d", revision)
            "$baseNumber Rev.$revStr"
        }
    }
}
