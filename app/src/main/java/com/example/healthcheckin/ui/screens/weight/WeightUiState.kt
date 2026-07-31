package com.example.healthcheckin.ui.screens.weight

import com.example.healthcheckin.domain.model.WeightChartRange
import com.example.healthcheckin.domain.model.WeightProgressInfo
import com.example.healthcheckin.domain.model.WeightRecordItem
import java.time.LocalDate

enum class WeightInputMode { CREATE, EDIT }

data class WeightInputUiState(
    val recordId: String? = null,
    val weightText: String = "",
    val localDate: LocalDate = LocalDate.now(),
    val note: String = "",
    val dateEditable: Boolean = true,
)

data class WeightOverwritePrompt(
    val localDate: String,
    val existingWeightKg: Double,
    val newWeightKg: Double,
)

data class WeightChartUiState(
    val latestRecord: WeightRecordItem? = null,
    val progress: WeightProgressInfo? = null,
    val selectedRange: WeightChartRange = WeightChartRange.DAYS_30,
    val chartRecords: List<WeightRecordItem> = emptyList(),
    val historyRecords: List<WeightRecordItem> = emptyList(),
    val targetWeightKg: Double? = null,
    val showInputSheet: Boolean = false,
    val inputMode: WeightInputMode = WeightInputMode.CREATE,
    val inputState: WeightInputUiState = WeightInputUiState(),
    val overwritePrompt: WeightOverwritePrompt? = null,
    val largeDiffPrompt: Double? = null,
    val pendingSaveAfterConfirm: Boolean = false,
    val deleteTarget: WeightRecordItem? = null,
    val minDate: String = "",
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
)
