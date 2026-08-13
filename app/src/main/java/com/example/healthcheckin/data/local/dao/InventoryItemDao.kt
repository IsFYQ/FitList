package com.example.healthcheckin.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.healthcheckin.data.local.entity.InventoryItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface InventoryItemDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: InventoryItemEntity)

    @Update
    suspend fun update(entity: InventoryItemEntity)

    @Query(
        """
        SELECT * FROM inventory_items
        WHERE userId = :userId AND deletedAt IS NULL
        ORDER BY category ASC, remainingAmount ASC, purchaseDate ASC, createdAt DESC
        """,
    )
    fun observeAll(userId: String): Flow<List<InventoryItemEntity>>

    @Query("SELECT * FROM inventory_items WHERE id = :id AND deletedAt IS NULL LIMIT 1")
    suspend fun getById(id: String): InventoryItemEntity?

    @Query("SELECT * FROM inventory_items WHERE id = :id LIMIT 1")
    suspend fun getByIdRaw(id: String): InventoryItemEntity?

    @Query(
        """
        SELECT * FROM inventory_items
        WHERE userId = :userId AND deletedAt IS NULL AND remainingAmount > 0
        ORDER BY purchaseDate ASC, createdAt ASC
        """,
    )
    suspend fun getAvailable(userId: String): List<InventoryItemEntity>

    @Query(
        """
        SELECT * FROM inventory_items
        WHERE userId = :userId AND deletedAt IS NULL AND ingredientKey = :ingredientKey AND remainingAmount > 0
        ORDER BY purchaseDate ASC, createdAt ASC
        LIMIT 1
        """,
    )
    suspend fun findByIngredientKey(userId: String, ingredientKey: String): InventoryItemEntity?

    @Query(
        """
        SELECT DISTINCT name FROM inventory_items
        WHERE userId = :userId AND nameNormalized LIKE :prefix || '%'
        ORDER BY updatedAt DESC
        LIMIT :limit
        """,
    )
    suspend fun suggestNames(userId: String, prefix: String, limit: Int = 5): List<String>

    @Query(
        """
        UPDATE inventory_items
        SET remainingAmount = :remaining,
            version = :newVersion,
            updatedAt = :updatedAt,
            syncState = :syncState
        WHERE id = :id AND version = :expectedVersion AND deletedAt IS NULL
        """,
    )
    suspend fun updateRemainingWithVersion(
        id: String,
        remaining: Double,
        expectedVersion: Int,
        newVersion: Int,
        updatedAt: Long,
        syncState: String,
    ): Int

    @Query(
        """
        UPDATE inventory_items
        SET deletedAt = :deletedAt, updatedAt = :updatedAt, syncState = :syncState
        WHERE id = :id
        """,
    )
    suspend fun softDelete(id: String, deletedAt: Long, updatedAt: Long, syncState: String)

    @Query("DELETE FROM inventory_items WHERE userId = :userId")
    suspend fun deleteAllForUser(userId: String)

    @Query("SELECT COUNT(*) FROM inventory_items WHERE userId = :userId AND deletedAt IS NULL")
    suspend fun countActiveForUser(userId: String): Int

    @Query(
        """
        SELECT * FROM inventory_items
        WHERE userId = :userId AND deletedAt IS NULL
        ORDER BY id ASC
        LIMIT :limit OFFSET :offset
        """,
    )
    suspend fun getActivePageForUser(userId: String, limit: Int, offset: Int): List<InventoryItemEntity>

    @Query(
        """
        SELECT * FROM inventory_items
        WHERE userId = :userId AND nameNormalized = :nameNormalized
          AND purchaseDate = :purchaseDate AND deletedAt IS NULL
        LIMIT 1
        """,
    )
    suspend fun findByNameAndPurchaseDate(
        userId: String,
        nameNormalized: String,
        purchaseDate: String,
    ): InventoryItemEntity?
}
