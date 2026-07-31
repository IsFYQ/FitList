package com.example.healthcheckin.data.repository

import androidx.room.withTransaction
import com.example.healthcheckin.data.local.HealthDatabase
import com.example.healthcheckin.data.local.dao.SyncQueueDao
import com.example.healthcheckin.data.local.dao.WeightRecordDao
import com.example.healthcheckin.data.local.entity.SyncQueueEntity
import com.example.healthcheckin.data.local.entity.WeightRecordEntity
import com.example.healthcheckin.domain.algorithm.WeightProgressCalculator
import com.example.healthcheckin.domain.model.SaveWeightRequest
import com.example.healthcheckin.domain.model.WeightRecordItem
import com.example.healthcheckin.domain.repository.WeightRepository
import com.example.healthcheckin.util.DateTimeUtil
import com.example.healthcheckin.util.PrecisionUtil
import com.example.healthcheckin.util.SyncState
import com.example.healthcheckin.util.UuidV7
import com.example.healthcheckin.util.ValidationConstants
import com.example.healthcheckin.util.Validators
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WeightRepositoryImpl @Inject constructor(
    private val database: HealthDatabase,
    private val weightRecordDao: WeightRecordDao,
    private val syncQueueDao: SyncQueueDao,
    private val deviceId: String,
) : WeightRepository {

    override fun observeAllRecords(userId: String): Flow<List<WeightRecordItem>> =
        weightRecordDao.observeAll(userId).map { entities ->
            toItemsDescending(entities)
        }

    override fun observeRecordsInRange(
        userId: String,
        startDate: String,
        endDate: String,
    ): Flow<List<WeightRecordItem>> =
        weightRecordDao.observeByDateRange(userId, startDate, endDate).map { entities ->
            toItemsAscending(userId, entities)
        }

    override suspend fun getByDate(userId: String, localDate: String): WeightRecordItem? {
        val entity = weightRecordDao.getByDate(userId, localDate) ?: return null
        val previous = weightRecordDao.getPreviousBeforeDate(userId, localDate)
        return entity.toItem(previous?.weightKg)
    }

    override suspend fun saveWeight(
        userId: String,
        request: SaveWeightRequest,
        overwrite: Boolean,
    ): Result<WeightRecordItem> = runCatching {
        validateWeight(request.weightKg)
        validateDate(request.localDate)

        val now = DateTimeUtil.nowEpochMillis()
        val rounded = PrecisionUtil.roundWeightDisplay(request.weightKg)
        val note = request.note?.take(ValidationConstants.NOTE_MAX_LENGTH)

        database.withTransaction {
            val existing = weightRecordDao.getByDate(userId, request.localDate)
            if (existing != null && !overwrite) {
                throw WeightOverwriteRequiredException(existing.weightKg)
            }

            val record = if (existing != null) {
                val updated = existing.copy(
                    weightKg = rounded,
                    note = note,
                    updatedAt = now,
                    syncState = SyncState.PENDING,
                )
                weightRecordDao.update(updated)
                enqueueSync("weight_records", updated.id, now)
                updated
            } else {
                val id = UuidV7.generate()
                val entity = WeightRecordEntity(
                    id = id,
                    userId = userId,
                    localDate = request.localDate,
                    tzOffsetMinutes = DateTimeUtil.tzOffsetMinutes(),
                    weightKg = rounded,
                    note = note,
                    deviceId = deviceId,
                    createdAt = now,
                    updatedAt = now,
                    syncState = SyncState.PENDING,
                )
                weightRecordDao.insert(entity)
                enqueueSync("weight_records", id, now)
                entity
            }

            val previous = weightRecordDao.getPreviousBeforeDate(userId, record.localDate)
            record.toItem(previous?.weightKg)
        }
    }

    override suspend fun updateWeight(
        recordId: String,
        weightKg: Double,
        note: String?,
    ): Result<WeightRecordItem> = runCatching {
        validateWeight(weightKg)
        val existing = weightRecordDao.getById(recordId)
            ?: throw IllegalStateException("Record not found")
        val now = DateTimeUtil.nowEpochMillis()
        val rounded = PrecisionUtil.roundWeightDisplay(weightKg)
        val trimmedNote = note?.take(ValidationConstants.NOTE_MAX_LENGTH)

        database.withTransaction {
            val updated = existing.copy(
                weightKg = rounded,
                note = trimmedNote,
                updatedAt = now,
                syncState = SyncState.PENDING,
            )
            weightRecordDao.update(updated)
            enqueueSync("weight_records", updated.id, now)
            val previous = weightRecordDao.getPreviousBeforeDate(existing.userId, existing.localDate)
            updated.toItem(previous?.weightKg)
        }
    }

    override suspend fun deleteWeight(recordId: String): Result<Unit> = runCatching {
        val now = DateTimeUtil.nowEpochMillis()
        database.withTransaction {
            weightRecordDao.softDelete(recordId, now, now, SyncState.PENDING)
            enqueueSync("weight_records", recordId, now)
        }
    }

    private fun toItemsDescending(
        entities: List<WeightRecordEntity>,
    ): List<WeightRecordItem> {
        if (entities.isEmpty()) return emptyList()
        val sorted = entities.sortedByDescending { it.localDate }
        return sorted.mapIndexed { index, entity ->
            val previous = sorted.getOrNull(index + 1)
            entity.toItem(previous?.weightKg)
        }
    }

    private suspend fun toItemsAscending(
        userId: String,
        entities: List<WeightRecordEntity>,
    ): List<WeightRecordItem> =
        entities.map { entity ->
            val previous = weightRecordDao.getPreviousBeforeDate(userId, entity.localDate)
            entity.toItem(previous?.weightKg)
        }

    private fun validateWeight(weightKg: Double) {
        require(
            Validators.validateWeightKg(weightKg).isValid,
        ) { "Weight out of range" }
    }

    private fun validateDate(localDate: String) {
        require(!DateTimeUtil.isFutureDateString(localDate)) { "Cannot record future date" }
    }

    private suspend fun enqueueSync(tableName: String, rowId: String, now: Long) {
        syncQueueDao.insert(
            SyncQueueEntity(
                id = UuidV7.generate(),
                tableName = tableName,
                rowId = rowId,
                operation = "UPSERT",
                createdAt = now,
                updatedAt = now,
            )
        )
    }

    private fun WeightRecordEntity.toItem(previousKg: Double?) = WeightRecordItem(
        id = id,
        localDate = localDate,
        weightKg = weightKg,
        note = note,
        deltaKg = WeightProgressCalculator.deltaFromPrevious(weightKg, previousKg),
    )
}

class WeightOverwriteRequiredException(val existingWeightKg: Double) : Exception()
