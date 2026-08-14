package com.example.healthcheckin.ui.screens.body

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.healthcheckin.data.auth.SessionManager
import com.example.healthcheckin.data.local.dao.ProfileDao
import com.example.healthcheckin.data.repository.BodyMeasurementOverwriteRequiredException
import com.example.healthcheckin.domain.analytics.AnalyticsEvents
import com.example.healthcheckin.domain.analytics.AnalyticsTracker
import com.example.healthcheckin.domain.model.BodyChartRange
import com.example.healthcheckin.domain.model.BodyMeasurementItem
import com.example.healthcheckin.domain.model.BodyMetricSummary
import com.example.healthcheckin.domain.model.SaveBodyMeasurementRequest
import com.example.healthcheckin.domain.repository.BodyMeasurementRepository
import com.example.healthcheckin.util.BodyMetric
import com.example.healthcheckin.util.DateTimeUtil
import com.example.healthcheckin.util.P1ValidationConstants
import com.example.healthcheckin.util.Validators
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject
import kotlin.math.abs

data class BodyListUiState(
    val summaries: List<BodyMetricSummary> = emptyList(),
    val recordingMetric: BodyMetric? = null,
    val showSheet: Boolean = false,
    val valueText: String = "",
    val localDate: LocalDate = DateTimeUtil.todayLocalDate(),
    val minDate: String = DateTimeUtil.todayLocalDateString(),
    val isSaving: Boolean = false,
    val overwriteExisting: Double? = null,
    val largeDiff: Double? = null,
    val errorMessage: String? = null,
)

data class BodyDetailUiState(
    val metric: BodyMetric = BodyMetric.WAIST,
    val range: BodyChartRange = BodyChartRange.DAYS_30,
    val records: List<BodyMeasurementItem> = emptyList(),
    val showSheet: Boolean = false,
    val valueText: String = "",
    val localDate: LocalDate = DateTimeUtil.todayLocalDate(),
    val minDate: String = DateTimeUtil.todayLocalDateString(),
    val isSaving: Boolean = false,
    val overwriteExisting: Double? = null,
    val largeDiff: Double? = null,
    val errorMessage: String? = null,
    val deleteTarget: BodyMeasurementItem? = null,
)

@HiltViewModel
class BodyMeasurementsViewModel @Inject constructor(
    private val repository: BodyMeasurementRepository,
    private val sessionManager: SessionManager,
    private val profileDao: ProfileDao,
    private val analyticsTracker: AnalyticsTracker,
) : ViewModel() {
    private val _uiState = MutableStateFlow(BodyListUiState())
    val uiState: StateFlow<BodyListUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val userId = sessionManager.getUserId() ?: return@launch
            val profile = profileDao.getById(userId)
            val minDate = DateTimeUtil.backfillMinDateString(
                profile?.registeredLocalDate ?: DateTimeUtil.todayLocalDateString(),
            )
            _uiState.update { it.copy(minDate = minDate) }
            repository.observeSummaries(userId).collect { summaries ->
                _uiState.update { it.copy(summaries = summaries) }
            }
        }
    }

    fun openRecordSheet(metric: BodyMetric) {
        val latest = _uiState.value.summaries.find { it.metric == metric }?.latest
        _uiState.update {
            it.copy(
                recordingMetric = metric,
                showSheet = true,
                valueText = latest?.valueCm?.toString().orEmpty(),
                localDate = DateTimeUtil.todayLocalDate(),
                overwriteExisting = null,
                largeDiff = null,
            )
        }
    }

    fun dismissSheet() = _uiState.update {
        it.copy(showSheet = false, recordingMetric = null, overwriteExisting = null, largeDiff = null)
    }

    fun updateValue(text: String) = _uiState.update {
        it.copy(valueText = Validators.filterDecimalInput(text))
    }

    fun updateDate(date: LocalDate) = _uiState.update { it.copy(localDate = date) }

    fun save(overwrite: Boolean = false, skipLargeDiff: Boolean = false) {
        val state = _uiState.value
        val metric = state.recordingMetric ?: return
        if (state.isSaving) return
        val value = Validators.parseDecimalInput(state.valueText) ?: return
        if (value !in P1ValidationConstants.BODY_METRIC_MIN_CM..P1ValidationConstants.BODY_METRIC_MAX_CM) {
            _uiState.update { it.copy(errorMessage = "range") }
            return
        }
        if (!skipLargeDiff) {
            val previous = state.summaries.find { it.metric == metric }?.latest
            if (previous != null && abs(value - previous.valueCm) > P1ValidationConstants.BODY_METRIC_DIFF_CONFIRM_CM) {
                _uiState.update { it.copy(largeDiff = abs(value - previous.valueCm)) }
                return
            }
        }
        viewModelScope.launch {
            val userId = sessionManager.getUserId() ?: return@launch
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }
            repository.save(
                userId,
                SaveBodyMeasurementRequest(metric, value, DateTimeUtil.formatLocalDate(state.localDate)),
                overwrite,
            ).fold(
                onSuccess = {
                    analyticsTracker.track(
                        AnalyticsEvents.MEASUREMENT_RECORDED,
                        mapOf("metric" to metric.name),
                    )
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            showSheet = false,
                            recordingMetric = null,
                            overwriteExisting = null,
                            largeDiff = null,
                        )
                    }
                },
                onFailure = { err ->
                    if (err is BodyMeasurementOverwriteRequiredException) {
                        _uiState.update { it.copy(isSaving = false, overwriteExisting = err.existingValueCm) }
                    } else {
                        _uiState.update { it.copy(isSaving = false, errorMessage = "save_failed") }
                    }
                },
            )
        }
    }

    fun confirmOverwrite() = save(overwrite = true, skipLargeDiff = true)
    fun confirmLargeDiff() = save(overwrite = _uiState.value.overwriteExisting != null, skipLargeDiff = true)
    fun clearError() = _uiState.update { it.copy(errorMessage = null) }
}

