package com.example.healthcheckin.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.healthcheckin.data.local.entity.InventoryLedgerEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface InventoryLedgerDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: InventoryLedgerEntity)

    @Query(
        """
        SELECT * FROM inventory_ledger
        WHERE inventoryItemId = :itemId AND deletedAt IS NULL
        ORDER BY createdAt DESC
        LIMIT :limit
        """,
    )
    fun observeRecentForItem(itemId: String, limit: Int = 20): Flow<List<InventoryLedgerEntity>>

    @Query(
        """
        SELECT * FROM inventory_ledger
        WHERE inventoryItemId = :itemId AND deletedAt IS NULL AND changeType = 'MEAL_DEDUCT'
        ORDER BY createdAt DESC
        LIMIT 1
        """,
    )
    suspend fun latestDeduct(itemId: String): InventoryLedgerEntity?

    @Query("SELECT * FROM inventory_ledger WHERE id = :id LIMIT 1")
    suspend fun getByIdRaw(id: String): InventoryLedgerEntity?

    @Query("UPDATE inventory_ledger SET syncState = :syncState, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateSyncState(id: String, syncState: String, updatedAt: Long)

    @Query("SELECT COUNT(*) FROM inventory_ledger WHERE refMealEntryId = :mealEntryId AND changeType = 'MEAL_DEDUCT' AND deletedAt IS NULL")
    suspend fun countDeductsForMeal(mealEntryId: String): Int

    @Query("DELETE FROM inventory_ledger WHERE userId = :userId")
    suspend fun deleteAllForUser(userId: String)

    @Query("SELECT COUNT(*) FROM inventory_ledger WHERE userId = :userId AND deletedAt IS NULL")
    suspend fun countActiveForUser(userId: String): Int

    @Query(
        """
        SELECT * FROM inventory_ledger
        WHERE userId = :userId AND deletedAt IS NULL
        ORDER BY id ASC
        LIMIT :limit OFFSET :offset
        """,
    )
    suspend fun getActivePageForUser(userId: String, limit: Int, offset: Int): List<InventoryLedgerEntity>
}
