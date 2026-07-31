package com.example.healthcheckin.domain.repository

import com.example.healthcheckin.data.local.entity.GoalEntity
import com.example.healthcheckin.domain.model.GoalSaveRequest
import com.example.healthcheckin.domain.model.GoalSaveResult
import com.example.healthcheckin.domain.model.OnboardingFormData
import kotlinx.coroutines.flow.Flow

interface GoalRepository {
    fun observeActiveGoal(userId: String): Flow<GoalEntity?>
    suspend fun getActiveGoal(userId: String): GoalEntity?
    suspend fun loadFormFromProfile(userId: String): OnboardingFormData?
    suspend fun saveGoal(userId: String, request: GoalSaveRequest): Result<GoalSaveResult>
    suspend fun recordWeight(userId: String, weightKg: Double): Result<Unit>
    suspend fun hasCompletedOnboarding(userId: String): Boolean
    suspend fun shouldPromptAgeUpdate(userId: String): Boolean
}
