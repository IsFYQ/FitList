package com.example.healthcheckin.data.analytics

import android.os.Build
import android.util.Log
import com.example.healthcheckin.BuildConfig
import com.example.healthcheckin.data.auth.SessionManager
import com.example.healthcheckin.data.local.dao.AnalyticsEventDao
import com.example.healthcheckin.data.local.dao.AppSettingDao
import com.example.healthcheckin.data.local.entity.AnalyticsEventEntity
import com.example.healthcheckin.data.local.entity.AppSettingEntity
import com.example.healthcheckin.domain.analytics.AnalyticsEvents
import com.example.healthcheckin.domain.analytics.AnalyticsSettingsKeys
import com.example.healthcheckin.domain.analytics.AnalyticsTracker
import com.example.healthcheckin.util.DateTimeUtil
import com.example.healthcheckin.util.NetworkMonitor
import com.example.healthcheckin.util.SyncState
import com.example.healthcheckin.util.UuidV7
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.ArrayDeque
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AnalyticsManager @Inject constructor(
    private val analyticsEventDao: AnalyticsEventDao,
    private val appSettingDao: AppSettingDao,
    private val sessionManager: SessionManager,
    private val sessionTracker: AnalyticsSessionTracker,
    private val networkMonitor: NetworkMonitor,
    private val deviceId: String,
) : AnalyticsTracker {

    private val scope = CoroutineScope(SupervisorJob() + kotlinx.coroutines.Dispatchers.IO)
    private val queue = ArrayDeque<QueuedEvent>(QUEUE_CAPACITY)
    private val queueMutex = Mutex()
    private val enabledState = MutableStateFlow(true)
    private var flushJobStarted = false

    init {
        scope.launch {
            val enabled = readEnabledSetting()
            enabledState.value = enabled
        }
    }

    override fun track(eventName: String, params: Map<String, Any?>) {
        try {
            if (!enabledState.value && eventName != AnalyticsEvents.ANALYTICS_TOGGLE_CHANGED) return
            scope.launch {
                enqueue(eventName, params)
            }
        } catch (_: Exception) {
            // E13-06: never propagate
        }
    }

    override fun isEnabled(): Flow<Boolean> = enabledState

    override suspend fun setEnabled(enabled: Boolean) {
        val now = DateTimeUtil.nowEpochMillis()
        appSettingDao.upsert(
            AppSettingEntity(
                key = AnalyticsSettingsKeys.ANALYTICS_ENABLED,
                valueJson = enabled.toString(),
                updatedAt = now,
            ),
        )
        enabledState.value = enabled
        track(AnalyticsEvents.ANALYTICS_TOGGLE_CHANGED, mapOf("enabled" to enabled))
    }

    override suspend fun onUserLoggedIn(userId: String) {
        val now = DateTimeUtil.nowEpochMillis()
        analyticsEventDao.backfillUserId(
            sessionId = sessionTracker.currentSessionId(),
            userId = userId,
            updatedAt = now,
        )
    }

    override suspend fun runMaintenance() {
        val now = DateTimeUtil.nowEpochMillis()
        val cutoff = now - RETENTION_MS
        analyticsEventDao.deleteSyncedOlderThan(cutoff)

        val total = analyticsEventDao.countAll()
        if (total > MAX_ROWS) {
            analyticsEventDao.deleteOldestSynced(total - MAX_ROWS)
        }
    }

    suspend fun flushNow() {
        flushBatch(forceAll = true)
    }

    private suspend fun enqueue(eventName: String, params: Map<String, Any?>) {
        ensureFlushLoop()
        val event = QueuedEvent(eventName, params)
        queueMutex.withLock {
            if (queue.size >= QUEUE_CAPACITY) {
                queue.removeFirst()
            }
            queue.addLast(event)
            if (queue.size >= BATCH_SIZE) {
                scope.launch { flushBatch(forceAll = false) }
            }
        }
    }

    private fun ensureFlushLoop() {
        if (flushJobStarted) return
        flushJobStarted = true
        scope.launch {
            while (true) {
                delay(FLUSH_INTERVAL_MS)
                flushBatch(forceAll = false)
            }
        }
    }

    private suspend fun flushBatch(forceAll: Boolean) {
        if (!enabledState.value && !forceAll) return
        val batch = queueMutex.withLock {
            if (queue.isEmpty()) return
            val count = if (forceAll) queue.size else minOf(queue.size, BATCH_SIZE)
            buildList {
                repeat(count) {
                    if (queue.isNotEmpty()) add(queue.removeFirst())
                }
            }
        }
        if (batch.isEmpty()) return

        try {
            val now = DateTimeUtil.nowEpochMillis()
            val entities = batch.map { it.toEntity(now) }
            analyticsEventDao.insertAll(entities)
        } catch (e: Exception) {
            Log.w(TAG, "Analytics flush failed", e)
        }
    }

    private suspend fun readEnabledSetting(): Boolean {
        val raw = appSettingDao.get(AnalyticsSettingsKeys.ANALYTICS_ENABLED)?.valueJson
        return when {
            raw == null -> true
            raw.equals("true", ignoreCase = true) -> true
            raw.equals("false", ignoreCase = true) -> false
            else -> true
        }
    }

    private fun QueuedEvent.toEntity(now: Long): AnalyticsEventEntity {
        val eventAt = now
        return AnalyticsEventEntity(
            id = UuidV7.generate(),
            userId = sessionManager.getUserId(),
            eventName = eventName,
            eventAt = eventAt,
            localDate = DateTimeUtil.toLocalDateString(eventAt),
            tzOffsetMinutes = DateTimeUtil.tzOffsetMinutes(),
            sessionId = sessionTracker.currentSessionId(),
            appVersion = BuildConfig.VERSION_NAME,
            osVersion = Build.VERSION.RELEASE,
            deviceModel = Build.MODEL,
            paramsJson = AnalyticsParamBuilder.toJson(params),
            deviceId = deviceId,
            createdAt = now,
            updatedAt = now,
            syncState = SyncState.PENDING,
        )
    }

    private data class QueuedEvent(
        val eventName: String,
        val params: Map<String, Any?>,
    )

    companion object {
        private const val TAG = "AnalyticsManager"
        private const val QUEUE_CAPACITY = 200
        private const val BATCH_SIZE = 20
        private const val FLUSH_INTERVAL_MS = 10_000L
        private const val RETENTION_MS = 90L * 24 * 60 * 60 * 1000
        private const val MAX_ROWS = 20_000
    }
}
