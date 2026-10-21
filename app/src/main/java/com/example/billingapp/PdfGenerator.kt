package com.example.billingapp

import android.content.ContentValues
import android.content.Context
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.File
import java.io.FileOutputStream
import java.util.Locale

object PdfGenerator {

    fun generateInvoicePdf(
        context: Context,
        invoice: Invoice,
        items: List<InvoiceItem>
    ): Uri {

        val document = PdfDocument()

        val pageWidth = 595
        val pageHeight = 842

        val paint = Paint().apply {
            textSize = 16f
            isAntiAlias = true
        }

        var pageNumber = 1
        var y = 50f

        var pageInfo = PdfDocument.PageInfo.Builder(
            pageWidth,
            pageHeight,
            pageNumber
        ).create()

        var page = document.startPage(pageInfo)
        var canvas = page.canvas

        fun newPage() {

            document.finishPage(page)

            pageNumber++

            pageInfo = PdfDocument.PageInfo.Builder(
                pageWidth,
                pageHeight,
                pageNumber
            ).create()

            page = document.startPage(pageInfo)
            canvas = page.canvas

            y = 50f
        }

        fun checkSpace(requiredHeight: Float = 30f) {

            if (y + requiredHeight > pageHeight - 50f) {
                newPage()
            }
        }

        fun drawText(
            text: String,
            x: Float = 50f,
            size: Float = 16f
        ) {

            checkSpace(30f)

            paint.textSize = size

            canvas.drawText(
                text,
                x,
                y,
                paint
            )

            y += 30f
        }

        fun drawHorizontalLine() {

            checkSpace(20f)

            paint.strokeWidth = 1f

            canvas.drawLine(
                40f,
                y,
                555f,
                y,
                paint
            )

            y += 15f
        }

        // -------------------------
        // HEADER
        // -------------------------

        paint.textSize = 24f

        canvas.drawText(
            "MONGINIS",
            220f,
            y,
            paint
        )

        y += 25f

        paint.textSize = 14f

        canvas.drawText(
            "INVOICE",
            260f,
            y,
            paint
        )

        y += 30f

        drawHorizontalLine()

        // -------------------------
        // INVOICE DETAILS
        // -------------------------

        drawText(
            "Invoice #${invoice.id}"
        )

        drawText(
            "Customer : ${invoice.customerName}"
        )

        drawText(
            "Phone : ${invoice.phoneNumber}"
        )

        y += 5f

        drawHorizontalLine()

        // -------------------------
        // ITEMS HEADER
        // -------------------------

        paint.textSize = 14f

        checkSpace(30f)

        canvas.drawText(
            "Product",
            45f,
            y,
            paint
        )

        canvas.drawText(
            "Qty",
            350f,
            y,
            paint
        )

        canvas.drawText(
            "Price",
            405f,
            y,
            paint
        )

        canvas.drawText(
            "Amount",
            485f,
            y,
            paint
        )

        y += 15f

        drawHorizontalLine()

        // -------------------------
        // ITEMS
        // -------------------------

        paint.textSize = 14f

        items.forEach { item ->

            checkSpace(45f)

            canvas.drawText(
                item.productName,
                45f,
                y,
                paint
            )

            canvas.drawText(
                item.quantity.toString(),
                350f,
                y,
                paint
            )

            canvas.drawText(
                "₹${"%.2f".format(
                    Locale.getDefault(),
                    item.price
                )}",
                405f,
                y,
                paint
            )

            canvas.drawText(
                "₹${"%.2f".format(
                    Locale.getDefault(),
                    item.amount
                )}",
                485f,
                y,
                paint
            )

            y += 30f
        }

        drawHorizontalLine()

        // -------------------------
        // TOTALS
        // -------------------------

        checkSpace(120f)

        drawText(
            "Subtotal : ₹${"%.2f".format(
                Locale.getDefault(),
                invoice.subtotal
            )}"
        )

        drawText(
            "${gstLabel()} : ₹${"%.2f".format(
                Locale.getDefault(),
                invoice.gst
            )}"
        )

        y += 5f

        drawHorizontalLine()

        paint.textSize = 20f

        checkSpace(35f)

        canvas.drawText(
            "TOTAL : ₹${"%.2f".format(
                Locale.getDefault(),
                invoice.total
            )}",
            350f,
            y,
            paint
        )

        y += 35f

        drawHorizontalLine()

        // -------------------------
        // FOOTER
        // -------------------------

        checkSpace(40f)

        paint.textSize = 16f

        canvas.drawText(
            "Thank You!",
            50f,
            y,
            paint
        )

        document.finishPage(page)

        // -------------------------
        // SAVE PDF
        // -------------------------

        val fileName =
            "Invoice_${invoice.id}.pdf"

        var resultUri: Uri

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {

            val values = ContentValues()

            values.put(
                MediaStore.Downloads.DISPLAY_NAME,
                fileName
            )

            values.put(
                MediaStore.Downloads.MIME_TYPE,
                "application/pdf"
            )

            values.put(
                MediaStore.Downloads.RELATIVE_PATH,
                Environment.DIRECTORY_DOWNLOADS +
                        "/Monginis Bills"
            )

            resultUri =
                context.contentResolver.insert(
                    MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                    values
                )
                    ?: throw Exception(
                        "Unable to create PDF file"
                    )

            val outputStream =
                context.contentResolver
                    .openOutputStream(resultUri)
                    ?: throw Exception(
                        "Unable to open PDF file"
                    )

            document.writeTo(outputStream)

            outputStream.close()

        } else {

            val directory = File(
                Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DOWNLOADS
                ),
                "Monginis Bills"
            )

            if (!directory.exists()) {
                directory.mkdirs()
            }

            val file = File(
                directory,
                fileName
            )

            val outputStream =
                FileOutputStream(file)

            document.writeTo(outputStream)

            outputStream.close()

            resultUri =
                Uri.fromFile(file)
        }

        document.close()

        return resultUri
    }
}