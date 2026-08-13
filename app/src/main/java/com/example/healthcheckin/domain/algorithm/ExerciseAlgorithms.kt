package com.example.healthcheckin.domain.algorithm

import com.example.healthcheckin.util.ExerciseType
import com.example.healthcheckin.util.PrecisionUtil
import java.time.LocalDate

object ExerciseMetCalculator {
    fun estimatedKcal(met: Double, weightKg: Double, durationMinutes: Int): Int {
        val hours = durationMinutes / 60.0
        return PrecisionUtil.roundInt(met * weightKg * hours)
    }

    fun metFor(type: ExerciseType, customMet: Double?): Double = when (type) {
        ExerciseType.CUSTOM -> customMet ?: 4.0
        else -> type.defaultMet
    }
}

data class ExerciseStreakResult(
    val currentStreak: Int,
    val bestStreak: Int,
    val isNewBest: Boolean,
    val milestoneDays: Int? = null,
)

object ExerciseStreakCalculator {
    private const val MIN_EFFECTIVE_MINUTES = 10

    fun computeStreak(
        recordsByDate: Map<String, Int>,
        today: LocalDate,
        previousBest: Int,
        allowMilestoneToast: Boolean,
    ): ExerciseStreakResult {
        var streak = 0
        var date = today
        while (true) {
            val minutes = recordsByDate[date.toString()] ?: 0
            if (minutes >= MIN_EFFECTIVE_MINUTES) {
                streak++
                date = date.minusDays(1)
            } else {
                break
            }
        }
        val best = maxOf(previousBest, streak)
        val milestone = if (allowMilestoneToast && streak in listOf(7, 30, 100) && streak > previousBest) streak else null
        return ExerciseStreakResult(
            currentStreak = streak,
            bestStreak = best,
            isNewBest = streak > previousBest,
            milestoneDays = milestone,
        )
    }

    fun aggregateMinutesByDate(records: List<Pair<String, Int>>): Map<String, Int> =
        records.groupBy({ it.first }, { it.second }).mapValues { (_, mins) -> mins.sum() }
}
