package com.example.billingapp.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.billingapp.Invoice
import com.example.billingapp.util.formatRupees

/** Quiet grey trash icon, for the end of a row. */
@Composable
fun DeleteButton(
    onClick: () -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier
) {
    IconButton(
        onClick = onClick,
        modifier = modifier.size(40.dp)
    ) {
        Icon(
            imageVector = Icons.Filled.Delete,
            contentDescription = contentDescription,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** "Are you sure?" box for things that can't be undone. The confirm button is red. */
@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(confirmLabel, color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

/** The confirmation used everywhere an invoice can be deleted. */
@Composable
fun DeleteInvoiceDialog(
    invoice: Invoice,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val customer = invoice.customerName.ifBlank { "walk-in customer" }

    ConfirmDialog(
        title = "Delete invoice #${invoice.id}?",
        message = "The ${formatRupees(invoice.total)} bill for $customer will be deleted for good. " +
                "This can't be undone.",
        confirmLabel = "Delete",
        onConfirm = onConfirm,
        onDismiss = onDismiss
    )
}
