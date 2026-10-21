package com.example.billingapp.ui.stock

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.example.billingapp.AppDatabase
import com.example.billingapp.Product
import com.example.billingapp.StockEntry
import com.example.billingapp.ui.products.ProductForm
import com.example.billingapp.ui.products.categoriesOf
import kotlinx.coroutines.launch

/**
 * Everything for entering stock, as pages that can be dropped into any screen: the search-and-quantity
 * page ([AddStockPage]) and, when a search finds nothing, the new-product form. Used by the Stocks
 * section and by New Bill (when an item is out of stock). [onSaved] is called once the stock is
 * saved; [onBackClick] when the person leaves without saving.
 */
@Composable
fun AddStockFlow(
    db: AppDatabase,
    onBackClick: () -> Unit,
    onSaved: () -> Unit
) {
    val scope = rememberCoroutineScope()

    var products by remember { mutableStateOf<List<Product>>(emptyList()) }
    var left by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }

    // What is being added, by product name. Kept here so it survives a trip to the new-product form.
    var draft by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }

    // Not null while the new-product form is open: the name typed in the search.
    var newProductName by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        products = db.productDao().getAllProducts()
        left = db.loadStockData().leftByName()
    }

    BackHandler {
        if (newProductName != null) newProductName = null else onBackClick()
    }

    val creating = newProductName

    if (creating != null) {

        ProductForm(
            existing = null,
            categories = categoriesOf(products),
            initialName = creating,
            onBackClick = { newProductName = null },
            onSave = { name, price, category ->
                scope.launch {
                    db.productDao().insert(Product(name = name, price = price, category = category))
                    products = db.productDao().getAllProducts()

                    // Straight onto the list being added, ready for its quantity.
                    draft = draft + (name to (draft[name] ?: 1))
                    newProductName = null
                }
            },
            onDelete = {}
        )

    } else {

        AddStockPage(
            products = products,
            leftByName = left,
            draft = draft,
            onQuantityChange = { product, quantity -> draft = draft + (product.name to quantity) },
            onNewProduct = { newProductName = it },
            onBackClick = onBackClick,
            onSave = {
                scope.launch {
                    val at = System.currentTimeMillis()

                    val fresh = draft
                        .filterValues { it > 0 }
                        .map { (name, quantity) -> StockEntry(productName = name, quantity = quantity, addedAt = at) }

                    if (fresh.isNotEmpty()) db.stockDao().insertAll(fresh)

                    onSaved()
                }
            }
        )
    }
}
