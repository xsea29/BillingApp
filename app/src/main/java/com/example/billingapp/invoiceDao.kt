package com.example.billingapp

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface InvoiceDao {

    @Insert
    suspend fun insert(invoice: Invoice): Long

    // Returns how many invoices were changed: 1, or 0 if there is no invoice with that number.
    @Update
    suspend fun update(invoice: Invoice): Int

    @Query("UPDATE invoices SET customerId = :customerId WHERE id = :invoiceId")
    suspend fun setCustomerId(invoiceId: Int, customerId: Int)

    @Query("DELETE FROM invoices WHERE id = :invoiceId")
    suspend fun deleteById(invoiceId: Int)

    @Query("SELECT * FROM invoices ORDER BY id DESC")
    suspend fun getAllInvoices(): List<Invoice>

    // Null when there are no invoices yet.
    @Query("SELECT MAX(id) FROM invoices")
    suspend fun getLastInvoiceId(): Int?

    @Query("SELECT * FROM invoices WHERE id = :invoiceId")
    suspend fun getInvoiceById(
        invoiceId: Int
    ): Invoice

    // Get invoices between two timestamps
    @Query(
        "SELECT * FROM invoices " +
                "WHERE createdAt >= :startTime " +
                "AND createdAt < :endTime " +
                "ORDER BY createdAt ASC"
    )
    suspend fun getInvoicesBetween(
        startTime: Long,
        endTime: Long
    ): List<Invoice>
}