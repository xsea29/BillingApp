package com.example.billingapp.util

/** The most that can be typed as a stock count (five digits). */
const val MAX_STOCK = 99_999

/** Digits only, up to five of them. "" is fine while typing. */
fun isStockInput(text: String): Boolean =
    text.length <= 5 && text.all { it in '0'..'9' }

/** The count typed so far: "" is 0, "007" is 7. */
fun parseStock(text: String): Int = text.toIntOrNull() ?: 0

/** The number the stepper starts on: 0 for a product that isn't counted (or was billed below zero). */
fun stockShown(stored: Int?): Int = stored?.coerceAtLeast(0) ?: 0

/**
 * What to store when the form is saved. The stepper always shows a number, so a product that
 * isn't counted shows 0; saving without touching it must not turn it into a counted one. Only
 * when the number was changed is it stored.
 */
fun stockToSave(stored: Int?, shown: Int): Int? =
    if (shown == stockShown(stored)) stored else shown
