package com.example.billingapp.ui.newbill

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
import com.example.billingapp.Bill
import com.example.billingapp.Product
import com.example.billingapp.ui.components.AddCircleButton
import com.example.billingapp.ui.components.QuantityStepper
import com.example.billingapp.ui.components.SearchField
import com.example.billingapp.ui.components.SmallCaps
import com.example.billingapp.ui.components.TopRow
import com.example.billingapp.util.formatRupees

/** Search the product list and set quantities. Changes go straight onto the bill. */
@Composable
fun ItemsPage(
    products: List<Product>,
    bill: Bill,
    onQuantityChange: (Product, Int) -> Unit,
    onBackClick: () -> Unit
) {
    var query by remember { mutableStateOf("") }

    val matches = remember(products, query) {
        val text = query.trim()
        products.filter {
            it.name.contains(text, ignoreCase = true) ||
                    it.category.contains(text, ignoreCase = true)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(start = 24.dp, end = 24.dp, top = 8.dp)
    ) {

        TopRow(startText = "Back", onStartClick = onBackClick)

        Text(
            text = "Add Items",
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
            items(matches, key = { it.id }) { product ->
                ProductRow(
                    product = product,
                    quantity = bill.quantityOf(product),
                    onQuantityChange = { onQuantityChange(product, it) }
                )
            }

            if (matches.isEmpty()) {
                item {
                    Text(
                        text = "No products match \"${query.trim()}\".",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 32.dp)
                    )
                }
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
                    text = "${bill.itemCount} ${if (bill.itemCount == 1) "item" else "items"}"
                )
                Text(
                    text = formatRupees(bill.subtotal),
                    style = MaterialTheme.typography.titleMedium
                )
            }

            Button(
                onClick = onBackClick,
                modifier = Modifier
                    .width(140.dp)
                    .height(52.dp)
            ) {
                Text("Done")
            }
        }
    }
}

@Composable
private fun ProductRow(
    product: Product,
    quantity: Int,
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
                text = "${formatRupees(product.price)} · ${product.category}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        if (quantity == 0) {
            AddCircleButton(onClick = { onQuantityChange(1) })
        } else {
            QuantityStepper(
                quantity = quantity,
                onChange = onQuantityChange
            )
        }
    }
}
