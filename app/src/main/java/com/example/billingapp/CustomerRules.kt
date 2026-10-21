package com.example.billingapp

/** A phone number is exactly 10 digits. */
fun isValidPhone(text: String): Boolean =
    text.length == 10 && text.all { it.isDigit() }

/** Email is optional: empty is fine, otherwise it must look like name@domain.tld. */
fun isValidEmail(text: String): Boolean =
    text.isBlank() || Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$").matches(text.trim())

/** Another saved customer using [phone], if any. [ignoreId] is the one being edited. */
fun otherCustomerWithPhone(phone: String, customers: List<Customer>, ignoreId: Int?): Customer? =
    customers.firstOrNull { it.phone == phone && it.id != ignoreId }

/**
 * The customer to add to the customer list for a bill's customer details, or null when there
 * is nothing to add: no name, a phone that isn't a full 10 digits, or a phone that already
 * belongs to a saved customer (the saved one is left exactly as it is).
 */
fun newCustomerFor(name: String, phone: String, existing: List<Customer>): Customer? {
    val cleanName = name.trim()

    if (cleanName.isEmpty() || !isValidPhone(phone)) return null
    if (existing.any { it.phone == phone }) return null

    return Customer(name = cleanName, phone = phone)
}

/**
 * Customers that already have bills but were never saved: one per phone number, named as on
 * their most recent bill that had a name. Phones that are already saved are skipped.
 */
fun customersFromInvoices(invoices: List<Invoice>, existing: List<Customer>): List<Customer> {
    val saved = existing.map { it.phone }.toSet()

    return invoices
        .filter { isValidPhone(it.phoneNumber) && it.customerName.isNotBlank() && it.phoneNumber !in saved }
        .groupBy { it.phoneNumber }
        .map { (phone, bills) ->
            Customer(name = bills.maxBy { it.createdAt }.customerName.trim(), phone = phone)
        }
        .sortedBy { it.name.lowercase() }
}
