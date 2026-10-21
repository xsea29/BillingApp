package com.example.billingapp.util

/** The most that can be typed as a stock count (five digits). */
const val MAX_STOCK = 99_999

/** Digits only, up to five of them. "" is fine while typing. */
fun isStockInput(text: String): Boolean =
    text.length <= 5 && text.all { it in '0'..'9' }

/** The count typed so far: "" is 0, "007" is 7. */
fun parseStock(text: String): Int = text.toIntOrNull() ?: 0
