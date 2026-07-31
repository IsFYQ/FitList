package com.example.healthcheckin.ui.screens.onboarding.steps

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.healthcheckin.R
import com.example.healthcheckin.util.DateTimeUtil
import com.example.healthcheckin.util.Sex
import com.example.healthcheckin.util.ValidationConstants

@Composable
fun OnboardingStep1(
    sex: Sex?,
    birthYearMonth: String,
    sexError: String?,
    birthError: String?,
    onSexSelected: (Sex) -> Unit,
    onBirthYearMonthChanged: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            text = stringResource(R.string.onboarding_step1_title),
            style = MaterialTheme.typography.headlineSmall,
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SexCard(
                label = stringResource(R.string.onboarding_sex_male),
                selected = sex == Sex.MALE,
                onClick = { onSexSelected(Sex.MALE) },
                modifier = Modifier.weight(1f),
            )
            SexCard(
                label = stringResource(R.string.onboarding_sex_female),
                selected = sex == Sex.FEMALE,
                onClick = { onSexSelected(Sex.FEMALE) },
                modifier = Modifier.weight(1f),
            )
        }
        if (sexError != null) {
            Text(text = sexError, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }

        BirthYearMonthPicker(
            value = birthYearMonth,
            error = birthError,
            onValueChanged = onBirthYearMonthChanged,
        )
    }
}

@Composable
private fun SexCard(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = if (selected) 2.dp else 1.dp,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
        ),
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(vertical = 32.dp, horizontal = 16.dp),
            style = MaterialTheme.typography.titleMedium,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BirthYearMonthPicker(
    value: String,
    error: String?,
    onValueChanged: (String) -> Unit,
) {
    val today = DateTimeUtil.todayLocalDate()
    val minYear = today.year - ValidationConstants.AGE_MAX
    val maxYear = today.year - ValidationConstants.AGE_MIN
    val years = (minYear..maxYear).toList().reversed()
    val months = (1..12).map { it.toString().padStart(2, '0') }

    val parts = value.split("-")
    var selectedYear by remember(value) { mutableStateOf(parts.getOrNull(0)?.toIntOrNull() ?: 1995) }
    var selectedMonth by remember(value) { mutableStateOf(parts.getOrNull(1) ?: "01") }

    var yearExpanded by remember { mutableStateOf(false) }
    var monthExpanded by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text = stringResource(R.string.onboarding_birth_label), style = MaterialTheme.typography.titleSmall)

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ExposedDropdownMenuBox(
                expanded = yearExpanded,
                onExpandedChange = { yearExpanded = it },
                modifier = Modifier.weight(1f),
            ) {
                OutlinedTextField(
                    value = selectedYear.toString(),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.onboarding_birth_year)) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = yearExpanded) },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth(),
                )
                DropdownMenu(
                    expanded = yearExpanded,
                    onDismissRequest = { yearExpanded = false },
                ) {
                    years.forEach { year ->
                        DropdownMenuItem(
                            text = { Text(year.toString()) },
                            onClick = {
                                selectedYear = year
                                yearExpanded = false
                                onValueChanged("$selectedYear-$selectedMonth")
                            },
                        )
                    }
                }
            }

            ExposedDropdownMenuBox(
                expanded = monthExpanded,
                onExpandedChange = { monthExpanded = it },
                modifier = Modifier.weight(1f),
            ) {
                OutlinedTextField(
                    value = selectedMonth,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.onboarding_birth_month)) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = monthExpanded) },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth(),
                )
                DropdownMenu(
                    expanded = monthExpanded,
                    onDismissRequest = { monthExpanded = false },
                ) {
                    months.forEach { month ->
                        DropdownMenuItem(
                            text = { Text(month) },
                            onClick = {
                                selectedMonth = month
                                monthExpanded = false
                                onValueChanged("$selectedYear-$selectedMonth")
                            },
                        )
                    }
                }
            }
        }

        if (error != null) {
            Text(text = error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
    }
}
