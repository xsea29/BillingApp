package com.example.billingapp.ui.stock

import com.example.billingapp.Bill
import com.example.billingapp.InvoiceItem
import com.example.billingapp.SoldLine
import com.example.billingapp.StockEntry
import com.example.billingapp.util.startOfDay

/*
 * Can this bill be made from what is in stock? The same two lists the Stocks screen sums
 * (what came in, what was billed) answer it, so the answer can never disagree with that screen.
 */

/** Everything the stock sums need: all the stock that came in, and every bill line from the first of it on. */
class StockData(
    val entries: List<StockEntry>,
    val sold: List<SoldLine>
) {
    /** What is left of every counted product right now. */
    fun leftByName(): Map<String, Int> =
        entries
            .map { it.productName }
            .distinct()
            .associateWith { availableStock(entries, sold, it, saleDate = Long.MAX_VALUE) ?: 0 }
}

/** One product that a bill asks too much of. [available] is how many can still be sold; 0 or less is out of stock. */
data class Shortage(val name: String, val available: Int)

/** Pieces per product name on a saved invoice's lines. */
fun List<InvoiceItem>.piecesByName(): Map<String, Int> =
    groupBy { it.productName }
        .mapValues { (_, lines) -> lines.sumOf { it.quantity } }

/** Pieces per product name on this bill; two lines of the same product add up. */
fun Bill.piecesByName(): Map<String, Int> =
    items
        .groupBy { it.product.name }
        .mapValues { (_, lines) -> lines.sumOf { it.quantity } }

/**
 * How many of [name] can still be sold on a bill dated [saleDate], or null when the product isn't
 * counted: no stock was ever entered for it, or the bill is dated before the day its first stock
 * came in (such a bill doesn't take from stock, see [stockRows]).
 *
 * [ignoreInvoice] leaves one invoice out of what was sold: the invoice being changed, whose own
 * pieces are free to be chosen again.
 */
fun availableStock(
    entries: List<StockEntry>,
    sold: List<SoldLine>,
    name: String,
    saleDate: Long,
    ignoreInvoice: Int? = null
): Int? {
    val own = entries.filter { it.productName == name }
    if (own.isEmpty()) return null

    val countedFrom = startOfDay(own.minOf { it.addedAt })
    if (saleDate < countedFrom) return null

    val soldSince = sold
        .filter { it.productName == name && it.soldAt >= countedFrom && it.invoiceId != ignoreInvoice }
        .sumOf { it.quantity }

    return own.sumOf { it.quantity } - soldSince
}

/**
 * The products on a bill ([asked]: pieces per product name) that the stock can't cover, A to Z.
 * Products that aren't counted are never a shortage.
 *
 * [alreadyOnInvoice] is what the saved invoice being changed already had: a line that doesn't ask
 * for more than that is let through, so changing only the customer of an old invoice is never
 * blocked, whatever has happened to the stock since.
 */
fun stockShortages(
    asked: Map<String, Int>,
    entries: List<StockEntry>,
    sold: List<SoldLine>,
    saleDate: Long,
    editingInvoice: Int? = null,
    alreadyOnInvoice: Map<String, Int> = emptyMap()
): List<Shortage> =
    asked
        .mapNotNull { (name, pieces) ->
            val available = availableStock(entries, sold, name, saleDate, editingInvoice)
                ?: return@mapNotNull null

            val allowed = maxOf(available, alreadyOnInvoice[name] ?: 0)

            if (pieces > allowed) Shortage(name, available) else null
        }
        .sortedBy { it.name.lowercase() }

/** "Veg Puff is out of stock.", "Only 3 of Veg Puff left in stock.", or one line per product when several are short. */
fun shortageMessage(shortages: List<Shortage>): String =
    if (shortages.size == 1) {
        val one = shortages.first()

        if (one.available <= 0) {
            "${one.name} is out of stock."
        } else {
            "Only ${one.available} of ${one.name} left in stock."
        }
    } else {
        shortages.joinToString("\n") {
            "${it.name}: ${if (it.available <= 0) "out of stock" else "only ${it.available} left"}"
        }
    }
