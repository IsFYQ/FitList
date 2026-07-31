package com.example.healthcheckin.data.repository

import androidx.room.withTransaction
import com.example.healthcheckin.data.local.HealthDatabase
import com.example.healthcheckin.data.local.dao.DailyBudgetDao
import com.example.healthcheckin.data.local.dao.GoalDao
import com.example.healthcheckin.data.local.dao.ProfileDao
import com.example.healthcheckin.data.local.dao.SyncQueueDao
import com.example.healthcheckin.data.local.dao.WeightRecordDao
import com.example.healthcheckin.data.local.entity.DailyBudgetEntity
import com.example.healthcheckin.data.local.entity.GoalEntity
import com.example.healthcheckin.data.local.entity.SyncQueueEntity
import com.example.healthcheckin.data.local.entity.WeightRecordEntity
import com.example.healthcheckin.domain.algorithm.GoalCalculationService
import com.example.healthcheckin.domain.model.GoalSaveRequest
import com.example.healthcheckin.domain.model.GoalSaveResult
import com.example.healthcheckin.domain.model.OnboardingFormData
import com.example.healthcheckin.domain.repository.GoalRepository
import com.example.healthcheckin.util.ActivityLevel
import com.example.healthcheckin.util.DateTimeUtil
import com.example.healthcheckin.util.PrecisionUtil
import com.example.healthcheckin.util.Sex
import com.example.healthcheckin.util.SyncState
import com.example.healthcheckin.util.UuidV7
import com.example.healthcheckin.util.ValidationConstants
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GoalRepositoryImpl @Inject constructor(
    private val database: HealthDatabase,
    private val profileDao: ProfileDao,
    private val goalDao: GoalDao,
    private val dailyBudgetDao: DailyBudgetDao,
    private val weightRecordDao: WeightRecordDao,
    private val syncQueueDao: SyncQueueDao,
    private val deviceId: String,
) : GoalRepository {

    override fun observeActiveGoal(userId: String): Flow<GoalEntity?> =
        goalDao.observeActiveGoal(userId)

    override suspend fun getActiveGoal(userId: String): GoalEntity? =
        goalDao.getActiveGoal(userId)

    override suspend fun loadFormFromProfile(userId: String): OnboardingFormData? {
        val profile = profileDao.getById(userId) ?: return null
        val goal = goalDao.getActiveGoal(userId)
        return OnboardingFormData(
            sex = profile.sex?.let { Sex.valueOf(it) },
            birthYearMonth = profile.birthYearMonth ?: ValidationConstants.DEFAULT_BIRTH_YEAR_MONTH,
            heightCm = profile.heightCm?.let { PrecisionUtil.roundWeightDisplay(it).toString() } ?: "",
            currentWeightKg = goal?.currentWeightKg?.let { PrecisionUtil.roundWeightDisplay(it).toString() } ?: "",
            targetWeightKg = goal?.targetWeightKg?.let { PrecisionUtil.roundWeightDisplay(it).toString() } ?: "",
            targetWeeks = goal?.targetWeeks ?: ValidationConstants.DEFAULT_TARGET_WEEKS,
            activityLevel = goal?.activityLevel?.let { ActivityLevel.valueOf(it) } ?: ActivityLevel.LIGHT,
        )
    }

    override suspend fun hasCompletedOnboarding(userId: String): Boolean {
        val profile = profileDao.getById(userId) ?: return false
        return profile.onboardingCompletedAt != null
    }

    override suspend fun shouldPromptAgeUpdate(userId: String): Boolean {
        val profile = profileDao.getById(userId) ?: return false
        val goal = goalDao.getActiveGoal(userId) ?: return false
        val sex = profile.sex?.let { Sex.valueOf(it) } ?: return false
        val birthYearMonth = profile.birthYearMonth ?: return false
        val height = profile.heightCm ?: return false
        val weight = goal.currentWeightKg
        val age = DateTimeUtil.ageYears(birthYearMonth)
        val recalculated = GoalCalculationService.calculate(
            sex = sex,
            currentWeightKg = weight,
            heightCm = height,
            ageYears = age,
            targetWeightKg = goal.targetWeightKg,
            targetWeeks = goal.targetWeeks,
            activityLevel = ActivityLevel.valueOf(goal.activityLevel),
        )
        return kotlin.math.abs(recalculated.bmr.bmrKcal - goal.bmrKcal) >= ValidationConstants.BMR_AGE_UPDATE_THRESHOLD
    }

    override suspend fun saveGoal(userId: String, request: GoalSaveRequest): Result<GoalSaveResult> = runCatching {
        val now = DateTimeUtil.nowEpochMillis()
        val today = DateTimeUtil.todayLocalDateString()
        val age = DateTimeUtil.ageYears(request.birthYearMonth)
        val calculation = GoalCalculationService.calculate(
            sex = request.sex,
            currentWeightKg = request.currentWeightKg,
            heightCm = request.heightCm,
            ageYears = age,
            targetWeightKg = request.targetWeightKg,
            targetWeeks = request.targetWeeks,
            activityLevel = request.activityLevel,
        )

        val goalId = UuidV7.generate()
        val budgetId = UuidV7.generate()
        val weightRecordId = UuidV7.generate()

        val goalEntity = GoalEntity(
            id = goalId,
            userId = userId,
            currentWeightKg = PrecisionUtil.roundWeightDisplay(request.currentWeightKg),
            targetWeightKg = PrecisionUtil.roundWeightDisplay(request.targetWeightKg),
            targetWeeks = request.targetWeeks,
            activityLevel = request.activityLevel.name,
            goalType = calculation.budget.goalType.name,
            bmrKcal = calculation.bmr.bmrKcal,
            tdeeKcal = calculation.tdeeKcal,
            dailyDeltaKcal = calculation.budget.dailyDeltaKcal,
            budgetKcal = calculation.finalBudgetKcal,
            proteinG = calculation.macro.proteinG,
            carbG = calculation.macro.carbG,
            fatG = calculation.macro.fatG,
            clamped = calculation.budget.clamped || calculation.bmr.bmrClamped,
            estWeeks = calculation.budget.estWeeks,
            effectiveFrom = today,
            isActive = true,
            deviceId = deviceId,
            createdAt = now,
            updatedAt = now,
            syncState = SyncState.PENDING,
        )

        val dailyBudgetEntity = DailyBudgetEntity(
            id = budgetId,
            userId = userId,
            localDate = today,
            goalId = goalId,
            budgetKcal = calculation.finalBudgetKcal,
            proteinG = calculation.macro.proteinG,
            carbG = calculation.macro.carbG,
            fatG = calculation.macro.fatG,
            deviceId = deviceId,
            createdAt = now,
            updatedAt = now,
            syncState = SyncState.PENDING,
        )

        var promptWeightRecord = false

        database.withTransaction {
            val profile = profileDao.getById(userId)
                ?: throw IllegalStateException("Profile not found")

            goalDao.deactivateAll(userId, now)

            goalDao.insert(goalEntity)
            enqueueSync("goals", goalId, now)

            profileDao.update(
                profile.copy(
                    sex = request.sex.name,
                    birthYearMonth = request.birthYearMonth,
                    heightCm = PrecisionUtil.roundWeightDisplay(request.heightCm),
                    initialWeightKg = if (request.isFirstTime) {
                        PrecisionUtil.roundWeightDisplay(request.currentWeightKg)
                    } else {
                        profile.initialWeightKg
                            ?: PrecisionUtil.roundWeightDisplay(request.currentWeightKg)
                    },
                    onboardingCompletedAt = profile.onboardingCompletedAt ?: now,
                    updatedAt = now,
                    syncState = SyncState.PENDING,
                )
            )
            enqueueSync("profiles", userId, now)

            dailyBudgetDao.upsert(dailyBudgetEntity)
            enqueueSync("daily_budgets", budgetId, now)

            if (request.isFirstTime) {
                val existingWeight = weightRecordDao.getByDate(userId, today)
                if (existingWeight == null) {
                    val weightEntity = WeightRecordEntity(
                        id = weightRecordId,
                        userId = userId,
                        localDate = today,
                        tzOffsetMinutes = DateTimeUtil.tzOffsetMinutes(),
                        weightKg = PrecisionUtil.roundWeightDisplay(request.currentWeightKg),
                        deviceId = deviceId,
                        createdAt = now,
                        updatedAt = now,
                        syncState = SyncState.PENDING,
                    )
                    weightRecordDao.insert(weightEntity)
                    enqueueSync("weight_records", weightRecordId, now)
                }
            } else {
                val previous = request.previousCurrentWeightKg
                if (previous != null &&
                    PrecisionUtil.roundWeightDisplay(previous) !=
                    PrecisionUtil.roundWeightDisplay(request.currentWeightKg)
                ) {
                    promptWeightRecord = true
                }
            }
        }

        GoalSaveResult(
            goalId = goalId,
            promptWeightRecord = promptWeightRecord,
            weightToRecord = if (promptWeightRecord) {
                PrecisionUtil.roundWeightDisplay(request.currentWeightKg)
            } else {
                null
            },
        )
    }

    override suspend fun recordWeight(userId: String, weightKg: Double): Result<Unit> = runCatching {
        val now = DateTimeUtil.nowEpochMillis()
        val today = DateTimeUtil.todayLocalDateString()
        val weightId = UuidV7.generate()
        val rounded = PrecisionUtil.roundWeightDisplay(weightKg)

        database.withTransaction {
            val existing = weightRecordDao.getByDate(userId, today)
            if (existing != null) {
                weightRecordDao.update(
                    existing.copy(
                        weightKg = rounded,
                        updatedAt = now,
                        syncState = SyncState.PENDING,
                    )
                )
                enqueueSync("weight_records", existing.id, now)
            } else {
                weightRecordDao.insert(
                    WeightRecordEntity(
                        id = weightId,
                        userId = userId,
                        localDate = today,
                        tzOffsetMinutes = DateTimeUtil.tzOffsetMinutes(),
                        weightKg = rounded,
                        deviceId = deviceId,
                        createdAt = now,
                        updatedAt = now,
                        syncState = SyncState.PENDING,
                    )
                )
                enqueueSync("weight_records", weightId, now)
            }
        }
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
}
