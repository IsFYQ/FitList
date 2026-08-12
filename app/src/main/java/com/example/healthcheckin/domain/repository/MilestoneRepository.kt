package com.example.healthcheckin.domain.repository

import com.example.healthcheckin.domain.model.MilestoneAchievementEvent
import com.example.healthcheckin.domain.model.MilestoneItem
import com.example.healthcheckin.domain.model.SaveMilestoneRequest
import kotlinx.coroutines.flow.Flow

interface MilestoneRepository {
    fun observeMilestones(userId: String): Flow<List<MilestoneItem>>
    suspend fun create(userId: String, request: SaveMilestoneRequest): Result<MilestoneItem>
    suspend fun update(userId: String, milestoneId: String, request: SaveMilestoneRequest): Result<MilestoneItem>
    suspend fun delete(milestoneId: String): Result<Unit>
    suspend fun reset(milestoneId: String): Result<MilestoneItem>
    suspend fun markShared(milestoneId: String): Result<Unit>
    suspend fun evaluateOnWeightRecorded(
        userId: String,
        weightKg: Double,
        localDate: String,
    ): List<MilestoneAchievementEvent>
    suspend fun countActive(userId: String): Int
}
