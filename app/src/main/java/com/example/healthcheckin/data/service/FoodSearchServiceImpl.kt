package com.example.healthcheckin.data.service

import com.example.healthcheckin.data.local.dao.FoodDao
import com.example.healthcheckin.data.local.dao.MealEntryDao
import com.example.healthcheckin.data.local.dao.PublicFoodDao
import com.example.healthcheckin.data.local.entity.FoodEntity
import com.example.healthcheckin.data.local.entity.PublicFoodEntity
import com.example.healthcheckin.data.remote.FoodSearchApi
import com.example.healthcheckin.data.remote.FoodSearchRemoteItemDto
import com.example.healthcheckin.domain.algorithm.FoodSearchMerger
import com.example.healthcheckin.domain.algorithm.FoodSearchScorer
import com.example.healthcheckin.domain.model.FoodSearchItem
import com.example.healthcheckin.domain.model.FoodSearchResult
import com.example.healthcheckin.domain.model.RecentFrequentFoods
import com.example.healthcheckin.domain.model.RemoteFetchResult
import com.example.healthcheckin.domain.model.SearchBanner
import com.example.healthcheckin.domain.service.FoodSearchService
import com.example.healthcheckin.util.BasisUnit
import com.example.healthcheckin.util.DateTimeUtil
import com.example.healthcheckin.util.FoodSource
import com.example.healthcheckin.util.MealSlot
import com.example.healthcheckin.util.MealUnit
import com.example.healthcheckin.util.NetworkMonitor
import com.example.healthcheckin.util.Validators
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FoodSearchServiceImpl @Inject constructor(
    private val foodDao: FoodDao,
    private val mealEntryDao: MealEntryDao,
    private val publicFoodDao: PublicFoodDao,
    private val foodSearchApi: FoodSearchApi,
    private val cacheStore: FoodSearchCacheStore,
    private val networkMonitor: NetworkMonitor,
) : FoodSearchService {

    override suspend fun searchLocal(query: String, userId: String): FoodSearchResult {
        val normalized = Validators.normalizeFoodName(query)
        if (normalized.isEmpty()) {
            return FoodSearchResult(emptyList())
        }

        val localFoods = foodDao.searchByName(userId, normalized, limit = 30)
        val publicFoods = publicFoodDao.searchByName(normalized, limit = 20)
        val items = buildLocalItems(query, localFoods, publicFoods)

        val banner = when {
            !networkMonitor.isOnline() -> {
                if (normalized.length >= 2) SearchBanner.OFFLINE else SearchBanner.NONE
            }
            normalized.length >= 2 -> SearchBanner.REMOTE_LOADING
            else -> SearchBanner.NONE
        }

        if (!networkMonitor.isOnline() && normalized.length >= 2) {
            val cached = loadCachedRemote(query, userId)
            if (cached.isNotEmpty()) {
                return FoodSearchResult(
                    items = FoodSearchMerger.appendRemote(items, cached),
                    banner = SearchBanner.FROM_CACHE,
                )
            }
        }

        return FoodSearchResult(items = items, banner = banner)
    }

    override suspend fun fetchRemote(
        query: String,
        userId: String,
        existingItems: List<FoodSearchItem>,
    ): RemoteFetchResult {
        val normalized = Validators.normalizeFoodName(query)
        if (normalized.length < 2) {
            return RemoteFetchResult(emptyList())
        }
        if (!networkMonitor.isOnline()) {
            return RemoteFetchResult(emptyList(), SearchBanner.OFFLINE)
        }

        return try {
            val response = foodSearchApi.search(query = query.trim(), pageSize = 20)
            val remoteItems = response.items
                .map { it.toSearchItem() }
                .map { item ->
                    val useCount = item.foodId?.let { foodDao.getById(it)?.useCount30d } ?: 0
                    FoodSearchScorer.withScore(query, item, useCount)
                }
            cacheStore.save(query, response.items, response.quotaRemaining)

            val banner = when {
                remoteItems.isEmpty() -> resolveRemoteBanner(response.sources, response.quotaRemaining)
                response.quotaRemaining == 0 -> SearchBanner.QUOTA_EXHAUSTED
                else -> SearchBanner.NONE
            }
            RemoteFetchResult(
                appendedItems = FoodSearchMerger.appendRemote(existingItems, remoteItems)
                    .drop(existingItems.size),
                banner = banner,
            )
        } catch (e: HttpException) {
            handleRemoteError(query, userId, existingItems, e)
        } catch (_: IOException) {
            handleRemoteError(query, userId, existingItems, null)
        }
    }

    override suspend fun getRecentAndFrequentFoods(userId: String): RecentFrequentFoods {
        val sectionLimit = 8
        val sinceDate = DateTimeUtil.toLocalDateString(
            DateTimeUtil.nowEpochMillis() - THIRTY_DAYS_MS,
        )

        val frequentIds = mealEntryDao
            .getFrequentFoodCounts(userId, sinceDate, minCount = 3, limit = sectionLimit)
            .map { it.foodId }
            .toSet()

        val recentIds = mealEntryDao
            .getRecentFoodIds(userId, limit = sectionLimit + frequentIds.size)
            .filter { it !in frequentIds }
            .take(sectionLimit)

        val allIds = (recentIds + frequentIds).distinct()
        if (allIds.isEmpty()) return RecentFrequentFoods()

        val foodMap = foodDao.getByIds(allIds).associateBy { it.id }
        val recent = recentIds.mapNotNull { foodMap[it]?.toSearchItem() }
        val frequent = mealEntryDao
            .getFrequentFoodCounts(userId, sinceDate, minCount = 3, limit = sectionLimit)
            .mapNotNull { foodMap[it.foodId]?.toSearchItem() }

        return RecentFrequentFoods(recent = recent, frequent = frequent)
    }

    private companion object {
        const val THIRTY_DAYS_MS = 30L * 24 * 60 * 60 * 1000
    }

    private suspend fun handleRemoteError(
        query: String,
        userId: String,
        existingItems: List<FoodSearchItem>,
        httpException: HttpException?,
    ): RemoteFetchResult {
        val cached = loadCachedRemote(query, userId)
        val banner = when (httpException?.code()) {
            429 -> SearchBanner.QUOTA_EXHAUSTED
            502 -> SearchBanner.REMOTE_UNAVAILABLE
            else -> if (cached.isEmpty()) SearchBanner.REMOTE_UNAVAILABLE else SearchBanner.FROM_CACHE
        }
        if (cached.isEmpty()) {
            return RemoteFetchResult(emptyList(), banner)
        }
        return RemoteFetchResult(
            appendedItems = FoodSearchMerger.appendRemote(existingItems, cached).drop(existingItems.size),
            banner = if (banner == SearchBanner.QUOTA_EXHAUSTED) SearchBanner.QUOTA_EXHAUSTED else SearchBanner.FROM_CACHE,
        )
    }

    private suspend fun loadCachedRemote(query: String, userId: String): List<FoodSearchItem> {
        val cached = cacheStore.get(query) ?: return emptyList()
        return cached.map { it.toSearchItem() }
            .map { item ->
                val useCount = item.foodId?.let { foodDao.getById(it)?.useCount30d } ?: 0
                FoodSearchScorer.withScore(query, item, useCount)
            }
    }

    private fun buildLocalItems(
        query: String,
        localFoods: List<FoodEntity>,
        publicFoods: List<PublicFoodEntity>,
    ): List<FoodSearchItem> {
        val localItems = localFoods.map { it.toSearchItem() }
        val localKeys = localItems.map { FoodSearchMerger.dedupeKey(it) }.toSet()
        val publicItems = publicFoods
            .filter { FoodSearchMerger.dedupeKey(it.name, it.brand) !in localKeys }
            .map { it.toSearchItem() }
        val merged = (localItems + publicItems).map { item ->
            val useCount = when {
                item.foodId != null -> localFoods.find { it.id == item.foodId }?.useCount30d ?: 0
                else -> 0
            }
            FoodSearchScorer.withScore(query, item, useCount)
        }
        return FoodSearchMerger.mergeByScore(query, merged)
    }

    private fun resolveRemoteBanner(
        sources: com.example.healthcheckin.data.remote.FoodSearchSourcesDto?,
        quotaRemaining: Int?,
    ): SearchBanner {
        if (quotaRemaining == 0) return SearchBanner.QUOTA_EXHAUSTED
        val fatsecretStatus = sources?.fatsecret?.status
        val offStatus = sources?.off?.status
        val allFailed = listOfNotNull(fatsecretStatus, offStatus).all { it == "FAILED" || it == "TIMEOUT" }
        if (allFailed && fatsecretStatus != null && offStatus != null) {
            return SearchBanner.REMOTE_UNAVAILABLE
        }
        if (fatsecretStatus == "TIMEOUT" || offStatus == "TIMEOUT") {
            return SearchBanner.REMOTE_TIMEOUT
        }
        return SearchBanner.NONE
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
        source = runCatching { FoodSource.valueOf(source) }.getOrDefault(FoodSource.CUSTOM),
        dataIncomplete = dataIncomplete,
        nutritionWarning = nutritionWarning,
        lastUsedAt = lastUsedAt,
        lastQuantity = lastQuantity,
        lastUnit = lastUnit?.let { runCatching { MealUnit.valueOf(it) }.getOrNull() },
        lastMealSlot = lastMealSlot?.let { runCatching { MealSlot.valueOf(it) }.getOrNull() },
    )

    private fun PublicFoodEntity.toSearchItem() = FoodSearchItem(
        foodId = null,
        publicFoodId = id,
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
        source = FoodSource.PUBLIC,
        dataIncomplete = dataIncomplete,
        nutritionWarning = nutritionWarning,
        lastUsedAt = null,
        lastQuantity = null,
        lastUnit = null,
    )

    private fun FoodSearchRemoteItemDto.toSearchItem() = FoodSearchItem(
        foodId = null,
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
        source = runCatching { FoodSource.valueOf(source) }.getOrDefault(FoodSource.OFF),
        dataIncomplete = dataIncomplete,
        nutritionWarning = false,
        lastUsedAt = null,
        lastQuantity = null,
        lastUnit = null,
        barcode = barcode,
    )
}
