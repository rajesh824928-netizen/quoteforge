package com.example.domain.engine

import kotlin.math.max
import kotlin.math.round

enum class CalculationMode {
    SELLING_PRICE,
    COST_PLUS_MARGIN
}

enum class DiscountType {
    PERCENTAGE,
    FIXED
}

enum class GstType {
    CGST_SGST,
    IGST,
    NONE
}

data class CalculatedItem(
    val id: Long = 0,
    val roomName: String = "",
    val category: String = "",
    val itemName: String,
    val description: String = "",
    val specification: String = "",
    val unit: String = "nos",
    val quantity: Double = 1.0,
    val rate: Double = 0.0,
    val discountType: DiscountType = DiscountType.PERCENTAGE,
    val discountValue: Double = 0.0,
    val taxRate: Double = 0.0, // Item specific tax if applicable
    val materialCost: Double = 0.0,
    val labourCost: Double = 0.0,
    val installationCost: Double = 0.0,
    val overheadCost: Double = 0.0,
    val sortOrder: Int = 0,

    // Computed values
    val effectiveRate: Double = 0.0,
    val grossAmount: Double = 0.0,
    val discountAmount: Double = 0.0,
    val netAmount: Double = 0.0,
    val totalCost: Double = 0.0,
    val itemProfit: Double = 0.0
)

data class QuotationCalculationResult(
    val items: List<CalculatedItem>,
    val subtotal: Double,
    val discountAmount: Double,
    val taxableAmount: Double,
    val gstRate: Double,
    val isGstInclusive: Boolean,
    val gstType: GstType,
    val cgstAmount: Double,
    val sgstAmount: Double,
    val igstAmount: Double,
    val totalGstAmount: Double,
    val grandTotal: Double,
    val totalCost: Double,
    val totalProfit: Double,
    val profitMarginPercentage: Double,
    val roomTotals: Map<String, Double>,
    val categoryTotals: Map<String, Double>
)

object CalculationEngine {

    /**
     * Calculates an individual item based on mode and values
     */
    fun calculateItem(
        quantity: Double,
        rate: Double,
        discountType: DiscountType = DiscountType.PERCENTAGE,
        discountValue: Double = 0.0,
        taxRate: Double = 0.0,
        calculationMode: CalculationMode = CalculationMode.SELLING_PRICE,
        profitMarginPercent: Double = 20.0,
        materialCost: Double = 0.0,
        labourCost: Double = 0.0,
        installationCost: Double = 0.0,
        overheadCost: Double = 0.0
    ): ItemCalculationSummary {
        val safeQty = max(0.0, quantity)
        val unitCost = max(0.0, materialCost + labourCost + installationCost + overheadCost)

        val effectiveRate = when (calculationMode) {
            CalculationMode.COST_PLUS_MARGIN -> {
                val margin = max(0.0, profitMarginPercent) / 100.0
                unitCost * (1.0 + margin)
            }
            CalculationMode.SELLING_PRICE -> {
                max(0.0, rate)
            }
        }

        val grossAmount = safeQty * effectiveRate
        val discount = when (discountType) {
            DiscountType.PERCENTAGE -> {
                val pct = max(0.0, discountValue) / 100.0
                grossAmount * pct
            }
            DiscountType.FIXED -> {
                max(0.0, discountValue)
            }
        }

        val discountAmount = discount.coerceIn(0.0, grossAmount)
        val netAmount = max(0.0, grossAmount - discountAmount)
        val totalCost = safeQty * unitCost
        val itemProfit = netAmount - totalCost

        return ItemCalculationSummary(
            effectiveRate = roundTwoDecimals(effectiveRate),
            grossAmount = roundTwoDecimals(grossAmount),
            discountAmount = roundTwoDecimals(discountAmount),
            netAmount = roundTwoDecimals(netAmount),
            totalCost = roundTwoDecimals(totalCost),
            itemProfit = roundTwoDecimals(itemProfit)
        )
    }

