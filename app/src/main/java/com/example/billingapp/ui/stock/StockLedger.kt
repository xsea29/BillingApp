package com.example.billingapp.ui.stock

import com.example.billingapp.SoldLine
import com.example.billingapp.StockEntry
import com.example.billingapp.util.startOfDay
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/*
 * What the Stocks screen shows, worked out from two plain lists: the stock that came in
 * ([StockEntry]) and the product lines on invoices ([SoldLine]). Nothing is stored per day:
 * "today", "yesterday", a month, a year or one picked date are all the same sum over a
 * different stretch of time, and yesterday's leftover is today's opening because it is the
 * same running total.
 */

/** A ready-made stretch of time. A single picked date goes through [dayRange] instead. */
enum class StockPeriod(val label: String) {
    Today("Today"),
    Yesterday("Yesterday"),
    Month("This month"),
    Year("This year");

    /** Start (inclusive) and end (exclusive) of this period, in epoch millis. */
    fun range(now: Long = System.currentTimeMillis()): Pair<Long, Long> =
        when (this) {
            Today -> dayRange(now)

            Yesterday -> dayRange(startOfDay(now) - 1)

            Month -> {
                val start = Calendar.getInstance().apply {
                    timeInMillis = startOfDay(now)
                    set(Calendar.DAY_OF_MONTH, 1)
                }
                val end = Calendar.getInstance().apply {
                    timeInMillis = start.timeInMillis
                    add(Calendar.MONTH, 1)
                }
                start.timeInMillis to end.timeInMillis
            }

            Year -> {
                val start = Calendar.getInstance().apply {
                    timeInMillis = startOfDay(now)
                    set(Calendar.MONTH, Calendar.JANUARY)
                    set(Calendar.DAY_OF_MONTH, 1)
                }
                val end = Calendar.getInstance().apply {
                    timeInMillis = start.timeInMillis
                    add(Calendar.YEAR, 1)
                }
                start.timeInMillis to end.timeInMillis
            }
        }
}

/** The whole calendar day that contains [anyTimeInDay]: midnight (inclusive) to next midnight (exclusive). */
fun dayRange(anyTimeInDay: Long): Pair<Long, Long> {
    val start = startOfDay(anyTimeInDay)

    val end = Calendar.getInstance().apply {
        timeInMillis = start
        add(Calendar.DAY_OF_MONTH, 1)
    }.timeInMillis

    return start to end
}

/** One product over a stretch of time: what it started with, what came in, what was sold. */
data class StockRow(
    val name: String,
    /** What was left when the stretch began: yesterday's remaining, for a day. */
    val opening: Int,
    val added: Int,
    val sold: Int
) {
    /** Can be below zero when more was billed than was entered. */
    val remaining: Int get() = opening + added - sold
}

/**
 * One [StockRow] per product that has stock in [start] (inclusive) to [end] (exclusive), A to Z.
 *
 * A product is counted from the day its first stock was entered: bills from before that day
 * don't take from it, because nobody had told the app how many there were. After that day,
 * everything billed under its name is taken from the total. A product with nothing at all in
 * the stretch (nothing left over, nothing in, nothing out) is left out.
 */
fun stockRows(
    entries: List<StockEntry>,
    sold: List<SoldLine>,
    start: Long,
    end: Long
): List<StockRow> {

    val soldByName = sold.groupBy { it.productName }

    return entries
        .groupBy { it.productName }
        .mapNotNull { (name, own) ->

            val countedFrom = startOfDay(own.minOf { it.addedAt })
            if (end <= countedFrom) return@mapNotNull null

            val sales = soldByName[name].orEmpty()

            val opening = own.filter { it.addedAt < start }.sumOf { it.quantity } -
                    sales.filter { it.soldAt >= countedFrom && it.soldAt < start }.sumOf { it.quantity }

            val added = own.filter { it.addedAt >= start && it.addedAt < end }.sumOf { it.quantity }

            val soldIn = sales
                .filter { it.soldAt >= maxOf(start, countedFrom) && it.soldAt < end }
                .sumOf { it.quantity }

            StockRow(name, opening, added, soldIn)
                .takeIf { it.opening != 0 || it.added != 0 || it.sold != 0 }
        }
        .sortedBy { it.name.lowercase() }
}

/** The heading over the list: "Today · Wed 21 Oct", "October 2026", "2026", or a picked day. */
fun stockRangeTitle(period: StockPeriod?, pickedDay: Long?, now: Long = System.currentTimeMillis()): String {
    fun format(pattern: String, time: Long): String =
        SimpleDateFormat(pattern, Locale.getDefault()).format(Date(time))

    return when {
        pickedDay != null -> format("EEE d MMM yyyy", pickedDay)
        period == StockPeriod.Today -> "Today · ${format("EEE d MMM", now)}"
        period == StockPeriod.Yesterday -> "Yesterday · ${format("EEE d MMM", startOfDay(now) - 1)}"
        period == StockPeriod.Month -> format("MMMM yyyy", now)
        period == StockPeriod.Year -> format("yyyy", now)
        else -> ""
    }
}
