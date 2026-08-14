package com.example.healthcheckin.ui.screens.meal

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.healthcheckin.data.auth.SessionManager
import com.example.healthcheckin.data.local.dao.ProfileDao
import com.example.healthcheckin.data.local.entity.MealEntryEntity
import com.example.healthcheckin.domain.analytics.AnalyticsEvents
import com.example.healthcheckin.domain.analytics.AnalyticsTracker
import com.example.healthcheckin.domain.model.FoodSearchItem
import com.example.healthcheckin.domain.model.MealNutritionPreview
import com.example.healthcheckin.domain.model.UpdateMealRequest
import com.example.healthcheckin.domain.repository.DashboardRepository
import com.example.healthcheckin.domain.repository.MealRepository
import com.example.healthcheckin.domain.algorithm.MealNutritionCalculator
import com.example.healthcheckin.domain.algorithm.MealSlotInferencer
import com.example.healthcheckin.util.BasisUnit
import com.example.healthcheckin.util.DateTimeUtil
import com.example.healthcheckin.util.FoodSource
import com.example.healthcheckin.util.MealSlot
import com.example.healthcheckin.util.MealUnit
import com.example.healthcheckin.util.Validators
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject

data class MealEditUiState(
    val isLoading: Boolean = true,
    val notFound: Boolean = false,
    val minDate: String = "",
    val confirmState: MealConfirmUiState? = null,
    val errorMessage: String? = null,
    val deleted: Boolean = false,
    val saved: Boolean = false,
)

