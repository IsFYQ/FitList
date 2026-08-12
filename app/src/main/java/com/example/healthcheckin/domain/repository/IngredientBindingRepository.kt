package com.example.healthcheckin.domain.repository

import com.example.healthcheckin.domain.model.IngredientBindingItem
import kotlinx.coroutines.flow.Flow

interface IngredientBindingRepository {
    fun observeBindings(userId: String): Flow<List<IngredientBindingItem>>
    fun observeCount(userId: String): Flow<Int>
    suspend fun bind(userId: String, foodId: String, inventoryItemId: String): Result<IngredientBindingItem>
    suspend fun unbind(bindingId: String): Result<Unit>
    suspend fun resolveIngredientKey(name: String): String?
    suspend fun ensureAliasesSeeded()
}
