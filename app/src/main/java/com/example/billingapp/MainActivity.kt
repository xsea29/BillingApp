package com.example.billingapp

import android.os.Bundle
import android.os.SystemClock
import android.view.View
import android.view.ViewTreeObserver
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

/** How long the start-up screen (the word "Monginis") stays up. */
private const val STARTUP_SCREEN_MILLIS = 2000L

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
            .addMigrations(MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10)
            .build()

        lifecycleScope.launch {

            if (db.productDao().getAllProducts().isEmpty()) {

                db.productDao().insertAll(
                    ProductData.products
                )
            }

            // Bills made before invoices knew their customer: link them to customers, once.
            val prefs = getSharedPreferences("monginis", MODE_PRIVATE)

            if (!prefs.getBoolean("invoices_linked_to_customers", false)) {
                db.linkInvoicesToCustomers()
                prefs.edit().putBoolean("invoices_linked_to_customers", true).apply()
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

        // Only when the app is starting, not when the phone is turned and the screen rebuilt.
        if (savedInstanceState == null) holdStartupScreen()
    }

    /**
     * Keeps the start-up screen up for [STARTUP_SCREEN_MILLIS] by not letting the app draw its
     * first frame until then. The word "Monginis" stays the system's own start-up screen the
     * whole time, so it can't jump: a second copy drawn by the app sits between the status and
     * navigation bars, while the system's is centred on the whole screen, and the word dropped
     * a few dp when one replaced the other. The app still sets itself up behind the screen, so
     * the home page is ready the moment it goes.
     */
    private fun holdStartupScreen() {
        val content = findViewById<View>(android.R.id.content)
        val start = SystemClock.uptimeMillis()

        content.viewTreeObserver.addOnPreDrawListener(object : ViewTreeObserver.OnPreDrawListener {
            override fun onPreDraw(): Boolean {
                if (SystemClock.uptimeMillis() - start < STARTUP_SCREEN_MILLIS) return false

                content.viewTreeObserver.removeOnPreDrawListener(this)
                return true
            }
        })
    }
}
