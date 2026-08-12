package com.example.healthcheckin.ui.screens.dashboard

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
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
import android.widget.Toast
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.healthcheckin.R
import com.example.healthcheckin.domain.model.showBackfillAction
import com.example.healthcheckin.ui.screens.dashboard.components.CalorieCard
import com.example.healthcheckin.ui.screens.dashboard.components.DashboardSkeleton
import com.example.healthcheckin.ui.screens.dashboard.components.DeviceTimeWarningBanner
import com.example.healthcheckin.ui.screens.dashboard.components.HealthWarningBanner
import com.example.healthcheckin.ui.screens.dashboard.components.MacroProgressSection
import com.example.healthcheckin.ui.screens.dashboard.components.MealListSection
import com.example.healthcheckin.ui.screens.dashboard.components.WeightCard
import com.example.healthcheckin.ui.theme.HealthCheckInDimens
import com.example.healthcheckin.util.DateTimeUtil
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onNavigateSettings: () -> Unit,
    onNavigateOnboarding: () -> Unit,
    onNavigateAddMeal: (localDate: String) -> Unit,
    onNavigateMealEdit: (entryId: String) -> Unit,
    onNavigateWeight: () -> Unit,
    onNavigateBodyMeasurements: () -> Unit = {},
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
                        Text(
                            text = DateTimeUtil.formatDashboardDate(uiState.selectedDate),
                            style = MaterialTheme.typography.titleLarge,
                        )
                        if (!uiState.isToday) {
                            TextButton(
                                onClick = viewModel::goToToday,
                                modifier = Modifier.height(HealthCheckInDimens.MinTouchTarget),
                            ) {
                                Text(stringResource(R.string.dashboard_back_to_today))
                            }
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = onNavigateSettings,
                        modifier = Modifier.size(HealthCheckInDimens.MinTouchTarget),
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.settings_title))
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onNavigateAddMeal(uiState.selectedDate) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
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
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(HealthCheckInDimens.PagePadding),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            text = stringResource(R.string.dashboard_load_error),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Spacer(modifier = Modifier.height(HealthCheckInDimens.Space4))
                        Button(
                            onClick = viewModel::retryLoad,
                            modifier = Modifier.height(HealthCheckInDimens.ButtonHeight),
                        ) {
                            Text(stringResource(R.string.common_retry))
                        }
                        Spacer(modifier = Modifier.weight(1f))
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
                                .padding(HealthCheckInDimens.PagePadding),
                        ) {
                            if (uiState.showDeviceTimeWarning) {
                                DeviceTimeWarningBanner(
                                    modifier = Modifier.padding(bottom = HealthCheckInDimens.Space3),
                                )
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
                                    modifier = Modifier.padding(bottom = HealthCheckInDimens.Space3),
                                )
                            }

                            CalorieCard(
                                overview = data?.calorieOverview,
                                budget = data?.budget,
                                hasNoGoal = data?.hasNoGoal == true,
                                budgetAbnormal = data?.budgetAbnormal == true,
                                onSetupGoal = onNavigateOnboarding,
                                modifier = Modifier.padding(bottom = HealthCheckInDimens.SectionGap),
                            )

                            MacroProgressSection(
                                macros = data?.macros.orEmpty(),
                                modifier = Modifier.padding(bottom = HealthCheckInDimens.SectionGap),
                            )

                            WeightCard(
                                data = data?.weightCard ?: com.example.healthcheckin.domain.model.WeightCardData(
                                    null, null, false, null, false, false, null,
                                ),
                                onClick = onNavigateWeight,
                                modifier = Modifier.padding(bottom = HealthCheckInDimens.Space2),
                            )

                            androidx.compose.material3.ListItem(
                                headlineContent = { Text(stringResource(R.string.body_entry_from_dashboard)) },
                                modifier = Modifier
                                    .padding(bottom = HealthCheckInDimens.SectionGap)
                                    .clickable(onClick = onNavigateBodyMeasurements),
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
