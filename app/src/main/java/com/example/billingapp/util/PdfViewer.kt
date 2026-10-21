package com.example.billingapp.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast

/** Opens a PDF in whatever viewer the phone has, or says there isn't one. */
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

/** Opens the share sheet (WhatsApp, email, Drive, ...) with the PDF attached. */
fun sharePdf(context: Context, uri: Uri, subject: String, message: String) {
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "application/pdf"
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_SUBJECT, subject)
        putExtra(Intent.EXTRA_TEXT, message)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    val chooser = Intent.createChooser(send, "Share invoice").apply {
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    try {
        context.startActivity(chooser)
    } catch (e: Exception) {
        Toast.makeText(context, "Nothing on this phone can receive it", Toast.LENGTH_SHORT).show()
    }
}
