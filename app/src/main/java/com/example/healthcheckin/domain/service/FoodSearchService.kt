package com.example.healthcheckin.domain.service

import com.example.healthcheckin.domain.model.FoodSearchItem
import com.example.healthcheckin.domain.model.FoodSearchResult
import com.example.healthcheckin.domain.model.RecentFrequentFoods
import com.example.healthcheckin.domain.model.RemoteFetchResult

interface FoodSearchService {
    suspend fun searchLocal(query: String, userId: String): FoodSearchResult
    suspend fun fetchRemote(
        query: String,
        userId: String,
        existingItems: List<FoodSearchItem>,
    ): RemoteFetchResult
    suspend fun getRecentAndFrequentFoods(userId: String): RecentFrequentFoods
}
