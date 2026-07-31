package com.example.healthcheckin.ui.screens.meal

import com.example.healthcheckin.domain.model.FoodSearchItem
import com.example.healthcheckin.domain.model.MealNutritionPreview
import com.example.healthcheckin.domain.model.RecentFrequentFoods
import com.example.healthcheckin.domain.model.SearchBanner
import com.example.healthcheckin.util.MealSlot
import com.example.healthcheckin.util.MealUnit
import java.time.LocalDate
import java.time.LocalTime

data class MealSearchUiState(
    val localDate: String = "",
    val minDate: String = "",
    val isBackfill: Boolean = false,
    val query: String = "",
    val isSearching: Boolean = false,
    val searchResults: List<FoodSearchItem> = emptyList(),
    val recentFrequent: RecentFrequentFoods = RecentFrequentFoods(),
    val banner: SearchBanner = SearchBanner.NONE,
    val selectedFood: FoodSearchItem? = null,
    val selectedFromRecent: Boolean = false,
    val confirmState: MealConfirmUiState? = null,
    val errorMessage: String? = null,
    val savedEntryId: String? = null,
)

data class MealConfirmUiState(
    val food: FoodSearchItem,
    val quantityText: String,
    val unit: MealUnit,
    val servingGramsText: String,
    val mealSlot: MealSlot,
    val localDate: LocalDate,
    val time: LocalTime,
    val nutrition: MealNutritionPreview,
    val canSubmit: Boolean,
    val isSaving: Boolean = false,
    val showZeroKcalDialog: Boolean = false,
    val isEditMode: Boolean = false,
    val entryId: String? = null,
)

enum class MealSlotLabel(val slot: MealSlot, val label: String) {
    BREAKFAST(MealSlot.BREAKFAST, "早餐"),
    LUNCH(MealSlot.LUNCH, "午餐"),
    DINNER(MealSlot.DINNER, "晚餐"),
    SNACK(MealSlot.SNACK, "加餐"),
}

fun foodSourceLabel(source: String): String = when (source) {
    "CUSTOM" -> "自建"
    "PUBLIC" -> "常见食物"
    "FATSECRET" -> "FatSecret"
    "OFF" -> "Open Food Facts"
    else -> source
}

fun foodDataSourceText(source: String): String? = when (source) {
    "FATSECRET", "OFF", "PUBLIC" -> foodSourceLabel(source)
    else -> null
}

fun basisUnitLabel(unit: String): String = when (unit) {
    "ML" -> "ml"
    else -> "g"
}

fun lastPortionLabel(food: FoodSearchItem): String? {
    val quantity = food.lastQuantity ?: return null
    val unit = food.lastUnit ?: return null
    val unitLabel = when (unit.name) {
        "ML" -> "ml"
        "SERVING" -> "份"
        else -> "g"
    }
    val qtyText = if (quantity % 1.0 == 0.0) quantity.toInt().toString() else quantity.toString()
    return "上次 $qtyText$unitLabel"
}
