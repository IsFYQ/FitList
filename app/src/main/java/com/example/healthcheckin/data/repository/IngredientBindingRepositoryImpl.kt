package com.example.healthcheckin.data.repository

import android.content.Context
import com.example.healthcheckin.data.local.dao.IngredientAliasDao
import com.example.healthcheckin.data.local.dao.IngredientBindingDao
import com.example.healthcheckin.data.local.dao.InventoryItemDao
import com.example.healthcheckin.data.local.dao.FoodDao
import com.example.healthcheckin.data.local.dao.SyncQueueDao
import com.example.healthcheckin.data.local.entity.IngredientBindingEntity
import com.example.healthcheckin.data.local.entity.SyncQueueEntity
import com.example.healthcheckin.data.local.seed.IngredientAliasSeeder
import com.example.healthcheckin.domain.model.IngredientBindingItem
import com.example.healthcheckin.domain.repository.IngredientBindingRepository
import com.example.healthcheckin.util.DateTimeUtil
import com.example.healthcheckin.util.SyncState
import com.example.healthcheckin.util.UuidV7
import com.example.healthcheckin.util.Validators
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class IngredientBindingRepositoryImpl @Inject constructor(
    private val bindingDao: IngredientBindingDao,
    private val aliasDao: IngredientAliasDao,
    private val foodDao: FoodDao,
    private val inventoryItemDao: InventoryItemDao,
    private val syncQueueDao: SyncQueueDao,
    @ApplicationContext private val context: Context,
    private val deviceId: String,
) : IngredientBindingRepository {
    override fun observeBindings(userId: String): Flow<List<IngredientBindingItem>> = bindingDao.observeAll(userId).map { bindings ->
        bindings.mapNotNull { binding ->
            val food = foodDao.getById(binding.foodId) ?: return@mapNotNull null
            val inventory = inventoryItemDao.getById(binding.inventoryItemId) ?: return@mapNotNull null
            IngredientBindingItem(binding.id, binding.foodId, food.name, binding.inventoryItemId, inventory.name)
        }
    }
    override fun observeCount(userId: String): Flow<Int> = bindingDao.observeCount(userId)
    override suspend fun bind(userId: String, foodId: String, inventoryItemId: String): Result<IngredientBindingItem> = runCatching {
        val food = foodDao.getById(foodId) ?: error("Food not found")
        val inventory = inventoryItemDao.getById(inventoryItemId) ?: error("Inventory item not found")
        require(food.userId == userId && inventory.userId == userId)
        val now = DateTimeUtil.nowEpochMillis()
        val old = bindingDao.findByFood(userId, foodId)
        val binding = old?.copy(inventoryItemId = inventoryItemId, updatedAt = now, deletedAt = null, syncState = SyncState.PENDING)
            ?: IngredientBindingEntity(UuidV7.generate(), userId, foodId, inventoryItemId, deviceId, now, now, syncState = SyncState.PENDING)
        if (old == null) bindingDao.insert(binding) else bindingDao.update(binding)
        syncQueueDao.insert(SyncQueueEntity(id = UuidV7.generate(), tableName = "ingredient_bindings", rowId = binding.id, operation = "UPSERT", createdAt = now, updatedAt = now))
        IngredientBindingItem(binding.id, foodId, food.name, inventoryItemId, inventory.name)
    }
    override suspend fun unbind(bindingId: String): Result<Unit> = runCatching {
        val now = DateTimeUtil.nowEpochMillis()
        bindingDao.softDelete(bindingId, now, now, SyncState.PENDING)
        syncQueueDao.insert(SyncQueueEntity(id = UuidV7.generate(), tableName = "ingredient_bindings", rowId = bindingId, operation = "UPSERT", createdAt = now, updatedAt = now))
    }
    override suspend fun resolveIngredientKey(name: String): String? = aliasDao.findByAlias(Validators.normalizeFoodName(name))?.ingredientKey
    override suspend fun ensureAliasesSeeded() { IngredientAliasSeeder(context, aliasDao).seedIfNeeded() }
}
