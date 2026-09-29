package com.example.domain.engine

import kotlin.math.round

data class PaymentMilestone(
    val stageName: String,
    val percentage: Double,
    val amount: Double = 0.0,
    val description: String = ""
)

object PaymentMilestoneEngine {

    val PRESET_FULL_ADVANCE = listOf(
        PaymentMilestone("Advance upon confirmation", 100.0)
    )

    val PRESET_50_40_10 = listOf(
        PaymentMilestone("Advance upon contract signing", 50.0),
        PaymentMilestone("During site execution / dispatch", 40.0),
        PaymentMilestone("Prior to final handover & punchlist", 10.0)
    )

    val PRESET_40_40_20 = listOf(
        PaymentMilestone("Booking & design finalization", 40.0),
        PaymentMilestone("Factory production initiation", 40.0),
        PaymentMilestone("Site installation completion", 20.0)
    )

    val PRESET_CIVIL_CONSTRUCTION = listOf(
        PaymentMilestone("Mobilization advance", 20.0),
        PaymentMilestone("Plinth & Foundation level", 20.0),
        PaymentMilestone("Slab casting completion", 30.0),
        PaymentMilestone("Brickwork & plastering", 20.0),
        PaymentMilestone("Final finishing & handover", 10.0)
    )

    fun calculateMilestones(milestones: List<PaymentMilestone>, totalAmount: Double): List<PaymentMilestone> {
        val safeTotal = kotlin.math.max(0.0, totalAmount)
        return milestones.map { milestone ->
            val amt = (safeTotal * (milestone.percentage / 100.0))
            milestone.copy(amount = round(amt * 100.0) / 100.0)
        }
    }
}
