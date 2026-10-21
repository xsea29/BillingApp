package com.example.billingapp

/*
 * How billing moves product stock. Lines are matched to products by name, because that is what
 * a saved invoice line keeps. Products without a stock count are skipped later, in the database
 * query, so nothing here needs to know which products are counted.
 */

/** Pieces per product name on this bill; two lines of the same product add up. */
fun Bill.piecesByName(): Map<String, Int> =
    items
        .groupBy { it.product.name }
        .mapValues { (_, lines) -> lines.sumOf { it.quantity } }

/** Pieces per product name on a saved invoice's lines. */
fun List<InvoiceItem>.piecesByName(): Map<String, Int> =
    groupBy { it.productName }
        .mapValues { (_, lines) -> lines.sumOf { it.quantity } }

/**
 * How each product's stock changes when an invoice's lines go from [before] to [after] (pieces
 * per product name): what was taken out before comes back, what is taken out now goes. Positive
 * adds to stock, negative takes away. Products whose number didn't change are left out.
 *
 * A new bill is `stockChanges(emptyMap(), pieces)`, deleting one is `stockChanges(pieces, emptyMap())`,
 * and editing one only touches what actually changed.
 */
fun stockChanges(before: Map<String, Int>, after: Map<String, Int>): Map<String, Int> =
    (before.keys + after.keys)
        .associateWith { (before[it] ?: 0) - (after[it] ?: 0) }
        .filterValues { it != 0 }
