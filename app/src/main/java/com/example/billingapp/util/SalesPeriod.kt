package com.example.billingapp.util

import java.util.Calendar

enum class SalesPeriod(val label: String) {
    Today("Today"),
    Week("This week"),
    Month("This month");

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
}
