package com.example.billingapp

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface InvoiceItemDao {

    @Insert
    suspend fun insert(invoiceItem: InvoiceItem): Long

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
}