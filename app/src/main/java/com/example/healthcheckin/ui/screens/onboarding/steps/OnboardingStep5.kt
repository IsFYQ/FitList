package com.example.healthcheckin.ui.screens.onboarding.steps

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.healthcheckin.R
import com.example.healthcheckin.domain.algorithm.GoalCalculationResult
import com.example.healthcheckin.ui.theme.HealthCheckInColors
import com.example.healthcheckin.util.PrecisionUtil
import kotlin.math.abs

@Composable
fun OnboardingStep5(
    calculation: GoalCalculationResult,
    targetWeeks: Int,
    showExpanded: Boolean,
    onToggleExpanded: () -> Unit,
) {
    val budget = calculation.finalBudgetKcal
    val macro = calculation.macro
    val budgetResult = calculation.budget
    val bmr = calculation.bmr

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            text = stringResource(R.string.onboarding_step5_title),
            style = MaterialTheme.typography.headlineSmall,
        )

        Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Text(
                text = PrecisionUtil.formatCaloriesWithSeparator(budget),
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(text = stringResource(R.string.onboarding_budget_unit))
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            MacroCard(
                name = stringResource(R.string.onboarding_macro_protein),
                grams = macro.proteinG,
                percent = percentOfBudget(macro.proteinG * 4, budget),
                modifier = Modifier.weight(1f),
            )
            MacroCard(
                name = stringResource(R.string.onboarding_macro_carb),
                grams = macro.carbG,
                percent = percentOfBudget(macro.carbG * 4, budget),
                modifier = Modifier.weight(1f),
            )
            MacroCard(
                name = stringResource(R.string.onboarding_macro_fat),
                grams = macro.fatG,
                percent = percentOfBudget(macro.fatG * 9, budget),
                modifier = Modifier.weight(1f),
            )
        }

        TextButton(onClick = onToggleExpanded) {
            Text(
                text = if (showExpanded) {
                    stringResource(R.string.onboarding_calculation_collapse)
                } else {
                    stringResource(R.string.onboarding_calculation_expand)
                }
            )
        }

        if (showExpanded) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(stringResource(R.string.onboarding_calc_bmr, calculation.bmr.bmrKcal))
                    Text(stringResource(R.string.onboarding_calc_tdee, calculation.tdeeKcal))
                    val delta = budgetResult.actualDailyDelta
                    val deltaText = if (delta >= 0) "+$delta" else delta.toString()
                    Text(stringResource(R.string.onboarding_calc_delta, deltaText, budget))
                }
            }
        }

        if (bmr.bmrClamped) {
            WarningBanner(text = stringResource(R.string.onboarding_bmr_clamped_warning))
        }

        if (budgetResult.clamped && budgetResult.estWeeks != null) {
            val deltaAbs = abs(budgetResult.actualDailyDelta)
            WarningBanner(
                text = stringResource(
                    R.string.onboarding_budget_clamped_warning,
                    deltaAbs,
                    budgetResult.estWeeks!!,
                    targetWeeks,
                ) + if (budgetResult.estWeeks!! > 52) {
                    stringResource(R.string.onboarding_est_weeks_long_suffix)
                } else {
                    ""
                },
            )
        }

        if (calculation.macroBudgetAdjusted) {
            WarningBanner(
                text = stringResource(
                    R.string.onboarding_macro_adjusted_warning,
                    PrecisionUtil.formatCaloriesWithSeparator(budget),
                ),
            )
        }
    }
}

@Composable
private fun MacroCard(
    name: String,
    grams: Double,
    percent: Int,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
        ) {
            Text(text = name, style = MaterialTheme.typography.labelMedium)
            Text(text = "${PrecisionUtil.roundMacroDisplay(grams)}g", style = MaterialTheme.typography.titleMedium)
            Text(text = "$percent%", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun WarningBanner(text: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = HealthCheckInColors.CalorieWarn.copy(alpha = 0.15f)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(12.dp),
            color = HealthCheckInColors.CalorieWarn,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

private fun percentOfBudget(kcal: Double, budget: Int): Int {
    if (budget <= 0) return 0
    return PrecisionUtil.roundInt(kcal / budget * 100.0)
}
