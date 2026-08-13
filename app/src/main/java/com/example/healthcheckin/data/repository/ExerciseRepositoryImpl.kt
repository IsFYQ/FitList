package com.example.healthcheckin.data.repository

import androidx.room.withTransaction
import com.example.healthcheckin.data.local.HealthDatabase
import com.example.healthcheckin.data.local.dao.AppSettingDao
import com.example.healthcheckin.data.local.dao.ExerciseRecordDao
import com.example.healthcheckin.data.local.dao.ProfileDao
import com.example.healthcheckin.data.local.dao.SyncQueueDao
import com.example.healthcheckin.data.local.dao.WeightRecordDao
import com.example.healthcheckin.data.local.entity.ExerciseRecordEntity
import com.example.healthcheckin.data.local.entity.SyncQueueEntity
import com.example.healthcheckin.domain.algorithm.ExerciseMetCalculator
import com.example.healthcheckin.domain.algorithm.ExerciseStreakCalculator
import com.example.healthcheckin.domain.model.ExerciseRecordItem
import com.example.healthcheckin.domain.model.ExerciseWeekSummary
import com.example.healthcheckin.domain.model.SaveExerciseRequest
import com.example.healthcheckin.domain.repository.ExerciseRepository
import com.example.healthcheckin.util.DateTimeUtil
import com.example.healthcheckin.util.ExerciseType
import com.example.healthcheckin.util.P2ValidationConstants
import com.example.healthcheckin.util.SyncState
import com.example.healthcheckin.util.UuidV7
import com.example.healthcheckin.util.ValidationConstants
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExerciseRepositoryImpl @Inject constructor(
    private val database: HealthDatabase,
    private val exerciseRecordDao: ExerciseRecordDao,
    private val weightRecordDao: WeightRecordDao,
    private val profileDao: ProfileDao,
    private val appSettingDao: AppSettingDao,
    private val syncQueueDao: SyncQueueDao,
    private val deviceId: String,
) : ExerciseRepository {

    override fun observeRecords(userId: String): Flow<List<ExerciseRecordItem>> =
        exerciseRecordDao.observeAll(userId).map { list -> list.map { it.toItem() } }

    override suspend fun save(userId: String, request: SaveExerciseRequest): Result<ExerciseRecordItem> = runCatching {
        require(request.durationMinutes in P2ValidationConstants.EXERCISE_DURATION_MIN..P2ValidationConstants.EXERCISE_DURATION_MAX)
        if (request.exerciseType == ExerciseType.CUSTOM) {
            require(!request.customName.isNullOrBlank())
        }
        val met = ExerciseMetCalculator.metFor(request.exerciseType, request.customMet)
        require(met in P2ValidationConstants.EXERCISE_MET_MIN..P2ValidationConstants.EXERCISE_MET_MAX)
        val weight = weightRecordDao.getByDate(userId, request.localDate)?.weightKg
            ?: profileDao.getById(userId)?.initialWeightKg
            ?: error("No weight baseline")
        val estimated = ExerciseMetCalculator.estimatedKcal(met, weight, request.durationMinutes)
        val now = DateTimeUtil.nowEpochMillis()
        val entity = ExerciseRecordEntity(
            id = UuidV7.generate(),
            userId = userId,
            localDate = request.localDate,
            tzOffsetMinutes = DateTimeUtil.tzOffsetMinutes(),
            exerciseType = request.exerciseType.name,
            customName = request.customName?.trim(),
            metValue = met,
            durationMinutes = request.durationMinutes,
            estimatedKcal = estimated,
            deviceId = deviceId,
            createdAt = now,
            updatedAt = now,
            syncState = SyncState.PENDING,
        )
        database.withTransaction {
            exerciseRecordDao.insert(entity)
            enqueue(entity.id, now)
            updateStreak(userId, request.localDate, allowToast = request.localDate == DateTimeUtil.todayLocalDateString())
        }
        entity.toItem()
    }

    override suspend fun delete(recordId: String): Result<Unit> = runCatching {
        val record = exerciseRecordDao.getById(recordId) ?: error("Record not found")
        val now = DateTimeUtil.nowEpochMillis()
        database.withTransaction {
            exerciseRecordDao.softDelete(recordId, now, now, SyncState.PENDING)
            enqueue(recordId, now)
            updateStreak(record.userId, DateTimeUtil.todayLocalDateString(), allowToast = false)
        }
    }

    override suspend fun getWeekSummary(userId: String): ExerciseWeekSummary {
        val today = LocalDate.parse(DateTimeUtil.todayLocalDateString())
        val weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val records = exerciseRecordDao.getAll(userId)
        val weekRecords = records.filter {
            val date = LocalDate.parse(it.localDate)
            !date.isBefore(weekStart) && !date.isAfter(today)
        }
        val minutesByDate = ExerciseStreakCalculator.aggregateMinutesByDate(
            records.map { it.localDate to it.durationMinutes },
        )
        val previousBest = appSettingDao.get(BEST_STREAK_KEY)?.valueJson?.toIntOrNull() ?: 0
        val streak = ExerciseStreakCalculator.computeStreak(minutesByDate, today, previousBest, allowMilestoneToast = false)
        val weekDates = (0..6).map { weekStart.plusDays(it.toLong()).toString() }
        val activeDates = weekRecords
            .groupBy { it.localDate }
            .filterValues { list -> list.sumOf { r -> r.durationMinutes } >= 10 }
            .keys
        return ExerciseWeekSummary(
            totalMinutes = weekRecords.sumOf { it.durationMinutes },
            sessionCount = weekRecords.size,
            currentStreak = streak.currentStreak,
            bestStreak = streak.bestStreak,
            weekDates = weekDates,
            activeDates = activeDates,
        )
    }

    private suspend fun updateStreak(userId: String, anchorDate: String, allowToast: Boolean) {
        val records = exerciseRecordDao.getAll(userId)
        val minutesByDate = ExerciseStreakCalculator.aggregateMinutesByDate(
            records.map { it.localDate to it.durationMinutes },
        )
        val previousBest = appSettingDao.get(BEST_STREAK_KEY)?.valueJson?.toIntOrNull() ?: 0
        val streak = ExerciseStreakCalculator.computeStreak(
            minutesByDate,
            LocalDate.parse(anchorDate),
            previousBest,
            allowMilestoneToast = allowToast,
        )
        if (streak.bestStreak > previousBest) {
            appSettingDao.upsert(
                com.example.healthcheckin.data.local.entity.AppSettingEntity(
                    key = BEST_STREAK_KEY,
                    valueJson = streak.bestStreak.toString(),
                    updatedAt = DateTimeUtil.nowEpochMillis(),
                ),
            )
        }
    }

    private suspend fun enqueue(rowId: String, now: Long) {
        syncQueueDao.insert(
            SyncQueueEntity(
                id = UuidV7.generate(),
                tableName = "exercise_records",
                rowId = rowId,
                operation = "UPSERT",
                createdAt = now,
                updatedAt = now,
            ),
        )
    }

    private fun ExerciseRecordEntity.toItem() = ExerciseRecordItem(
        id = id,
        localDate = localDate,
        exerciseType = ExerciseType.valueOf(exerciseType),
        customName = customName,
        metValue = metValue,
        durationMinutes = durationMinutes,
        estimatedKcal = estimatedKcal,
        createdAt = createdAt,
    )

    companion object {
        const val BEST_STREAK_KEY = "best_exercise_streak"
    }
}
