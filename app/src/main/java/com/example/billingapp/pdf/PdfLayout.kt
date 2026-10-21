package com.example.billingapp.pdf

import com.example.billingapp.Invoice
import com.example.billingapp.InvoiceItem
import com.example.billingapp.gstLabel
import com.example.billingapp.util.amountInWords
import com.example.billingapp.util.formatRupees
import com.example.billingapp.util.formatTime
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/*
 * The invoice PDF, as a plain list of "draw this" instructions per page.
 *
 * Nothing here touches Android: the layout is worked out as data, so it can be tested and
 * previewed on any computer, and the Android side (PdfGenerator) only has to carry out the
 * instructions on a canvas. Black, white and greys only.
 */

const val PAGE_WIDTH = 595f
const val PAGE_HEIGHT = 842f

enum class Face { Serif, Sans, SansBold }

enum class Align { Left, Center, Right }

sealed interface PdfOp {

    /** [y] is the text's baseline. [gray] is 0 (black) to 255 (white). [spacing] is extra letter spacing, in em. */
    data class Text(
        val text: String,
        val x: Float,
        val y: Float,
        val size: Float,
        val face: Face = Face.Sans,
        val align: Align = Align.Left,
        val gray: Int = 17,
        val spacing: Float = 0f
    ) : PdfOp

    data class Line(
        val x1: Float,
        val y1: Float,
        val x2: Float,
        val y2: Float,
        val width: Float = 0.6f,
        val gray: Int = 200
    ) : PdfOp

    data class Box(
        val x: Float,
        val y: Float,
        val width: Float,
        val height: Float,
        val gray: Int
    ) : PdfOp
}

data class PdfPage(val ops: List<PdfOp>)

/** Width of [text] when drawn at [size] in [face]. The Android side measures with the real fonts. */
typealias Measure = (text: String, size: Float, face: Face) -> Float

private const val LEFT = 40f
private const val RIGHT = 555f

private const val ROW_HEIGHT = 26f
private const val TABLE_HEADER_HEIGHT = 24f

// Nothing but the footer is drawn below this line.
private const val CONTENT_BOTTOM = 760f
private const val FOOTER_LINE_Y = 792f

// The totals block (words on the left, numbers on the right) and the room it needs.
private const val TOTALS_LEFT = 340f
private const val TOTALS_HEIGHT = 120f

// Table columns.
private const val NUMBER_X = 50f
private const val NAME_X = 80f
private const val NAME_RIGHT = 318f
private const val QTY_RIGHT = 372f
private const val RATE_RIGHT = 462f
private const val AMOUNT_RIGHT = 547f

