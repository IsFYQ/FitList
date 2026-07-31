package com.example.healthcheckin.ui.screens.customfood

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.healthcheckin.data.auth.SessionManager
import com.example.healthcheckin.domain.repository.FoodRepository
import com.example.healthcheckin.util.PrecisionUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CustomFoodListViewModel @Inject constructor(
    private val foodRepository: FoodRepository,
    private val sessionManager: SessionManager,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CustomFoodListUiState())
    val uiState: StateFlow<CustomFoodListUiState> = _uiState.asStateFlow()

    init {
        loadFoods()
    }

    fun loadFoods() {
        viewModelScope.launch {
            val userId = sessionManager.getUserId() ?: return@launch
            val items = foodRepository.listCustomFoods(userId).map { food ->
                CustomFoodListItem(
                    id = food.foodId!!,
                    name = food.name,
                    kcalPer100 = PrecisionUtil.roundCaloriesDisplay(food.kcalPer100),
                    basisUnit = food.basisUnit,
                )
            }
            _uiState.update { it.copy(items = items) }
        }
    }

    fun requestDelete(item: CustomFoodListItem) {
        viewModelScope.launch {
            val count = foodRepository.countMealReferences(item.id)
            _uiState.update { it.copy(deleteTarget = item, deleteReferenceCount = count) }
        }
    }

    fun dismissDeleteDialog() {
        _uiState.update { it.copy(deleteTarget = null, deleteReferenceCount = 0) }
    }

    fun confirmDelete() {
        val target = _uiState.value.deleteTarget ?: return
        viewModelScope.launch {
            val userId = sessionManager.getUserId() ?: return@launch
            _uiState.update { it.copy(isDeleting = true) }
            foodRepository.deleteCustomFood(userId, target.id)
            _uiState.update { it.copy(isDeleting = false, deleteTarget = null, deleteReferenceCount = 0) }
            loadFoods()
        }
    }
}
