package com.example.healthcheckin.ui.screens.settings

import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.healthcheckin.BuildConfig
import com.example.healthcheckin.data.local.dao.SyncQueueDao
import com.example.healthcheckin.data.local.entity.SyncQueueEntity
import com.example.healthcheckin.domain.model.BackupPendingByTable
import com.example.healthcheckin.domain.repository.BackupRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SyncErrorItem(
    val tableName: String,
    val errorCode: String?,
    val updatedAt: Long,
    val retryCount: Int,
)

data class DiagnosticsUiState(
    val pendingByTable: List<BackupPendingByTable> = emptyList(),
    val recentErrors: List<SyncErrorItem> = emptyList(),
    val isRetrying: Boolean = false,
)

@HiltViewModel
class DiagnosticsViewModel @Inject constructor(
    private val backupRepository: BackupRepository,
    private val syncQueueDao: SyncQueueDao,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DiagnosticsUiState())
    val uiState: StateFlow<DiagnosticsUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            val pending = backupRepository.getPendingByTable()
            val errors = syncQueueDao.getRecentErrors(limit = 20).map { it.toSyncErrorItem() }
            _uiState.update {
                it.copy(pendingByTable = pending, recentErrors = errors)
            }
        }
    }

    fun retryAll() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRetrying = true) }
            backupRepository.resetFailedRetries()
            refresh()
            _uiState.update { it.copy(isRetrying = false) }
        }
    }

    fun buildDiagnosticText(): String = buildString {
        appendLine("app_version: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
        appendLine("os_version: Android ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})")
        appendLine("device_model: ${Build.MODEL}")
        appendLine()
        appendLine("pending_backup:")
        _uiState.value.pendingByTable.forEach { item ->
            appendLine("- ${item.tableName}: ${item.count}")
        }
        if (_uiState.value.pendingByTable.isEmpty()) {
            appendLine("- none")
        }
        appendLine()
        appendLine("recent_errors:")
        _uiState.value.recentErrors.forEach { error ->
            appendLine("- ${error.tableName} ${error.errorCode ?: "?"} retry=${error.retryCount}")
        }
        if (_uiState.value.recentErrors.isEmpty()) {
            appendLine("- none")
        }
    }

    private fun SyncQueueEntity.toSyncErrorItem() = SyncErrorItem(
        tableName = tableName,
        errorCode = lastErrorCode,
        updatedAt = updatedAt,
        retryCount = retryCount,
    )
}
