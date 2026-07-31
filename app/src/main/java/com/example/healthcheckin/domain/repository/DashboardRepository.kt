package com.example.healthcheckin.domain.repository

import com.example.healthcheckin.data.local.entity.MealEntryEntity
import com.example.healthcheckin.domain.model.DashboardData
import com.example.healthcheckin.domain.model.HealthWarning
import com.example.healthcheckin.domain.model.HealthWarningType
import com.example.healthcheckin.domain.model.SyncBadge
import kotlinx.coroutines.flow.Flow

interface DashboardRepository {
    fun observeDashboard(userId: String, localDate: String): Flow<DashboardData>
    suspend fun ensureTodayBudget(userId: String)
    suspend fun deleteMealEntry(entry: MealEntryEntity): Result<Unit>
    suspend fun undoDeleteMealEntry(entry: MealEntryEntity): Result<Unit>
    suspend fun evaluateHealthWarning(userId: String, todayEntryCount: Int): HealthWarning?
    fun observeSyncBadge(): Flow<SyncBadge>
    suspend fun triggerManualBackup()
    suspend fun dismissHealthWarning(type: HealthWarningType)
    fun getMinViewDate(registeredLocalDate: String): String
    fun isDeviceTimeSuspicious(): Boolean
}
