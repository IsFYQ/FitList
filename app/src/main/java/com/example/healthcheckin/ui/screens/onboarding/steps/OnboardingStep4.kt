package com.example.healthcheckin.ui.screens.onboarding.steps

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.healthcheckin.R
import com.example.healthcheckin.util.ActivityLevel

@Composable
fun OnboardingStep4(
    selected: ActivityLevel,
    onSelected: (ActivityLevel) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = stringResource(R.string.onboarding_step4_title),
            style = MaterialTheme.typography.headlineSmall,
        )

        ActivityLevel.entries.forEach { level ->
            ActivityLevelItem(
                level = level,
                selected = selected == level,
                onClick = { onSelected(level) },
            )
        }
    }
}

@Composable
private fun ActivityLevelItem(
    level: ActivityLevel,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val title = when (level) {
        ActivityLevel.SEDENTARY -> stringResource(R.string.activity_sedentary_title)
        ActivityLevel.LIGHT -> stringResource(R.string.activity_light_title)
        ActivityLevel.MODERATE -> stringResource(R.string.activity_moderate_title)
        ActivityLevel.ACTIVE -> stringResource(R.string.activity_active_title)
        ActivityLevel.ATHLETE -> stringResource(R.string.activity_athlete_title)
    }
    val subtitle = when (level) {
        ActivityLevel.SEDENTARY -> stringResource(R.string.activity_sedentary_subtitle)
        ActivityLevel.LIGHT -> stringResource(R.string.activity_light_subtitle)
        ActivityLevel.MODERATE -> stringResource(R.string.activity_moderate_subtitle)
        ActivityLevel.ACTIVE -> stringResource(R.string.activity_active_subtitle)
        ActivityLevel.ATHLETE -> stringResource(R.string.activity_athlete_subtitle)
    }

    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
    ) {
        androidx.compose.foundation.layout.Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RadioButton(selected = selected, onClick = onClick)
            Column(modifier = Modifier.padding(start = 8.dp)) {
                Text(text = title, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                )
            }
        }
    }
}
