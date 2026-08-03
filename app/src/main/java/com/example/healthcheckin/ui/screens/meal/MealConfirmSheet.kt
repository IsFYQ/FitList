package com.example.healthcheckin.ui.screens.meal

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.healthcheckin.R
import com.example.healthcheckin.ui.theme.HealthCheckInDimens
import com.example.healthcheckin.ui.theme.HealthCheckInRadius
import com.example.healthcheckin.util.BasisUnit
import com.example.healthcheckin.util.DateTimeUtil
import com.example.healthcheckin.util.MealUnit
import com.example.healthcheckin.util.PrecisionUtil
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun MealConfirmSheet(
    state: MealConfirmUiState,
    minDate: LocalDate,
    onDismiss: () -> Unit,
    onQuantityChange: (String) -> Unit,
    onAdjustQuantity: (Double) -> Unit,
    onUnitChange: (MealUnit) -> Unit,
    onQuickQuantity: (Double) -> Unit,
    onServingGramsChange: (String) -> Unit,
    onMealSlotChange: (com.example.healthcheckin.util.MealSlot) -> Unit,
    onDateChange: (LocalDate) -> Unit,
    onTimeChange: (java.time.LocalTime) -> Unit,
    onSubmit: () -> Unit,
    onDismissZeroKcal: () -> Unit,
    onConfirmZeroKcal: () -> Unit,
    onDelete: (() -> Unit)? = null,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(
            topStart = HealthCheckInRadius.Sheet,
            topEnd = HealthCheckInRadius.Sheet,
        ),
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = state.food.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    foodDataSourceText(state.food.source.name)?.let { source ->
                        Text(
                            text = stringResource(R.string.meal_data_source, source),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        )
                    }
                }
                if (onDelete != null) {
                    TextButton(onClick = { showDeleteDialog = true }) {
                        Text(stringResource(R.string.meal_delete))
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            val needsServing = state.food.dataIncomplete ||
                (state.unit == MealUnit.SERVING && state.food.servingGrams == null)
            if (needsServing) {
                OutlinedTextField(
                    value = state.servingGramsText,
                    onValueChange = onServingGramsChange,
                    label = { Text(stringResource(R.string.meal_serving_grams_label)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            val availableUnits = buildAvailableUnits(state)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                availableUnits.forEach { unit ->
                    FilterChip(
                        selected = state.unit == unit,
                        onClick = { onUnitChange(unit) },
                        label = { Text(unitLabel(unit)) },
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth(),
            ) {
                IconButton(onClick = { onAdjustQuantity(-1.0) }) {
                    Icon(Icons.Default.Remove, contentDescription = null)
                }
                OutlinedTextField(
                    value = state.quantityText,
                    onValueChange = onQuantityChange,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                )
                IconButton(onClick = { onAdjustQuantity(1.0) }) {
                    Icon(Icons.Default.Add, contentDescription = null)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                quickQuantityChips(state).forEach { value ->
                    FilterChip(
                        selected = false,
                        onClick = { onQuickQuantity(value) },
                        label = { Text(quickQuantityLabel(state, value)) },
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "${PrecisionUtil.roundCaloriesDisplay(state.nutrition.kcal)} ${stringResource(R.string.common_kcal)}",
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            val macroLine = buildString {
                state.nutrition.proteinG?.let { append("蛋白 ${PrecisionUtil.roundMacroDisplay(it)}g  ") }
                state.nutrition.carbG?.let { append("碳水 ${PrecisionUtil.roundMacroDisplay(it)}g  ") }
                state.nutrition.fatG?.let { append("脂肪 ${PrecisionUtil.roundMacroDisplay(it)}g") }
            }
            if (macroLine.isNotBlank()) {
                Text(
                    text = macroLine.trim(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MealSlotLabel.entries.forEach { item ->
                    FilterChip(
                        selected = state.mealSlot == item.slot,
                        onClick = { onMealSlotChange(item.slot) },
                        label = { Text(item.label) },
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            val dateFormatter = remember { DateTimeFormatter.ofPattern("M月d日") }
            val timeFormatter = remember { DateTimeFormatter.ofPattern("HH:mm") }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                TextButton(onClick = { showDatePicker = true }) {
                    Text("${state.localDate.format(dateFormatter)}")
                }
                TextButton(onClick = { showTimePicker = true }) {
                    Text(state.time.format(timeFormatter))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onSubmit,
                enabled = state.canSubmit && !state.isSaving,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = HealthCheckInDimens.ButtonHeight),
                shape = RoundedCornerShape(HealthCheckInRadius.Button),
            ) {
                Text(
                    if (state.isEditMode) {
                        stringResource(R.string.meal_save)
                    } else {
                        stringResource(R.string.meal_log)
                    },
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    if (showDatePicker) {
        MealDatePickerDialog(
            initialDate = state.localDate,
            minDate = minDate,
            maxDate = DateTimeUtil.todayLocalDate(),
            onDismiss = { showDatePicker = false },
            onConfirm = { date ->
                if (date.isBefore(minDate)) {
                    Toast.makeText(context, context.getString(R.string.dashboard_date_limit_toast), Toast.LENGTH_SHORT).show()
                } else {
                    onDateChange(date)
                }
                showDatePicker = false
            },
        )
    }

    if (showTimePicker) {
        MealTimePickerDialog(
            initialTime = state.time,
            onDismiss = { showTimePicker = false },
            onConfirm = { time ->
                onTimeChange(time)
                showTimePicker = false
            },
        )
    }

    if (state.showZeroKcalDialog) {
        AlertDialog(
            onDismissRequest = onDismissZeroKcal,
            title = { Text(stringResource(R.string.meal_zero_kcal_title)) },
            text = { Text(stringResource(R.string.meal_zero_kcal_message)) },
            confirmButton = {
                TextButton(onClick = onConfirmZeroKcal) {
                    Text(stringResource(R.string.common_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissZeroKcal) {
                    Text(stringResource(R.string.common_cancel))
                }
            },
        )
    }

    if (showDeleteDialog && onDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.meal_delete_confirm_title)) },
            text = { Text(stringResource(R.string.meal_delete_confirm_message)) },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    onDelete()
                }) {
                    Text(stringResource(R.string.meal_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(stringResource(R.string.common_cancel))
                }
            },
        )
    }
}

private fun buildAvailableUnits(state: MealConfirmUiState): List<MealUnit> {
    val basis = when (state.food.basisUnit) {
        BasisUnit.ML -> MealUnit.ML
        BasisUnit.G -> MealUnit.G
    }
    val units = mutableListOf(basis)
    if (state.food.servingGrams != null || state.servingGramsText.isNotBlank() || state.food.dataIncomplete) {
        units.add(MealUnit.SERVING)
    }
    return units
}

private fun unitLabel(unit: MealUnit): String = when (unit) {
    MealUnit.G -> "克(g)"
    MealUnit.ML -> "毫升(ml)"
    MealUnit.SERVING -> "份"
}

private fun quickQuantityChips(state: MealConfirmUiState): List<Double> =
    if (state.unit == MealUnit.SERVING) listOf(0.5, 1.0, 1.5, 2.0) else listOf(50.0, 100.0, 150.0, 200.0)

private fun quickQuantityLabel(state: MealConfirmUiState, value: Double): String =
    if (state.unit == MealUnit.SERVING) "${value}份" else "${value.toInt()}g"
