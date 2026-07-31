package com.example.healthcheckin.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "daily_budgets",
    indices = [Index(value = ["userId", "localDate"], unique = true)]
)
data class DailyBudgetEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val localDate: String,
    val goalId: String,
    val budgetKcal: Int,
    val proteinG: Double,
    val carbG: Double,
    val fatG: Double,
    val deviceId: String,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long? = null,
    val syncState: String,
)
