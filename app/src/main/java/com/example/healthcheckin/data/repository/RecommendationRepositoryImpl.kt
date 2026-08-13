package com.example.healthcheckin.data.repository

import com.example.healthcheckin.data.local.dao.DailyBudgetDao
import com.example.healthcheckin.data.local.dao.FoodDao
import com.example.healthcheckin.data.local.dao.IngredientBindingDao
import com.example.healthcheckin.data.local.dao.InventoryItemDao
import com.example.healthcheckin.data.local.dao.MealEntryDao
import com.example.healthcheckin.data.local.dao.PublicFoodDao
import com.example.healthcheckin.data.local.entity.FoodEntity
import com.example.healthcheckin.data.local.entity.InventoryItemEntity
import com.example.healthcheckin.data.local.entity.PublicFoodEntity
import com.example.healthcheckin.domain.algorithm.InventoryExpiryEvaluator
import com.example.healthcheckin.domain.algorithm.NutritionRecommendationEngine
import com.example.healthcheckin.domain.algorithm.RecommendationCandidate
import com.example.healthcheckin.domain.algorithm.RecommendationResult
import com.example.healthcheckin.domain.model.FoodSearchItem
import com.example.healthcheckin.domain.model.RecommendationMealBatchItem
import com.example.healthcheckin.domain.repository.RecommendationRepository
import com.example.healthcheckin.util.BasisUnit
import com.example.healthcheckin.util.FoodSource
import com.example.healthcheckin.util.InventoryExpiryStatus
import com.example.healthcheckin.util.MealUnit
import com.example.healthcheckin.util.Validators
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecommendationRepositoryImpl @Inject constructor(
    private val dailyBudgetDao: DailyBudgetDao,
    private val mealEntryDao: MealEntryDao,
    private val inventoryItemDao: InventoryItemDao,
    private val foodDao: FoodDao,
    private val publicFoodDao: PublicFoodDao,
    private val bindingDao: IngredientBindingDao,
) : RecommendationRepository {

    override suspend fun loadRecommendation(userId: String, localDate: String): RecommendationResult {
        val budget = dailyBudgetDao.getByDate(userId, localDate)
        val summary = mealEntryDao.getDailySummary(userId, localDate)
        val candidates = inventoryItemDao.getAvailable(userId)
            .filter { entity ->
                InventoryExpiryEvaluator.evaluate(entity.purchaseDate, entity.expiryDate).status !=
                    InventoryExpiryStatus.EXPIRED
            }
            .mapNotNull { entity -> toCandidate(userId, entity) }
        return NutritionRecommendationEngine.compute(
            budgetKcal = budget?.budgetKcal ?: 0,
            consumedKcal = summary.consumedKcal,
            consumedProtein = summary.consumedProtein,
            consumedCarb = summary.consumedCarb,
            consumedFat = summary.consumedFat,
            targetProtein = budget?.proteinG ?: 0.0,
            targetCarb = budget?.carbG ?: 0.0,
            targetFat = budget?.fatG ?: 0.0,
            candidates = candidates,
        )
    }

    override suspend fun buildMealBatchItems(
        userId: String,
        comboIndex: Int,
        result: RecommendationResult,
    ): List<RecommendationMealBatchItem> {
        val combo = result.combos.getOrNull(comboIndex) ?: return emptyList()
        return combo.items.mapNotNull { item ->
            val entity = inventoryItemDao.getById(item.candidate.inventoryItemId) ?: return@mapNotNull null
            val food = toFoodSearchItem(userId, entity) ?: return@mapNotNull null
            RecommendationMealBatchItem(
                inventoryItemId = item.candidate.inventoryItemId,
                food = food,
                quantity = item.portionBasis,
                unit = MealUnit.G,
                servingGrams = food.servingGrams,
                inventoryDeductBasis = item.portionBasis,
            )
        }
    }

    private suspend fun toCandidate(userId: String, entity: InventoryItemEntity): RecommendationCandidate? {
        val resolved = resolveNutrition(userId, entity) ?: return null
        val expiry = InventoryExpiryEvaluator.evaluate(entity.purchaseDate, entity.expiryDate)
        return RecommendationCandidate(
            inventoryItemId = entity.id,
            inventoryName = entity.name,
            foodId = resolved.foodId,
            foodName = resolved.name,
            remainingAmount = entity.remainingAmount,
            inventoryUnit = entity.unit,
            basisUnit = resolved.basisUnit,
            kcalPer100 = resolved.kcalPer100,
            proteinPer100 = resolved.proteinPer100,
            carbPer100 = resolved.carbPer100,
            fatPer100 = resolved.fatPer100,
            expiryStatus = expiry.status,
            itemScore = 0.0,
        )
    }

    private data class ResolvedNutrition(
        val foodId: String?,
        val publicFoodId: String?,
        val name: String,
        val brand: String?,
        val basisUnit: String,
        val kcalPer100: Double,
        val proteinPer100: Double,
        val carbPer100: Double,
        val fatPer100: Double,
        val servingName: String?,
        val servingGrams: Double?,
        val source: FoodSource,
        val externalId: String?,
        val dataIncomplete: Boolean,
    )

    private suspend fun resolveNutrition(userId: String, entity: InventoryItemEntity): ResolvedNutrition? {
        bindingDao.findByInventoryItem(userId, entity.id)?.foodId?.let { foodDao.getById(it) }?.let { return it.toResolved() }
        foodDao.findCustomByName(userId, entity.nameNormalized)?.let { return it.toResolved() }
        publicFoodDao.searchByName(entity.name, limit = 1).firstOrNull()?.let { return it.toResolved() }
        return null
    }

    private suspend fun toFoodSearchItem(userId: String, entity: InventoryItemEntity): FoodSearchItem? {
        val resolved = resolveNutrition(userId, entity) ?: return null
        return FoodSearchItem(
            foodId = resolved.foodId,
            publicFoodId = resolved.publicFoodId,
            externalId = resolved.externalId,
            name = resolved.name,
            brand = resolved.brand,
            kcalPer100 = resolved.kcalPer100,
            proteinPer100 = resolved.proteinPer100,
            carbPer100 = resolved.carbPer100,
            fatPer100 = resolved.fatPer100,
            basisUnit = BasisUnit.valueOf(resolved.basisUnit),
            servingName = resolved.servingName,
            servingGrams = resolved.servingGrams,
            source = resolved.source,
            dataIncomplete = resolved.dataIncomplete,
            nutritionWarning = false,
            lastUsedAt = null,
            lastQuantity = null,
            lastUnit = null,
        )
    }

    private fun FoodEntity.toResolved() = ResolvedNutrition(
        foodId = id,
        publicFoodId = null,
        name = name,
        brand = brand,
        basisUnit = basisUnit,
        kcalPer100 = kcalPer100,
        proteinPer100 = proteinPer100 ?: 0.0,
        carbPer100 = carbPer100 ?: 0.0,
        fatPer100 = fatPer100 ?: 0.0,
        servingName = servingName,
        servingGrams = servingGrams,
        source = FoodSource.valueOf(source),
        externalId = externalId,
        dataIncomplete = dataIncomplete,
    )

    private fun PublicFoodEntity.toResolved() = ResolvedNutrition(
        foodId = null,
        publicFoodId = id,
        name = name,
        brand = brand,
        basisUnit = basisUnit,
        kcalPer100 = kcalPer100,
        proteinPer100 = proteinPer100 ?: 0.0,
        carbPer100 = carbPer100 ?: 0.0,
        fatPer100 = fatPer100 ?: 0.0,
        servingName = servingName,
        servingGrams = servingGrams,
        source = FoodSource.PUBLIC,
        externalId = externalId,
        dataIncomplete = dataIncomplete,
    )
}
