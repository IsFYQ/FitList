package com.example.healthcheckin.ui.screens.exercise

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.healthcheckin.data.auth.SessionManager
import com.example.healthcheckin.data.local.dao.AppSettingDao
import com.example.healthcheckin.data.local.dao.ProfileDao
import com.example.healthcheckin.data.repository.ExerciseRepositoryImpl
import com.example.healthcheckin.domain.algorithm.ExerciseMetCalculator
import com.example.healthcheckin.domain.algorithm.ExerciseStreakCalculator
import com.example.healthcheckin.domain.analytics.AnalyticsTracker
import com.example.healthcheckin.domain.model.ExerciseRecordItem
import com.example.healthcheckin.domain.model.ExerciseWeekSummary
import com.example.healthcheckin.domain.model.SaveExerciseRequest
import com.example.healthcheckin.domain.repository.ExerciseRepository
import com.example.healthcheckin.util.DateTimeUtil
import com.example.healthcheckin.util.ExerciseType
import com.example.healthcheckin.util.P2ValidationConstants
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class ExerciseRecordSheetState(
    val exerciseType: ExerciseType = ExerciseType.RUNNING,
    val durationText: String = "30",
    val localDate: LocalDate = DateTimeUtil.todayLocalDate(),
    val customName: String = "",
    val customMetText: String = "4.0",
    val estimatedKcal: Int = 0,
    val validationError: String? = null,
)

data class ExerciseUiState(
    val records: List<ExerciseRecordItem> = emptyList(),
    val weekSummary: ExerciseWeekSummary? = null,
    val minDate: String = DateTimeUtil.todayLocalDateString(),
    val showRecordSheet: Boolean = false,
    val sheet: ExerciseRecordSheetState = ExerciseRecordSheetState(),
    val isSaving: Boolean = false,
    val snackbarMessage: String? = null,
    val errorMessage: String? = null,
)

