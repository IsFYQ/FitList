package com.example.healthcheckin.domain.repository

import com.example.healthcheckin.domain.algorithm.RecommendationResult
import com.example.healthcheckin.domain.model.RecommendationMealBatchItem

interface RecommendationRepository {
    suspend fun loadRecommendation(userId: String, localDate: String): RecommendationResult
    suspend fun buildMealBatchItems(
        userId: String,
        comboIndex: Int,
        result: RecommendationResult,
    ): List<RecommendationMealBatchItem>
}
