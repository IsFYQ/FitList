package com.example.healthcheckin.ui.screens.meal

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.healthcheckin.data.auth.SessionManager
import com.example.healthcheckin.data.local.dao.ProfileDao
import com.example.healthcheckin.domain.analytics.AnalyticsEvents
import com.example.healthcheckin.domain.analytics.AnalyticsTracker
import com.example.healthcheckin.domain.model.AddMealRequest
import com.example.healthcheckin.domain.model.FoodSearchItem
import com.example.healthcheckin.domain.model.MealNutritionPreview
import com.example.healthcheckin.domain.model.SearchBanner
import com.example.healthcheckin.domain.repository.DashboardRepository
import com.example.healthcheckin.domain.repository.FoodRepository
import com.example.healthcheckin.domain.repository.MealRepository
import com.example.healthcheckin.domain.service.FoodSearchService
import com.example.healthcheckin.domain.algorithm.MealNutritionCalculator
import com.example.healthcheckin.domain.algorithm.MealSlotInferencer
import com.example.healthcheckin.util.BasisUnit
import com.example.healthcheckin.util.DateTimeUtil
import com.example.healthcheckin.util.MealEntrySource
import com.example.healthcheckin.util.MealSlot
import com.example.healthcheckin.util.MealUnit
import com.example.healthcheckin.util.NetworkMonitor
import com.example.healthcheckin.util.Validators
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject

