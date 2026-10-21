package com.example.billingapp.ui.products

import com.example.billingapp.Product
import java.util.Locale

/** Every category in use, A to Z, each once. */
fun categoriesOf(products: List<Product>): List<String> =
    products
        .map { it.category }
        .distinct()
        .sortedBy { it.lowercase() }

/**
 * Products in the chosen [category] (null = all) whose name or category contains [query],
 * grouped by category and A to Z inside each.
 */
fun filterProducts(
    products: List<Product>,
    query: String,
    category: String?
): List<Product> {
    val text = query.trim()

    return products
        .filter { product ->
            (category == null || product.category == category) &&
                    (text.isEmpty() ||
                            product.name.contains(text, ignoreCase = true) ||
                            product.category.contains(text, ignoreCase = true))
        }
        .sortedWith(compareBy({ it.category.lowercase() }, { it.name.lowercase() }))
}

/** Up to 7 digits and at most 2 decimals: "290", "199.5", "12.", ".5" and "" are fine while typing. */
fun isPriceInput(text: String): Boolean =
    Regex("^\\d{0,7}(\\.\\d{0,2})?$").matches(text)

/** The price typed so far, or null if it isn't a usable price yet (empty, "." or zero). */
fun parsePrice(text: String): Double? =
    text.toDoubleOrNull()?.takeIf { it > 0 }

/** 290.0 -> "290", 199.5 -> "199.5", 12.25 -> "12.25" (for filling the price field when editing). */
fun priceToInput(price: Double): String =
    if (price % 1.0 == 0.0) {
        price.toLong().toString()
    } else {
        String.format(Locale.US, "%.2f", price).trimEnd('0').trimEnd('.')
    }

/** If [typed] is an existing category apart from capitalisation, use the existing spelling. */
fun matchCategory(typed: String, existing: List<String>): String {
    val text = typed.trim()
    return existing.firstOrNull { it.equals(text, ignoreCase = true) } ?: text
}
