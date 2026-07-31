package com.example.healthcheckin.domain.repository

import com.example.healthcheckin.domain.model.ExportFormat
import com.example.healthcheckin.domain.model.ExportProgress
import com.example.healthcheckin.domain.model.ExportResult
import kotlinx.coroutines.flow.Flow

interface ExportRepository {
    fun observeProgress(): Flow<ExportProgress>

    suspend fun cleanupTempFiles()

    suspend fun export(format: ExportFormat): ExportResult

    fun cancelExport()
}
