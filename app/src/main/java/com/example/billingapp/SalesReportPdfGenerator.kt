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
import java.util.Date
import java.util.Locale
import java.text.SimpleDateFormat

object SalesReportPdfGenerator {

    fun generateSalesReportPdf(
        context: Context,
        invoices: List<Invoice>,
        invoiceItems: List<InvoiceItem>,
        period: String
    ): Uri {

        val document = PdfDocument()

        val pageWidth = 595
        val pageHeight = 842

        val paint = Paint().apply {
            isAntiAlias = true
            textSize = 14f
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
            x: Float = 45f,
            size: Float = 14f
        ) {

            checkSpace(30f)

            paint.textSize = size

            canvas.drawText(
                text,
                x,
                y,
                paint
            )

            y += 25f
        }

        fun drawLine() {

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

        // --------------------------------
        // HEADER
        // --------------------------------

        paint.textSize = 24f

        canvas.drawText(
            "MONGINIS",
            220f,
            y,
            paint
        )

        y += 30f

        paint.textSize = 18f

        canvas.drawText(
            "SALES REPORT",
            225f,
            y,
            paint
        )

        y += 25f

        paint.textSize = 14f

        canvas.drawText(
            period,
            255f,
            y,
            paint
        )

        y += 25f

        drawLine()

        // --------------------------------
        // GROUP ITEMS BY DATE
        // --------------------------------

        val invoiceMap =
            invoices.associateBy { it.id }

        val dateFormatter =
            SimpleDateFormat(
                "dd MMM yyyy",
                Locale.getDefault()
            )

        val groupedItems =
            invoiceItems.groupBy { item ->

                val invoice =
                    invoiceMap[item.invoiceId]

                invoice?.let {
                    dateFormatter.format(
                        Date(it.createdAt)
                    )
                } ?: "Unknown Date"
            }

        // --------------------------------
        // DATE SECTIONS
        // --------------------------------

        groupedItems
            .toSortedMap()
            .forEach { (date, itemsForDate) ->

                checkSpace(120f)

                paint.textSize = 18f

                canvas.drawText(
                    date,
                    45f,
                    y,
                    paint
                )

                y += 25f

                drawLine()

                // Table header

                paint.textSize = 13f

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
                    "Amount",
                    470f,
                    y,
                    paint
                )

                y += 15f

                drawLine()

                // Group same product together

                val productsForDate =
                    itemsForDate.groupBy {
                        it.productName
                    }

                productsForDate.forEach {
                        (productName, productItems) ->

                    checkSpace(35f)

                    val quantity =
                        productItems.sumOf {
                            it.quantity
                        }

                    val amount =
                        productItems.sumOf {
                            it.amount
                        }

                    paint.textSize = 13f

                    canvas.drawText(
                        productName,
                        45f,
                        y,
                        paint
                    )

                    canvas.drawText(
                        quantity.toString(),
                        350f,
                        y,
                        paint
                    )

                    canvas.drawText(
                        "₹${"%.2f".format(
                            Locale.getDefault(),
                            amount
                        )}",
                        470f,
                        y,
                        paint
                    )

                    y += 25f
                }

                drawLine()

                // Daily subtotal

                val daySubtotal =
                    itemsForDate.sumOf {
                        it.amount
                    }
                val invoiceIdsForDate =
                    itemsForDate
                        .map { it.invoiceId }
                        .distinct()

                val dayInvoices =
                    invoices.filter {
                        it.id in invoiceIdsForDate
                    }

                val dayGst =
                    dayInvoices.sumOf {
                        it.gst
                    }

                val dayTotal =
                    dayInvoices.sumOf {
                        it.total
                    }

                drawText(
                    "Subtotal : ₹${"%.2f".format(
                        Locale.getDefault(),
                        daySubtotal
                    )}"
                )

                drawText(
                    "GST : ₹${"%.2f".format(
                        Locale.getDefault(),
                        dayGst
                    )}"
                )

                paint.textSize = 16f

                drawText(
                    "Day Total : ₹${"%.2f".format(
                        Locale.getDefault(),
                        dayTotal
                    )}",
                    size = 16f
                )

                y += 10f

                drawLine()
            }

        // --------------------------------
        // OVERALL SUMMARY
        // --------------------------------

        checkSpace(150f)

        paint.textSize = 20f

        canvas.drawText(
            "SALES SUMMARY",
            210f,
            y,
            paint
        )

        y += 30f

        drawLine()

        val totalBills =
            invoices.size

        val totalItems =
            invoiceItems.sumOf {
                it.quantity
            }

        val totalSales =
            invoices.sumOf {
                it.total
            }

        val totalSubtotal =
            invoices.sumOf {
                it.subtotal
            }

        val totalGst =
            invoices.sumOf {
                it.gst
            }

        drawText(
            "Total Bills : $totalBills"
        )

        drawText(
            "Total Items Sold : $totalItems"
        )

        drawText(
            "Subtotal : ₹${"%.2f".format(
                Locale.getDefault(),
                totalSubtotal
            )}"
        )

        drawText(
            "GST : ₹${"%.2f".format(
                Locale.getDefault(),
                totalGst
            )}"
        )

        y += 5f

        drawLine()

        paint.textSize = 20f

        drawText(
            "TOTAL SALES : ₹${"%.2f".format(
                Locale.getDefault(),
                totalSales
            )}",
            size = 20f
        )

        y += 15f

        drawLine()

        paint.textSize = 14f

        drawText(
            "Thank You!"
        )

        document.finishPage(page)

        // --------------------------------
        // SAVE PDF
        // --------------------------------

        val safePeriod =
            period.replace(
                " ",
                "_"
            )

        val fileName =
            "Sales_Report_$safePeriod.pdf"

        val resultUri: Uri

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {

            val values =
                ContentValues().apply {

                    put(
                        MediaStore.Downloads.DISPLAY_NAME,
                        fileName
                    )

                    put(
                        MediaStore.Downloads.MIME_TYPE,
                        "application/pdf"
                    )

                    put(
                        MediaStore.Downloads.RELATIVE_PATH,
                        Environment.DIRECTORY_DOWNLOADS +
                                "/Monginis Bills"
                    )
                }

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

            val directory =
                File(
                    Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_DOWNLOADS
                    ),
                    "Monginis Bills"
                )

            if (!directory.exists()) {
                directory.mkdirs()
            }

            val file =
                File(
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