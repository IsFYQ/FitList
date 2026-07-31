package com.example.healthcheckin.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "meal_entries",
    indices = [
        Index(value = ["userId", "localDate"]),
        Index(value = ["userId", "foodId", "localDate"]),
        Index(value = ["userId", "consumedAt"])
    ]
)
data class MealEntryEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val localDate: String,
    val tzOffsetMinutes: Int,
    val consumedAt: Long,
    val mealSlot: String,
    val foodId: String? = null,
    val quantity: Double,
    val unit: String,
    val basisAmount: Double,
    val snapFoodName: String,
    val snapBrand: String? = null,
    val snapSource: String,
    val snapBasisUnit: String,
    val snapKcalPer100: Double,
    val snapProteinPer100: Double? = null,
    val snapCarbPer100: Double? = null,
    val snapFatPer100: Double? = null,
    val snapServingName: String? = null,
    val snapServingGrams: Double? = null,
    val kcal: Double,
    val proteinG: Double? = null,
    val carbG: Double? = null,
    val fatG: Double? = null,
    val fromInventory: Boolean = false,
    val inventoryItemId: String? = null,
    val inventoryDeductedAmount: Double? = null,
    val entrySource: String,
    val deviceId: String,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long? = null,
    val syncState: String,
)
