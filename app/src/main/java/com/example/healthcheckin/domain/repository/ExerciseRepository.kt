package com.example.healthcheckin.domain.repository

import com.example.healthcheckin.domain.model.ExerciseRecordItem
import com.example.healthcheckin.domain.model.ExerciseWeekSummary
import com.example.healthcheckin.domain.model.SaveExerciseRequest
import kotlinx.coroutines.flow.Flow

interface ExerciseRepository {
    fun observeRecords(userId: String): Flow<List<ExerciseRecordItem>>
    suspend fun save(userId: String, request: SaveExerciseRequest): Result<ExerciseRecordItem>
    suspend fun delete(recordId: String): Result<Unit>
    suspend fun getWeekSummary(userId: String): ExerciseWeekSummary
}
