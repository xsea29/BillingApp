package com.example.billingapp

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface InvoiceDao {

    @Insert
    suspend fun insert(invoice: Invoice): Long

    @Query("SELECT * FROM invoices ORDER BY id DESC")
    suspend fun getAllInvoices(): List<Invoice>

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