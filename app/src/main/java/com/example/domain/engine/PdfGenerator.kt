package com.example.domain.engine

import android.content.Context
import android.graphics.*
import android.graphics.pdf.PdfDocument
import com.example.data.model.BrandKitEntity
import com.example.data.model.ClientEntity
import com.example.data.model.CompanyEntity
import com.example.data.model.ProjectEntity
import com.example.data.model.QuotationEntity
import com.example.util.ImageUtils
import org.json.JSONArray
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max

data class PdfTemplateConfig(
    val id: String,
    val name: String,
    val styleCode: String, // MINIMAL_STUDIO, PROFESSIONAL_CORPORATE, PREMIUM_ARCHITECTURAL
    val primaryColor: Int,
    val secondaryColor: Int,
    val headerBgColor: Int,
    val tableHeaderBgColor: Int,
    val tableHeaderTextColor: Int,
    val alternatingRowColor: Int,
    val borderColor: Int
)

object PdfPresets {
    // 3 Dedicated Professional Styles
    fun resolveConfig(
        styleCode: String,
        customAccentHex: String?
    ): PdfTemplateConfig {
        val accentColor = try {
            if (!customAccentHex.isNullOrBlank()) Color.parseColor(customAccentHex) else Color.rgb(163, 131, 76)
        } catch (e: Exception) {
            Color.rgb(163, 131, 76)
        }

        return when (styleCode.uppercase()) {
            "MINIMAL_STUDIO" -> PdfTemplateConfig(
                id = "style_minimal",
                name = "Minimal Studio (Architecture)",
                styleCode = "MINIMAL_STUDIO",
                primaryColor = accentColor,
                secondaryColor = Color.rgb(71, 85, 105),
                headerBgColor = Color.WHITE,
                tableHeaderBgColor = Color.rgb(250, 250, 250),
                tableHeaderTextColor = Color.rgb(24, 24, 27),
                alternatingRowColor = Color.WHITE,
                borderColor = Color.rgb(228, 228, 231)
            )
            "PROFESSIONAL_CORPORATE" -> PdfTemplateConfig(
                id = "style_corporate",
                name = "Professional Corporate (Contractors)",
                styleCode = "PROFESSIONAL_CORPORATE",
                primaryColor = accentColor,
                secondaryColor = Color.rgb(51, 65, 85),
                headerBgColor = Color.rgb(248, 250, 252),
                tableHeaderBgColor = accentColor,
                tableHeaderTextColor = Color.WHITE,
                alternatingRowColor = Color.rgb(248, 250, 252),
                borderColor = Color.rgb(203, 213, 225)
            )
            else -> PdfTemplateConfig( // PREMIUM_ARCHITECTURAL
                id = "style_architectural",
                name = "Premium Architectural Proposal",
                styleCode = "PREMIUM_ARCHITECTURAL",
                primaryColor = accentColor,
                secondaryColor = Color.rgb(31, 31, 31),
                headerBgColor = Color.rgb(253, 252, 248),
                tableHeaderBgColor = Color.rgb(30, 41, 59),
                tableHeaderTextColor = Color.WHITE,
                alternatingRowColor = Color.rgb(255, 255, 255),
                borderColor = Color.rgb(226, 232, 240)
            )
        }
    }
}

object PdfGenerator {

    // Standard A4 dimensions in PostScript points: 595 x 842 pt
    private const val PAGE_WIDTH = 595
    private const val PAGE_HEIGHT = 842
    private const val MARGIN_LEFT = 36f
    private const val MARGIN_RIGHT = 36f
    private const val MARGIN_TOP = 36f
    private const val MARGIN_BOTTOM = 40f
    private const val CONTENT_WIDTH = PAGE_WIDTH - MARGIN_LEFT - MARGIN_RIGHT

    /**
     * Requirement 52: Automatically generates sanitized filename
     * Format: [QuotationNumber]_[Revision]_[ClientName].pdf
     * Example: QT-2026-001_Rev01_Suresh-Babu.pdf
     */
    fun getSanitizedFileName(quotationNumber: String, revision: Int, clientName: String?): String {
        val revStr = if (revision == 0) "Rev00" else "Rev" + String.format(Locale.US, "%02d", revision)
        val safeNumber = quotationNumber.replace(Regex("[^a-zA-Z0-9.-]"), "-")
        val safeClient = (clientName ?: "Client").trim().replace(Regex("[^a-zA-Z0-9.-]"), "-").take(25)
        return "${safeNumber}_${revStr}_${safeClient}.pdf"
    }

