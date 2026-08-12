package com.example.healthcheckin.data.backup

import com.example.healthcheckin.data.local.dao.AnalyticsEventDao
import com.example.healthcheckin.data.local.dao.DailyBudgetDao
import com.example.healthcheckin.data.local.dao.FoodDao
import com.example.healthcheckin.data.local.dao.GoalDao
import com.example.healthcheckin.data.local.dao.MealEntryDao
import com.example.healthcheckin.data.local.dao.ProfileDao
import com.example.healthcheckin.data.local.dao.WeightRecordDao
import com.example.healthcheckin.data.local.dao.BodyMeasurementDao
import com.example.healthcheckin.data.local.dao.MilestoneDao
import com.example.healthcheckin.data.local.dao.InventoryItemDao
import com.example.healthcheckin.data.local.dao.InventoryLedgerDao
import com.example.healthcheckin.data.local.dao.IngredientBindingDao
import com.example.healthcheckin.data.local.entity.AnalyticsEventEntity
import com.example.healthcheckin.data.local.entity.DailyBudgetEntity
import com.example.healthcheckin.data.local.entity.FoodEntity
import com.example.healthcheckin.data.local.entity.GoalEntity
import com.example.healthcheckin.data.local.entity.MealEntryEntity
import com.example.healthcheckin.data.local.entity.ProfileEntity
import com.example.healthcheckin.data.local.entity.WeightRecordEntity
import com.example.healthcheckin.data.local.entity.BodyMeasurementEntity
import com.example.healthcheckin.data.local.entity.MilestoneEntity
import com.example.healthcheckin.data.local.entity.InventoryItemEntity
import com.example.healthcheckin.data.local.entity.InventoryLedgerEntity
import com.example.healthcheckin.data.local.entity.IngredientBindingEntity
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
    private val bodyMeasurementDao: BodyMeasurementDao,
    private val milestoneDao: MilestoneDao,
    private val inventoryItemDao: InventoryItemDao,
    private val inventoryLedgerDao: InventoryLedgerDao,
    private val ingredientBindingDao: IngredientBindingDao,
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
        BackupTable.BODY_MEASUREMENTS -> bodyMeasurementDao.getByIdRaw(rowId)
        BackupTable.MILESTONES -> milestoneDao.getByIdRaw(rowId)
        BackupTable.INVENTORY_ITEMS -> inventoryItemDao.getByIdRaw(rowId)
        BackupTable.INVENTORY_LEDGER -> inventoryLedgerDao.getByIdRaw(rowId)
        BackupTable.INGREDIENT_BINDINGS -> ingredientBindingDao.getByIdRaw(rowId)
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
        BackupTable.BODY_MEASUREMENTS -> rows.forEach { bodyMeasurementDao.insert(it as BodyMeasurementEntity) }
        BackupTable.MILESTONES -> rows.forEach { milestoneDao.insert(it as MilestoneEntity) }
        BackupTable.INVENTORY_ITEMS -> rows.forEach { inventoryItemDao.insert(it as InventoryItemEntity) }
        BackupTable.INVENTORY_LEDGER -> rows.forEach { inventoryLedgerDao.insert(it as InventoryLedgerEntity) }
        BackupTable.INGREDIENT_BINDINGS -> rows.forEach { ingredientBindingDao.insert(it as IngredientBindingEntity) }
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
        ingredientBindingDao.deleteAllForUser(userId)
        inventoryLedgerDao.deleteAllForUser(userId)
        inventoryItemDao.deleteAllForUser(userId)
        milestoneDao.deleteAllForUser(userId)
        bodyMeasurementDao.deleteAllForUser(userId)
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
            BackupTable.BODY_MEASUREMENTS -> bodyMeasurementDao.getByIdRaw(rowId)?.let {
                bodyMeasurementDao.update(it.copy(syncState = state, updatedAt = now))
            }
            BackupTable.MILESTONES -> milestoneDao.getByIdRaw(rowId)?.let {
                milestoneDao.update(it.copy(syncState = state, updatedAt = now))
            }
            BackupTable.INVENTORY_ITEMS -> inventoryItemDao.getByIdRaw(rowId)?.let {
                inventoryItemDao.update(it.copy(syncState = state, updatedAt = now))
            }
            BackupTable.INVENTORY_LEDGER -> inventoryLedgerDao.updateSyncState(rowId, state, now)
            BackupTable.INGREDIENT_BINDINGS -> ingredientBindingDao.getByIdRaw(rowId)?.let {
                ingredientBindingDao.update(it.copy(syncState = state, updatedAt = now))
            }
            BackupTable.ANALYTICS_EVENTS -> analyticsEventDao.updateSyncState(rowId, state, now)
        }
    }
}
