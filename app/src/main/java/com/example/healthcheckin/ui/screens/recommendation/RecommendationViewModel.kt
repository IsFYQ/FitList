package com.example.healthcheckin.ui.screens.recommendation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.healthcheckin.data.auth.SessionManager
import com.example.healthcheckin.domain.algorithm.MealSlotInferencer
import com.example.healthcheckin.domain.algorithm.RecommendationCombo
import com.example.healthcheckin.domain.algorithm.RecommendationFallback
import com.example.healthcheckin.domain.algorithm.RecommendationResult
import com.example.healthcheckin.domain.analytics.AnalyticsTracker
import com.example.healthcheckin.domain.repository.MealRepository
import com.example.healthcheckin.domain.repository.RecommendationRepository
import com.example.healthcheckin.util.DateTimeUtil
import com.example.healthcheckin.util.MealSlot
import com.example.healthcheckin.util.P2ValidationConstants
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalTime
import javax.inject.Inject

data class RecommendationComboUi(
    val combo: RecommendationCombo,
    val index: Int,
)

data class RecommendationUiState(
    val isLoading: Boolean = true,
    val localDate: String = DateTimeUtil.todayLocalDateString(),
    val result: RecommendationResult? = null,
    val displayedCombos: List<RecommendationComboUi> = emptyList(),
    val swapCount: Int = 0,
    val swapExhausted: Boolean = false,
    val confirmComboIndex: Int? = null,
    val mealSlot: MealSlot = MealSlotInferencer.infer(LocalTime.now()),
    val isLoggingMeal: Boolean = false,
    val loggedEntryIds: List<String>? = null,
    val errorMessage: String? = null,
    val toastMessage: String? = null,
)

@HiltViewModel
class RecommendationViewModel @Inject constructor(
    private val recommendationRepository: RecommendationRepository,
    private val mealRepository: MealRepository,
    private val sessionManager: SessionManager,
    private val analyticsTracker: AnalyticsTracker,
) : ViewModel() {

    private val _uiState = MutableStateFlow(RecommendationUiState())
    val uiState: StateFlow<RecommendationUiState> = _uiState.asStateFlow()

    private var alternatePool: List<RecommendationCombo> = emptyList()
    private var cachedResult: RecommendationResult? = null

    init {
        loadRecommendation()
    }

    fun loadRecommendation() {
        viewModelScope.launch {
            val userId = sessionManager.getUserId()
            if (userId == null) {
                _uiState.update { it.copy(isLoading = false, errorMessage = "recommend_error_load") }
                return@launch
            }
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val started = System.currentTimeMillis()
            val result = runCatching {
                recommendationRepository.loadRecommendation(userId, DateTimeUtil.todayLocalDateString())
            }.getOrElse {
                _uiState.update { state ->
                    state.copy(
                        isLoading = false,
                        errorMessage = "recommend_error_load",
                    )
                }
                return@launch
            }
            cachedResult = result
            alternatePool = result.alternateCombos.toMutableList()
            val elapsed = System.currentTimeMillis() - started
            analyticsTracker.track(
                "recommendation_requested",
                mapOf(
                    "gap_kcal" to result.gap.gapKcal.toInt(),
                    "candidate_count" to result.topSingles.size,
                    "combo_count" to result.combos.size,
                    "elapsed_ms" to elapsed,
                ),
            )
            analyticsTracker.track(
                "recommendation_shown",
                mapOf("fallback_type" to fallbackAnalyticsType(result.fallback)),
            )
            _uiState.update {
                it.copy(
                    isLoading = false,
                    localDate = DateTimeUtil.todayLocalDateString(),
                    result = result,
                    displayedCombos = result.combos.mapIndexed { index, combo ->
                        RecommendationComboUi(combo, index)
                    },
                    swapCount = 0,
                    swapExhausted = result.alternateCombos.isEmpty(),
                )
            }
        }
    }

    fun swapCombo(cardIndex: Int) {
        val state = _uiState.value
        if (state.swapCount >= P2ValidationConstants.RECOMMEND_SWAP_MAX) {
            _uiState.update { it.copy(toastMessage = "recommend_swap_exhausted") }
            return
        }
        val next = alternatePool.firstOrNull()
        if (next == null) {
            _uiState.update { it.copy(swapExhausted = true, toastMessage = "recommend_swap_exhausted") }
            return
        }
        alternatePool = alternatePool.drop(1)
        val updated = state.displayedCombos.toMutableList()
        if (cardIndex in updated.indices) {
            updated[cardIndex] = RecommendationComboUi(next, cardIndex)
        }
        analyticsTracker.track("recommendation_swapped", mapOf("combo_index" to cardIndex))
        _uiState.update {
            it.copy(
                displayedCombos = updated,
                swapCount = state.swapCount + 1,
                swapExhausted = alternatePool.isEmpty() ||
                    state.swapCount + 1 >= P2ValidationConstants.RECOMMEND_SWAP_MAX,
            )
        }
    }

    fun openLogMealSheet(comboIndex: Int) {
        _uiState.update {
            it.copy(
                confirmComboIndex = comboIndex,
                mealSlot = MealSlotInferencer.infer(LocalTime.now()),
            )
        }
    }

    fun dismissLogMealSheet() = _uiState.update { it.copy(confirmComboIndex = null) }

    fun setMealSlot(slot: MealSlot) = _uiState.update { it.copy(mealSlot = slot) }

    fun logMeal() {
        val comboIndex = _uiState.value.confirmComboIndex ?: return
        val result = cachedResult ?: return
        viewModelScope.launch {
            val userId = sessionManager.getUserId() ?: return@launch
            _uiState.update { it.copy(isLoggingMeal = true, errorMessage = null) }
            val patchedResult = result.copy(
                combos = _uiState.value.displayedCombos.map { it.combo },
            )
            val items = recommendationRepository.buildMealBatchItems(userId, comboIndex, patchedResult)
            if (items.isEmpty()) {
                _uiState.update {
                    it.copy(isLoggingMeal = false, errorMessage = "recommend_error_log")
                }
                return@launch
            }
            val consumedAt = DateTimeUtil.nowEpochMillis()
            mealRepository.addMealsBatch(
                userId = userId,
                items = items,
                consumedAt = consumedAt,
                mealSlot = _uiState.value.mealSlot,
            ).onSuccess { entryIds ->
                analyticsTracker.track(
                    "recommendation_accepted",
                    mapOf(
                        "combo_index" to comboIndex,
                        "item_count" to items.size,
                    ),
                )
                _uiState.update {
                    it.copy(
                        isLoggingMeal = false,
                        confirmComboIndex = null,
                        loggedEntryIds = entryIds,
                    )
                }
            }.onFailure {
                _uiState.update {
                    it.copy(
                        isLoggingMeal = false,
                        errorMessage = "recommend_error_log",
                    )
                }
            }
        }
    }

    fun clearLoggedEntryIds() = _uiState.update { it.copy(loggedEntryIds = null) }

    fun clearToast() = _uiState.update { it.copy(toastMessage = null) }

    fun clearError() = _uiState.update { it.copy(errorMessage = null) }

    private fun fallbackAnalyticsType(fallback: RecommendationFallback): String = when (fallback) {
        RecommendationFallback.NONE -> "NONE"
        RecommendationFallback.EMPTY_INVENTORY,
        RecommendationFallback.NO_NUTRITION,
        -> "GENERIC"
        RecommendationFallback.NO_COMBO -> "SINGLE_ITEM"
        else -> fallback.name
    }
}
