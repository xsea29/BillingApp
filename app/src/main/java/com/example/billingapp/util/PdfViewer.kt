package com.example.billingapp.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast

/** Opens a saved PDF in whatever viewer the phone has, or says there isn't one. */
fun openPdf(context: Context, uri: Uri) {
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, "application/pdf")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    try {
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "No PDF viewer found", Toast.LENGTH_SHORT).show()
    }
}
