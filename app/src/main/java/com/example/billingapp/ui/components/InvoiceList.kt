package com.example.billingapp.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.billingapp.Invoice
import com.example.billingapp.util.dayLabel
import com.example.billingapp.util.startOfDay

/**
 * Adds [invoices] to a LazyColumn, grouped by day under small-caps headings
 * ("TODAY", "YESTERDAY", "12 Oct 2026"). Expects the list newest-first.
 */
fun LazyListScope.invoiceGroups(
    invoices: List<Invoice>,
    onInvoiceClick: (Int) -> Unit
) {
    invoices
        .groupBy { startOfDay(it.createdAt) }
        .forEach { (_, dayInvoices) ->

            item(key = "day-${dayInvoices.first().id}") {
                SmallCaps(
                    text = dayLabel(dayInvoices.first().createdAt),
                    modifier = Modifier.padding(top = 20.dp, bottom = 4.dp)
                )
            }

            items(dayInvoices, key = { it.id }) { invoice ->
                InvoiceRow(
                    invoice = invoice,
                    onClick = { onInvoiceClick(invoice.id) }
                )
            }
        }
}
