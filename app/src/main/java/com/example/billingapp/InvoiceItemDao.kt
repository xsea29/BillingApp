package com.example.billingapp

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface InvoiceItemDao {

    @Insert
    suspend fun insert(invoiceItem: InvoiceItem): Long

    @Query("DELETE FROM invoice_items WHERE invoiceId = :invoiceId")
    suspend fun deleteByInvoiceId(invoiceId: Int)

    @Query(
        "SELECT * FROM invoice_items WHERE invoiceId = :invoiceId"
    )
    suspend fun getItemsByInvoiceId(
        invoiceId: Int
    ): List<InvoiceItem>

    @Query(
        "SELECT * FROM invoice_items WHERE invoiceId IN (:invoiceIds)"
    )
    suspend fun getItemsByInvoiceIds(
        invoiceIds: List<Int>
    ): List<InvoiceItem>

    // Every product line on invoices dated [since] or later, each with its invoice's date.
    @Query(
        "SELECT invoice_items.productName AS productName, " +
                "invoice_items.quantity AS quantity, " +
                "invoices.createdAt AS soldAt " +
                "FROM invoice_items " +
                "INNER JOIN invoices ON invoices.id = invoice_items.invoiceId " +
                "WHERE invoices.createdAt >= :since"
    )
    suspend fun getSoldLines(since: Long): List<SoldLine>
}