package com.example.billingapp

import androidx.room.withTransaction

/** Adds the customer of a bill to the customer list if they are new (see [newCustomerFor]). */
suspend fun AppDatabase.rememberCustomer(name: String, phone: String) {
    val customer = newCustomerFor(name, phone, customerDao().getAllCustomers()) ?: return
    customerDao().insert(customer)
}

/**
 * One-off catch-up for bills made before customers were saved automatically: adds every
 * customer who has a bill but no entry in the list. Returns how many were added.
 */
suspend fun AppDatabase.addCustomersFromInvoices(): Int = withTransaction {
    val toAdd = customersFromInvoices(invoiceDao().getAllInvoices(), customerDao().getAllCustomers())
    toAdd.forEach { customerDao().insert(it) }
    toAdd.size
}
