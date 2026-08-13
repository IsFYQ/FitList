package com.example.healthcheckin.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.healthcheckin.data.local.entity.IngredientBindingEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface IngredientBindingDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: IngredientBindingEntity)

    @Update
    suspend fun update(entity: IngredientBindingEntity)

    @Query(
        """
        SELECT * FROM ingredient_bindings
        WHERE userId = :userId AND foodId = :foodId AND deletedAt IS NULL
        LIMIT 1
        """,
    )
    suspend fun findByFood(userId: String, foodId: String): IngredientBindingEntity?

    @Query(
        """
        SELECT * FROM ingredient_bindings
        WHERE userId = :userId AND inventoryItemId = :inventoryItemId AND deletedAt IS NULL
        LIMIT 1
        """,
    )
    suspend fun findByInventoryItem(userId: String, inventoryItemId: String): IngredientBindingEntity?

    @Query(
        """
        SELECT * FROM ingredient_bindings
        WHERE userId = :userId AND deletedAt IS NULL
        ORDER BY updatedAt DESC
        """,
    )
    fun observeAll(userId: String): Flow<List<IngredientBindingEntity>>

    @Query("SELECT COUNT(*) FROM ingredient_bindings WHERE userId = :userId AND deletedAt IS NULL")
    fun observeCount(userId: String): Flow<Int>

    @Query("SELECT * FROM ingredient_bindings WHERE id = :id AND deletedAt IS NULL LIMIT 1")
    suspend fun getById(id: String): IngredientBindingEntity?

    @Query("SELECT * FROM ingredient_bindings WHERE id = :id LIMIT 1")
    suspend fun getByIdRaw(id: String): IngredientBindingEntity?

    @Query(
        """
        UPDATE ingredient_bindings
        SET deletedAt = :deletedAt, updatedAt = :updatedAt, syncState = :syncState
        WHERE inventoryItemId = :inventoryItemId AND deletedAt IS NULL
        """,
    )
    suspend fun softDeleteByInventoryItem(
        inventoryItemId: String,
        deletedAt: Long,
        updatedAt: Long,
        syncState: String,
    )

    @Query(
        """
        UPDATE ingredient_bindings
        SET deletedAt = :deletedAt, updatedAt = :updatedAt, syncState = :syncState
        WHERE id = :id
        """,
    )
    suspend fun softDelete(id: String, deletedAt: Long, updatedAt: Long, syncState: String)

    @Query("DELETE FROM ingredient_bindings WHERE userId = :userId")
    suspend fun deleteAllForUser(userId: String)

    @Query("SELECT COUNT(*) FROM ingredient_bindings WHERE userId = :userId AND deletedAt IS NULL")
    suspend fun countActiveForUser(userId: String): Int

    @Query(
        """
        SELECT * FROM ingredient_bindings
        WHERE userId = :userId AND deletedAt IS NULL
        ORDER BY id ASC
        LIMIT :limit OFFSET :offset
        """,
    )
    suspend fun getActivePageForUser(userId: String, limit: Int, offset: Int): List<IngredientBindingEntity>
}
