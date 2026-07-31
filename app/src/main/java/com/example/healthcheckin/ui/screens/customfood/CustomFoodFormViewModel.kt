package com.example.healthcheckin.ui.screens.customfood

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.healthcheckin.data.auth.SessionManager
import com.example.healthcheckin.data.analytics.AnalyticsParamBuilder
import com.example.healthcheckin.domain.analytics.AnalyticsEvents
import com.example.healthcheckin.domain.analytics.AnalyticsTracker
import com.example.healthcheckin.domain.repository.FoodRepository
import com.example.healthcheckin.domain.repository.SaveCustomFoodRequest
import com.example.healthcheckin.domain.repository.SaveCustomFoodResult
import com.example.healthcheckin.util.BasisUnit
import com.example.healthcheckin.util.Validators
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CustomFoodFormViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val foodRepository: FoodRepository,
    private val sessionManager: SessionManager,
    private val analyticsTracker: AnalyticsTracker,
) : ViewModel() {

    private val prefilledName: String = savedStateHandle["prefilledName"] ?: ""
    private val editFoodId: String? = savedStateHandle["foodId"]

    private val _uiState = MutableStateFlow(CustomFoodFormUiState(name = prefilledName))
    val uiState: StateFlow<CustomFoodFormUiState> = _uiState.asStateFlow()

    init {
        editFoodId?.let { loadFood(it) }
    }

    private fun loadFood(foodId: String) {
        viewModelScope.launch {
            val userId = sessionManager.getUserId() ?: return@launch
            val food = foodRepository.getCustomFood(userId, foodId) ?: return@launch
            _uiState.update {
                it.copy(
                    foodId = food.foodId,
                    name = food.name,
                    basisUnit = food.basisUnit,
                    kcalText = formatNum(food.kcalPer100),
                    proteinText = formatNum(food.proteinPer100 ?: 0.0),
                    carbText = formatNum(food.carbPer100 ?: 0.0),
                    fatText = formatNum(food.fatPer100 ?: 0.0),
                    servingGramsText = food.servingGrams?.let(::formatNum).orEmpty(),
                )
            }
        }
    }

    fun updateName(value: String) = _uiState.update { it.copy(name = value, errorMessage = null) }

    fun updateBasisUnit(unit: BasisUnit) = _uiState.update { it.copy(basisUnit = unit) }

    fun updateKcal(value: String) = updateDecimalField(value) { copy(kcalText = it) }

    fun updateProtein(value: String) = updateDecimalField(value) { copy(proteinText = it) }

    fun updateCarb(value: String) = updateDecimalField(value) { copy(carbText = it) }

    fun updateFat(value: String) = updateDecimalField(value) { copy(fatText = it) }

    fun updateServingGrams(value: String) = updateDecimalField(value) { copy(servingGramsText = it) }

    fun dismissDuplicateDialog() {
        _uiState.update { it.copy(duplicateName = null, duplicateFoodId = null) }
    }

    fun save(overwrite: Boolean = false) {
        val state = _uiState.value
        if (state.isSaving) return
        viewModelScope.launch {
            val userId = sessionManager.getUserId() ?: return@launch
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }

            val servingGrams = state.servingGramsText.takeIf { it.isNotBlank() }
                ?.let { Validators.parseDecimalInput(it) }

            val result = foodRepository.saveCustomFood(
                userId,
                SaveCustomFoodRequest(
                    id = state.foodId,
                    name = state.name,
                    basisUnit = state.basisUnit,
                    kcalPer100 = Validators.parseDecimalInput(state.kcalText) ?: 0.0,
                    proteinPer100 = Validators.parseDecimalInput(state.proteinText) ?: 0.0,
                    carbPer100 = Validators.parseDecimalInput(state.carbText) ?: 0.0,
                    fatPer100 = Validators.parseDecimalInput(state.fatText) ?: 0.0,
                    servingGrams = servingGrams,
                    overwriteExistingId = if (overwrite) state.duplicateFoodId else null,
                ),
            )

            when (result) {
                is SaveCustomFoodResult.Success -> {
                    if (state.foodId == null) {
                        analyticsTracker.track(
                            AnalyticsEvents.CUSTOM_FOOD_CREATED,
                            mapOf(
                                "has_serving" to (servingGrams != null),
                                "nutrition_warning" to result.food.nutritionWarning,
                                "from_zero_result" to prefilledName.isNotBlank(),
                            ),
                        )
                    }
                    _uiState.update {
                        it.copy(isSaving = false, savedFoodId = result.food.foodId)
                    }
                }
                is SaveCustomFoodResult.DuplicateName -> {
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            duplicateName = result.existingName,
                            duplicateFoodId = result.existingId,
                        )
                    }
                }
                is SaveCustomFoodResult.ValidationFailed -> {
                    _uiState.update {
                        it.copy(isSaving = false, errorMessage = result.messageKey)
                    }
                }
            }
        }
    }

    fun clearSavedFoodId() {
        _uiState.update { it.copy(savedFoodId = null) }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    private inline fun updateDecimalField(value: String, transform: CustomFoodFormUiState.(String) -> CustomFoodFormUiState) {
        val filtered = Validators.filterDecimalInput(value)
        _uiState.update { it.transform(filtered).copy(errorMessage = null) }
    }

    private fun formatNum(value: Double): String =
        if (value % 1.0 == 0.0) value.toInt().toString() else value.toString()
}
