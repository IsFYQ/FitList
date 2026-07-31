package com.example.healthcheckin.ui.screens.dashboard.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.filled.Delete
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import com.example.healthcheckin.domain.model.MacroKind
import com.example.healthcheckin.util.GoalType
import kotlin.math.roundToInt
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.healthcheckin.R
import com.example.healthcheckin.domain.model.CalorieOverview
import com.example.healthcheckin.domain.model.HealthWarning
import com.example.healthcheckin.domain.model.ruleId
import com.example.healthcheckin.domain.model.showBackfillAction
import com.example.healthcheckin.domain.model.MacroProgress
import com.example.healthcheckin.domain.model.MealGroup
import com.example.healthcheckin.domain.model.ResolvedBudget
import com.example.healthcheckin.domain.model.WeightCardData
import com.example.healthcheckin.ui.theme.HealthCheckInColors
import com.example.healthcheckin.util.CalorieState
import com.example.healthcheckin.util.PrecisionUtil

@Composable
fun DeviceTimeWarningBanner(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = HealthCheckInColors.CalorieWarn.copy(alpha = 0.15f)),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Default.Info,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = HealthCheckInColors.CalorieWarn,
            )
            Text(
                text = stringResource(R.string.dashboard_time_inaccurate),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
    }
}

@Composable
fun HealthWarningBanner(
    warning: HealthWarning,
    onDismiss: () -> Unit,
    onBackfill: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val message = when (warning.type) {
        com.example.healthcheckin.domain.model.HealthWarningType.LOW_INTAKE ->
            stringResource(R.string.dashboard_warning_low_intake)
        com.example.healthcheckin.domain.model.HealthWarningType.HIGH_INTAKE ->
            stringResource(R.string.dashboard_warning_high_intake)
        com.example.healthcheckin.domain.model.HealthWarningType.RECORD_GAP ->
            stringResource(R.string.dashboard_warning_record_gap)
    }
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(20.dp))
            Column(modifier = Modifier.padding(start = 8.dp).weight(1f)) {
                Text(text = message, style = MaterialTheme.typography.bodyMedium)
                Text(
                    text = stringResource(R.string.dashboard_health_disclaimer),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                )
                if (warning.type.showBackfillAction && onBackfill != null) {
                    TextButton(onClick = onBackfill) {
                        Text(stringResource(R.string.dashboard_health_backfill))
                    }
                }
            }
            IconButton(onClick = onDismiss) {
                Icon(Icons.Default.Close, contentDescription = stringResource(R.string.common_cancel))
            }
        }
    }
}

@Composable
fun CalorieCard(
    overview: CalorieOverview?,
    budget: ResolvedBudget?,
    hasNoGoal: Boolean,
    budgetAbnormal: Boolean,
    onSetupGoal: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .then(if (hasNoGoal) Modifier.clickable(onClick = onSetupGoal) else Modifier),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            when {
                hasNoGoal -> {
                    Text(stringResource(R.string.dashboard_no_goal_hint))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.dashboard_go_setup_goal),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
                budgetAbnormal || overview == null -> {
                    Text(stringResource(R.string.dashboard_budget_abnormal))
                }
                else -> {
                    val color = calorieColor(overview.state)
                    val mainText = when (overview.state) {
                        CalorieState.OVER -> stringResource(
                            R.string.dashboard_over_calories,
                            PrecisionUtil.formatCaloriesWithSeparator(kotlin.math.abs(overview.remaining)),
                        )
                        else -> stringResource(
                            R.string.dashboard_remaining_calories,
                            PrecisionUtil.formatCaloriesWithSeparator(overview.remaining),
                        )
                    }
                    Text(
                        text = mainText,
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Bold,
                        color = color,
                    )
                    val subtitle = when (overview.state) {
                        CalorieState.WARN -> stringResource(R.string.dashboard_warn_near_limit)
                        else -> stringResource(
                            R.string.dashboard_budget_consumed,
                            PrecisionUtil.formatCaloriesWithSeparator(overview.budget),
                            PrecisionUtil.formatCaloriesWithSeparator(overview.consumed),
                        )
                    }
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    )
                    if (budget?.isInferred == true) {
                        Text(
                            text = stringResource(R.string.dashboard_budget_inferred),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    val progress = if (overview.budget > 0) {
                        overview.consumed.toFloat() / overview.budget.toFloat()
                    } else {
                        0f
                    }
                    CircularProgressIndicator(
                        progress = { progress.coerceIn(0f, 1f) },
                        modifier = Modifier.size(80.dp),
                        color = color,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        strokeWidth = 8.dp,
                    )
                }
            }
        }
    }
}

