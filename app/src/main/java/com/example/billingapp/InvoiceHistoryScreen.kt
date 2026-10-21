package com.example.billingapp

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.activity.compose.BackHandler


@Composable
fun InvoiceHistoryScreen(
    db: AppDatabase,
    onBackClick: () -> Unit,
    onInvoiceClick: (Int) -> Unit
) {    BackHandler {
    onBackClick()
}

    var invoices by remember {
        mutableStateOf<List<Invoice>>(emptyList())
    }

    LaunchedEffect(Unit) {

        invoices =
            db.invoiceDao().getAllInvoices()

    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(
                rememberScrollState()
            )
            .padding(20.dp),

        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {

        Text(
            text = "Invoice History",
            style = MaterialTheme.typography.headlineMedium
        )

        invoices.forEach { invoice ->

            Card(
                modifier = Modifier.fillMaxWidth()
            ) {

                Column(
                    modifier = Modifier.padding(12.dp)
                ) {

                    Text(
                        text = "Invoice #${invoice.id}"
                    )

                    Text(
                        text = invoice.customerName
                    )

                    Text(
                        text = invoice.phoneNumber
                    )

                    Text(
                        text = "Subtotal ₹${invoice.subtotal}"
                    )

                    Text(
                        text = "GST ₹${invoice.gst}"
                    )

                    Text(
                        text = "Total ₹${invoice.total}"
                    )

                    Spacer(
                        modifier = Modifier.height(8.dp)
                    )

                    Button(
                        onClick = {
                            onInvoiceClick(invoice.id)
                        }
                    ) {
                        Text("View Details")
                    }

                }

            }

        }

        Button(
            onClick = onBackClick
        ) {

            Text("Back")

        }

    }

}