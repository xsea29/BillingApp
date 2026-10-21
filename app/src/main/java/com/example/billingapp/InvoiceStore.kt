package com.example.billingapp

import androidx.room.withTransaction

/**
 * Saves a new invoice and all its lines in one transaction, so a half-saved bill
 * can never end up in the database. The bill's customer is added to the customer list too,
 * if new (see [customerIdFor]). Returns the new invoice number.
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

    invoiceId
}

/**
 * Changes an existing invoice to match [bill]: the invoice row is updated in place (same
 * number) and its lines are replaced. All or nothing.
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

    invoiceItemDao().deleteByInvoiceId(invoiceId)
    insertLines(invoiceId, bill)
}

/** Removes an invoice together with its lines. All or nothing. */
suspend fun AppDatabase.deleteInvoice(invoiceId: Int) = withTransaction {
    invoiceItemDao().deleteByInvoiceId(invoiceId)
    invoiceDao().deleteById(invoiceId)
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
