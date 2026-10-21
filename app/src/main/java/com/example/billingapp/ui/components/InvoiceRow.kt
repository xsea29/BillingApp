package com.example.billingapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.billingapp.Invoice
import com.example.billingapp.ui.theme.MoneyGreen
import com.example.billingapp.util.formatRupees
import com.example.billingapp.util.formatTime

/** Shape holding the first letter of a name, in the serif font. Rounded square by default. */
@Composable
fun InitialAvatar(
    name: String,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    shape: Shape = RoundedCornerShape(12.dp),
    textStyle: TextStyle = MaterialTheme.typography.titleLarge,
    background: Color = MaterialTheme.colorScheme.surfaceVariant
) {
    val initial = name.trim().firstOrNull()?.uppercase() ?: "#"

    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(background),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initial,
            style = textStyle
        )
    }
}

/** One invoice in a list: avatar, customer + number on the left, total + time on the right, and a delete icon if [onDeleteClick] is given. */
@Composable
fun InvoiceRow(
    invoice: Invoice,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onDeleteClick: (() -> Unit)? = null
) {
    val name = invoice.customerName.ifBlank { "Walk-in customer" }
    val muted = MaterialTheme.colorScheme.onSurfaceVariant

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        InitialAvatar(name = name)

        Spacer(modifier = Modifier.width(14.dp))

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = name,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "Invoice #${invoice.id}",
                style = MaterialTheme.typography.bodySmall,
                color = muted
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = formatRupees(invoice.total),
                style = MaterialTheme.typography.titleMedium,
                color = MoneyGreen
            )
            Text(
                text = formatTime(invoice.createdAt),
                style = MaterialTheme.typography.bodySmall,
                color = muted
            )
        }

        if (onDeleteClick != null) {
            DeleteButton(
                onClick = onDeleteClick,
                contentDescription = "Delete invoice ${invoice.id}",
                modifier = Modifier.padding(start = 4.dp)
            )
        }
    }
}
