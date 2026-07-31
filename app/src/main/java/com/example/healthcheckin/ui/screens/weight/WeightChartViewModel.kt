package com.example.healthcheckin.ui.screens.weight

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.healthcheckin.data.auth.SessionManager
import com.example.healthcheckin.data.local.dao.ProfileDao
import com.example.healthcheckin.data.repository.WeightOverwriteRequiredException
import com.example.healthcheckin.domain.algorithm.WeightProgressCalculator
import com.example.healthcheckin.data.analytics.AnalyticsParamBuilder
import com.example.healthcheckin.domain.analytics.AnalyticsEvents
import com.example.healthcheckin.domain.analytics.AnalyticsTracker
import com.example.healthcheckin.domain.model.SaveWeightRequest
import com.example.healthcheckin.domain.model.WeightChartRange
import com.example.healthcheckin.domain.model.WeightProgressInfo
import com.example.healthcheckin.domain.model.WeightRecordItem
import com.example.healthcheckin.domain.repository.GoalRepository
import com.example.healthcheckin.domain.repository.WeightRepository
import com.example.healthcheckin.util.DateTimeUtil
import com.example.healthcheckin.util.GoalType
import com.example.healthcheckin.util.PrecisionUtil
import com.example.healthcheckin.util.ValidationResult
import com.example.healthcheckin.util.Validators
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject
import kotlin.math.abs

