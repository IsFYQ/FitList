package com.example.healthcheckin.data.repository

import androidx.room.withTransaction
import com.example.healthcheckin.data.local.HealthDatabase
import com.example.healthcheckin.data.local.dao.DailyBudgetDao
import com.example.healthcheckin.data.local.dao.FoodDao
import com.example.healthcheckin.data.local.dao.GoalDao
import com.example.healthcheckin.data.local.dao.MealEntryDao
import com.example.healthcheckin.data.local.dao.PublicFoodDao
import com.example.healthcheckin.data.local.dao.SyncQueueDao
import com.example.healthcheckin.data.local.entity.DailyBudgetEntity
import com.example.healthcheckin.data.local.entity.FoodEntity
import com.example.healthcheckin.data.local.entity.MealEntryEntity
import com.example.healthcheckin.data.local.entity.SyncQueueEntity
import com.example.healthcheckin.domain.algorithm.MealNutritionCalculator
import com.example.healthcheckin.domain.model.AddMealRequest
import com.example.healthcheckin.domain.model.FoodSearchItem
import com.example.healthcheckin.domain.model.UpdateMealRequest
import com.example.healthcheckin.domain.repository.MealRepository
import com.example.healthcheckin.util.DateTimeUtil
import com.example.healthcheckin.util.SyncState
import com.example.healthcheckin.util.UuidV7
import com.example.healthcheckin.util.Validators
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MealRepositoryImpl @Inject constructor(
    private val database: HealthDatabase,
    private val mealEntryDao: MealEntryDao,
    private val foodDao: FoodDao,
    private val publicFoodDao: PublicFoodDao,
    private val dailyBudgetDao: DailyBudgetDao,
    private val goalDao: GoalDao,
    private val syncQueueDao: SyncQueueDao,
    private val deviceId: String,
) : MealRepository {

    override suspend fun addMeal(userId: String, request: AddMealRequest): Result<MealEntryEntity> =
        runCatching {
            validateConsumedAt(request.consumedAt)
            val now = DateTimeUtil.nowEpochMillis()
            val entryId = UuidV7.generate()

            database.withTransaction {
                val food = resolveFood(userId, request.food, request.servingGrams, now)
                val effectiveServingGrams = request.servingGrams ?: food.servingGrams
                val nutrition = MealNutritionCalculator.compute(
                    quantity = request.quantity,
                    unit = request.unit,
                    servingGrams = effectiveServingGrams,
                    kcalPer100 = food.kcalPer100,
                    proteinPer100 = food.proteinPer100,
                    carbPer100 = food.carbPer100,
                    fatPer100 = food.fatPer100,
                )
                val basisAmount = MealNutritionCalculator.basisAmount(
                    request.quantity,
                    request.unit,
                    effectiveServingGrams,
                )
                val localDate = DateTimeUtil.toLocalDateString(request.consumedAt)

                val entry = MealEntryEntity(
                    id = entryId,
                    userId = userId,
                    localDate = localDate,
                    tzOffsetMinutes = DateTimeUtil.tzOffsetMinutes(),
                    consumedAt = request.consumedAt,
                    mealSlot = request.mealSlot.name,
                    foodId = food.id,
                    quantity = request.quantity,
                    unit = request.unit.name,
                    basisAmount = basisAmount,
                    snapFoodName = food.name,
                    snapBrand = food.brand,
                    snapSource = food.source,
                    snapBasisUnit = food.basisUnit,
                    snapKcalPer100 = food.kcalPer100,
                    snapProteinPer100 = food.proteinPer100,
                    snapCarbPer100 = food.carbPer100,
                    snapFatPer100 = food.fatPer100,
                    snapServingName = food.servingName,
                    snapServingGrams = effectiveServingGrams,
                    kcal = nutrition.kcal,
                    proteinG = nutrition.proteinG,
                    carbG = nutrition.carbG,
                    fatG = nutrition.fatG,
                    entrySource = request.entrySource.name,
                    deviceId = deviceId,
                    createdAt = now,
                    updatedAt = now,
                    syncState = SyncState.PENDING,
                )
                mealEntryDao.insert(entry)
                enqueueSync("meal_entries", entryId, now)

                foodDao.update(
                    food.copy(
                        lastUsedAt = now,
                        lastQuantity = request.quantity,
                        lastUnit = request.unit.name,
                        lastMealSlot = request.mealSlot.name,
                        useCount30d = food.useCount30d + 1,
                        servingGrams = if (request.servingGrams != null) request.servingGrams else food.servingGrams,
                        dataIncomplete = if (request.servingGrams != null) false else food.dataIncomplete,
                        updatedAt = now,
                        syncState = SyncState.PENDING,
                    )
                )
                enqueueSync("foods", food.id, now)

                ensureDailyBudget(userId, localDate, now)
                entry
            }
        }

    override suspend fun updateMeal(userId: String, request: UpdateMealRequest): Result<MealEntryEntity> =
        runCatching {
            validateConsumedAt(request.consumedAt)
            val existing = mealEntryDao.getById(request.entryId)
                ?: throw IllegalStateException("Entry not found")
            require(existing.userId == userId) { "Entry does not belong to user" }

            val now = DateTimeUtil.nowEpochMillis()
            val effectiveServingGrams = request.servingGrams ?: existing.snapServingGrams
            val nutrition = MealNutritionCalculator.compute(
                quantity = request.quantity,
                unit = request.unit,
                servingGrams = effectiveServingGrams,
                kcalPer100 = existing.snapKcalPer100,
                proteinPer100 = existing.snapProteinPer100,
                carbPer100 = existing.snapCarbPer100,
                fatPer100 = existing.snapFatPer100,
            )
            val basisAmount = MealNutritionCalculator.basisAmount(
                request.quantity,
                request.unit,
                effectiveServingGrams,
            )
            val localDate = DateTimeUtil.toLocalDateString(request.consumedAt)
            val previousDate = existing.localDate

            database.withTransaction {
                val updated = existing.copy(
                    localDate = localDate,
                    tzOffsetMinutes = DateTimeUtil.tzOffsetMinutes(),
                    consumedAt = request.consumedAt,
                    mealSlot = request.mealSlot.name,
                    quantity = request.quantity,
                    unit = request.unit.name,
                    basisAmount = basisAmount,
                    snapServingGrams = effectiveServingGrams,
                    kcal = nutrition.kcal,
                    proteinG = nutrition.proteinG,
                    carbG = nutrition.carbG,
                    fatG = nutrition.fatG,
                    updatedAt = now,
                    syncState = SyncState.PENDING,
                )
                mealEntryDao.update(updated)
                enqueueSync("meal_entries", updated.id, now)

                existing.foodId?.let { foodId ->
                    foodDao.getById(foodId)?.let { food ->
                        foodDao.update(
                            food.copy(
                                lastUsedAt = now,
                                lastQuantity = request.quantity,
                                lastUnit = request.unit.name,
                                lastMealSlot = request.mealSlot.name,
                                servingGrams = effectiveServingGrams ?: food.servingGrams,
                                updatedAt = now,
                                syncState = SyncState.PENDING,
                            )
                        )
                        enqueueSync("foods", foodId, now)
                    }
                }

                ensureDailyBudget(userId, localDate, now)
                if (previousDate != localDate) {
                    ensureDailyBudget(userId, previousDate, now)
                }
                updated
            }
        }

    override suspend fun deleteMeal(entryId: String): Result<Unit> = runCatching {
        val now = DateTimeUtil.nowEpochMillis()
        mealEntryDao.softDelete(entryId, now, now, SyncState.PENDING)
        enqueueSync("meal_entries", entryId, now)
    }

    override suspend fun getMealById(entryId: String): MealEntryEntity? =
        mealEntryDao.getById(entryId)

    override suspend fun undoDelete(entryId: String): Result<Unit> = runCatching {
        val now = DateTimeUtil.nowEpochMillis()
        mealEntryDao.restoreSoftDelete(entryId, now, SyncState.PENDING)
        enqueueSync("meal_entries", entryId, now)
    }

    private suspend fun resolveFood(
        userId: String,
        item: FoodSearchItem,
        servingGramsOverride: Double?,
        now: Long,
    ): FoodEntity {
        item.foodId?.let { foodDao.getById(it) }?.let { return it }

        if (item.externalId != null) {
            foodDao.getByExternalId(userId, item.source.name, item.externalId)?.let { return it }
        }

        val publicFood = item.publicFoodId?.let { publicFoodDao.getById(it) }

        val servingGrams = servingGramsOverride ?: item.servingGrams ?: publicFood?.servingGrams
        val id = UuidV7.generate()
        val entity = FoodEntity(
            id = id,
            userId = userId,
            source = item.source.name,
            externalId = item.externalId ?: publicFood?.externalId,
            name = item.name,
            nameNormalized = Validators.normalizeFoodName(item.name),
            brand = item.brand ?: publicFood?.brand,
            basisUnit = item.basisUnit.name,
            kcalPer100 = item.kcalPer100,
            proteinPer100 = item.proteinPer100 ?: publicFood?.proteinPer100,
            carbPer100 = item.carbPer100 ?: publicFood?.carbPer100,
            fatPer100 = item.fatPer100 ?: publicFood?.fatPer100,
            servingName = item.servingName ?: publicFood?.servingName,
            servingGrams = servingGrams,
            dataIncomplete = item.dataIncomplete && servingGramsOverride == null,
            nutritionWarning = item.nutritionWarning,
            deviceId = deviceId,
            createdAt = now,
            updatedAt = now,
            syncState = SyncState.PENDING,
        )
        foodDao.insert(entity)
        enqueueSync("foods", id, now)
        return entity
    }

    private suspend fun ensureDailyBudget(userId: String, localDate: String, now: Long) {
        if (dailyBudgetDao.getByDate(userId, localDate) != null) return
        val goal = goalDao.getGoalEffectiveOnDate(userId, localDate)
            ?: goalDao.getActiveGoal(userId)
            ?: return
        val budgetId = UuidV7.generate()
        dailyBudgetDao.upsert(
            DailyBudgetEntity(
                id = budgetId,
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
        enqueueSync("daily_budgets", budgetId, now)
    }

    private fun validateConsumedAt(consumedAt: Long) {
        require(consumedAt <= DateTimeUtil.nowEpochMillis()) {
            "Cannot record future meals"
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
