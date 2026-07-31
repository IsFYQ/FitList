package com.example.healthcheckin.ui.screens.export

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.healthcheckin.domain.analytics.AnalyticsEvents
import com.example.healthcheckin.domain.analytics.AnalyticsTracker
import com.example.healthcheckin.domain.model.ExportFormat
import com.example.healthcheckin.domain.model.ExportProgress
import com.example.healthcheckin.domain.repository.ExportRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

data class ExportUiState(
    val format: ExportFormat = ExportFormat.BOTH,
    val isExporting: Boolean = false,
    val progress: ExportProgress = ExportProgress(),
    val exportedFile: File? = null,
    val savedPath: String? = null,
    val isEmptyData: Boolean = false,
    val errorMessage: String? = null,
    val toastMessage: String? = null,
)

@HiltViewModel
class ExportViewModel @Inject constructor(
    private val exportRepository: ExportRepository,
    private val analyticsTracker: AnalyticsTracker,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExportUiState())
    val uiState = _uiState.asStateFlow()

    private val _shareEvents = MutableSharedFlow<File>()
    val shareEvents: SharedFlow<File> = _shareEvents.asSharedFlow()

    init {
        viewModelScope.launch {
            exportRepository.cleanupTempFiles()
        }
        viewModelScope.launch {
            exportRepository.observeProgress().collect { progress ->
                if (progress.table != null || progress.message.isNotEmpty()) {
                    _uiState.update { it.copy(progress = progress) }
                }
            }
        }
    }

    fun setFormat(format: ExportFormat) {
        if (_uiState.value.isExporting) return
        _uiState.update { it.copy(format = format) }
    }

    fun startExport() {
        if (_uiState.value.isExporting) return
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isExporting = true,
                    exportedFile = null,
                    savedPath = null,
                    errorMessage = null,
                    isEmptyData = false,
                    progress = ExportProgress(),
                )
            }
            analyticsTracker.track(
                AnalyticsEvents.DATA_EXPORT_STARTED,
                mapOf("format" to _uiState.value.format.name),
            )
            val result = exportRepository.export(_uiState.value.format)
            when {
                result.cancelled -> {
                    _uiState.update {
                        it.copy(
                            isExporting = false,
                            toastMessage = "cancelled",
                        )
                    }
                }
                result.success && result.file != null -> {
                    analyticsTracker.track(
                        AnalyticsEvents.DATA_EXPORT_COMPLETED,
                        mapOf(
                            "format" to _uiState.value.format.name,
                            "total_rows" to result.totalRows,
                            "file_size_kb" to result.fileSizeKb,
                            "elapsed_ms" to result.elapsedMs.toInt(),
                        ),
                    )
                    _uiState.update {
                        it.copy(
                            isExporting = false,
                            exportedFile = result.file,
                            savedPath = result.file.absolutePath,
                            isEmptyData = result.isEmptyData,
                        )
                    }
                    _shareEvents.emit(result.file)
                }
                else -> {
                    analyticsTracker.track(
                        AnalyticsEvents.DATA_EXPORT_FAILED,
                        mapOf(
                            "format" to _uiState.value.format.name,
                            "error_code" to (result.errorCode ?: "E6014"),
                        ),
                    )
                    _uiState.update {
                        it.copy(
                            isExporting = false,
                            errorMessage = result.errorCode ?: "E6014",
                        )
                    }
                }
            }
        }
    }

    fun cancelExport() {
        exportRepository.cancelExport()
    }

    fun shareAgain() {
        val file = _uiState.value.exportedFile ?: return
        viewModelScope.launch {
            _shareEvents.emit(file)
        }
    }

    fun clearToast() {
        _uiState.update { it.copy(toastMessage = null) }
    }
}
