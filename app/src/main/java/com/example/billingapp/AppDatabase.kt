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
        InvoiceItem::class,
        StockEntry::class
    ],
    version = 10
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun customerDao(): CustomerDao

    abstract fun productDao(): ProductDao

    abstract fun invoiceDao(): InvoiceDao

    abstract fun invoiceItemDao(): InvoiceItemDao

    abstract fun stockDao(): StockDao
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

/**
 * v9: an earlier version of stock kept a count on each product. v10 replaces it (see below), so
 * this step only exists for phones that are still on v8.
 */
val MIGRATION_8_9 = object : Migration(8, 9) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE products ADD COLUMN stock INTEGER")
    }
}

/**
 * v10: stock becomes a list of what came in, per day ([StockEntry]); what was sold is read from
 * the invoices. Any count already typed on a product becomes an entry dated now, and the
 * products table goes back to its four columns (SQLite can't drop one in place, so it is
 * rebuilt with every product copied over, ids included).
 */
val MIGRATION_9_10 = object : Migration(9, 10) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS stock_entries (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "productName TEXT NOT NULL, " +
                    "quantity INTEGER NOT NULL, " +
                    "addedAt INTEGER NOT NULL)"
        )

        db.execSQL(
            "INSERT INTO stock_entries (productName, quantity, addedAt) " +
                    "SELECT name, stock, CAST(strftime('%s', 'now') AS INTEGER) * 1000 " +
                    "FROM products WHERE stock IS NOT NULL AND stock > 0"
        )

        db.execSQL(
            "CREATE TABLE products_new (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "name TEXT NOT NULL, " +
                    "price REAL NOT NULL, " +
                    "category TEXT NOT NULL)"
        )
        db.execSQL("INSERT INTO products_new (id, name, price, category) SELECT id, name, price, category FROM products")
        db.execSQL("DROP TABLE products")
        db.execSQL("ALTER TABLE products_new RENAME TO products")
    }
}