@HiltViewModel
class WeightChartViewModel @Inject constructor(
    private val weightRepository: WeightRepository,
    private val goalRepository: GoalRepository,
    private val sessionManager: SessionManager,
    private val profileDao: ProfileDao,
    private val analyticsTracker: AnalyticsTracker,
) : ViewModel() {

    private val _uiState = MutableStateFlow(WeightChartUiState())
    val uiState: StateFlow<WeightChartUiState> = _uiState.asStateFlow()

    private var allRecords: List<WeightRecordItem> = emptyList()

    init {
        viewModelScope.launch {
            val userId = sessionManager.getUserId() ?: return@launch
            val profile = profileDao.getById(userId)
            val minDate = DateTimeUtil.backfillMinDateString(
                profile?.registeredLocalDate ?: DateTimeUtil.todayLocalDateString(),
            )
            _uiState.update { it.copy(minDate = minDate) }

            combine(
                weightRepository.observeAllRecords(userId),
                goalRepository.observeActiveGoal(userId),
            ) { records, goal ->
                Triple(records, goal, profile)
            }.collect { (records, goal, prof) ->
                allRecords = records
                val latest = records.firstOrNull()
                val goalType = goal?.goalType?.let { runCatching { GoalType.valueOf(it) }.getOrNull() }
                val initial = prof?.initialWeightKg ?: goal?.currentWeightKg
                val target = goal?.targetWeightKg
                val progress = if (goal != null && latest != null && initial != null && target != null && goalType != null) {
                    WeightProgressInfo(
                        initialWeightKg = initial,
                        targetWeightKg = target,
                        goalType = goalType,
                        progressPercent = WeightProgressCalculator.progressPercent(
                            goalType, initial, target, latest.weightKg,
                        ),
                        maintainDistanceKg = if (goalType == GoalType.MAINTAIN) {
                            WeightProgressCalculator.maintainDistanceKg(target, latest.weightKg)
                        } else {
                            null
                        },
                    )
                } else {
                    null
                }
                _uiState.update { state ->
                    state.copy(
                        latestRecord = latest,
                        historyRecords = records,
                        progress = progress,
                        targetWeightKg = if (goalType != GoalType.MAINTAIN) target else null,
                        chartRecords = filterByRange(records, state.selectedRange),
                    )
                }
            }
        }
    }

    fun selectRange(range: WeightChartRange) {
        _uiState.update {
            it.copy(
                selectedRange = range,
                chartRecords = filterByRange(allRecords, range),
            )
        }
    }

    fun openCreateSheet() {
        val latest = _uiState.value.latestRecord
        _uiState.update {
            it.copy(
                showInputSheet = true,
                inputMode = WeightInputMode.CREATE,
                inputState = WeightInputUiState(
                    weightText = latest?.weightKg?.let { formatWeight(it) }.orEmpty(),
                    localDate = DateTimeUtil.todayLocalDate(),
                    dateEditable = true,
                ),
            )
        }
    }

    fun openEditSheet(record: WeightRecordItem) {
        _uiState.update {
            it.copy(
                showInputSheet = true,
                inputMode = WeightInputMode.EDIT,
                inputState = WeightInputUiState(
                    recordId = record.id,
                    weightText = formatWeight(record.weightKg),
                    localDate = DateTimeUtil.parseLocalDate(record.localDate),
                    note = record.note.orEmpty(),
                    dateEditable = false,
                ),
            )
        }
    }

    fun dismissInputSheet() {
        _uiState.update {
            it.copy(showInputSheet = false, overwritePrompt = null, largeDiffPrompt = null)
        }
    }

    fun updateWeightText(text: String) {
        _uiState.update {
            it.copy(inputState = it.inputState.copy(weightText = Validators.filterDecimalInput(text)))
        }
    }

    fun updateNote(text: String) {
        _uiState.update {
            it.copy(inputState = it.inputState.copy(note = text.take(100)))
        }
    }

    fun updateInputDate(date: LocalDate) {
        _uiState.update {
            it.copy(inputState = it.inputState.copy(localDate = date))
        }
    }

    fun requestDelete(record: WeightRecordItem) {
        _uiState.update { it.copy(deleteTarget = record) }
    }

    fun dismissDeleteDialog() {
        _uiState.update { it.copy(deleteTarget = null) }
    }

    fun confirmDelete() {
        val target = _uiState.value.deleteTarget ?: return
        viewModelScope.launch {
            weightRepository.deleteWeight(target.id)
            _uiState.update { it.copy(deleteTarget = null) }
        }
    }

    fun saveWeight(overwrite: Boolean = false, skipLargeDiffCheck: Boolean = false) {
        val state = _uiState.value
        if (state.isSaving) return
        val weight = Validators.parseDecimalInput(state.inputState.weightText) ?: return
        if (Validators.validateWeightKg(weight) !is ValidationResult.Valid<*>) {
            _uiState.update { it.copy(errorMessage = "weight_error_range") }
            return
        }

        if (!skipLargeDiffCheck && state.inputMode == WeightInputMode.CREATE) {
            val previous = findPreviousForDate(state.inputState.localDate)
            previous?.let { prev ->
                val diff = abs(weight - prev.weightKg)
                if (diff > 5.0) {
                    _uiState.update { it.copy(largeDiffPrompt = diff) }
                    return
                }
            }
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }
            when (state.inputMode) {
                WeightInputMode.CREATE -> {
                    val localDate = DateTimeUtil.formatLocalDate(state.inputState.localDate)
                    weightRepository.saveWeight(
                        userId = sessionManager.getUserId() ?: return@launch,
                        request = SaveWeightRequest(
                            weightKg = weight,
                            localDate = localDate,
                            note = state.inputState.note.takeIf { it.isNotBlank() },
                        ),
                        overwrite = overwrite,
                    ).fold(
                        onSuccess = { saved ->
                            analyticsTracker.track(
                                AnalyticsEvents.WEIGHT_RECORDED,
                                mapOf(
                                    "delta_bucket" to AnalyticsParamBuilder.weightDeltaBucket(
                                        saved.deltaKg,
                                        saved.deltaKg == null,
                                    ),
                                    "is_backfill" to state.inputState.localDate.isBefore(DateTimeUtil.todayLocalDate()),
                                    "is_overwrite" to overwrite,
                                ),
                            )
                            _uiState.update {
                                it.copy(isSaving = false, showInputSheet = false, overwritePrompt = null)
                            }
                        },
                        onFailure = { error ->
                            when (error) {
                                is WeightOverwriteRequiredException -> {
                                    _uiState.update {
                                        it.copy(
                                            isSaving = false,
                                            overwritePrompt = WeightOverwritePrompt(
                                                localDate = DateTimeUtil.formatLocalDate(state.inputState.localDate),
                                                existingWeightKg = error.existingWeightKg,
                                                newWeightKg = weight,
                                            ),
                                        )
                                    }
                                }
                                else -> {
                                    _uiState.update {
                                        it.copy(isSaving = false, errorMessage = "weight_save_failed")
                                    }
                                }
                            }
                        },
                    )
                }
                WeightInputMode.EDIT -> {
                    val recordId = state.inputState.recordId ?: return@launch
                    weightRepository.updateWeight(
                        recordId = recordId,
                        weightKg = weight,
                        note = state.inputState.note.takeIf { it.isNotBlank() },
                    ).fold(
                        onSuccess = {
                            _uiState.update {
                                it.copy(isSaving = false, showInputSheet = false)
                            }
                        },
                        onFailure = {
                            _uiState.update {
                                it.copy(isSaving = false, errorMessage = "weight_save_failed")
                            }
                        },
                    )
                }
            }
        }
    }

    fun confirmOverwrite() {
        _uiState.update { it.copy(overwritePrompt = null) }
        saveWeight(overwrite = true, skipLargeDiffCheck = true)
    }

    fun dismissOverwriteDialog() {
        _uiState.update { it.copy(overwritePrompt = null) }
    }

    fun confirmLargeDiff() {
        _uiState.update { it.copy(largeDiffPrompt = null) }
        saveWeight(skipLargeDiffCheck = true)
    }

    fun dismissLargeDiffDialog() {
        _uiState.update { it.copy(largeDiffPrompt = null) }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    private fun filterByRange(
        records: List<WeightRecordItem>,
        range: WeightChartRange,
    ): List<WeightRecordItem> {
        if (records.isEmpty()) return emptyList()
        val end = DateTimeUtil.todayLocalDate()
        val start = end.minusDays(range.days - 1)
        return records
            .filter {
                val date = DateTimeUtil.parseLocalDate(it.localDate)
                !date.isBefore(start) && !date.isAfter(end)
            }
            .sortedBy { it.localDate }
    }

    private fun findPreviousForDate(date: LocalDate): WeightRecordItem? {
        val dateStr = DateTimeUtil.formatLocalDate(date)
        return allRecords.firstOrNull { it.localDate < dateStr }
    }

    private fun formatWeight(value: Double): String =
        PrecisionUtil.roundWeightDisplay(value).toString()
}
