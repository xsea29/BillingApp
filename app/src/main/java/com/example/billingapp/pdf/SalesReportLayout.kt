package com.example.billingapp.pdf

import com.example.billingapp.Invoice
import com.example.billingapp.InvoiceItem
import com.example.billingapp.ui.sales.DaySales
import com.example.billingapp.ui.sales.SalesReport
import com.example.billingapp.ui.sales.dailySales
import com.example.billingapp.ui.sales.percentChange
import com.example.billingapp.util.SalesPeriod
import com.example.billingapp.util.formatRupees
import com.example.billingapp.util.formatTime
import com.example.billingapp.util.roundToPaise
import com.example.billingapp.util.startOfDay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

/*
 * The sales report PDF, laid out the same way as the invoice (see PdfLayout.kt): plain "draw
 * this" instructions per page, no Android in sight, black, white and greys only.
 */

/** Everything the sales report shows. */
data class SalesReportInput(
    val periodLabel: String,
    /** "19 – 21 October 2026". */
    val rangeText: String,
    /** "last week": what the change in the summary is measured against. */
    val previousLabel: String,
    val generatedAt: Long,
    val report: SalesReport,
    val days: List<DaySales>
)

fun salesReportInput(
    period: SalesPeriod,
    report: SalesReport,
    invoices: List<Invoice>,
    items: List<InvoiceItem>,
    now: Long
) = SalesReportInput(
    periodLabel = period.label,
    rangeText = salesRangeText(period, now),
    previousLabel = period.previousLabel,
    generatedAt = now,
    report = report,
    days = dailySales(invoices, items)
)

/**
 * The days a report covers, in words. A week or month that is still going is reported up to
 * today, like the numbers are: "1 – 21 October 2026", not "1 – 31 October 2026".
 */
fun salesRangeText(period: SalesPeriod, now: Long): String {
    val (start, end) = period.range(now)

    val first = startOfDay(start)
    val last = minOf(startOfDay(end - 1), startOfDay(now))

    fun format(pattern: String, time: Long): String =
        SimpleDateFormat(pattern, Locale.getDefault()).format(Date(time))

    return when {
        first == last -> format("EEEE d MMMM yyyy", first)
        format("yyyyMM", first) == format("yyyyMM", last) ->
            "${format("d", first)} – ${format("d MMMM yyyy", last)}"
        format("yyyy", first) == format("yyyy", last) ->
            "${format("d MMM", first)} – ${format("d MMM yyyy", last)}"
        else -> "${format("d MMM yyyy", first)} – ${format("d MMM yyyy", last)}"
    }
}

/** "Sales_Report_This_week_2026-10-21.pdf": one file per kind of report per day. */
fun salesReportFileName(period: SalesPeriod, now: Long): String =
    "Sales_Report_${period.label.replace(" ", "_")}_" +
            "${SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(now))}.pdf"

private const val LEFT = 40f
private const val RIGHT = 555f

private const val ROW_HEIGHT = 24f
private const val TABLE_HEADER_HEIGHT = 24f

// Tables up to this many rows are never split over two pages.
private const val SHORT_TABLE_ROWS = 10

// Nothing but the footer is drawn below this line.
private const val CONTENT_BOTTOM = 760f
private const val FOOTER_LINE_Y = 792f

// The share bars of the category table.
private const val BAR_X = 305f
private const val BAR_WIDTH = 100f

/** One column of a table. [maxWidth] above 0 cuts longer text with "…". */
private class Col(
    val label: String,
    val x: Float,
    val align: Align = Align.Left,
    val maxWidth: Float = 0f,
    val bold: Boolean = false,
    val gray: Int = 17
)

