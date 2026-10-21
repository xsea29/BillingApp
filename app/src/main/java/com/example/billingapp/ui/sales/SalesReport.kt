package com.example.billingapp.ui.sales

import com.example.billingapp.Invoice
import com.example.billingapp.InvoiceItem
import com.example.billingapp.util.SalesPeriod
import com.example.billingapp.util.roundToPaise
import com.example.billingapp.util.startOfDay
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.roundToInt

/** One bar of the chart: an hour (Today) or a day (week / month). */
data class Bucket(
    /** Short axis text: "Mon", "14", "3p". */
    val label: String,
    /** Full text for the selection line: "Wed 21 Oct", "3 PM – 4 PM". */
    val caption: String,
    val amount: Double,
    /** False for buckets whose axis label is left out so the axis stays readable. */
    val showLabel: Boolean,
    val isCurrent: Boolean
)

data class CategorySales(val name: String, val quantity: Int, val amount: Double)

data class ProductSales(val name: String, val quantity: Int, val amount: Double)

/** Everything the Sales screen shows for one period. */
data class SalesReport(
    val billCount: Int,
    /** Pieces sold: 2 cakes + 3 pastries = 5. */
    val itemCount: Int,
    val subtotal: Double,
    val gst: Double,
    val total: Double,
    val previousTotal: Double,
    val buckets: List<Bucket>,
    /** Biggest first. Amounts are before GST, like the item lines they come from. */
    val categories: List<CategorySales>,
    /** Biggest first. Amounts are before GST. */
    val products: List<ProductSales>
) {
    val topCategory: CategorySales? get() = categories.firstOrNull()

    val bestSeller: ProductSales?
        get() = products.maxWithOrNull(compareBy({ it.quantity }, { it.amount }))
}

/**
 * @param categoryOf product name -> category. Items whose product was renamed or deleted
 * since the sale land in "Uncategorised".
 * @param previousTotal sales over the same stretch of the previous period.
 */
fun buildSalesReport(
    period: SalesPeriod,
    invoices: List<Invoice>,
    items: List<InvoiceItem>,
    categoryOf: Map<String, String>,
    previousTotal: Double,
    now: Long = System.currentTimeMillis()
): SalesReport {

    val categories = items
        .groupBy { categoryOf[it.productName] ?: "Uncategorised" }
        .map { (name, lines) ->
            CategorySales(
                name = name,
                quantity = lines.sumOf { it.quantity },
                amount = roundToPaise(lines.sumOf { it.amount })
            )
        }
        .sortedByDescending { it.amount }

    val products = items
        .groupBy { it.productName }
        .map { (name, lines) ->
            ProductSales(
                name = name,
                quantity = lines.sumOf { it.quantity },
                amount = roundToPaise(lines.sumOf { it.amount })
            )
        }
        .sortedByDescending { it.amount }

    return SalesReport(
        billCount = invoices.size,
        itemCount = items.sumOf { it.quantity },
        subtotal = roundToPaise(invoices.sumOf { it.subtotal }),
        gst = roundToPaise(invoices.sumOf { it.gst }),
        total = roundToPaise(invoices.sumOf { it.total }),
        previousTotal = previousTotal,
        buckets = buildBuckets(period, invoices, now),
        categories = categories,
        products = products
    )
}

/** One day of the period: what was billed, for the "Sales by day" table of the PDF. */
data class DaySales(
    /** Midnight at the start of the day. */
    val dayStart: Long,
    val bills: Int,
    /** Pieces sold that day. */
    val items: Int,
    val subtotal: Double,
    val gst: Double,
    val total: Double
)

/** One [DaySales] per day that had bills, oldest first. */
fun dailySales(invoices: List<Invoice>, items: List<InvoiceItem>): List<DaySales> {
    val piecesByInvoice = items
        .groupBy { it.invoiceId }
        .mapValues { (_, lines) -> lines.sumOf { it.quantity } }

    return invoices
        .groupBy { startOfDay(it.createdAt) }
        .map { (day, bills) ->
            DaySales(
                dayStart = day,
                bills = bills.size,
                items = bills.sumOf { piecesByInvoice[it.id] ?: 0 },
                subtotal = roundToPaise(bills.sumOf { it.subtotal }),
                gst = roundToPaise(bills.sumOf { it.gst }),
                total = roundToPaise(bills.sumOf { it.total })
            )
        }
        .sortedBy { it.dayStart }
}

/**
 * Percent change from [previous] to [current], or null when there is nothing to compare
 * against (a percentage of zero means nothing).
 */
