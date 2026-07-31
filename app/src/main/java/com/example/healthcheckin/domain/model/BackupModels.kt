package com.example.healthcheckin.domain.model

data class BackupState(
    val pendingCount: Int = 0,
    val lastBackupAt: Long? = null,
    val isRunning: Boolean = false,
    val progressDone: Int = 0,
    val progressTotal: Int = 0,
)

data class BackupResult(
    val success: Boolean,
    val skipped: Boolean = false,
    val syncedCount: Int = 0,
    val failedCount: Int = 0,
)

data class RestoreResult(
    val success: Boolean,
    val errorMessage: String? = null,
)

data class BackupPendingByTable(
    val tableName: String,
    val label: String,
    val count: Int,
)
