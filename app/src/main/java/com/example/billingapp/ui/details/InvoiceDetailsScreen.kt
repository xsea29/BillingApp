package com.example.billingapp.ui.details

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
import androidx.compose.material3.FilledTonalButton
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
import com.example.billingapp.gstLabel
import com.example.billingapp.ui.components.LabeledRow
import com.example.billingapp.ui.components.RoundActionButton
import com.example.billingapp.ui.components.SmallCaps
import com.example.billingapp.ui.components.TextAction
import com.example.billingapp.ui.components.TopRow
import com.example.billingapp.ui.components.TotalBox
import com.example.billingapp.ui.theme.MoneyGreen
import com.example.billingapp.util.formatDateTime
import com.example.billingapp.util.formatRupees
import com.example.billingapp.util.openPdf
import com.example.billingapp.util.sharePdf
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * A saved invoice: customer, items and totals. Share (top) sends the PDF to any app, Edit changes
 * the bill, Download (bottom left) keeps a copy in Downloads, and the PDF button opens it.
 */
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
    var busy by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    /**
     * Builds the invoice PDF off the main thread and hands it to [then]. Taps are ignored while
     * one is already being made, so a double tap can't start two.
     */
    fun withPdf(failure: String, then: suspend (Invoice, File) -> Unit) {
        val current = invoice ?: return
        if (busy) return

        busy = true
        scope.launch {
            try {
                val file = withContext(Dispatchers.IO) {
                    PdfGenerator.createInvoiceFile(context, current, items)
                }
                then(current, file)
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Toast.makeText(context, failure, Toast.LENGTH_LONG).show()
            } finally {
                busy = false
            }
        }
    }

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
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextAction(
                        text = "Share",
                        onClick = {
                            withPdf("Could not make the PDF to share") { current, file ->
                                sharePdf(
                                    context = context,
                                    uri = PdfGenerator.uriFor(context, file),
                                    subject = "Invoice #${current.id} from Monginis",
                                    message = "Invoice #${current.id} from Monginis. " +
                                            "Total ${formatRupees(current.total)}. Thank you!"
                                )
                            }
                        },
                        contentAlignment = Alignment.CenterEnd
                    )

                    TextAction(
                        text = "Edit",
                        onClick = onEditClick,
                        contentAlignment = Alignment.CenterEnd
                    )
                }
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
                LabeledRow(label = gstLabel(), value = formatRupees(current.gst))

                TotalBox(
                    total = current.total,
                    modifier = Modifier.padding(top = 12.dp),
                    amountColor = MoneyGreen
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
            // Saves a copy in the phone's Downloads folder.
            FilledTonalButton(
                onClick = {
                    withPdf("Could not make the PDF") { _, file ->
                        val saved = withContext(Dispatchers.IO) {
                            PdfGenerator.saveToDownloads(context, file)
                        }

                        Toast.makeText(
                            context,
                            if (saved) {
                                "Saved to Downloads/Monginis Bills"
                            } else {
                                "Couldn't save to Downloads on this phone. Use Share to send or save the PDF."
                            },
                            Toast.LENGTH_LONG
                        ).show()
                    }
                },
                enabled = invoice != null && !busy,
                modifier = Modifier.height(52.dp)
            ) {
                Text("↓  Download")
            }

            Spacer(modifier = Modifier.weight(1f))

            // Opens the PDF to read or print.
            RoundActionButton(
                text = "PDF",
                enabled = invoice != null && !busy,
                onClick = {
                    withPdf("Could not make the PDF") { _, file ->
                        openPdf(context, PdfGenerator.uriFor(context, file))
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
