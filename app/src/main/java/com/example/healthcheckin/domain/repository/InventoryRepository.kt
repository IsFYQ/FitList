package com.example.healthcheckin.domain.repository

import com.example.healthcheckin.domain.model.InventoryDeductChoice
import com.example.healthcheckin.domain.model.InventoryDeductPreview
import com.example.healthcheckin.domain.model.InventoryItem
import com.example.healthcheckin.domain.model.InventoryMatchResult
import com.example.healthcheckin.domain.model.SaveInventoryRequest
import com.example.healthcheckin.domain.model.UpdateInventoryRequest
import com.example.healthcheckin.util.BasisUnit
import kotlinx.coroutines.flow.Flow

interface InventoryRepository {
    fun observeItems(userId: String): Flow<List<InventoryItem>>
    suspend fun getById(itemId: String): InventoryItem?
    suspend fun create(userId: String, request: SaveInventoryRequest): Result<InventoryItem>
    suspend fun update(userId: String, request: UpdateInventoryRequest): Result<InventoryItem>
    suspend fun markUsedUp(itemId: String): Result<InventoryItem>
    suspend fun delete(itemId: String): Result<Pair<InventoryItem, Int>>
    suspend fun suggestNames(userId: String, prefix: String): List<String>
    suspend fun matchForFood(
        userId: String,
        foodId: String?,
        foodName: String,
        foodBasisUnit: BasisUnit,
        mealBasisAmount: Double,
    ): InventoryMatchResult
    suspend fun previewDeduct(
        itemId: String,
        mealBasisAmount: Double,
        foodBasisUnit: BasisUnit,
    ): InventoryDeductPreview?
    suspend fun applyDeduct(
        userId: String,
        itemId: String,
        mealEntryId: String,
        mealBasisAmount: Double,
        foodBasisUnit: BasisUnit,
        choice: InventoryDeductChoice,
    ): Result<Double>
    suspend fun revertDeduct(userId: String, mealEntryId: String, itemId: String, amount: Double): Result<Unit>
}
