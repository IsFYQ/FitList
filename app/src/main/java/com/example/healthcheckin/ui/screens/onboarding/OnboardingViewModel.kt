package com.example.healthcheckin.ui.screens.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.healthcheckin.data.auth.SessionManager
import com.example.healthcheckin.domain.algorithm.GoalCalculationService
import com.example.healthcheckin.domain.analytics.AnalyticsEvents
import com.example.healthcheckin.domain.analytics.AnalyticsTracker
import com.example.healthcheckin.domain.model.GoalSaveRequest
import com.example.healthcheckin.domain.repository.GoalRepository
import com.example.healthcheckin.util.ActivityLevel
import com.example.healthcheckin.util.DateTimeUtil
import com.example.healthcheckin.util.GoalType
import com.example.healthcheckin.util.PrecisionUtil
import com.example.healthcheckin.util.Sex
import com.example.healthcheckin.util.ValidationConstants
import com.example.healthcheckin.util.ValidationError
import com.example.healthcheckin.util.Validators
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.abs

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val goalRepository: GoalRepository,
    private val sessionManager: SessionManager,
    private val analyticsTracker: AnalyticsTracker,
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    fun initialize(isEditMode: Boolean) {
        viewModelScope.launch {
            _uiState.update { it.copy(isEditMode = isEditMode, isLoading = true) }
            val userId = sessionManager.getUserId() ?: return@launch
            if (isEditMode) {
                val form = goalRepository.loadFormFromProfile(userId)
                val activeGoal = goalRepository.getActiveGoal(userId)
                if (form != null) {
                    _uiState.update {
                        it.copy(
                            sex = form.sex,
                            birthYearMonth = form.birthYearMonth,
                            heightCm = form.heightCm,
                            currentWeightKg = form.currentWeightKg,
                            targetWeightKg = form.targetWeightKg,
                            targetWeeks = form.targetWeeks,
                            activityLevel = form.activityLevel,
                            previousCurrentWeightKg = activeGoal?.currentWeightKg,
                            isLoading = false,
                        )
                    }
                    validateStep1(showErrors = false)
                    validateStep2(showErrors = false)
                    validateStep3(showErrors = false)
                } else {
                    _uiState.update { it.copy(isLoading = false) }
                }
            } else {
                _uiState.update {
                    it.copy(
                        birthYearMonth = ValidationConstants.DEFAULT_BIRTH_YEAR_MONTH,
                        targetWeeks = ValidationConstants.DEFAULT_TARGET_WEEKS,
                        isLoading = false,
                    )
                }
                validateStep1(showErrors = false)
            }
        }
    }

    fun onSexSelected(sex: Sex) {
        _uiState.update { it.copy(sex = sex, sexError = null) }
    }

    fun onBirthYearMonthChanged(value: String) {
        _uiState.update { it.copy(birthYearMonth = value) }
        validateStep1(showErrors = true)
    }

    fun onHeightChanged(value: String) {
        val filtered = Validators.filterDecimalInput(value)
        _uiState.update { it.copy(heightCm = filtered) }
        validateStep2(showErrors = _uiState.value.heightCm.isNotBlank())
    }

    fun onCurrentWeightChanged(value: String) {
        val filtered = Validators.filterDecimalInput(value)
        _uiState.update { it.copy(currentWeightKg = filtered) }
        validateStep2(showErrors = _uiState.value.currentWeightKg.isNotBlank())
        validateStep3(showErrors = false)
    }

    fun onTargetWeightChanged(value: String) {
        val filtered = Validators.filterDecimalInput(value)
        _uiState.update { it.copy(targetWeightKg = filtered) }
        validateStep3(showErrors = _uiState.value.targetWeightKg.isNotBlank())
    }

    fun onTargetWeeksChanged(weeks: Int) {
        _uiState.update { it.copy(targetWeeks = weeks.coerceIn(ValidationConstants.TARGET_WEEKS_MIN, ValidationConstants.TARGET_WEEKS_MAX)) }
    }

    fun onActivityLevelSelected(level: ActivityLevel) {
        _uiState.update { it.copy(activityLevel = level) }
    }

    fun toggleCalculationExpanded() {
        _uiState.update { it.copy(showCalculationExpanded = !it.showCalculationExpanded) }
    }

    fun nextStep() {
        val state = _uiState.value
        when (state.currentStep) {
            1 -> {
                validateStep1(showErrors = true)
                if (!_uiState.value.canProceed) return
            }
            2 -> {
                validateStep2(showErrors = true)
                if (!_uiState.value.canProceed) return
            }
            3 -> {
                validateStep3(showErrors = true)
                if (!_uiState.value.canProceed) return
                val current = state.currentWeightKg.toDoubleOrNull() ?: return
                val target = state.targetWeightKg.toDoubleOrNull() ?: return
                if (abs(target - current) > ValidationConstants.WEIGHT_DIFF_CONFIRM_KG) {
                    _uiState.update {
                        it.copy(
                            showWeightDiffConfirm = true,
                            pendingWeightDiffKg = abs(target - current),
                        )
                    }
                    return
                }
            }
            4 -> {
                computeAndGoToResult()
                return
            }
            else -> return
        }
        if (state.currentStep < 4) {
            _uiState.update { it.copy(currentStep = state.currentStep + 1) }
        }
    }

    fun confirmWeightDiffAndProceed() {
        _uiState.update { it.copy(showWeightDiffConfirm = false, pendingWeightDiffKg = null) }
        val step = _uiState.value.currentStep
        if (step == 3) {
            _uiState.update { it.copy(currentStep = 4) }
        }
    }

    fun dismissWeightDiffConfirm() {
        _uiState.update { it.copy(showWeightDiffConfirm = false, pendingWeightDiffKg = null) }
    }

    fun previousStep() {
        _uiState.update { state ->
            if (state.currentStep > 1) state.copy(currentStep = state.currentStep - 1) else state
        }
    }

    fun saveGoal() {
        val state = _uiState.value
        val userId = sessionManager.getUserId() ?: return
        val sex = state.sex ?: return
        val height = state.heightCm.toDoubleOrNull() ?: return
        val current = state.currentWeightKg.toDoubleOrNull() ?: return
        val target = state.targetWeightKg.toDoubleOrNull() ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }
            val isFirstTime = !goalRepository.hasCompletedOnboarding(userId)
            val result = goalRepository.saveGoal(
                userId = userId,
                request = GoalSaveRequest(
                    sex = sex,
                    birthYearMonth = state.birthYearMonth,
                    heightCm = height,
                    currentWeightKg = current,
                    targetWeightKg = target,
                    targetWeeks = if (state.isMaintainGoal) ValidationConstants.DEFAULT_TARGET_WEEKS else state.targetWeeks,
                    activityLevel = state.activityLevel,
                    isFirstTime = isFirstTime,
                    previousCurrentWeightKg = state.previousCurrentWeightKg,
                ),
            )
            result.fold(
                onSuccess = { saveResult ->
                    val eventName = if (isFirstTime) {
                        AnalyticsEvents.ONBOARDING_COMPLETE
                    } else {
                        AnalyticsEvents.GOAL_UPDATED
                    }
                    analyticsTracker.track(
                        eventName,
                        mapOf(
                            "goal_type" to (state.goalType?.name ?: GoalType.MAINTAIN.name),
                            "target_weeks" to state.targetWeeks,
                            "budget_kcal" to (state.calculation?.budget?.budgetKcal ?: 0),
                            "budget_delta_kcal" to 0,
                        ),
                    )
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            saveSuccess = true,
                            promptWeightRecord = saveResult.promptWeightRecord,
                            weightToRecord = saveResult.weightToRecord,
                        )
                    }
                    if (!saveResult.promptWeightRecord) {
                        // saveSuccess handled by LaunchedEffect for immediate navigation
                    }
                },
                onFailure = {
                    _uiState.update {
                        it.copy(isSaving = false, errorMessage = "保存失败，请重试")
                    }
                },
            )
        }
    }

    fun consumeSaveSuccess() {
        _uiState.update { it.copy(saveSuccess = false, promptWeightRecord = false, weightToRecord = null) }
    }

    fun recordWeightAndComplete(onDone: () -> Unit) {
        val weight = _uiState.value.weightToRecord ?: return
        viewModelScope.launch {
            val userId = sessionManager.getUserId() ?: return@launch
            goalRepository.recordWeight(userId, weight)
            consumeSaveSuccess()
            onDone()
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    private fun computeAndGoToResult() {
        _uiState.update { it.copy(currentStep = 5) }
        computeResult()
    }

    private fun computeResult() {
        val state = _uiState.value
        val sex = state.sex ?: return
        val height = state.heightCm.toDoubleOrNull() ?: return
        val current = state.currentWeightKg.toDoubleOrNull() ?: return
        val target = state.targetWeightKg.toDoubleOrNull() ?: return
        val age = DateTimeUtil.ageYears(state.birthYearMonth)
        val weeks = if (state.isMaintainGoal) ValidationConstants.DEFAULT_TARGET_WEEKS else state.targetWeeks

        val calculation = GoalCalculationService.calculate(
            sex = sex,
            currentWeightKg = current,
            heightCm = height,
            ageYears = age,
            targetWeightKg = target,
            targetWeeks = weeks,
            activityLevel = state.activityLevel,
        )
        _uiState.update { it.copy(calculation = calculation) }
    }

    private fun validateStep1(showErrors: Boolean) {
        val state = _uiState.value
        val birthResult = Validators.validateBirthYearMonth(state.birthYearMonth)
        _uiState.update {
            it.copy(
                sexError = if (showErrors && state.sex == null) "请选择性别" else null,
                birthError = if (showErrors && birthResult is com.example.healthcheckin.util.ValidationResult.Error) {
                    when (birthResult.error) {
                        ValidationError.AGE_OUT_OF_RANGE -> "年龄需在14-100岁之间"
                        else -> "年龄需在14-100岁之间"
                    }
                } else if (birthResult is com.example.healthcheckin.util.ValidationResult.Error) {
                    null
                } else {
                    null
                },
            )
        }
        if (birthResult is com.example.healthcheckin.util.ValidationResult.Error && showErrors) {
            _uiState.update { it.copy(birthError = "年龄需在14-100岁之间") }
        } else if (birthResult.isValid) {
            _uiState.update { it.copy(birthError = null) }
        }
    }

    private fun validateStep2(showErrors: Boolean) {
        val height = Validators.parseDecimalInput(_uiState.value.heightCm)
        val weight = Validators.parseDecimalInput(_uiState.value.currentWeightKg)
        _uiState.update {
            it.copy(
                heightError = if (showErrors && (height == null || Validators.validateHeightCm(height) is com.example.healthcheckin.util.ValidationResult.Error)) {
                    "身高需在100.0-250.0cm之间"
                } else {
                    null
                },
                currentWeightError = if (showErrors && (weight == null || Validators.validateWeightKg(weight) is com.example.healthcheckin.util.ValidationResult.Error)) {
                    "体重需在25.0-300.0kg之间"
                } else {
                    null
                },
            )
        }
    }

    private fun validateStep3(showErrors: Boolean) {
        val weight = Validators.parseDecimalInput(_uiState.value.targetWeightKg)
        _uiState.update {
            it.copy(
                targetWeightError = if (showErrors && (weight == null || Validators.validateWeightKg(weight) is com.example.healthcheckin.util.ValidationResult.Error)) {
                    "目标体重需在25.0-300.0kg之间"
                } else {
                    null
                },
            )
        }
    }

    fun goalPreviewText(): String {
        val state = _uiState.value
        val current = state.currentWeightKg.toDoubleOrNull() ?: return ""
        val target = state.targetWeightKg.toDoubleOrNull() ?: return ""
        val type = BudgetCalculatorPreview.determineGoalType(current, target)
        return when (type) {
            GoalType.MAINTAIN -> "保持当前体重"
            GoalType.LOSE -> "目标：减${PrecisionUtil.roundWeightDisplay(abs(target - current))}kg"
            GoalType.GAIN -> "目标：增${PrecisionUtil.roundWeightDisplay(abs(target - current))}kg"
        }
    }
}

private object BudgetCalculatorPreview {
    fun determineGoalType(current: Double, target: Double) =
        com.example.healthcheckin.domain.algorithm.BudgetCalculator.determineGoalType(current, target)
}
