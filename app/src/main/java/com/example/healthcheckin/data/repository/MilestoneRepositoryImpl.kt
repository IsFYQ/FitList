package com.example.healthcheckin.data.repository

import androidx.room.withTransaction
import com.example.healthcheckin.data.local.HealthDatabase
import com.example.healthcheckin.data.local.dao.GoalDao
import com.example.healthcheckin.data.local.dao.MilestoneDao
import com.example.healthcheckin.data.local.dao.ProfileDao
import com.example.healthcheckin.data.local.dao.SyncQueueDao
import com.example.healthcheckin.data.local.entity.MilestoneEntity
import com.example.healthcheckin.data.local.entity.SyncQueueEntity
import com.example.healthcheckin.domain.algorithm.MilestoneEvaluator
import com.example.healthcheckin.domain.model.MilestoneAchievementEvent
import com.example.healthcheckin.domain.model.MilestoneItem
import com.example.healthcheckin.domain.model.SaveMilestoneRequest
import com.example.healthcheckin.domain.repository.MilestoneRepository
import com.example.healthcheckin.util.DateTimeUtil
import com.example.healthcheckin.util.GoalType
import com.example.healthcheckin.util.P1ValidationConstants
import com.example.healthcheckin.util.PrecisionUtil
import com.example.healthcheckin.util.SyncState
import com.example.healthcheckin.util.UuidV7
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

@Singleton
class MilestoneRepositoryImpl @Inject constructor(
    private val database: HealthDatabase,
    private val milestoneDao: MilestoneDao,
    private val goalDao: GoalDao,
    private val profileDao: ProfileDao,
    private val syncQueueDao: SyncQueueDao,
    private val deviceId: String,
) : MilestoneRepository {
    override fun observeMilestones(userId: String): Flow<List<MilestoneItem>> =
        milestoneDao.observeAll(userId).map { milestones -> milestones.map { it.toItem() } }

    override suspend fun create(userId: String, request: SaveMilestoneRequest): Result<MilestoneItem> = runCatching {
        validate(request)
        require(milestoneDao.countActive(userId) < P1ValidationConstants.MILESTONE_ACTIVE_MAX) { "Maximum active milestones reached" }
        val now = DateTimeUtil.nowEpochMillis()
        val entity = MilestoneEntity(
            id = UuidV7.generate(), userId = userId, title = request.title.trim(),
            targetWeightKg = PrecisionUtil.roundWeightDisplay(request.targetWeightKg),
            rewardText = request.rewardText?.trim()?.take(P1ValidationConstants.MILESTONE_REWARD_MAX),
            deviceId = deviceId, createdAt = now, updatedAt = now, syncState = SyncState.PENDING,
        )
        database.withTransaction { milestoneDao.insert(entity); enqueue(entity.id, now) }
        entity.toItem()
    }

    override suspend fun update(userId: String, milestoneId: String, request: SaveMilestoneRequest): Result<MilestoneItem> = runCatching {
        validate(request)
        val existing = milestoneDao.getById(milestoneId) ?: error("Milestone not found")
        require(existing.userId == userId)
        val now = DateTimeUtil.nowEpochMillis()
        val updated = existing.copy(title = request.title.trim(), targetWeightKg = PrecisionUtil.roundWeightDisplay(request.targetWeightKg),
            rewardText = request.rewardText?.trim()?.take(P1ValidationConstants.MILESTONE_REWARD_MAX), updatedAt = now, syncState = SyncState.PENDING)
        database.withTransaction { milestoneDao.update(updated); enqueue(updated.id, now) }
        updated.toItem()
    }

    override suspend fun delete(milestoneId: String): Result<Unit> = runCatching {
        val now = DateTimeUtil.nowEpochMillis()
        database.withTransaction { milestoneDao.softDelete(milestoneId, now, now, SyncState.PENDING); enqueue(milestoneId, now) }
    }

    override suspend fun reset(milestoneId: String): Result<MilestoneItem> = runCatching {
        val existing = milestoneDao.getById(milestoneId) ?: error("Milestone not found")
        val now = DateTimeUtil.nowEpochMillis()
        val updated = existing.copy(achievedAt = null, achievedWeightKg = null, daysElapsed = null, updatedAt = now, syncState = SyncState.PENDING)
        database.withTransaction { milestoneDao.update(updated); enqueue(updated.id, now) }
        updated.toItem()
    }

    override suspend fun markShared(milestoneId: String): Result<Unit> = runCatching {
        val existing = milestoneDao.getById(milestoneId) ?: error("Milestone not found")
        val now = DateTimeUtil.nowEpochMillis()
        val updated = existing.copy(sharedCount = existing.sharedCount + 1, updatedAt = now, syncState = SyncState.PENDING)
        database.withTransaction { milestoneDao.update(updated); enqueue(updated.id, now) }
    }

    override suspend fun evaluateOnWeightRecorded(userId: String, weightKg: Double, localDate: String): List<MilestoneAchievementEvent> {
        val goal = goalDao.getGoalEffectiveOnDate(userId, localDate) ?: goalDao.getActiveGoal(userId) ?: return emptyList()
        val now = DateTimeUtil.nowEpochMillis()
        return database.withTransaction {
            val hits = MilestoneEvaluator.evaluate(weightKg, localDate, now, goal.goalType == GoalType.GAIN.name,
                milestoneDao.getActive(userId).map { MilestoneEvaluator.MilestoneCandidate(it.id, it.title, it.targetWeightKg, it.rewardText, it.createdAt) },
                profileDao.getActiveByUserId(userId)?.initialWeightKg)
            hits.map { hit ->
                val entity = milestoneDao.getById(hit.milestoneId) ?: return@map null
                milestoneDao.update(entity.copy(achievedAt = now, achievedWeightKg = weightKg, daysElapsed = hit.daysElapsed, updatedAt = now, syncState = SyncState.PENDING))
                enqueue(entity.id, now)
                MilestoneAchievementEvent(hit.milestoneId, hit.title, hit.achievedWeightKg, hit.daysElapsed, hit.rewardText)
            }.filterNotNull()
        }
    }

    override suspend fun countActive(userId: String): Int = milestoneDao.countActive(userId)

    private fun validate(request: SaveMilestoneRequest) {
        require(request.title.trim().isNotEmpty() && request.title.trim().length <= P1ValidationConstants.MILESTONE_TITLE_MAX)
        require(request.targetWeightKg > 0)
    }
    private suspend fun enqueue(id: String, now: Long) = syncQueueDao.insert(SyncQueueEntity(id = UuidV7.generate(), tableName = "milestones", rowId = id, operation = "UPSERT", createdAt = now, updatedAt = now))
    private fun MilestoneEntity.toItem() = MilestoneItem(id, title, targetWeightKg, rewardText, achievedAt, achievedWeightKg, daysElapsed, sharedCount, createdAt,
        achievedWeightKg?.let { null } ?: abs(targetWeightKg), if (achievedWeightKg != null) 1f else 0f)
}
