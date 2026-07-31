package com.example.healthcheckin.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "foods",
    indices = [
        Index(value = ["userId", "nameNormalized"]),
        Index(value = ["userId", "lastUsedAt"]),
        Index(value = ["userId", "source", "externalId"], unique = true)
    ]
)
data class FoodEntity(
    @PrimaryKey val id: String,
    val userId: String?,
    val source: String,
    val externalId: String? = null,
    val name: String,
    val nameNormalized: String,
    val brand: String? = null,
    val basisUnit: String,
    val kcalPer100: Double,
    val proteinPer100: Double? = null,
    val carbPer100: Double? = null,
    val fatPer100: Double? = null,
    val servingName: String? = null,
    val servingGrams: Double? = null,
    val dataIncomplete: Boolean = false,
    val nutritionWarning: Boolean = false,
    val ingredientKey: String? = null,
    val lastUsedAt: Long? = null,
    val useCount30d: Int = 0,
    val lastQuantity: Double? = null,
    val lastUnit: String? = null,
    val lastMealSlot: String? = null,
    val deviceId: String,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long? = null,
    val syncState: String,
)
