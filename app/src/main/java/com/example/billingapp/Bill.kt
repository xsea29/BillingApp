package com.example.billingapp

import com.example.billingapp.util.roundToPaise

const val GST_RATE = 0.05

/**
 * A bill that is still being made. Immutable: every change returns a new [Bill],
 * so the screen can just keep it in one piece of state.
 */
data class Bill(
    val customerName: String = "",
    val customerPhone: String = "",
    val items: List<BillItem> = emptyList()
) {

    val subtotal: Double
        get() = roundToPaise(items.sumOf { it.amount() })

    val gst: Double
        get() = roundToPaise(subtotal * GST_RATE)

    val total: Double
        get() = roundToPaise(subtotal + gst)

    /** Total number of pieces, e.g. 2 cakes + 3 pastries = 5. */
    val itemCount: Int
        get() = items.sumOf { it.quantity }

    fun quantityOf(product: Product): Int =
        items.firstOrNull { it.product.id == product.id }?.quantity ?: 0

    /** Sets how many of [product] are on the bill. 0 (or less) removes the line. */
    fun withQuantity(product: Product, quantity: Int): Bill {
        val exists = items.any { it.product.id == product.id }

        val updated = when {
            quantity <= 0 -> items.filterNot { it.product.id == product.id }
            exists -> items.map {
                if (it.product.id == product.id) it.copy(quantity = quantity) else it
            }
            else -> items + BillItem(product, quantity)
        }

        return copy(items = updated)
    }

    fun withCustomer(name: String, phone: String): Bill =
        copy(customerName = name, customerPhone = phone)
}