    fun generateQuotationPdf(
        context: Context,
        quotation: QuotationEntity,
        calculation: QuotationCalculationResult,
        company: CompanyEntity?,
        client: ClientEntity?,
        project: ProjectEntity?,
        brandKit: BrandKitEntity? = null
    ): File {
        val template = PdfPresets.resolveConfig(quotation.pdfStyle, quotation.templatePrimaryColor)
        val pdfDocument = PdfDocument()

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(30, 41, 59)
            textSize = 9f
        }

        val boldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(15, 23, 42)
            textSize = 9f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        val primaryPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = template.primaryColor
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = template.borderColor
            style = Paint.Style.STROKE
            strokeWidth = 0.8f
        }

        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
        }

        // Layout measurements: Column widths
        // S.No(24), Description & Spec(252), Qty(40), Unit(44), Rate(75), Amount(88) = 523 pt (fits CONTENT_WIDTH exactly)
        val colW = floatArrayOf(24f, 252f, 40f, 44f, 75f, 88f)
        val colX = FloatArray(colW.size + 1)
        colX[0] = MARGIN_LEFT
        for (i in colW.indices) {
            colX[i + 1] = colX[i] + colW[i]
        }

        // Watermark setup: Text or Logo
        val isLogoWatermark = quotation.watermarkType.equals("LOGO", ignoreCase = true) ||
                brandKit?.watermarkType?.equals("LOGO", ignoreCase = true) == true
        val watermarkLogoPath = brandKit?.logoUri ?: company?.logoUri ?: brandKit?.secondaryLogoUri
        val watermarkLogoBitmap = if (isLogoWatermark && watermarkLogoPath != null) {
            ImageUtils.loadBitmap(context, watermarkLogoPath)
        } else null

        val shouldDrawWatermark = quotation.watermarkEnabled ||
                brandKit?.watermarkEnabled == true ||
                quotation.status.equals("Draft", ignoreCase = true) ||
                quotation.watermarkText.isNotBlank() ||
                (isLogoWatermark && watermarkLogoBitmap != null)

        val watermarkText = if (quotation.watermarkText.isNotBlank()) {
            quotation.watermarkText
        } else if (brandKit?.watermarkText?.isNotBlank() == true) {
            brandKit.watermarkText
        } else if (quotation.status.equals("Draft", ignoreCase = true)) {
            "DRAFT"
        } else {
            "CONFIDENTIAL"
        }

        val opacity = if (quotation.watermarkOpacity > 0f) quotation.watermarkOpacity else (brandKit?.watermarkOpacity ?: 0.12f)

        fun renderWatermark(targetCanvas: Canvas) {
            if (!shouldDrawWatermark) return
            if (isLogoWatermark && watermarkLogoBitmap != null) {
                drawLogoWatermark(targetCanvas, watermarkLogoBitmap, opacity)
            } else {
                drawWatermark(targetCanvas, watermarkText, opacity)
            }
        }

        // Pages prepared dynamically
        var pageNumber = 1
        var pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
        var currentPage = pdfDocument.startPage(pageInfo)
        var canvas = currentPage.canvas

        renderWatermark(canvas)

        // Company header logo
        val headerLogoPath = company?.logoUri ?: brandKit?.logoUri
        val headerLogoBitmap = headerLogoPath?.let { ImageUtils.loadBitmap(context, it) }

        // Header on First Page
        var currentY = drawHeader(
            canvas, quotation, company, client, project, template,
            headerLogoBitmap, primaryPaint, boldPaint, textPaint, fillPaint, borderPaint
        )

        // Draw Table Header
        currentY = drawTableHeader(canvas, currentY, colX, template, boldPaint, fillPaint, borderPaint)

        // Items grouped by Room and Category
        var sNo = 1
        var lastRoom = ""
        val maxTableBottom = PAGE_HEIGHT - MARGIN_BOTTOM - 20f
        val maxColDescWidth = colW[1] - 12f

        calculation.items.forEachIndexed { index, item ->
            val roomName = if (item.roomName.isBlank()) "General Works" else item.roomName
            val isNewRoom = roomName != lastRoom

            boldPaint.textSize = 8.5f
            val itemNameLines = wrapText(item.itemName, maxColDescWidth, boldPaint)

            textPaint.textSize = 7.5f
            val descLines = if (item.description.isNotBlank()) wrapText(item.description, maxColDescWidth, textPaint) else emptyList()
            val specLines = if (item.specification.isNotBlank()) wrapText("Spec: ${item.specification}", maxColDescWidth, textPaint) else emptyList()

            val textTotalHeight = (itemNameLines.size * 10.5f) + (descLines.size * 9.5f) + (specLines.size * 9.5f)
            val rowHeight = max(26f, textTotalHeight + 10f)
            val neededHeight = rowHeight + if (isNewRoom) 22f else 0f

            // Page break check
            if (currentY + neededHeight > maxTableBottom) {
                drawPageFooter(canvas, pageNumber)
                pdfDocument.finishPage(currentPage)

                pageNumber++
                pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
                currentPage = pdfDocument.startPage(pageInfo)
                canvas = currentPage.canvas

                renderWatermark(canvas)

                currentY = MARGIN_TOP + 10f
                boldPaint.textSize = 8f
                boldPaint.color = Color.GRAY
                canvas.drawText("${quotation.quotationNumber} — Page $pageNumber (Continued)", MARGIN_LEFT, currentY, boldPaint)
                currentY += 12f
                currentY = drawTableHeader(canvas, currentY, colX, template, boldPaint, fillPaint, borderPaint)
            }

            // Room / Section Header
            if (isNewRoom) {
                lastRoom = roomName
                fillPaint.color = template.headerBgColor
                canvas.drawRect(MARGIN_LEFT, currentY, MARGIN_LEFT + CONTENT_WIDTH, currentY + 18f, fillPaint)
                canvas.drawRect(MARGIN_LEFT, currentY, MARGIN_LEFT + CONTENT_WIDTH, currentY + 18f, borderPaint)

                // Accent tick
                fillPaint.color = template.primaryColor
                canvas.drawRect(MARGIN_LEFT, currentY, MARGIN_LEFT + 3.5f, currentY + 18f, fillPaint)

                boldPaint.textSize = 8.5f
                boldPaint.color = template.primaryColor
                canvas.drawText("ROOM / AREA: ${roomName.uppercase()}", MARGIN_LEFT + 10f, currentY + 12.5f, boldPaint)
                currentY += 18f
            }

            // Alternating Row Background
            if (index % 2 == 1 && template.alternatingRowColor != Color.WHITE) {
                fillPaint.color = template.alternatingRowColor
                canvas.drawRect(MARGIN_LEFT, currentY, MARGIN_LEFT + CONTENT_WIDTH, currentY + rowHeight, fillPaint)
            }

            // Draw Row content with cell clipping
            var textY = currentY + 12f

            // S.No
            textPaint.textSize = 8f
            textPaint.color = Color.rgb(100, 116, 139)
            canvas.drawText(sNo.toString(), colX[0] + 5f, textY, textPaint)

            // Description, Specifications
            canvas.save()
            canvas.clipRect(colX[1] + 4f, currentY, colX[2] - 4f, currentY + rowHeight)
            boldPaint.textSize = 8.5f
            boldPaint.color = Color.rgb(15, 23, 42)
            for (line in itemNameLines) {
                canvas.drawText(line, colX[1] + 4f, textY, boldPaint)
                textY += 10.5f
            }

            textPaint.textSize = 7.5f
            textPaint.color = Color.rgb(71, 85, 105)
            for (line in descLines) {
                canvas.drawText(line, colX[1] + 4f, textY, textPaint)
                textY += 9.5f
            }

            if (specLines.isNotEmpty()) {
                textPaint.color = template.primaryColor
                for (line in specLines) {
                    canvas.drawText(line, colX[1] + 4f, textY, textPaint)
                    textY += 9.5f
                }
            }
            canvas.restore()

            // Qty
            canvas.save()
            canvas.clipRect(colX[2] + 2f, currentY, colX[3] - 2f, currentY + rowHeight)
            textPaint.color = Color.rgb(15, 23, 42)
            val qtyStr = if (item.quantity % 1.0 == 0.0) item.quantity.toInt().toString() else String.format(Locale.US, "%.2f", item.quantity)
            canvas.drawText(qtyStr, colX[2] + 4f, currentY + 13f, textPaint)
            canvas.restore()

            // Unit
            canvas.save()
            canvas.clipRect(colX[3] + 2f, currentY, colX[4] - 2f, currentY + rowHeight)
            textPaint.color = Color.rgb(100, 116, 139)
            canvas.drawText(item.unit, colX[3] + 4f, currentY + 13f, textPaint)
            canvas.restore()

            // Rate
            canvas.save()
            canvas.clipRect(colX[4] + 2f, currentY, colX[5] - 2f, currentY + rowHeight)
            textPaint.color = Color.rgb(15, 23, 42)
            canvas.drawText(CurrencyFormatter.formatInr(item.effectiveRate, false), colX[4] + 4f, currentY + 13f, textPaint)
            canvas.restore()

            // Amount
            canvas.save()
            canvas.clipRect(colX[5] + 2f, currentY, colX[6] - 2f, currentY + rowHeight)
            boldPaint.textSize = 8.5f
            boldPaint.color = Color.rgb(15, 23, 42)
            canvas.drawText(CurrencyFormatter.formatInr(item.netAmount, false), colX[5] + 4f, currentY + 13f, boldPaint)
            canvas.restore()

            // Outer cell border lines
            canvas.drawLine(MARGIN_LEFT, currentY + rowHeight, MARGIN_LEFT + CONTENT_WIDTH, currentY + rowHeight, borderPaint)
            for (cx in colX) {
                canvas.drawLine(cx, currentY, cx, currentY + rowHeight, borderPaint)
            }

            currentY += rowHeight
            sNo++
        }

        // Check if totals & footer fit on this page, else create final summary page
        val summaryNeededHeight = 230f
        if (currentY + summaryNeededHeight > PAGE_HEIGHT - MARGIN_BOTTOM) {
            drawPageFooter(canvas, pageNumber)
            pdfDocument.finishPage(currentPage)

            pageNumber++
            pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
            currentPage = pdfDocument.startPage(pageInfo)
            canvas = currentPage.canvas

            if (shouldDrawWatermark) {
                drawWatermark(canvas, watermarkText, quotation.watermarkOpacity)
            }

            currentY = MARGIN_TOP + 10f
            boldPaint.textSize = 8f
            boldPaint.color = Color.GRAY
            canvas.drawText("${quotation.quotationNumber} — Summary & Approval", MARGIN_LEFT, currentY, boldPaint)
            currentY += 20f
        }

        // Draw Financial Totals Box (Right Aligned)
        currentY += 14f
        val summaryBoxWidth = 220f
        val summaryLeft = MARGIN_LEFT + CONTENT_WIDTH - summaryBoxWidth
        var sumY = currentY

        fillPaint.color = template.headerBgColor
        canvas.drawRoundRect(RectF(summaryLeft, sumY, summaryLeft + summaryBoxWidth, sumY + 95f), 4f, 4f, fillPaint)
        canvas.drawRoundRect(RectF(summaryLeft, sumY, summaryLeft + summaryBoxWidth, sumY + 95f), 4f, 4f, borderPaint)

        sumY += 15f
        textPaint.textSize = 8.5f
        textPaint.color = Color.rgb(71, 85, 105)
        canvas.drawText("Subtotal:", summaryLeft + 10f, sumY, textPaint)
        boldPaint.textSize = 8.5f
        boldPaint.color = Color.BLACK
        canvas.drawText(CurrencyFormatter.formatInr(calculation.subtotal), summaryLeft + summaryBoxWidth - 85f, sumY, boldPaint)

        if (calculation.discountAmount > 0) {
            sumY += 13f
            textPaint.color = Color.rgb(185, 28, 28)
            canvas.drawText("Discount (${quotation.discountValue}%):", summaryLeft + 10f, sumY, textPaint)
            canvas.drawText("- " + CurrencyFormatter.formatInr(calculation.discountAmount), summaryLeft + summaryBoxWidth - 85f, sumY, textPaint)
        }

        sumY += 13f
        textPaint.color = Color.rgb(71, 85, 105)
        val taxLabel = if (quotation.gstType == "IGST") "IGST (${quotation.taxRate}%):" else "GST (CGST+SGST ${quotation.taxRate}%):"
        canvas.drawText(taxLabel, summaryLeft + 10f, sumY, textPaint)
        canvas.drawText(CurrencyFormatter.formatInr(calculation.totalGstAmount), summaryLeft + summaryBoxWidth - 85f, sumY, boldPaint)

        // Grand Total Row with accent highlight
        sumY += 15f
        fillPaint.color = template.primaryColor
        canvas.drawRect(summaryLeft, sumY - 10f, summaryLeft + summaryBoxWidth, sumY + 16f, fillPaint)
        boldPaint.textSize = 9.5f
        boldPaint.color = Color.WHITE
        canvas.drawText("GRAND TOTAL:", summaryLeft + 10f, sumY + 5f, boldPaint)
        canvas.drawText(CurrencyFormatter.formatInr(calculation.grandTotal), summaryLeft + summaryBoxWidth - 95f, sumY + 5f, boldPaint)

        // Left Column: Amount in Words & Bank Details & Payment Milestones
        var leftY = currentY + 10f
        boldPaint.textSize = 8f
        boldPaint.color = template.primaryColor
        canvas.drawText("AMOUNT IN WORDS:", MARGIN_LEFT, leftY, boldPaint)
        leftY += 11f
        textPaint.textSize = 8f
        textPaint.color = Color.rgb(30, 41, 59)
        canvas.drawText(CurrencyFormatter.amountToWords(calculation.grandTotal), MARGIN_LEFT, leftY, textPaint)
        leftY += 15f

        // Payment Milestones (if configured)
        if (quotation.paymentMilestonesJson.isNotBlank()) {
            try {
                val milestones = JSONArray(quotation.paymentMilestonesJson)
                if (milestones.length() > 0) {
                    boldPaint.textSize = 8f
                    boldPaint.color = template.primaryColor
                    canvas.drawText("PAYMENT SCHEDULE:", MARGIN_LEFT, leftY, boldPaint)
                    leftY += 10f
                    for (i in 0 until milestones.length()) {
                        val m = milestones.getJSONObject(i)
                        val mName = m.optString("name", "Stage ${i + 1}")
                        val mPct = m.optDouble("percentage", 0.0)
                        val mAmt = calculation.grandTotal * (mPct / 100.0)
                        textPaint.textSize = 7.5f
                        canvas.drawText("• $mName (${mPct.toInt()}%): ${CurrencyFormatter.formatInr(mAmt)}", MARGIN_LEFT + 4f, leftY, textPaint)
                        leftY += 9f
                    }
                    leftY += 5f
                }
            } catch (e: Exception) {
                // Skip if JSON malformed
            }
        }

        // Bank Details
        if (company?.bankName?.isNotBlank() == true) {
            boldPaint.textSize = 8f
            boldPaint.color = template.primaryColor
            canvas.drawText("BANK DETAILS FOR NEFT/RTGS/UPI:", MARGIN_LEFT, leftY, boldPaint)
            leftY += 11f
            textPaint.textSize = 7.5f
            canvas.drawText("Bank: ${company.bankName}  |  A/C No: ${company.accountNumber}", MARGIN_LEFT, leftY, textPaint)
            leftY += 9.5f
            canvas.drawText("IFSC Code: ${company.ifsc}  |  UPI ID: ${company.upiId}", MARGIN_LEFT, leftY, textPaint)
            leftY += 12f
        }

        currentY = max(sumY + 34f, leftY + 15f)

        // Terms & Conditions section
        val termsText = if (quotation.termsAndConditions.isNotBlank()) quotation.termsAndConditions else "1. Quotation valid for ${quotation.validityDays} days.\n2. Work will commence upon advance payment.\n3. Prevailing GST rates applicable."
        boldPaint.textSize = 8.5f
        boldPaint.color = template.primaryColor
        canvas.drawText("TERMS & CONDITIONS:", MARGIN_LEFT, currentY, boldPaint)
        currentY += 11f
        textPaint.textSize = 7.5f
        textPaint.color = Color.rgb(71, 85, 105)
        for (termLine in termsText.lines().take(4)) {
            canvas.drawText(termLine, MARGIN_LEFT, currentY, textPaint)
            currentY += 9.5f
        }

        // Authorized Signature Block (Right Aligned)
        currentY += 15f
        val sigX = colX.last() - 170f
        canvas.drawLine(sigX, currentY + 30f, colX.last(), currentY + 30f, borderPaint)
        boldPaint.textSize = 8.5f
        boldPaint.color = Color.BLACK
        canvas.drawText(company?.signatureName ?: "Authorized Signatory", sigX + 10f, currentY + 42f, boldPaint)
        textPaint.textSize = 7.5f
        textPaint.color = Color.GRAY
        canvas.drawText(company?.name ?: "Knot Architects", sigX + 10f, currentY + 52f, textPaint)

        // Final page footer
        drawPageFooter(canvas, pageNumber)
        pdfDocument.finishPage(currentPage)

        // Output file with strict SaaS naming format: [QuotationNumber]_[Revision]_[ClientName].pdf
        val fileName = getSanitizedFileName(quotation.quotationNumber, quotation.revision, client?.name)
        val pdfFile = File(context.cacheDir, fileName)
        val outputStream = FileOutputStream(pdfFile)
        pdfDocument.writeTo(outputStream)
        outputStream.close()
        pdfDocument.close()

        return pdfFile
    }

    private fun drawLogoWatermark(canvas: Canvas, logoBitmap: Bitmap, opacity: Float) {
        val watermarkPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            alpha = (255 * opacity.coerceIn(0.04f, 0.40f)).toInt()
        }
        val targetWidth = 220f
        val aspectRatio = logoBitmap.height.toFloat() / logoBitmap.width.coerceAtLeast(1).toFloat()
        val targetHeight = (targetWidth * aspectRatio).coerceIn(60f, 260f)

        val left = (PAGE_WIDTH - targetWidth) / 2f
        val top = (PAGE_HEIGHT - targetHeight) / 2f

        canvas.save()
        val destRect = RectF(left, top, left + targetWidth, top + targetHeight)
        canvas.drawBitmap(logoBitmap, null, destRect, watermarkPaint)
        canvas.restore()
    }

    private fun drawWatermark(canvas: Canvas, text: String, opacity: Float) {
        val watermarkPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.GRAY
            alpha = (255 * opacity.coerceIn(0.04f, 0.35f)).toInt()
            textSize = 58f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.save()
        canvas.rotate(-40f, PAGE_WIDTH / 2f, PAGE_HEIGHT / 2f)
        canvas.drawText(text.uppercase(), PAGE_WIDTH / 2f, PAGE_HEIGHT / 2f, watermarkPaint)
        canvas.restore()
    }

    private fun drawHeader(
        canvas: Canvas,
        quotation: QuotationEntity,
        company: CompanyEntity?,
        client: ClientEntity?,
        project: ProjectEntity?,
        template: PdfTemplateConfig,
        headerLogoBitmap: Bitmap?,
        primaryPaint: Paint,
        boldPaint: Paint,
        textPaint: Paint,
        fillPaint: Paint,
        borderPaint: Paint
    ): Float {
        var y = MARGIN_TOP

        when (template.styleCode) {
            "MINIMAL_STUDIO" -> {
                // Architectural minimalist header: clean line, subtle typography, company logo
                var nameStartX = MARGIN_LEFT
                if (headerLogoBitmap != null) {
                    val logoSize = 34f
                    val dest = RectF(MARGIN_LEFT, y - 2f, MARGIN_LEFT + logoSize, y - 2f + logoSize)
                    canvas.drawBitmap(headerLogoBitmap, null, dest, null)
                    nameStartX += logoSize + 10f
                }

                primaryPaint.textSize = 18f
                canvas.drawText(company?.name ?: "Knot Architects", nameStartX, y + 16f, primaryPaint)

                boldPaint.textSize = 10f
                boldPaint.color = Color.rgb(100, 116, 139)
                val docTitle = if (quotation.isEstimate) "COST ESTIMATE" else "ARCHITECTURAL PROPOSAL"
                canvas.drawText(docTitle, MARGIN_LEFT + CONTENT_WIDTH - 160f, y + 16f, boldPaint)

                y += 24f
                textPaint.textSize = 8f
                textPaint.color = Color.rgb(71, 85, 105)
                canvas.drawText(company?.address ?: "", nameStartX, y, textPaint)
                canvas.drawText("No: ${quotation.quotationNumber} (Rev ${String.format(Locale.US, "%02d", quotation.revision)})", MARGIN_LEFT + CONTENT_WIDTH - 160f, y, boldPaint)

                y += 12f
                val dateFmt = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(quotation.date))
                canvas.drawText("Date: $dateFmt  |  Validity: ${quotation.validityDays} Days", MARGIN_LEFT + CONTENT_WIDTH - 160f, y, textPaint)

                y += 14f
                canvas.drawLine(MARGIN_LEFT, y, MARGIN_LEFT + CONTENT_WIDTH, y, borderPaint)
                y += 14f
            }
            "PROFESSIONAL_CORPORATE" -> {
                // Prominent top color accent band
                fillPaint.color = template.primaryColor
                canvas.drawRect(MARGIN_LEFT, MARGIN_TOP, MARGIN_LEFT + CONTENT_WIDTH, MARGIN_TOP + 6f, fillPaint)

                y = MARGIN_TOP + 22f
                var nameStartX = MARGIN_LEFT
                if (headerLogoBitmap != null) {
                    val logoSize = 34f
                    val dest = RectF(MARGIN_LEFT, y - 10f, MARGIN_LEFT + logoSize, y - 10f + logoSize)
                    canvas.drawBitmap(headerLogoBitmap, null, dest, null)
                    nameStartX += logoSize + 10f
                }

                primaryPaint.textSize = 17f
                canvas.drawText(company?.name ?: "Contractor Co.", nameStartX, y, primaryPaint)

                val docTypeTitle = if (quotation.isEstimate) "ESTIMATE" else "COMMERCIAL QUOTATION"
                primaryPaint.textSize = 13f
                canvas.drawText(docTypeTitle, MARGIN_LEFT + CONTENT_WIDTH - 160f, y, primaryPaint)

                y += 14f
                textPaint.textSize = 8f
                textPaint.color = Color.rgb(100, 116, 139)
                if (company?.address?.isNotBlank() == true) {
                    canvas.drawText(company.address, nameStartX, y, textPaint)
                }
                boldPaint.textSize = 9.5f
                boldPaint.color = Color.BLACK
                canvas.drawText("No: ${quotation.quotationNumber} Rev.${quotation.revision}", MARGIN_LEFT + CONTENT_WIDTH - 160f, y, boldPaint)

                y += 12f
                val contactLine = "Phone: ${company?.phone ?: ""}  |  Email: ${company?.email ?: ""}"
                canvas.drawText(contactLine, nameStartX, y, textPaint)
                val dateFmt = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(quotation.date))
                canvas.drawText("Date: $dateFmt", MARGIN_LEFT + CONTENT_WIDTH - 160f, y, textPaint)

                y += 16f
                canvas.drawLine(MARGIN_LEFT, y, MARGIN_LEFT + CONTENT_WIDTH, y, borderPaint)
                y += 14f
            }
            else -> { // PREMIUM_ARCHITECTURAL
                // Editorial architectural proposal aesthetic
                fillPaint.color = template.primaryColor
                canvas.drawRect(MARGIN_LEFT, MARGIN_TOP, MARGIN_LEFT + 4f, MARGIN_TOP + 32f, fillPaint)

                var nameStartX = MARGIN_LEFT + 12f
                if (headerLogoBitmap != null) {
                    val logoSize = 32f
                    val dest = RectF(nameStartX, MARGIN_TOP, nameStartX + logoSize, MARGIN_TOP + logoSize)
                    canvas.drawBitmap(headerLogoBitmap, null, dest, null)
                    nameStartX += logoSize + 10f
                }

                primaryPaint.textSize = 19f
                canvas.drawText(company?.name ?: "Knot Architects & Interiors", nameStartX, MARGIN_TOP + 18f, primaryPaint)

                textPaint.textSize = 8f
                textPaint.color = Color.rgb(100, 116, 139)
                canvas.drawText("ARCHITECTURAL ESTIMATION & PROPOSAL", nameStartX, MARGIN_TOP + 30f, textPaint)

                boldPaint.textSize = 10f
                boldPaint.color = Color.BLACK
                canvas.drawText("REF: ${quotation.quotationNumber}", MARGIN_LEFT + CONTENT_WIDTH - 150f, MARGIN_TOP + 16f, boldPaint)

                val dateFmt = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(quotation.date))
                canvas.drawText("Date: $dateFmt  |  Rev: ${String.format(Locale.US, "%02d", quotation.revision)}", MARGIN_LEFT + CONTENT_WIDTH - 150f, MARGIN_TOP + 28f, textPaint)

                y = MARGIN_TOP + 46f
                canvas.drawLine(MARGIN_LEFT, y, MARGIN_LEFT + CONTENT_WIDTH, y, borderPaint)
                y += 14f
            }
        }

        // Two Cards: Left = Bill To (Client), Right = Project Details
        val cardWidth = (CONTENT_WIDTH - 12f) / 2f
        val cardHeight = 72f

        // Left Card: Client
        fillPaint.color = template.headerBgColor
        canvas.drawRoundRect(RectF(MARGIN_LEFT, y, MARGIN_LEFT + cardWidth, y + cardHeight), 4f, 4f, fillPaint)
        canvas.drawRoundRect(RectF(MARGIN_LEFT, y, MARGIN_LEFT + cardWidth, y + cardHeight), 4f, 4f, borderPaint)

        boldPaint.textSize = 8f
        boldPaint.color = template.primaryColor
        canvas.drawText("PREPARED FOR (CLIENT):", MARGIN_LEFT + 8f, y + 14f, boldPaint)
        boldPaint.textSize = 9.5f
        boldPaint.color = Color.BLACK
        canvas.drawText(client?.name ?: "Valued Client", MARGIN_LEFT + 8f, y + 27f, boldPaint)
        textPaint.textSize = 7.5f
        textPaint.color = Color.rgb(71, 85, 105)
        if (client?.companyName?.isNotBlank() == true) {
            canvas.drawText(client.companyName, MARGIN_LEFT + 8f, y + 38f, textPaint)
        }
        val clientContact = "Phone: ${client?.mobile ?: "-"}  |  Email: ${client?.email ?: "-"}"
        canvas.drawText(clientContact, MARGIN_LEFT + 8f, y + 49f, textPaint)
        if (client?.gstin?.isNotBlank() == true) {
            canvas.drawText("GSTIN: ${client.gstin}", MARGIN_LEFT + 8f, y + 60f, textPaint)
        }

        // Right Card: Project Details
        val rightX = MARGIN_LEFT + cardWidth + 12f
        canvas.drawRoundRect(RectF(rightX, y, rightX + cardWidth, y + cardHeight), 4f, 4f, fillPaint)
        canvas.drawRoundRect(RectF(rightX, y, rightX + cardWidth, y + cardHeight), 4f, 4f, borderPaint)

        boldPaint.color = template.primaryColor
        canvas.drawText("PROJECT & SITE DETAILS:", rightX + 8f, y + 14f, boldPaint)
        boldPaint.color = Color.BLACK
        canvas.drawText(project?.name ?: "Project Site Works", rightX + 8f, y + 27f, boldPaint)
        textPaint.color = Color.rgb(71, 85, 105)
        if (project?.address?.isNotBlank() == true) {
            canvas.drawText(project.address, rightX + 8f, y + 38f, textPaint)
        }
        val metaLine = "Type: ${project?.projectType ?: "General"}  |  Area: ${project?.area ?: "-"}"
        canvas.drawText(metaLine, rightX + 8f, y + 49f, textPaint)
        val prepBy = "Prepared By: ${quotation.createdByName.ifBlank { "Rajesh (Knot Architects)" }}"
        canvas.drawText(prepBy, rightX + 8f, y + 60f, textPaint)

        return y + cardHeight + 16f
    }

    private fun drawTableHeader(
        canvas: Canvas,
        y: Float,
        colX: FloatArray,
        template: PdfTemplateConfig,
        boldPaint: Paint,
        fillPaint: Paint,
        borderPaint: Paint
    ): Float {
        val h = 20f
        fillPaint.color = template.tableHeaderBgColor
        canvas.drawRect(MARGIN_LEFT, y, MARGIN_LEFT + CONTENT_WIDTH, y + h, fillPaint)
        canvas.drawRect(MARGIN_LEFT, y, MARGIN_LEFT + CONTENT_WIDTH, y + h, borderPaint)

        boldPaint.textSize = 8f
        boldPaint.color = template.tableHeaderTextColor

        val headers = arrayOf("#", "DESCRIPTION & SPECIFICATIONS", "QTY", "UNIT", "RATE (₹)", "AMOUNT (₹)")
        for (i in headers.indices) {
            canvas.drawText(headers[i], colX[i] + 4f, y + 13.5f, boldPaint)
            if (i > 0) {
                canvas.drawLine(colX[i], y, colX[i], y + h, borderPaint)
            }
        }

        return y + h
    }

    private fun drawPageFooter(canvas: Canvas, pageNumber: Int) {
        val footerY = PAGE_HEIGHT - MARGIN_BOTTOM + 16f
        val linePaint = Paint().apply {
            color = Color.rgb(226, 232, 240)
            strokeWidth = 0.8f
        }
        canvas.drawLine(MARGIN_LEFT, footerY - 10f, MARGIN_LEFT + CONTENT_WIDTH, footerY - 10f, linePaint)

        val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(148, 163, 184)
            textSize = 7.5f
        }
        canvas.drawText("Generated via QuoteForge SaaS • Confidential Quotation", MARGIN_LEFT, footerY, footerPaint)

        val pageStr = "Page $pageNumber"
        val pageW = footerPaint.measureText(pageStr)
        canvas.drawText(pageStr, MARGIN_LEFT + CONTENT_WIDTH - pageW, footerY, footerPaint)
    }

    private fun wrapText(text: String, maxWidth: Float, paint: Paint): List<String> {
        if (text.isBlank()) return emptyList()
        val lines = mutableListOf<String>()
        val paragraphs = text.split("\n")

        for (para in paragraphs) {
            val words = para.split(" ")
            var currentLine = StringBuilder()

            for (word in words) {
                val candidate = if (currentLine.isEmpty()) word else "$currentLine $word"
                if (paint.measureText(candidate) <= maxWidth) {
                    currentLine = StringBuilder(candidate)
                } else {
                    if (currentLine.isNotEmpty()) {
                        lines.add(currentLine.toString())
                        currentLine = StringBuilder()
                    }
                    if (paint.measureText(word) <= maxWidth) {
                        currentLine = StringBuilder(word)
                    } else {
                        // Break word manually by characters
                        for (ch in word) {
                            if (paint.measureText("$currentLine$ch") <= maxWidth) {
                                currentLine.append(ch)
                            } else {
                                if (currentLine.isNotEmpty()) lines.add(currentLine.toString())
                                currentLine = StringBuilder(ch.toString())
                            }
                        }
                    }
                }
            }
            if (currentLine.isNotEmpty()) {
                lines.add(currentLine.toString())
            }
        }
        return lines
    }
}
