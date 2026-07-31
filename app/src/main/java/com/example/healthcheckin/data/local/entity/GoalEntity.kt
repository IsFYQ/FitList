package com.example.healthcheckin.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "goals",
    indices = [
        Index(value = ["userId", "isActive"]),
        Index(value = ["userId", "effectiveFrom"])
    ]
)
data class GoalEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val currentWeightKg: Double,
    val targetWeightKg: Double,
    val targetWeeks: Int,
    val activityLevel: String,
    val goalType: String,
    val bmrKcal: Int,
    val tdeeKcal: Int,
    val dailyDeltaKcal: Int,
    val budgetKcal: Int,
    val proteinG: Double,
    val carbG: Double,
    val fatG: Double,
    val clamped: Boolean,
    val estWeeks: Int? = null,
    val effectiveFrom: String,
    val isActive: Boolean,
    val deviceId: String,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long? = null,
    val syncState: String,
)
