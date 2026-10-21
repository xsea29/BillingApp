package com.example.billingapp.ui.newbill

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.billingapp.AppDatabase
import com.example.billingapp.Bill
import com.example.billingapp.BillItem
import com.example.billingapp.Customer
import com.example.billingapp.GST_RATE
import com.example.billingapp.billFromInvoice
import com.example.billingapp.nextInvoiceNumber
import com.example.billingapp.Product
import com.example.billingapp.saveBill
import com.example.billingapp.updateBill
import com.example.billingapp.ui.components.ArrowRow
import com.example.billingapp.ui.components.LabeledRow
import com.example.billingapp.ui.components.QuantityStepper
import com.example.billingapp.ui.components.RoundActionButton
import com.example.billingapp.ui.components.SmallCaps
import com.example.billingapp.ui.components.TopRow
import com.example.billingapp.ui.components.TotalBox
import com.example.billingapp.util.dayLabel
import com.example.billingapp.util.formatRupees
import com.example.billingapp.util.moveToPickedDay
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

private enum class Step { Bill, Customer, Items }

/**
 * Makes a new bill, or (when [editInvoiceId] is given) changes a saved one: same page, with
 * the customer, items and date already filled in, and an "Update" button.
 */
@Composable
fun NewBillScreen(
    db: AppDatabase,
    editInvoiceId: Int? = null,
    onBackClick: () -> Unit,
    onSaved: (invoiceId: Int) -> Unit
) {
    val editing = editInvoiceId != null

    var step by remember { mutableStateOf(Step.Bill) }
    var bill by remember { mutableStateOf(Bill()) }
    var showDatePicker by remember { mutableStateOf(false) }

    // When editing, wait for the saved bill to load so the page doesn't flash empty.
    var ready by remember { mutableStateOf(!editing) }

    var products by remember { mutableStateOf<List<Product>>(emptyList()) }
    var customers by remember { mutableStateOf<List<Customer>>(emptyList()) }
    var newInvoiceNumber by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(Unit) {
        products = db.productDao().getAllProducts()
        customers = db.customerDao().getAllCustomers()

        if (editInvoiceId == null) {
            newInvoiceNumber = nextInvoiceNumber(db.invoiceDao().getLastInvoiceId())
        } else {
            bill = billFromInvoice(
                invoice = db.invoiceDao().getInvoiceById(editInvoiceId),
                items = db.invoiceItemDao().getItemsByInvoiceId(editInvoiceId),
                catalogue = products
            )
            ready = true
        }
    }

    // Back steps out of the customer / items page first, and only then leaves the screen.
    BackHandler {
        if (step == Step.Bill) onBackClick() else step = Step.Bill
    }

    if (!ready) {
        Box(modifier = Modifier.fillMaxSize())
    } else {
        when (step) {

            Step.Bill -> BillPage(
                bill = bill,
                editing = editing,
                invoiceNumber = editInvoiceId ?: newInvoiceNumber,
                onQuantityChange = { product, quantity ->
                    bill = bill.withQuantity(product, quantity)
                },
                onDateClick = { showDatePicker = true },
                onCustomerClick = { step = Step.Customer },
                onAddItemClick = { step = Step.Items },
                onBackClick = onBackClick,
                save = { toSave ->
                    if (editInvoiceId == null) {
                        db.saveBill(toSave)
                    } else {
                        db.updateBill(editInvoiceId, toSave)
                        editInvoiceId
                    }
                },
                onSaved = onSaved
            )

            Step.Customer -> CustomerPage(
                customers = customers,
                initialName = bill.customerName,
                initialPhone = bill.customerPhone,
                onBackClick = { step = Step.Bill },
                onDone = { name, phone ->
                    bill = bill.withCustomer(name, phone)
                    step = Step.Bill
                }
            )

            Step.Items -> ItemsPage(
                products = products,
                bill = bill,
                onQuantityChange = { product, quantity ->
                    bill = bill.withQuantity(product, quantity)
                },
                onBackClick = { step = Step.Bill }
            )
        }

        if (showDatePicker) {
            InvoiceDatePickerDialog(
                current = bill.createdAt,
                onDismiss = { showDatePicker = false },
                onPick = { picked ->
                    bill = bill.withDate(moveToPickedDay(bill.createdAt, picked))
                }
            )
        }
    }
}

@Composable
private fun BillPage(
    bill: Bill,
    editing: Boolean,
    invoiceNumber: Int?,
    onQuantityChange: (Product, Int) -> Unit,
    onDateClick: () -> Unit,
    onCustomerClick: () -> Unit,
    onAddItemClick: () -> Unit,
    onBackClick: () -> Unit,
    save: suspend (Bill) -> Int,
    onSaved: (Int) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var saving by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(start = 24.dp, end = 24.dp, top = 8.dp)
        ) {

            TopRow(startText = "Back", onStartClick = onBackClick)

            Text(
                text = if (editing) "Update Bill" else "New Bill",
                style = MaterialTheme.typography.headlineLarge,
                modifier = Modifier.padding(top = 24.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = invoiceNumber?.let { "Invoice #$it" } ?: "New invoice",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = if (editing) "Editing" else "Draft",
                    style = MaterialTheme.typography.titleMedium
                )
            }

            LabeledRow(
                label = "Invoice date",
                value = dayLabel(bill.createdAt),
                onClick = onDateClick
            )
            HorizontalDivider()

            Spacer(modifier = Modifier.height(20.dp))

            ArrowRow(
                title = bill.customerName.ifBlank { "Add Customer" },
                subtitle = bill.customerPhone.ifBlank { null },
                onClick = onCustomerClick
            )

            bill.items.forEach { item ->
                BillLine(
                    item = item,
                    onQuantityChange = { onQuantityChange(item.product, it) }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            ArrowRow(
                title = "Add Item",
                subtitle = if (bill.items.isEmpty()) null else "${bill.itemCount} added",
                onClick = onAddItemClick
            )

            Spacer(modifier = Modifier.height(12.dp))

            LabeledRow(label = "Subtotal", value = formatRupees(bill.subtotal))
            HorizontalDivider()
            LabeledRow(
                label = "GST ${(GST_RATE * 100).toInt()}%",
                value = formatRupees(bill.gst)
            )

            TotalBox(
                total = bill.total,
                modifier = Modifier.padding(top = 12.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SmallCaps(
                text = "${bill.itemCount} ${if (bill.itemCount == 1) "item" else "items"}"
            )

            RoundActionButton(
                text = if (editing) "Update" else "Save",
                enabled = bill.items.isNotEmpty() && !saving,
                onClick = {
                    saving = true
                    scope.launch {
                        try {
                            onSaved(save(bill))
                        } catch (e: Exception) {
                            if (e is CancellationException) throw e
                            saving = false
                            Toast.makeText(
                                context,
                                if (editing) "Could not update the bill" else "Could not save the bill",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }
            )
        }
    }
}

/** One line on the bill: name, price each, quantity stepper, and the line amount. */
@Composable
private fun BillLine(
    item: BillItem,
    onQuantityChange: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp),
        verticalAlignment = Alignment.Top
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = item.product.name,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${formatRupees(item.product.price)} each",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            QuantityStepper(
                quantity = item.quantity,
                onChange = onQuantityChange
            )
        }

        Text(
            text = formatRupees(item.amount()),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(start = 12.dp)
        )
    }
}
