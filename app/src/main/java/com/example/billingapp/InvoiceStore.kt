package com.example.billingapp

import androidx.room.withTransaction

/**
 * Saves a new invoice and all its lines in one transaction, so a half-saved bill
 * can never end up in the database. The bill's customer is added to the customer list too,
 * if new (see [customerIdFor]), and what it sold is taken out of product stock. Returns the
 * new invoice number.
 *
 * The number is chosen here (see [nextInvoiceNumber]) rather than left to the database's own
 * counter, which never goes backwards: after deleting invoice #1 it would hand out #2 while the
 * New Bill page had promised #1.
 */
suspend fun AppDatabase.saveBill(bill: Bill): Int = withTransaction {

    val invoiceId = nextInvoiceNumber(invoiceDao().getLastInvoiceId())

    invoiceDao().insert(
        Invoice(
            id = invoiceId,
            customerName = bill.customerName,
            phoneNumber = bill.customerPhone,
            subtotal = bill.subtotal,
            gst = bill.gst,
            total = bill.total,
            createdAt = bill.createdAt,
            customerId = customerIdFor(bill)
        )
    )

    insertLines(invoiceId, bill)
    changeStock(stockChanges(before = emptyMap(), after = bill.piecesByName()))

    invoiceId
}

/**
 * Changes an existing invoice to match [bill]: the invoice row is updated in place (same
 * number) and its lines are replaced; product stock moves by what changed. All or nothing.
 */
suspend fun AppDatabase.updateBill(invoiceId: Int, bill: Bill) = withTransaction {

    val updated = invoiceDao().update(
        Invoice(
            id = invoiceId,
            customerName = bill.customerName,
            phoneNumber = bill.customerPhone,
            subtotal = bill.subtotal,
            gst = bill.gst,
            total = bill.total,
            createdAt = bill.createdAt,
            customerId = customerIdFor(bill)
        )
    )

    // Never carry on if the invoice isn't there: that would leave lines belonging to nothing.
    check(updated == 1) { "Invoice #$invoiceId no longer exists" }

    // Stock moves by the difference between the old lines and the new ones.
    val before = invoiceItemDao().getItemsByInvoiceId(invoiceId).piecesByName()

    invoiceItemDao().deleteByInvoiceId(invoiceId)
    insertLines(invoiceId, bill)
    changeStock(stockChanges(before = before, after = bill.piecesByName()))
}

/** Removes an invoice together with its lines, and gives its pieces back to stock. All or nothing. */
suspend fun AppDatabase.deleteInvoice(invoiceId: Int) = withTransaction {
    val before = invoiceItemDao().getItemsByInvoiceId(invoiceId).piecesByName()

    invoiceItemDao().deleteByInvoiceId(invoiceId)
    invoiceDao().deleteById(invoiceId)
    changeStock(stockChanges(before = before, after = emptyMap()))
}

/** Applies [changes] (see [stockChanges]) to the products that have a stock count. */
private suspend fun AppDatabase.changeStock(changes: Map<String, Int>) {
    changes.forEach { (name, change) -> productDao().adjustStock(name, change) }
}

private suspend fun AppDatabase.insertLines(invoiceId: Int, bill: Bill) {
    bill.items.forEach { item ->
        invoiceItemDao().insert(
            InvoiceItem(
                invoiceId = invoiceId,
                productName = item.product.name,
                quantity = item.quantity,
                price = item.product.price,
                amount = item.amount()
            )
        )
    }
}
