package com.example.healthcheckin.domain.model

import com.example.healthcheckin.util.BasisUnit
import com.example.healthcheckin.util.FoodSource
import com.example.healthcheckin.util.MealEntrySource
import com.example.healthcheckin.util.MealSlot
import com.example.healthcheckin.util.MealUnit
import com.example.healthcheckin.domain.model.InventoryDeductChoice

data class FoodSearchItem(
    val foodId: String?,
    val publicFoodId: String?,
    val externalId: String?,
    val name: String,
    val brand: String?,
    val kcalPer100: Double,
    val proteinPer100: Double?,
    val carbPer100: Double?,
    val fatPer100: Double?,
    val basisUnit: BasisUnit,
    val servingName: String?,
    val servingGrams: Double?,
    val source: FoodSource,
    val dataIncomplete: Boolean,
    val nutritionWarning: Boolean,
    val lastUsedAt: Long?,
    val lastQuantity: Double?,
    val lastUnit: MealUnit?,
    val lastMealSlot: MealSlot? = null,
    val score: Double = 0.0,
    val barcode: String? = null,
)

enum class SearchBanner {
    NONE,
    OFFLINE,
    REMOTE_LOADING,
    REMOTE_TIMEOUT,
    REMOTE_UNAVAILABLE,
    QUOTA_EXHAUSTED,
    FROM_CACHE,
}

data class RemoteFetchResult(
    val appendedItems: List<FoodSearchItem>,
    val banner: SearchBanner = SearchBanner.NONE,
)

data class FoodSearchResult(
    val items: List<FoodSearchItem>,
    val banner: SearchBanner = SearchBanner.NONE,
)

data class RecentFrequentFoods(
    val recent: List<FoodSearchItem> = emptyList(),
    val frequent: List<FoodSearchItem> = emptyList(),
)

data class CustomFoodFormData(
    val id: String? = null,
    val name: String = "",
    val basisUnit: BasisUnit = BasisUnit.G,
    val kcalText: String = "",
    val proteinText: String = "",
    val carbText: String = "",
    val fatText: String = "",
    val servingGramsText: String = "",
)

data class MealNutritionPreview(
    val kcal: Double,
    val proteinG: Double?,
    val carbG: Double?,
    val fatG: Double?,
)

data class AddMealRequest(
    val food: FoodSearchItem,
    val quantity: Double,
    val unit: MealUnit,
    val servingGrams: Double?,
    val consumedAt: Long,
    val mealSlot: MealSlot,
    val entrySource: MealEntrySource,
    val inventoryItemId: String? = null,
    val deductChoice: InventoryDeductChoice? = null,
)

data class UpdateMealRequest(
    val entryId: String,
    val quantity: Double,
    val unit: MealUnit,
    val servingGrams: Double?,
    val consumedAt: Long,
    val mealSlot: MealSlot,
)
