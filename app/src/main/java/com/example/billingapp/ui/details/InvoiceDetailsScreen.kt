package com.example.billingapp.ui.details

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
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
import com.example.billingapp.Invoice
import com.example.billingapp.InvoiceItem
import com.example.billingapp.PdfGenerator
import com.example.billingapp.ui.components.LabeledRow
import com.example.billingapp.ui.components.RoundActionButton
import com.example.billingapp.ui.components.SmallCaps
import com.example.billingapp.ui.components.TextAction
import com.example.billingapp.ui.components.TopRow
import com.example.billingapp.ui.components.TotalBox
import com.example.billingapp.util.formatDateTime
import com.example.billingapp.util.formatRupees
import com.example.billingapp.util.openPdf
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** A saved invoice: customer, items and totals, an Edit button, and a button to make the PDF. */
@Composable
fun InvoiceDetailsScreen(
    db: AppDatabase,
    invoiceId: Int,
    onBackClick: () -> Unit,
    onEditClick: () -> Unit
) {
    BackHandler(onBack = onBackClick)

    var invoice by remember { mutableStateOf<Invoice?>(null) }
    var items by remember { mutableStateOf<List<InvoiceItem>>(emptyList()) }
    var pdfUri by remember { mutableStateOf<Uri?>(null) }
    var generating by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    LaunchedEffect(invoiceId) {
        invoice = db.invoiceDao().getInvoiceById(invoiceId)
        items = db.invoiceItemDao().getItemsByInvoiceId(invoiceId)
    }

    Column(modifier = Modifier.fillMaxSize()) {

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(start = 24.dp, end = 24.dp, top = 8.dp)
        ) {

            TopRow(startText = "Back", onStartClick = onBackClick) {
                TextAction(
                    text = "Edit",
                    onClick = onEditClick,
                    contentAlignment = Alignment.CenterEnd
                )
            }

            Text(
                text = "Invoice #$invoiceId",
                style = MaterialTheme.typography.headlineLarge,
                modifier = Modifier.padding(top = 24.dp, bottom = 12.dp)
            )

            invoice?.let { current ->

                LabeledRow(label = "Date", value = formatDateTime(current.createdAt))
                HorizontalDivider()
                LabeledRow(
                    label = "Customer",
                    value = current.customerName.ifBlank { "Walk-in customer" }
                )

                if (current.phoneNumber.isNotBlank()) {
                    HorizontalDivider()
                    LabeledRow(label = "Phone", value = current.phoneNumber)
                }

                SmallCaps(
                    text = "Items",
                    modifier = Modifier.padding(top = 28.dp, bottom = 4.dp)
                )

                items.forEach { item ->
                    ItemLine(item)
                }

                Spacer(modifier = Modifier.height(12.dp))

                LabeledRow(label = "Subtotal", value = formatRupees(current.subtotal))
                HorizontalDivider()
                LabeledRow(label = "GST", value = formatRupees(current.gst))

                TotalBox(
                    total = current.total,
                    modifier = Modifier.padding(top = 12.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            pdfUri?.let { uri ->
                TextAction(
                    text = "Open PDF →",
                    onClick = { openPdf(context, uri) }
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            RoundActionButton(
                text = "PDF",
                enabled = invoice != null && !generating,
                onClick = {
                    val current = invoice ?: return@RoundActionButton

                    generating = true
                    scope.launch {
                        try {
                            val uri = withContext(Dispatchers.IO) {
                                PdfGenerator.generateInvoicePdf(
                                    context = context,
                                    invoice = current,
                                    items = items
                                )
                            }

                            pdfUri = uri

                            Toast.makeText(
                                context,
                                "PDF saved to Downloads/Monginis Bills",
                                Toast.LENGTH_SHORT
                            ).show()

                        } catch (e: Exception) {
                            if (e is CancellationException) throw e

                            Toast.makeText(
                                context,
                                "Could not create PDF",
                                Toast.LENGTH_LONG
                            ).show()
                        } finally {
                            generating = false
                        }
                    }
                }
            )
        }
    }
}

/** One purchased item: name and "₹290.00 × 3" on the left, line amount on the right. */
@Composable
private fun ItemLine(item: InvoiceItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = item.productName,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${formatRupees(item.price)} × ${item.quantity}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Text(
            text = formatRupees(item.amount),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(start = 12.dp)
        )
    }
}
