package com.example.healthcheckin.ui.screens.dashboard.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.outlined.Restaurant
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.healthcheckin.R
import com.example.healthcheckin.domain.model.CalorieOverview
import com.example.healthcheckin.domain.model.HealthWarning
import com.example.healthcheckin.domain.model.HealthWarningType
import com.example.healthcheckin.domain.model.MacroKind
import com.example.healthcheckin.domain.model.MacroProgress
import com.example.healthcheckin.domain.model.MealGroup
import com.example.healthcheckin.domain.model.ResolvedBudget
import com.example.healthcheckin.domain.model.WeightCardData
import com.example.healthcheckin.domain.model.showBackfillAction
import com.example.healthcheckin.ui.components.AppEmptyState
import com.example.healthcheckin.ui.components.DashboardSkeletonContent
import com.example.healthcheckin.ui.theme.HealthCheckInDimens
import com.example.healthcheckin.ui.theme.HealthCheckInRadius
import com.example.healthcheckin.ui.theme.HealthCheckInThemeExtras
import com.example.healthcheckin.util.CalorieState
import com.example.healthcheckin.util.GoalType
import com.example.healthcheckin.util.PrecisionUtil
import kotlin.math.roundToInt

@Composable
fun DeviceTimeWarningBanner(modifier: Modifier = Modifier) {
    val warning = HealthCheckInThemeExtras.extendedColors.warning
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(HealthCheckInRadius.Card),
        colors = CardDefaults.cardColors(containerColor = warning.copy(alpha = 0.16f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            modifier = Modifier
                .padding(HealthCheckInDimens.Space3)
                .height(HealthCheckInDimens.MinTouchTarget),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Default.Info,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = warning,
            )
            Text(
                text = stringResource(R.string.dashboard_time_inaccurate),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(start = HealthCheckInDimens.Space2),
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
        HealthWarningType.LOW_INTAKE ->
            stringResource(R.string.dashboard_warning_low_intake)
        HealthWarningType.HIGH_INTAKE ->
            stringResource(R.string.dashboard_warning_high_intake)
        HealthWarningType.RECORD_GAP ->
            stringResource(R.string.dashboard_warning_record_gap)
    }
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(HealthCheckInRadius.Card),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            modifier = Modifier.padding(HealthCheckInDimens.Space3),
            verticalAlignment = Alignment.Top,
        ) {
            Icon(
                Icons.Default.Info,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.secondary,
            )
            Column(modifier = Modifier.padding(start = HealthCheckInDimens.Space2).weight(1f)) {
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
                Text(
                    text = stringResource(R.string.dashboard_health_disclaimer),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = HealthCheckInDimens.Space1),
                )
                if (warning.type.showBackfillAction && onBackfill != null) {
                    TextButton(
                        onClick = onBackfill,
                        modifier = Modifier.height(HealthCheckInDimens.MinTouchTarget),
                    ) {
                        Text(stringResource(R.string.dashboard_health_backfill))
                    }
                }
            }
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.size(HealthCheckInDimens.MinTouchTarget),
            ) {
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
        shape = RoundedCornerShape(HealthCheckInRadius.Card),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(
            modifier = Modifier.padding(HealthCheckInDimens.Space5),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            when {
                hasNoGoal -> {
                    Text(
                        text = stringResource(R.string.dashboard_no_goal_hint),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    Spacer(modifier = Modifier.height(HealthCheckInDimens.Space2))
                    Text(
                        text = stringResource(R.string.dashboard_go_setup_goal),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .height(HealthCheckInDimens.MinTouchTarget)
                            .padding(top = HealthCheckInDimens.Space1),
                    )
                }
                budgetAbnormal || overview == null -> {
                    Text(
                        text = stringResource(R.string.dashboard_budget_abnormal),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
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
                        style = MaterialTheme.typography.displaySmall,
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
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = HealthCheckInDimens.Space1),
                    )
                    if (budget?.isInferred == true) {
                        Text(
                            text = stringResource(R.string.dashboard_budget_inferred),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Spacer(modifier = Modifier.height(HealthCheckInDimens.Space3))
                    val progress = if (overview.budget > 0) {
                        overview.consumed.toFloat() / overview.budget.toFloat()
                    } else {
                        0f
                    }
                    CircularProgressIndicator(
                        progress = { progress.coerceIn(0f, 1f) },
                        modifier = Modifier.size(80.dp),
                        color = color,
                        trackColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                        strokeWidth = 8.dp,
                    )
                }
            }
        }
    }
}

@Composable
fun MacroProgressSection(macros: List<MacroProgress>, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(HealthCheckInRadius.Card),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant,
        ),
    ) {
        Column(
            modifier = Modifier.padding(HealthCheckInDimens.CardPadding),
            verticalArrangement = Arrangement.spacedBy(HealthCheckInDimens.Space3),
        ) {
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
                color = if (macro.isOver) indicatorColor else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        LinearProgressIndicator(
            progress = { macro.progress },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = HealthCheckInDimens.Space1)
                .height(6.dp)
                .clip(RoundedCornerShape(HealthCheckInRadius.Chip)),
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
private fun macroIndicatorColor(kind: MacroKind, isOver: Boolean, ratio: Double): Color {
    val ext = HealthCheckInThemeExtras.extendedColors
    return when {
        isOver && kind == MacroKind.FAT -> MaterialTheme.colorScheme.error
        isOver -> ext.warning
        ratio > 0.9 -> ext.warning
        else -> MaterialTheme.colorScheme.primary
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeightCard(
    data: WeightCardData,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(HealthCheckInRadius.Card),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant,
        ),
    ) {
        Row(
            modifier = Modifier
                .padding(HealthCheckInDimens.CardPadding)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                if (!data.hasRecords) {
                    Text(
                        text = stringResource(R.string.dashboard_weight_empty),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                } else {
                    Text(
                        text = "${PrecisionUtil.roundWeightDisplay(data.latestWeightKg ?: 0.0)}kg",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface,
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
                        data.goalReached -> Text(
                            text = stringResource(R.string.dashboard_goal_reached),
                            style = MaterialTheme.typography.bodySmall,
                            color = HealthCheckInThemeExtras.extendedColors.success,
                        )
                        data.distanceToTargetKg != null -> Text(
                            text = stringResource(R.string.dashboard_distance_to_goal, data.distanceToTargetKg),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp),
            )
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
        AppEmptyState(
            title = stringResource(R.string.dashboard_empty_meals),
            icon = Icons.Outlined.Restaurant,
            modifier = modifier,
        )
        return
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(HealthCheckInDimens.Space2),
    ) {
        groups.forEach { group ->
            Text(
                text = stringResource(
                    R.string.dashboard_meal_group_header,
                    group.slotLabel,
                    PrecisionUtil.formatCaloriesWithSeparator(group.totalKcal),
                ),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = HealthCheckInDimens.Space2),
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
                .padding(start = HealthCheckInDimens.Space2),
            contentAlignment = Alignment.CenterEnd,
        ) {
            Box(
                modifier = Modifier
                    .width(deleteWidth)
                    .fillMaxHeight()
                    .clip(
                        RoundedCornerShape(
                            topEnd = HealthCheckInRadius.Card,
                            bottomEnd = HealthCheckInRadius.Card,
                        ),
                    )
                    .background(MaterialTheme.colorScheme.error),
                contentAlignment = Alignment.Center,
            ) {
                IconButton(
                    onClick = {
                        onDelete()
                        offsetX = 0f
                    },
                    modifier = Modifier.size(HealthCheckInDimens.MinTouchTarget),
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = stringResource(R.string.dashboard_delete_meal),
                        tint = MaterialTheme.colorScheme.onError,
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
            shape = RoundedCornerShape(HealthCheckInRadius.Card),
            colors = CardDefaults.cardColors(containerColor = containerColor),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant,
            ),
        ) {
            Row(
                modifier = Modifier
                    .padding(horizontal = HealthCheckInDimens.Space3, vertical = HealthCheckInDimens.Space3)
                    .height(HealthCheckInDimens.ListRowMinHeight),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(name, style = MaterialTheme.typography.bodyLarge)
                    Text(
                        text = "$quantity · ${PrecisionUtil.formatCaloriesWithSeparator(kcal)}大卡",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    text = sourceLabel(source),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }
        }
    }
}

@Composable
fun weightDeltaColor(goalType: GoalType?, deltaPositive: Boolean): Color {
    val ext = HealthCheckInThemeExtras.extendedColors
    return when (goalType) {
        GoalType.LOSE -> if (deltaPositive) ext.warning else ext.success
        GoalType.GAIN -> if (deltaPositive) ext.success else ext.warning
        GoalType.MAINTAIN, null -> MaterialTheme.colorScheme.onSurfaceVariant
    }
}

@Composable
fun DashboardSkeleton(modifier: Modifier = Modifier) {
    DashboardSkeletonContent(modifier = modifier)
}

@Composable
private fun calorieColor(state: CalorieState): Color {
    val ext = HealthCheckInThemeExtras.extendedColors
    return when (state) {
        CalorieState.NORMAL -> ext.success
        CalorieState.WARN -> ext.warning
        CalorieState.OVER -> MaterialTheme.colorScheme.error
    }
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
