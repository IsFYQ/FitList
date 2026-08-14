package com.example.healthcheckin.ui.screens.recommendation

import android.widget.Toast
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
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
import com.example.healthcheckin.domain.algorithm.RecommendationCombo
import com.example.healthcheckin.domain.algorithm.RecommendationComboItem
import com.example.healthcheckin.domain.algorithm.RecommendationFallback
import com.example.healthcheckin.domain.algorithm.RecommendationResult
import com.example.healthcheckin.ui.components.AppEmptyState
import com.example.healthcheckin.ui.components.SkeletonBlock
import com.example.healthcheckin.ui.theme.HealthCheckInDimens
import com.example.healthcheckin.util.InventoryExpiryStatus
import com.example.healthcheckin.util.MealSlot
import com.example.healthcheckin.util.PrecisionUtil

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecommendationScreen(
    onBack: () -> Unit,
    onNavigateInventory: () -> Unit = {},
    onNavigateBindings: () -> Unit = {},
    onMealLogged: () -> Unit = {},
    viewModel: RecommendationViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(uiState.loggedEntryIds) {
        uiState.loggedEntryIds?.let {
            Toast.makeText(context, context.getString(R.string.recommend_log_success), Toast.LENGTH_SHORT).show()
            onMealLogged()
            viewModel.clearLoggedEntryIds()
        }
    }

    LaunchedEffect(uiState.toastMessage) {
        uiState.toastMessage?.let { key ->
            val resId = when (key) {
                "recommend_swap_exhausted" -> R.string.recommend_swap_exhausted
                else -> R.string.recommend_swap_exhausted
            }
            Toast.makeText(context, context.getString(resId), Toast.LENGTH_SHORT).show()
            viewModel.clearToast()
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { key ->
            val resId = when (key) {
                "recommend_error_load" -> R.string.recommend_error_load
                else -> R.string.recommend_error_log
            }
            Toast.makeText(context, context.getString(resId), Toast.LENGTH_SHORT).show()
            viewModel.clearError()
        }
    }

    uiState.confirmComboIndex?.let { comboIndex ->
        RecommendLogSheet(
            mealSlot = uiState.mealSlot,
            isSaving = uiState.isLoggingMeal,
            onDismiss = viewModel::dismissLogMealSheet,
            onMealSlotChange = viewModel::setMealSlot,
            onConfirm = viewModel::logMeal,
            combo = uiState.displayedCombos.getOrNull(comboIndex)?.combo,
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.recommend_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
            )
        },
    ) { padding ->
        when {
            uiState.isLoading -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(HealthCheckInDimens.PagePadding),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    repeat(3) { SkeletonBlock(height = 160.dp) }
                }
            }
            uiState.result == null -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center,
                ) {
                    AppEmptyState(title = stringResource(R.string.recommend_error_load))
                }
            }
            else -> RecommendationContent(
                modifier = Modifier.fillMaxSize().padding(padding),
                result = uiState.result!!,
                displayedCombos = uiState.displayedCombos,
                swapExhausted = uiState.swapExhausted,
                onSwap = viewModel::swapCombo,
                onLogMeal = viewModel::openLogMealSheet,
                onNavigateInventory = onNavigateInventory,
                onNavigateBindings = onNavigateBindings,
            )
        }
    }
}

