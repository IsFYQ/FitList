package com.example.healthcheckin.domain.algorithm

import com.example.healthcheckin.util.ActivityLevel
import com.example.healthcheckin.util.Sex

data class GoalCalculationResult(
    val bmr: BmrResult,
    val tdeeKcal: Int,
    val budget: BudgetResult,
    val macro: MacroResult,
    val finalBudgetKcal: Int,
    val macroBudgetAdjusted: Boolean,
)

object GoalCalculationService {

    fun calculate(
        sex: Sex,
        currentWeightKg: Double,
        heightCm: Double,
        ageYears: Int,
        targetWeightKg: Double,
        targetWeeks: Int,
        activityLevel: ActivityLevel,
    ): GoalCalculationResult {
        val bmr = BmrCalculator.calculate(sex, currentWeightKg, heightCm, ageYears)
        val tdee = TdeeCalculator.calculate(bmr.bmrKcal, activityLevel)
        val budget = BudgetCalculator.calculate(
            sex = sex,
            tdeeKcal = tdee,
            currentWeightKg = currentWeightKg,
            targetWeightKg = targetWeightKg,
            targetWeeks = targetWeeks,
        )
        val macro = MacroCalculator.calculate(
            goalType = budget.goalType,
            budgetKcal = budget.budgetKcal,
            currentWeightKg = currentWeightKg,
        )
        val finalBudget = if (macro.budgetAdjusted) macro.adjustedBudgetKcal else budget.budgetKcal
        return GoalCalculationResult(
            bmr = bmr,
            tdeeKcal = tdee,
            budget = budget,
            macro = macro,
            finalBudgetKcal = finalBudget,
            macroBudgetAdjusted = macro.budgetAdjusted,
        )
    }
}
