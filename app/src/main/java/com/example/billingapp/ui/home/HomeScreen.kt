package com.example.billingapp.ui.home

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.billingapp.AppDatabase
import com.example.billingapp.Invoice
import com.example.billingapp.deleteInvoice
import com.example.billingapp.ui.components.AppDrawerSheet
import com.example.billingapp.ui.components.DeleteInvoiceDialog
import com.example.billingapp.ui.components.DrawerEntry
import com.example.billingapp.ui.components.EmptyState
import com.example.billingapp.ui.components.RoundActionButton
import com.example.billingapp.ui.components.SegmentedToggle
import com.example.billingapp.ui.components.SmallCaps
import com.example.billingapp.ui.components.TextAction
import com.example.billingapp.ui.components.invoiceGroups
import com.example.billingapp.util.SalesPeriod
import com.example.billingapp.util.formatRupees
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(
    db: AppDatabase,
    onNewBillClick: () -> Unit,
    onInvoiceClick: (Int) -> Unit,
    onInvoiceHistoryClick: () -> Unit,
    onSalesSummaryClick: () -> Unit,
    onCustomersClick: () -> Unit,
    onProductsClick: () -> Unit
) {
    var period by remember { mutableStateOf(SalesPeriod.Today) }

    // null = still loading, so we don't flash "No bills" before the query returns.
    var invoices by remember { mutableStateOf<List<Invoice>?>(null) }
    var pendingDelete by remember { mutableStateOf<Invoice?>(null) }
    var reload by remember { mutableStateOf(0) }

    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    LaunchedEffect(period, reload) {
        val (start, end) = period.range()
        invoices = db.invoiceDao()
            .getInvoicesBetween(start, end)
            .sortedByDescending { it.createdAt }
    }

    // Back closes the side menu first, and only then leaves the app.
    BackHandler(enabled = drawerState.isOpen) {
        scope.launch { drawerState.close() }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            AppDrawerSheet(
                entries = listOf(
                    DrawerEntry("Invoice History", onInvoiceHistoryClick),
                    DrawerEntry("Sales Summary", onSalesSummaryClick),
                    DrawerEntry("Customers", onCustomersClick),
                    DrawerEntry("Products", onProductsClick)
                )
            )
        }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                // Bottom padding keeps the last row clear of the floating button.
                contentPadding = PaddingValues(
                    start = 24.dp,
                    end = 24.dp,
                    top = 8.dp,
                    bottom = 120.dp
                )
            ) {

                item {
                    HomeTopRow(
                        onMenuClick = { scope.launch { drawerState.open() } }
                    )
                }

                item {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(20.dp),
                        modifier = Modifier.padding(top = 24.dp)
                    ) {
                        Text(
                            text = "Invoices",
                            style = MaterialTheme.typography.headlineLarge
                        )

                        SegmentedToggle(
                            options = SalesPeriod.values().toList(),
                            selected = period,
                            label = { it.label },
                            onSelect = { period = it }
                        )
                    }
                }

                val loaded = invoices

                if (loaded != null && loaded.isEmpty()) {

                    item {
                        EmptyState(
                            title = "No bills yet",
                            message = "Tap + New to create one."
                        )
                    }

                } else if (loaded != null) {

                    item { SalesSummary(loaded) }

                    invoiceGroups(
                        invoices = loaded,
                        onInvoiceClick = onInvoiceClick,
                        onDeleteClick = { pendingDelete = it }
                    )
                }
            }

            RoundActionButton(
                text = "+ New",
                onClick = onNewBillClick,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(24.dp)
            )
        }
    }

    pendingDelete?.let { invoice ->
        DeleteInvoiceDialog(
            invoice = invoice,
            onConfirm = {
                pendingDelete = null
                scope.launch {
                    db.deleteInvoice(invoice.id)
                    reload++
                }
            },
            onDismiss = { pendingDelete = null }
        )
    }
}

@Composable
private fun HomeTopRow(onMenuClick: () -> Unit) {
    val today = remember {
        SimpleDateFormat("EEE d MMM", Locale.getDefault()).format(Date())
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextAction(
            text = "Menu",
            onClick = onMenuClick
        )

        SmallCaps(text = today)
    }
}

/** "SALES · 14 BILLS" on the left, the total on the right. */
@Composable
private fun SalesSummary(invoices: List<Invoice>) {
    val count = invoices.size

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 28.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        SmallCaps(text = "Sales · $count ${if (count == 1) "bill" else "bills"}")

        Text(
            text = formatRupees(invoices.sumOf { it.total }),
            style = MaterialTheme.typography.titleMedium
        )
    }
}
