package com.example.healthcheckin.domain.repository

import com.example.healthcheckin.domain.model.SaveWeightRequest
import com.example.healthcheckin.domain.model.WeightRecordItem
import com.example.healthcheckin.domain.model.MilestoneAchievementEvent
import kotlinx.coroutines.flow.Flow

interface WeightRepository {
    fun observeAllRecords(userId: String): Flow<List<WeightRecordItem>>
    fun observeRecordsInRange(userId: String, startDate: String, endDate: String): Flow<List<WeightRecordItem>>
    suspend fun getByDate(userId: String, localDate: String): WeightRecordItem?
    suspend fun saveWeight(userId: String, request: SaveWeightRequest, overwrite: Boolean): Result<Pair<WeightRecordItem, List<MilestoneAchievementEvent>>>
    suspend fun updateWeight(recordId: String, weightKg: Double, note: String?): Result<Pair<WeightRecordItem, List<MilestoneAchievementEvent>>>
    suspend fun deleteWeight(recordId: String): Result<Unit>
}
