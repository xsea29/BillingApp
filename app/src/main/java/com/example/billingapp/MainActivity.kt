package com.example.billingapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import androidx.room.Room
import com.example.billingapp.ui.theme.BillingAppTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    lateinit var db: AppDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // The theme's window background is the start-up screen (the word "Monginis");
        // from here on it should just be white, never showing behind the app.
        window.setBackgroundDrawableResource(android.R.color.white)

        // No "wipe and start again" fallback: a schema change without a migration
        // should fail loudly, not silently delete the shop's invoices.
        db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "cake_db"
        )
            .addMigrations(MIGRATION_6_7)
            .build()

        lifecycleScope.launch {

            if (db.productDao().getAllProducts().isEmpty()) {

                db.productDao().insertAll(
                    ProductData.products
                )
            }

            // Bills made before customers were saved automatically: add their customers, once.
            val prefs = getSharedPreferences("monginis", MODE_PRIVATE)

            if (!prefs.getBoolean("customers_from_bills_done", false)) {
                db.addCustomersFromInvoices()
                prefs.edit().putBoolean("customers_from_bills_done", true).apply()
            }
        }

        setContent {
            BillingAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    BillingApp(db)
                }
            }
        }
    }
}
