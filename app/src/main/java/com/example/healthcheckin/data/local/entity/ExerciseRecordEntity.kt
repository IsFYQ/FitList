package com.example.healthcheckin.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "exercise_records",
    indices = [
        Index(value = ["userId", "localDate"]),
    ],
)
data class ExerciseRecordEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val localDate: String,
    val tzOffsetMinutes: Int,
    val exerciseType: String,
    val customName: String? = null,
    val metValue: Double,
    val durationMinutes: Int,
    val estimatedKcal: Int,
    val deviceId: String,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long? = null,
    val syncState: String,
)
