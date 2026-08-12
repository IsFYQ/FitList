package com.example.healthcheckin.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "milestones",
    indices = [
        Index(value = ["userId", "achievedAt"]),
    ],
)
data class MilestoneEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val title: String,
    val targetWeightKg: Double,
    val rewardText: String? = null,
    val achievedAt: Long? = null,
    val achievedWeightKg: Double? = null,
    val daysElapsed: Int? = null,
    val sharedCount: Int = 0,
    val deviceId: String,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long? = null,
    val syncState: String,
)
