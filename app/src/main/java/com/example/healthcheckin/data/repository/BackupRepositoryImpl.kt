package com.example.healthcheckin.data.repository

import androidx.room.withTransaction
import com.example.healthcheckin.data.auth.SessionManager
import com.example.healthcheckin.data.backup.BackupRetryPolicy
import com.example.healthcheckin.data.backup.BackupRowLoader
import com.example.healthcheckin.data.backup.BackupSettings
import com.example.healthcheckin.data.backup.BackupTable
import com.example.healthcheckin.data.backup.SupabaseRowMapper
import com.example.healthcheckin.data.local.HealthDatabase
import com.example.healthcheckin.data.local.dao.AnalyticsEventDao
import com.example.healthcheckin.data.local.dao.AppSettingDao
import com.example.healthcheckin.data.local.dao.FoodSearchCacheDao
import com.example.healthcheckin.data.local.dao.SyncQueueDao
import com.example.healthcheckin.data.local.entity.AppSettingEntity
import com.example.healthcheckin.data.remote.SupabaseSyncApi
import com.example.healthcheckin.domain.analytics.AnalyticsEvents
import com.example.healthcheckin.domain.analytics.AnalyticsTracker
import com.example.healthcheckin.domain.model.BackupPendingByTable
import com.example.healthcheckin.domain.model.BackupResult
import com.example.healthcheckin.domain.model.BackupState
import com.example.healthcheckin.domain.model.RestoreResult
import com.example.healthcheckin.domain.repository.BackupRepository
import com.example.healthcheckin.util.DateTimeUtil
import com.example.healthcheckin.util.NetworkMonitor
import com.example.healthcheckin.util.SyncState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.serialization.json.Json
import retrofit2.HttpException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BackupRepositoryImpl @Inject constructor(
    private val database: HealthDatabase,
    private val syncQueueDao: SyncQueueDao,
    private val appSettingDao: AppSettingDao,
    private val foodSearchCacheDao: FoodSearchCacheDao,
    private val supabaseSyncApi: SupabaseSyncApi,
    private val backupRowLoader: BackupRowLoader,
    private val sessionManager: SessionManager,
    private val networkMonitor: NetworkMonitor,
    private val backupScheduler: BackupScheduler,
    private val analyticsEventDao: AnalyticsEventDao,
    private val analyticsTracker: AnalyticsTracker,
    private val json: Json,
) : BackupRepository {

    private val runningState = MutableStateFlow(false)
    private val progressState = MutableStateFlow(0 to 0)
    private val lastBackupAtState = MutableStateFlow<Long?>(null)

    override fun observeBackupState(): Flow<BackupState> =
        combine(
            syncQueueDao.observePendingCount(),
            runningState,
            progressState,
            lastBackupAtState,
        ) { pending, running, (done, total), lastBackupAt ->
            BackupState(
                pendingCount = pending,
                lastBackupAt = lastBackupAt,
                isRunning = running,
                progressDone = done,
                progressTotal = total,
            )
        }

    override fun scheduleBackup() {
        backupScheduler.schedule()
    }

    override suspend fun triggerBackup(force: Boolean): BackupResult {
        refreshLastBackupAt()
        if (!networkMonitor.isOnline()) {
            return BackupResult(success = true, skipped = true)
        }
        sessionManager.getUserId() ?: return BackupResult(success = false, skipped = true)

        runningState.value = true
        val now = DateTimeUtil.nowEpochMillis()
        val backupStartedAt = now
        var synced = 0
        var failed = 0
        var total = 0
        var tablesSynced = 0

        try {
            for (table in BackupTable.uploadOrder) {
                if (table == BackupTable.ANALYTICS_EVENTS) continue
                val pending = syncQueueDao.getPendingForTable(table.tableName, now, limit = 200)
                if (pending.isEmpty()) continue

                total += pending.size
                progressState.value = synced to total

                val rowIds = pending.map { it.rowId }
                backupRowLoader.markSyncing(table, rowIds)
                val rows = backupRowLoader.loadRows(table, rowIds)
                if (rows.isEmpty()) {
                    pending.forEach { syncQueueDao.delete(it.id) }
                    continue
                }

                try {
                    val body = SupabaseRowMapper.encode(table, rows)
                    val response = supabaseSyncApi.upsertRows(table.tableName, body = body)
                    if (!response.isSuccessful) {
                        throw HttpException(response)
                    }
                    backupRowLoader.markSynced(table, rowIds)
                    pending.forEach { syncQueueDao.delete(it.id) }
                    synced += rows.size
                    tablesSynced++
                } catch (e: Exception) {
                    val httpCode = (e as? HttpException)?.code()
                    val errorCode = BackupRetryPolicy.errorCode(httpCode)
                    analyticsTracker.track(
                        AnalyticsEvents.SYNC_FAILED,
                        mapOf(
                            "table" to table.tableName,
                            "error_code" to errorCode,
                            "retry_count" to (pending.firstOrNull()?.retryCount?.plus(1) ?: 1),
                        ),
                    )
                    pending.forEach { item ->
                        val retryCount = item.retryCount + 1
                        syncQueueDao.updateRetry(
                            id = item.id,
                            retryCount = retryCount,
                            nextRetryAt = BackupRetryPolicy.nextRetryAt(retryCount, now),
                            errorCode = errorCode,
                            updatedAt = now,
                        )
                    }
                    backupRowLoader.markFailed(table, rowIds)
                    failed += pending.size
                }
                progressState.value = synced to total
            }

            val analyticsUploaded = uploadAnalyticsEvents(now)

            if (synced > 0 || analyticsUploaded > 0) {
                if (failed == 0) {
                    saveLastBackupAt(now)
                }
            }
            val elapsed = System.currentTimeMillis() - backupStartedAt
            if (synced > 0 || analyticsUploaded > 0 || failed > 0) {
                analyticsTracker.track(
                    AnalyticsEvents.SYNC_BATCH_COMPLETED,
                    mapOf(
                        "table_count" to tablesSynced,
                        "row_count" to (synced + analyticsUploaded),
                        "elapsed_ms" to elapsed.toInt(),
                        "failed_count" to failed,
                    ),
                )
            }
            return BackupResult(success = failed == 0, syncedCount = synced, failedCount = failed)
        } finally {
            runningState.value = false
            progressState.value = 0 to 0
        }
    }

    override suspend fun restoreFromCloud(): RestoreResult {
        if (!networkMonitor.isOnline()) {
            return RestoreResult(success = false, errorMessage = "offline")
        }
        val userId = sessionManager.getUserId() ?: return RestoreResult(success = false, errorMessage = "no_session")

        return try {
            database.withTransaction {
                backupRowLoader.clearUserData(userId)
                for (table in BackupTable.uploadOrder) {
                    var offset = 0
                    while (true) {
                        val payload = supabaseSyncApi.fetchRows(
                            table = table.tableName,
                            userFilter = "eq.$userId",
                            offset = offset,
                        )
                        val rows = SupabaseRowMapper.decode(table, payload, json)
                        if (rows.isEmpty()) break
                        backupRowLoader.insertRestored(table, rows)
                        if (rows.size < 1000) break
                        offset += 1000
                    }
                }
                syncQueueDao.deleteAll()
                foodSearchCacheDao.deleteAll()
                saveLastRestoreAt(DateTimeUtil.nowEpochMillis())
            }
            RestoreResult(success = true)
        } catch (e: Exception) {
            RestoreResult(success = false, errorMessage = e.message ?: "restore_failed")
        }
    }

    override suspend fun getPendingByTable(): List<BackupPendingByTable> {
        val counts = syncQueueDao.countPendingByTable()
        return BackupTable.uploadOrder.mapNotNull { table ->
            if (table == BackupTable.ANALYTICS_EVENTS) return@mapNotNull null
            val count = counts.find { it.tableName == table.tableName }?.cnt ?: 0
            if (count == 0) null else BackupPendingByTable(table.tableName, table.label, count)
        }
    }

    override suspend fun resetFailedRetries() {
        syncQueueDao.resetAllRetries(DateTimeUtil.nowEpochMillis())
        triggerBackup(force = true)
    }

    private suspend fun refreshLastBackupAt() {
        lastBackupAtState.value = appSettingDao.get(BackupSettings.LAST_BACKUP_AT)
            ?.valueJson
            ?.trim('"')
            ?.toLongOrNull()
    }

    private suspend fun saveLastBackupAt(epochMillis: Long) {
        appSettingDao.upsert(
            AppSettingEntity(
                key = BackupSettings.LAST_BACKUP_AT,
                valueJson = epochMillis.toString(),
                updatedAt = epochMillis,
            )
        )
        lastBackupAtState.value = epochMillis
    }

    private suspend fun saveLastRestoreAt(epochMillis: Long) {
        appSettingDao.upsert(
            AppSettingEntity(
                key = BackupSettings.LAST_RESTORE_AT,
                valueJson = epochMillis.toString(),
                updatedAt = epochMillis,
            )
        )
    }

    private suspend fun uploadAnalyticsEvents(now: Long): Int {
        var uploaded = 0
        while (true) {
            val batch = analyticsEventDao.getUnsynced(limit = 200)
            if (batch.isEmpty()) break
            try {
                val body = SupabaseRowMapper.encode(BackupTable.ANALYTICS_EVENTS, batch)
                val response = supabaseSyncApi.upsertRows(
                    BackupTable.ANALYTICS_EVENTS.tableName,
                    body = body,
                )
                if (!response.isSuccessful) break
                analyticsEventDao.markSyncState(
                    ids = batch.map { it.id },
                    syncState = SyncState.SYNCED,
                    updatedAt = now,
                )
                uploaded += batch.size
            } catch (_: Exception) {
                break
            }
        }
        return uploaded
    }
}
