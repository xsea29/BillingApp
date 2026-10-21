package com.example.billingapp

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "products")
data class Product(

    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val name: String,

    val price: Double,

    val category: String,

    /**
     * How many are in stock, or null when this product isn't counted. Billing takes from it
     * (and deleting or editing a bill gives back), so it can go below zero if more was billed
     * than was entered; the screens show that as 0.
     */
    val stock: Int? = null
)