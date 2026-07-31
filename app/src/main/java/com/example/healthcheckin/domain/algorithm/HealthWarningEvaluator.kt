package com.example.healthcheckin.domain.algorithm

import com.example.healthcheckin.domain.model.HealthWarning
import com.example.healthcheckin.domain.model.HealthWarningType
import com.example.healthcheckin.util.DateTimeUtil

object HealthWarningEvaluator {

    private const val LOW_INTAKE_KCAL = 800.0
    private const val HIGH_INTAKE_RATIO = 1.5
    private const val MIN_ENTRIES = 2

    data class DayStats(
        val localDate: String,
        val entryCount: Int,
        val totalKcal: Double,
        val budgetKcal: Int,
    )

    fun evaluate(
        todayEntryCount: Int,
        recentDays: List<DayStats>,
        dismissedWarnings: Set<HealthWarningType>,
    ): HealthWarning? {
        val candidates = listOfNotNull(
            evaluateLowIntake(recentDays)?.takeIf { it.type !in dismissedWarnings },
            evaluateHighIntake(recentDays)?.takeIf { it.type !in dismissedWarnings },
            evaluateRecordGap(todayEntryCount, recentDays)?.takeIf { it.type !in dismissedWarnings },
        )
        return candidates.firstOrNull()
    }

    private fun evaluateLowIntake(recentDays: List<DayStats>): HealthWarning? {
        val lastThree = lastThreeCompleteDays(recentDays) ?: return null
        val allLow = lastThree.all {
            it.entryCount >= MIN_ENTRIES && it.totalKcal < LOW_INTAKE_KCAL
        }
        return if (allLow) HealthWarning(HealthWarningType.LOW_INTAKE) else null
    }

    private fun evaluateHighIntake(recentDays: List<DayStats>): HealthWarning? {
        val lastThree = lastThreeCompleteDays(recentDays) ?: return null
        val allHigh = lastThree.all {
            it.entryCount >= MIN_ENTRIES &&
                it.budgetKcal > 0 &&
                it.totalKcal > it.budgetKcal * HIGH_INTAKE_RATIO
        }
        return if (allHigh) HealthWarning(HealthWarningType.HIGH_INTAKE) else null
    }

    private fun evaluateRecordGap(
        todayEntryCount: Int,
        recentDays: List<DayStats>,
    ): HealthWarning? {
        if (todayEntryCount > 0) return null
        val today = DateTimeUtil.todayLocalDate()
        val yesterdayStr = DateTimeUtil.formatLocalDate(today.minusDays(1))
        val dayBeforeStr = DateTimeUtil.formatLocalDate(today.minusDays(2))

        fun isEmpty(date: String): Boolean {
            val stats = recentDays.find { it.localDate == date }
            return stats == null || stats.entryCount == 0
        }

        return if (isEmpty(yesterdayStr) && isEmpty(dayBeforeStr)) {
            HealthWarning(HealthWarningType.RECORD_GAP)
        } else {
            null
        }
    }

    private fun lastThreeCompleteDays(recentDays: List<DayStats>): List<DayStats>? {
        val today = DateTimeUtil.todayLocalDateString()
        val filtered = recentDays
            .filter { it.localDate < today }
            .sortedByDescending { it.localDate }
            .take(3)
        return if (filtered.size == 3) filtered else null
    }
}
