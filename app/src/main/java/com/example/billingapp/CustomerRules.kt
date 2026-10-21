package com.example.billingapp

/** A phone number is exactly 10 digits. */
fun isValidPhone(text: String): Boolean =
    text.length == 10 && text.all { it.isDigit() }

/** Email is optional: empty is fine, otherwise it must look like name@domain.tld. */
fun isValidEmail(text: String): Boolean =
    text.isBlank() || Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$").matches(text.trim())

/**
 * How names are compared to decide whether two are the same person: surrounding and repeated
 * spaces and capital letters don't matter ("  Ravi   KUMAR " is "ravi kumar").
 */
fun normalizeName(name: String): String =
    name.trim().split(Regex("\\s+")).joinToString(" ").lowercase()

/**
 * The saved customer with exactly this name AND phone, if any.
 *
 * Phone alone is never enough: family members and shop staff often share one number, and each
 * is their own customer. A bill typed out in full for someone already saved is recognised here
 * so they aren't added twice.
 */
fun findCustomer(name: String, phone: String, customers: List<Customer>): Customer? {
    val wanted = normalizeName(name)
    if (wanted.isEmpty() || phone.isEmpty()) return null

    return customers.firstOrNull { it.phone == phone && normalizeName(it.name) == wanted }
}

/** Another saved customer with the same name and phone, if any. [ignoreId] is the one being edited. */
fun otherCustomerWithSameDetails(
    name: String,
    phone: String,
    customers: List<Customer>,
    ignoreId: Int?
): Customer? =
    findCustomer(name, phone, customers.filter { it.id != ignoreId })

/**
 * The customer to add for a bill's details, or null when there is nobody to add: no name, or a
 * phone that isn't a full 10 digits (a walk-in). Doesn't check whether they are already saved;
 * use [findCustomer] for that first.
 */
fun newCustomerFor(name: String, phone: String): Customer? {
    val cleanName = name.trim()

    if (cleanName.isEmpty() || !isValidPhone(phone)) return null

    return Customer(name = cleanName, phone = phone)
}

/** The answer to "who is this bill for?" (see [chooseCustomer]). */
sealed interface CustomerChoice {
    /** A customer who is already saved. */
    data class Existing(val id: Int) : CustomerChoice

    /** Somebody new, to be added to the customer list. */
    data class New(val customer: Customer) : CustomerChoice

    /** Nobody to link: no name, or no full 10-digit phone number (a walk-in). */
    object None : CustomerChoice
}

/**
 * Decides who a bill is for, from what is on it.
 *
 *  1. Someone picked from the saved list ([pickedId]) stays that customer, as long as they still exist.
 *  2. The same name AND phone typed out in full is that saved customer.
 *  3. Anything else with a name and a full phone is a NEW customer, even when the phone number is
 *     already saved for someone else. A bill is never filed under a customer by phone alone.
 */
fun chooseCustomer(
    name: String,
    phone: String,
    pickedId: Int,
    customers: List<Customer>
): CustomerChoice {
    if (pickedId != 0 && customers.any { it.id == pickedId }) return CustomerChoice.Existing(pickedId)

    findCustomer(name, phone, customers)?.let { return CustomerChoice.Existing(it.id) }

    val added = newCustomerFor(name, phone) ?: return CustomerChoice.None
    return CustomerChoice.New(added)
}

/**
 * What [linkInvoices] works out for bills that were made before invoices knew their customer.
 * [newCustomers] carry temporary negative ids (-1, -2, ...); in [assignments] (invoice id ->
 * customer id) a negative customer id means "the new customer with that temporary id".
 */
data class CustomerLinks(
    val newCustomers: List<Customer>,
    val assignments: Map<Int, Int>
)

/**
 * Links every not-yet-linked invoice to a customer, matching on name AND phone. Someone who has
 * bills but isn't saved yet becomes a new customer (named as on their earliest bill). Bills with
 * no name or no full phone number are left unlinked. Two names on the same phone become two
 * customers.
 */
fun linkInvoices(invoices: List<Invoice>, customers: List<Customer>): CustomerLinks {
    val known = customers.toMutableList()
    val created = mutableListOf<Customer>()
    val assignments = linkedMapOf<Int, Int>()
    var nextTemporaryId = -1

    invoices
        .filter { it.customerId == 0 }
        .sortedBy { it.createdAt }
        .forEach { invoice ->
            if (!isValidPhone(invoice.phoneNumber) || invoice.customerName.isBlank()) return@forEach

            val match = findCustomer(invoice.customerName, invoice.phoneNumber, known)
                ?: Customer(
                    id = nextTemporaryId--,
                    name = invoice.customerName.trim(),
                    phone = invoice.phoneNumber
                ).also {
                    known.add(it)
                    created.add(it)
                }

            assignments[invoice.id] = match.id
        }

    return CustomerLinks(created, assignments)
}
