package com.example.healthcheckin.data.repository

import androidx.room.withTransaction
import com.example.healthcheckin.data.local.HealthDatabase
import com.example.healthcheckin.data.local.dao.FoodDao
import com.example.healthcheckin.data.local.dao.MealEntryDao
import com.example.healthcheckin.data.local.dao.SyncQueueDao
import com.example.healthcheckin.data.local.entity.FoodEntity
import com.example.healthcheckin.data.local.entity.SyncQueueEntity
import com.example.healthcheckin.domain.model.FoodSearchItem
import com.example.healthcheckin.domain.repository.FoodRepository
import com.example.healthcheckin.domain.repository.SaveCustomFoodRequest
import com.example.healthcheckin.domain.repository.SaveCustomFoodResult
import com.example.healthcheckin.util.BasisUnit
import com.example.healthcheckin.util.CustomFoodValidator
import com.example.healthcheckin.util.DateTimeUtil
import com.example.healthcheckin.util.FoodSource
import com.example.healthcheckin.util.MealSlot
import com.example.healthcheckin.util.MealUnit
import com.example.healthcheckin.util.SyncState
import com.example.healthcheckin.util.UuidV7
import com.example.healthcheckin.util.Validators
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FoodRepositoryImpl @Inject constructor(
    private val database: HealthDatabase,
    private val foodDao: FoodDao,
    private val mealEntryDao: MealEntryDao,
    private val syncQueueDao: SyncQueueDao,
    private val deviceId: String,
) : FoodRepository {

    override suspend fun listCustomFoods(userId: String): List<FoodSearchItem> =
        foodDao.listCustomFoods(userId).map { it.toSearchItem() }

    override suspend fun getCustomFood(userId: String, foodId: String): FoodSearchItem? {
        val entity = foodDao.getById(foodId) ?: return null
        if (entity.userId != userId || entity.source != FoodSource.CUSTOM.name) return null
        return entity.toSearchItem()
    }

    override suspend fun getFoodSearchItem(userId: String, foodId: String): FoodSearchItem? {
        val entity = foodDao.getById(foodId) ?: return null
        if (entity.userId != userId) return null
        return entity.toSearchItem()
    }

    override suspend fun saveCustomFood(
        userId: String,
        request: SaveCustomFoodRequest,
    ): SaveCustomFoodResult {
        val validation = CustomFoodValidator.validate(
            name = request.name,
            kcalPer100 = request.kcalPer100,
            proteinPer100 = request.proteinPer100,
            carbPer100 = request.carbPer100,
            fatPer100 = request.fatPer100,
            servingGrams = request.servingGrams,
        )
        if (validation != null) {
            return SaveCustomFoodResult.ValidationFailed(validation)
        }

        val normalizedName = Validators.normalizeFoodName(request.name)
        val displayName = Validators.normalizeText(request.name)
        val existing = foodDao.findCustomByName(userId, normalizedName)
        if (existing != null && existing.id != request.id && request.overwriteExistingId == null) {
            return SaveCustomFoodResult.DuplicateName(existing.name, existing.id)
        }

        val now = DateTimeUtil.nowEpochMillis()
        val targetId = request.overwriteExistingId ?: request.id ?: UuidV7.generate()
        val existingEntity = request.id?.let { foodDao.getById(it) }
            ?: request.overwriteExistingId?.let { foodDao.getById(it) }

        val entity = FoodEntity(
            id = targetId,
            userId = userId,
            source = FoodSource.CUSTOM.name,
            externalId = null,
            name = displayName,
            nameNormalized = normalizedName,
            brand = null,
            basisUnit = request.basisUnit.name,
            kcalPer100 = request.kcalPer100,
            proteinPer100 = request.proteinPer100,
            carbPer100 = request.carbPer100,
            fatPer100 = request.fatPer100,
            servingName = if (request.servingGrams != null) "1份" else null,
            servingGrams = request.servingGrams,
            dataIncomplete = request.servingGrams == null,
            nutritionWarning = false,
            lastUsedAt = existingEntity?.lastUsedAt,
            useCount30d = existingEntity?.useCount30d ?: 0,
            lastQuantity = existingEntity?.lastQuantity,
            lastUnit = existingEntity?.lastUnit,
            lastMealSlot = existingEntity?.lastMealSlot,
            deviceId = existingEntity?.deviceId ?: deviceId,
            createdAt = existingEntity?.createdAt ?: now,
            updatedAt = now,
            syncState = SyncState.PENDING,
        )

        database.withTransaction {
            foodDao.insert(entity)
            enqueueSync("foods", entity.id, now)
        }
        return SaveCustomFoodResult.Success(entity.toSearchItem())
    }

    override suspend fun deleteCustomFood(userId: String, foodId: String): Result<Unit> = runCatching {
        val entity = foodDao.getById(foodId) ?: throw IllegalStateException("Food not found")
        require(entity.userId == userId && entity.source == FoodSource.CUSTOM.name) {
            "Not a custom food owned by user"
        }
        val now = DateTimeUtil.nowEpochMillis()
        database.withTransaction {
            foodDao.softDelete(foodId, now, now, SyncState.PENDING)
            enqueueSync("foods", foodId, now)
        }
    }

    override suspend fun countMealReferences(foodId: String): Int =
        mealEntryDao.countByFoodId(foodId)

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

    private fun FoodEntity.toSearchItem() = FoodSearchItem(
        foodId = id,
        publicFoodId = null,
        externalId = externalId,
        name = name,
        brand = brand,
        kcalPer100 = kcalPer100,
        proteinPer100 = proteinPer100,
        carbPer100 = carbPer100,
        fatPer100 = fatPer100,
        basisUnit = runCatching { BasisUnit.valueOf(basisUnit) }.getOrDefault(BasisUnit.G),
        servingName = servingName,
        servingGrams = servingGrams,
        source = FoodSource.CUSTOM,
        dataIncomplete = dataIncomplete,
        nutritionWarning = nutritionWarning,
        lastUsedAt = lastUsedAt,
        lastQuantity = lastQuantity,
        lastUnit = lastUnit?.let { runCatching { MealUnit.valueOf(it) }.getOrNull() },
        lastMealSlot = lastMealSlot?.let { runCatching { MealSlot.valueOf(it) }.getOrNull() },
    )
}
