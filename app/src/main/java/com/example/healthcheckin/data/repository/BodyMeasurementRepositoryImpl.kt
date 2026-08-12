package com.example.healthcheckin.data.repository

import androidx.room.withTransaction
import com.example.healthcheckin.data.local.HealthDatabase
import com.example.healthcheckin.data.local.dao.BodyMeasurementDao
import com.example.healthcheckin.data.local.dao.SyncQueueDao
import com.example.healthcheckin.data.local.entity.BodyMeasurementEntity
import com.example.healthcheckin.data.local.entity.SyncQueueEntity
import com.example.healthcheckin.domain.model.BodyChartRange
import com.example.healthcheckin.domain.model.BodyMeasurementItem
import com.example.healthcheckin.domain.model.BodyMetricSummary
import com.example.healthcheckin.domain.model.SaveBodyMeasurementRequest
import com.example.healthcheckin.domain.repository.BodyMeasurementRepository
import com.example.healthcheckin.util.BodyMetric
import com.example.healthcheckin.util.DateTimeUtil
import com.example.healthcheckin.util.P1ValidationConstants
import com.example.healthcheckin.util.PrecisionUtil
import com.example.healthcheckin.util.SyncState
import com.example.healthcheckin.util.UuidV7
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BodyMeasurementRepositoryImpl @Inject constructor(
    private val database: HealthDatabase,
    private val measurementDao: BodyMeasurementDao,
    private val syncQueueDao: SyncQueueDao,
    private val deviceId: String,
) : BodyMeasurementRepository {
    override fun observeSummaries(userId: String): Flow<List<BodyMetricSummary>> =
        measurementDao.observeAll(userId).map { records ->
            BodyMetric.entries.map { metric ->
                val items = records.filter { it.metric == metric.name }.mapIndexed { index, entity ->
                    entity.toItem(records.filter { r -> r.metric == metric.name }.getOrNull(index + 1)?.valueCm)
                }
                BodyMetricSummary(metric, items.firstOrNull(), items.take(7).map { it.valueCm }.reversed())
            }
        }

    override fun observeMetricHistory(userId: String, metric: BodyMetric, range: BodyChartRange): Flow<List<BodyMeasurementItem>> {
        val flow = range.days?.let { days ->
            val start = LocalDate.now().minusDays((days - 1).toLong()).format(DateTimeFormatter.ISO_LOCAL_DATE)
            measurementDao.observeByMetricRange(userId, metric.name, start, DateTimeUtil.todayLocalDateString())
        } ?: measurementDao.observeByMetric(userId, metric.name)
        return flow.map { entities ->
            entities.sortedBy { it.localDate }.mapIndexed { index, entity ->
                entity.toItem(entities.sortedBy { it.localDate }.getOrNull(index - 1)?.valueCm)
            }
        }
    }

    override suspend fun save(userId: String, request: SaveBodyMeasurementRequest, overwrite: Boolean): Result<BodyMeasurementItem> = runCatching {
        require(request.valueCm in P1ValidationConstants.BODY_METRIC_MIN_CM..P1ValidationConstants.BODY_METRIC_MAX_CM)
        require(!DateTimeUtil.isFutureDateString(request.localDate))
        val now = DateTimeUtil.nowEpochMillis()
        database.withTransaction {
            val existing = measurementDao.getByMetricDate(userId, request.metric.name, request.localDate)
            if (existing != null && !overwrite) throw BodyMeasurementOverwriteRequiredException(existing.valueCm)
            val entity = existing?.copy(
                valueCm = PrecisionUtil.roundWeightDisplay(request.valueCm),
                updatedAt = now,
                syncState = SyncState.PENDING,
            ) ?: BodyMeasurementEntity(
                id = UuidV7.generate(), userId = userId, metric = request.metric.name,
                localDate = request.localDate, tzOffsetMinutes = DateTimeUtil.tzOffsetMinutes(),
                valueCm = PrecisionUtil.roundWeightDisplay(request.valueCm), deviceId = deviceId,
                createdAt = now, updatedAt = now, syncState = SyncState.PENDING,
            )
            if (existing == null) measurementDao.insert(entity) else measurementDao.update(entity)
            enqueue(entity.id, now)
            entity.toItem(measurementDao.getPreviousBeforeDate(userId, entity.metric, entity.localDate)?.valueCm)
        }
    }

    override suspend fun delete(recordId: String): Result<Unit> = runCatching {
        val now = DateTimeUtil.nowEpochMillis()
        database.withTransaction {
            measurementDao.softDelete(recordId, now, now, SyncState.PENDING)
            enqueue(recordId, now)
        }
    }

    override suspend fun getByMetricDate(userId: String, metric: BodyMetric, localDate: String): BodyMeasurementItem? =
        measurementDao.getByMetricDate(userId, metric.name, localDate)?.let {
            it.toItem(measurementDao.getPreviousBeforeDate(userId, metric.name, localDate)?.valueCm)
        }

    private suspend fun enqueue(id: String, now: Long) = syncQueueDao.insert(
        SyncQueueEntity(id = UuidV7.generate(), tableName = "body_measurements", rowId = id, operation = "UPSERT", createdAt = now, updatedAt = now),
    )

    private fun BodyMeasurementEntity.toItem(previous: Double?) = BodyMeasurementItem(
        id, BodyMetric.valueOf(metric), localDate, valueCm, previous?.let { valueCm - it },
    )
}

class BodyMeasurementOverwriteRequiredException(val existingValueCm: Double) : Exception()
