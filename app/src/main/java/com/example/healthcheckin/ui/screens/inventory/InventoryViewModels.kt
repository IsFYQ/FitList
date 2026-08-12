package com.example.healthcheckin.ui.screens.inventory

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.healthcheckin.data.auth.SessionManager
import com.example.healthcheckin.domain.analytics.AnalyticsEvents
import com.example.healthcheckin.domain.analytics.AnalyticsTracker
import com.example.healthcheckin.domain.model.InventoryItem
import com.example.healthcheckin.domain.model.SaveInventoryRequest
import com.example.healthcheckin.domain.model.UpdateInventoryRequest
import com.example.healthcheckin.domain.repository.IngredientBindingRepository
import com.example.healthcheckin.domain.repository.InventoryRepository
import com.example.healthcheckin.util.DateTimeUtil
import com.example.healthcheckin.util.InventoryCategory
import com.example.healthcheckin.util.InventoryExpiryStatus
import com.example.healthcheckin.util.InventorySortMode
import com.example.healthcheckin.util.InventoryUnit
import com.example.healthcheckin.util.Validators
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class InventoryListUiState(
    val items: List<InventoryItem> = emptyList(),
    val query: String = "",
    val sortMode: InventorySortMode = InventorySortMode.BY_CATEGORY,
    val deleteConfirm: Pair<InventoryItem, Int>? = null,
)

data class InventoryFormUiState(
    val itemId: String? = null,
    val name: String = "",
    val category: InventoryCategory = InventoryCategory.OTHER,
    val amountText: String = "",
    val remainingText: String = "",
    val unit: InventoryUnit = InventoryUnit.G,
    val pieceGramsText: String = "",
    val purchaseDate: String = DateTimeUtil.todayLocalDateString(),
    val expiryDate: String = "",
    val unitPriceText: String = "",
    val suggestions: List<String> = emptyList(),
    val unitLocked: Boolean = false,
    val errorMessage: String? = null,
    val isSaving: Boolean = false,
    val saved: Boolean = false,
)

@HiltViewModel
class InventoryListViewModel @Inject constructor(
    private val inventoryRepository: InventoryRepository,
    private val sessionManager: SessionManager,
    private val analyticsTracker: AnalyticsTracker,
) : ViewModel() {
    private val _uiState = MutableStateFlow(InventoryListUiState())
    val uiState: StateFlow<InventoryListUiState> = _uiState.asStateFlow()
    private var allItems: List<InventoryItem> = emptyList()

    init {
        viewModelScope.launch {
            val userId = sessionManager.getUserId() ?: return@launch
            analyticsTracker.track(AnalyticsEvents.INVENTORY_LIST_VIEWED)
            inventoryRepository.observeItems(userId).collect { items ->
                allItems = items
                applyFilter()
            }
        }
    }

    fun updateQuery(q: String) {
        _uiState.update { it.copy(query = q) }
        applyFilter()
    }

    fun setSort(mode: InventorySortMode) {
        _uiState.update { it.copy(sortMode = mode) }
        applyFilter()
    }

    private fun applyFilter() {
        val q = Validators.normalizeFoodName(_uiState.value.query)
        var list = allItems.filter {
            q.isEmpty() || Validators.normalizeFoodName(it.name).contains(q)
        }
        list = when (_uiState.value.sortMode) {
            InventorySortMode.BY_CATEGORY -> list.sortedWith(
                compareBy<InventoryItem> { it.category.ordinal }
                    .thenBy { if (it.remainingAmount <= 0) 1 else 0 }
                    .thenBy { it.purchaseDate },
            )
            InventorySortMode.BY_EXPIRY -> list.sortedWith(
                compareBy<InventoryItem> { it.daysLeft ?: Int.MAX_VALUE }
                    .thenBy { it.name },
            )
            InventorySortMode.BY_RECENT -> list.sortedByDescending { it.purchaseDate }
        }
        _uiState.update { it.copy(items = list) }
    }

    fun markUsedUp(item: InventoryItem) {
        viewModelScope.launch {
            inventoryRepository.markUsedUp(item.id)
            analyticsTracker.track(AnalyticsEvents.INVENTORY_ITEM_USED_UP)
        }
    }

    fun requestDelete(item: InventoryItem) {
        viewModelScope.launch {
            // delete returns Pair<item, linkedCount>; we call delete which needs confirm if linked
            // Use provisional confirm with 0, real count from delete result path
            _uiState.update { it.copy(deleteConfirm = item to 0) }
        }
    }

    fun dismissDelete() = _uiState.update { it.copy(deleteConfirm = null) }

    fun confirmDelete() {
        val target = _uiState.value.deleteConfirm?.first ?: return
        viewModelScope.launch {
            inventoryRepository.delete(target.id).onSuccess {
                analyticsTracker.track(AnalyticsEvents.INVENTORY_ITEM_DELETED)
            }
            _uiState.update { it.copy(deleteConfirm = null) }
        }
    }
}

