package com.example.healthcheckin.domain.algorithm

import com.example.healthcheckin.util.GoalType
import com.example.healthcheckin.util.PrecisionUtil
import kotlin.math.abs
import kotlin.math.min

object WeightProgressCalculator {

    fun deltaFromPrevious(currentKg: Double, previousKg: Double?): Double? {
        previousKg ?: return null
        return PrecisionUtil.roundWeightDisplay(currentKg - previousKg)
    }

    fun progressPercent(
        goalType: GoalType,
        initialKg: Double,
        targetKg: Double,
        latestKg: Double,
    ): Int? {
        val total = abs(targetKg - initialKg)
        if (total <= 0.0) return null

        var done = abs(latestKg - initialKg)
        when (goalType) {
            GoalType.LOSE -> if (latestKg > initialKg) done = 0.0
            GoalType.GAIN -> if (latestKg < initialKg) done = 0.0
            GoalType.MAINTAIN -> return null
        }
        return PrecisionUtil.roundInt(min(done / total, 1.0) * 100.0)
    }

    fun maintainDistanceKg(targetKg: Double, latestKg: Double): Double =
        PrecisionUtil.roundWeightDisplay(abs(latestKg - targetKg))

    fun yAxisRange(weights: List<Double>): ClosedFloatingPointRange<Double> {
        if (weights.isEmpty()) return 50.0..100.0
        val minW = weights.min()
        val maxW = weights.max()
        if (minW == maxW) {
            return (minW - 1.0)..(maxW + 1.0)
        }
        return (kotlin.math.floor(minW - 1.0))..(kotlin.math.ceil(maxW + 1.0))
    }
}
