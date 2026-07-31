package com.example.healthcheckin.data.backup

import com.example.healthcheckin.data.local.dao.AnalyticsEventDao
import com.example.healthcheckin.data.local.dao.DailyBudgetDao
import com.example.healthcheckin.data.local.dao.FoodDao
import com.example.healthcheckin.data.local.dao.GoalDao
import com.example.healthcheckin.data.local.dao.MealEntryDao
import com.example.healthcheckin.data.local.dao.ProfileDao
import com.example.healthcheckin.data.local.dao.WeightRecordDao
import com.example.healthcheckin.data.local.entity.AnalyticsEventEntity
import com.example.healthcheckin.data.local.entity.DailyBudgetEntity
import com.example.healthcheckin.data.local.entity.FoodEntity
import com.example.healthcheckin.data.local.entity.GoalEntity
import com.example.healthcheckin.data.local.entity.MealEntryEntity
import com.example.healthcheckin.data.local.entity.ProfileEntity
import com.example.healthcheckin.data.local.entity.WeightRecordEntity
import com.example.healthcheckin.util.DateTimeUtil
import com.example.healthcheckin.util.SyncState
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BackupRowLoader @Inject constructor(
    private val profileDao: ProfileDao,
    private val goalDao: GoalDao,
    private val dailyBudgetDao: DailyBudgetDao,
    private val foodDao: FoodDao,
    private val mealEntryDao: MealEntryDao,
    private val weightRecordDao: WeightRecordDao,
    private val analyticsEventDao: AnalyticsEventDao,
) {
    suspend fun loadRows(table: BackupTable, rowIds: List<String>): List<Any> =
        rowIds.mapNotNull { loadRow(table, it) }

    suspend fun loadRow(table: BackupTable, rowId: String): Any? = when (table) {
        BackupTable.PROFILES -> profileDao.getByIdRaw(rowId)
        BackupTable.GOALS -> goalDao.getByIdRaw(rowId)
        BackupTable.DAILY_BUDGETS -> dailyBudgetDao.getByIdRaw(rowId)
        BackupTable.FOODS -> foodDao.getByIdRaw(rowId)
        BackupTable.MEAL_ENTRIES -> mealEntryDao.getByIdRaw(rowId)
        BackupTable.WEIGHT_RECORDS -> weightRecordDao.getByIdRaw(rowId)
        BackupTable.ANALYTICS_EVENTS -> analyticsEventDao.getByIdRaw(rowId)
    }

    suspend fun markSyncing(table: BackupTable, rowIds: List<String>) {
        val now = DateTimeUtil.nowEpochMillis()
        rowIds.forEach { id -> markSyncState(table, id, SyncState.SYNCING, now) }
    }

    suspend fun markSynced(table: BackupTable, rowIds: List<String>) {
        val now = DateTimeUtil.nowEpochMillis()
        rowIds.forEach { id -> markSyncState(table, id, SyncState.SYNCED, now) }
    }

    suspend fun markFailed(table: BackupTable, rowIds: List<String>) {
        val now = DateTimeUtil.nowEpochMillis()
        rowIds.forEach { id -> markSyncState(table, id, SyncState.FAILED, now) }
    }

    suspend fun insertRestored(table: BackupTable, rows: List<Any>) {
        when (table) {
            BackupTable.PROFILES -> rows.forEach { profileDao.insert(it as ProfileEntity) }
            BackupTable.GOALS -> rows.forEach { goalDao.insert(it as GoalEntity) }
            BackupTable.DAILY_BUDGETS -> rows.forEach { dailyBudgetDao.upsert(it as DailyBudgetEntity) }
            BackupTable.FOODS -> rows.forEach { foodDao.insert(it as FoodEntity) }
            BackupTable.MEAL_ENTRIES -> rows.forEach { mealEntryDao.insert(it as MealEntryEntity) }
            BackupTable.WEIGHT_RECORDS -> rows.forEach { weightRecordDao.insert(it as WeightRecordEntity) }
            BackupTable.ANALYTICS_EVENTS -> rows.forEach { analyticsEventDao.insert(it as AnalyticsEventEntity) }
        }
    }

    suspend fun clearUserData(userId: String) {
        mealEntryDao.deleteAllForUser(userId)
        weightRecordDao.deleteAllForUser(userId)
        foodDao.deleteAllForUser(userId)
        dailyBudgetDao.deleteAllForUser(userId)
        goalDao.deleteAllForUser(userId)
        analyticsEventDao.deleteAllForUser(userId)
        profileDao.deleteAllForUser(userId)
    }

    private suspend fun markSyncState(table: BackupTable, rowId: String, state: String, now: Long) {
        when (table) {
            BackupTable.PROFILES -> profileDao.getByIdRaw(rowId)?.let {
                profileDao.update(it.copy(syncState = state, updatedAt = now))
            }
            BackupTable.GOALS -> goalDao.getByIdRaw(rowId)?.let {
                goalDao.update(it.copy(syncState = state, updatedAt = now))
            }
            BackupTable.DAILY_BUDGETS -> dailyBudgetDao.getByIdRaw(rowId)?.let {
                dailyBudgetDao.upsert(it.copy(syncState = state, updatedAt = now))
            }
            BackupTable.FOODS -> foodDao.getByIdRaw(rowId)?.let {
                foodDao.update(it.copy(syncState = state, updatedAt = now))
            }
            BackupTable.MEAL_ENTRIES -> mealEntryDao.getByIdRaw(rowId)?.let {
                mealEntryDao.update(it.copy(syncState = state, updatedAt = now))
            }
            BackupTable.WEIGHT_RECORDS -> weightRecordDao.getByIdRaw(rowId)?.let {
                weightRecordDao.update(it.copy(syncState = state, updatedAt = now))
            }
            BackupTable.ANALYTICS_EVENTS -> analyticsEventDao.updateSyncState(rowId, state, now)
        }
    }
}
