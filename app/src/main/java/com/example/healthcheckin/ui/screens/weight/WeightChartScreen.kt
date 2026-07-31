package com.example.healthcheckin.ui.screens.weight

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.healthcheckin.R
import com.example.healthcheckin.domain.model.WeightChartRange
import com.example.healthcheckin.domain.model.WeightProgressInfo
import com.example.healthcheckin.domain.model.WeightRecordItem
import com.example.healthcheckin.ui.screens.dashboard.components.weightDeltaColor
import com.example.healthcheckin.util.DateTimeUtil
import com.example.healthcheckin.util.GoalType
import com.example.healthcheckin.util.PrecisionUtil

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeightChartScreen(
    onBack: () -> Unit,
    viewModel: WeightChartViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { key ->
            val resId = when (key) {
                "weight_error_range" -> R.string.weight_error_range
                else -> R.string.weight_save_failed
            }
            Toast.makeText(context, context.getString(resId), Toast.LENGTH_SHORT).show()
            viewModel.clearError()
        }
    }

    uiState.overwritePrompt?.let { prompt ->
        AlertDialog(
            onDismissRequest = viewModel::dismissOverwriteDialog,
            title = { Text(stringResource(R.string.weight_overwrite_title)) },
            text = {
                Text(
                    stringResource(
                        R.string.weight_overwrite_message,
                        DateTimeUtil.formatDashboardDate(prompt.localDate),
                        PrecisionUtil.roundWeightDisplay(prompt.existingWeightKg),
                        PrecisionUtil.roundWeightDisplay(prompt.newWeightKg),
                    ),
                )
            },
            confirmButton = {
                TextButton(onClick = viewModel::confirmOverwrite) {
                    Text(stringResource(R.string.common_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissOverwriteDialog) {
                    Text(stringResource(R.string.common_cancel))
                }
            },
        )
    }

    uiState.largeDiffPrompt?.let { diff ->
        AlertDialog(
            onDismissRequest = viewModel::dismissLargeDiffDialog,
            title = { Text(stringResource(R.string.weight_large_diff_title)) },
            text = {
                Text(
                    stringResource(
                        R.string.weight_large_diff_message,
                        PrecisionUtil.roundWeightDisplay(diff),
                    ),
                )
            },
            confirmButton = {
                TextButton(onClick = viewModel::confirmLargeDiff) {
                    Text(stringResource(R.string.common_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissLargeDiffDialog) {
                    Text(stringResource(R.string.common_cancel))
                }
            },
        )
    }

    uiState.deleteTarget?.let { target ->
        AlertDialog(
            onDismissRequest = viewModel::dismissDeleteDialog,
            title = { Text(stringResource(R.string.weight_delete_title)) },
            text = {
                Text(
                    stringResource(
                        R.string.weight_delete_message,
                        DateTimeUtil.formatDashboardDate(target.localDate),
                        PrecisionUtil.roundWeightDisplay(target.weightKg),
                    ),
                )
            },
            confirmButton = {
                TextButton(onClick = viewModel::confirmDelete) {
                    Text(stringResource(R.string.common_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissDeleteDialog) {
                    Text(stringResource(R.string.common_cancel))
                }
            },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.weight_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = viewModel::openCreateSheet) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.weight_record_title))
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = 88.dp),
        ) {
            item {
                WeightStatusCard(
                    record = uiState.latestRecord,
                    modifier = Modifier.padding(16.dp),
                )
            }

            uiState.progress?.let { progress ->
                item {
                    WeightProgressSection(
                        progress = progress,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
            }

            item {
                WeightRangeSelector(
                    selected = uiState.selectedRange,
                    onSelect = viewModel::selectRange,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }

            item {
                WeightChartSection(
                    records = uiState.chartRecords,
                    targetWeightKg = uiState.targetWeightKg,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }

            item {
                Text(
                    text = stringResource(R.string.weight_history_title),
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                )
            }

            if (uiState.historyRecords.isEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.weight_history_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
            } else {
                items(uiState.historyRecords, key = { it.id }) { record ->
                    WeightHistoryRow(
                        record = record,
                        onClick = { viewModel.openEditSheet(record) },
                        onDelete = { viewModel.requestDelete(record) },
                    )
                }
            }
        }
    }

    if (uiState.showInputSheet) {
        WeightInputSheet(
            state = uiState.inputState,
            mode = uiState.inputMode,
            isSaving = uiState.isSaving,
            minDate = uiState.minDate,
            onDismiss = viewModel::dismissInputSheet,
            onWeightChange = viewModel::updateWeightText,
            onNoteChange = viewModel::updateNote,
            onDateChange = viewModel::updateInputDate,
            onSave = { viewModel.saveWeight() },
        )
    }
}

@Composable
private fun WeightStatusCard(
    record: WeightRecordItem?,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(20.dp)) {
            if (record == null) {
                Text(
                    text = stringResource(R.string.dashboard_weight_empty),
                    style = MaterialTheme.typography.bodyLarge,
                )
            } else {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = PrecisionUtil.roundWeightDisplay(record.weightKg).toString(),
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = stringResource(R.string.weight_unit_kg),
                        modifier = Modifier.padding(start = 4.dp, bottom = 6.dp),
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
                record.deltaKg?.let { delta ->
                    val positive = delta > 0
                    val sign = if (positive) "↑" else "↓"
                    Text(
                        text = "$sign${PrecisionUtil.roundWeightDisplay(kotlin.math.abs(delta))} kg",
                        color = weightDeltaColor(null, positive),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                Text(
                    text = DateTimeUtil.formatDashboardDate(record.localDate),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                )
            }
        }
    }
}

@Composable
private fun WeightProgressSection(
    progress: WeightProgressInfo,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        when (progress.goalType) {
            GoalType.MAINTAIN -> {
                progress.maintainDistanceKg?.let { distance ->
                    Text(
                        text = stringResource(R.string.weight_maintain_distance, distance),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
            else -> {
                val initial = progress.initialWeightKg ?: return
                val target = progress.targetWeightKg ?: return
                Text(
                    text = stringResource(
                        R.string.weight_progress_label,
                        PrecisionUtil.roundWeightDisplay(initial),
                        PrecisionUtil.roundWeightDisplay(target),
                    ),
                    style = MaterialTheme.typography.bodySmall,
                )
                Spacer(modifier = Modifier.height(8.dp))
                progress.progressPercent?.let { pct ->
                    LinearProgressIndicator(
                        progress = { pct / 100f },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text(
                        text = stringResource(R.string.weight_progress_percent, pct),
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun WeightRangeSelector(
    selected: WeightChartRange,
    onSelect: (WeightChartRange) -> Unit,
    modifier: Modifier = Modifier,
) {
    SingleChoiceSegmentedButtonRow(modifier = modifier.fillMaxWidth()) {
        WeightChartRange.entries.forEachIndexed { index, range ->
            SegmentedButton(
                selected = selected == range,
                onClick = { onSelect(range) },
                shape = SegmentedButtonDefaults.itemShape(index, WeightChartRange.entries.size),
            ) {
                Text(
                    when (range) {
                        WeightChartRange.DAYS_7 -> stringResource(R.string.weight_range_7d)
                        WeightChartRange.DAYS_30 -> stringResource(R.string.weight_range_30d)
                        WeightChartRange.DAYS_90 -> stringResource(R.string.weight_range_90d)
                    },
                )
            }
        }
    }
}

@Composable
private fun WeightChartSection(
    records: List<WeightRecordItem>,
    targetWeightKg: Double?,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(240.dp),
        contentAlignment = Alignment.Center,
    ) {
        when {
            records.isEmpty() -> {
                Text(
                    text = stringResource(R.string.weight_chart_empty),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                )
            }
            records.size == 1 -> {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${PrecisionUtil.roundWeightDisplay(records.first().weightKg)} kg",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = stringResource(R.string.weight_chart_single_point),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }
            else -> {
                WeightLineChart(
                    records = records,
                    targetWeightKg = targetWeightKg,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WeightHistoryRow(
    record: WeightRecordItem,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                onDelete()
                false
            } else {
                false
            }
        },
    )

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {},
        enableDismissFromStartToEnd = false,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(
                    text = DateTimeUtil.formatDashboardDate(record.localDate),
                    style = MaterialTheme.typography.bodyLarge,
                )
                record.note?.takeIf { it.isNotBlank() }?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${PrecisionUtil.roundWeightDisplay(record.weightKg)} kg",
                    fontWeight = FontWeight.Medium,
                )
                record.deltaKg?.let { delta ->
                    val positive = delta > 0
                    val sign = if (positive) "↑" else "↓"
                    Text(
                        text = "$sign${PrecisionUtil.roundWeightDisplay(kotlin.math.abs(delta))}",
                        style = MaterialTheme.typography.labelSmall,
                        color = weightDeltaColor(null, positive),
                    )
                }
            }
        }
    }
}
