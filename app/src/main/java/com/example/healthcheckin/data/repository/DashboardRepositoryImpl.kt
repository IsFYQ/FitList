package com.example.healthcheckin.data.repository

import com.example.healthcheckin.data.local.dao.AppSettingDao
import com.example.healthcheckin.data.local.dao.DailyBudgetDao
import com.example.healthcheckin.data.local.dao.GoalDao
import com.example.healthcheckin.data.local.dao.MealEntryDao
import com.example.healthcheckin.data.local.dao.SyncQueueDao
import com.example.healthcheckin.data.local.dao.SyncStatusDao
import com.example.healthcheckin.data.local.dao.WeightRecordDao
import com.example.healthcheckin.data.local.entity.DailyBudgetEntity
import com.example.healthcheckin.data.local.entity.GoalEntity
import com.example.healthcheckin.data.local.entity.MealEntryEntity
import com.example.healthcheckin.data.local.entity.AppSettingEntity
import com.example.healthcheckin.data.local.entity.WeightRecordEntity
import com.example.healthcheckin.data.local.model.DailyConsumptionSummary
import com.example.healthcheckin.domain.algorithm.CalorieStateCalculator
import com.example.healthcheckin.domain.algorithm.HealthWarningEvaluator
import com.example.healthcheckin.domain.model.CalorieOverview
import com.example.healthcheckin.domain.model.ConsumptionDisplay
import com.example.healthcheckin.domain.model.DashboardData
import com.example.healthcheckin.domain.model.HealthWarning
import com.example.healthcheckin.domain.model.HealthWarningType
import com.example.healthcheckin.domain.model.MacroKind
import com.example.healthcheckin.domain.model.MealGroup
import com.example.healthcheckin.domain.model.ResolvedBudget
import com.example.healthcheckin.domain.model.SyncBadge
import com.example.healthcheckin.domain.model.SyncBadgeType
import com.example.healthcheckin.domain.model.WeightCardData
import com.example.healthcheckin.domain.repository.BackupRepository
import com.example.healthcheckin.domain.repository.DashboardRepository
import com.example.healthcheckin.util.DateTimeUtil
import com.example.healthcheckin.util.GoalType
import com.example.healthcheckin.util.MealSlot
import com.example.healthcheckin.domain.model.MacroProgress
import com.example.healthcheckin.util.DeviceTimeMonitor
import com.example.healthcheckin.util.NetworkMonitor
import com.example.healthcheckin.util.PrecisionUtil
import com.example.healthcheckin.util.SyncState
import com.example.healthcheckin.util.UuidV7
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs
import kotlin.math.min

