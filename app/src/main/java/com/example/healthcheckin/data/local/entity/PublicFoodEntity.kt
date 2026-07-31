package com.example.healthcheckin.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "public_foods",
    indices = [Index(value = ["nameNormalized"])]
)
data class PublicFoodEntity(
    @PrimaryKey val id: String,
    val source: String = "PUBLIC",
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
    val createdAt: Long,
    val updatedAt: Long,
)
