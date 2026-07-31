package com.example.healthcheckin.domain.repository

import com.example.healthcheckin.data.local.entity.MealEntryEntity
import com.example.healthcheckin.domain.model.AddMealRequest
import com.example.healthcheckin.domain.model.UpdateMealRequest

interface MealRepository {
    suspend fun addMeal(userId: String, request: AddMealRequest): Result<MealEntryEntity>
    suspend fun updateMeal(userId: String, request: UpdateMealRequest): Result<MealEntryEntity>
    suspend fun deleteMeal(entryId: String): Result<Unit>
    suspend fun getMealById(entryId: String): MealEntryEntity?
    suspend fun undoDelete(entryId: String): Result<Unit>
}