@HiltViewModel
class InventoryFormViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val inventoryRepository: InventoryRepository,
    private val bindingRepository: IngredientBindingRepository,
    private val sessionManager: SessionManager,
    private val analyticsTracker: AnalyticsTracker,
) : ViewModel() {
    private val itemId: String? = savedStateHandle["itemId"]
    private val _uiState = MutableStateFlow(InventoryFormUiState(itemId = itemId, unitLocked = itemId != null))
    val uiState: StateFlow<InventoryFormUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            bindingRepository.ensureAliasesSeeded()
            val id = itemId ?: return@launch
            val item = inventoryRepository.getById(id) ?: return@launch
            _uiState.update {
                it.copy(
                    name = item.name,
                    category = item.category,
                    amountText = item.initialAmount.toString(),
                    remainingText = item.remainingAmount.toString(),
                    unit = item.unit,
                    pieceGramsText = item.pieceGrams?.toString().orEmpty(),
                    purchaseDate = item.purchaseDate,
                    expiryDate = item.expiryDate.orEmpty(),
                    unitPriceText = item.unitPrice?.toString().orEmpty(),
                    unitLocked = true,
                )
            }
        }
    }

    fun updateName(v: String) {
        _uiState.update { it.copy(name = v.take(50)) }
        viewModelScope.launch {
            val userId = sessionManager.getUserId() ?: return@launch
            val suggestions = inventoryRepository.suggestNames(userId, Validators.normalizeFoodName(v))
            _uiState.update { it.copy(suggestions = suggestions) }
        }
    }

    fun updateCategory(c: InventoryCategory) = _uiState.update { it.copy(category = c) }
    fun updateAmount(v: String) = _uiState.update { it.copy(amountText = Validators.filterDecimalInput(v)) }
    fun updateRemaining(v: String) = _uiState.update { it.copy(remainingText = Validators.filterDecimalInput(v)) }
    fun updateUnit(u: InventoryUnit) {
        if (_uiState.value.unitLocked) return
        _uiState.update { it.copy(unit = u) }
    }
    fun updatePieceGrams(v: String) = _uiState.update { it.copy(pieceGramsText = Validators.filterDecimalInput(v)) }
    fun updatePurchaseDate(v: String) = _uiState.update { it.copy(purchaseDate = v) }
    fun updateExpiryDate(v: String) = _uiState.update { it.copy(expiryDate = v) }
    fun updateUnitPrice(v: String) = _uiState.update { it.copy(unitPriceText = Validators.filterDecimalInput(v)) }
    fun clearError() = _uiState.update { it.copy(errorMessage = null) }

    fun save() {
        val state = _uiState.value
        if (state.isSaving) return
        val name = state.name.trim()
        if (name.isEmpty()) return
        val amount = Validators.parseDecimalInput(if (state.itemId == null) state.amountText else state.remainingText) ?: return
        if (amount <= 0 && state.itemId == null) {
            _uiState.update { it.copy(errorMessage = "amount") }
            return
        }
        if (state.expiryDate.isNotBlank() && state.expiryDate < state.purchaseDate) {
            _uiState.update { it.copy(errorMessage = "expiry") }
            return
        }
        viewModelScope.launch {
            val userId = sessionManager.getUserId() ?: return@launch
            _uiState.update { it.copy(isSaving = true) }
            val piece = state.pieceGramsText.takeIf { it.isNotBlank() }?.let { Validators.parseDecimalInput(it) }
            val price = state.unitPriceText.takeIf { it.isNotBlank() }?.let { Validators.parseDecimalInput(it) }
            val result = if (state.itemId == null) {
                inventoryRepository.create(
                    userId,
                    SaveInventoryRequest(
                        name = name,
                        category = state.category,
                        amount = amount,
                        unit = state.unit,
                        pieceGrams = piece,
                        purchaseDate = state.purchaseDate,
                        expiryDate = state.expiryDate.takeIf { it.isNotBlank() },
                        unitPrice = price,
                    ),
                ).onSuccess { analyticsTracker.track(AnalyticsEvents.INVENTORY_ITEM_CREATED, mapOf("category" to state.category.name, "unit" to state.unit.name)) }
            } else {
                inventoryRepository.update(
                    userId,
                    UpdateInventoryRequest(
                        itemId = state.itemId,
                        name = name,
                        category = state.category,
                        remainingAmount = amount.coerceAtLeast(0.0),
                        pieceGrams = piece,
                        purchaseDate = state.purchaseDate,
                        expiryDate = state.expiryDate.takeIf { it.isNotBlank() },
                        unitPrice = price,
                    ),
                ).onSuccess { analyticsTracker.track(AnalyticsEvents.INVENTORY_ITEM_EDITED) }
            }
            result.fold(
                onSuccess = { _uiState.update { it.copy(isSaving = false, saved = true) } },
                onFailure = { _uiState.update { it.copy(isSaving = false, errorMessage = "save_failed") } },
            )
        }
    }
}
