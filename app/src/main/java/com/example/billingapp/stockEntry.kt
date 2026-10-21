package com.example.billingapp

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Stock that came in: [quantity] pieces of the product called [productName] on [addedAt].
 * Only what comes in is stored; what was sold is read from the invoices, so editing or deleting
 * an invoice can never leave the stock out of step. Matched to invoice lines by product name,
 * which is what an invoice line keeps.
 */
@Entity(tableName = "stock_entries")
data class StockEntry(

    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val productName: String,

    val quantity: Int,

    val addedAt: Long = System.currentTimeMillis()
)

/** One product line of one invoice, with the time of that invoice. What the stock sums up as sold. */
data class SoldLine(
    val productName: String,
    val quantity: Int,
    val soldAt: Long
)
