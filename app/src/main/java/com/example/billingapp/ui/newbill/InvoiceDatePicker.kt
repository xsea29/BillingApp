package com.example.billingapp.ui.newbill

import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.example.billingapp.util.pickerMillisOf
import java.util.Calendar

/**
 * Calendar for choosing the date of a bill. Any day up to today can be picked, so a bill
 * that was forgotten can be entered later under the right date. [onPick] receives the day in
 * the date picker's own form; turn it into a bill date with `moveToPickedDay`.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvoiceDatePickerDialog(
    current: Long,
    onDismiss: () -> Unit,
    onPick: (pickerMillis: Long) -> Unit
) {
    val lastPickable = remember { pickerMillisOf(System.currentTimeMillis()) }
    val thisYear = remember { Calendar.getInstance().get(Calendar.YEAR) }
    val currentYear = remember(current) {
        Calendar.getInstance().apply { timeInMillis = current }.get(Calendar.YEAR)
    }

    val state = rememberDatePickerState(
        initialSelectedDateMillis = pickerMillisOf(current),
        // The calendar crashes if the date being edited falls outside the years it shows.
        yearRange = minOf(thisYear - 5, currentYear)..maxOf(thisYear, currentYear),
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean = utcTimeMillis <= lastPickable
            override fun isSelectableYear(year: Int): Boolean = year <= thisYear
        }
    )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    state.selectedDateMillis?.let(onPick)
                    onDismiss()
                },
                enabled = state.selectedDateMillis != null
            ) {
                Text("Set date")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    ) {
        // Calendar only: no keyboard-entry toggle, so it stays clean.
        DatePicker(
            state = state,
            showModeToggle = false
        )
    }
}