@HiltViewModel
class BodyMetricDetailViewModel @Inject constructor(
    private val repository: BodyMeasurementRepository,
    private val sessionManager: SessionManager,
    private val profileDao: ProfileDao,
    private val analyticsTracker: AnalyticsTracker,
) : ViewModel() {
    private val _uiState = MutableStateFlow(BodyDetailUiState())
    val uiState: StateFlow<BodyDetailUiState> = _uiState.asStateFlow()
    private var allForMetric: List<BodyMeasurementItem> = emptyList()

    fun initMetric(metricName: String) {
        val metric = runCatching { BodyMetric.valueOf(metricName) }.getOrDefault(BodyMetric.WAIST)
        _uiState.update { it.copy(metric = metric) }
        viewModelScope.launch {
            val userId = sessionManager.getUserId() ?: return@launch
            val profile = profileDao.getById(userId)
            val minDate = DateTimeUtil.backfillMinDateString(
                profile?.registeredLocalDate ?: DateTimeUtil.todayLocalDateString(),
            )
            _uiState.update { it.copy(minDate = minDate) }
            repository.observeMetricHistory(userId, metric, BodyChartRange.ALL).collect { records ->
                allForMetric = records.sortedByDescending { it.localDate }
                analyticsTracker.track(AnalyticsEvents.MEASUREMENT_CHART_VIEWED, mapOf("metric" to metric.name))
                applyRange(_uiState.value.range)
            }
        }
    }

    fun selectRange(range: BodyChartRange) {
        _uiState.update { it.copy(range = range) }
        applyRange(range)
    }

    private fun applyRange(range: BodyChartRange) {
        val filtered = when (val days = range.days) {
            null -> allForMetric
            else -> {
                val start = DateTimeUtil.todayLocalDate().minusDays((days - 1).toLong())
                allForMetric.filter {
                    val date = DateTimeUtil.parseLocalDateOrNull(it.localDate) ?: return@filter false
                    date >= start
                }
            }
        }
        _uiState.update { it.copy(records = filtered) }
    }

    fun openSheet(prefill: BodyMeasurementItem? = null) {
        _uiState.update {
            it.copy(
                showSheet = true,
                valueText = prefill?.valueCm?.toString().orEmpty(),
                localDate = prefill?.localDate?.let(DateTimeUtil::parseLocalDateOrNull)
                    ?: DateTimeUtil.todayLocalDate(),
                overwriteExisting = null,
                largeDiff = null,
            )
        }
    }

    fun dismissSheet() = _uiState.update {
        it.copy(showSheet = false, overwriteExisting = null, largeDiff = null)
    }

    fun updateValue(text: String) = _uiState.update {
        it.copy(valueText = Validators.filterDecimalInput(text))
    }

    fun updateDate(date: LocalDate) = _uiState.update { it.copy(localDate = date) }

    fun save(overwrite: Boolean = false, skipLargeDiff: Boolean = false) {
        val state = _uiState.value
        if (state.isSaving) return
        val value = Validators.parseDecimalInput(state.valueText) ?: return
        if (value !in P1ValidationConstants.BODY_METRIC_MIN_CM..P1ValidationConstants.BODY_METRIC_MAX_CM) {
            _uiState.update { it.copy(errorMessage = "range") }
            return
        }
        if (!skipLargeDiff) {
            val previous = allForMetric.firstOrNull()
            if (previous != null && abs(value - previous.valueCm) > P1ValidationConstants.BODY_METRIC_DIFF_CONFIRM_CM) {
                _uiState.update { it.copy(largeDiff = abs(value - previous.valueCm)) }
                return
            }
        }
        viewModelScope.launch {
            val userId = sessionManager.getUserId() ?: return@launch
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }
            repository.save(
                userId,
                SaveBodyMeasurementRequest(state.metric, value, DateTimeUtil.formatLocalDate(state.localDate)),
                overwrite,
            ).fold(
                onSuccess = {
                    analyticsTracker.track(
                        AnalyticsEvents.MEASUREMENT_RECORDED,
                        mapOf("metric" to state.metric.name),
                    )
                    _uiState.update { it.copy(isSaving = false, showSheet = false, overwriteExisting = null, largeDiff = null) }
                },
                onFailure = { err ->
                    if (err is BodyMeasurementOverwriteRequiredException) {
                        _uiState.update { it.copy(isSaving = false, overwriteExisting = err.existingValueCm) }
                    } else {
                        _uiState.update { it.copy(isSaving = false, errorMessage = "save_failed") }
                    }
                },
            )
        }
    }

    fun confirmOverwrite() = save(overwrite = true, skipLargeDiff = true)
    fun confirmLargeDiff() = save(overwrite = _uiState.value.overwriteExisting != null, skipLargeDiff = true)
    fun clearError() = _uiState.update { it.copy(errorMessage = null) }
    fun requestDelete(item: BodyMeasurementItem) = _uiState.update { it.copy(deleteTarget = item) }
    fun dismissDelete() = _uiState.update { it.copy(deleteTarget = null) }
    fun confirmDelete() {
        val target = _uiState.value.deleteTarget ?: return
        viewModelScope.launch {
            repository.delete(target.id)
            analyticsTracker.track(AnalyticsEvents.MEASUREMENT_DELETED, mapOf("metric" to target.metric.name))
            _uiState.update { it.copy(deleteTarget = null) }
        }
    }
}
