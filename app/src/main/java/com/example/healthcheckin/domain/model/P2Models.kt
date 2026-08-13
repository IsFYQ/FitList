package com.example.healthcheckin.domain.model

import com.example.healthcheckin.util.ExerciseType
import com.example.healthcheckin.util.InventoryCategory
import com.example.healthcheckin.util.InventoryUnit

data class OcrConfirmLine(
    val id: String,
    val name: String,
    val quantity: Double,
    val unit: InventoryUnit,
    val unitPrice: Double?,
    val category: InventoryCategory,
    val rawText: String,
    val selected: Boolean = true,
    val needsReview: Boolean = false,
    val duplicateExistingId: String? = null,
    val mergeDuplicate: Boolean = true,
)

data class OcrImportLineRequest(
    val name: String,
    val quantity: Double,
    val unit: InventoryUnit,
    val unitPrice: Double?,
    val category: InventoryCategory,
    val rawText: String,
    val mergeExistingId: String?,
)

data class ExerciseRecordItem(
    val id: String,
    val localDate: String,
    val exerciseType: ExerciseType,
    val customName: String?,
    val metValue: Double,
    val durationMinutes: Int,
    val estimatedKcal: Int,
    val createdAt: Long,
)

data class SaveExerciseRequest(
    val exerciseType: ExerciseType,
    val customName: String?,
    val customMet: Double?,
    val durationMinutes: Int,
    val localDate: String,
)

data class ExerciseWeekSummary(
    val totalMinutes: Int,
    val sessionCount: Int,
    val currentStreak: Int,
    val bestStreak: Int,
    val weekDates: List<String>,
    val activeDates: Set<String>,
)

data class RecommendationMealBatchItem(
    val inventoryItemId: String,
    val food: FoodSearchItem,
    val quantity: Double,
    val unit: com.example.healthcheckin.util.MealUnit,
    val servingGrams: Double?,
    val inventoryDeductBasis: Double,
)
