package com.example.billingapp.ui.stock

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.billingapp.ui.theme.LightRed

/**
 * "Out of stock": what is short, and a way to put it right. [onAddStock] opens the Add Stock page
 * and nothing else of the Stocks section.
 */
@Composable
fun StockShortageDialog(
    shortages: List<Shortage>,
    onAddStock: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Out of stock", color = LightRed) },
        text = {
            Column {
                Text(
                    text = shortageMessage(shortages),
                    style = MaterialTheme.typography.bodyLarge,
                    color = LightRed,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "Add some stock to continue.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 12.dp)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onAddStock) {
                Text("Add stock")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}
