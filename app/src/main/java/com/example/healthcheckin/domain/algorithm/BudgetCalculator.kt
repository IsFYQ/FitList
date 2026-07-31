package com.example.healthcheckin.domain.algorithm

import com.example.healthcheckin.util.GoalType
import com.example.healthcheckin.util.PrecisionUtil
import com.example.healthcheckin.util.Sex
import com.example.healthcheckin.util.ValidationConstants
import kotlin.math.abs
import kotlin.math.ceil

data class BudgetResult(
    val goalType: GoalType,
    val rawDelta: Double,
    val dailyDeltaKcal: Int,
    val budgetKcal: Int,
    val clamped: Boolean,
    val actualDailyDelta: Int,
    val estWeeks: Int?,
)

object BudgetCalculator {

    fun determineGoalType(currentWeightKg: Double, targetWeightKg: Double): GoalType {
        val delta = targetWeightKg - currentWeightKg
        return when {
            delta < -ValidationConstants.GOAL_MAINTAIN_THRESHOLD -> GoalType.LOSE
            delta > ValidationConstants.GOAL_MAINTAIN_THRESHOLD -> GoalType.GAIN
            else -> GoalType.MAINTAIN
        }
    }

    fun calculate(
        sex: Sex,
        tdeeKcal: Int,
        currentWeightKg: Double,
        targetWeightKg: Double,
        targetWeeks: Int,
    ): BudgetResult {
        val goalType = determineGoalType(currentWeightKg, targetWeightKg)
        val deltaWeight = abs(targetWeightKg - currentWeightKg)
        val totalDays = targetWeeks * 7

        val rawDelta = if (goalType == GoalType.MAINTAIN || totalDays == 0) {
            0.0
        } else {
            ValidationConstants.ENERGY_PER_KG * deltaWeight / totalDays
        }

        var deltaWasCapped = false
        val dailyDelta = when (goalType) {
            GoalType.MAINTAIN -> 0
            GoalType.LOSE -> {
                val capped = minOf(rawDelta, ValidationConstants.LOSE_DELTA_CAP.toDouble())
                deltaWasCapped = capped < rawDelta
                -capped.toInt()
            }
            GoalType.GAIN -> {
                val capped = minOf(rawDelta, ValidationConstants.GAIN_DELTA_CAP.toDouble())
                deltaWasCapped = capped < rawDelta
                capped.toInt()
            }
        }

        val budgetRaw = tdeeKcal + dailyDelta
        val safetyFloor = when (sex) {
            Sex.MALE -> ValidationConstants.SAFETY_FLOOR_MALE
            Sex.FEMALE -> ValidationConstants.SAFETY_FLOOR_FEMALE
        }
        val budgetClamped = budgetRaw < safetyFloor
        val budget = PrecisionUtil.roundInt(maxOf(budgetRaw.toDouble(), safetyFloor.toDouble()))
        val clamped = deltaWasCapped || budgetClamped

        val actualDailyDelta = budget - tdeeKcal
        val estWeeks = if (goalType == GoalType.MAINTAIN || actualDailyDelta == 0) {
            null
        } else {
            ceil(
                ValidationConstants.ENERGY_PER_KG * deltaWeight /
                    (abs(actualDailyDelta) * 7.0)
            ).toInt()
        }

        return BudgetResult(
            goalType = goalType,
            rawDelta = rawDelta,
            dailyDeltaKcal = dailyDelta,
            budgetKcal = budget,
            clamped = clamped,
            actualDailyDelta = actualDailyDelta,
            estWeeks = estWeeks,
        )
    }
}