/** Lays the report out on as many A4 pages as its tables need. */
fun layoutSalesReport(input: SalesReportInput, measure: Measure): List<PdfPage> {

    val report = input.report

    val pages = mutableListOf<MutableList<PdfOp>>()
    var ops = mutableListOf<PdfOp>()
    pages += ops

    /** A small running header for pages after the first; returns where content may start. */
    fun newPage(): Float {
        ops = mutableListOf()
        pages += ops

        ops += PdfOp.Text("Monginis", LEFT, 46f, 16f, Face.Serif)
        ops += PdfOp.Text("Sales Report (continued)", RIGHT, 45f, 9.5f, Face.Sans, Align.Right, gray = 110)
        ops += PdfOp.Line(LEFT, 58f, RIGHT, 58f, 0.8f, 60)

        return 76f
    }

    // ------------------------------------------------------------ title and period

    ops += PdfOp.Text("Monginis", LEFT, 62f, 30f, Face.Serif)
    ops += PdfOp.Text("Sales Report", RIGHT, 62f, 30f, Face.Serif, Align.Right)
    ops += PdfOp.Line(LEFT, 80f, RIGHT, 80f, 0.9f, 50)

    ops += PdfOp.Text("PERIOD", LEFT, 108f, 8.5f, Face.SansBold, gray = 120, spacing = 0.12f)
    ops += PdfOp.Text(input.periodLabel, LEFT, 128f, 13f, Face.SansBold)
    ops += PdfOp.Text(fit(input.rangeText, 300f, 10.5f, Face.Sans, measure), LEFT, 145f, 10.5f, gray = 90)

    val metaLabelX = 372f
    val generatedDate = SimpleDateFormat("d MMMM yyyy", Locale.getDefault()).format(Date(input.generatedAt))

    fun meta(baseline: Float, label: String, value: String) {
        ops += PdfOp.Text(label, metaLabelX, baseline, 8.5f, Face.SansBold, gray = 120, spacing = 0.12f)
        ops += PdfOp.Text(value, RIGHT, baseline, 10.5f, Face.Sans, Align.Right)
    }

    meta(108f, "GENERATED", generatedDate)
    meta(126f, "TIME", formatTime(input.generatedAt))

    // ------------------------------------------------------------ the four headline figures

    val tileTop = 172f
    val tileHeight = 58f
    val tileGap = 10f
    val tileWidth = (RIGHT - LEFT - 3 * tileGap) / 4

    val average = if (report.billCount > 0) roundToPaise(report.total / report.billCount) else 0.0

    val tiles = listOf(
        "TOTAL SALES" to formatRupees(report.total),
        "BILLS" to report.billCount.toString(),
        "ITEMS SOLD" to report.itemCount.toString(),
        "AVG. BILL" to formatRupees(average)
    )

    tiles.forEachIndexed { index, (name, value) ->
        val x = LEFT + index * (tileWidth + tileGap)

        ops += PdfOp.Box(x, tileTop, tileWidth, tileHeight, gray = 243)
        ops += PdfOp.Text(name, x + 12f, tileTop + 20f, 8f, Face.SansBold, gray = 120, spacing = 0.12f)

        val size = fitSize(value, tileWidth - 24f, 16f, Face.SansBold, measure)
        ops += PdfOp.Text(value, x + 12f, tileTop + 43f, size, Face.SansBold)
    }

    val detailBaseline = tileTop + tileHeight + 24f

    ops += PdfOp.Text(
        "Before GST ${formatRupees(report.subtotal)}   ·   GST ${formatRupees(report.gst)}",
        LEFT, detailBaseline, 10.5f, gray = 90
    )
    ops += PdfOp.Text(
        changeText(report, input.previousLabel),
        RIGHT, detailBaseline, 10.5f, Face.SansBold, Align.Right, gray = 50
    )

    var y = detailBaseline + 26f

    // ------------------------------------------------------------ tables

    /**
     * A titled table that carries on over as many pages as it needs, repeating its column
     * headings. [total] is a closing row; [decorate] draws extras on a row (the share bars).
     */
    fun table(
        title: String,
        note: String,
        cols: List<Col>,
        rows: List<List<String>>,
        total: List<String>? = null,
        decorate: (index: Int, rowTop: Float) -> Unit = { _, _ -> }
    ) {
        if (rows.isEmpty()) return

        fun columnHeadings() {
            ops += PdfOp.Box(LEFT, y, RIGHT - LEFT, TABLE_HEADER_HEIGHT, gray = 243)

            for (col in cols) {
                if (col.label.isEmpty()) continue
                ops += PdfOp.Text(col.label, col.x, y + 16f, 8.5f, Face.SansBold, col.align, gray = 90, spacing = 0.1f)
            }

            y += TABLE_HEADER_HEIGHT
        }

        // The title never sits alone at the bottom of a page. A short table stays in one piece
        // (it moves to the next page if it doesn't fit here); a long one keeps just two rows with its title.
        val keptTogether = if (rows.size <= SHORT_TABLE_ROWS) {
            rows.size + if (total != null) 1 else 0
        } else {
            minOf(rows.size, 2)
        }

        if (y + 20f + TABLE_HEADER_HEIGHT + keptTogether * ROW_HEIGHT > CONTENT_BOTTOM) y = newPage()

        ops += PdfOp.Text(title.uppercase(), LEFT, y + 10f, 9f, Face.SansBold, gray = 40, spacing = 0.12f)
        if (note.isNotEmpty()) {
            ops += PdfOp.Text(note, RIGHT, y + 10f, 8.5f, Face.Sans, Align.Right, gray = 130)
        }
        y += 20f

        columnHeadings()

        rows.forEachIndexed { index, cells ->

            // The last row stays with the closing row instead of leaving it alone on a page.
            val needed = if (index == rows.lastIndex && total != null) 2 * ROW_HEIGHT else ROW_HEIGHT

            if (y + needed > CONTENT_BOTTOM) {
                y = newPage()
                columnHeadings()
            }

            cols.forEachIndexed { c, col ->
                if (cells[c].isEmpty()) return@forEachIndexed

                val face = if (col.bold) Face.SansBold else Face.Sans
                val text = if (col.maxWidth > 0f) fit(cells[c], col.maxWidth, 10.5f, face, measure) else cells[c]

                ops += PdfOp.Text(text, col.x, y + 16f, 10.5f, face, col.align, gray = col.gray)
            }

            decorate(index, y)

            ops += PdfOp.Line(LEFT, y + ROW_HEIGHT, RIGHT, y + ROW_HEIGHT, 0.5f, 226)
            y += ROW_HEIGHT
        }

        if (total != null) {
            ops += PdfOp.Box(LEFT, y, RIGHT - LEFT, ROW_HEIGHT, gray = 243)

            cols.forEachIndexed { c, col ->
                if (total[c].isNotEmpty()) {
                    ops += PdfOp.Text(total[c], col.x, y + 16f, 10.5f, Face.SansBold, col.align)
                }
            }

            y += ROW_HEIGHT
        }

        y += 26f
    }

    // Sales by day: only worth a table when there is more than one day to compare.
    if (input.days.size > 1) {
        val dayFormat = SimpleDateFormat("EEE d MMM", Locale.getDefault())

        table(
            title = "Sales by day",
            note = "Days with sales",
            cols = listOf(
                Col("DATE", 50f),
                Col("BILLS", 160f, Align.Right),
                Col("ITEMS", 210f, Align.Right),
                Col("BEFORE GST", 330f, Align.Right),
                Col("GST", 440f, Align.Right),
                Col("TOTAL", 547f, Align.Right, bold = true)
            ),
            rows = input.days.map { day ->
                listOf(
                    dayFormat.format(Date(day.dayStart)),
                    day.bills.toString(),
                    day.items.toString(),
                    formatRupees(day.subtotal),
                    formatRupees(day.gst),
                    formatRupees(day.total)
                )
            },
            total = listOf(
                "Total",
                report.billCount.toString(),
                report.itemCount.toString(),
                formatRupees(report.subtotal),
                formatRupees(report.gst),
                formatRupees(report.total)
            )
        )
    }

    val categoryTotal = report.categories.sumOf { it.amount }

    fun shareOf(amount: Double): Double = if (categoryTotal > 0) amount / categoryTotal else 0.0

    table(
        title = "Sales by category",
        note = "Amounts before GST",
        cols = listOf(
            Col("CATEGORY", 50f, maxWidth = 190f),
            Col("ITEMS", 285f, Align.Right),
            Col("SHARE", BAR_X),
            Col("", 440f, Align.Right),
            Col("AMOUNT", 547f, Align.Right, bold = true)
        ),
        rows = report.categories.map { category ->
            listOf(
                category.name,
                category.quantity.toString(),
                "",
                percentText(shareOf(category.amount)),
                formatRupees(category.amount)
            )
        },
        decorate = { index, top ->
            val share = shareOf(report.categories[index].amount)

            ops += PdfOp.Box(BAR_X, top + 10f, BAR_WIDTH, 5f, gray = 232)
            if (share > 0) {
                ops += PdfOp.Box(BAR_X, top + 10f, maxOf(BAR_WIDTH * share.toFloat(), 1.5f), 5f, gray = 70)
            }
        }
    )

    table(
        title = "Products sold",
        note = "Amounts before GST",
        cols = listOf(
            Col("#", 50f, gray = 130),
            Col("PRODUCT", 80f, maxWidth = 290f),
            Col("QTY", 420f, Align.Right),
            Col("AMOUNT", 547f, Align.Right, bold = true)
        ),
        rows = report.products.mapIndexed { index, product ->
            listOf(
                (index + 1).toString(),
                product.name,
                product.quantity.toString(),
                formatRupees(product.amount)
            )
        }
    )

    // ------------------------------------------------------------ footer, now the page count is known

    pages.forEachIndexed { index, pageOps ->
        pageOps += PdfOp.Line(LEFT, FOOTER_LINE_Y, RIGHT, FOOTER_LINE_Y, 0.5f, 215)
        pageOps += PdfOp.Text(
            fit("${input.periodLabel} · ${input.rangeText}", 380f, 9f, Face.Sans, measure),
            LEFT, 808f, 9f, gray = 110
        )
        pageOps += PdfOp.Text("Total sales include GST. This is a computer-generated report.", LEFT, 820f, 8f, gray = 150)
        pageOps += PdfOp.Text("Page ${index + 1} of ${pages.size}", RIGHT, 808f, 9f, align = Align.Right, gray = 110)
    }

    return pages.map { PdfPage(it) }
}

/** "+13% vs yesterday so far". Plain signs, so no font has to have the arrows. */
private fun changeText(report: SalesReport, previousLabel: String): String {
    val percent = percentChange(report.total, report.previousTotal)

    return when {
        percent == null -> "No sales $previousLabel to compare"
        percent > 0 -> "+$percent% vs $previousLabel so far"
        percent < 0 -> "-${-percent}% vs $previousLabel so far"
        else -> "Same as $previousLabel so far"
    }
}

/** 0.42 -> "42%"; anything above zero but under half a percent -> "<1%". */
private fun percentText(share: Double): String {
    val percent = (share * 100).roundToInt()
    return if (share > 0 && percent == 0) "<1%" else "$percent%"
}

/** [size], or the largest smaller size (down to 9) at which [text] fits in [maxWidth]. */
private fun fitSize(text: String, maxWidth: Float, size: Float, face: Face, measure: Measure): Float {
    var current = size
    while (current > 9f && measure(text, current, face) > maxWidth) current -= 0.5f
    return current
}