@Composable
fun MacroProgressSection(macros: List<MacroProgress>, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        macros.forEachIndexed { index, macro ->
            val label = when (index) {
                0 -> stringResource(R.string.onboarding_macro_protein)
                1 -> stringResource(R.string.onboarding_macro_carb)
                else -> stringResource(R.string.onboarding_macro_fat)
            }
            MacroProgressRow(macro.copy(name = label))
        }
    }
}

@Composable
private fun MacroProgressRow(macro: MacroProgress) {
    val ratio = if (macro.target > 0) macro.consumed / macro.target else 0.0
    val indicatorColor = macroIndicatorColor(macro.kind, macro.isOver, ratio)
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(macro.name, style = MaterialTheme.typography.bodyMedium)
            Text(
                text = "${PrecisionUtil.roundMacroDisplay(macro.consumed)}/${PrecisionUtil.roundMacroDisplay(macro.target)}g · ${macro.percentText}",
                style = MaterialTheme.typography.bodySmall,
                color = if (macro.isOver) indicatorColor else MaterialTheme.colorScheme.onSurface,
            )
        }
        LinearProgressIndicator(
            progress = { macro.progress },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            color = indicatorColor,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
        )
        if (macro.overAmount != null) {
            Text(
                text = stringResource(R.string.dashboard_macro_over, macro.overAmount),
                style = MaterialTheme.typography.labelSmall,
                color = indicatorColor,
            )
        }
    }
}

@Composable
private fun macroIndicatorColor(kind: MacroKind, isOver: Boolean, ratio: Double): Color = when {
    isOver && kind == MacroKind.FAT -> HealthCheckInColors.CalorieOver
    isOver -> HealthCheckInColors.CalorieWarn
    ratio > 0.9 -> HealthCheckInColors.CalorieWarn
    else -> MaterialTheme.colorScheme.primary
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeightCard(
    data: WeightCardData,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(onClick = onClick, modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                if (!data.hasRecords) {
                    Text(stringResource(R.string.dashboard_weight_empty))
                } else {
                    Text(
                        text = "${PrecisionUtil.roundWeightDisplay(data.latestWeightKg ?: 0.0)}kg",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    data.deltaKg?.let { delta ->
                        val sign = if (data.deltaPositive) "↑" else "↓"
                        Text(
                            text = "$sign${delta}kg",
                            style = MaterialTheme.typography.bodySmall,
                            color = weightDeltaColor(data.goalType, data.deltaPositive),
                        )
                    }
                    when {
                        data.goalReached -> Text(stringResource(R.string.dashboard_goal_reached))
                        data.distanceToTargetKg != null -> Text(
                            stringResource(R.string.dashboard_distance_to_goal, data.distanceToTargetKg),
                        )
                    }
                }
            }
            Text("›", fontSize = 24.sp)
        }
    }
}

