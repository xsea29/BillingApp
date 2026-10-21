package com.example.billingapp.ui.stock

import com.example.billingapp.AppDatabase
import com.example.billingapp.util.startOfDay

/** Reads what the stock sums need from the database (see [StockData]). */
suspend fun AppDatabase.loadStockData(): StockData {
    val entries = stockDao().getAll()

    // Bills from before the first stock was entered never count, so they aren't even loaded.
    val sold = if (entries.isEmpty()) {
        emptyList()
    } else {
        invoiceItemDao().getSoldLines(startOfDay(entries.minOf { it.addedAt }))
    }

    return StockData(entries, sold)
}
