package com.example.billingapp

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.material3.Divider
import androidx.compose.material3.Card
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Icon
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.activity.compose.BackHandler



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewBillScreen(
    db: AppDatabase,
    onBackClick: () -> Unit
) {

    BackHandler {
        onBackClick()
    }

    var customerName by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }

    var selectedProduct by remember { mutableStateOf("") }
    var searchProduct by remember {
        mutableStateOf("")
    }
    var quantity by remember { mutableStateOf("") }
    var expanded by remember {
        mutableStateOf(false)
    }

    var products by remember {
        mutableStateOf<List<Product>>(emptyList())
    }

    var billItems by remember {
        mutableStateOf<List<BillItem>>(emptyList())
    }

    LaunchedEffect(Unit) {
        products = db.productDao().getAllProducts()
    }

    val subtotal = billItems.sumOf { it.amount() }
    val gst = subtotal * 0.05
    val total = subtotal + gst

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),

        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {

        Text(
            text = "New Bill",
            style = MaterialTheme.typography.headlineMedium
        )

        OutlinedTextField(
            value = customerName,
            onValueChange = {
                customerName = it
            },
            modifier = Modifier.fillMaxWidth(),
            label = {
                Text("Customer Name")
            }
        )

        OutlinedTextField(
            value = phoneNumber,
            onValueChange = {

                if (it.length <= 10) {
                    phoneNumber = it
                }

            },
            modifier = Modifier.fillMaxWidth(),
            label = {
                Text("Phone Number")
            }
        )

        Text(
            text = "Select Product"
        )

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = {
                expanded = !expanded
            }
        ) {

            OutlinedTextField(
                value = searchProduct,
                onValueChange = {
                    searchProduct = it
                    expanded = true
                },
                readOnly = false,
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(),
                label = {
                    Text("Search Product")
                },
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(
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

                products
                    .filter {
                        it.name.contains(
                            searchProduct,
                            ignoreCase = true
                        )
                    }
                    .forEach { product ->

                        DropdownMenuItem(
                            text = {
                                Text("${product.name} ₹${product.price}")
                            },
                            onClick = {
                                selectedProduct = product.name
                                searchProduct = product.name
                                expanded = false
                            }
                        )
                    }
            }
        }
        OutlinedTextField(
            value = selectedProduct,
            onValueChange = {},
            modifier = Modifier.fillMaxWidth(),
            label = {
                Text("Selected Product")
            }
        )

        OutlinedTextField(
            value = quantity,
            onValueChange = {
                quantity = it
            },
            modifier = Modifier.fillMaxWidth(),
            label = {
                Text("Quantity")
            }
        )

        Button(
            onClick = {

                val product =
                    products.find {
                        it.name == selectedProduct
                    }

                if (
                    product != null &&
                    quantity.isNotBlank()
                ) {

                    billItems = billItems + BillItem(
                        product,
                        quantity.toInt()
                    )

                    quantity = ""
                    selectedProduct = ""
                    searchProduct = ""
                }
            }
        ) {
            Text("Add Item")
        }

        Text(
            text = "Bill Items",
            style = MaterialTheme.typography.titleLarge
        )

        billItems.forEach { item ->

            Card(
                modifier = Modifier.fillMaxWidth()
            ) {

                Column(
                    modifier = Modifier.padding(12.dp)
                ) {

                    Text(
                        text = item.product.name,
                        style = MaterialTheme.typography.titleMedium
                    )

                    Text(
                        text = "Qty ${item.quantity}"
                    )

                    Text(
                        text = "₹${item.amount()}"
                    )

                    Spacer(
                        modifier = Modifier.height(8.dp)
                    )

                    Button(
                        onClick = {

                            billItems =
                                billItems.filter {
                                    it != item
                                }

                        }
                    ) {

                        Text("Delete")

                    }

                }

            }

            Spacer(
                modifier = Modifier.height(8.dp)
            )

        }

        Divider()

        Text(
            text = "Subtotal ₹$subtotal"
        )

        Text(
            text = "GST 5% ₹$gst"
        )

        Text(
            text = "Total ₹$total",
            style = MaterialTheme.typography.headlineSmall
        )

        Button(
            onClick = {

                CoroutineScope(Dispatchers.IO).launch {

                    val invoiceId = db.invoiceDao().insert(

                        Invoice(
                            customerName = customerName,
                            phoneNumber = phoneNumber,
                            subtotal = subtotal,
                            gst = gst,
                            total = total
                        )

                    ).toInt()

                    billItems.forEach {

                        db.invoiceItemDao().insert(

                            InvoiceItem(
                                invoiceId = invoiceId,
                                productName = it.product.name,
                                quantity = it.quantity,
                                price = it.product.price,
                                amount = it.amount()
                            )

                        )

                    }

                }

            }
        ) {
            Text("Save Bill")
        }

        Button(
            onClick = onBackClick
        ) {
            Text("Back")
        }
    }
}