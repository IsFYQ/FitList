package com.example.healthcheckin.domain.model

import com.example.healthcheckin.util.GoalType

data class WeightRecordItem(
    val id: String,
    val localDate: String,
    val weightKg: Double,
    val note: String?,
    val deltaKg: Double?,
)

enum class WeightChartRange(val days: Long, val labelResKey: String) {
    DAYS_7(7, "weight_range_7d"),
    DAYS_30(30, "weight_range_30d"),
    DAYS_90(90, "weight_range_90d"),
}

data class WeightProgressInfo(
    val initialWeightKg: Double?,
    val targetWeightKg: Double?,
    val goalType: GoalType?,
    val progressPercent: Int?,
    val maintainDistanceKg: Double?,
)

data class SaveWeightRequest(
    val weightKg: Double,
    val localDate: String,
    val note: String?,
)
