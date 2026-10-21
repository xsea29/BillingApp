package com.example.billingapp

import androidx.room.withTransaction

/**
 * Saves the invoice and all its lines in one transaction, so a half-saved bill
 * can never end up in the database. Returns the new invoice id.
 */
suspend fun AppDatabase.saveBill(bill: Bill): Int = withTransaction {

    val invoiceId = invoiceDao().insert(
        Invoice(
            customerName = bill.customerName,
            phoneNumber = bill.customerPhone,
            subtotal = bill.subtotal,
            gst = bill.gst,
            total = bill.total
        )
    ).toInt()

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

    invoiceId
}
