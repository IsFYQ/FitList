package com.example.healthcheckin.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "body_measurements",
    indices = [
        Index(value = ["userId", "metric", "localDate"], unique = true),
    ],
)
data class BodyMeasurementEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val metric: String,
    val localDate: String,
    val tzOffsetMinutes: Int,
    val valueCm: Double,
    val deviceId: String,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long? = null,
    val syncState: String,
)
