package com.example.billingapp.ui.sales

import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.billingapp.AppDatabase
import com.example.billingapp.Invoice
import com.example.billingapp.InvoiceItem
import com.example.billingapp.PdfGenerator
import com.example.billingapp.R
import com.example.billingapp.ui.components.BorderedCard
import com.example.billingapp.ui.components.EmptyState
import com.example.billingapp.ui.components.MiniStat
import com.example.billingapp.ui.components.RoundActionButton
import com.example.billingapp.ui.components.SectionHeading
import com.example.billingapp.ui.components.SegmentedToggle
import com.example.billingapp.ui.components.SmallCaps
import com.example.billingapp.ui.components.TextAction
import com.example.billingapp.ui.components.TopRow
import com.example.billingapp.ui.theme.AwardGold
import com.example.billingapp.ui.theme.BarPurple
import com.example.billingapp.ui.theme.BarSoft
import com.example.billingapp.ui.theme.DeltaUp
import com.example.billingapp.ui.theme.MoneyGreen
import com.example.billingapp.util.SalesPeriod
import com.example.billingapp.util.formatRupees
import com.example.billingapp.util.openPdf
import com.example.billingapp.util.roundToPaise
import com.example.billingapp.util.sharePdf
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.roundToInt

/** What the screen needs for one period: the raw rows (for the PDF) and the numbers made from them. */
private data class SalesData(
    val period: SalesPeriod,
    val invoices: List<Invoice>,
    val items: List<InvoiceItem>,
    val report: SalesReport
)

private suspend fun AppDatabase.loadSales(period: SalesPeriod): SalesData {
    val now = System.currentTimeMillis()
    val (start, end) = period.range(now)
    val (previousStart, previousEnd) = period.previousRange(now)

    val invoices = invoiceDao().getInvoicesBetween(start, end)

    // SQLite allows ~999 values in one IN (...) list, and a busy month can have more bills.
    val items = invoices
        .map { it.id }
        .chunked(500)
        .flatMap { invoiceItemDao().getItemsByInvoiceIds(it) }

    val previousTotal = invoiceDao()
        .getInvoicesBetween(previousStart, previousEnd)
        .sumOf { it.total }

    val categoryOf = productDao().getAllProducts().associate { it.name to it.category }

    return SalesData(
        period = period,
        invoices = invoices,
        items = items,
        report = buildSalesReport(
            period = period,
            invoices = invoices,
            items = items,
            categoryOf = categoryOf,
            previousTotal = roundToPaise(previousTotal),
            now = now
        )
    )
}

/**
 * Sales for Today / This week / This month. Share (top) sends the PDF report to any app, Download
 * (bottom left) keeps a copy in Downloads, and the PDF button opens it.
 */