fun percentChange(current: Double, previous: Double): Int? {
    if (previous <= 0.0) return null
    return ((current - previous) / previous * 100).roundToInt()
}

/**
 * A donut with 20 slivers is unreadable: keep the biggest [maxShown] categories and
 * fold the rest into one "Other" slice.
 */
fun List<CategorySales>.foldTail(maxShown: Int = 5): List<CategorySales> {
    if (size <= maxShown) return this

    val rest = drop(maxShown)

    return take(maxShown) + CategorySales(
        name = "Other",
        quantity = rest.sumOf { it.quantity },
        amount = roundToPaise(rest.sumOf { it.amount })
    )
}

/** Smallest "round" number (1, 2, 5 x 10^n) that is at least [value]; the top of the chart. */
fun niceCeiling(value: Double): Double {
    if (value <= 0.0) return 1.0

    val unit = 10.0.pow(floor(log10(value)))
    val fraction = value / unit

    val nice = when {
        fraction <= 1.0 -> 1.0
        fraction <= 2.0 -> 2.0
        fraction <= 5.0 -> 5.0
        else -> 10.0
    }

    return nice * unit
}

// ---------------------------------------------------------------- buckets

private fun buildBuckets(
    period: SalesPeriod,
    invoices: List<Invoice>,
    now: Long
): List<Bucket> =
    when (period) {
        SalesPeriod.Today -> hourBuckets(invoices, now)
        SalesPeriod.Week, SalesPeriod.Month -> dayBuckets(period, invoices, now)
    }

private fun hourOf(timeMillis: Long): Int =
    Calendar.getInstance().apply { timeInMillis = timeMillis }.get(Calendar.HOUR_OF_DAY)

/** "3 PM", "12 AM". */
private fun longHour(hour: Int): String {
    val twelveHour = if (hour % 12 == 0) 12 else hour % 12
    return "$twelveHour ${if (hour % 24 < 12) "AM" else "PM"}"
}

/** "3p", "12a". */
private fun shortHour(hour: Int): String {
    val twelveHour = if (hour % 12 == 0) 12 else hour % 12
    return "$twelveHour${if (hour % 24 < 12) "a" else "p"}"
}

/**
 * One bar per hour. Shows 8 am to 9 pm at least, stretching earlier or later when
 * there were sales outside shop hours (or the clock is) so nothing is cut off.
 */
private fun hourBuckets(invoices: List<Invoice>, now: Long): List<Bucket> {
    val byHour = invoices
        .groupBy { hourOf(it.createdAt) }
        .mapValues { (_, list) -> list.sumOf { it.total } }

    val nowHour = hourOf(now)
    val first = minOf(8, nowHour, byHour.keys.minOrNull() ?: 8)
    val last = maxOf(21, nowHour, byHour.keys.maxOrNull() ?: 21)

    return (first..last).map { hour ->
        Bucket(
            label = shortHour(hour),
            caption = "${longHour(hour)} – ${longHour(hour + 1)}",
            amount = roundToPaise(byHour[hour] ?: 0.0),
            showLabel = (hour - first) % 3 == 0,
            isCurrent = hour == nowHour
        )
    }
}

/** One bar per day: 7 for a week, 28-31 for a month. */
private fun dayBuckets(
    period: SalesPeriod,
    invoices: List<Invoice>,
    now: Long
): List<Bucket> {
    val (start, end) = period.range(now)

    val byDay = invoices
        .groupBy { startOfDay(it.createdAt) }
        .mapValues { (_, list) -> list.sumOf { it.total } }

    val today = startOfDay(now)
    val labelFormat = SimpleDateFormat(if (period == SalesPeriod.Week) "EEE" else "d", Locale.getDefault())
    val captionFormat = SimpleDateFormat("EEE d MMM", Locale.getDefault())

    val cursor = Calendar.getInstance().apply { timeInMillis = start }
    val buckets = mutableListOf<Bucket>()

    while (cursor.timeInMillis < end) {
        val day = cursor.timeInMillis
        val dayOfMonth = cursor.get(Calendar.DAY_OF_MONTH)

        buckets += Bucket(
            label = labelFormat.format(Date(day)),
            caption = captionFormat.format(Date(day)),
            amount = roundToPaise(byDay[day] ?: 0.0),
            // A month has ~30 bars: label the 1st and every 5th so the axis doesn't crowd.
            showLabel = period == SalesPeriod.Week || dayOfMonth == 1 || dayOfMonth % 5 == 0,
            isCurrent = day == today
        )

        cursor.add(Calendar.DAY_OF_MONTH, 1)
    }

    return buckets
}
