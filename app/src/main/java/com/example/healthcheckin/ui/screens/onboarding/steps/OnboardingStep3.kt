package com.example.healthcheckin.ui.screens.onboarding.steps

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.healthcheckin.R
import com.example.healthcheckin.util.ValidationConstants

@Composable
fun OnboardingStep3(
    targetWeightKg: String,
    targetWeeks: Int,
    goalPreview: String,
    isMaintain: Boolean,
    targetWeightError: String?,
    onTargetWeightChanged: (String) -> Unit,
    onTargetWeeksChanged: (Int) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            text = stringResource(R.string.onboarding_step3_title),
            style = MaterialTheme.typography.headlineSmall,
        )

        OutlinedTextField(
            value = targetWeightKg,
            onValueChange = onTargetWeightChanged,
            label = { Text(stringResource(R.string.onboarding_target_weight_label)) },
            suffix = { Text(stringResource(R.string.onboarding_unit_kg)) },
            isError = targetWeightError != null,
            supportingText = targetWeightError?.let { { Text(it) } },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        if (goalPreview.isNotBlank()) {
            Text(
                text = goalPreview,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }

        Text(
            text = if (isMaintain) {
                stringResource(R.string.onboarding_maintain_weeks_disabled)
            } else {
                stringResource(R.string.onboarding_target_weeks_label, targetWeeks)
            },
            style = MaterialTheme.typography.bodyLarge,
        )

        Slider(
            value = targetWeeks.toFloat(),
            onValueChange = { onTargetWeeksChanged(it.toInt()) },
            valueRange = ValidationConstants.TARGET_WEEKS_MIN.toFloat()..ValidationConstants.TARGET_WEEKS_MAX.toFloat(),
            steps = ValidationConstants.TARGET_WEEKS_MAX - ValidationConstants.TARGET_WEEKS_MIN - 1,
            enabled = !isMaintain,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