@Composable
fun SalesSummaryScreen(
    db: AppDatabase,
    onBackClick: () -> Unit,
    onNewBillClick: () -> Unit
) {
    BackHandler(onBack = onBackClick)

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var period by remember { mutableStateOf(SalesPeriod.Today) }

    // Keeps showing the previous period until the new one is ready, instead of flashing blank.
    var data by remember { mutableStateOf<SalesData?>(null) }
    var generating by remember { mutableStateOf(false) }

    LaunchedEffect(period) {
        data = db.loadSales(period)
    }

    val loaded = data
    val canExport = loaded != null && loaded.invoices.isNotEmpty() && !generating

    /**
     * Builds the report PDF off the main thread and hands it to [then]. Taps are ignored while
     * one is already being made, so a double tap can't start two.
     */
    fun withPdf(failure: String, then: suspend (SalesData, File) -> Unit) {
        val current = data ?: return
        if (generating) return

        generating = true
        scope.launch {
            try {
                val file = withContext(Dispatchers.IO) {
                    PdfGenerator.createSalesReportFile(
                        context = context,
                        period = current.period,
                        report = current.report,
                        invoices = current.invoices,
                        items = current.items
                    )
                }
                then(current, file)
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Toast.makeText(context, failure, Toast.LENGTH_LONG).show()
            } finally {
                generating = false
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(start = 24.dp, end = 24.dp, top = 8.dp)
        ) {

            TopRow(startText = "Back", onStartClick = onBackClick) {
                IconButton(
                    onClick = {
                        withPdf("Could not make the report to share") { current, file ->
                            sharePdf(
                                context = context,
                                uri = PdfGenerator.uriFor(context, file),
                                subject = "Monginis sales report: ${current.period.label}",
                                message = "Monginis sales report for ${current.period.label.lowercase()}. " +
                                        "Total ${formatRupees(current.report.total)} from " +
                                        "${current.report.billCount} ${if (current.report.billCount == 1) "bill" else "bills"}."
                            )
                        }
                    },
                    enabled = canExport
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_share),
                        contentDescription = "Share sales report",
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Text(
                text = "Sales",
                style = MaterialTheme.typography.headlineLarge,
                modifier = Modifier.padding(top = 24.dp, bottom = 20.dp)
            )

            SegmentedToggle(
                options = SalesPeriod.values().toList(),
                selected = period,
                label = { it.label },
                onSelect = { period = it }
            )

            if (loaded != null && loaded.invoices.isEmpty()) {

                EmptyState(
                    title = "No sales yet",
                    message = "Bills you make will show up here."
                )

                Button(
                    onClick = onNewBillClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 28.dp)
                        .height(52.dp)
                ) {
                    Text("New bill")
                }

            } else if (loaded != null) {

                SalesContent(loaded)
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Saves a copy in the phone's Downloads folder.
            FilledTonalButton(
                onClick = {
                    withPdf("Could not make the report") { _, file ->
                        val saved = withContext(Dispatchers.IO) {
                            PdfGenerator.saveToDownloads(context, file)
                        }

                        Toast.makeText(
                            context,
                            if (saved) {
                                "Saved to Downloads/Monginis Bills"
                            } else {
                                "Couldn't save to Downloads on this phone. Use Share to send or save the PDF."
                            },
                            Toast.LENGTH_LONG
                        ).show()
                    }
                },
                enabled = canExport,
                modifier = Modifier.height(52.dp),
                contentPadding = PaddingValues(start = 16.dp, end = 22.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_download),
                    contentDescription = null,
                    modifier = Modifier.size(28.dp)
                )

                Spacer(modifier = Modifier.width(10.dp))

                Text("Download")
            }

            Spacer(modifier = Modifier.weight(1f))

            // Opens the report to read or print.
            RoundActionButton(
                text = "PDF",
                enabled = canExport,
                onClick = {
                    withPdf("Could not make the report") { _, file ->
                        openPdf(context, PdfGenerator.uriFor(context, file))
                    }
                }
            )
        }
    }
}

@Composable
private fun SalesContent(data: SalesData) {
    val report = data.report
    var showAllProducts by remember(report) { mutableStateOf(false) }

    SectionHeading(text = "Overview")
    OverviewCard(report = report, period = data.period)

    SectionHeading(text = "Categories", trailing = "Before GST")
    CategoriesCard(report = report)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 24.dp)
            .height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        report.topCategory?.let { category ->
            StatTile(
                icon = R.drawable.ic_crown,
                label = "Top category",
                value = formatRupees(category.amount),
                valueColor = MoneyGreen,
                name = category.name,
                detail = "${category.quantity} ${if (category.quantity == 1) "item" else "items"}",
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            )
        }

        report.bestSeller?.let { product ->
            StatTile(
                icon = R.drawable.ic_star,
                label = "Best seller",
                value = "${product.quantity} sold",
                name = product.name,
                detail = formatRupees(product.amount),
                detailColor = MoneyGreen,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            )
        }
    }

    SectionHeading(text = "Top products", trailing = "Before GST")

    val shownProducts = if (showAllProducts) report.products else report.products.take(PRODUCTS_SHOWN)
    val biggest = report.products.maxOfOrNull { it.amount } ?: 0.0

    shownProducts.forEach { product ->
        AmountRow(
            name = product.name,
            detail = "${product.quantity} sold",
            amount = product.amount,
            fraction = if (biggest > 0) (product.amount / biggest).toFloat() else 0f
        )
    }

    if (report.products.size > PRODUCTS_SHOWN) {
        TextAction(
            text = if (showAllProducts) "Show less" else "Show all ${report.products.size}",
            onClick = { showAllProducts = !showAllProducts }
        )
    }
}

private const val PRODUCTS_SHOWN = 6

