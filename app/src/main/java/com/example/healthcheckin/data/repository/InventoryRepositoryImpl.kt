package com.example.healthcheckin.data.repository

import androidx.room.withTransaction
import com.example.healthcheckin.data.local.HealthDatabase
import com.example.healthcheckin.data.local.dao.IngredientAliasDao
import com.example.healthcheckin.data.local.dao.IngredientBindingDao
import com.example.healthcheckin.data.local.dao.InventoryItemDao
import com.example.healthcheckin.data.local.dao.InventoryLedgerDao
import com.example.healthcheckin.data.local.dao.SyncQueueDao
import com.example.healthcheckin.data.local.entity.InventoryItemEntity
import com.example.healthcheckin.data.local.entity.InventoryLedgerEntity
import com.example.healthcheckin.data.local.entity.SyncQueueEntity
import com.example.healthcheckin.domain.algorithm.InventoryExpiryEvaluator
import com.example.healthcheckin.domain.algorithm.InventoryUnitConverter
import com.example.healthcheckin.domain.model.InventoryDeductChoice
import com.example.healthcheckin.domain.model.InventoryDeductPreview
import com.example.healthcheckin.domain.model.InventoryItem
import com.example.healthcheckin.domain.model.InventoryMatchResult
import com.example.healthcheckin.domain.model.SaveInventoryRequest
import com.example.healthcheckin.domain.model.UpdateInventoryRequest
import com.example.healthcheckin.domain.repository.InventoryRepository
import com.example.healthcheckin.util.BasisUnit
import com.example.healthcheckin.util.DateTimeUtil
import com.example.healthcheckin.util.InventoryChangeType
import com.example.healthcheckin.util.InventoryDeductResolution
import com.example.healthcheckin.util.InventoryMatchLevel
import com.example.healthcheckin.util.InventoryUnit
import com.example.healthcheckin.util.PrecisionUtil
import com.example.healthcheckin.util.SyncState
import com.example.healthcheckin.util.UuidV7
import com.example.healthcheckin.util.Validators
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InventoryRepositoryImpl @Inject constructor(
    private val database: HealthDatabase,
    private val itemDao: InventoryItemDao,
    private val ledgerDao: InventoryLedgerDao,
    private val bindingDao: IngredientBindingDao,
    private val aliasDao: IngredientAliasDao,
    private val syncQueueDao: SyncQueueDao,
    private val deviceId: String,
) : InventoryRepository {
    override fun observeItems(userId: String): Flow<List<InventoryItem>> = itemDao.observeAll(userId).map { it.map { item -> item.toItem() } }
    override suspend fun getById(itemId: String): InventoryItem? = itemDao.getById(itemId)?.toItem()

    override suspend fun create(userId: String, request: SaveInventoryRequest): Result<InventoryItem> = runCatching {
        validate(request.name, request.amount)
        val now = DateTimeUtil.nowEpochMillis()
        val entity = InventoryItemEntity(UuidV7.generate(), userId, request.name.trim(), Validators.normalizeFoodName(request.name),
            aliasDao.findByAlias(Validators.normalizeFoodName(request.name))?.ingredientKey, request.category.name,
            PrecisionUtil.roundStorage(request.amount), PrecisionUtil.roundStorage(request.amount), request.unit.name, request.pieceGrams,
            request.purchaseDate, request.expiryDate, request.unitPrice, deviceId = deviceId, createdAt = now, updatedAt = now, syncState = SyncState.PENDING)
        database.withTransaction {
            itemDao.insert(entity); ledger(entity, InventoryChangeType.CREATE, entity.remainingAmount, null, now)
            enqueue("inventory_items", entity.id, now)
        }
        entity.toItem()
    }

    override suspend fun update(userId: String, request: UpdateInventoryRequest): Result<InventoryItem> = runCatching {
        val current = itemDao.getById(request.itemId) ?: error("Inventory item not found")
        require(current.userId == userId); validate(request.name, request.remainingAmount)
        val now = DateTimeUtil.nowEpochMillis()
        val remaining = PrecisionUtil.roundStorage(request.remainingAmount)
        val updated = current.copy(name = request.name.trim(), nameNormalized = Validators.normalizeFoodName(request.name),
            ingredientKey = aliasDao.findByAlias(Validators.normalizeFoodName(request.name))?.ingredientKey, category = request.category.name,
            remainingAmount = remaining, pieceGrams = request.pieceGrams, purchaseDate = request.purchaseDate, expiryDate = request.expiryDate,
            unitPrice = request.unitPrice, version = current.version + 1, updatedAt = now, syncState = SyncState.PENDING)
        database.withTransaction {
            itemDao.update(updated)
            if (remaining != current.remainingAmount) ledger(updated, InventoryChangeType.MANUAL_ADJUST, remaining - current.remainingAmount, null, now)
            enqueue("inventory_items", updated.id, now)
        }
        updated.toItem()
    }

    override suspend fun markUsedUp(itemId: String): Result<InventoryItem> = runCatching {
        val item = itemDao.getById(itemId) ?: error("Inventory item not found"); val now = DateTimeUtil.nowEpochMillis()
        val updated = item.copy(remainingAmount = 0.0, version = item.version + 1, updatedAt = now, syncState = SyncState.PENDING)
        database.withTransaction { itemDao.update(updated); ledger(updated, InventoryChangeType.DISCARD, -item.remainingAmount, null, now); enqueue("inventory_items", itemId, now) }
        updated.toItem()
    }

    override suspend fun delete(itemId: String): Result<Pair<InventoryItem, Int>> = runCatching {
        val item = itemDao.getById(itemId) ?: error("Inventory item not found"); val now = DateTimeUtil.nowEpochMillis()
        database.withTransaction { itemDao.softDelete(itemId, now, now, SyncState.PENDING); bindingDao.softDeleteByInventoryItem(itemId, now, now, SyncState.PENDING); enqueue("inventory_items", itemId, now) }
        item.toItem() to 0
    }
    override suspend fun suggestNames(userId: String, prefix: String) = itemDao.suggestNames(userId, Validators.normalizeFoodName(prefix))

    override suspend fun matchForFood(userId: String, foodId: String?, foodName: String, foodBasisUnit: BasisUnit, mealBasisAmount: Double): InventoryMatchResult {
        val binding = foodId?.let { bindingDao.findByFood(userId, it) }
        val bound = binding?.let { itemDao.getById(it.inventoryItemId)?.takeIf { item -> item.deletedAt == null } }
        if (bound != null && InventoryUnitConverter.dimensionsCompatible(foodBasisUnit.name, bound.unit)) {
            return match(InventoryMatchLevel.L1, bound)
        }
        val normalized = Validators.normalizeFoodName(foodName)
        val foodKey = aliasDao.findByAlias(normalized)?.ingredientKey
        val available = itemDao.getAvailable(userId)
            .filter { InventoryUnitConverter.dimensionsCompatible(foodBasisUnit.name, it.unit) }
        if (foodKey != null) {
            val keyed = available.firstOrNull {
                it.ingredientKey == foodKey ||
                    aliasDao.findByAlias(it.nameNormalized)?.ingredientKey == foodKey
            }
            if (keyed != null) return match(InventoryMatchLevel.L2, keyed)
        }
        val named = available.firstOrNull { inv ->
            val a = inv.nameNormalized
            val b = normalized
            val shorter = minOf(a.length, b.length)
            shorter >= 2 && (a.contains(b) || b.contains(a))
        }
        return named?.let { match(InventoryMatchLevel.L3, it) }
            ?: InventoryMatchResult(InventoryMatchLevel.NONE, 0.0, null, null)
    }
    override suspend fun previewDeduct(itemId: String, mealBasisAmount: Double, foodBasisUnit: BasisUnit): InventoryDeductPreview? {
        val item = itemDao.getById(itemId) ?: return null
        if (!InventoryUnitConverter.dimensionsCompatible(foodBasisUnit.name, item.unit)) return null
        val amount = PrecisionUtil.roundStorage(InventoryUnitConverter.fromBasis(mealBasisAmount, item.unit, item.pieceGrams))
        return InventoryDeductPreview(match(InventoryMatchLevel.L1, item), mealBasisAmount, InventoryUnit.valueOf(item.unit), amount, (item.remainingAmount - amount).coerceAtLeast(0.0), amount > item.remainingAmount)
    }
    override suspend fun applyDeduct(userId: String, itemId: String, mealEntryId: String, mealBasisAmount: Double, foodBasisUnit: BasisUnit, choice: InventoryDeductChoice): Result<Double> = runCatching {
        if (choice.resolution == InventoryDeductResolution.SKIP) return@runCatching 0.0
        repeat(3) {
            val item = itemDao.getById(itemId) ?: error("Inventory item not found")
            require(item.userId == userId && InventoryUnitConverter.dimensionsCompatible(foodBasisUnit.name, item.unit))
            val requested = choice.manualAmount ?: InventoryUnitConverter.fromBasis(mealBasisAmount, item.unit, item.pieceGrams)
            val deduct = PrecisionUtil.roundStorage(if (choice.resolution == InventoryDeductResolution.DEDUCT_REMAINING) requested.coerceAtMost(item.remainingAmount) else requested)
            require(deduct >= 0 && deduct <= item.remainingAmount)
            val now = DateTimeUtil.nowEpochMillis(); val balance = PrecisionUtil.roundStorage(item.remainingAmount - deduct)
            if (itemDao.updateRemainingWithVersion(item.id, balance, item.version, item.version + 1, now, SyncState.PENDING) == 1) {
                database.withTransaction { ledger(item.copy(remainingAmount = balance), InventoryChangeType.MEAL_DEDUCT, -deduct, mealEntryId, now); enqueue("inventory_items", item.id, now) }
                return@runCatching deduct
            }
        }; error("Inventory changed concurrently")
    }
    override suspend fun revertDeduct(userId: String, mealEntryId: String, itemId: String, amount: Double): Result<Unit> = runCatching {
        val item = itemDao.getById(itemId) ?: error("Inventory item not found"); require(item.userId == userId); val now = DateTimeUtil.nowEpochMillis()
        val updated = item.copy(remainingAmount = PrecisionUtil.roundStorage(item.remainingAmount + amount), version = item.version + 1, updatedAt = now, syncState = SyncState.PENDING)
        database.withTransaction { itemDao.update(updated); ledger(updated, InventoryChangeType.MEAL_REVERT, amount, mealEntryId, now); enqueue("inventory_items", itemId, now) }
    }
    private fun match(level: InventoryMatchLevel, item: InventoryItemEntity) = InventoryMatchResult(
        level,
        when (level) {
            InventoryMatchLevel.L1 -> 1.0
            InventoryMatchLevel.L2 -> 0.90
            InventoryMatchLevel.L3 -> 0.60
            InventoryMatchLevel.NONE -> 0.0
        },
        item.toItem(),
        item.name,
    )
    private fun validate(name: String, amount: Double) { require(name.trim().isNotEmpty()); require(amount >= 0) }
    private suspend fun ledger(item: InventoryItemEntity, type: InventoryChangeType, delta: Double, mealId: String?, now: Long) {
        val ledgerId = UuidV7.generate()
        ledgerDao.insert(InventoryLedgerEntity(ledgerId, item.userId, item.id, type.name, PrecisionUtil.roundStorage(delta), item.remainingAmount, mealId, deviceId = deviceId, createdAt = now, updatedAt = now, syncState = SyncState.PENDING))
        enqueue("inventory_ledger", ledgerId, now)
    }
    private suspend fun enqueue(table: String, id: String, now: Long) = syncQueueDao.insert(SyncQueueEntity(id = UuidV7.generate(), tableName = table, rowId = id, operation = "UPSERT", createdAt = now, updatedAt = now))
    private fun InventoryItemEntity.toItem(): InventoryItem {
        val expiry = InventoryExpiryEvaluator.evaluate(purchaseDate, expiryDate)
        val deductable = remainingAmount > 0 && (unit != InventoryUnit.PIECE.name || (pieceGrams != null && pieceGrams > 0))
        return InventoryItem(
            id, name, com.example.healthcheckin.util.InventoryCategory.valueOf(category),
            remainingAmount, initialAmount, InventoryUnit.valueOf(unit), pieceGrams,
            purchaseDate, expiryDate, unitPrice, ingredientKey, expiry.status, expiry.daysStored,
            expiry.daysLeft, expiry.label, deductable, null,
        )
    }
}
