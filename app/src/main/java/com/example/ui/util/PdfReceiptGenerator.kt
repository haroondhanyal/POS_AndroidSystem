package com.example.ui.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.model.SaleEntity
import com.example.data.model.SaleItemEntity
import com.example.ui.components.formatCurrency
import com.example.ui.components.formatDateTime
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException

object PdfReceiptGenerator {

    /**
     * Generates a professional PDF receipt and saves it to the cache directory.
     */
    fun generateReceiptPdf(
        context: Context,
        sale: SaleEntity,
        items: List<SaleItemEntity>
    ): File {
        val receiptsDir = File(context.cacheDir, "receipts").apply { mkdirs() }
        val pdfFile = File(receiptsDir, "Receipt_${sale.invoiceNumber}.pdf")

        val pageWidth = 360
        val headerHeight = 210
        val itemsHeight = items.size * 28 + 30
        val totalsHeight = 190
        val footerHeight = 110
        val pageHeight = (headerHeight + itemsHeight + totalsHeight + footerHeight).coerceAtLeast(560)

        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
        val page = document.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        // Background
        canvas.drawColor(Color.WHITE)

        // Paints
        val primaryPaint = Paint().apply {
            color = Color.rgb(30, 41, 59) // Slate 800
            isAntiAlias = true
        }

        val textCenterBold = Paint().apply {
            color = Color.rgb(15, 23, 42)
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = 15f
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }

        val textCenterSmall = Paint().apply {
            color = Color.rgb(100, 116, 139) // Slate 500
            textSize = 9.5f
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }

        val textBold = Paint().apply {
            color = Color.rgb(30, 41, 59)
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = 10f
            isAntiAlias = true
        }

        val textRegular = Paint().apply {
            color = Color.rgb(51, 65, 85)
            textSize = 9.5f
            isAntiAlias = true
        }

        val textMuted = Paint().apply {
            color = Color.rgb(100, 116, 139)
            textSize = 9f
            isAntiAlias = true
        }

        val dividerPaint = Paint().apply {
            color = Color.rgb(203, 213, 225) // Slate 300
            strokeWidth = 1f
            style = Paint.Style.STROKE
            isAntiAlias = true
        }

        val darkDividerPaint = Paint().apply {
            color = Color.rgb(148, 163, 184)
            strokeWidth = 1.5f
            style = Paint.Style.STROKE
            isAntiAlias = true
        }

        val margin = 20f
        val contentWidth = pageWidth - (margin * 2)
        var currentY = 32f

        // 1. Header Box
        canvas.drawText("RETAIL POS TERMINAL", pageWidth / 2f, currentY, textCenterBold)
        currentY += 15f
        canvas.drawText("Downtown Flagship Store #104", pageWidth / 2f, currentY, textCenterSmall)
        currentY += 13f
        canvas.drawText("Tel: (555) 019-2831  •  Tax ID: US-98421034", pageWidth / 2f, currentY, textCenterSmall)
        currentY += 16f

        // Status Banner if Refunded
        if (sale.status == "REFUNDED") {
            val refundBox = RectF(margin, currentY, pageWidth - margin, currentY + 22f)
            val refundBg = Paint().apply { color = Color.rgb(254, 226, 226) }
            val refundText = Paint().apply {
                color = Color.rgb(185, 28, 28)
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textSize = 10f
                textAlign = Paint.Align.CENTER
                isAntiAlias = true
            }
            canvas.drawRoundRect(refundBox, 4f, 4f, refundBg)
            canvas.drawText("*** REFUNDED RECEIPT ***", pageWidth / 2f, currentY + 15f, refundText)
            currentY += 30f
        } else {
            currentY += 4f
        }

        canvas.drawLine(margin, currentY, pageWidth - margin, currentY, darkDividerPaint)
        currentY += 16f

        // 2. Metadata Section
        fun drawMetaRow(label: String, value: String) {
            canvas.drawText(label, margin, currentY, textMuted)
            val valuePaint = Paint(textBold).apply { textAlign = Paint.Align.RIGHT }
            canvas.drawText(value, pageWidth - margin, currentY, valuePaint)
            currentY += 14f
        }

        drawMetaRow("Invoice No:", sale.invoiceNumber)
        drawMetaRow("Date & Time:", formatDateTime(sale.timestamp))
        drawMetaRow("Cashier:", sale.cashierName)
        drawMetaRow("Customer:", sale.customerName)
        drawMetaRow("Status:", sale.status)

        currentY += 6f
        canvas.drawLine(margin, currentY, pageWidth - margin, currentY, dividerPaint)
        currentY += 16f

        // 3. Items Table Header
        val colItem = margin
        val colQty = margin + 180f
        val colPrice = margin + 235f
        val colTotal = pageWidth - margin

        val headerPaint = Paint(textBold).apply { color = Color.rgb(15, 23, 42) }
        canvas.drawText("ITEM", colItem, currentY, headerPaint)
        canvas.drawText("QTY", colQty, currentY, headerPaint)
        canvas.drawText("PRICE", colPrice, currentY, headerPaint)
        val rightHeader = Paint(headerPaint).apply { textAlign = Paint.Align.RIGHT }
        canvas.drawText("TOTAL", colTotal, currentY, rightHeader)

        currentY += 6f
        canvas.drawLine(margin, currentY, pageWidth - margin, currentY, dividerPaint)
        currentY += 14f

        // Items List
        val rightAlignText = Paint(textRegular).apply { textAlign = Paint.Align.RIGHT }
        val rightAlignBold = Paint(textBold).apply { textAlign = Paint.Align.RIGHT }

        items.forEach { item ->
            val itemName = if (item.productName.length > 24) item.productName.take(22) + "..." else item.productName
            canvas.drawText(itemName, colItem, currentY, textBold)
            canvas.drawText("${item.quantity}", colQty + 6f, currentY, textRegular)
            canvas.drawText(formatCurrency(item.unitSellingPrice), colPrice - 4f, currentY, textRegular)
            canvas.drawText(formatCurrency(item.itemTotal), colTotal, currentY, rightAlignBold)

            currentY += 12f
            if (item.discountPercent > 0) {
                canvas.drawText("  * Disc: ${item.discountPercent.toInt()}% off", colItem, currentY, textMuted)
                currentY += 11f
            }
            currentY += 2f
        }

        currentY += 4f
        canvas.drawLine(margin, currentY, pageWidth - margin, currentY, dividerPaint)
        currentY += 16f

        // 4. Totals Breakdown
        fun drawAmountRow(label: String, amountStr: String, isBold: Boolean = false, isAccent: Boolean = false) {
            val labelP = if (isBold) textBold else textRegular
            val valP = Paint(if (isBold) textBold else textRegular).apply {
                textAlign = Paint.Align.RIGHT
                if (isAccent) {
                    color = Color.rgb(2, 132, 199) // Primary Sky/Blue
                    textSize = 14f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                }
            }
            canvas.drawText(label, margin, currentY, labelP)
            canvas.drawText(amountStr, colTotal, currentY, valP)
            currentY += if (isAccent) 20f else 14f
        }

        drawAmountRow("Subtotal:", formatCurrency(sale.subtotal))
        if (sale.itemDiscountTotal > 0) {
            drawAmountRow("Item Discounts:", "-${formatCurrency(sale.itemDiscountTotal)}")
        }
        if (sale.cartDiscountTotal > 0) {
            drawAmountRow("Cart Discount:", "-${formatCurrency(sale.cartDiscountTotal)}")
        }
        drawAmountRow("Tax (${formatCurrency(sale.taxTotal)} est):", formatCurrency(sale.taxTotal))

        currentY += 4f
        canvas.drawLine(margin, currentY, pageWidth - margin, currentY, darkDividerPaint)
        currentY += 16f

        drawAmountRow("GRAND TOTAL:", formatCurrency(sale.grandTotal), isBold = true, isAccent = true)

        canvas.drawLine(margin, currentY, pageWidth - margin, currentY, dividerPaint)
        currentY += 14f

        // Payment Method & Tendered
        drawAmountRow("Payment Method:", sale.paymentMethod.replace("_", " "), isBold = true)
        if (sale.paymentMethod == "CASH" || sale.paymentMethod == "SPLIT") {
            drawAmountRow("Cash Tendered:", formatCurrency(sale.amountReceived))
            drawAmountRow("Change Returned:", formatCurrency(sale.changeGiven))
        }

        currentY += 12f

        // 5. Barcode Simulation Graphic
        val barcodeWidth = contentWidth * 0.75f
        val barcodeLeft = (pageWidth - barcodeWidth) / 2f
        val barcodeHeight = 24f
        val barPaint = Paint().apply { color = Color.BLACK }

        var barX = barcodeLeft
        val barPattern = intArrayOf(2, 1, 3, 2, 1, 4, 2, 1, 3, 1, 2, 3, 1, 4, 2, 2, 1, 3, 2, 4, 1, 2, 3, 1, 2, 1, 3, 2, 1, 4, 2, 1, 3)
        var index = 0
        while (barX < barcodeLeft + barcodeWidth && index < barPattern.size * 2) {
            val width = barPattern[index % barPattern.size].toFloat()
            val drawBar = (index % 2 == 0)
            if (drawBar) {
                canvas.drawRect(barX, currentY, barX + width, currentY + barcodeHeight, barPaint)
            }
            barX += width + 1.5f
            index++
        }

        currentY += barcodeHeight + 10f
        canvas.drawText(sale.invoiceNumber, pageWidth / 2f, currentY, textCenterSmall)
        currentY += 16f

        // Footer Greeting
        val footerPaint = Paint().apply {
            color = Color.rgb(71, 85, 105)
            textSize = 9f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        canvas.drawText("Thank you for your business!", pageWidth / 2f, currentY, footerPaint)
        currentY += 12f
        canvas.drawText("Local SQLite POS • Verified Offline Transaction", pageWidth / 2f, currentY, textCenterSmall)

        document.finishPage(page)

        try {
            val fos = FileOutputStream(pdfFile)
            document.writeTo(fos)
            fos.flush()
            fos.close()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            document.close()
        }

        return pdfFile
    }

    /**
     * Opens the generated PDF in an external PDF viewer or chooser.
     */
    fun openPdf(context: Context, pdfFile: File) {
        if (!pdfFile.exists()) {
            Toast.makeText(context, "Receipt PDF not found. Generating...", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                pdfFile
            )

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            context.startActivity(Intent.createChooser(intent, "Open Receipt PDF"))
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(context, "No PDF viewer app found on device.", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(context, "Could not open PDF: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Shares the generated PDF file via standard Android share sheet.
     */
    fun sharePdf(context: Context, pdfFile: File, invoiceNumber: String) {
        if (!pdfFile.exists()) {
            Toast.makeText(context, "Receipt PDF file does not exist.", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                pdfFile
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Receipt $invoiceNumber")
                putExtra(
                    Intent.EXTRA_TEXT,
                    "Here is your official PDF receipt for invoice $invoiceNumber from Retail POS Terminal."
                )
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(shareIntent, "Share Receipt PDF")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (e: Exception) {
            Toast.makeText(context, "Could not share PDF: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Prints the PDF receipt using Android's native PrintManager.
     */
    fun printPdf(context: Context, pdfFile: File, invoiceNumber: String) {
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
        if (printManager == null) {
            Toast.makeText(context, "Print service not available on this device", Toast.LENGTH_SHORT).show()
            return
        }

        val printAdapter = object : PrintDocumentAdapter() {
            override fun onLayout(
                oldAttributes: PrintAttributes?,
                newAttributes: PrintAttributes?,
                cancellationSignal: CancellationSignal?,
                callback: LayoutResultCallback?,
                extras: Bundle?
            ) {
                if (cancellationSignal?.isCanceled == true) {
                    callback?.onLayoutCancelled()
                    return
                }

                val pdi = PrintDocumentInfo.Builder("Receipt_$invoiceNumber.pdf")
                    .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                    .setPageCount(1)
                    .build()

                callback?.onLayoutFinished(pdi, true)
            }

            override fun onWrite(
                pages: Array<out PageRange>?,
                destination: ParcelFileDescriptor?,
                cancellationSignal: CancellationSignal?,
                callback: WriteResultCallback?
            ) {
                var input: FileInputStream? = null
                var output: FileOutputStream? = null

                try {
                    input = FileInputStream(pdfFile)
                    output = FileOutputStream(destination?.fileDescriptor)

                    val buffer = ByteArray(1024)
                    var bytesRead: Int
                    while (input.read(buffer).also { bytesRead = it } >= 0) {
                        if (cancellationSignal?.isCanceled == true) {
                            callback?.onWriteCancelled()
                            return
                        }
                        output.write(buffer, 0, bytesRead)
                    }

                    callback?.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
                } catch (e: Exception) {
                    callback?.onWriteFailed(e.message)
                } finally {
                    try {
                        input?.close()
                        output?.close()
                    } catch (e: IOException) {
                        // ignore
                    }
                }
            }
        }

        val printJobName = "Receipt_$invoiceNumber"
        val attributes = PrintAttributes.Builder()
            .setMediaSize(PrintAttributes.MediaSize.ISO_A6)
            .setColorMode(PrintAttributes.COLOR_MODE_MONOCHROME)
            .build()

        printManager.print(printJobName, printAdapter, attributes)
    }

    /**
     * Generates a PDF report for sales summary (Today, Weekly, Monthly, Yearly).
     */
    fun generateSalesReportPdf(
        context: Context,
        periodName: String,
        totalRevenue: Double,
        estimatedProfit: Double,
        estimatedCOGS: Double,
        totalTransactions: Int,
        averageTicket: Double,
        topProducts: List<Pair<String, Pair<Int, Double>>>,
        sales: List<SaleEntity>
    ): File {
        val reportsDir = File(context.cacheDir, "receipts").apply { mkdirs() }
        val sanitizedPeriod = periodName.replace(" ", "_")
        val pdfFile = File(reportsDir, "Sales_Report_${sanitizedPeriod}_${System.currentTimeMillis()}.pdf")

        val pageWidth = 420
        val pageHeight = 700

        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
        val page = document.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        canvas.drawColor(Color.WHITE)

        val headerPaint = Paint().apply {
            color = Color.parseColor("#1E293B")
            textSize = 18f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val subPaint = Paint().apply {
            color = Color.parseColor("#64748B")
            textSize = 10f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            isAntiAlias = true
        }

        val boldPaint = Paint().apply {
            color = Color.parseColor("#0F172A")
            textSize = 11f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val regularPaint = Paint().apply {
            color = Color.parseColor("#334155")
            textSize = 10f
            isAntiAlias = true
        }

        val primaryBarPaint = Paint().apply {
            color = Color.parseColor("#0284C7")
            style = Paint.Style.FILL
            isAntiAlias = true
        }

        val linePaint = Paint().apply {
            color = Color.parseColor("#E2E8F0")
            strokeWidth = 1f
        }

        // Top decorative bar
        canvas.drawRect(0f, 0f, pageWidth.toFloat(), 6f, primaryBarPaint)

        var y = 36f
        canvas.drawText("RETAIL POS SALES REPORT", 20f, y, headerPaint)
        y += 16f
        canvas.drawText("Period: $periodName • Generated on: ${formatDateTime(System.currentTimeMillis())}", 20f, y, subPaint)
        y += 18f
        canvas.drawLine(20f, y, (pageWidth - 20).toFloat(), y, linePaint)

        // KPI Cards Row
        y += 20f
        val cardPaint = Paint().apply {
            color = Color.parseColor("#F8FAFC")
            style = Paint.Style.FILL
        }
        val cardBorder = Paint().apply {
            color = Color.parseColor("#E2E8F0")
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }

        // Gross Revenue Card
        val card1 = RectF(20f, y, 195f, y + 55f)
        canvas.drawRoundRect(card1, 8f, 8f, cardPaint)
        canvas.drawRoundRect(card1, 8f, 8f, cardBorder)
        canvas.drawText("TOTAL GROSS REVENUE", 30f, y + 20f, subPaint)
        val kpiValPaint = Paint().apply {
            color = Color.parseColor("#0369A1")
            textSize = 16f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        canvas.drawText(formatCurrency(totalRevenue), 30f, y + 42f, kpiValPaint)

        // Est Profit Card
        val card2 = RectF(215f, y, (pageWidth - 20).toFloat(), y + 55f)
        canvas.drawRoundRect(card2, 8f, 8f, cardPaint)
        canvas.drawRoundRect(card2, 8f, 8f, cardBorder)
        canvas.drawText("ESTIMATED GROSS PROFIT", 225f, y + 20f, subPaint)
        val profitPaint = Paint().apply {
            color = Color.parseColor("#15803D")
            textSize = 16f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        canvas.drawText(formatCurrency(estimatedProfit), 225f, y + 42f, profitPaint)

        y += 70f
        // Quick metrics row
        canvas.drawText("Transactions: $totalTransactions orders", 20f, y, regularPaint)
        canvas.drawText("Avg Ticket: ${formatCurrency(averageTicket)}", 150f, y, regularPaint)
        canvas.drawText("Est. COGS: ${formatCurrency(estimatedCOGS)}", 280f, y, regularPaint)

        y += 18f
        canvas.drawLine(20f, y, (pageWidth - 20).toFloat(), y, linePaint)

        // Top Selling Section
        y += 20f
        canvas.drawText("TOP SELLING PRODUCTS ($periodName)", 20f, y, boldPaint)
        y += 14f

        val tableHeaderPaint = Paint().apply {
            color = Color.parseColor("#F1F5F9")
            style = Paint.Style.FILL
        }
        canvas.drawRect(20f, y, (pageWidth - 20).toFloat(), y + 20f, tableHeaderPaint)
        canvas.drawText("Product", 26f, y + 14f, subPaint)
        canvas.drawText("Qty Sold", 260f, y + 14f, subPaint)
        canvas.drawText("Revenue", 340f, y + 14f, subPaint)
        y += 24f

        if (topProducts.isEmpty()) {
            canvas.drawText("No product sales data in this period.", 26f, y + 10f, subPaint)
            y += 20f
        } else {
            for (item in topProducts.take(6)) {
                val prodName = if (item.first.length > 32) item.first.take(30) + "…" else item.first
                canvas.drawText(prodName, 26f, y + 12f, regularPaint)
                canvas.drawText("${item.second.first}", 265f, y + 12f, regularPaint)
                canvas.drawText(formatCurrency(item.second.second), 340f, y + 12f, boldPaint)
                y += 20f
            }
        }

        y += 10f
        canvas.drawLine(20f, y, (pageWidth - 20).toFloat(), y, linePaint)

        // Recent Orders in Period
        y += 20f
        canvas.drawText("RECENT TRANSACTIONS IN PERIOD", 20f, y, boldPaint)
        y += 14f
        canvas.drawRect(20f, y, (pageWidth - 20).toFloat(), y + 20f, tableHeaderPaint)
        canvas.drawText("Invoice", 26f, y + 14f, subPaint)
        canvas.drawText("Cashier", 140f, y + 14f, subPaint)
        canvas.drawText("Method", 240f, y + 14f, subPaint)
        canvas.drawText("Total", 340f, y + 14f, subPaint)
        y += 24f

        for (sale in sales.take(8)) {
            canvas.drawText(sale.invoiceNumber, 26f, y + 12f, regularPaint)
            val cashier = if (sale.cashierName.length > 14) sale.cashierName.take(12) + "…" else sale.cashierName
            canvas.drawText(cashier, 140f, y + 12f, regularPaint)
            canvas.drawText(sale.paymentMethod, 240f, y + 12f, regularPaint)
            canvas.drawText(formatCurrency(sale.grandTotal), 340f, y + 12f, boldPaint)
            y += 20f
        }

        // Footer
        val footerY = (pageHeight - 25).toFloat()
        canvas.drawLine(20f, footerY - 10f, (pageWidth - 20).toFloat(), footerY - 10f, linePaint)
        val footerPaint = Paint().apply {
            color = Color.parseColor("#94A3B8")
            textSize = 9f
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        canvas.drawText("Modern Retail POS • Automated Financial Audit & Reporting", (pageWidth / 2).toFloat(), footerY, footerPaint)

        document.finishPage(page)

        try {
            FileOutputStream(pdfFile).use { out ->
                document.writeTo(out)
            }
        } finally {
            document.close()
        }

        return pdfFile
    }
}
