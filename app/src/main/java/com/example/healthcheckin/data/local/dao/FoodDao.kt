package com.example.healthcheckin.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.healthcheckin.data.local.entity.FoodEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FoodDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: FoodEntity)

    @Update
    suspend fun update(entity: FoodEntity)

    @Query("SELECT * FROM foods WHERE id = :id AND deletedAt IS NULL LIMIT 1")
    suspend fun getById(id: String): FoodEntity?

    @Query("SELECT * FROM foods WHERE id = :id LIMIT 1")
    suspend fun getByIdRaw(id: String): FoodEntity?

    @Query("""
        SELECT * FROM foods
        WHERE userId = :userId AND source = :source AND externalId = :externalId AND deletedAt IS NULL
        LIMIT 1
    """)
    suspend fun getByExternalId(userId: String, source: String, externalId: String): FoodEntity?

    @Query("SELECT * FROM foods WHERE id IN (:ids) AND deletedAt IS NULL")
    suspend fun getByIds(ids: List<String>): List<FoodEntity>

    @Query("""
        SELECT * FROM foods
        WHERE userId = :userId
          AND deletedAt IS NULL
          AND nameNormalized LIKE '%' || :query || '%'
        ORDER BY lastUsedAt DESC
        LIMIT :limit
    """)
    suspend fun searchByName(userId: String, query: String, limit: Int = 20): List<FoodEntity>

    @Query("""
        SELECT * FROM foods
        WHERE userId = :userId AND deletedAt IS NULL AND lastUsedAt IS NOT NULL
        ORDER BY lastUsedAt DESC
        LIMIT :limit
    """)
    suspend fun getRecent(userId: String, limit: Int = 8): List<FoodEntity>

    @Query("""
        SELECT * FROM foods
        WHERE userId = :userId AND deletedAt IS NULL
        ORDER BY lastUsedAt DESC
        LIMIT :limit
    """)
    fun observeRecent(userId: String, limit: Int = 20): Flow<List<FoodEntity>>

    @Query("""
        SELECT * FROM foods
        WHERE userId = :userId AND deletedAt IS NULL AND useCount30d >= :minCount
        ORDER BY useCount30d DESC, lastUsedAt DESC
        LIMIT :limit
    """)
    suspend fun getFrequent(userId: String, minCount: Int = 3, limit: Int = 8): List<FoodEntity>

    @Query("""
        SELECT * FROM foods
        WHERE userId = :userId AND source = 'CUSTOM' AND deletedAt IS NULL
        ORDER BY name COLLATE NOCASE ASC
    """)
    suspend fun listCustomFoods(userId: String): List<FoodEntity>

    @Query("""
        SELECT COUNT(*) FROM foods
        WHERE userId = :userId AND source = 'CUSTOM' AND deletedAt IS NULL
    """)
    fun observeCustomFoodCount(userId: String): Flow<Int>

    @Query("""
        SELECT * FROM foods
        WHERE userId = :userId AND source = 'CUSTOM' AND nameNormalized = :nameNormalized
          AND deletedAt IS NULL
        LIMIT 1
    """)
    suspend fun findCustomByName(userId: String, nameNormalized: String): FoodEntity?

    @Query("""
        UPDATE foods
        SET deletedAt = :deletedAt, updatedAt = :updatedAt, syncState = :syncState
        WHERE id = :id
    """)
    suspend fun softDelete(id: String, deletedAt: Long, updatedAt: Long, syncState: String)

    @Query("DELETE FROM foods WHERE userId = :userId")
    suspend fun deleteAllForUser(userId: String)

    @Query("SELECT COUNT(*) FROM foods WHERE userId = :userId AND deletedAt IS NULL")
    suspend fun countActiveForUser(userId: String): Int

    @Query("""
        SELECT * FROM foods
        WHERE userId = :userId AND deletedAt IS NULL
        ORDER BY id ASC
        LIMIT :limit OFFSET :offset
    """)
    suspend fun getActivePageForUser(userId: String, limit: Int, offset: Int): List<FoodEntity>
}
