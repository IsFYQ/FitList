package com.example.healthcheckin.ui.screens.onboarding.steps

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.healthcheckin.R

@Composable
fun OnboardingStep2(
    heightCm: String,
    currentWeightKg: String,
    heightError: String?,
    currentWeightError: String?,
    onHeightChanged: (String) -> Unit,
    onCurrentWeightChanged: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            text = stringResource(R.string.onboarding_step2_title),
            style = MaterialTheme.typography.headlineSmall,
        )

        OutlinedTextField(
            value = heightCm,
            onValueChange = onHeightChanged,
            label = { Text(stringResource(R.string.onboarding_height_label)) },
            suffix = { Text(stringResource(R.string.onboarding_unit_cm)) },
            placeholder = { Text(stringResource(R.string.onboarding_height_placeholder)) },
            isError = heightError != null,
            supportingText = heightError?.let { { Text(it) } },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        OutlinedTextField(
            value = currentWeightKg,
            onValueChange = onCurrentWeightChanged,
            label = { Text(stringResource(R.string.onboarding_current_weight_label)) },
            suffix = { Text(stringResource(R.string.onboarding_unit_kg)) },
            placeholder = { Text(stringResource(R.string.onboarding_weight_placeholder)) },
            isError = currentWeightError != null,
            supportingText = currentWeightError?.let { { Text(it) } },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
