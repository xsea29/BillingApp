package com.example.billingapp.util

import java.util.Calendar

enum class SalesPeriod(val label: String, val previousLabel: String) {
    Today("Today", "yesterday"),
    Week("This week", "last week"),
    Month("This month", "last month");

    /** Start (inclusive) and end (exclusive) of this period, in epoch millis. */
    fun range(now: Long = System.currentTimeMillis()): Pair<Long, Long> {
        val start = Calendar.getInstance().apply {
            timeInMillis = startOfDay(now)
            when (this@SalesPeriod) {
                Today -> Unit
                Week -> set(Calendar.DAY_OF_WEEK, firstDayOfWeek)
                Month -> set(Calendar.DAY_OF_MONTH, 1)
            }
        }

        val end = Calendar.getInstance().apply {
            timeInMillis = start.timeInMillis
            when (this@SalesPeriod) {
                Today -> add(Calendar.DAY_OF_MONTH, 1)
                Week -> add(Calendar.WEEK_OF_YEAR, 1)
                Month -> add(Calendar.MONTH, 1)
            }
        }

        return start.timeInMillis to end.timeInMillis
    }

    /**
     * The same stretch of the previous period: today up to this time vs yesterday up to
     * this time, this week so far vs last week so far, and so on. A fair comparison,
     * unlike today-so-far against all of yesterday.
     */
    fun previousRange(now: Long = System.currentTimeMillis()): Pair<Long, Long> {
        val start = range(now).first

        val previousStart = Calendar.getInstance().apply {
            timeInMillis = start
            when (this@SalesPeriod) {
                Today -> add(Calendar.DAY_OF_MONTH, -1)
                Week -> add(Calendar.WEEK_OF_YEAR, -1)
                Month -> add(Calendar.MONTH, -1)
            }
        }.timeInMillis

        val elapsed = (now - start).coerceAtLeast(0)

        return previousStart to minOf(previousStart + elapsed, start)
    }
}