@HiltViewModel
class MealEditViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val mealRepository: MealRepository,
    private val dashboardRepository: DashboardRepository,
    private val sessionManager: SessionManager,
    private val profileDao: ProfileDao,
    private val analyticsTracker: AnalyticsTracker,
) : ViewModel() {

    private val entryId: String = checkNotNull(savedStateHandle["entryId"]) { "entryId required" }
    private val _uiState = MutableStateFlow(MealEditUiState())
    val uiState: StateFlow<MealEditUiState> = _uiState.asStateFlow()

    private var entryEntity: MealEntryEntity? = null

    init {
        viewModelScope.launch {
            try {
                val userId = sessionManager.getUserId() ?: return@launch
                val profile = profileDao.getById(userId)
                val minDate = dashboardRepository.getMinViewDate(
                    profile?.registeredLocalDate ?: DateTimeUtil.todayLocalDateString(),
                )
                // minDate must be on state before toConfirmState() → validateConfirm()
                _uiState.update { it.copy(minDate = minDate) }
                val entry = mealRepository.getMealById(entryId)
                if (entry == null || entry.userId != userId) {
                    _uiState.update { it.copy(isLoading = false, notFound = true) }
                    return@launch
                }
                entryEntity = entry
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        confirmState = entry.toConfirmState(),
                    )
                }
            } catch (_: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, notFound = true, errorMessage = "load_failed")
                }
            }
        }
    }

    fun updateQuantity(text: String) = updateConfirm { applyQuantity(it, text) }
    fun adjustQuantity(delta: Double) {
        val current = _uiState.value.confirmState ?: return
        val value = Validators.parseDecimalInput(current.quantityText) ?: 0.0
        val step = if (current.unit == MealUnit.SERVING) 0.5 else 10.0
        val next = (value + delta * step).coerceAtLeast(0.0)
        updateQuantity(if (next % 1.0 == 0.0) next.toInt().toString() else next.toString())
    }
    fun selectUnit(unit: MealUnit) = updateConfirm { state ->
        val quantity = if (unit == MealUnit.SERVING) "1" else "100"
        applyQuantity(state.copy(unit = unit), quantity)
    }
    fun selectQuickQuantity(value: Double) =
        updateQuantity(if (value % 1.0 == 0.0) value.toInt().toString() else value.toString())
    fun updateServingGrams(text: String) = updateConfirm { applyServing(it, text) }
    fun selectMealSlot(slot: MealSlot) = updateConfirm { state ->
        state.copy(mealSlot = slot, canSubmit = validateConfirm(state.copy(mealSlot = slot)))
    }
    fun updateDate(date: LocalDate) = updateConfirm { state ->
        val next = state.copy(localDate = date)
        next.copy(canSubmit = validateConfirm(next))
    }
    fun updateTime(time: LocalTime) = updateConfirm { state ->
        val next = state.copy(time = time, mealSlot = MealSlotInferencer.infer(time))
        next.copy(canSubmit = validateConfirm(next))
    }

    fun submit() {
        val confirm = _uiState.value.confirmState ?: return
        if (!confirm.canSubmit || confirm.isSaving) return
        if (confirm.nutrition.kcal <= 0.0 && !confirm.showZeroKcalDialog) {
            _uiState.update { it.copy(confirmState = confirm.copy(showZeroKcalDialog = true)) }
            return
        }
        viewModelScope.launch {
            val userId = sessionManager.getUserId() ?: return@launch
            _uiState.update { it.copy(confirmState = confirm.copy(isSaving = true)) }
            val quantity = Validators.parseDecimalInput(confirm.quantityText) ?: return@launch
            val servingGrams = confirm.servingGramsText.takeIf { it.isNotBlank() }
                ?.let { Validators.parseDecimalInput(it) }
            val consumedAt = DateTimeUtil.combineDateAndTime(confirm.localDate, confirm.time)
            val request = UpdateMealRequest(
                entryId = entryId,
                quantity = quantity,
                unit = confirm.unit,
                servingGrams = servingGrams,
                consumedAt = consumedAt,
                mealSlot = confirm.mealSlot,
            )
            mealRepository.updateMeal(userId, request).fold(
                onSuccess = {
                    analyticsTracker.track(
                        AnalyticsEvents.MEAL_EDITED,
                        mapOf(
                            "changed_fields" to "quantity,meal_slot,time",
                            "kcal_delta" to 0,
                        ),
                    )
                    _uiState.update { it.copy(saved = true) }
                },
                onFailure = {
                    _uiState.update {
                        it.copy(confirmState = confirm.copy(isSaving = false), errorMessage = "save_failed")
                    }
                },
            )
        }
    }

    fun delete() {
        viewModelScope.launch {
            val entry = entryEntity
            mealRepository.deleteMeal(entryId).fold(
                onSuccess = {
                    entry?.let {
                        val ageHours = ((DateTimeUtil.nowEpochMillis() - it.createdAt) / 3_600_000).toInt()
                        analyticsTracker.track(
                            AnalyticsEvents.MEAL_DELETED,
                            mapOf(
                                "kcal" to it.kcal.toInt(),
                                "age_hours" to ageHours,
                            ),
                        )
                    }
                    _uiState.update { it.copy(deleted = true) }
                },
                onFailure = { _uiState.update { it.copy(errorMessage = "delete_failed") } },
            )
        }
    }

    fun dismissZeroKcalDialog() = updateConfirm { it.copy(showZeroKcalDialog = false) }
    fun confirmZeroKcal() {
        updateConfirm { it.copy(showZeroKcalDialog = false) }
        submit()
    }
    fun clearError() = _uiState.update { it.copy(errorMessage = null) }

    private fun updateConfirm(transform: (MealConfirmUiState) -> MealConfirmUiState) {
        val current = _uiState.value.confirmState ?: return
        _uiState.update { it.copy(confirmState = transform(current)) }
    }

    private fun applyQuantity(state: MealConfirmUiState, text: String): MealConfirmUiState {
        val filtered = Validators.filterDecimalInput(text)
        val next = state.copy(quantityText = filtered)
        return next.copy(
            nutrition = computeNutrition(next),
            canSubmit = validateConfirm(next),
        )
    }

    private fun applyServing(state: MealConfirmUiState, text: String): MealConfirmUiState {
        val filtered = Validators.filterDecimalInput(text)
        val next = state.copy(servingGramsText = filtered)
        return next.copy(
            nutrition = computeNutrition(next),
            canSubmit = validateConfirm(next),
        )
    }

    private fun computeNutrition(state: MealConfirmUiState): MealNutritionPreview {
        val quantity = Validators.parseDecimalInput(state.quantityText) ?: 0.0
        val servingGrams = Validators.parseDecimalInput(state.servingGramsText)
            ?: state.food.servingGrams
        return MealNutritionCalculator.compute(
            quantity = quantity,
            unit = state.unit,
            servingGrams = servingGrams,
            kcalPer100 = state.food.kcalPer100,
            proteinPer100 = state.food.proteinPer100,
            carbPer100 = state.food.carbPer100,
            fatPer100 = state.food.fatPer100,
        )
    }

    private fun validateConfirm(state: MealConfirmUiState): Boolean {
        val quantity = Validators.parseDecimalInput(state.quantityText) ?: return false
        if (quantity <= 0.0) return false
        val max = if (state.unit == MealUnit.SERVING) 50.0 else 5000.0
        if (quantity > max) return false
        if (state.unit == MealUnit.SERVING) {
            val serving = Validators.parseDecimalInput(state.servingGramsText)
                ?: state.food.servingGrams ?: return false
            if (serving <= 0.0) return false
        }
        val consumedAt = DateTimeUtil.combineDateAndTime(state.localDate, state.time)
        if (consumedAt > DateTimeUtil.nowEpochMillis()) return false
        val minDateText = _uiState.value.minDate
        if (minDateText.isNotBlank()) {
            val minDate = runCatching { DateTimeUtil.parseLocalDate(minDateText) }.getOrNull()
            if (minDate != null && state.localDate.isBefore(minDate)) return false
        }
        return true
    }

    private fun MealEntryEntity.toConfirmState(): MealConfirmUiState {
        val dateTime = DateTimeUtil.toLocalDateTime(consumedAt)
        val food = FoodSearchItem(
            foodId = foodId,
            publicFoodId = null,
            externalId = null,
            name = snapFoodName,
            brand = snapBrand,
            kcalPer100 = snapKcalPer100,
            proteinPer100 = snapProteinPer100,
            carbPer100 = snapCarbPer100,
            fatPer100 = snapFatPer100,
            basisUnit = runCatching { BasisUnit.valueOf(snapBasisUnit) }.getOrDefault(BasisUnit.G),
            servingName = snapServingName,
            servingGrams = snapServingGrams,
            source = runCatching { FoodSource.valueOf(snapSource) }.getOrDefault(FoodSource.CUSTOM),
            dataIncomplete = false,
            nutritionWarning = false,
            lastUsedAt = null,
            lastQuantity = null,
            lastUnit = null,
        )
        val unit = runCatching { MealUnit.valueOf(unit) }.getOrDefault(MealUnit.G)
        val state = MealConfirmUiState(
            food = food,
            quantityText = if (quantity % 1.0 == 0.0) quantity.toInt().toString() else quantity.toString(),
            unit = unit,
            servingGramsText = snapServingGrams?.let {
                if (it % 1.0 == 0.0) it.toInt().toString() else it.toString()
            }.orEmpty(),
            mealSlot = runCatching { MealSlot.valueOf(mealSlot) }.getOrDefault(MealSlot.LUNCH),
            localDate = dateTime.toLocalDate(),
            time = dateTime.toLocalTime(),
            nutrition = MealNutritionPreview(kcal, proteinG, carbG, fatG),
            canSubmit = true,
            isEditMode = true,
            entryId = id,
        )
        return state.copy(canSubmit = validateConfirm(state))
    }
}
