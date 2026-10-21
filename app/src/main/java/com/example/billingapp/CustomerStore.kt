package com.example.billingapp

import androidx.room.withTransaction

/**
 * The customer a bill belongs to, adding them to the customer list if they are new. Returns 0
 * for a bill with nobody to link (a walk-in). The decision itself is [chooseCustomer].
 */
suspend fun AppDatabase.customerIdFor(bill: Bill): Int =
    when (val choice = chooseCustomer(
        name = bill.customerName,
        phone = bill.customerPhone,
        pickedId = bill.customerId,
        customers = customerDao().getAllCustomers()
    )) {
        is CustomerChoice.Existing -> choice.id
        is CustomerChoice.New -> customerDao().insert(choice.customer).toInt()
        CustomerChoice.None -> 0
    }

/**
 * One-off, for bills made before invoices knew their customer: links each to a customer by name
 * and phone, adding customers who aren't saved yet (see [linkInvoices]). Nothing is deleted.
 */
suspend fun AppDatabase.linkInvoicesToCustomers() = withTransaction {
    val links = linkInvoices(invoiceDao().getAllInvoices(), customerDao().getAllCustomers())

    val realIds = links.newCustomers.associate { temporary ->
        temporary.id to customerDao().insert(temporary.copy(id = 0)).toInt()
    }

    links.assignments.forEach { (invoiceId, customerId) ->
        invoiceDao().setCustomerId(invoiceId, realIds[customerId] ?: customerId)
    }
}
