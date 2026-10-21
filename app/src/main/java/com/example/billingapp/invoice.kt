package com.example.billingapp

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "invoices")
data class Invoice(

    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val customerName: String,

    val phoneNumber: String,

    val subtotal: Double,

    val gst: Double,

    val total: Double,

    val createdAt: Long = System.currentTimeMillis()
)