/** Lays the invoice out on as many A4 pages as the items need. */
fun layoutInvoice(
    invoice: Invoice,
    items: List<InvoiceItem>,
    measure: Measure
): List<PdfPage> {

    val pages = mutableListOf<MutableList<PdfOp>>()
    var ops = mutableListOf<PdfOp>()
    pages += ops

    /** Column headings on a grey band; returns where the first row starts. */
    fun tableHeader(top: Float): Float {
        ops += PdfOp.Box(LEFT, top, RIGHT - LEFT, TABLE_HEADER_HEIGHT, gray = 243)

        val baseline = top + 16f
        ops += PdfOp.Text("#", NUMBER_X, baseline, 8.5f, Face.SansBold, gray = 90, spacing = 0.1f)
        ops += PdfOp.Text("ITEM", NAME_X, baseline, 8.5f, Face.SansBold, gray = 90, spacing = 0.1f)
        ops += PdfOp.Text("QTY", QTY_RIGHT, baseline, 8.5f, Face.SansBold, Align.Right, gray = 90, spacing = 0.1f)
        ops += PdfOp.Text("RATE", RATE_RIGHT, baseline, 8.5f, Face.SansBold, Align.Right, gray = 90, spacing = 0.1f)
        ops += PdfOp.Text("AMOUNT", AMOUNT_RIGHT, baseline, 8.5f, Face.SansBold, Align.Right, gray = 90, spacing = 0.1f)

        return top + TABLE_HEADER_HEIGHT
    }

    /** A small running header for pages after the first; returns where content may start. */
    fun continuationPage(withTable: Boolean): Float {
        ops = mutableListOf()
        pages += ops

        ops += PdfOp.Text("Monginis", LEFT, 46f, 16f, Face.Serif)
        ops += PdfOp.Text("Invoice #${invoice.id} (continued)", RIGHT, 45f, 9.5f, Face.Sans, Align.Right, gray = 110)
        ops += PdfOp.Line(LEFT, 58f, RIGHT, 58f, 0.8f, 60)

        return if (withTable) tableHeader(76f) else 76f
    }

    // ------------------------------------------------------------ first page: title and parties

    ops += PdfOp.Text("Monginis", LEFT, 62f, 30f, Face.Serif)
    ops += PdfOp.Text("Invoice", RIGHT, 62f, 30f, Face.Serif, Align.Right)
    ops += PdfOp.Line(LEFT, 80f, RIGHT, 80f, 0.9f, 50)

    val customer = invoice.customerName.ifBlank { "Walk-in customer" }

    ops += PdfOp.Text("BILLED TO", LEFT, 108f, 8.5f, Face.SansBold, gray = 120, spacing = 0.12f)
    ops += PdfOp.Text(fit(customer, 280f, 13f, Face.SansBold, measure), LEFT, 128f, 13f, Face.SansBold)
    if (invoice.phoneNumber.isNotBlank()) {
        ops += PdfOp.Text("Phone  ${invoice.phoneNumber}", LEFT, 145f, 10.5f, gray = 90)
    }

    val metaLabelX = 372f
    val date = SimpleDateFormat("d MMMM yyyy", Locale.getDefault()).format(Date(invoice.createdAt))

    fun meta(baseline: Float, label: String, value: String, bold: Boolean = false) {
        ops += PdfOp.Text(label, metaLabelX, baseline, 8.5f, Face.SansBold, gray = 120, spacing = 0.12f)
        ops += PdfOp.Text(value, RIGHT, baseline, 10.5f, if (bold) Face.SansBold else Face.Sans, Align.Right)
    }

    meta(108f, "INVOICE NO.", "#${invoice.id}", bold = true)
    meta(126f, "DATE", date)
    meta(144f, "TIME", formatTime(invoice.createdAt))

    var y = tableHeader(172f)

    // ------------------------------------------------------------ item rows

    items.forEachIndexed { index, item ->

        if (y + ROW_HEIGHT > CONTENT_BOTTOM) y = continuationPage(withTable = true)

        val baseline = y + 17f

        ops += PdfOp.Text("${index + 1}", NUMBER_X, baseline, 10f, gray = 130)
        ops += PdfOp.Text(fit(item.productName, NAME_RIGHT - NAME_X, 10.5f, Face.Sans, measure), NAME_X, baseline, 10.5f)
        ops += PdfOp.Text("${item.quantity}", QTY_RIGHT, baseline, 10.5f, align = Align.Right)
        ops += PdfOp.Text(formatRupees(item.price), RATE_RIGHT, baseline, 10.5f, align = Align.Right)
        ops += PdfOp.Text(formatRupees(item.amount), AMOUNT_RIGHT, baseline, 10.5f, Face.SansBold, Align.Right)
        ops += PdfOp.Line(LEFT, y + ROW_HEIGHT, RIGHT, y + ROW_HEIGHT, 0.5f, 226)

        y += ROW_HEIGHT
    }

    // ------------------------------------------------------------ totals

    // If the totals wouldn't fit under the last row, they get a page of their own.
    if (y + 22f + TOTALS_HEIGHT > CONTENT_BOTTOM) y = continuationPage(withTable = false)

    val top = y + 22f

    ops += PdfOp.Text("AMOUNT IN WORDS", LEFT, top + 12f, 8.5f, Face.SansBold, gray = 120, spacing = 0.12f)
    wrap(amountInWords(invoice.total), 250f, 10.5f, Face.Sans, measure).forEachIndexed { line, text ->
        ops += PdfOp.Text(text, LEFT, top + 30f + line * 15f, 10.5f)
    }

    ops += PdfOp.Text("Subtotal", TOTALS_LEFT, top + 12f, 10.5f, gray = 90)
    ops += PdfOp.Text(formatRupees(invoice.subtotal), RIGHT, top + 12f, 10.5f, align = Align.Right)

    ops += PdfOp.Text(gstLabel(), TOTALS_LEFT, top + 36f, 10.5f, gray = 90)
    ops += PdfOp.Text(formatRupees(invoice.gst), RIGHT, top + 36f, 10.5f, align = Align.Right)

    ops += PdfOp.Line(TOTALS_LEFT, top + 48f, RIGHT, top + 48f, 0.8f, 60)

    ops += PdfOp.Box(TOTALS_LEFT, top + 58f, RIGHT - TOTALS_LEFT, 38f, gray = 243)
    ops += PdfOp.Text("Total", TOTALS_LEFT + 12f, top + 82f, 12f, Face.SansBold)
    ops += PdfOp.Text(formatRupees(invoice.total), RIGHT - 12f, top + 83f, 15f, Face.SansBold, Align.Right)

    // ------------------------------------------------------------ footer, now the page count is known

    pages.forEachIndexed { index, pageOps ->
        pageOps += PdfOp.Line(LEFT, FOOTER_LINE_Y, RIGHT, FOOTER_LINE_Y, 0.5f, 215)
        pageOps += PdfOp.Text("Thank you for your business.", LEFT, 808f, 9f, gray = 110)
        pageOps += PdfOp.Text("This is a computer-generated invoice.", LEFT, 820f, 8f, gray = 150)
        pageOps += PdfOp.Text("Page ${index + 1} of ${pages.size}", RIGHT, 808f, 9f, align = Align.Right, gray = 110)
    }

    return pages.map { PdfPage(it) }
}

/**
 * [text], or as much of it as fits in [maxWidth] followed by "…". Cut at a space when that
 * doesn't lose most of the text, so it reads "Special Edition…" and not "Special Edit…".
 */
private fun fit(text: String, maxWidth: Float, size: Float, face: Face, measure: Measure): String {
    if (measure(text, size, face) <= maxWidth) return text

    var cut = text
    while (cut.isNotEmpty() && measure("$cut…", size, face) > maxWidth) cut = cut.dropLast(1)

    val lastSpace = cut.lastIndexOf(' ')
    if (lastSpace > cut.length / 2) cut = cut.substring(0, lastSpace)

    return cut.trimEnd() + "…"
}

/** Breaks [text] into lines no wider than [maxWidth], at spaces. At most [maxLines] lines. */
private fun wrap(
    text: String,
    maxWidth: Float,
    size: Float,
    face: Face,
    measure: Measure,
    maxLines: Int = 3
): List<String> {
    val lines = mutableListOf<String>()
    var current = ""

    for (word in text.split(" ")) {
        val candidate = if (current.isEmpty()) word else "$current $word"

        if (current.isEmpty() || measure(candidate, size, face) <= maxWidth) {
            current = candidate
        } else {
            lines += current
            current = word
        }
    }

    if (current.isNotEmpty()) lines += current

    return lines.take(maxLines)
}
