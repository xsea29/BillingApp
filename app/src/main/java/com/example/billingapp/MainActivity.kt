package com.example.billingapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import androidx.room.Room
import com.example.billingapp.ui.theme.BillingAppTheme
import kotlinx.coroutines.launch
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Card
import androidx.compose.foundation.layout.Spacer
import kotlinx.coroutines.GlobalScope
import com.example.billingapp.ui.home.HomeScreen


class MainActivity : ComponentActivity() {

    lateinit var db: AppDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "cake_db"
        )
            .fallbackToDestructiveMigration()
            .build()

        lifecycleScope.launch {

            if (db.productDao().getAllProducts().isEmpty()) {

                db.productDao().insertAll(
                    ProductData.products
                )
            }
        }

        setContent {
            BillingAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    BillingApp(db)
                }
            }
        }
    }
}

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

    when (currentScreen) {

        "home" -> HomeScreen(
            db = db,
            onNewBillClick = {
                currentScreen = "bill"
            },
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
                currentScreen = "customers"
            },
            onProductsClick = {
                currentScreen = "products"
            }
        )

        "customers" -> CustomerScreen(
            db = db,
            onBackClick = {
                currentScreen = "home"
            }
        )

        "products" -> ProductScreen(
            db = db,
            onBackClick = {
                currentScreen = "home"
            }
        )

        "bill" -> NewBillScreen(
            db = db,
            onBackClick = {
                currentScreen = "home"
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

            }
        )
        "invoiceDetails" -> InvoiceDetailsScreen(
            db = db,
            invoiceId = selectedInvoiceId,
            onBackClick = {

                currentScreen = invoiceDetailsBackTo

            }
        )
        "salesSummary" -> SalesSummaryScreen(
            db = db,
            onBackClick = {
                currentScreen = "home"
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerScreen(
    db: AppDatabase,
    onBackClick: () -> Unit
) {
    BackHandler {
        onBackClick()
    }

    var customerName by remember {
        mutableStateOf("")
    }

    var phoneNumber by remember {
        mutableStateOf("")
    }

    var searchText by remember {
        mutableStateOf("")
    }

    var customers by remember {
        mutableStateOf<List<Customer>>(emptyList())
    }

    LaunchedEffect(Unit) {
        customers = db.customerDao().getAllCustomers()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {

        Text(
            text = "Add Customer",
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
                if (
                    it.length <= 10 &&
                    it.all { char -> char.isDigit() }
                ) {
                    phoneNumber = it
                }
            },
            modifier = Modifier.fillMaxWidth(),
            label = {
                Text("Phone Number")
            }
        )

        Button(
            onClick = {

                if (
                    customerName.isNotBlank() &&
                    phoneNumber.isNotBlank()
                ) {

                    GlobalScope.launch {

                        db.customerDao().insert(
                            Customer(
                                name = customerName,
                                phone = phoneNumber
                            )
                        )

                        val updatedCustomers =
                            db.customerDao().getAllCustomers()

                        withContext(Dispatchers.Main) {

                            customers = updatedCustomers

                            customerName = ""
                            phoneNumber = ""
                        }
                    }
                }
            }
        ) {
            Text("Save Customer")
        }

        OutlinedTextField(
            value = searchText,
            onValueChange = {
                searchText = it
            },
            modifier = Modifier.fillMaxWidth(),
            label = {
                Text("Search Customer")
            }
        )

        Text(
            text = "Saved Customers",
            style = MaterialTheme.typography.titleMedium
        )

        customers
            .filter {
                it.name.contains(searchText, ignoreCase = true) ||
                        it.phone.contains(searchText)
            }
            .forEach { customer ->

                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {

                    Column(
                        modifier = Modifier.padding(12.dp)
                    ) {

                        Text(
                            text = customer.name,
                            style = MaterialTheme.typography.titleMedium
                        )

                        Text(
                            text = customer.phone
                        )

                        Spacer(
                            modifier = Modifier.height(8.dp)
                        )

                        Button(
                            onClick = {

                                GlobalScope.launch {

                                    db.customerDao().delete(customer)

                                    val updatedCustomers =
                                        db.customerDao().getAllCustomers()

                                    withContext(Dispatchers.Main) {
                                        customers = updatedCustomers
                                    }
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

        Button(
            onClick = onBackClick
        ) {
            Text("Back")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductScreen(
    db: AppDatabase,
    onBackClick: () -> Unit
) {
    BackHandler {
        onBackClick()
    }

    var productName by remember {
        mutableStateOf("")
    }

    var price by remember {
        mutableStateOf("")
    }

    var category by remember {
        mutableStateOf("Veg Snacks")
    }
    val categories = listOf(
        "Veg Snacks",
        "Chicken Snacks",
        "Pastries",
        "Mousses",
        "Desserts"
    )
    var searchText by remember {
        mutableStateOf("")
    }

    var products by remember {
        mutableStateOf<List<Product>>(emptyList())
    }

    LaunchedEffect(Unit) {
        products = db.productDao().getAllProducts()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {

        Text(
            text = "Add Product",
            style = MaterialTheme.typography.headlineMedium
        )

        OutlinedTextField(
            value = productName,
            onValueChange = {
                productName = it
            },
            modifier = Modifier.fillMaxWidth(),
            label = {
                Text("Product Name")
            }
        )

        OutlinedTextField(
            value = price,
            onValueChange = {
                if (it.all { ch -> ch.isDigit() })
                    price = it
            },
            modifier = Modifier.fillMaxWidth(),
            label = {
                Text("Price")
            }
        )
        var expanded by remember {
            mutableStateOf(false)
        }

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = {
                expanded = !expanded
            }
        ) {

            OutlinedTextField(
                value = category,
                onValueChange = {},
                readOnly = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(),
                label = {
                    Text("Category")
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

                categories.forEach {

                    DropdownMenuItem(
                        text = {
                            Text(it)
                        },
                        onClick = {

                            category = it
                            expanded = false

                        }
                    )

                }

            }
        }

        Button(
            onClick = {

                if (
                    productName.isNotBlank() &&
                    price.isNotBlank()
                ) {

                    GlobalScope.launch {

                        db.productDao().insert(
                            Product(
                                name = productName,
                                price = price.toDouble(),
                                category = category
                            )
                        )

                        val updatedProducts =
                            db.productDao().getAllProducts()

                        withContext(Dispatchers.Main) {

                            products = updatedProducts

                            productName = ""
                            price = ""
                        }
                    }
                }
            }
        ) {
            Text("Save Product")
        }

        OutlinedTextField(
            value = searchText,
            onValueChange = {
                searchText = it
            },
            modifier = Modifier.fillMaxWidth(),
            label = {
                Text("Search Product")
            }
        )

        Text(
            text = "Saved Products",
            style = MaterialTheme.typography.titleMedium
        )

        products
            .filter {
                it.name.contains(searchText, ignoreCase = true)
            }
            .forEach { product ->

                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {

                    Column(
                        modifier = Modifier.padding(12.dp)
                    ) {

                        Text(
                            text = product.name,
                            style = MaterialTheme.typography.titleMedium
                        )

                        Text(
                            text = "₹${product.price}"
                        )

                        Spacer(
                            modifier = Modifier.height(8.dp)
                        )

                        Button(
                            onClick = {

                                GlobalScope.launch {

                                    db.productDao().delete(product)

                                    val updatedProducts =
                                        db.productDao().getAllProducts()

                                    withContext(Dispatchers.Main) {
                                        products = updatedProducts
                                    }
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

        Button(
            onClick = onBackClick
        ) {
            Text("Back")
        }
    }
}