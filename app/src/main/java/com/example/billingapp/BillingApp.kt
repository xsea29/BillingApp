package com.example.billingapp

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.billingapp.ui.customers.CustomersScreen
import com.example.billingapp.ui.details.InvoiceDetailsScreen
import com.example.billingapp.ui.history.InvoiceHistoryScreen
import com.example.billingapp.ui.home.HomeScreen
import com.example.billingapp.ui.newbill.NewBillScreen
import com.example.billingapp.ui.products.ProductsScreen
import com.example.billingapp.ui.sales.SalesSummaryScreen
import com.example.billingapp.ui.stock.StocksScreen

/** Decides which screen is showing, and where "back" from an invoice should return to. */
@Composable
fun BillingApp(db: AppDatabase) {

    var currentScreen by remember {
        mutableStateOf("home")
    }
    var selectedInvoiceId by remember {
        mutableStateOf(0)
    }
    var invoiceDetailsBackTo by remember {
        mutableStateOf("invoiceHistory")
    }
    // The customer whose page is open on the Customers screen, kept here so that
    // coming back from one of their bills lands on the same customer.
    var openCustomerId by remember {
        mutableStateOf<Int?>(null)
    }
    // Set when a saved invoice is being edited on the New Bill screen; null for a brand-new bill.
    var editInvoiceId by remember {
        mutableStateOf<Int?>(null)
    }

    fun startNewBill() {
        editInvoiceId = null
        currentScreen = "bill"
    }

    when (currentScreen) {

        "home" -> HomeScreen(
            db = db,
            onNewBillClick = ::startNewBill,
            onInvoiceClick = {
                selectedInvoiceId = it
                invoiceDetailsBackTo = "home"
                currentScreen = "invoiceDetails"
            },
            onInvoiceHistoryClick = {
                currentScreen = "invoiceHistory"
            },
            onSalesSummaryClick = {
                currentScreen = "salesSummary"
            },
            onCustomersClick = {
                openCustomerId = null
                currentScreen = "customers"
            },
            onProductsClick = {
                currentScreen = "products"
            },
            onStocksClick = {
                currentScreen = "stocks"
            }
        )

        "customers" -> CustomersScreen(
            db = db,
            openCustomerId = openCustomerId,
            onOpenCustomerChange = {
                openCustomerId = it
            },
            onInvoiceClick = {
                selectedInvoiceId = it
                invoiceDetailsBackTo = "customers"
                currentScreen = "invoiceDetails"
            },
            onBackClick = {
                openCustomerId = null
                currentScreen = "home"
            }
        )

        "products" -> ProductsScreen(
            db = db,
            onBackClick = {
                currentScreen = "home"
            }
        )

        "stocks" -> StocksScreen(
            db = db,
            onBackClick = {
                currentScreen = "home"
            }
        )

        "bill" -> NewBillScreen(
            db = db,
            editInvoiceId = editInvoiceId,
            onBackClick = {
                // Backing out of an edit returns to that invoice, not to Home.
                currentScreen = if (editInvoiceId != null) "invoiceDetails" else "home"
                editInvoiceId = null
            },
            onSaved = { invoiceId ->
                // A new bill goes back to Home when closed; an updated one keeps its old way back.
                if (editInvoiceId == null) invoiceDetailsBackTo = "home"
                selectedInvoiceId = invoiceId
                editInvoiceId = null
                currentScreen = "invoiceDetails"
            }
        )

        "invoiceHistory" -> InvoiceHistoryScreen(
            db = db,
            onBackClick = {
                currentScreen = "home"
            },
            onInvoiceClick = {
                selectedInvoiceId = it
                invoiceDetailsBackTo = "invoiceHistory"
                currentScreen = "invoiceDetails"
            },
            onNewBillClick = ::startNewBill
        )

        "invoiceDetails" -> InvoiceDetailsScreen(
            db = db,
            invoiceId = selectedInvoiceId,
            onBackClick = {
                currentScreen = invoiceDetailsBackTo
            },
            onEditClick = {
                editInvoiceId = selectedInvoiceId
                currentScreen = "bill"
            }
        )

        "salesSummary" -> SalesSummaryScreen(
            db = db,
            onBackClick = {
                currentScreen = "home"
            },
            onNewBillClick = ::startNewBill
        )
    }
}
