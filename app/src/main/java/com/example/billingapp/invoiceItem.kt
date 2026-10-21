package com.example.billingapp

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "invoice_items")
data class InvoiceItem(

    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val invoiceId: Int,

    val productName: String,

    val quantity: Int,

    val price: Double,

    val amount: Double
)