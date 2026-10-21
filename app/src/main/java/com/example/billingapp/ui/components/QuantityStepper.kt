package com.example.billingapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.billingapp.util.MAX_STOCK
import com.example.billingapp.util.isStockInput
import com.example.billingapp.util.parseStock

/** Grey pill with "−  2  +". Going below 1 reports 0, so the caller can remove the line. */
@Composable
fun QuantityStepper(
    quantity: Int,
    onChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        verticalAlignment = Alignment.CenterVertically
    ) {
        StepButton(symbol = "−", description = "Decrease quantity") {
            onChange(quantity - 1)
        }

        Text(
            text = quantity.toString(),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(min = 28.dp)
        )

        StepButton(symbol = "+", description = "Increase quantity") {
            onChange(quantity + 1)
        }
    }
}

/**
 * The same grey pill for a stock count: "−  12  +". The number can also be tapped and typed over,
 * so a delivery of 40 doesn't take 40 taps. Never goes below 0.
 */
@Composable
fun StockStepper(
    quantity: Int,
    onChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    // What is typed so far. Starts again from [quantity] whenever the buttons change it.
    var text by remember(quantity) { mutableStateOf(quantity.toString()) }
    val ink = MaterialTheme.colorScheme.onSurface

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        verticalAlignment = Alignment.CenterVertically
    ) {
        StepButton(symbol = "−", description = "Decrease quantity") {
            onChange(maxOf(quantity - 1, 0))
        }

        BasicTextField(
            value = text,
            onValueChange = {
                if (isStockInput(it)) {
                    text = it
                    onChange(parseStock(it))
                }
            },
            singleLine = true,
            textStyle = MaterialTheme.typography.titleMedium.copy(
                color = ink,
                textAlign = TextAlign.Center
            ),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            cursorBrush = SolidColor(ink),
            modifier = Modifier
                .width(64.dp)
                .semantics { contentDescription = "Quantity in stock" }
        )

        StepButton(symbol = "+", description = "Increase quantity") {
            onChange(minOf(quantity + 1, MAX_STOCK))
        }
    }
}

/** Round grey "+" used when a product isn't in the bill (or the stock list) yet. */
@Composable
fun AddCircleButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    description: String = "Add to bill"
) {
    Box(
        modifier = modifier
            .size(40.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .semantics { contentDescription = description }
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "+",
            style = MaterialTheme.typography.titleMedium
        )
    }
}

@Composable
private fun StepButton(
    symbol: String,
    description: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .semantics { contentDescription = description }
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = symbol,
            style = MaterialTheme.typography.titleMedium
        )
    }
}
