package com.example.billingapp.ui.history

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import com.example.billingapp.ui.components.EmptyState
import com.example.billingapp.ui.components.DeleteInvoiceDialog
import com.example.billingapp.ui.components.RoundActionButton
import com.example.billingapp.ui.components.SearchField
import com.example.billingapp.ui.components.SmallCaps
import com.example.billingapp.ui.components.TopRow
import com.example.billingapp.ui.components.invoiceGroups
import kotlinx.coroutines.launch

/** Every invoice ever made, newest first, with search by customer, phone or invoice number. */
@Composable
fun InvoiceHistoryScreen(
    db: AppDatabase,
    onBackClick: () -> Unit,
    onInvoiceClick: (Int) -> Unit,
    onNewBillClick: () -> Unit
) {
    BackHandler(onBack = onBackClick)

    // null = still loading.
    var invoices by remember { mutableStateOf<List<Invoice>?>(null) }
    var query by remember { mutableStateOf("") }
    var pendingDelete by remember { mutableStateOf<Invoice?>(null) }
    var reload by remember { mutableStateOf(0) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(reload) {
        // By bill date, not by number: a bill entered late under an earlier date belongs
        // further down the list, with its own day.
        invoices = db.invoiceDao().getAllInvoices().sortedByDescending { it.createdAt }
    }

    val shown = remember(invoices, query) {
        invoices?.filter { it.matches(query) }
    }

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
                TopRow(startText = "Back", onStartClick = onBackClick)
            }

            item {
                Column(
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                    modifier = Modifier.padding(top = 24.dp)
                ) {
                    Text(
                        text = "History",
                        style = MaterialTheme.typography.headlineLarge
                    )

                    SearchField(
                        value = query,
                        onValueChange = { query = it },
                        placeholder = "Name, phone or invoice #"
                    )
                }
            }

            when {
                shown == null -> Unit

                shown.isEmpty() -> item {
                    if (query.isBlank()) {
                        EmptyState(
                            title = "No invoices yet",
                            message = "Tap + New to create your first bill."
                        )
                    } else {
                        EmptyState(
                            title = "No matches",
                            message = "Nothing found for \"${query.trim()}\"."
                        )
                    }
                }

                else -> {
                    item {
                        SmallCaps(
                            text = "${shown.size} ${if (shown.size == 1) "invoice" else "invoices"}",
                            modifier = Modifier.padding(top = 28.dp)
                        )
                    }

                    invoiceGroups(
                        invoices = shown,
                        onInvoiceClick = onInvoiceClick,
                        onDeleteClick = { pendingDelete = it }
                    )
                }
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
