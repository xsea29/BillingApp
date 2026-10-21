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
    val items: List<BillItem> = emptyList(),
    /** When the bill is dated. Starts as "now"; can be moved to an earlier day. */
    val createdAt: Long = System.currentTimeMillis()
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

    fun withDate(createdAt: Long): Bill =
        copy(createdAt = createdAt)
}

/**
 * Turns a saved invoice back into a bill that can be edited.
 *
 * Each line keeps the price it was sold at, even if the catalogue price has changed since.
 * A line whose product has since been deleted or renamed is kept too (with a made-up
 * negative id so it can't clash with a real product); it just isn't in the items list any more.
 */
fun billFromInvoice(
    invoice: Invoice,
    items: List<InvoiceItem>,
    catalogue: List<Product>
): Bill {
    var nextMissingId = -1

    val lines = items
        .map { item ->
            val product = catalogue.firstOrNull { it.name == item.productName }
                ?.copy(price = item.price)
                ?: Product(
                    id = nextMissingId--,
                    name = item.productName,
                    price = item.price,
                    category = ""
                )

            BillItem(product, item.quantity)
        }
        // Two lines for the same product become one, quantities added.
        .fold(emptyList<BillItem>()) { merged, line ->
            val index = merged.indexOfFirst { it.product.id == line.product.id }

            if (index < 0) {
                merged + line
            } else {
                merged.toMutableList().also {
                    it[index] = it[index].copy(quantity = it[index].quantity + line.quantity)
                }
            }
        }

    return Bill(
        customerName = invoice.customerName,
        customerPhone = invoice.phoneNumber,
        items = lines,
        createdAt = invoice.createdAt
    )
}
