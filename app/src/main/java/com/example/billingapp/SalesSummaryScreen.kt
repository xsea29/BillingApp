package com.example.billingapp

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import android.content.Intent
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SalesSummaryScreen(
    db: AppDatabase,
    onBackClick: () -> Unit
) {

    BackHandler {
        onBackClick()
    }

    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var selectedPeriod by remember {
        mutableStateOf("Today")
    }

    var expanded by remember {
        mutableStateOf(false)
    }

    var invoices by remember {
        mutableStateOf<List<Invoice>>(emptyList())
    }

    var invoiceItems by remember {
        mutableStateOf<List<InvoiceItem>>(emptyList())
    }

    fun getStartAndEnd(): Pair<Long, Long> {

        val start = Calendar.getInstance()

        when (selectedPeriod) {

            "Today" -> {

                start.set(
                    Calendar.HOUR_OF_DAY,
                    0
                )
                start.set(Calendar.MINUTE, 0)
                start.set(Calendar.SECOND, 0)
                start.set(Calendar.MILLISECOND, 0)
            }

            "This Week" -> {

                start.set(
                    Calendar.DAY_OF_WEEK,
                    start.firstDayOfWeek
                )

                start.set(
                    Calendar.HOUR_OF_DAY,
                    0
                )
                start.set(Calendar.MINUTE, 0)
                start.set(Calendar.SECOND, 0)
                start.set(Calendar.MILLISECOND, 0)
            }

            "This Month" -> {

                start.set(
                    Calendar.DAY_OF_MONTH,
                    1
                )

                start.set(
                    Calendar.HOUR_OF_DAY,
                    0
                )
                start.set(Calendar.MINUTE, 0)
                start.set(Calendar.SECOND, 0)
                start.set(Calendar.MILLISECOND, 0)
            }
        }

        val end = Calendar.getInstance()

        end.timeInMillis = start.timeInMillis

        when (selectedPeriod) {

            "Today" -> {
                end.add(Calendar.DAY_OF_MONTH, 1)
            }

            "This Week" -> {
                end.add(Calendar.WEEK_OF_YEAR, 1)
            }

            "This Month" -> {
                end.add(Calendar.MONTH, 1)
            }
        }

        return Pair(
            start.timeInMillis,
            end.timeInMillis
        )
    }

    fun loadSales() {

        scope.launch {

            val range = getStartAndEnd()

            val loadedInvoices =
                db.invoiceDao()
                    .getInvoicesBetween(
                        range.first,
                        range.second
                    )

            invoices = loadedInvoices

            if (loadedInvoices.isNotEmpty()) {

                val invoiceIds =
                    loadedInvoices.map {
                        it.id
                    }

                invoiceItems =
                    db.invoiceItemDao()
                        .getItemsByInvoiceIds(
                            invoiceIds
                        )

            } else {

                invoiceItems = emptyList()
            }
        }
    }

    LaunchedEffect(selectedPeriod) {
        loadSales()
    }

    val totalSales =
        invoices.sumOf {
            it.total
        }

    val totalItems =
        invoiceItems.sumOf {
            it.quantity
        }

    /*
     * Group invoice items by date.
     *
     * InvoiceItem doesn't contain a date,
     * so we use invoiceId to find the
     * corresponding Invoice and its createdAt.
     */
    val invoiceDateMap =
        invoices.associateBy {
            it.id
        }

    val dateFormatter =
        SimpleDateFormat(
            "dd MMM yyyy",
            Locale.getDefault()
        )

    val groupedItems =
        invoiceItems
            .groupBy { item ->

                val invoice =
                    invoiceDateMap[item.invoiceId]

                invoice?.let {
                    dateFormatter.format(
                        Date(it.createdAt)
                    )
                } ?: "Unknown Date"
            }
            .toSortedMap()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(
                rememberScrollState()
            )
            .padding(20.dp),

        verticalArrangement =
        Arrangement.spacedBy(16.dp)
    ) {

        Text(
            text = "Sales Summary",
            style =
            MaterialTheme.typography.headlineMedium
        )

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = {
                expanded = !expanded
            }
        ) {

            OutlinedTextField(
                value = selectedPeriod,
                onValueChange = {},
                readOnly = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(),
                label = {
                    Text("Sales Period")
                },
                trailingIcon = {

                    ExposedDropdownMenuDefaults
                        .TrailingIcon(
                            expanded = expanded
                        )
                }
            )

            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = {
                    expanded = false
                }
            ) {

                listOf(
                    "Today",
                    "This Week",
                    "This Month"
                ).forEach { period ->

                    DropdownMenuItem(
                        text = {
                            Text(period)
                        },
                        onClick = {

                            selectedPeriod =
                                period

                            expanded = false
                        }
                    )
                }
            }
        }

        // Total Sales

        Card(
            modifier = Modifier.fillMaxWidth()
        ) {

            Column(
                modifier =
                Modifier.padding(20.dp)
            ) {

                Text(
                    text = "Total Sales",
                    style =
                    MaterialTheme.typography
                        .titleMedium
                )

                Spacer(
                    modifier =
                    Modifier.height(8.dp)
                )

                Text(
                    text =
                    "₹${"%.2f".format(totalSales)}",
                    style =
                    MaterialTheme.typography
                        .headlineMedium
                )
            }
        }

        // Total Bills

        Card(
            modifier = Modifier.fillMaxWidth()
        ) {

            Column(
                modifier =
                Modifier.padding(20.dp)
            ) {

                Text(
                    text = "Total Bills"
                )

                Text(
                    text =
                    invoices.size.toString(),
                    style =
                    MaterialTheme.typography
                        .headlineSmall
                )
            }
        }

        // Total Items

        Card(
            modifier = Modifier.fillMaxWidth()
        ) {

            Column(
                modifier =
                Modifier.padding(20.dp)
            ) {

                Text(
                    text = "Total Items Sold"
                )

                Text(
                    text =
                    totalItems.toString(),
                    style =
                    MaterialTheme.typography
                        .headlineSmall
                )
            }
        }

        Text(
            text = "Products Sold",
            style =
            MaterialTheme.typography
                .titleLarge
        )

        if (invoiceItems.isEmpty()) {

            Text(
                text =
                "No sales found for this period."
            )

        } else {

            groupedItems.forEach { (date, itemsForDate) ->

                Card(
                    modifier =
                    Modifier.fillMaxWidth()
                ) {

                    Column(
                        modifier =
                        Modifier.padding(16.dp)
                    ) {

                        Text(
                            text = date,
                            style =
                            MaterialTheme.typography
                                .titleLarge
                        )

                        Spacer(
                            modifier =
                            Modifier.height(12.dp)
                        )

                        /*
                         * Group same products together.
                         *
                         * Example:
                         * Veg Pizza x 2
                         * Veg Pizza x 3
                         *
                         * becomes:
                         * Veg Pizza x 5
                         */
                        val productsForDate =
                            itemsForDate
                                .groupBy {
                                    it.productName
                                }

                        productsForDate.forEach {
                                (productName, productItems) ->

                            val quantity =
                                productItems.sumOf {
                                    it.quantity
                                }

                            val amount =
                                productItems.sumOf {
                                    it.amount
                                }

                            Row(
                                modifier =
                                Modifier.fillMaxWidth(),
                                horizontalArrangement =
                                Arrangement.SpaceBetween
                            ) {

                                Column(
                                    modifier =
                                    Modifier.weight(1f)
                                ) {

                                    Text(
                                        text =
                                        productName,
                                        style =
                                        MaterialTheme
                                            .typography
                                            .titleMedium
                                    )

                                    Text(
                                        text =
                                        "Qty: $quantity"
                                    )
                                }

                                Text(
                                    text =
                                    "₹${"%.2f".format(amount)}"
                                )
                            }

                            Spacer(
                                modifier =
                                Modifier.height(10.dp)
                            )
                        }

                        HorizontalDivider()

                        Spacer(
                            modifier =
                            Modifier.height(10.dp)
                        )

                        val daySubtotal =
                            itemsForDate.sumOf {
                                it.amount
                            }

                        val invoiceIdsForDate =
                            itemsForDate
                                .map { it.invoiceId }
                                .distinct()

                        val dayInvoices =
                            invoices.filter {
                                it.id in invoiceIdsForDate
                            }

                        val dayGst =
                            dayInvoices.sumOf {
                                it.gst
                            }

                        val dayTotal =
                            dayInvoices.sumOf {
                                it.total
                            }

                        Text(
                            text =
                            "Subtotal: ₹${"%.2f".format(daySubtotal)}"
                        )

                        Text(
                            text =
                            "GST: ₹${"%.2f".format(dayGst)}"
                        )

                        Spacer(
                            modifier = Modifier.height(4.dp)
                        )

                        Text(
                            text =
                            "Day Total: ₹${"%.2f".format(dayTotal)}",
                            style =
                            MaterialTheme.typography.titleMedium
                        )
                    }
                }
            }
        }

        // Overall Total

        if (invoiceItems.isNotEmpty()) {

            Card(
                modifier =
                Modifier.fillMaxWidth()
            ) {

                Column(
                    modifier =
                    Modifier.padding(20.dp)
                ) {

                    Text(
                        text = "Period Total",
                        style =
                        MaterialTheme.typography
                            .titleMedium
                    )

                    Spacer(
                        modifier =
                        Modifier.height(6.dp)
                    )

                    Text(
                        text =
                        "₹${"%.2f".format(totalSales)}",
                        style =
                        MaterialTheme.typography
                            .headlineSmall
                    )
                }
            }
        }

        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = {

                if (invoices.isEmpty()) {

                    Toast.makeText(
                        context,
                        "No sales available for this period",
                        Toast.LENGTH_SHORT
                    ).show()

                } else {

                    scope.launch {

                        try {

                            val uri =
                                SalesReportPdfGenerator
                                    .generateSalesReportPdf(
                                        context = context,
                                        invoices = invoices,
                                        invoiceItems = invoiceItems,
                                        period = selectedPeriod
                                    )

                            Toast.makeText(
                                context,
                                "Sales report saved to Downloads/Monginis Bills",
                                Toast.LENGTH_LONG
                            ).show()

                            // Open the generated PDF
                            val intent = Intent(
                                Intent.ACTION_VIEW
                            ).apply {

                                setDataAndType(
                                    uri,
                                    "application/pdf"
                                )

                                addFlags(
                                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                                )
                            }

                            try {

                                context.startActivity(intent)

                            } catch (e: Exception) {

                                // PDF is already saved even if
                                // there is no PDF viewer installed.

                            }

                        } catch (e: Exception) {

                            Toast.makeText(
                                context,
                                "Failed to generate PDF",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }
            }
        ) {
            Text("Download Sales Report PDF")
        }

        Button(
            modifier =
            Modifier.fillMaxWidth(),
            onClick = onBackClick
        ) {

            Text("Back")
        }
    }
}