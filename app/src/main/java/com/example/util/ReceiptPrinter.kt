package com.example.util

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import android.print.pdf.PrintedPdfDocument
import com.example.data.OrderDetails
import com.example.data.ShopProfile
import com.example.data.entity.PaymentMethod
import com.example.ui.PosViewModel
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ReceiptPrinter {

    fun printReceipt(
        context: Context,
        orderDetails: OrderDetails,
        shopProfile: ShopProfile,
        paperSize: String = "58mm",
        displayCurrency: String = orderDetails.order.currency,
        showCurrencyDetails: Boolean = false
    ) {
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager ?: return
        val jobName = "Receipt_${orderDetails.order.invoiceNumber}"

        val printAdapter = object : PrintDocumentAdapter() {
            private var pdfDocument: PrintedPdfDocument? = null
            private val is58mm = paperSize.contains("58")

            override fun onLayout(
                oldAttributes: PrintAttributes?,
                newAttributes: PrintAttributes,
                cancellationSignal: CancellationSignal?,
                callback: LayoutResultCallback,
                metadata: Bundle?
            ) {
                if (cancellationSignal?.isCanceled == true) {
                    callback.onLayoutCancelled()
                    return
                }

                // Create custom media size based on paper selection (width in mils: 1/1000 inch)
                val mediaSize = newAttributes.mediaSize ?: if (is58mm) {
                    PrintAttributes.MediaSize("Receipt_58mm", "Thermal 58mm", 2280, 12000)
                } else {
                    PrintAttributes.MediaSize("Receipt_80mm", "Thermal 80mm", 3150, 14000)
                }

                val customAttributes = PrintAttributes.Builder()
                    .setMediaSize(mediaSize)
                    .setResolution(PrintAttributes.Resolution("res1", "Receipt Printer", 203, 203))
                    .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
                    .build()

                pdfDocument = PrintedPdfDocument(context, customAttributes)

                val info = PrintDocumentInfo.Builder(jobName)
                    .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                    .setPageCount(1)
                    .build()

                callback.onLayoutFinished(info, true)
            }

            override fun onWrite(
                pages: Array<out PageRange>?,
                destination: ParcelFileDescriptor,
                cancellationSignal: CancellationSignal?,
                callback: WriteResultCallback
            ) {
                if (cancellationSignal?.isCanceled == true) {
                    callback.onWriteCancelled()
                    return
                }

                val page = pdfDocument?.startPage(0) ?: return
                val canvas = page.canvas
                val actualPageWidth = page.info.pageWidth.takeIf { it > 0 } ?: if (is58mm) 164 else 226

                renderReceiptToCanvas(
                    canvas = canvas,
                    orderDetails = orderDetails,
                    shopProfile = shopProfile,
                    width = actualPageWidth,
                    is58mm = is58mm,
                    displayCurrency = displayCurrency,
                    showCurrencyDetails = showCurrencyDetails
                )

                pdfDocument?.finishPage(page)

                try {
                    pdfDocument?.writeTo(FileOutputStream(destination.fileDescriptor))
                    callback.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
                } catch (e: IOException) {
                    callback.onWriteFailed(e.message)
                } finally {
                    pdfDocument?.close()
                    pdfDocument = null
                }
            }
        }

        printManager.print(jobName, printAdapter, PrintAttributes.Builder().build())
    }

    /**
     * Splits text into multiple lines that fit within [maxWidth] pixels using [paint].
     * Never truncates with ".."; all text is preserved across lines.
     */
    private fun wrapTextToLines(text: String, paint: Paint, maxWidth: Float): List<String> {
        if (text.isBlank()) return emptyList()
        if (maxWidth <= 10f) return listOf(text)

        val result = mutableListOf<String>()
        val paragraphs = text.split("\n")
        for (paragraph in paragraphs) {
            var remaining = paragraph.trim()
            if (remaining.isEmpty()) continue
            while (remaining.isNotEmpty()) {
                val count = paint.breakText(remaining, true, maxWidth, null)
                if (count <= 0 || count >= remaining.length) {
                    result.add(remaining)
                    break
                }
                // Try breaking at a natural space or separator if close to the end of the line
                var breakIndex = count
                val lastSpace = remaining.substring(0, count).lastIndexOf(' ')
                if (lastSpace > count / 2) {
                    breakIndex = lastSpace
                }
                result.add(remaining.substring(0, breakIndex).trim())
                remaining = remaining.substring(breakIndex).trimStart()
            }
        }
        return result
    }

    /**
     * Draws centered multi-line text and returns the updated Y position.
     */
    private fun drawCenteredWrappedText(
        canvas: Canvas,
        text: String,
        centerX: Float,
        startY: Float,
        maxWidth: Float,
        paint: Paint,
        lineHeight: Float
    ): Float {
        var y = startY
        paint.textAlign = Paint.Align.CENTER
        val lines = wrapTextToLines(text, paint, maxWidth)
        for (line in lines) {
            canvas.drawText(line, centerX, y, paint)
            y += lineHeight
        }
        return y
    }

    /**
     * Draws a left-aligned label and right-aligned value on the same row if they fit,
     * or wraps the label/value across lines so neither overlaps or gets cut off.
     */
    private fun drawKeyValueRow(
        canvas: Canvas,
        label: String,
        value: String,
        leftX: Float,
        rightX: Float,
        startY: Float,
        paint: Paint,
        lineHeight: Float
    ): Float {
        var y = startY
        val totalWidth = rightX - leftX
        val labelWidth = paint.measureText(label)
        val valueWidth = paint.measureText(value)

        if (labelWidth + valueWidth + 8f <= totalWidth) {
            paint.textAlign = Paint.Align.LEFT
            canvas.drawText(label, leftX, y, paint)
            paint.textAlign = Paint.Align.RIGHT
            canvas.drawText(value, rightX, y, paint)
            return y + lineHeight
        }

        // If value fits on the right, wrap label on the left with reserved space for value
        if (valueWidth < totalWidth * 0.55f) {
            val maxLabelWidth = (totalWidth - valueWidth - 6f).coerceAtLeast(totalWidth * 0.4f)
            val labelLines = wrapTextToLines(label, paint, maxLabelWidth)
            paint.textAlign = Paint.Align.LEFT
            labelLines.forEachIndexed { index, line ->
                canvas.drawText(line, leftX, y, paint)
                if (index == labelLines.lastIndex) {
                    paint.textAlign = Paint.Align.RIGHT
                    canvas.drawText(value, rightX, y, paint)
                }
                y += lineHeight
            }
            return y
        } else {
            // Print label on its own wrapped line(s) and value right-aligned below it
            paint.textAlign = Paint.Align.LEFT
            val labelLines = wrapTextToLines(label, paint, totalWidth)
            for (line in labelLines) {
                canvas.drawText(line, leftX, y, paint)
                y += lineHeight
            }
            paint.textAlign = Paint.Align.RIGHT
            val valueLines = wrapTextToLines(value, paint, totalWidth)
            for (vLine in valueLines) {
                canvas.drawText(vLine, rightX, y, paint)
                y += lineHeight
            }
            return y
        }
    }

    private fun renderReceiptToCanvas(
        canvas: Canvas,
        orderDetails: OrderDetails,
        shopProfile: ShopProfile,
        width: Int,
        is58mm: Boolean,
        displayCurrency: String = orderDetails.order.currency,
        showCurrencyDetails: Boolean = false
    ) {
        val order = orderDetails.order
        val items = orderDetails.items
        val dateFormat = SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.getDefault())
        val formattedDate = dateFormat.format(Date(order.timestamp))

        canvas.drawColor(Color.WHITE)

        val paintText = Paint().apply {
            color = Color.BLACK
            isAntiAlias = true
        }

        val paintLine = Paint().apply {
            color = Color.DKGRAY
            strokeWidth = 0.8f
            style = Paint.Style.STROKE
        }

        // Scale typography proportionally to actual canvas width so 58mm (164pt), 80mm (226pt), or A4 all render crisply
        val scale = (width.toFloat() / if (is58mm) 164f else 226f).coerceIn(0.85f, 1.8f)
        val margin = (if (is58mm) 6f else 10f) * scale
        val leftMargin = margin
        val rightMargin = width - margin
        val contentWidth = rightMargin - leftMargin
        val centerX = width / 2f

        val titleSize = (if (is58mm) 10.5f else 12.5f) * scale
        val subHeaderSize = (if (is58mm) 7.5f else 8.5f) * scale
        val bodySize = (if (is58mm) 7.2f else 8.2f) * scale
        val smallSize = (if (is58mm) 6.6f else 7.5f) * scale
        val totalSize = (if (is58mm) 9.0f else 10.5f) * scale
        val bodyLineHeight = bodySize * 1.45f

        var y = 18f * scale

        // Shop Name Header (Wrapped so long shop names appear in full)
        val shopTitle = if (shopProfile.shopName.isNotBlank()) shopProfile.shopName else "အရောင်းပြေစာ"
        paintText.textSize = titleSize
        paintText.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        y = drawCenteredWrappedText(canvas, shopTitle, centerX, y, contentWidth, paintText, titleSize * 1.35f)

        if (shopProfile.englishName.isNotBlank()) {
            paintText.textSize = subHeaderSize
            paintText.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            y = drawCenteredWrappedText(canvas, shopProfile.englishName, centerX, y, contentWidth, paintText, subHeaderSize * 1.35f)
        }

        if (shopProfile.address.isNotBlank()) {
            paintText.textSize = smallSize
            paintText.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            y = drawCenteredWrappedText(canvas, shopProfile.address, centerX, y, contentWidth, paintText, smallSize * 1.4f)
        }

        if (shopProfile.phone.isNotBlank()) {
            paintText.textSize = smallSize
            paintText.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            y = drawCenteredWrappedText(canvas, "ဖုန်း - ${shopProfile.phone}", centerX, y, contentWidth, paintText, smallSize * 1.4f)
        }

        // Divider
        y += 2f * scale
        canvas.drawLine(leftMargin, y, rightMargin, y, paintLine)
        y += bodyLineHeight

        // Invoice Number & Date & Cashier (All wrapped safely without overlapping)
        paintText.textSize = bodySize
        paintText.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        y = drawKeyValueRow(canvas, "ဘောက်ချာ:", order.invoiceNumber, leftMargin, rightMargin, y, paintText, bodyLineHeight)

        paintText.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paintText.textSize = smallSize
        val smallLineHeight = smallSize * 1.45f
        y = drawKeyValueRow(canvas, "နေ့စွဲ: $formattedDate", "ကောင်တာ: ${shopProfile.cashierName.ifBlank { "Cashier" }}", leftMargin, rightMargin, y, paintText, smallLineHeight)

        if (order.customerName.isNotBlank() || order.customerPhone.isNotBlank()) {
            val custLabel = if (order.customerName.isNotBlank()) "ဝယ်ယူသူ: ${order.customerName}" else "ဝယ်ယူသူ:"
            y = drawKeyValueRow(canvas, custLabel, order.customerPhone, leftMargin, rightMargin, y, paintText, smallLineHeight)
        }

        // Divider
        y += 2f * scale
        canvas.drawLine(leftMargin, y, rightMargin, y, paintLine)
        y += bodyLineHeight

        // Table Header
        paintText.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paintText.textSize = bodySize
        paintText.textAlign = Paint.Align.LEFT
        canvas.drawText("ပစ္စည်းအမည်", leftMargin, y, paintText)

        val amountColWidth = contentWidth * 0.34f
        val qtyColWidth = contentWidth * 0.16f
        val nameColMaxWidth = contentWidth - amountColWidth - qtyColWidth - (4f * scale)
        val qtyCenterX = leftMargin + nameColMaxWidth + (qtyColWidth / 2f)

        paintText.textAlign = Paint.Align.CENTER
        canvas.drawText("ဦးရေ", qtyCenterX, y, paintText)
        paintText.textAlign = Paint.Align.RIGHT
        canvas.drawText(if (displayCurrency == "THB") "သင့်ငွေ(฿)" else "သင့်ငွေ(Ks)", rightMargin, y, paintText)
        y += 6f * scale

        canvas.drawLine(leftMargin, y, rightMargin, y, paintLine)
        y += bodyLineHeight

        // Items List (Full product name wrapped across lines + unit price + quantity + subtotal)
        val isThbReceipt = displayCurrency == "THB"
        val thbSellFromShop = shopProfile.thbSellingRate.replace(",", ".").toDoubleOrNull() ?: 0.0
        val thbBuyFromShop = shopProfile.thbBuyingRate.replace(",", ".").toDoubleOrNull() ?: thbSellFromShop
        val mmkSellFromShop = shopProfile.mmkSellingRate.replace(",", ".").toDoubleOrNull() ?: (if (thbSellFromShop > 0) 1.0 / thbSellFromShop else 0.0)
        val mmkBuyFromShop = shopProfile.mmkBuyingRate.replace(",", ".").toDoubleOrNull() ?: (if (thbBuyFromShop > 0) 1.0 / thbBuyFromShop else mmkSellFromShop)

        val fallbackThbToMmk = if (isThbReceipt) {
            if (mmkBuyFromShop > 0.0) com.example.data.toThbToMmkRate(mmkBuyFromShop) else if (thbBuyFromShop > 0.0) thbBuyFromShop else 129.43
        } else {
            if (mmkSellFromShop > 0.0) com.example.data.toThbToMmkRate(mmkSellFromShop) else if (thbSellFromShop > 0.0) thbSellFromShop else 129.43
        }
        val rate = if (order.exchangeRate > 0) com.example.data.toThbToMmkRate(order.exchangeRate) else fallbackThbToMmk

        for (item in items) {
            val isThbItem = com.example.data.isThbCurrencyString(item.currency) || com.example.data.isThbCurrencyString(item.productName)
            val uPrice = if (isThbReceipt) {
                if (!isThbItem) (if (rate > 0) Math.round(item.unitPrice / rate) else 0L) else item.unitPrice
            } else {
                if (isThbItem) Math.round(item.unitPrice * rate) else item.unitPrice
            }
            val sTotal = if (isThbReceipt) {
                if (!isThbItem) (if (rate > 0) Math.round(item.subtotal / rate) else 0L) else item.subtotal
            } else {
                if (isThbItem) Math.round(item.subtotal * rate) else item.subtotal
            }

            val subtotalText = if (isThbReceipt) {
                PosViewModel.formatCurrency(sTotal, "THB")
            } else {
                PosViewModel.formatKyatMm(sTotal)
            }
            val unitPriceText = if (isThbReceipt) {
                "@ ${PosViewModel.formatCurrency(uPrice, "THB")}"
            } else {
                "@ ${PosViewModel.formatKyatMm(uPrice)}"
            }

            paintText.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paintText.textSize = bodySize
            val nameLines = wrapTextToLines(item.productName, paintText, nameColMaxWidth)

            nameLines.forEachIndexed { index, line ->
                paintText.textAlign = Paint.Align.LEFT
                canvas.drawText(line, leftMargin, y, paintText)

                // Draw quantity and subtotal on the first line of the item
                if (index == 0) {
                    paintText.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                    paintText.textAlign = Paint.Align.CENTER
                    canvas.drawText("x${item.quantity}", qtyCenterX, y, paintText)

                    paintText.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    paintText.textAlign = Paint.Align.RIGHT
                    canvas.drawText(subtotalText, rightMargin, y, paintText)
                }
                y += bodyLineHeight
            }

            // Unit price subtitle line
            paintText.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            paintText.textSize = smallSize
            paintText.textAlign = Paint.Align.LEFT
            canvas.drawText(unitPriceText, leftMargin, y, paintText)
            y += smallLineHeight + (2f * scale)
        }

        // Divider
        y += 2f * scale
        canvas.drawLine(leftMargin, y, rightMargin, y, paintLine)
        y += bodyLineHeight

        // Subtotal / Discount / Total
        paintText.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paintText.textSize = bodySize
        val effSubtotal = if (isThbReceipt) order.getEffectiveSubtotalThb() else order.getEffectiveSubtotalMmk()
        val subtotalLabel = if (isThbReceipt) "စုစုပေါင်း (ဘတ်) :" else "စုစုပေါင်း (ကျပ်) :"
        val subtotalVal = if (isThbReceipt) PosViewModel.formatCurrency(effSubtotal, "THB") else PosViewModel.formatKyatMm(effSubtotal)
        y = drawKeyValueRow(canvas, subtotalLabel, subtotalVal, leftMargin, rightMargin, y, paintText, bodyLineHeight)

        if (order.discount > 0) {
            val effDiscount = if (isThbReceipt) order.getEffectiveDiscountThb() else order.getEffectiveDiscountMmk()
            val discountLabel = if (isThbReceipt) "အထူးလျှော့စျေး (ဘတ်) :" else "အထူးလျှော့စျေး (ကျပ်) :"
            val discountVal = if (isThbReceipt) "-${PosViewModel.formatCurrency(effDiscount, "THB")}" else "-${PosViewModel.formatKyatMm(effDiscount)}"
            y = drawKeyValueRow(canvas, discountLabel, discountVal, leftMargin, rightMargin, y, paintText, bodyLineHeight)
        }

        // Grand Total
        y += 2f * scale
        canvas.drawLine(leftMargin, y, rightMargin, y, paintLine)
        y += totalSize * 1.25f

        val effGrandTotal = if (isThbReceipt) order.getEffectiveTotalThb() else order.getEffectiveTotalMmk()
        paintText.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paintText.textSize = totalSize
        val totalLabel = if (isThbReceipt) "အသားတင် ကျသင့်ငွေ (ဘတ်) :" else "အသားတင် ကျသင့်ငွေ (ကျပ်) :"
        val totalVal = if (isThbReceipt) PosViewModel.formatCurrency(effGrandTotal, "THB") else PosViewModel.formatKyatMm(effGrandTotal)
        y = drawKeyValueRow(canvas, totalLabel, totalVal, leftMargin, rightMargin, y, paintText, totalSize * 1.4f)

        // Dual Currency & Exchange Rate Details (Only printed when showCurrencyDetails is true)
        if (showCurrencyDetails) {
            val prMmk = order.getEffectiveTotalMmk()
            val prThb = order.getEffectiveTotalThb()

            paintText.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            paintText.textSize = smallSize
            y = drawKeyValueRow(canvas, "မြန်မာငွေ စုစုပေါင်း :", PosViewModel.formatKyatMm(prMmk), leftMargin, rightMargin, y, paintText, smallLineHeight)
            y = drawKeyValueRow(canvas, "ထိုင်းငွေ စုစုပေါင်း :", PosViewModel.formatCurrency(prThb, "THB"), leftMargin, rightMargin, y, paintText, smallLineHeight)

            val totalRawProfitMmk = items.sumOf { item ->
                val unitProfit = item.unitPrice - item.costPrice
                val isThbItem = com.example.data.isThbCurrencyString(item.currency) || com.example.data.isThbCurrencyString(item.productName)
                val itemProfitMmk = if (isThbItem) Math.round(unitProfit * rate) else unitProfit
                itemProfitMmk * item.quantity
            }
            val netProfitMmk = (totalRawProfitMmk - order.getEffectiveDiscountMmk()).coerceAtLeast(0L)
            val profitActive = if (isThbReceipt) (if (rate > 0) Math.round(netProfitMmk.toDouble() / rate) else 0L) else netProfitMmk
            val profitMarginPercent = if (effGrandTotal > 0) ((profitActive.toDouble() / effGrandTotal) * 100).toInt() else 0

            y = drawKeyValueRow(
                canvas,
                "ခန့်မှန်း အသားတင်အမြတ် :",
                "($profitMarginPercent%) +${PosViewModel.formatCurrency(profitActive, displayCurrency)}",
                leftMargin,
                rightMargin,
                y,
                paintText,
                smallLineHeight
            )

            val activeThbToMmk = if (order.exchangeRate > 0) com.example.data.toThbToMmkRate(order.exchangeRate) else rate
            if (activeThbToMmk > 0) {
                val bahtAmount = if (order.exchangeRate > 0) {
                    com.example.data.toMmkToThbRate(order.exchangeRate)
                } else if (!isThbReceipt && mmkSellFromShop > 0) {
                    mmkSellFromShop
                } else if (isThbReceipt && mmkBuyFromShop > 0) {
                    mmkBuyFromShop
                } else {
                    1.0 / activeThbToMmk
                }
                val bahtAmountStr = if (order.exchangeRate > 0) {
                    val formatted = String.format(Locale.US, "%.6f", com.example.data.toMmkToThbRate(order.exchangeRate))
                    formatted.dropLastWhile { it == '0' }.let {
                        if (it.endsWith(".")) it.dropLast(1) else it
                    }
                } else if (!isThbReceipt && shopProfile.mmkSellingRate.isNotBlank()) {
                    shopProfile.mmkSellingRate
                } else if (isThbReceipt && shopProfile.mmkBuyingRate.isNotBlank()) {
                    shopProfile.mmkBuyingRate
                } else if (bahtAmount > 0) {
                    val formatted = String.format(Locale.US, "%.6f", bahtAmount)
                    formatted.dropLastWhile { it == '0' }.let {
                        if (it.endsWith(".")) it.dropLast(1) else it
                    }
                } else {
                    "0"
                }
                val label = "ပြေစာငွေလဲနှုန်း (၁ ကျပ်လျှင်) :"
                y = drawKeyValueRow(canvas, label, "$bahtAmountStr ဘတ်", leftMargin, rightMargin, y, paintText, smallLineHeight)
            }
        }

        // Payment Info
        val effTendered = if (isThbReceipt) order.getEffectiveTenderedThb() else order.getEffectiveTenderedMmk()
        val effChange = if (isThbReceipt) order.getEffectiveChangeThb() else order.getEffectiveChangeMmk()
        paintText.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paintText.textSize = bodySize
        y = drawKeyValueRow(
            canvas,
            "ငွေပေးချေမှု :",
            "${order.paymentMethod.labelMm} (${if (isThbReceipt) "ဘတ်" else "ကျပ်"})",
            leftMargin,
            rightMargin,
            y,
            paintText,
            bodyLineHeight
        )

        if (order.paymentMethod == PaymentMethod.CASH) {
            val tenderedVal = if (isThbReceipt) PosViewModel.formatCurrency(effTendered, "THB") else PosViewModel.formatKyatMm(effTendered)
            val changeVal = if (isThbReceipt) PosViewModel.formatCurrency(effChange, "THB") else PosViewModel.formatKyatMm(effChange)
            y = drawKeyValueRow(canvas, "လက်ခံရရှိငွေ :", tenderedVal, leftMargin, rightMargin, y, paintText, bodyLineHeight)
            y = drawKeyValueRow(canvas, "ပြန်အမ်းငွေ :", changeVal, leftMargin, rightMargin, y, paintText, bodyLineHeight)
        }

        // Footer (Always wrapped and printed in full)
        y += 6f * scale
        canvas.drawLine(leftMargin, y, rightMargin, y, paintLine)
        y += bodyLineHeight

        paintText.textSize = smallSize
        paintText.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        val footerText = if (shopProfile.receiptFooter.isNotBlank()) {
            shopProfile.receiptFooter
        } else {
            "ဝယ်ယူအားပေးမှုကို ကျေးဇူးတင်ပါသည်"
        }
        drawCenteredWrappedText(canvas, footerText, centerX, y, contentWidth, paintText, smallLineHeight)
    }
}

