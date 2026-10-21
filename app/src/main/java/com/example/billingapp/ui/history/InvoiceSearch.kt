package com.example.billingapp.ui.history

import com.example.billingapp.Invoice

/**
 * True when [query] is blank, or matches the customer name, part of the phone number,
 * or the exact invoice number. A query starting with "#" ("#28") matches invoice numbers only.
 */
fun Invoice.matches(query: String): Boolean {
    val text = query.trim()

    if (text.isEmpty()) return true

    if (text.startsWith("#")) {
        val number = text.drop(1).trim()
        return number.isEmpty() || id.toString() == number
    }

    return customerName.contains(text, ignoreCase = true) ||
            phoneNumber.contains(text) ||
            id.toString() == text
}