@Composable
fun MealListSection(
    groups: List<MealGroup>,
    highlightEntryId: String? = null,
    onEntryClick: (String) -> Unit,
    onDeleteEntry: (com.example.healthcheckin.data.local.entity.MealEntryEntity) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (groups.isEmpty()) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.dashboard_empty_meals),
                style = MaterialTheme.typography.titleMedium,
            )
        }
        return
    }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        groups.forEach { group ->
            Text(
                text = stringResource(
                    R.string.dashboard_meal_group_header,
                    group.slotLabel,
                    PrecisionUtil.formatCaloriesWithSeparator(group.totalKcal),
                ),
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(top = 8.dp),
            )
            group.entries.forEach { entry ->
                MealEntryRow(
                    name = entry.snapFoodName,
                    quantity = formatQuantity(entry),
                    kcal = PrecisionUtil.roundCaloriesDisplay(entry.kcal),
                    source = entry.snapSource,
                    highlighted = entry.id == highlightEntryId,
                    onClick = { onEntryClick(entry.id) },
                    onDelete = { onDeleteEntry(entry) },
                )
            }
        }
    }
}

@Composable
private fun MealEntryRow(
    name: String,
    quantity: String,
    kcal: Int,
    source: String,
    highlighted: Boolean = false,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    var offsetX by remember { mutableFloatStateOf(0f) }
    val deleteWidth = 72.dp
    val deleteWidthPx = with(LocalDensity.current) { deleteWidth.toPx() }
    val maxOffset = -deleteWidthPx
    val containerColor by animateColorAsState(
        targetValue = if (highlighted) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surface
        },
        animationSpec = tween(durationMillis = 300),
        label = "mealHighlight",
    )

    Box(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .padding(start = 8.dp),
            contentAlignment = Alignment.CenterEnd,
        ) {
            Box(
                modifier = Modifier
                    .width(deleteWidth)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(topEnd = 12.dp, bottomEnd = 12.dp))
                    .background(HealthCheckInColors.CalorieOver),
                contentAlignment = Alignment.Center,
            ) {
                IconButton(onClick = {
                    onDelete()
                    offsetX = 0f
                }) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = stringResource(R.string.dashboard_delete_meal),
                        tint = Color.White,
                    )
                }
            }
        }
        Card(
            onClick = onClick,
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(offsetX.roundToInt(), 0) }
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            offsetX = if (kotlin.math.abs(offsetX) > deleteWidthPx / 2f) maxOffset else 0f
                        },
                        onHorizontalDrag = { _, dragAmount ->
                            offsetX = (offsetX + dragAmount).coerceIn(maxOffset, 0f)
                        },
                    )
                },
            colors = CardDefaults.cardColors(containerColor = containerColor),
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(name, style = MaterialTheme.typography.bodyLarge)
                    Text(
                        text = "$quantity · ${PrecisionUtil.formatCaloriesWithSeparator(kcal)}大卡",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    )
                }
                Text(
                    text = sourceLabel(source),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                )
            }
        }
    }
}

@Composable
fun weightDeltaColor(goalType: GoalType?, deltaPositive: Boolean): Color = when (goalType) {
    GoalType.LOSE -> if (deltaPositive) HealthCheckInColors.CalorieWarn else HealthCheckInColors.CalorieNormal
    GoalType.GAIN -> if (deltaPositive) HealthCheckInColors.CalorieNormal else HealthCheckInColors.CalorieWarn
    GoalType.MAINTAIN, null -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
}

@Composable
fun DashboardSkeleton(modifier: Modifier = Modifier) {
    Column(modifier = modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        repeat(4) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (it == 0) 120.dp else 60.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
            )
        }
    }
}

private fun calorieColor(state: CalorieState): Color = when (state) {
    CalorieState.NORMAL -> HealthCheckInColors.CalorieNormal
    CalorieState.WARN -> HealthCheckInColors.CalorieWarn
    CalorieState.OVER -> HealthCheckInColors.CalorieOver
}

private fun formatQuantity(entry: com.example.healthcheckin.data.local.entity.MealEntryEntity): String {
    val unit = when (entry.unit) {
        "G" -> "g"
        "ML" -> "ml"
        "SERVING" -> "份"
        else -> entry.unit
    }
    return "${PrecisionUtil.roundMacroDisplay(entry.quantity)}$unit"
}

private fun sourceLabel(source: String): String = when (source) {
    "CUSTOM" -> "自建"
    "PUBLIC" -> "公共"
    "FATSECRET" -> "FS"
    "OFF" -> "OFF"
    else -> source
}
