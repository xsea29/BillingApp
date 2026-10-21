package com.example.billingapp

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        Customer::class,
        Product::class,
        Invoice::class,
        InvoiceItem::class
    ],
    version = 6
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun customerDao(): CustomerDao

    abstract fun productDao(): ProductDao

    abstract fun invoiceDao(): InvoiceDao

    abstract fun invoiceItemDao(): InvoiceItemDao
}