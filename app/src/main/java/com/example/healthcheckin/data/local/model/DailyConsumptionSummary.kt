package com.example.healthcheckin.data.local.model

import androidx.room.ColumnInfo

data class DailyConsumptionSummary(
    @ColumnInfo(name = "consumedKcal") val consumedKcal: Double,
    @ColumnInfo(name = "consumedProtein") val consumedProtein: Double,
    @ColumnInfo(name = "consumedCarb") val consumedCarb: Double,
    @ColumnInfo(name = "consumedFat") val consumedFat: Double,
    @ColumnInfo(name = "entryCount") val entryCount: Int,
)
