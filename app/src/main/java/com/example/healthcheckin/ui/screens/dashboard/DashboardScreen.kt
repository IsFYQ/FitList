package com.example.healthcheckin.ui.screens.dashboard

import android.widget.Toast
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.healthcheckin.R
import com.example.healthcheckin.domain.model.SyncBadgeType
import com.example.healthcheckin.domain.model.showBackfillAction
import com.example.healthcheckin.ui.screens.dashboard.components.CalorieCard
import com.example.healthcheckin.ui.screens.dashboard.components.DashboardSkeleton
import com.example.healthcheckin.ui.screens.dashboard.components.DeviceTimeWarningBanner
import com.example.healthcheckin.ui.screens.dashboard.components.HealthWarningBanner
import com.example.healthcheckin.ui.screens.dashboard.components.MacroProgressSection
import com.example.healthcheckin.ui.screens.dashboard.components.MealListSection
import com.example.healthcheckin.ui.screens.dashboard.components.WeightCard
import com.example.healthcheckin.util.DateTimeUtil
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onNavigateSettings: () -> Unit,
    onNavigateDiagnostics: () -> Unit = onNavigateSettings,
    onNavigateOnboarding: () -> Unit,
    onNavigateAddMeal: (localDate: String) -> Unit,
    onNavigateMealEdit: (entryId: String) -> Unit,
    onNavigateWeight: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(uiState.snackbarMessage, uiState.undoDeleteEntry) {
        val key = uiState.snackbarMessage ?: return@LaunchedEffect
        val message = when {
            key.startsWith("deleted:") -> context.getString(
                R.string.dashboard_meal_deleted_name,
                key.removePrefix("deleted:"),
            )
            key == "undo_failed" -> context.getString(R.string.dashboard_undo_failed)
            else -> key
        }
        val result = snackbarHostState.showSnackbar(
            message = message,
            actionLabel = if (uiState.undoDeleteEntry != null) {
                context.getString(R.string.dashboard_undo_delete)
            } else {
                null
            },
            duration = androidx.compose.material3.SnackbarDuration.Long,
        )
        if (result == SnackbarResult.ActionPerformed) {
            viewModel.undoDelete()
        }
        viewModel.clearSnackbar()
    }

    LaunchedEffect(uiState.showDateLimitToast) {
        if (uiState.showDateLimitToast) {
            Toast.makeText(context, context.getString(R.string.dashboard_date_limit_toast), Toast.LENGTH_SHORT).show()
            viewModel.clearSnackbar()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(DateTimeUtil.formatDashboardDate(uiState.selectedDate))
                        if (!uiState.isToday) {
                            TextButton(onClick = viewModel::goToToday) {
                                Text(stringResource(R.string.dashboard_back_to_today))
                            }
                        }
                    }
                },
                actions = {
                    uiState.syncBadge?.let { badge ->
                        if (badge.type != SyncBadgeType.NONE) {
                            IconButton(onClick = {
                                when (badge.type) {
                                    SyncBadgeType.OFFLINE -> {
                                        Toast.makeText(
                                            context,
                                            context.getString(R.string.dashboard_sync_offline_toast),
                                            Toast.LENGTH_SHORT,
                                        ).show()
                                    }
                                    SyncBadgeType.PENDING -> {
                                        Toast.makeText(
                                            context,
                                            context.getString(R.string.dashboard_sync_pending_toast, badge.count),
                                            Toast.LENGTH_SHORT,
                                        ).show()
                                    }
                                    SyncBadgeType.FAILED -> {
                                        Toast.makeText(
                                            context,
                                            context.getString(R.string.dashboard_sync_failed_toast, badge.count),
                                            Toast.LENGTH_SHORT,
                                        ).show()
                                        onNavigateDiagnostics()
                                    }
                                    else -> Unit
                                }
                            }) {
                                Icon(
                                    imageVector = when (badge.type) {
                                        SyncBadgeType.OFFLINE -> Icons.Default.CloudOff
                                        SyncBadgeType.FAILED -> Icons.Default.Warning
                                        else -> Icons.Default.Sync
                                    },
                                    contentDescription = null,
                                    tint = if (badge.type == SyncBadgeType.FAILED) {
                                        MaterialTheme.colorScheme.secondary
                                    } else {
                                        MaterialTheme.colorScheme.onSurface
                                    },
                                )
                            }
                        }
                    }
                    IconButton(onClick = onNavigateSettings) {
                        Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.settings_title))
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onNavigateAddMeal(uiState.selectedDate) },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
            ) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.dashboard_add_meal))
            }
        },
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = uiState.isRefreshing,
            onRefresh = viewModel::refresh,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            when (uiState.loadState) {
                DashboardLoadState.LOADING -> DashboardSkeleton()
                DashboardLoadState.ERROR -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(stringResource(R.string.dashboard_load_error))
                        Button(onClick = viewModel::retryLoad) {
                            Text(stringResource(R.string.common_retry))
                        }
                    }
                }
                DashboardLoadState.SUCCESS -> {
                    val data = uiState.data
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(uiState.selectedDate) {
                                detectHorizontalDragGestures { _, dragAmount ->
                                    if (abs(dragAmount) > 80) {
                                        if (dragAmount > 0) viewModel.goToPreviousDay()
                                        else viewModel.goToNextDay()
                                    }
                                }
                            },
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(16.dp),
                        ) {
                            if (uiState.showDeviceTimeWarning) {
                                DeviceTimeWarningBanner(modifier = Modifier.padding(bottom = 12.dp))
                            }

                            if (uiState.healthWarning != null && !uiState.healthWarningDismissed) {
                                val warning = uiState.healthWarning!!
                                HealthWarningBanner(
                                    warning = warning,
                                    onDismiss = viewModel::dismissHealthWarning,
                                    onBackfill = if (warning.type.showBackfillAction) {
                                        {
                                            viewModel.onHealthWarningBackfill {
                                                onNavigateAddMeal(uiState.selectedDate)
                                            }
                                        }
                                    } else {
                                        null
                                    },
                                )
                            }

                            CalorieCard(
                                overview = data?.calorieOverview,
                                budget = data?.budget,
                                hasNoGoal = data?.hasNoGoal == true,
                                budgetAbnormal = data?.budgetAbnormal == true,
                                onSetupGoal = onNavigateOnboarding,
                                modifier = Modifier.padding(bottom = 16.dp),
                            )

                            MacroProgressSection(
                                macros = data?.macros.orEmpty(),
                                modifier = Modifier.padding(bottom = 16.dp),
                            )

                            WeightCard(
                                data = data?.weightCard ?: com.example.healthcheckin.domain.model.WeightCardData(
                                    null, null, false, null, false, false, null,
                                ),
                                onClick = onNavigateWeight,
                                modifier = Modifier.padding(bottom = 16.dp),
                            )

                            MealListSection(
                                groups = data?.mealGroups.orEmpty(),
                                highlightEntryId = uiState.highlightEntryId,
                                onEntryClick = onNavigateMealEdit,
                                onDeleteEntry = viewModel::deleteMeal,
                            )
                        }
                    }
                }
            }
        }
    }
}
