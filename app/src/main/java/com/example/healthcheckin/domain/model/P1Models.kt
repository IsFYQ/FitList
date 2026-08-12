package com.example.healthcheckin.domain.model

import com.example.healthcheckin.util.BodyMetric
import com.example.healthcheckin.util.GoalType
import com.example.healthcheckin.util.InventoryCategory
import com.example.healthcheckin.util.InventoryDeductResolution
import com.example.healthcheckin.util.InventoryExpiryStatus
import com.example.healthcheckin.util.InventoryMatchLevel
import com.example.healthcheckin.util.InventorySortMode
import com.example.healthcheckin.util.InventoryUnit
import com.example.healthcheckin.util.MealUnit

data class BodyMeasurementItem(
    val id: String,
    val metric: BodyMetric,
    val localDate: String,
    val valueCm: Double,
    val deltaCm: Double?,
)

data class BodyMetricSummary(
    val metric: BodyMetric,
    val latest: BodyMeasurementItem?,
    val sparkline: List<Double>,
)

data class SaveBodyMeasurementRequest(
    val metric: BodyMetric,
    val valueCm: Double,
    val localDate: String,
)

enum class BodyChartRange(val days: Int?) {
    DAYS_30(30),
    DAYS_90(90),
    ALL(null),
}

data class MilestoneItem(
    val id: String,
    val title: String,
    val targetWeightKg: Double,
    val rewardText: String?,
    val achievedAt: Long?,
    val achievedWeightKg: Double?,
    val daysElapsed: Int?,
    val sharedCount: Int,
    val createdAt: Long,
    val remainingKg: Double?,
    val progress: Float,
)

data class SaveMilestoneRequest(
    val title: String,
    val targetWeightKg: Double,
    val rewardText: String?,
)

data class MilestoneAchievementEvent(
    val milestoneId: String,
    val title: String,
    val achievedWeightKg: Double,
    val daysElapsed: Int,
    val rewardText: String?,
)

data class InventoryItem(
    val id: String,
    val name: String,
    val category: InventoryCategory,
    val remainingAmount: Double,
    val initialAmount: Double,
    val unit: InventoryUnit,
    val pieceGrams: Double?,
    val purchaseDate: String,
    val expiryDate: String?,
    val unitPrice: Double?,
    val ingredientKey: String?,
    val expiryStatus: InventoryExpiryStatus,
    val daysStored: Int,
    val daysLeft: Int?,
    val expiryLabel: String?,
    val canDeduct: Boolean,
    val lastDeductLabel: String?,
    val boundFoodId: String? = null,
)

data class SaveInventoryRequest(
    val name: String,
    val category: InventoryCategory,
    val amount: Double,
    val unit: InventoryUnit,
    val pieceGrams: Double?,
    val purchaseDate: String,
    val expiryDate: String?,
    val unitPrice: Double?,
    val boundFoodId: String? = null,
)

data class UpdateInventoryRequest(
    val itemId: String,
    val name: String,
    val category: InventoryCategory,
    val remainingAmount: Double,
    val pieceGrams: Double?,
    val purchaseDate: String,
    val expiryDate: String?,
    val unitPrice: Double?,
    val boundFoodId: String? = null,
)

data class InventoryListState(
    val sortMode: InventorySortMode = InventorySortMode.BY_CATEGORY,
    val query: String = "",
    val groups: List<InventoryCategoryGroup> = emptyList(),
    val flat: List<InventoryItem> = emptyList(),
)

data class InventoryCategoryGroup(
    val category: InventoryCategory,
    val items: List<InventoryItem>,
)

data class IngredientBindingItem(
    val id: String,
    val foodId: String,
    val foodName: String,
    val inventoryItemId: String,
    val inventoryName: String,
)

data class InventoryMatchResult(
    val level: InventoryMatchLevel,
    val confidence: Double,
    val item: InventoryItem?,
    val label: String?,
)

data class InventoryDeductPreview(
    val match: InventoryMatchResult,
    val needAmount: Double,
    val needUnit: InventoryUnit,
    val deductAmount: Double,
    val remainingAfter: Double,
    val insufficient: Boolean,
)

data class InventoryDeductChoice(
    val resolution: InventoryDeductResolution,
    val manualAmount: Double? = null,
)

data class AddMealWithInventoryRequest(
    val food: FoodSearchItem,
    val quantity: Double,
    val unit: MealUnit,
    val servingGrams: Double?,
    val consumedAt: Long,
    val mealSlot: com.example.healthcheckin.util.MealSlot,
    val entrySource: com.example.healthcheckin.util.MealEntrySource,
    val inventoryItemId: String?,
    val deductChoice: InventoryDeductChoice?,
)
