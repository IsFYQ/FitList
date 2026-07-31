package com.example.healthcheckin.domain.model

import com.example.healthcheckin.data.local.entity.MealEntryEntity
import com.example.healthcheckin.util.CalorieState
import com.example.healthcheckin.util.GoalType
import com.example.healthcheckin.util.MealSlot

data class ResolvedBudget(
    val budgetKcal: Int,
    val proteinG: Double,
    val carbG: Double,
    val fatG: Double,
    val isInferred: Boolean = false,
    val goalType: GoalType? = null,
    val targetWeightKg: Double? = null,
)

data class ConsumptionDisplay(
    val kcal: Int,
    val proteinG: Double,
    val carbG: Double,
    val fatG: Double,
    val entryCount: Int,
)

data class CalorieOverview(
    val budget: Int,
    val consumed: Int,
    val remaining: Int,
    val state: CalorieState,
)

enum class MacroKind { PROTEIN, CARB, FAT }

data class MacroProgress(
    val name: String,
    val kind: MacroKind,
    val consumed: Double,
    val target: Double,
    val progress: Float,
    val percentText: String,
    val isOver: Boolean,
    val overAmount: Double?,
)

data class WeightCardData(
    val latestWeightKg: Double?,
    val deltaKg: Double?,
    val deltaPositive: Boolean,
    val distanceToTargetKg: Double?,
    val goalReached: Boolean,
    val hasRecords: Boolean,
    val goalType: GoalType? = null,
)

data class MealGroup(
    val slot: MealSlot,
    val slotLabel: String,
    val totalKcal: Int,
    val entries: List<MealEntryEntity>,
)

enum class HealthWarningType { LOW_INTAKE, HIGH_INTAKE, RECORD_GAP }

val HealthWarningType.ruleId: String
    get() = when (this) {
        HealthWarningType.LOW_INTAKE -> "W-01"
        HealthWarningType.HIGH_INTAKE -> "W-02"
        HealthWarningType.RECORD_GAP -> "W-03"
    }

val HealthWarningType.showBackfillAction: Boolean
    get() = this == HealthWarningType.LOW_INTAKE || this == HealthWarningType.RECORD_GAP

data class HealthWarning(
    val type: HealthWarningType,
)

enum class SyncBadgeType { NONE, OFFLINE, PENDING, FAILED }

data class SyncBadge(
    val type: SyncBadgeType,
    val count: Int = 0,
)

data class DashboardData(
    val budget: ResolvedBudget?,
    val consumption: ConsumptionDisplay,
    val calorieOverview: CalorieOverview?,
    val macros: List<MacroProgress>,
    val weightCard: WeightCardData,
    val mealGroups: List<MealGroup>,
    val hasNoGoal: Boolean,
    val budgetAbnormal: Boolean,
)
