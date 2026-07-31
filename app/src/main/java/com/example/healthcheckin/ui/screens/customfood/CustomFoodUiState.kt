package com.example.healthcheckin.ui.screens.customfood

import com.example.healthcheckin.util.BasisUnit

data class CustomFoodFormUiState(
    val foodId: String? = null,
    val name: String = "",
    val basisUnit: BasisUnit = BasisUnit.G,
    val kcalText: String = "",
    val proteinText: String = "",
    val carbText: String = "",
    val fatText: String = "",
    val servingGramsText: String = "",
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val duplicateName: String? = null,
    val duplicateFoodId: String? = null,
    val savedFoodId: String? = null,
)

data class CustomFoodListItem(
    val id: String,
    val name: String,
    val kcalPer100: Int,
    val basisUnit: BasisUnit,
)

data class CustomFoodListUiState(
    val items: List<CustomFoodListItem> = emptyList(),
    val deleteTarget: CustomFoodListItem? = null,
    val deleteReferenceCount: Int = 0,
    val isDeleting: Boolean = false,
)
