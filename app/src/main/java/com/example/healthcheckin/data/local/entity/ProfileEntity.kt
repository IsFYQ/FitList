package com.example.healthcheckin.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "profiles",
    indices = [Index(value = ["email"])]
)
data class ProfileEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val email: String,
    val sex: String? = null,
    val birthYearMonth: String? = null,
    val heightCm: Double? = null,
    val initialWeightKg: Double? = null,
    val onboardingCompletedAt: Long? = null,
    val registeredLocalDate: String,
    val deviceId: String,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long? = null,
    val syncState: String,
)
