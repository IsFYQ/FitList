package com.example.healthcheckin.domain.repository

import com.example.healthcheckin.domain.model.FoodSearchItem
import com.example.healthcheckin.util.BasisUnit

data class SaveCustomFoodRequest(
    val id: String? = null,
    val name: String,
    val basisUnit: BasisUnit,
    val kcalPer100: Double,
    val proteinPer100: Double,
    val carbPer100: Double,
    val fatPer100: Double,
    val servingGrams: Double? = null,
    val overwriteExistingId: String? = null,
)

sealed class SaveCustomFoodResult {
    data class Success(val food: FoodSearchItem) : SaveCustomFoodResult()
    data class DuplicateName(val existingName: String, val existingId: String) : SaveCustomFoodResult()
    data class ValidationFailed(val messageKey: String) : SaveCustomFoodResult()
}

interface FoodRepository {
    suspend fun listCustomFoods(userId: String): List<FoodSearchItem>
    suspend fun getCustomFood(userId: String, foodId: String): FoodSearchItem?
    suspend fun getFoodSearchItem(userId: String, foodId: String): FoodSearchItem?
    suspend fun saveCustomFood(userId: String, request: SaveCustomFoodRequest): SaveCustomFoodResult
    suspend fun deleteCustomFood(userId: String, foodId: String): Result<Unit>
    suspend fun countMealReferences(foodId: String): Int
}
