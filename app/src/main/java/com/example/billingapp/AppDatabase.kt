package com.example.billingapp

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        Customer::class,
        Product::class,
        Invoice::class,
        InvoiceItem::class
    ],
    version = 8
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun customerDao(): CustomerDao

    abstract fun productDao(): ProductDao

    abstract fun invoiceDao(): InvoiceDao

    abstract fun invoiceItemDao(): InvoiceItemDao
}

/**
 * v7 adds an optional email to customers. Existing rows get an empty email, nothing is lost.
 *
 * Every future schema change needs a migration like this one; the database is built without
 * a "wipe and start again" fallback, so a missing migration fails loudly instead of silently
 * deleting the shop's invoices.
 */
val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE customers ADD COLUMN email TEXT NOT NULL DEFAULT ''")
    }
}

/**
 * v8: each invoice records which customer it belongs to. Existing invoices start at 0 ("not
 * linked yet"); they are linked once, at the next start, by [linkInvoicesToCustomers].
 */
val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE invoices ADD COLUMN customerId INTEGER NOT NULL DEFAULT 0")
    }
}