@Composable
private fun RecommendationContent(
    modifier: Modifier = Modifier,
    result: RecommendationResult,
    displayedCombos: List<RecommendationComboUi>,
    swapExhausted: Boolean,
    onSwap: (Int) -> Unit,
    onLogMeal: (Int) -> Unit,
    onNavigateInventory: () -> Unit,
    onNavigateBindings: () -> Unit,
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(
            horizontal = HealthCheckInDimens.PagePadding,
            vertical = HealthCheckInDimens.PagePadding,
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            GapSummaryCard(result = result)
        }
        if (result.mostlyExpired) {
            item {
                Text(
                    text = stringResource(R.string.recommend_mostly_expired),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
        when (result.fallback) {
            RecommendationFallback.ALL_MET -> {
                item {
                    AppEmptyState(title = stringResource(R.string.recommend_all_met))
                }
            }
            RecommendationFallback.LOW_GAP -> {
                item {
                    AppEmptyState(
                        title = stringResource(
                            R.string.recommend_low_gap,
                            PrecisionUtil.roundCaloriesDisplay(result.gap.gapKcal),
                        ),
                    )
                }
            }
            RecommendationFallback.EMPTY_INVENTORY,
            RecommendationFallback.NO_NUTRITION,
            -> {
                item {
                    FallbackAdviceCard(
                        message = result.genericAdvice ?: stringResource(R.string.recommend_empty_inventory),
                        actionLabel = stringResource(R.string.recommend_go_inventory),
                        onAction = onNavigateInventory,
                    )
                }
                if (result.fallback == RecommendationFallback.NO_NUTRITION) {
                    item {
                        TextButton(onClick = onNavigateBindings) {
                            Text(stringResource(R.string.recommend_go_bindings))
                        }
                    }
                }
            }
            RecommendationFallback.NO_COMBO -> {
                item {
                    Text(
                        text = stringResource(R.string.recommend_single_hint),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                items(result.topSingles.size) { index ->
                    val single = result.topSingles[index]
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = single.inventoryName,
                            modifier = Modifier.padding(16.dp),
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }
            }
            RecommendationFallback.NONE -> {
                itemsIndexed(displayedCombos, key = { _, item -> item.combo.hashCode() }) { index, comboUi ->
                    ComboCard(
                        index = index,
                        combo = comboUi.combo,
                        swapEnabled = !swapExhausted,
                        onSwap = { onSwap(index) },
                        onLogMeal = { onLogMeal(index) },
                    )
                }
            }
        }
        item {
            Text(
                text = stringResource(R.string.recommend_disclaimer),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

@Composable
private fun GapSummaryCard(result: RecommendationResult) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(
                    R.string.recommend_gap_kcal,
                    PrecisionUtil.roundCaloriesDisplay(result.gap.gapKcal),
                ),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(8.dp))
            MacroGapRow(
                label = stringResource(R.string.recommend_gap_protein),
                gap = result.gap.gapProtein,
            )
            MacroGapRow(
                label = stringResource(R.string.recommend_gap_carb),
                gap = result.gap.gapCarb,
            )
            MacroGapRow(
                label = stringResource(R.string.recommend_gap_fat),
                gap = result.gap.gapFat,
            )
        }
    }
}

@Composable
private fun MacroGapRow(label: String, gap: Double) {
    val met = gap <= 0.001
    Text(
        text = if (met) "$label ✓" else "$label −${PrecisionUtil.roundMacroDisplay(gap)}g",
        style = MaterialTheme.typography.bodySmall,
        color = if (met) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurface,
    )
}

@Composable
private fun FallbackAdviceCard(
    message: String,
    actionLabel: String,
    onAction: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = message, style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(12.dp))
            Button(onClick = onAction) { Text(actionLabel) }
        }
    }
}

@Composable
private fun ComboCard(
    index: Int,
    combo: RecommendationCombo,
    swapEnabled: Boolean,
    onSwap: () -> Unit,
    onLogMeal: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.recommend_combo_title, index + 1),
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = stringResource(
                        R.string.recommend_combo_kcal,
                        PrecisionUtil.roundCaloriesDisplay(combo.totalKcal),
                    ),
                    style = MaterialTheme.typography.labelMedium,
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            combo.items.forEach { item ->
                ComboItemRow(item)
            }
            Spacer(modifier = Modifier.height(8.dp))
            MacroProgress(label = stringResource(R.string.recommend_gap_protein), value = comboMacroRatio(combo, "protein"))
            MacroProgress(label = stringResource(R.string.recommend_gap_carb), value = comboMacroRatio(combo, "carb"))
            MacroProgress(label = stringResource(R.string.recommend_gap_fat), value = comboMacroRatio(combo, "fat"))
            Spacer(modifier = Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onLogMeal,
                    modifier = Modifier.weight(1f).heightIn(min = HealthCheckInDimens.ButtonHeight),
                ) {
                    Text(
                        text = stringResource(R.string.recommend_log_meal),
                        maxLines = 1,
                    )
                }
                OutlinedButton(
                    onClick = onSwap,
                    enabled = swapEnabled,
                    modifier = Modifier.weight(1f).heightIn(min = HealthCheckInDimens.ButtonHeight),
                ) {
                    Text(
                        text = stringResource(R.string.recommend_swap),
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

@Composable
private fun ComboItemRow(item: RecommendationComboItem) {
    val nearExpiry = item.candidate.expiryStatus == InventoryExpiryStatus.NEAR_EXPIRY
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(modifier = Modifier.weight(1f)) {
            Text(
                text = item.candidate.inventoryName,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
            if (nearExpiry) {
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = stringResource(R.string.recommend_near_expiry),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFE65100),
                )
            }
        }
        Text(
            text = "${PrecisionUtil.roundMacroDisplay(item.portionBasis)}${item.candidate.inventoryUnit}",
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun MacroProgress(label: String, value: Float) {
    Column(modifier = Modifier.padding(vertical = 2.dp)) {
        Text(text = label, style = MaterialTheme.typography.labelSmall)
        LinearProgressIndicator(
            progress = { value.coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

private fun comboMacroRatio(combo: RecommendationCombo, macro: String): Float {
    val total = combo.items.sumOf {
        when (macro) {
            "protein" -> it.proteinG
            "carb" -> it.carbG
            else -> it.fatG
        }
    }
    return (total / 50.0).toFloat().coerceIn(0f, 1f)
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun RecommendLogSheet(
    mealSlot: MealSlot,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onMealSlotChange: (MealSlot) -> Unit,
    onConfirm: () -> Unit,
    combo: RecommendationCombo?,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)) {
            Text(
                text = stringResource(R.string.recommend_confirm_title),
                style = MaterialTheme.typography.titleLarge,
            )
            combo?.let {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(
                        R.string.recommend_combo_kcal,
                        PrecisionUtil.roundCaloriesDisplay(it.totalKcal),
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(stringResource(R.string.recommend_meal_slot))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MealSlot.entries.forEach { slot ->
                    FilterChip(
                        selected = mealSlot == slot,
                        onClick = { onMealSlotChange(slot) },
                        label = {
                            Text(
                                when (slot) {
                                    MealSlot.BREAKFAST -> stringResource(R.string.meal_slot_breakfast)
                                    MealSlot.LUNCH -> stringResource(R.string.meal_slot_lunch)
                                    MealSlot.DINNER -> stringResource(R.string.meal_slot_dinner)
                                    MealSlot.SNACK -> stringResource(R.string.meal_slot_snack)
                                },
                            )
                        },
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onConfirm,
                enabled = !isSaving,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Text(stringResource(R.string.recommend_log_meal))
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
