package com.example.billingapp

import android.content.ContentValues
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import androidx.core.content.res.ResourcesCompat
import com.example.billingapp.pdf.Align
import com.example.billingapp.pdf.Face
import com.example.billingapp.pdf.PAGE_HEIGHT
import com.example.billingapp.pdf.PAGE_WIDTH
import com.example.billingapp.pdf.PdfOp
import com.example.billingapp.pdf.PdfPage
import com.example.billingapp.pdf.layoutInvoice
import java.io.File
import java.io.FileOutputStream

/**
 * Turns an invoice into a PDF file. What goes where on the page is decided by [layoutInvoice]
 * (plain code, tested); this only draws those instructions with Android's PDF canvas.
 */
object PdfGenerator {

    private const val DOWNLOAD_FOLDER = "Monginis Bills"

    /**
     * Builds the invoice PDF in the app's private cache. The name is the same every time
     * ("Invoice_28.pdf"), so making it again replaces the old one instead of piling up copies.
     * Use [uriFor] to hand it to the viewer or the share sheet, [saveToDownloads] to keep a copy.
     */
    fun createInvoiceFile(context: Context, invoice: Invoice, items: List<InvoiceItem>): File {
        val fonts = Fonts(context)

        // The layout needs real text widths to cut long names and wrap lines.
        val ruler = Paint(Paint.ANTI_ALIAS_FLAG)
        val pages = layoutInvoice(invoice, items) { text, size, face ->
            ruler.typeface = fonts.of(face)
            ruler.textSize = size
            ruler.measureText(text)
        }

        val folder = File(context.cacheDir, "invoices").apply { mkdirs() }
        val file = File(folder, "Invoice_${invoice.id}.pdf")

        val document = PdfDocument()

        try {
            val paint = Paint(Paint.ANTI_ALIAS_FLAG)

            pages.forEachIndexed { index, page ->
                val info = PdfDocument.PageInfo
                    .Builder(PAGE_WIDTH.toInt(), PAGE_HEIGHT.toInt(), index + 1)
                    .create()

                val pdfPage = document.startPage(info)
                draw(pdfPage.canvas, page, paint, fonts)
                document.finishPage(pdfPage)
            }

            FileOutputStream(file).use { document.writeTo(it) }

        } finally {
            document.close()
        }

        return file
    }

    /** An address other apps may read (through the app's FileProvider) for [file]. */
    fun uriFor(context: Context, file: File): Uri =
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)

    /**
     * Copies [file] to the phone's Downloads folder, in "Monginis Bills". Downloading the same
     * invoice again replaces the earlier copy. Returns false if the phone wouldn't let us
     * (older Android versions need a storage permission this app doesn't ask for).
     */
    fun saveToDownloads(context: Context, file: File): Boolean {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {

                val resolver = context.contentResolver
                val folder = "${Environment.DIRECTORY_DOWNLOADS}/$DOWNLOAD_FOLDER"

                // Replace our own earlier copy; otherwise Android keeps both as "Invoice_28 (1).pdf".
                runCatching {
                    resolver.delete(
                        MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                        "${MediaStore.MediaColumns.DISPLAY_NAME} = ? AND ${MediaStore.MediaColumns.RELATIVE_PATH} = ?",
                        arrayOf(file.name, "$folder/")
                    )
                }

                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, file.name)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, folder)
                }

                val target = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                    ?: return false

                val out = resolver.openOutputStream(target) ?: return false
                out.use { stream -> file.inputStream().use { it.copyTo(stream) } }

            } else {

                val folder = File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                    DOWNLOAD_FOLDER
                )
                folder.mkdirs()
                file.copyTo(File(folder, file.name), overwrite = true)
            }

            return true

        } catch (e: Exception) {
            return false
        }
    }

    // ------------------------------------------------------------------ drawing

    private class Fonts(context: Context) {
        private val serif: Typeface =
            ResourcesCompat.getFont(context, R.font.instrument_serif_regular) ?: Typeface.SERIF
        private val sans: Typeface = Typeface.create("sans-serif", Typeface.NORMAL)
        private val bold: Typeface = Typeface.create("sans-serif", Typeface.BOLD)

        fun of(face: Face): Typeface = when (face) {
            Face.Serif -> serif
            Face.Sans -> sans
            Face.SansBold -> bold
        }
    }

    private fun gray(level: Int): Int = Color.rgb(level, level, level)

    private fun draw(canvas: Canvas, page: PdfPage, paint: Paint, fonts: Fonts) {
        for (op in page.ops) {
            when (op) {
                is PdfOp.Box -> {
                    paint.style = Paint.Style.FILL
                    paint.color = gray(op.gray)
                    canvas.drawRect(op.x, op.y, op.x + op.width, op.y + op.height, paint)
                }

                is PdfOp.Line -> {
                    paint.style = Paint.Style.STROKE
                    paint.strokeWidth = op.width
                    paint.color = gray(op.gray)
                    canvas.drawLine(op.x1, op.y1, op.x2, op.y2, paint)
                }

                is PdfOp.Text -> {
                    paint.style = Paint.Style.FILL
                    paint.color = gray(op.gray)
                    paint.typeface = fonts.of(op.face)
                    paint.textSize = op.size
                    paint.letterSpacing = op.spacing
                    paint.textAlign = when (op.align) {
                        Align.Left -> Paint.Align.LEFT
                        Align.Center -> Paint.Align.CENTER
                        Align.Right -> Paint.Align.RIGHT
                    }
                    canvas.drawText(op.text, op.x, op.y, paint)
                }
            }
        }
    }
}
