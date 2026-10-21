package com.example.billingapp.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

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
