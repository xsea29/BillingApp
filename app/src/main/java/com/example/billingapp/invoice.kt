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

    val createdAt: Long = System.currentTimeMillis(),

    /**
     * Which saved customer this bill belongs to (their [Customer.id]), or 0 when it isn't linked
     * to one (a walk-in, or no full phone number). The name and phone above are what was written
     * on the bill; this is what says who it is for.
     */
    val customerId: Int = 0
)