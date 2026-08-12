package com.example.healthcheckin.ui.screens.dashboard

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.healthcheckin.data.auth.SessionManager
import com.example.healthcheckin.data.local.dao.ProfileDao
import com.example.healthcheckin.data.local.entity.MealEntryEntity
import com.example.healthcheckin.domain.model.HealthWarning
import com.example.healthcheckin.domain.model.ruleId
import com.example.healthcheckin.domain.model.showBackfillAction
import com.example.healthcheckin.domain.analytics.AnalyticsEvents
import com.example.healthcheckin.domain.analytics.AnalyticsTracker
import com.example.healthcheckin.domain.repository.DashboardRepository
import com.example.healthcheckin.util.DateTimeUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val dashboardRepository: DashboardRepository,
    private val sessionManager: SessionManager,
    private val profileDao: ProfileDao,
    private val analyticsTracker: AnalyticsTracker,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val today = DateTimeUtil.todayLocalDateString()

    private val _uiState = MutableStateFlow(DashboardUiState(selectedDate = today))
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    private val selectedDateFlow = MutableStateFlow(today)
    private var undoJob: Job? = null
    private var dashboardTracked = false
    private var lastShownHealthTip: com.example.healthcheckin.domain.model.HealthWarningType? = null

    init {
        savedStateHandle.getStateFlow<String?>("highlightEntryId", null)
            .onEach { entryId ->
                if (entryId != null) {
                    _uiState.update { it.copy(highlightEntryId = entryId) }
                    savedStateHandle.remove<String>("highlightEntryId")
                    viewModelScope.launch {
                        delay(300)
                        _uiState.update { it.copy(highlightEntryId = null) }
                    }
                }
            }
            .launchIn(viewModelScope)

        viewModelScope.launch {
            val userId = sessionManager.getUserId() ?: return@launch
            dashboardRepository.ensureTodayBudget(userId)
            val profile = profileDao.getById(userId)
            val minDate = dashboardRepository.getMinViewDate(
                profile?.registeredLocalDate ?: DateTimeUtil.todayLocalDateString()
            )
            _uiState.update {
                it.copy(
                    selectedDate = DateTimeUtil.todayLocalDateString(),
                    isToday = true,
                    minDate = minDate,
                )
            }

            selectedDateFlow.flatMapLatest { date ->
                dashboardRepository.observeDashboard(userId, date)
            }.onEach { data ->
                _uiState.update {
                    it.copy(
                        loadState = DashboardLoadState.SUCCESS,
                        data = data,
                    )
                }
                if (!dashboardTracked) {
                    dashboardTracked = true
                    analyticsTracker.track(
                        AnalyticsEvents.DASHBOARD_VIEWED,
                        mapOf(
                            "entry_count" to data.consumption.entryCount,
                            "is_today" to (_uiState.value.selectedDate == DateTimeUtil.todayLocalDateString()),
                        ),
                    )
                }
                if (DateTimeUtil.todayLocalDateString() == _uiState.value.selectedDate) {
                    loadHealthWarning(userId, data.consumption.entryCount)
                }
            }.catch {
                _uiState.update { state -> state.copy(loadState = DashboardLoadState.ERROR) }
            }.launchIn(viewModelScope)

            _uiState.update {
                it.copy(showDeviceTimeWarning = dashboardRepository.isDeviceTimeSuspicious())
            }
        }
    }

    fun goToPreviousDay() {
        val current = LocalDate.parse(_uiState.value.selectedDate)
        val min = LocalDate.parse(_uiState.value.minDate)
        val previous = current.minusDays(1)
        if (previous.isBefore(min)) {
            _uiState.update { it.copy(showDateLimitToast = true) }
            return
        }
        changeDate(DateTimeUtil.formatLocalDate(previous))
    }

    fun goToNextDay() {
        val current = LocalDate.parse(_uiState.value.selectedDate)
        val today = DateTimeUtil.todayLocalDate()
        val next = current.plusDays(1)
        if (next.isAfter(today)) return
        changeDate(DateTimeUtil.formatLocalDate(next))
    }

    fun goToToday() {
        changeDate(DateTimeUtil.todayLocalDateString())
    }

    fun dismissHealthWarning() {
        val warning = _uiState.value.healthWarning ?: return
        viewModelScope.launch {
            dashboardRepository.dismissHealthWarning(warning.type)
            analyticsTracker.track(
                AnalyticsEvents.HEALTH_TIP_DISMISSED,
                mapOf("rule_id" to warning.type.ruleId),
            )
            _uiState.update { it.copy(healthWarning = null, healthWarningDismissed = true) }
        }
    }

    fun onHealthWarningBackfill(onNavigate: () -> Unit) {
        val warning = _uiState.value.healthWarning ?: return
        if (!warning.type.showBackfillAction) return
        analyticsTracker.track(
            AnalyticsEvents.HEALTH_TIP_ACTION_CLICKED,
            mapOf("rule_id" to warning.type.ruleId),
        )
        onNavigate()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }
            sessionManager.getUserId()?.let { dashboardRepository.ensureTodayBudget(it) }
            delay(500)
            _uiState.update {
                it.copy(
                    isRefreshing = false,
                    showDeviceTimeWarning = dashboardRepository.isDeviceTimeSuspicious(),
                )
            }
        }
    }

    fun deleteMeal(entry: MealEntryEntity) {
        viewModelScope.launch {
            dashboardRepository.deleteMealEntry(entry)
            undoJob?.cancel()
            _uiState.update {
                it.copy(
                    undoDeleteEntry = entry,
                    snackbarMessage = "deleted:${entry.snapFoodName}",
                )
            }
            undoJob = launch {
                delay(5000)
                _uiState.update { it.copy(undoDeleteEntry = null) }
            }
        }
    }

    fun undoDelete() {
        val entry = _uiState.value.undoDeleteEntry ?: return
        viewModelScope.launch {
            dashboardRepository.undoDeleteMealEntry(entry).fold(
                onSuccess = {
                    undoJob?.cancel()
                    _uiState.update { it.copy(undoDeleteEntry = null, snackbarMessage = null) }
                },
                onFailure = {
                    _uiState.update { it.copy(snackbarMessage = "undo_failed") }
                },
            )
        }
    }

    fun clearSnackbar() {
        _uiState.update { it.copy(snackbarMessage = null, showDateLimitToast = false) }
    }

    fun retryLoad() {
        _uiState.update { it.copy(loadState = DashboardLoadState.LOADING) }
        changeDate(_uiState.value.selectedDate)
    }

    private fun changeDate(date: String) {
        val today = DateTimeUtil.todayLocalDateString()
        _uiState.update {
            it.copy(
                selectedDate = date,
                isToday = date == today,
                loadState = DashboardLoadState.LOADING,
            )
        }
        selectedDateFlow.value = date
    }

    private suspend fun loadHealthWarning(userId: String, todayEntryCount: Int) {
        val warning = dashboardRepository.evaluateHealthWarning(userId, todayEntryCount)
        _uiState.update { it.copy(healthWarning = warning, healthWarningDismissed = false) }
        if (warning != null && lastShownHealthTip != warning.type) {
            lastShownHealthTip = warning.type
            analyticsTracker.track(
                AnalyticsEvents.HEALTH_TIP_SHOWN,
                mapOf("rule_id" to warning.type.ruleId),
            )
        }
    }
}
