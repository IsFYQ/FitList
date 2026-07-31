package com.example.healthcheckin.domain.repository

import com.example.healthcheckin.domain.model.BackupPendingByTable
import com.example.healthcheckin.domain.model.BackupResult
import com.example.healthcheckin.domain.model.BackupState
import com.example.healthcheckin.domain.model.RestoreResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

interface BackupRepository {
    fun observeBackupState(): Flow<BackupState>
    suspend fun triggerBackup(force: Boolean = true): BackupResult
    suspend fun restoreFromCloud(): RestoreResult
    suspend fun getPendingByTable(): List<BackupPendingByTable>
    suspend fun resetFailedRetries()
    fun scheduleBackup()
}
