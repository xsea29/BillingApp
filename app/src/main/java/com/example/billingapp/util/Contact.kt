package com.example.billingapp.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast

/** Opens the dialer with [phone] filled in (the user still presses call). */
fun dialPhone(context: Context, phone: String) =
    open(context, Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone")))

/** Opens the messaging app with a new text to [phone]. */
fun textPhone(context: Context, phone: String) =
    open(context, Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$phone")))

/** Opens the email app with a new message to [email]. */
fun emailTo(context: Context, email: String) =
    open(context, Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$email")))

private fun open(context: Context, intent: Intent) {
    try {
        context.startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(context, "No app found to do that", Toast.LENGTH_SHORT).show()
    }
}