@Singleton
class DashboardRepositoryImpl @Inject constructor(
    private val mealEntryDao: MealEntryDao,
    private val dailyBudgetDao: DailyBudgetDao,
    private val goalDao: GoalDao,
    private val weightRecordDao: WeightRecordDao,
    private val syncQueueDao: SyncQueueDao,
    private val syncStatusDao: SyncStatusDao,
    private val appSettingDao: AppSettingDao,
    private val networkMonitor: NetworkMonitor,
    private val deviceTimeMonitor: DeviceTimeMonitor,
    private val backupRepository: BackupRepository,
    private val deviceId: String,
) : DashboardRepository {

    override fun observeDashboard(userId: String, localDate: String): Flow<DashboardData> {
        return combine(
            dailyBudgetDao.observeByDate(userId, localDate),
            mealEntryDao.observeDailySummary(userId, localDate),
            mealEntryDao.observeByLocalDate(userId, localDate),
            weightRecordDao.observeLatestTwo(userId),
            goalDao.observeByUser(userId),
        ) { budgetEntity, summary, meals, weights, allGoals ->
            val effectiveGoal = allGoals
                .filter { it.effectiveFrom <= localDate }
                .maxByOrNull { it.effectiveFrom }
                ?: allGoals.minByOrNull { it.effectiveFrom }

            buildDashboardData(localDate, budgetEntity, summary, meals, weights, effectiveGoal)
        }
    }

    override suspend fun ensureTodayBudget(userId: String) {
        val today = DateTimeUtil.todayLocalDateString()
        if (dailyBudgetDao.getByDate(userId, today) != null) return
        val activeGoal = goalDao.getActiveGoal(userId) ?: return
        createBudgetSnapshot(userId, today, activeGoal)
    }

    override suspend fun deleteMealEntry(entry: MealEntryEntity): Result<Unit> = runCatching {
        val now = DateTimeUtil.nowEpochMillis()
        mealEntryDao.softDelete(entry.id, now, now, SyncState.PENDING)
    }

    override suspend fun undoDeleteMealEntry(entry: MealEntryEntity): Result<Unit> = runCatching {
        val now = DateTimeUtil.nowEpochMillis()
        mealEntryDao.restoreSoftDelete(entry.id, now, SyncState.PENDING)
    }

    override suspend fun evaluateHealthWarning(userId: String, todayEntryCount: Int): HealthWarning? {
        val dismissed = loadDismissedWarnings()
        val today = DateTimeUtil.todayLocalDate()
        val recentDays = (1..7).map { offset ->
            val date = DateTimeUtil.formatLocalDate(today.minusDays(offset.toLong()))
            val summary = mealEntryDao.getDailySummary(userId, date)
            val budgetKcal = dailyBudgetDao.getByDate(userId, date)?.budgetKcal
                ?: goalDao.getGoalEffectiveOnDate(userId, date)?.budgetKcal
                ?: goalDao.getEarliestGoal(userId)?.budgetKcal
                ?: 0
            HealthWarningEvaluator.DayStats(
                localDate = date,
                entryCount = summary.entryCount,
                totalKcal = summary.consumedKcal,
                budgetKcal = budgetKcal,
            )
        }
        return HealthWarningEvaluator.evaluate(todayEntryCount, recentDays, dismissed)
    }

    override fun observeSyncBadge(): Flow<SyncBadge> {
        return combine(
            syncQueueDao.observePendingCount(),
            syncStatusDao.observeFailedSyncCount(),
        ) { pending, failed ->
            when {
                !networkMonitor.isOnline() -> SyncBadge(SyncBadgeType.OFFLINE)
                failed > 0 -> SyncBadge(SyncBadgeType.FAILED, failed)
                pending > 0 -> SyncBadge(SyncBadgeType.PENDING, pending)
                else -> SyncBadge(SyncBadgeType.NONE)
            }
        }
    }

    override fun isDeviceTimeSuspicious(): Boolean = deviceTimeMonitor.isDeviceTimeSuspicious()

    override suspend fun triggerManualBackup() {
        backupRepository.triggerBackup(force = true)
    }

    override suspend fun dismissHealthWarning(type: HealthWarningType) {
        val key = "health_warning_dismissed_${type.name.lowercase()}"
        val cooldownDays = when (type) {
            HealthWarningType.LOW_INTAKE, HealthWarningType.HIGH_INTAKE -> 7
            HealthWarningType.RECORD_GAP -> 3
        }
        val expiresAt = DateTimeUtil.nowEpochMillis() + cooldownDays * 24L * 60 * 60 * 1000
        appSettingDao.upsert(
            AppSettingEntity(
                key = key,
                valueJson = expiresAt.toString(),
                updatedAt = DateTimeUtil.nowEpochMillis(),
            )
        )
    }

    override fun getMinViewDate(registeredLocalDate: String): String =
        DateTimeUtil.backfillMinDateString(registeredLocalDate)

    private fun buildDashboardData(
        localDate: String,
        budgetEntity: DailyBudgetEntity?,
        summary: DailyConsumptionSummary,
        meals: List<MealEntryEntity>,
        weights: List<WeightRecordEntity>,
        effectiveGoal: GoalEntity?,
    ): DashboardData {
        val resolvedBudget = when {
            budgetEntity != null -> ResolvedBudget(
                budgetKcal = budgetEntity.budgetKcal,
                proteinG = budgetEntity.proteinG,
                carbG = budgetEntity.carbG,
                fatG = budgetEntity.fatG,
                isInferred = false,
                goalType = effectiveGoal?.goalType?.let { GoalType.valueOf(it) },
                targetWeightKg = effectiveGoal?.targetWeightKg,
            )
            effectiveGoal != null -> ResolvedBudget(
                budgetKcal = effectiveGoal.budgetKcal,
                proteinG = effectiveGoal.proteinG,
                carbG = effectiveGoal.carbG,
                fatG = effectiveGoal.fatG,
                isInferred = localDate != DateTimeUtil.todayLocalDateString(),
                goalType = GoalType.valueOf(effectiveGoal.goalType),
                targetWeightKg = effectiveGoal.targetWeightKg,
            )
            else -> null
        }

        val consumption = summary.toConsumptionDisplay()
        val calorieOverview = resolvedBudget?.let { budget ->
            val (state, remaining) = CalorieStateCalculator.calculate(budget.budgetKcal, consumption.kcal)
            CalorieOverview(
                budget = budget.budgetKcal,
                consumed = consumption.kcal,
                remaining = remaining,
                state = state,
            )
        }

        return DashboardData(
            budget = resolvedBudget,
            consumption = consumption,
            calorieOverview = calorieOverview,
            macros = buildMacroProgress(resolvedBudget, consumption),
            weightCard = buildWeightCard(weights, effectiveGoal),
            mealGroups = buildMealGroups(meals),
            hasNoGoal = resolvedBudget == null,
            budgetAbnormal = resolvedBudget?.budgetKcal?.let { it <= 0 } == true,
        )
    }

    private suspend fun createBudgetSnapshot(userId: String, localDate: String, goal: GoalEntity) {
        val now = DateTimeUtil.nowEpochMillis()
        dailyBudgetDao.upsert(
            DailyBudgetEntity(
                id = UuidV7.generate(),
                userId = userId,
                localDate = localDate,
                goalId = goal.id,
                budgetKcal = goal.budgetKcal,
                proteinG = goal.proteinG,
                carbG = goal.carbG,
                fatG = goal.fatG,
                deviceId = deviceId,
                createdAt = now,
                updatedAt = now,
                syncState = SyncState.PENDING,
            )
        )
    }

    private fun DailyConsumptionSummary.toConsumptionDisplay() = ConsumptionDisplay(
        kcal = PrecisionUtil.aggregateCalories(listOf(consumedKcal)),
        proteinG = PrecisionUtil.aggregateMacros(listOf(consumedProtein)),
        carbG = PrecisionUtil.aggregateMacros(listOf(consumedCarb)),
        fatG = PrecisionUtil.aggregateMacros(listOf(consumedFat)),
        entryCount = entryCount,
    )

    private fun buildMacroProgress(
        budget: ResolvedBudget?,
        consumption: ConsumptionDisplay,
    ): List<MacroProgress> {
        if (budget == null) return emptyList()
        return listOf(
            macroItem(MacroKind.PROTEIN, consumption.proteinG, budget.proteinG),
            macroItem(MacroKind.CARB, consumption.carbG, budget.carbG),
            macroItem(MacroKind.FAT, consumption.fatG, budget.fatG),
        )
    }

    private fun macroItem(kind: MacroKind, consumed: Double, target: Double): MacroProgress {
        val name = when (kind) {
            MacroKind.PROTEIN -> "蛋白质"
            MacroKind.CARB -> "碳水"
            MacroKind.FAT -> "脂肪"
        }
        if (target <= 0) {
            return MacroProgress(name, kind, consumed, target, 0f, "—", false, null)
        }
        val ratio = consumed / target
        val progress = min(ratio.toFloat(), 1f)
        val percent = PrecisionUtil.roundInt(ratio * 100.0)
        val isOver = consumed > target
        val overAmount = if (isOver) PrecisionUtil.roundMacroDisplay(consumed - target) else null
        return MacroProgress(
            name = name,
            kind = kind,
            consumed = consumed,
            target = target,
            progress = progress,
            percentText = "$percent%",
            isOver = isOver,
            overAmount = overAmount,
        )
    }

    private fun buildMealGroups(meals: List<MealEntryEntity>): List<MealGroup> {
        val order = listOf(MealSlot.BREAKFAST, MealSlot.LUNCH, MealSlot.SNACK, MealSlot.DINNER)
        return order.mapNotNull { slot ->
            val slotMeals = meals.filter { it.mealSlot == slot.name }
            if (slotMeals.isEmpty()) return@mapNotNull null
            MealGroup(
                slot = slot,
                slotLabel = slotLabel(slot),
                totalKcal = PrecisionUtil.aggregateCalories(slotMeals.map { it.kcal }),
                entries = slotMeals,
            )
        }
    }

    private fun slotLabel(slot: MealSlot): String = when (slot) {
        MealSlot.BREAKFAST -> "早餐"
        MealSlot.LUNCH -> "午餐"
        MealSlot.DINNER -> "晚餐"
        MealSlot.SNACK -> "加餐"
    }

    private fun buildWeightCard(weights: List<WeightRecordEntity>, activeGoal: GoalEntity?): WeightCardData {
        val goalType = activeGoal?.goalType?.let { runCatching { GoalType.valueOf(it) }.getOrNull() }
        if (weights.isEmpty()) {
            return WeightCardData(null, null, false, null, false, false, goalType)
        }
        val latest = weights[0]
        val previous = weights.getOrNull(1)
        val delta = previous?.let { latest.weightKg - it.weightKg }
        val target = activeGoal?.targetWeightKg
        val distance = target?.let { abs(latest.weightKg - it) }
        return WeightCardData(
            latestWeightKg = latest.weightKg,
            deltaKg = delta?.let { PrecisionUtil.roundWeightDisplay(abs(it)) },
            deltaPositive = (delta ?: 0.0) > 0,
            distanceToTargetKg = distance?.let { PrecisionUtil.roundWeightDisplay(it) },
            goalReached = distance != null && distance <= 0.1,
            hasRecords = true,
            goalType = goalType,
        )
    }

    private suspend fun loadDismissedWarnings(): Set<HealthWarningType> {
        val now = DateTimeUtil.nowEpochMillis()
        return HealthWarningType.entries.filter { type ->
            val key = "health_warning_dismissed_${type.name.lowercase()}"
            val setting = appSettingDao.get(key) ?: return@filter false
            val expiresAt = setting.valueJson.toLongOrNull() ?: return@filter false
            expiresAt > now
        }.toSet()
    }
}