@Composable
private fun OverviewCard(report: SalesReport, period: SalesPeriod) {
    // Starts on "now" (this hour / today); moves when the chart is tapped or dragged.
    var selected by remember(report) {
        mutableStateOf(report.buckets.indexOfFirst { it.isCurrent }.coerceAtLeast(0))
    }
    val bucket = report.buckets.getOrNull(selected)

    BorderedCard {
        SmallCaps(text = period.label)

        Text(
            text = formatRupees(report.total),
            style = MaterialTheme.typography.displaySmall,
            color = MoneyGreen,
            modifier = Modifier.padding(top = 4.dp)
        )

        DeltaLine(
            current = report.total,
            previous = report.previousTotal,
            previousLabel = period.previousLabel
        )

        // The value of the selected bar, always on screen (a tooltip would be touch-only).
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SmallCaps(text = bucket?.caption.orEmpty())
            Text(
                text = formatRupees(bucket?.amount ?: 0.0),
                style = MaterialTheme.typography.titleMedium,
                color = MoneyGreen
            )
        }

        SalesBarChart(
            buckets = report.buckets,
            selectedIndex = selected,
            onSelect = { selected = it }
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
            MiniStat(label = "Bills", value = report.billCount.toString(), modifier = Modifier.weight(1f))
            MiniStat(label = "Items", value = report.itemCount.toString(), modifier = Modifier.weight(1f))
            MiniStat(label = "GST", value = formatRupees(report.gst), modifier = Modifier.weight(1.4f))
        }
    }
}

/** "▲ 13% vs yesterday so far", green when up, red when down; the arrow and words carry it without colour. */
@Composable
private fun DeltaLine(current: Double, previous: Double, previousLabel: String) {
    val percent = percentChange(current, previous)
    val muted = MaterialTheme.colorScheme.onSurfaceVariant

    val (text, color) = when {
        percent == null -> "No sales $previousLabel to compare" to muted
        percent > 0 -> "▲ $percent% vs $previousLabel so far" to DeltaUp
        percent < 0 -> "▼ ${-percent}% vs $previousLabel so far" to MaterialTheme.colorScheme.error
        else -> "Same as $previousLabel so far" to muted
    }

    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = color,
        modifier = Modifier.padding(top = 4.dp)
    )
}

/** Every category with its share of sales: name, "60% · 24 items", amount, and a share bar. */
@Composable
private fun CategoriesCard(report: SalesReport) {
    val total = report.categories.sumOf { it.amount }

    BorderedCard {
        report.categories.forEach { category ->
            val share = if (total > 0) category.amount / total else 0.0
            val percent = (share * 100).roundToInt()

            AmountRow(
                name = category.name,
                detail = "${if (percent == 0) "<1" else percent.toString()}% · " +
                        "${category.quantity} ${if (category.quantity == 1) "item" else "items"}",
                amount = category.amount,
                fraction = share.toFloat(),
                verticalPadding = 8.dp
            )
        }
    }
}

/**
 * Soft grey tile: a crown or star and a label, one big figure, what it belongs to, and a small
 * white pill with the detail. [valueColor] and [detailColor] let money be green.
 */
@Composable
private fun StatTile(
    @DrawableRes icon: Int,
    label: String,
    value: String,
    name: String,
    detail: String,
    modifier: Modifier = Modifier,
    valueColor: Color = Color.Unspecified,
    detailColor: Color = Color.Unspecified
) {
    val colors = MaterialTheme.colorScheme

    Column(
        modifier = modifier
            .clip(MaterialTheme.shapes.large)
            .background(colors.surfaceVariant)
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(colors.surface),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(icon),
                    contentDescription = null,
                    tint = AwardGold,
                    modifier = Modifier.size(16.dp)
                )
            }

            SmallCaps(text = label)
        }

        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium.copy(
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold
            ),
            color = valueColor,
            maxLines = 1,
            modifier = Modifier.padding(top = 14.dp)
        )

        // Two lines reserved, so the pills of two tiles side by side line up.
        Text(
            text = name,
            style = MaterialTheme.typography.titleMedium,
            minLines = 2,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 2.dp)
        )

        Box(
            modifier = Modifier
                .padding(top = 12.dp)
                .clip(CircleShape)
                .background(colors.surface)
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Text(
                text = detail,
                style = MaterialTheme.typography.labelLarge,
                color = detailColor,
                maxLines = 1
            )
        }
    }
}

/** A name, a line of detail under it, an amount on the right, and a thin purple bar for [fraction] of the width. */
@Composable
private fun AmountRow(
    name: String,
    detail: String,
    amount: Double,
    fraction: Float,
    verticalPadding: Dp = 10.dp
) {
    Column(modifier = Modifier.padding(vertical = verticalPadding)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = detail,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = formatRupees(amount),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(start = 12.dp)
            )
        }

        Box(
            modifier = Modifier
                .padding(top = 8.dp)
                .fillMaxWidth()
                .height(4.dp)
                .clip(CircleShape)
                .background(BarSoft)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(fraction.coerceIn(0.02f, 1f))
                    .background(BarPurple)
            )
        }
    }
}
