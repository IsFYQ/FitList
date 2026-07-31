package com.example.healthcheckin.ui.screens.dashboard

import com.example.healthcheckin.data.local.entity.MealEntryEntity
import com.example.healthcheckin.domain.model.DashboardData
import com.example.healthcheckin.domain.model.HealthWarning
import com.example.healthcheckin.domain.model.SyncBadge

enum class DashboardLoadState { LOADING, SUCCESS, ERROR }

data class DashboardUiState(
    val loadState: DashboardLoadState = DashboardLoadState.LOADING,
    val selectedDate: String = "",
    val isToday: Boolean = true,
    val minDate: String = "",
    val data: DashboardData? = null,
    val healthWarning: HealthWarning? = null,
    val healthWarningDismissed: Boolean = false,
    val syncBadge: SyncBadge? = null,
    val isRefreshing: Boolean = false,
    val snackbarMessage: String? = null,
    val undoDeleteEntry: MealEntryEntity? = null,
    val showDateLimitToast: Boolean = false,
    val showDeviceTimeWarning: Boolean = false,
    val highlightEntryId: String? = null,
)
