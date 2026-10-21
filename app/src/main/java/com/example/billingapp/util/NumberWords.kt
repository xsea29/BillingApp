package com.example.billingapp.util

private val ONES = listOf(
    "Zero", "One", "Two", "Three", "Four", "Five", "Six", "Seven", "Eight", "Nine", "Ten",
    "Eleven", "Twelve", "Thirteen", "Fourteen", "Fifteen", "Sixteen", "Seventeen", "Eighteen", "Nineteen"
)

private val TENS = listOf("", "", "Twenty", "Thirty", "Forty", "Fifty", "Sixty", "Seventy", "Eighty", "Ninety")

/** 0..99 in words. */
private fun below100(n: Int): String =
    if (n < 20) ONES[n] else TENS[n / 10] + if (n % 10 != 0) " " + ONES[n % 10] else ""

/** 1..999 in words. */
private fun below1000(n: Int): String =
    if (n < 100) {
        below100(n)
    } else {
        ONES[n / 100] + " Hundred" + if (n % 100 != 0) " " + below100(n % 100) else ""
    }

/** A whole number (1 or more) in words, in the Indian system: thousand, lakh, crore. */
private fun wholeNumberInWords(n: Long): String {
    require(n > 0)

    val parts = mutableListOf<String>()
    var rest = n

    // Crores can themselves be hundreds of crores, so take them as a plain number.
    val crores = rest / 1_00_00_000
    rest %= 1_00_00_000
    if (crores > 0) parts += (if (crores < 1000) below1000(crores.toInt()) else wholeNumberInWords(crores)) + " Crore"

    val lakhs = (rest / 1_00_000).toInt()
    rest %= 1_00_000
    if (lakhs > 0) parts += below100(lakhs) + " Lakh"

    val thousands = (rest / 1_000).toInt()
    rest %= 1_000
    if (thousands > 0) parts += below100(thousands) + " Thousand"

    if (rest > 0) parts += below1000(rest.toInt())

    return parts.joinToString(" ")
}

/**
 * An amount of money in words, as printed on Indian invoices:
 * 1050.5 -> "Rupees One Thousand Fifty and Fifty Paise Only".
 */
fun amountInWords(amount: Double): String {
    val totalPaise = Math.round(amount * 100)
    val rupees = totalPaise / 100
    val paise = (totalPaise % 100).toInt()

    val rupeeWords = if (rupees == 0L) "Zero" else wholeNumberInWords(rupees)

    return buildString {
        append("Rupees ").append(rupeeWords)
        if (paise > 0) append(" and ").append(below100(paise)).append(" Paise")
        append(" Only")
    }
}