    /**
     * Calculates the entire quotation with tax rules, discounts, and room/category aggregations
     */
    fun calculateQuotation(
        items: List<CalculatedItem>,
        overallDiscountType: DiscountType = DiscountType.PERCENTAGE,
        overallDiscountValue: Double = 0.0,
        gstRate: Double = 18.0,
        isGstInclusive: Boolean = false,
        gstType: GstType = GstType.CGST_SGST
    ): QuotationCalculationResult {
        var subtotal = 0.0
        var totalCost = 0.0
        val roomTotals = mutableMapOf<String, Double>()
        val categoryTotals = mutableMapOf<String, Double>()

        items.forEach { item ->
            subtotal += item.netAmount
            totalCost += item.totalCost

            val roomKey = if (item.roomName.isBlank()) "General" else item.roomName
            val catKey = if (item.category.isBlank()) "Standard" else item.category

            roomTotals[roomKey] = (roomTotals[roomKey] ?: 0.0) + item.netAmount
            categoryTotals[catKey] = (categoryTotals[catKey] ?: 0.0) + item.netAmount
        }

        val overallDiscount = when (overallDiscountType) {
            DiscountType.PERCENTAGE -> {
                val pct = max(0.0, overallDiscountValue) / 100.0
                subtotal * pct
            }
            DiscountType.FIXED -> {
                max(0.0, overallDiscountValue)
            }
        }.coerceIn(0.0, subtotal)

        val netAfterDiscount = max(0.0, subtotal - overallDiscount)

        val safeTaxRate = max(0.0, gstRate)
        val (taxableAmount, totalGstAmount, grandTotal) = if (gstType == GstType.NONE || safeTaxRate == 0.0) {
            Triple(netAfterDiscount, 0.0, netAfterDiscount)
        } else if (isGstInclusive) {
            // Amount includes GST: Taxable = Gross / (1 + Rate)
            val baseTaxable = netAfterDiscount / (1.0 + (safeTaxRate / 100.0))
            val tax = netAfterDiscount - baseTaxable
            Triple(baseTaxable, tax, netAfterDiscount)
        } else {
            // Amount is exclusive of GST: Taxable = Gross, GST = Gross * Rate
            val tax = netAfterDiscount * (safeTaxRate / 100.0)
            Triple(netAfterDiscount, tax, netAfterDiscount + tax)
        }

        val (cgst, sgst, igst) = when (gstType) {
            GstType.CGST_SGST -> Triple(totalGstAmount / 2.0, totalGstAmount / 2.0, 0.0)
            GstType.IGST -> Triple(0.0, 0.0, totalGstAmount)
            GstType.NONE -> Triple(0.0, 0.0, 0.0)
        }

        val totalProfit = netAfterDiscount - totalCost
        val marginPct = if (totalCost > 0) (totalProfit / totalCost) * 100.0 else 0.0

        return QuotationCalculationResult(
            items = items,
            subtotal = roundTwoDecimals(subtotal),
            discountAmount = roundTwoDecimals(overallDiscount),
            taxableAmount = roundTwoDecimals(taxableAmount),
            gstRate = safeTaxRate,
            isGstInclusive = isGstInclusive,
            gstType = gstType,
            cgstAmount = roundTwoDecimals(cgst),
            sgstAmount = roundTwoDecimals(sgst),
            igstAmount = roundTwoDecimals(igst),
            totalGstAmount = roundTwoDecimals(totalGstAmount),
            grandTotal = roundTwoDecimals(grandTotal),
            totalCost = roundTwoDecimals(totalCost),
            totalProfit = roundTwoDecimals(totalProfit),
            profitMarginPercentage = roundTwoDecimals(marginPct),
            roomTotals = roomTotals.mapValues { roundTwoDecimals(it.value) },
            categoryTotals = categoryTotals.mapValues { roundTwoDecimals(it.value) }
        )
    }

    private fun roundTwoDecimals(value: Double): Double {
        return round(value * 100.0) / 100.0
    }
}

data class ItemCalculationSummary(
    val effectiveRate: Double,
    val grossAmount: Double,
    val discountAmount: Double,
    val netAmount: Double,
    val totalCost: Double,
    val itemProfit: Double
)
