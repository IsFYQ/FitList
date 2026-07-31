package com.example.healthcheckin.domain.algorithm

import com.example.healthcheckin.util.GoalType
import com.example.healthcheckin.util.PrecisionUtil
import com.example.healthcheckin.util.ValidationConstants
import kotlin.math.abs
import kotlin.math.max

data class MacroResult(
    val proteinG: Double,
    val carbG: Double,
    val fatG: Double,
    val budgetAdjusted: Boolean,
    val adjustedBudgetKcal: Int,
)

object MacroCalculator {

    private data class Ratios(val protein: Double, val carb: Double, val fat: Double)

    fun calculate(
        goalType: GoalType,
        budgetKcal: Int,
        currentWeightKg: Double,
    ): MacroResult {
        val ratios = when (goalType) {
            GoalType.LOSE -> Ratios(0.30, 0.40, 0.30)
            GoalType.MAINTAIN -> Ratios(0.20, 0.50, 0.30)
            GoalType.GAIN -> Ratios(0.25, 0.50, 0.25)
        }

        val proteinFloor = (if (goalType == GoalType.LOSE) 1.6 else 1.2) * currentWeightKg
        var proteinG = max(budgetKcal * ratios.protein / 4.0, proteinFloor)

        val fatMin = budgetKcal * 0.20 / 9.0
        var fatG = max(budgetKcal * ratios.fat / 9.0, fatMin)

        var carbG = (budgetKcal - proteinG * 4.0 - fatG * 9.0) / 4.0
        var budgetAdjusted = false
        var adjustedBudget = budgetKcal

        if (carbG < ValidationConstants.CARB_ABSOLUTE_MIN_G) {
            carbG = ValidationConstants.CARB_ABSOLUTE_MIN_G
            val surplus = proteinG * 4.0 + fatG * 9.0 + carbG * 4.0 - budgetKcal
            if (surplus > 0) {
                val reducibleFat = fatG - fatMin
                val fatReduction = minOf(reducibleFat, surplus / 9.0)
                fatG -= fatReduction
                var remainingSurplus = surplus - fatReduction * 9.0

                if (remainingSurplus > 0) {
                    val reducibleProtein = proteinG - proteinFloor
                    val proteinReduction = minOf(reducibleProtein, remainingSurplus / 4.0)
                    proteinG -= proteinReduction
                    remainingSurplus -= proteinReduction * 4.0
                }

                if (remainingSurplus > 0) {
                    adjustedBudget = PrecisionUtil.roundInt(
                        proteinG * 4.0 + fatG * 9.0 + carbG * 4.0
                    )
                    budgetAdjusted = true
                }
            }
        }

        return MacroResult(
            proteinG = PrecisionUtil.roundMacroDisplay(proteinG),
            carbG = PrecisionUtil.roundMacroDisplay(carbG),
            fatG = PrecisionUtil.roundMacroDisplay(fatG),
            budgetAdjusted = budgetAdjusted,
            adjustedBudgetKcal = adjustedBudget,
        )
    }

    fun isWithinTolerance(
        proteinG: Double,
        carbG: Double,
        fatG: Double,
        budgetKcal: Int,
    ): Boolean {
        val total = proteinG * 4.0 + carbG * 4.0 + fatG * 9.0
        return abs(total - budgetKcal) <= ValidationConstants.MACRO_TOLERANCE_KCAL
    }
}
