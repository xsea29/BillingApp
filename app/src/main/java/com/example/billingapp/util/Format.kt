package com.example.billingapp.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.round

/**
 * 1234567.5 -> "₹12,34,567.50" (Indian lakh/crore grouping).
 * Done by hand because java.text.NumberFormat can't do the 2-digit grouping.
 */
fun formatRupees(amount: Double): String {
    val (whole, fraction) = String.format(Locale.US, "%.2f", amount).split(".")

    val grouped = if (whole.length <= 3) {
        whole
    } else {
        val lastThree = whole.takeLast(3)
        val rest = whole.dropLast(3).reversed().chunked(2).joinToString(",").reversed()
        "$rest,$lastThree"
    }

    return "₹$grouped.$fraction"
}

/** Rounds to 2 decimals, so sums of prices never show float noise like 9.950000000000001. */
fun roundToPaise(value: Double): Double = round(value * 100) / 100

/** 12500 -> "₹12.5K", 200000 -> "₹2L", 30000000 -> "₹3Cr". For chart axes. */
fun formatRupeesCompact(amount: Double): String {
    fun trim(value: Double): String =
        if (value == Math.floor(value)) value.toLong().toString()
        else String.format(Locale.US, "%.1f", value)

    return "₹" + when {
        amount >= 1_00_00_000 -> trim(amount / 1_00_00_000) + "Cr"
        amount >= 1_00_000 -> trim(amount / 1_00_000) + "L"
        amount >= 1_000 -> trim(amount / 1_000) + "K"
        else -> trim(amount)
    }
}

/** "3:45 PM" */
fun formatTime(timeMillis: Long): String =
    SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(timeMillis))

/** "21 Oct 2026, 3:45 PM" */
fun formatDateTime(timeMillis: Long): String =
    SimpleDateFormat("d MMM yyyy, h:mm a", Locale.getDefault()).format(Date(timeMillis))

/** Midnight at the start of the day that contains [timeMillis]. */
fun startOfDay(timeMillis: Long): Long =
    Calendar.getInstance().apply {
        this.timeInMillis = timeMillis
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

/** "Today", "Yesterday", or "20 Oct 2026". */
fun dayLabel(timeMillis: Long): String {
    val today = startOfDay(System.currentTimeMillis())
    val yesterday = Calendar.getInstance().apply {
        this.timeInMillis = today
        add(Calendar.DAY_OF_YEAR, -1)
    }.timeInMillis

    return when (startOfDay(timeMillis)) {
        today -> "Today"
        yesterday -> "Yesterday"
        else -> SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(Date(timeMillis))
    }
}

/**
 * The date picker works in "midnight UTC of the chosen calendar day". This turns a normal
 * timestamp into that, for the same calendar day as seen on the phone.
 */
fun pickerMillisOf(localMillis: Long): Long {
    val local = Calendar.getInstance().apply { timeInMillis = localMillis }

    return Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
        clear()
        set(local.get(Calendar.YEAR), local.get(Calendar.MONTH), local.get(Calendar.DAY_OF_MONTH))
    }.timeInMillis
}

/**
 * Moves [localMillis] to the calendar day the date picker returned, keeping the time of day.
 * (A bill dated yesterday still carries the time it was entered.)
 */
fun moveToPickedDay(localMillis: Long, pickerMillis: Long): Long {
    val picked = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = pickerMillis }

    return Calendar.getInstance().apply {
        timeInMillis = localMillis
        set(picked.get(Calendar.YEAR), picked.get(Calendar.MONTH), picked.get(Calendar.DAY_OF_MONTH))
    }.timeInMillis
}