@HiltViewModel
class MealSearchViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val foodSearchService: FoodSearchService,
    private val foodRepository: FoodRepository,
    private val mealRepository: MealRepository,
    private val inventoryRepository: com.example.healthcheckin.domain.repository.InventoryRepository,
    private val bindingRepository: com.example.healthcheckin.domain.repository.IngredientBindingRepository,
    private val dashboardRepository: DashboardRepository,
    private val sessionManager: SessionManager,
    private val profileDao: ProfileDao,
    private val analyticsTracker: AnalyticsTracker,
    private val networkMonitor: NetworkMonitor,
) : ViewModel() {

    private val routeLocalDate: String = checkNotNull(savedStateHandle["localDate"]) {
        "localDate argument required"
    }
    private val _uiState = MutableStateFlow(MealSearchUiState())
    val uiState: StateFlow<MealSearchUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null
    private var mealFlowStartedAt: Long? = null

    init {
        viewModelScope.launch {
            val userId = sessionManager.getUserId() ?: return@launch
            val profile = profileDao.getById(userId)
            val minDate = dashboardRepository.getMinViewDate(
                profile?.registeredLocalDate ?: DateTimeUtil.todayLocalDateString(),
            )
            val today = DateTimeUtil.todayLocalDateString()
            _uiState.update {
                it.copy(
                    localDate = routeLocalDate,
                    minDate = minDate,
                    isBackfill = routeLocalDate != today,
                )
            }
            loadRecentFrequent(userId)
        }
    }

    fun onPendingFoodSelected(foodId: String) {
        viewModelScope.launch {
            val userId = sessionManager.getUserId() ?: return@launch
            val food = foodRepository.getFoodSearchItem(userId, foodId) ?: return@launch
            selectFood(food, fromRecent = false)
        }
    }

    fun onQueryChange(query: String) {
        _uiState.update { it.copy(query = query) }
        searchJob?.cancel()
        if (query.isBlank()) {
            _uiState.update { it.copy(searchResults = emptyList(), banner = SearchBanner.NONE, isSearching = false) }
            return
        }
        searchJob = viewModelScope.launch {
            delay(300)
            val userId = sessionManager.getUserId() ?: return@launch
            val trimmed = query.trim()
            val startedAt = System.currentTimeMillis()
            _uiState.update { it.copy(isSearching = true) }

            val local = foodSearchService.searchLocal(trimmed, userId)
            val needsRemote = trimmed.length >= 2 && local.banner == SearchBanner.REMOTE_LOADING
            _uiState.update {
                it.copy(
                    searchResults = local.items,
                    banner = local.banner,
                    isSearching = needsRemote,
                )
            }

            val remoteAppended = if (needsRemote) {
                val remote = foodSearchService.fetchRemote(trimmed, userId, local.items)
                _uiState.update {
                    it.copy(
                        searchResults = local.items + remote.appendedItems,
                        banner = remote.banner,
                        isSearching = false,
                    )
                }
                remote.appendedItems
            } else {
                _uiState.update { it.copy(isSearching = false) }
                emptyList()
            }

            val finalResults = local.items + remoteAppended
            val localCount = local.items.size
            analyticsTracker.track(
                AnalyticsEvents.FOOD_SEARCH_PERFORMED,
                mapOf(
                    "query_length" to trimmed.length,
                    "result_count" to finalResults.size,
                    "local_count" to localCount,
                    "remote_count" to remoteAppended.size,
                    "elapsed_ms" to (System.currentTimeMillis() - startedAt).toInt(),
                    "is_cache_hit" to (local.banner == SearchBanner.FROM_CACHE),
                    "is_offline" to !networkMonitor.isOnline(),
                    "fatsecret_status" to if (needsRemote) "OK" else "SKIPPED",
                    "off_status" to if (needsRemote) "OK" else "SKIPPED",
                ),
            )
        }
    }

    fun selectFood(food: FoodSearchItem, fromRecent: Boolean = false) {
        mealFlowStartedAt = System.currentTimeMillis()
        val confirm = buildInitialConfirmState(food, fromRecent)
        _uiState.update { it.copy(selectedFood = food, selectedFromRecent = fromRecent, confirmState = confirm) }
        refreshInventoryMatch()
    }

    fun dismissConfirm() {
        _uiState.update { it.copy(selectedFood = null, selectedFromRecent = false, confirmState = null) }
    }

    fun updateQuantity(text: String) {
        updateConfirm { state ->
            val filtered = Validators.filterDecimalInput(text)
            state.copy(
                quantityText = filtered,
                nutrition = computeNutrition(state.copy(quantityText = filtered)),
                canSubmit = validateConfirm(state.copy(quantityText = filtered)),
            )
        }
        refreshInventoryMatch()
    }

    fun adjustQuantity(delta: Double) {
        val current = _uiState.value.confirmState ?: return
        val value = Validators.parseDecimalInput(current.quantityText) ?: 0.0
        val step = if (current.unit == MealUnit.SERVING) 0.5 else 10.0
        val next = (value + delta * step).coerceAtLeast(0.0)
        updateQuantity(formatQuantity(next))
    }

    fun selectUnit(unit: MealUnit) {
        updateConfirm { state ->
            val quantity = defaultQuantityForUnit(state.food, unit)
            state.copy(
                unit = unit,
                quantityText = quantity,
                nutrition = computeNutrition(state.copy(unit = unit, quantityText = quantity)),
                canSubmit = validateConfirm(state.copy(unit = unit, quantityText = quantity)),
            )
        }
    }

    fun selectQuickQuantity(value: Double) {
        updateQuantity(formatQuantity(value))
    }

    fun updateServingGrams(text: String) {
        updateConfirm { state ->
            val filtered = Validators.filterDecimalInput(text)
            state.copy(
                servingGramsText = filtered,
                nutrition = computeNutrition(state.copy(servingGramsText = filtered)),
                canSubmit = validateConfirm(state.copy(servingGramsText = filtered)),
            )
        }
    }

    fun selectMealSlot(slot: MealSlot) {
        updateConfirm { state ->
            val time = if (_uiState.value.isBackfill) {
                DateTimeUtil.mealSlotMidpointTime(slot)
            } else {
                state.time
            }
            state.copy(mealSlot = slot, time = time)
        }
    }

    fun updateDate(date: LocalDate) {
        updateConfirm { it.copy(localDate = date) }
    }

    fun updateTime(time: LocalTime) {
        updateConfirm { it.copy(time = time, mealSlot = MealSlotInferencer.infer(time)) }
    }

    fun submit() {
        submit(fromRecent = _uiState.value.selectedFromRecent)
    }

    fun submit(fromRecent: Boolean = false) {
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
            val request = AddMealRequest(
                food = confirm.food,
                quantity = quantity,
                unit = confirm.unit,
                servingGrams = servingGrams,
                consumedAt = consumedAt,
                mealSlot = confirm.mealSlot,
                entrySource = if (fromRecent) MealEntrySource.RECENT else MealEntrySource.SEARCH,
                inventoryItemId = if (confirm.deductChecked) confirm.inventoryMatch?.item?.id else null,
                deductChoice = if (confirm.deductChecked && confirm.inventoryMatch?.item != null) {
                    com.example.healthcheckin.domain.model.InventoryDeductChoice(
                        resolution = if (confirm.inventoryPreview?.insufficient == true) {
                            com.example.healthcheckin.util.InventoryDeductResolution.DEDUCT_REMAINING
                        } else {
                            com.example.healthcheckin.util.InventoryDeductResolution.DEDUCT_REMAINING
                        },
                    )
                } else null,
            )
            if (confirm.deductChecked && confirm.inventoryPreview?.insufficient == true && !confirm.showInsufficientDialog) {
                _uiState.update { it.copy(confirmState = confirm.copy(isSaving = false, showInsufficientDialog = true)) }
                return@launch
            }
            if (confirm.deductChecked && confirm.inventoryMatch?.level == com.example.healthcheckin.util.InventoryMatchLevel.L3 && !confirm.showL3Confirm && confirm.deductChecked) {
                // L3 already confirmed via checkbox path below
            }
            mealRepository.addMeal(userId, request).fold(
                onSuccess = { entry ->
                    val durationMs = mealFlowStartedAt?.let {
                        (System.currentTimeMillis() - it).toInt()
                    } ?: 0
                    analyticsTracker.track(
                        AnalyticsEvents.MEAL_LOGGED,
                        mapOf(
                            "meal_slot" to confirm.mealSlot.name,
                            "kcal" to entry.kcal.toInt(),
                            "unit" to confirm.unit.name,
                            "duration_ms" to durationMs,
                            "entry_source" to (if (fromRecent) MealEntrySource.RECENT else MealEntrySource.SEARCH).name,
                            "food_source" to entry.snapSource,
                            "is_backfill" to _uiState.value.isBackfill,
                            "from_inventory" to (entry.fromInventory),
                            "inventory_match_level" to (confirm.inventoryMatch?.level?.name ?: "NONE"),
                        ),
                    )
                    mealFlowStartedAt = null
                    loadRecentFrequent(userId)
                    _uiState.update {
                        it.copy(
                            confirmState = null,
                            selectedFood = null,
                            savedEntryId = entry.id,
                        )
                    }
                },
                onFailure = {
                    _uiState.update {
                        it.copy(
                            confirmState = confirm.copy(isSaving = false),
                            errorMessage = "save_failed",
                        )
                    }
                },
            )
        }
    }

    fun dismissZeroKcalDialog() {
        updateConfirm { it.copy(showZeroKcalDialog = false) }
    }

    fun confirmZeroKcal() {
        updateConfirm { it.copy(showZeroKcalDialog = false) }
        submit()
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun clearSavedEntry() {
        _uiState.update { it.copy(savedEntryId = null) }
    }

    fun toggleDeduct(checked: Boolean) {
        val confirm = _uiState.value.confirmState ?: return
        if (checked && confirm.inventoryMatch?.level == com.example.healthcheckin.util.InventoryMatchLevel.L3) {
            updateConfirm { it.copy(deductChecked = false, showL3Confirm = true) }
            return
        }
        updateConfirm { it.copy(deductChecked = checked) }
    }

    fun confirmL3Deduct() = updateConfirm { it.copy(showL3Confirm = false, deductChecked = true) }
    fun dismissL3Confirm() = updateConfirm { it.copy(showL3Confirm = false, deductChecked = false) }

    fun resolveInsufficient(resolution: com.example.healthcheckin.util.InventoryDeductResolution, manualAmount: Double? = null) {
        updateConfirm {
            it.copy(
                showInsufficientDialog = false,
                deductChecked = resolution != com.example.healthcheckin.util.InventoryDeductResolution.SKIP,
            )
        }
        val confirm = _uiState.value.confirmState ?: return
        viewModelScope.launch {
            val userId = sessionManager.getUserId() ?: return@launch
            val quantity = Validators.parseDecimalInput(confirm.quantityText) ?: return@launch
            val servingGrams = confirm.servingGramsText.takeIf { it.isNotBlank() }?.let { Validators.parseDecimalInput(it) }
            val consumedAt = DateTimeUtil.combineDateAndTime(confirm.localDate, confirm.time)
            mealRepository.addMeal(
                userId,
                AddMealRequest(
                    food = confirm.food,
                    quantity = quantity,
                    unit = confirm.unit,
                    servingGrams = servingGrams,
                    consumedAt = consumedAt,
                    mealSlot = confirm.mealSlot,
                    entrySource = if (_uiState.value.selectedFromRecent) MealEntrySource.RECENT else MealEntrySource.SEARCH,
                    inventoryItemId = if (resolution == com.example.healthcheckin.util.InventoryDeductResolution.SKIP) null else confirm.inventoryMatch?.item?.id,
                    deductChoice = if (resolution == com.example.healthcheckin.util.InventoryDeductResolution.SKIP) null else {
                        com.example.healthcheckin.domain.model.InventoryDeductChoice(resolution, manualAmount)
                    },
                ),
            ).onSuccess { entry ->
                _uiState.update { it.copy(confirmState = null, selectedFood = null, savedEntryId = entry.id) }
            }.onFailure {
                _uiState.update { it.copy(errorMessage = "save_failed") }
            }
        }
    }

    fun openInventoryPicker() {
        viewModelScope.launch {
            val userId = sessionManager.getUserId() ?: return@launch
            val items = inventoryRepository.observeItems(userId)
            items.first().let { list ->
                updateConfirm {
                    it.copy(
                        showInventoryPicker = true,
                        inventoryCandidates = list.filter { item -> item.remainingAmount > 0 },
                    )
                }
            }
        }
    }

    fun selectInventoryItem(itemId: String) {
        viewModelScope.launch {
            val userId = sessionManager.getUserId() ?: return@launch
            val confirm = _uiState.value.confirmState ?: return@launch
            val foodId = confirm.food.foodId
            if (foodId != null) {
                bindingRepository.bind(userId, foodId, itemId)
                analyticsTracker.track(AnalyticsEvents.INGREDIENT_BINDING_CREATED, mapOf("trigger" to "MEAL_PAGE"))
            }
            updateConfirm { it.copy(showInventoryPicker = false, deductChecked = true) }
            refreshInventoryMatch(forcedItemId = itemId)
        }
    }

    fun dismissInventoryPicker() = updateConfirm { it.copy(showInventoryPicker = false) }

    private fun refreshInventoryMatch(forcedItemId: String? = null) {
        viewModelScope.launch {
            val confirm = _uiState.value.confirmState ?: return@launch
            val userId = sessionManager.getUserId() ?: return@launch
            val quantity = Validators.parseDecimalInput(confirm.quantityText) ?: return@launch
            val serving = effectiveServingGrams(confirm)
            val basis = com.example.healthcheckin.domain.algorithm.MealNutritionCalculator.basisAmount(
                quantity, confirm.unit, serving,
            )
            val match = if (forcedItemId != null) {
                val item = inventoryRepository.getById(forcedItemId)
                if (item != null) {
                    com.example.healthcheckin.domain.model.InventoryMatchResult(
                        com.example.healthcheckin.util.InventoryMatchLevel.L1,
                        1.0,
                        item,
                        item.name,
                    )
                } else {
                    inventoryRepository.matchForFood(
                        userId, confirm.food.foodId, confirm.food.name, confirm.food.basisUnit, basis,
                    )
                }
            } else {
                inventoryRepository.matchForFood(
                    userId, confirm.food.foodId, confirm.food.name, confirm.food.basisUnit, basis,
                )
            }
            val preview = match.item?.id?.let {
                inventoryRepository.previewDeduct(it, basis, confirm.food.basisUnit)
            }
            updateConfirm {
                it.copy(
                    inventoryMatch = match,
                    inventoryPreview = preview,
                    deductChecked = if (it.inventoryMatch?.item?.id == match.item?.id) {
                        it.deductChecked || match.confidence >= 0.90
                    } else {
                        match.confidence >= 0.90
                    },
                )
            }
        }
    }

    private fun loadRecentFrequent(userId: String) {
        viewModelScope.launch {
            val data = foodSearchService.getRecentAndFrequentFoods(userId)
            _uiState.update { it.copy(recentFrequent = data) }
        }
    }

    private fun buildInitialConfirmState(food: FoodSearchItem, fromRecent: Boolean): MealConfirmUiState {
        val isBackfill = _uiState.value.isBackfill
        val localDate = DateTimeUtil.parseLocalDate(_uiState.value.localDate)
        val now = java.time.LocalDateTime.now(DateTimeUtil.zoneId())
        val time = if (isBackfill) {
            val slot = MealSlotInferencer.infer(now.toLocalTime())
            DateTimeUtil.mealSlotMidpointTime(slot)
        } else {
            now.toLocalTime()
        }
        val mealSlot = if (isBackfill) {
            MealSlotInferencer.infer(DateTimeUtil.mealSlotMidpointTime(MealSlot.LUNCH))
        } else {
            MealSlotInferencer.infer(time)
        }
        val defaultUnit = food.lastUnit ?: when (food.basisUnit) {
            BasisUnit.ML -> MealUnit.ML
            BasisUnit.G -> MealUnit.G
        }
        val quantityText = food.lastQuantity?.let { formatQuantity(it) }
            ?: defaultQuantityForUnit(food, defaultUnit)
        val state = MealConfirmUiState(
            food = food,
            quantityText = quantityText,
            unit = defaultUnit,
            servingGramsText = food.servingGrams?.let { formatQuantity(it) }.orEmpty(),
            mealSlot = if (fromRecent && food.lastMealSlot != null) {
                food.lastMealSlot!!
            } else {
                mealSlot
            },
            localDate = localDate,
            time = if (isBackfill) DateTimeUtil.mealSlotMidpointTime(
                if (fromRecent && food.lastMealSlot != null) food.lastMealSlot!! else mealSlot,
            ) else time,
            nutrition = MealNutritionPreview(0.0, null, null, null),
            canSubmit = false,
        )
        return state.copy(
            nutrition = computeNutrition(state),
            canSubmit = validateConfirm(state),
        )
    }

    private fun updateConfirm(transform: (MealConfirmUiState) -> MealConfirmUiState) {
        val current = _uiState.value.confirmState ?: return
        _uiState.update { it.copy(confirmState = transform(current)) }
    }

    private fun computeNutrition(state: MealConfirmUiState): MealNutritionPreview {
        val quantity = Validators.parseDecimalInput(state.quantityText) ?: 0.0
        val servingGrams = effectiveServingGrams(state)
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

    private fun effectiveServingGrams(state: MealConfirmUiState): Double? {
        val input = Validators.parseDecimalInput(state.servingGramsText)
        return input ?: state.food.servingGrams
    }

    private fun validateConfirm(state: MealConfirmUiState): Boolean {
        val quantity = Validators.parseDecimalInput(state.quantityText) ?: return false
        if (quantity <= 0.0) return false
        val max = if (state.unit == MealUnit.SERVING) 50.0 else 5000.0
        if (quantity > max) return false
        if (state.food.dataIncomplete || state.unit == MealUnit.SERVING) {
            val serving = effectiveServingGrams(state) ?: return false
            if (serving <= 0.0) return false
        }
        val consumedAt = DateTimeUtil.combineDateAndTime(state.localDate, state.time)
        if (consumedAt > DateTimeUtil.nowEpochMillis()) return false
        val minDate = DateTimeUtil.parseLocalDate(_uiState.value.minDate)
        if (state.localDate.isBefore(minDate)) return false
        return true
    }

    private fun defaultQuantityForUnit(food: FoodSearchItem, unit: MealUnit): String = when (unit) {
        MealUnit.SERVING -> "1"
        else -> "100"
    }

    private fun formatQuantity(value: Double): String {
        return if (value % 1.0 == 0.0) value.toInt().toString() else value.toString()
    }
}
