package com.example.healthcheckin.ui.screens.meal

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.example.healthcheckin.R
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
    val inventoryMatch: com.example.healthcheckin.domain.model.InventoryMatchResult? = null,
    val inventoryPreview: com.example.healthcheckin.domain.model.InventoryDeductPreview? = null,
    val deductChecked: Boolean = false,
    val showInventoryPicker: Boolean = false,
    val inventoryCandidates: List<com.example.healthcheckin.domain.model.InventoryItem> = emptyList(),
)

enum class MealSlotLabel(val slot: MealSlot, @StringRes val labelRes: Int) {
    BREAKFAST(MealSlot.BREAKFAST, R.string.meal_slot_breakfast),
    LUNCH(MealSlot.LUNCH, R.string.meal_slot_lunch),
    DINNER(MealSlot.DINNER, R.string.meal_slot_dinner),
    SNACK(MealSlot.SNACK, R.string.meal_slot_snack),
}

@Composable
fun foodSourceLabel(source: String): String = when (source) {
    "CUSTOM" -> stringResource(R.string.meal_source_custom)
    "PUBLIC" -> stringResource(R.string.meal_source_public)
    "FATSECRET" -> "FatSecret"
    "OFF" -> "Open Food Facts"
    else -> source
}

@Composable
fun foodDataSourceText(source: String): String? = when (source) {
    "FATSECRET", "OFF", "PUBLIC" -> foodSourceLabel(source)
    else -> null
}

fun basisUnitLabel(unit: String): String = when (unit) {
    "ML" -> "ml"
    else -> "g"
}

@Composable
fun lastPortionLabel(food: FoodSearchItem): String? {
    val quantity = food.lastQuantity ?: return null
    val unit = food.lastUnit ?: return null
    val unitLabel = when (unit.name) {
        "ML" -> stringResource(R.string.unit_ml)
        "SERVING" -> stringResource(R.string.meal_unit_serving)
        else -> stringResource(R.string.unit_gram)
    }
    val qtyText = if (quantity % 1.0 == 0.0) quantity.toInt().toString() else quantity.toString()
    return stringResource(R.string.meal_last_portion, qtyText, unitLabel)
}
