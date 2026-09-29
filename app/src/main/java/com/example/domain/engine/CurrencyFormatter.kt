package com.example.domain.engine

import java.text.DecimalFormat
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.roundToLong

object CurrencyFormatter {

    /**
     * Formats an amount into Indian currency format: ₹1,25,000.00 or ₹1,25,000
     */
    fun formatInr(amount: Double, includeDecimals: Boolean = true): String {
        if (amount.isNaN() || amount.isInfinite()) return "₹0.00"

        val isNegative = amount < 0
        val absAmount = Math.abs(amount)

        val longPart = absAmount.toLong()
        val decimalPart = ((absAmount - longPart) * 100).roundToLong()

        val longStr = longPart.toString()
        val formattedInteger = if (longStr.length <= 3) {
            longStr
        } else {
            val lastThree = longStr.substring(longStr.length - 3)
            val rest = longStr.substring(0, longStr.length - 3)

            val builder = StringBuilder()
            var count = 0
            for (i in rest.length - 1 downTo 0) {
                builder.append(rest[i])
                count++
                if (count == 2 && i != 0) {
                    builder.append(",")
                    count = 0
                }
            }
            builder.reverse().toString() + "," + lastThree
        }

        val prefix = if (isNegative) "-₹" else "₹"
        return if (includeDecimals) {
            val decStr = if (decimalPart < 10) "0$decimalPart" else decimalPart.toString()
            "$prefix$formattedInteger.$decStr"
        } else {
            "$prefix$formattedInteger"
        }
    }

    /**
     * Converts a number into Indian English words (e.g. Rupees One Lakh Twenty-Five Thousand Only)
     */
    fun amountToWords(amount: Double): String {
        if (amount <= 0) return "Rupees Zero Only"

        val totalAmount = amount.roundToLong()
        val paise = ((amount - totalAmount.toDouble()) * 100).roundToLong()

        val words = convertNumberToIndianWords(totalAmount)
        val result = StringBuilder("Rupees $words")

        if (paise > 0) {
            result.append(" and ").append(convertLessThanOneThousand(paise.toInt())).append(" Paise")
        }
        result.append(" Only")
        return result.toString()
    }

    private val units = arrayOf(
        "", "One", "Two", "Three", "Four", "Five", "Six", "Seven", "Eight", "Nine", "Ten",
        "Eleven", "Twelve", "Thirteen", "Fourteen", "Fifteen", "Sixteen", "Seventeen", "Eighteen", "Nineteen"
    )

    private val tens = arrayOf(
        "", "", "Twenty", "Thirty", "Forty", "Fifty", "Sixty", "Seventy", "Eighty", "Ninety"
    )

    private fun convertLessThanOneThousand(number: Int): String {
        var n = number
        var result = ""

        if (n >= 100) {
            result += units[n / 100] + " Hundred "
            n %= 100
        }

        if (n >= 20) {
            result += tens[n / 10]
            if (n % 10 > 0) {
                result += " " + units[n % 10]
            }
        } else if (n > 0) {
            result += units[n]
        }

        return result.trim()
    }

    private fun convertNumberToIndianWords(number: Long): String {
        if (number == 0L) return "Zero"

        var n = number
        val parts = mutableListOf<String>()

        val crores = n / 10000000L
        n %= 10000000L

        val lakhs = n / 100000L
        n %= 100000L

        val thousands = n / 1000L
        n %= 1000L

        val remainder = n.toInt()

        if (crores > 0) {
            parts.add(convertLessThanOneThousand(crores.toInt()) + " Crore")
        }
        if (lakhs > 0) {
            parts.add(convertLessThanOneThousand(lakhs.toInt()) + " Lakh")
        }
        if (thousands > 0) {
            parts.add(convertLessThanOneThousand(thousands.toInt()) + " Thousand")
        }
        if (remainder > 0) {
            parts.add(convertLessThanOneThousand(remainder))
        }

        return parts.joinToString(" ")
    }
}
