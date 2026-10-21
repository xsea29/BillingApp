package com.example.billingapp

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun InvoiceDetailsScreen(
    db: AppDatabase,
    invoiceId: Int,
    onBackClick: () -> Unit
) {

    BackHandler {
        onBackClick()
    }

    var items by remember {
        mutableStateOf<List<InvoiceItem>>(emptyList())
    }

    var invoice by remember {
        mutableStateOf<Invoice?>(null)
    }

    var pdfUri by remember {
        mutableStateOf<Uri?>(null)
    }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {

        invoice =
            db.invoiceDao()
                .getInvoiceById(invoiceId)

        items =
            db.invoiceItemDao()
                .getItemsByInvoiceId(invoiceId)
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
            text = "Invoice #$invoiceId",
            style = MaterialTheme.typography.headlineMedium
        )

        invoice?.let {

            Text(
                text = "Customer : ${it.customerName}"
            )

            Text(
                text = "Phone : ${it.phoneNumber}"
            )
        }

        items.forEach {

            Card(
                modifier = Modifier.fillMaxWidth()
            ) {

                Column(
                    modifier = Modifier.padding(12.dp)
                ) {

                    Text(
                        text = it.productName,
                        style = MaterialTheme.typography.titleMedium
                    )

                    Text(
                        text = "Qty ${it.quantity}"
                    )

                    Text(
                        text = "₹${it.price} × ${it.quantity}"
                    )

                    Text(
                        text = "₹${it.amount}"
                    )
                }
            }
        }

        invoice?.let { currentInvoice ->

            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = {

                    scope.launch(Dispatchers.IO) {

                        try {

                            val uri =
                                PdfGenerator.generateInvoicePdf(
                                    context = context,
                                    invoice = currentInvoice,
                                    items = items
                                )

                            launch(Dispatchers.Main) {

                                pdfUri = uri

                                Toast.makeText(
                                    context,
                                    "PDF saved to Downloads/Monginis Bills",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }

                        } catch (e: Exception) {

                            launch(Dispatchers.Main) {

                                Toast.makeText(
                                    context,
                                    "Could not create PDF",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    }
                }
            ) {

                Text("Download PDF")
            }

            pdfUri?.let { uri ->

                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {

                        try {

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

                            context.startActivity(intent)

                        } catch (e: Exception) {

                            Toast.makeText(
                                context,
                                "No PDF viewer found",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                ) {

                    Text("Open PDF")
                }
            }
        }

        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = onBackClick
        ) {

            Text("Back")
        }
    }
}