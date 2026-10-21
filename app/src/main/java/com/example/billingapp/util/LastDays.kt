package com.example.billingapp.util

import java.util.Calendar

/** The "last N days" choices offered by the filter on the invoice list. */
val LAST_DAYS_CHOICES = listOf(7, 10, 30, 90)

fun lastDaysLabel(days: Int): String = "Last $days days"

/**
 * Start (inclusive) and end (exclusive), in epoch millis, of the last [days] days counting
 * today: "last 7 days" is today and the six days before it, so a bill made a minute ago is
 * always in it. Whole calendar days, so the first day is included from its first minute.
 */
fun lastDaysRange(days: Int, now: Long = System.currentTimeMillis()): Pair<Long, Long> {
    val start = Calendar.getInstance().apply {
        timeInMillis = startOfDay(now)
        add(Calendar.DAY_OF_MONTH, -(days - 1))
    }

    val end = Calendar.getInstance().apply {
        timeInMillis = startOfDay(now)
        add(Calendar.DAY_OF_MONTH, 1)
    }

    return start.timeInMillis to end.timeInMillis
}
