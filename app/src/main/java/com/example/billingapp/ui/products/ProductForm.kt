package com.example.billingapp.ui.products

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.billingapp.Product
import com.example.billingapp.ui.components.ConfirmDialog
import com.example.billingapp.ui.components.PillChip
import com.example.billingapp.ui.components.SmallCaps
import com.example.billingapp.ui.components.TopRow

/**
 * Add a product ([existing] = null) or change one. Category is free text, with the
 * categories already in use offered as chips so spellings stay consistent. [initialName] fills
 * the name when a new product is started from a search that found nothing.
 */
@Composable
fun ProductForm(
    existing: Product?,
    categories: List<String>,
    initialName: String = "",
    onBackClick: () -> Unit,
    onSave: (name: String, price: Double, category: String) -> Unit,
    onDelete: () -> Unit
) {
    var name by remember { mutableStateOf(existing?.name ?: initialName) }
    var price by remember { mutableStateOf(existing?.let { priceToInput(it.price) }.orEmpty()) }
    var category by remember { mutableStateOf(existing?.category.orEmpty()) }

    var confirmDelete by remember { mutableStateOf(false) }

    val parsedPrice = parsePrice(price)
    val canSave = name.isNotBlank() && category.isNotBlank() && parsedPrice != null
    val fieldShape = MaterialTheme.shapes.medium

    Column(modifier = Modifier.fillMaxSize()) {

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(start = 24.dp, end = 24.dp, top = 8.dp)
        ) {

            TopRow(startText = "Back", onStartClick = onBackClick)

            Text(
                text = if (existing == null) "New Product" else "Edit Product",
                style = MaterialTheme.typography.headlineLarge,
                modifier = Modifier.padding(top = 24.dp, bottom = 24.dp)
            )

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Name") },
                singleLine = true,
                shape = fieldShape,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words)
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = price,
                onValueChange = { if (isPriceInput(it)) price = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Price") },
                prefix = { Text("₹") },
                singleLine = true,
                shape = fieldShape,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = category,
                onValueChange = { category = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Category") },
                singleLine = true,
                shape = fieldShape,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words)
            )

            if (categories.isNotEmpty()) {
                SmallCaps(
                    text = "Or pick one",
                    modifier = Modifier.padding(top = 20.dp, bottom = 8.dp)
                )

                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    categories.forEach { option ->
                        PillChip(
                            text = option,
                            selected = option.equals(category.trim(), ignoreCase = true),
                            onClick = { category = option }
                        )
                    }
                }
            }

            if (existing != null) {
                TextButton(
                    onClick = { confirmDelete = true },
                    modifier = Modifier.padding(top = 24.dp)
                ) {
                    Text(
                        text = "Delete product",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }

        Button(
            onClick = {
                if (parsedPrice != null) {
                    onSave(name.trim(), parsedPrice, matchCategory(category, categories))
                }
            },
            enabled = canSave,
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .height(52.dp)
        ) {
            Text("Save")
        }
    }

    if (confirmDelete && existing != null) {
        ConfirmDialog(
            title = "Delete product?",
            message = "“${existing.name}” will be removed from your product list. " +
                    "Past invoices keep their record of it.",
            confirmLabel = "Delete",
            onConfirm = {
                confirmDelete = false
                onDelete()
            },
            onDismiss = { confirmDelete = false }
        )
    }
}
