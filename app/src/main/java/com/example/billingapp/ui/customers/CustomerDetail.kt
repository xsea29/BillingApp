package com.example.billingapp.ui.customers

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.billingapp.Customer
import com.example.billingapp.Invoice
import com.example.billingapp.ui.components.ArrowCircle
import com.example.billingapp.ui.components.BorderedCard
import com.example.billingapp.ui.components.DeleteButton
import com.example.billingapp.ui.components.DeleteInvoiceDialog
import com.example.billingapp.ui.components.InitialAvatar
import com.example.billingapp.ui.components.LabeledRow
import com.example.billingapp.ui.components.MiniStat
import com.example.billingapp.ui.components.SectionHeading
import com.example.billingapp.ui.components.SmallCaps
import com.example.billingapp.ui.components.TextAction
import com.example.billingapp.ui.components.TopRow
import com.example.billingapp.util.dayLabel
import com.example.billingapp.util.dialPhone
import com.example.billingapp.util.emailTo
import com.example.billingapp.util.formatDateTime
import com.example.billingapp.util.formatRupees
import com.example.billingapp.util.textPhone

/** One customer: who they are, a way to reach them, what they've spent, and their bills. */
@Composable
fun CustomerDetail(
    customer: Customer,
    stats: CustomerStats,
    bills: List<Invoice>,
    onBackClick: () -> Unit,
    onEditClick: () -> Unit,
    onInvoiceClick: (Int) -> Unit,
    onDeleteBill: (Invoice) -> Unit
) {
    val context = LocalContext.current
    var pendingDelete by remember { mutableStateOf<Invoice?>(null) }
    val muted = MaterialTheme.colorScheme.onSurfaceVariant

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 32.dp)
    ) {

        TopRow(startText = "Back", onStartClick = onBackClick) {
            TextAction(
                text = "Edit",
                onClick = onEditClick,
                contentAlignment = Alignment.CenterEnd
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            InitialAvatar(
                name = customer.name,
                size = 96.dp,
                shape = CircleShape,
                textStyle = MaterialTheme.typography.headlineLarge
            )

            Text(
                text = customer.name,
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 16.dp)
            )

            Text(
                text = customer.phone,
                style = MaterialTheme.typography.bodyMedium,
                color = muted,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { dialPhone(context, customer.phone) },
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
            ) {
                Text("Call")
            }

            FilledTonalButton(
                onClick = { textPhone(context, customer.phone) },
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
            ) {
                Text("Message")
            }

            if (customer.email.isNotBlank()) {
                FilledTonalButton(
                    onClick = { emailTo(context, customer.email) },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                ) {
                    Text("Email")
                }
            }
        }

        BorderedCard(modifier = Modifier.padding(top = 24.dp)) {
            Row(modifier = Modifier.fillMaxWidth()) {
                MiniStat(
                    label = "Total spent",
                    value = formatRupees(stats.totalSpent),
                    modifier = Modifier.weight(1.6f)
                )
                MiniStat(
                    label = "Bills",
                    value = stats.billCount.toString(),
                    modifier = Modifier.weight(0.8f)
                )
                MiniStat(
                    label = "Last visit",
                    value = stats.lastVisit?.let { dayLabel(it) } ?: "—",
                    modifier = Modifier.weight(1.2f)
                )
            }
        }

        SectionHeading(text = "Details")

        LabeledRow(label = "Phone", value = customer.phone)
        HorizontalDivider()
        LabeledRow(label = "Email", value = customer.email.ifBlank { "—" })

        SectionHeading(text = "Bills", trailing = bills.size.toString())

        if (bills.isEmpty()) {

            Text(
                text = "No bills for this customer yet.",
                style = MaterialTheme.typography.bodyMedium,
                color = muted
            )

        } else {

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                bills.forEach { bill ->
                    BillCard(
                        bill = bill,
                        onClick = { onInvoiceClick(bill.id) },
                        onDeleteClick = { pendingDelete = bill }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
    }

    pendingDelete?.let { invoice ->
        DeleteInvoiceDialog(
            invoice = invoice,
            onConfirm = {
                pendingDelete = null
                onDeleteBill(invoice)
            },
            onDismiss = { pendingDelete = null }
        )
    }
}

/** One of the customer's bills: when, which, how much, and an arrow to open it. */
@Composable
private fun BillCard(
    bill: Invoice,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val colors = MaterialTheme.colorScheme

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(colors.surfaceVariant)
            .clickable(onClickLabel = "Open invoice ${bill.id}", onClick = onClick)
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            SmallCaps(text = formatDateTime(bill.createdAt))

            Text(
                text = "Invoice #${bill.id}",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 2.dp)
            )

            Text(
                text = formatRupees(bill.total),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold
                ),
                modifier = Modifier.padding(top = 10.dp)
            )
        }

        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ArrowCircle(
                glyph = "↗",
                container = colors.primary,
                content = colors.onPrimary
            )

            DeleteButton(
                onClick = onDeleteClick,
                contentDescription = "Delete invoice ${bill.id}"
            )
        }
    }
}
