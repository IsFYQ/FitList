package com.example.healthcheckin.ui.screens.body

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.healthcheckin.R
import com.example.healthcheckin.domain.model.BodyChartRange
import com.example.healthcheckin.domain.model.BodyMetricSummary
import com.example.healthcheckin.ui.components.AppEmptyState
import com.example.healthcheckin.ui.theme.HealthCheckInDimens
import com.example.healthcheckin.util.DateTimeUtil
import com.example.healthcheckin.util.PrecisionUtil

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BodyMeasurementsScreen(
    onBack: () -> Unit,
    onOpenMetric: (String) -> Unit,
    viewModel: BodyMeasurementsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.body_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(HealthCheckInDimens.PagePadding),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(uiState.summaries, key = { it.metric.name }) { summary ->
                BodyMetricCard(summary = summary, onClick = { onOpenMetric(summary.metric.name) })
            }
        }
    }
}

@Composable
private fun BodyMetricCard(summary: BodyMetricSummary, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(summary.metric.labelZh, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                val latest = summary.latest
                if (latest == null) {
                    Text(stringResource(R.string.body_empty_value), color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    Text(
                        "${PrecisionUtil.roundWeightDisplay(latest.valueCm)} ${stringResource(R.string.body_unit_cm)}",
                        style = MaterialTheme.typography.headlineSmall,
                    )
                    latest.deltaCm?.let { delta ->
                        val sign = if (delta > 0) "+" else ""
                        Text(
                            "$sign${PrecisionUtil.roundWeightDisplay(delta)} cm",
                            color = Color.Gray,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    if (summary.sparkline.size >= 2) {
                        Text(
                            summary.sparkline.joinToString(" · ") { PrecisionUtil.roundWeightDisplay(it).toString() },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            if (summary.latest == null) {
                TextButton(onClick = onClick) { Text(stringResource(R.string.body_record)) }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BodyMetricDetailScreen(
    metricName: String,
    onBack: () -> Unit,
    viewModel: BodyMetricDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(metricName) { viewModel.initMetric(metricName) }
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            val msg = when (it) {
                "range" -> context.getString(R.string.body_range_error, uiState.metric.labelZh)
                else -> context.getString(R.string.body_save_failed)
            }
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            viewModel.clearError()
        }
    }

    uiState.overwriteExisting?.let { existing ->
        AlertDialog(
            onDismissRequest = viewModel::dismissSheet,
            title = { Text(stringResource(R.string.body_overwrite_title)) },
            text = {
                Text(
                    stringResource(
                        R.string.body_overwrite_message,
                        existing,
                        ValidatorsParse(uiState.valueText),
                    ),
                )
            },
            confirmButton = { TextButton(onClick = viewModel::confirmOverwrite) { Text(stringResource(R.string.common_confirm)) } },
            dismissButton = { TextButton(onClick = viewModel::dismissSheet) { Text(stringResource(R.string.common_cancel)) } },
        )
    }
    uiState.largeDiff?.let { diff ->
        AlertDialog(
            onDismissRequest = { },
            title = { Text(stringResource(R.string.body_large_diff_title)) },
            text = { Text(stringResource(R.string.body_large_diff_message, diff)) },
            confirmButton = { TextButton(onClick = viewModel::confirmLargeDiff) { Text(stringResource(R.string.common_confirm)) } },
            dismissButton = { TextButton(onClick = viewModel::dismissSheet) { Text(stringResource(R.string.common_cancel)) } },
        )
    }
    uiState.deleteTarget?.let {
        AlertDialog(
            onDismissRequest = viewModel::dismissDelete,
            title = { Text(stringResource(R.string.common_confirm)) },
            text = { Text("${it.localDate}: ${it.valueCm} cm") },
            confirmButton = { TextButton(onClick = viewModel::confirmDelete) { Text(stringResource(R.string.inventory_delete)) } },
            dismissButton = { TextButton(onClick = viewModel::dismissDelete) { Text(stringResource(R.string.common_cancel)) } },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.metric.labelZh) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { viewModel.openSheet() }) {
                Icon(Icons.Default.Add, contentDescription = null)
            }
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(HealthCheckInDimens.PagePadding)) {
            val latest = uiState.records.firstOrNull()
            Text(
                text = latest?.let { "${PrecisionUtil.roundWeightDisplay(it.valueCm)} cm" }
                    ?: stringResource(R.string.body_empty_value),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(12.dp))
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                listOf(
                    BodyChartRange.DAYS_30 to R.string.body_range_30,
                    BodyChartRange.DAYS_90 to R.string.body_range_90,
                    BodyChartRange.ALL to R.string.body_range_all,
                ).forEachIndexed { index, (range, label) ->
                    SegmentedButton(
                        selected = uiState.range == range,
                        onClick = { viewModel.selectRange(range) },
                        shape = SegmentedButtonDefaults.itemShape(index, 3),
                    ) { Text(stringResource(label)) }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            when {
                uiState.records.isEmpty() -> AppEmptyState(title = stringResource(R.string.body_empty_value))
                uiState.records.size == 1 -> Text(stringResource(R.string.body_need_more_points))
                else -> Text(
                    uiState.records.asReversed().joinToString(" · ") {
                        "${it.localDate.takeLast(5)}:${PrecisionUtil.roundWeightDisplay(it.valueCm)}"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(uiState.records, key = { it.id }) { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.requestDelete(item) }
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(DateTimeUtil.formatDashboardDate(item.localDate))
                        Text("${PrecisionUtil.roundWeightDisplay(item.valueCm)} cm")
                    }
                }
            }
        }
    }

    if (uiState.showSheet) {
        AlertDialog(
            onDismissRequest = viewModel::dismissSheet,
            title = { Text(stringResource(R.string.body_record)) },
            text = {
                Column {
                    OutlinedTextField(
                        value = uiState.valueText,
                        onValueChange = viewModel::updateValue,
                        label = { Text(stringResource(R.string.body_unit_cm)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(DateTimeUtil.formatDashboardDate(DateTimeUtil.formatLocalDate(uiState.localDate)))
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.save() }, enabled = !uiState.isSaving) {
                    Text(stringResource(R.string.common_save))
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissSheet) { Text(stringResource(R.string.common_cancel)) }
            },
        )
    }
}

private fun ValidatorsParse(text: String): Double =
    text.toDoubleOrNull() ?: 0.0
