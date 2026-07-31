package com.example.healthcheckin.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "weight_records",
    indices = [
        Index(value = ["userId", "localDate"], unique = true)
    ]
)
data class WeightRecordEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val localDate: String,
    val tzOffsetMinutes: Int,
    val weightKg: Double,
    val note: String? = null,
    val deviceId: String,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long? = null,
    val syncState: String,
)
