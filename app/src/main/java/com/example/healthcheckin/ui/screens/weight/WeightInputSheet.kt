package com.example.healthcheckin.ui.screens.weight

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.healthcheckin.R
import com.example.healthcheckin.ui.screens.meal.MealDatePickerDialog
import com.example.healthcheckin.util.DateTimeUtil

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeightInputSheet(
    state: WeightInputUiState,
    mode: WeightInputMode,
    isSaving: Boolean,
    minDate: String,
    onDismiss: () -> Unit,
    onWeightChange: (String) -> Unit,
    onNoteChange: (String) -> Unit,
    onDateChange: (java.time.LocalDate) -> Unit,
    onSave: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showDatePicker by remember { mutableStateOf(false) }
    val minLocalDate = DateTimeUtil.parseLocalDate(minDate)
    val maxLocalDate = DateTimeUtil.todayLocalDate()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)) {
            Text(
                text = stringResource(
                    if (mode == WeightInputMode.EDIT) R.string.weight_edit_title else R.string.weight_record_title,
                ),
                style = MaterialTheme.typography.titleLarge,
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = state.weightText,
                onValueChange = onWeightChange,
                label = { Text(stringResource(R.string.weight_input_label)) },
                suffix = { Text(stringResource(R.string.weight_unit_kg)) },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                textStyle = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = DateTimeUtil.formatDashboardDate(DateTimeUtil.formatLocalDate(state.localDate)),
                onValueChange = {},
                readOnly = true,
                enabled = state.dateEditable,
                label = { Text(stringResource(R.string.weight_date_label)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (state.dateEditable) {
                            Modifier.clickable { showDatePicker = true }
                        } else {
                            Modifier
                        },
                    ),
                supportingText = if (!state.dateEditable) {
                    { Text(stringResource(R.string.weight_date_locked_hint)) }
                } else {
                    null
                },
            )
            if (state.dateEditable) {
                Text(
                    text = stringResource(R.string.weight_tap_to_change_date),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .padding(start = 4.dp, top = 4.dp)
                        .clickable { showDatePicker = true },
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = state.note,
                onValueChange = onNoteChange,
                label = { Text(stringResource(R.string.weight_note_label)) },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 3,
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onSave,
                enabled = !isSaving && state.weightText.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (isSaving) {
                    CircularProgressIndicator(modifier = Modifier.height(20.dp))
                } else {
                    Text(stringResource(R.string.meal_save))
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    if (showDatePicker && state.dateEditable) {
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
