package com.example.billingapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.billingapp.util.formatRupees

/** Outlined row with a round arrow on the right ("Add Customer →"). [subtitle] is optional detail. */
@Composable
fun ArrowRow(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null
) {
    val colors = MaterialTheme.colorScheme
    val shape = MaterialTheme.shapes.medium

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .border(1.dp, colors.outlineVariant, shape)
            .clickable(onClick = onClick)
            .heightIn(min = 60.dp)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant
                )
            }
        }

        Box(
            modifier = Modifier
                .size(36.dp)
                .background(colors.surfaceVariant, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "→",
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}

/** Label on the left, value on the right ("Subtotal      ₹390.00"). */
@Composable
fun LabeledRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    bold: Boolean = false
) {
    val weight = if (bold) FontWeight.SemiBold else FontWeight.Normal

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = weight
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = weight
        )
    }
}

/** The grey rounded box that holds the final amount (the "Amount Due" box in the design). */
@Composable
fun TotalBox(
    total: Double,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = MaterialTheme.shapes.medium
            )
            .padding(horizontal = 20.dp, vertical = 20.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Total",
            style = MaterialTheme.typography.titleMedium
        )
        Text(
            text = formatRupees(total),
            style = MaterialTheme.typography.displaySmall
        )
    }
}
