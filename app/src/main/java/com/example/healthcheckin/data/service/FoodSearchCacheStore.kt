package com.example.healthcheckin.data.service

import com.example.healthcheckin.data.local.dao.FoodSearchCacheDao
import com.example.healthcheckin.data.local.entity.FoodSearchCacheEntity
import com.example.healthcheckin.data.remote.FoodSearchCachePayloadDto
import com.example.healthcheckin.data.remote.FoodSearchRemoteItemDto
import com.example.healthcheckin.util.DateTimeUtil
import com.example.healthcheckin.util.Validators
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FoodSearchCacheStore @Inject constructor(
    private val cacheDao: FoodSearchCacheDao,
    private val json: Json,
) {
    private val ttlMs = 24L * 60 * 60 * 1000

    suspend fun get(query: String): List<FoodSearchRemoteItemDto>? {
        val normalized = Validators.normalizeFoodName(query)
        val entry = cacheDao.getByQuery(normalized) ?: return null
        if (entry.expiresAt < DateTimeUtil.nowEpochMillis()) {
            return null
        }
        return runCatching {
            json.decodeFromString<FoodSearchCachePayloadDto>(entry.payloadJson).items
        }.getOrNull()
    }

    suspend fun save(query: String, items: List<FoodSearchRemoteItemDto>, quotaRemaining: Int?) {
        val normalized = Validators.normalizeFoodName(query)
        if (normalized.isEmpty() || items.isEmpty()) return
        val now = DateTimeUtil.nowEpochMillis()
        runCatching {
            cacheDao.upsert(
                FoodSearchCacheEntity(
                    queryNormalized = normalized,
                    payloadJson = json.encodeToString(
                        FoodSearchCachePayloadDto(items = items, quotaRemaining = quotaRemaining),
                    ),
                    fetchedAt = now,
                    expiresAt = now + ttlMs,
                )
            )
        }
    }
}
