package com.example.healthcheckin.ui.screens.onboarding

import com.example.healthcheckin.domain.algorithm.GoalCalculationResult
import com.example.healthcheckin.util.ActivityLevel
import com.example.healthcheckin.util.GoalType
import com.example.healthcheckin.util.Sex
import com.example.healthcheckin.util.Validators

data class OnboardingUiState(
    val currentStep: Int = 1,
    val isEditMode: Boolean = false,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,

    val sex: Sex? = null,
    val birthYearMonth: String = "1995-01",
    val heightCm: String = "",
    val currentWeightKg: String = "",
    val targetWeightKg: String = "",
    val targetWeeks: Int = 12,
    val activityLevel: ActivityLevel = ActivityLevel.LIGHT,

    val sexError: String? = null,
    val birthError: String? = null,
    val heightError: String? = null,
    val currentWeightError: String? = null,
    val targetWeightError: String? = null,

    val calculation: GoalCalculationResult? = null,
    val showCalculationExpanded: Boolean = false,
    val showWeightDiffConfirm: Boolean = false,
    val pendingWeightDiffKg: Double? = null,

    val errorMessage: String? = null,
    val saveSuccess: Boolean = false,
    val promptWeightRecord: Boolean = false,
    val weightToRecord: Double? = null,
    val previousCurrentWeightKg: Double? = null,
) {
    val totalSteps: Int = 5
    val canProceed: Boolean get() = when (currentStep) {
        1 -> sex != null &&
            Validators.validateBirthYearMonth(birthYearMonth).isValid &&
            sexError == null && birthError == null
        2 -> heightError == null && currentWeightError == null &&
            heightCm.isNotBlank() && currentWeightKg.isNotBlank() &&
            Validators.parseDecimalInput(heightCm)?.let {
                Validators.validateHeightCm(it).isValid
            } == true &&
            Validators.parseDecimalInput(currentWeightKg)?.let {
                Validators.validateWeightKg(it).isValid
            } == true
        3 -> targetWeightError == null && targetWeightKg.isNotBlank() &&
            Validators.parseDecimalInput(targetWeightKg)?.let {
                Validators.validateWeightKg(it).isValid
            } == true
        4 -> true
        5 -> calculation != null && !isSaving
        else -> false
    }

    val goalType: GoalType? get() = calculation?.budget?.goalType
        ?: run {
            val current = currentWeightKg.toDoubleOrNull() ?: return null
            val target = targetWeightKg.toDoubleOrNull() ?: return null
            com.example.healthcheckin.domain.algorithm.BudgetCalculator.determineGoalType(current, target)
        }

    val isMaintainGoal: Boolean get() = goalType == GoalType.MAINTAIN
}
