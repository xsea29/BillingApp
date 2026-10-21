package com.example.billingapp.ui.stock

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.billingapp.Product
import com.example.billingapp.ui.components.AddCircleButton
import com.example.billingapp.ui.components.SearchField
import com.example.billingapp.ui.components.SmallCaps
import com.example.billingapp.ui.components.StockStepper
import com.example.billingapp.ui.components.TopRow
import com.example.billingapp.ui.products.filterProducts
import com.example.billingapp.util.formatRupees

/**
 * Enter what came in: search the product list, set a quantity on each product, then Save.
 * A search that finds nothing offers to add the name as a new product right here.
 *
 * [draft] is the quantity chosen so far per product name (a product is in it as soon as it has
 * been picked, even at 0, so its stepper doesn't vanish while a number is being typed over);
 * [leftByName] is what each counted product has left now, shown so you can see what is in stock.
 */
@Composable
fun AddStockPage(
    products: List<Product>,
    leftByName: Map<String, Int>,
    draft: Map<String, Int>,
    onQuantityChange: (Product, Int) -> Unit,
    onNewProduct: (name: String) -> Unit,
    onBackClick: () -> Unit,
    onSave: () -> Unit
) {
    var query by remember { mutableStateOf("") }

    val matches = remember(products, query) { filterProducts(products, query, null) }

    val text = query.trim()
    val alreadyListed = products.any { it.name.equals(text, ignoreCase = true) }

    val picked = draft.filterValues { it > 0 }
    val pieces = picked.values.sum()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(start = 24.dp, end = 24.dp, top = 8.dp)
    ) {

        TopRow(startText = "Back", onStartClick = onBackClick)

        Text(
            text = "Add Stock",
            style = MaterialTheme.typography.headlineLarge,
            modifier = Modifier.padding(top = 24.dp, bottom = 24.dp)
        )

        SearchField(
            value = query,
            onValueChange = { query = it },
            placeholder = "Search products"
        )

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(top = 8.dp)
        ) {
            if (text.isNotEmpty() && !alreadyListed) {
                item(key = "new") {
                    NewProductRow(name = text, onClick = { onNewProduct(text) })
                }
            }

            items(matches, key = { it.id }) { product ->
                StockProductRow(
                    product = product,
                    left = leftByName[product.name],
                    quantity = draft[product.name],
                    onQuantityChange = { onQuantityChange(product, it) }
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                SmallCaps(
                    text = "${picked.size} ${if (picked.size == 1) "product" else "products"}"
                )
                Text(
                    text = "$pieces ${if (pieces == 1) "piece" else "pieces"}",
                    style = MaterialTheme.typography.titleMedium
                )
            }

            Button(
                onClick = onSave,
                enabled = picked.isNotEmpty(),
                modifier = Modifier
                    .width(140.dp)
                    .height(52.dp)
            ) {
                Text("Save")
            }
        }
    }
}

/** "Add “Cola” as a new product": shown when what was typed isn't in the list. */
@Composable
private fun NewProductRow(name: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = "Add “$name” as a new product",
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "It isn't in your product list yet",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        AddCircleButton(onClick = onClick, description = "Add as a new product")
    }
}

/** One product: name and "₹25 · Veg Snacks · 12 left" on the left, "+" or the "- 5 +" stepper on the right. */
@Composable
private fun StockProductRow(
    product: Product,
    left: Int?,
    quantity: Int?,
    onQuantityChange: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = product.name,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = listOfNotNull(
                    formatRupees(product.price),
                    product.category,
                    left?.let { "$it left" }
                ).joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        if (quantity == null) {
            AddCircleButton(
                onClick = { onQuantityChange(1) },
                description = "Add stock"
            )
        } else {
            StockStepper(
                quantity = quantity,
                onChange = onQuantityChange
            )
        }
    }
}
