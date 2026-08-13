package com.example.healthcheckin.ui.screens.exercise

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.healthcheckin.R
import com.example.healthcheckin.domain.model.ExerciseRecordItem
import com.example.healthcheckin.domain.model.ExerciseWeekSummary
import com.example.healthcheckin.ui.components.AppEmptyState
import com.example.healthcheckin.ui.screens.meal.MealDatePickerDialog
import com.example.healthcheckin.ui.theme.HealthCheckInDimens
import com.example.healthcheckin.util.DateTimeUtil
import com.example.healthcheckin.util.ExerciseType
import com.example.healthcheckin.util.PrecisionUtil
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseScreen(
    embeddedInTabs: Boolean = false,
    viewModel: ExerciseViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.snackbarMessage) {
        uiState.snackbarMessage?.let { message ->
            val text = if (message.startsWith("exercise_streak_milestone:")) {
                val days = message.substringAfter(":").toIntOrNull() ?: 0
                context.getString(R.string.exercise_streak_milestone, days)
            } else {
                message
            }
            snackbarHostState.showSnackbar(text)
            viewModel.clearSnackbar()
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            Toast.makeText(context, context.getString(R.string.exercise_save_failed), Toast.LENGTH_SHORT).show()
            viewModel.clearError()
        }
    }

    if (uiState.showRecordSheet) {
        ExerciseRecordSheet(
            state = uiState.sheet,
            minDate = uiState.minDate,
            isSaving = uiState.isSaving,
            onDismiss = viewModel::dismissRecordSheet,
            onTypeChange = viewModel::setExerciseType,
            onDurationChange = viewModel::setDurationText,
            onQuickDuration = viewModel::setQuickDuration,
            onDateChange = viewModel::setLocalDate,
            onCustomNameChange = viewModel::setCustomName,
            onCustomMetChange = viewModel::setCustomMetText,
            onSave = viewModel::saveRecord,
        )
    }

    Scaffold(
        topBar = {
            if (!embeddedInTabs) {
                TopAppBar(title = { Text(stringResource(R.string.exercise_title)) })
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(onClick = viewModel::openRecordSheet) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.exercise_record_fab))
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(
                horizontal = HealthCheckInDimens.PagePadding,
                vertical = HealthCheckInDimens.PagePadding,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                uiState.weekSummary?.let { summary ->
                    WeekSummaryCard(summary)
                }
            }
            item {
                Text(
                    text = stringResource(R.string.exercise_budget_disclaimer),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.small)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(12.dp),
                )
            }
            item {
                uiState.weekSummary?.let { WeekCalendarRow(it) }
            }
            if (uiState.records.isEmpty()) {
                item {
                    AppEmptyState(title = stringResource(R.string.exercise_empty))
                }
            } else {
                val grouped = uiState.records
                    .groupBy { it.localDate }
                    .entries
                    .sortedByDescending { it.key }
                grouped.forEach { (date, records) ->
                    item {
                        Text(
                            text = DateTimeUtil.formatDashboardDate(date),
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(vertical = 4.dp),
                        )
                    }
                    items(records, key = { it.id }) { record ->
                        ExerciseRecordRow(record = record, onDelete = { viewModel.deleteRecord(record.id) })
                    }
                }
            }
            item { Spacer(modifier = Modifier.height(72.dp)) }
        }
    }
}

@Composable
private fun WeekSummaryCard(summary: ExerciseWeekSummary) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.exercise_week_summary),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(stringResource(R.string.exercise_week_minutes, summary.totalMinutes))
            Text(stringResource(R.string.exercise_week_sessions, summary.sessionCount))
            Text(stringResource(R.string.exercise_current_streak, summary.currentStreak))
            Text(stringResource(R.string.exercise_best_streak, summary.bestStreak))
        }
    }
}

@Composable
private fun WeekCalendarRow(summary: ExerciseWeekSummary) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        summary.weekDates.forEach { date ->
            val active = date in summary.activeDates
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(
                            if (active) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                        ),
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = LocalDate.parse(date).dayOfMonth.toString(),
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }
    }
}

@Composable
private fun ExerciseRecordRow(
    record: ExerciseRecordItem,
    onDelete: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (record.exerciseType == ExerciseType.CUSTOM) {
                        record.customName ?: record.exerciseType.labelZh
                    } else {
                        record.exerciseType.labelZh
                    },
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    text = stringResource(R.string.exercise_record_duration, record.durationMinutes),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = stringResource(
                        R.string.exercise_record_kcal,
                        record.estimatedKcal,
                    ),
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = stringResource(R.string.exercise_estimate_hint),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                )
            }
            TextButton(onClick = onDelete) {
                Text(stringResource(R.string.exercise_delete))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun ExerciseRecordSheet(
    state: ExerciseRecordSheetState,
    minDate: String,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onTypeChange: (ExerciseType) -> Unit,
    onDurationChange: (String) -> Unit,
    onQuickDuration: (Int) -> Unit,
    onDateChange: (LocalDate) -> Unit,
    onCustomNameChange: (String) -> Unit,
    onCustomMetChange: (String) -> Unit,
    onSave: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showDatePicker by remember { mutableStateOf(false) }
    val minLocalDate = DateTimeUtil.parseLocalDate(minDate)
    val maxLocalDate = DateTimeUtil.todayLocalDate()

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)) {
            Text(
                text = stringResource(R.string.exercise_record_title),
                style = MaterialTheme.typography.titleLarge,
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(stringResource(R.string.exercise_type_label))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ExerciseType.entries.forEach { type ->
                    FilterChip(
                        selected = state.exerciseType == type,
                        onClick = { onTypeChange(type) },
                        label = { Text(type.labelZh) },
                    )
                }
            }
            if (state.exerciseType == ExerciseType.CUSTOM) {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = state.customName,
                    onValueChange = onCustomNameChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.exercise_custom_name)) },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = state.customMetText,
                    onValueChange = onCustomMetChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.exercise_custom_met)) },
                    singleLine = true,
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = state.durationText,
                onValueChange = onDurationChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.exercise_duration_label)) },
                singleLine = true,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(15, 30, 45, 60).forEach { minutes ->
                    FilterChip(
                        selected = state.durationText == minutes.toString(),
                        onClick = { onQuickDuration(minutes) },
                        label = { Text(stringResource(R.string.exercise_duration_quick, minutes)) },
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = DateTimeUtil.formatDashboardDate(DateTimeUtil.formatLocalDate(state.localDate)),
                onValueChange = {},
                modifier = Modifier.fillMaxWidth(),
                readOnly = true,
                label = { Text(stringResource(R.string.exercise_date_label)) },
            )
            TextButton(onClick = { showDatePicker = true }) {
                Text(stringResource(R.string.exercise_change_date))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.exercise_estimated_kcal, state.estimatedKcal),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            state.validationError?.let { key ->
                val resId = when (key) {
                    "exercise_error_duration" -> R.string.exercise_error_duration
                    "exercise_error_custom_name" -> R.string.exercise_error_custom_name
                    "exercise_error_met" -> R.string.exercise_error_met
                    else -> R.string.exercise_save_failed
                }
                Text(
                    text = stringResource(resId),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            androidx.compose.material3.Button(
                onClick = onSave,
                enabled = !isSaving,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.common_save))
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    if (showDatePicker) {
        MealDatePickerDialog(
            initialDate = state.localDate,
            minDate = minLocalDate,
            maxDate = maxLocalDate,
            onDismiss = { showDatePicker = false },
            onConfirm = { date ->
                onDateChange(date)
                showDatePicker = false
            },
        )
    }
}
