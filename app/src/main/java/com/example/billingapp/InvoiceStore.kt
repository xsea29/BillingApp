package com.example.billingapp

import androidx.room.withTransaction

/**
 * Saves a new invoice and all its lines in one transaction, so a half-saved bill
 * can never end up in the database. Returns the new invoice id.
 */
suspend fun AppDatabase.saveBill(bill: Bill): Int = withTransaction {

    val invoiceId = invoiceDao().insert(
        Invoice(
            customerName = bill.customerName,
            phoneNumber = bill.customerPhone,
            subtotal = bill.subtotal,
            gst = bill.gst,
            total = bill.total,
            createdAt = bill.createdAt
        )
    ).toInt()

    insertLines(invoiceId, bill)

    invoiceId
}

/**
 * Changes an existing invoice to match [bill]: the invoice row is updated in place (same
 * number) and its lines are replaced. All or nothing.
 */
suspend fun AppDatabase.updateBill(invoiceId: Int, bill: Bill) = withTransaction {

    invoiceDao().update(
        Invoice(
            id = invoiceId,
            customerName = bill.customerName,
            phoneNumber = bill.customerPhone,
            subtotal = bill.subtotal,
            gst = bill.gst,
            total = bill.total,
            createdAt = bill.createdAt
        )
    )

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