@HiltViewModel
class ExerciseViewModel @Inject constructor(
    private val exerciseRepository: ExerciseRepository,
    private val sessionManager: SessionManager,
    private val profileDao: ProfileDao,
    private val appSettingDao: AppSettingDao,
    private val analyticsTracker: AnalyticsTracker,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExerciseUiState())
    val uiState: StateFlow<ExerciseUiState> = _uiState.asStateFlow()

    private var baselineWeightKg: Double = 70.0

    init {
        viewModelScope.launch {
            val userId = sessionManager.getUserId() ?: return@launch
            val profile = profileDao.getById(userId)
            baselineWeightKg = profile?.initialWeightKg ?: 70.0
            val minDate = DateTimeUtil.backfillMinDateString(
                profile?.registeredLocalDate ?: DateTimeUtil.todayLocalDateString(),
            )
            _uiState.update { it.copy(minDate = minDate) }
            refreshWeekSummary(userId)
            exerciseRepository.observeRecords(userId).collect { records ->
                _uiState.update { it.copy(records = records) }
                refreshWeekSummary(userId)
            }
        }
    }

    private suspend fun refreshWeekSummary(userId: String) {
        val summary = exerciseRepository.getWeekSummary(userId)
        _uiState.update { it.copy(weekSummary = summary) }
    }

    fun openRecordSheet() {
        _uiState.update {
            it.copy(
                showRecordSheet = true,
                sheet = ExerciseRecordSheetState(
                    estimatedKcal = estimateKcal(
                        ExerciseType.RUNNING,
                        30,
                        null,
                        4.0,
                    ),
                ),
                errorMessage = null,
            )
        }
    }

    fun dismissRecordSheet() = _uiState.update { it.copy(showRecordSheet = false) }

    fun setExerciseType(type: ExerciseType) {
        updateSheet { sheet ->
            val duration = sheet.durationText.toIntOrNull() ?: 30
            sheet.copy(
                exerciseType = type,
                estimatedKcal = estimateKcal(type, duration, sheet.customName, sheet.customMetText.toDoubleOrNull()),
                validationError = null,
            )
        }
    }

    fun setDurationText(text: String) {
        updateSheet { sheet ->
            sheet.copy(
                durationText = text.filter { it.isDigit() }.take(3),
                estimatedKcal = estimateKcal(
                    sheet.exerciseType,
                    text.toIntOrNull() ?: 0,
                    sheet.customName,
                    sheet.customMetText.toDoubleOrNull(),
                ),
                validationError = null,
            )
        }
    }

    fun setQuickDuration(minutes: Int) = setDurationText(minutes.toString())

    fun setLocalDate(date: LocalDate) {
        updateSheet { it.copy(localDate = date, validationError = null) }
    }

    fun setCustomName(name: String) {
        updateSheet { sheet ->
            sheet.copy(
                customName = name.take(P2ValidationConstants.EXERCISE_CUSTOM_NAME_MAX),
                validationError = null,
            )
        }
    }

    fun setCustomMetText(text: String) {
        updateSheet { sheet ->
            sheet.copy(
                customMetText = text,
                estimatedKcal = estimateKcal(
                    sheet.exerciseType,
                    sheet.durationText.toIntOrNull() ?: 0,
                    sheet.customName,
                    text.toDoubleOrNull(),
                ),
                validationError = null,
            )
        }
    }

    fun saveRecord() {
        val sheet = _uiState.value.sheet
        val duration = sheet.durationText.toIntOrNull()
        val validationError = when {
            duration == null || duration !in P2ValidationConstants.EXERCISE_DURATION_MIN..P2ValidationConstants.EXERCISE_DURATION_MAX ->
                "exercise_error_duration"
            sheet.exerciseType == ExerciseType.CUSTOM && sheet.customName.isBlank() ->
                "exercise_error_custom_name"
            sheet.exerciseType == ExerciseType.CUSTOM -> {
                val met = sheet.customMetText.toDoubleOrNull()
                if (met == null || met !in P2ValidationConstants.EXERCISE_MET_MIN..P2ValidationConstants.EXERCISE_MET_MAX) {
                    "exercise_error_met"
                } else {
                    null
                }
            }
            else -> null
        }
        if (validationError != null) {
            _uiState.update { it.copy(sheet = sheet.copy(validationError = validationError)) }
            return
        }

        viewModelScope.launch {
            val userId = sessionManager.getUserId() ?: return@launch
            val localDateStr = DateTimeUtil.formatLocalDate(sheet.localDate)
            val isBackfill = localDateStr != DateTimeUtil.todayLocalDateString()
            val previousBest = appSettingDao.get(ExerciseRepositoryImpl.BEST_STREAK_KEY)?.valueJson?.toIntOrNull() ?: 0
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }
            val request = SaveExerciseRequest(
                exerciseType = sheet.exerciseType,
                customName = if (sheet.exerciseType == ExerciseType.CUSTOM) sheet.customName.trim() else null,
                customMet = if (sheet.exerciseType == ExerciseType.CUSTOM) sheet.customMetText.toDoubleOrNull() else null,
                durationMinutes = duration!!,
                localDate = localDateStr,
            )
            exerciseRepository.save(userId, request)
                .onSuccess { saved ->
                    analyticsTracker.track(
                        "exercise_logged",
                        mapOf(
                            "exercise_type" to saved.exerciseType.name,
                            "duration_minutes" to saved.durationMinutes,
                            "estimated_kcal" to saved.estimatedKcal,
                            "is_backfill" to isBackfill,
                        ),
                    )
                    var milestoneMessage: String? = null
                    if (!isBackfill) {
                        val records = _uiState.value.records + saved
                        val minutesByDate = ExerciseStreakCalculator.aggregateMinutesByDate(
                            records.map { it.localDate to it.durationMinutes },
                        )
                        val streak = ExerciseStreakCalculator.computeStreak(
                            minutesByDate,
                            DateTimeUtil.todayLocalDate(),
                            previousBest,
                            allowMilestoneToast = true,
                        )
                        analyticsTracker.track(
                            "exercise_streak_updated",
                            mapOf(
                                "current_streak" to streak.currentStreak,
                                "is_new_best" to streak.isNewBest,
                            ),
                        )
                        streak.milestoneDays?.let { days ->
                            analyticsTracker.track("streak_milestone_reached", mapOf("days" to days))
                            milestoneMessage = "exercise_streak_milestone:$days"
                        }
                    }
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            showRecordSheet = false,
                            snackbarMessage = milestoneMessage,
                        )
                    }
                }
                .onFailure {
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            errorMessage = "exercise_save_failed",
                        )
                    }
                }
        }
    }

    fun deleteRecord(recordId: String) {
        viewModelScope.launch {
            exerciseRepository.delete(recordId)
                .onSuccess {
                    analyticsTracker.track("exercise_deleted")
                }
        }
    }

    fun clearSnackbar() = _uiState.update { it.copy(snackbarMessage = null) }

    fun clearError() = _uiState.update { it.copy(errorMessage = null) }

    private fun updateSheet(transform: (ExerciseRecordSheetState) -> ExerciseRecordSheetState) {
        _uiState.update { state ->
            state.copy(sheet = transform(state.sheet))
        }
    }

    private fun estimateKcal(
        type: ExerciseType,
        durationMinutes: Int,
        @Suppress("UNUSED_PARAMETER") customName: String?,
        customMet: Double?,
    ): Int {
        if (durationMinutes <= 0) return 0
        val met = ExerciseMetCalculator.metFor(type, customMet)
        return ExerciseMetCalculator.estimatedKcal(met, baselineWeightKg, durationMinutes)
    }
}
