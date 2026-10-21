package com.example.billingapp.ui.customers

import com.example.billingapp.Customer
import com.example.billingapp.Invoice
import com.example.billingapp.util.roundToPaise

enum class CustomerSort(val label: String) {
    Name("A–Z"),
    TopSpenders("Top spenders"),
    Recent("Recent")
}

/** What a customer has bought, worked out from their invoices. */
data class CustomerStats(
    val billCount: Int = 0,
    val totalSpent: Double = 0.0,
    /** When the latest bill was made, or null if there are none. */
    val lastVisit: Long? = null
)

/**
 * Invoices only store the customer's name and phone as text, so a customer's bills are the
 * ones with the same phone number. Bills without a phone number belong to nobody in particular.
 */
fun statsByPhone(invoices: List<Invoice>): Map<String, CustomerStats> =
    invoices
        .filter { it.phoneNumber.isNotBlank() }
        .groupBy { it.phoneNumber }
        .mapValues { (_, bills) ->
            CustomerStats(
                billCount = bills.size,
                totalSpent = roundToPaise(bills.sumOf { it.total }),
                lastVisit = bills.maxOf { it.createdAt }
            )
        }

/** The customer's bills, newest first. */
fun billsOf(customer: Customer, invoices: List<Invoice>): List<Invoice> =
    if (customer.phone.isBlank()) {
        emptyList()
    } else {
        invoices
            .filter { it.phoneNumber == customer.phone }
            .sortedByDescending { it.createdAt }
    }

/** Customers whose name, phone or email contains [query] (blank = everyone). */
fun filterCustomers(customers: List<Customer>, query: String): List<Customer> {
    val text = query.trim()
    if (text.isEmpty()) return customers

    return customers.filter {
        it.name.contains(text, ignoreCase = true) ||
                it.phone.contains(text) ||
                it.email.contains(text, ignoreCase = true)
    }
}

fun sortCustomers(
    customers: List<Customer>,
    stats: Map<String, CustomerStats>,
    sort: CustomerSort
): List<Customer> {
    val byName = compareBy<Customer> { it.name.lowercase() }

    return when (sort) {
        CustomerSort.Name -> customers.sortedWith(byName)

        CustomerSort.TopSpenders -> customers.sortedWith(
            compareByDescending<Customer> { stats[it.phone]?.totalSpent ?: 0.0 }.then(byName)
        )

        // People who have never bought go last.
        CustomerSort.Recent -> customers.sortedWith(
            compareByDescending<Customer> { stats[it.phone]?.lastVisit ?: Long.MIN_VALUE }.then(byName)
        )
    }
}
