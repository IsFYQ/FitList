package com.example.healthcheckin.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.healthcheckin.data.local.entity.MealEntryEntity
import com.example.healthcheckin.data.local.model.DailyConsumptionSummary
import kotlinx.coroutines.flow.Flow

@Dao
interface MealEntryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: MealEntryEntity)

    @Update
    suspend fun update(entity: MealEntryEntity)

    @Query("SELECT * FROM meal_entries WHERE id = :id AND deletedAt IS NULL LIMIT 1")
    suspend fun getById(id: String): MealEntryEntity?

    @Query("SELECT * FROM meal_entries WHERE id = :id LIMIT 1")
    suspend fun getByIdRaw(id: String): MealEntryEntity?

    @Query("""
        SELECT * FROM meal_entries
        WHERE userId = :userId AND localDate = :localDate AND deletedAt IS NULL
        ORDER BY consumedAt ASC
    """)
    fun observeByLocalDate(userId: String, localDate: String): Flow<List<MealEntryEntity>>

    @Query("""
        SELECT COALESCE(SUM(kcal), 0) FROM meal_entries
        WHERE userId = :userId AND localDate = :localDate AND deletedAt IS NULL
    """)
    fun observeTotalKcalByDate(userId: String, localDate: String): Flow<Double>

    @Query("""
        SELECT
            COALESCE(SUM(kcal), 0) AS consumedKcal,
            COALESCE(SUM(proteinG), 0) AS consumedProtein,
            COALESCE(SUM(carbG), 0) AS consumedCarb,
            COALESCE(SUM(fatG), 0) AS consumedFat,
            COUNT(*) AS entryCount
        FROM meal_entries
        WHERE userId = :userId AND localDate = :localDate AND deletedAt IS NULL
    """)
    fun observeDailySummary(userId: String, localDate: String): Flow<DailyConsumptionSummary>

    @Query("""
        SELECT
            COALESCE(SUM(kcal), 0) AS consumedKcal,
            COALESCE(SUM(proteinG), 0) AS consumedProtein,
            COALESCE(SUM(carbG), 0) AS consumedCarb,
            COALESCE(SUM(fatG), 0) AS consumedFat,
            COUNT(*) AS entryCount
        FROM meal_entries
        WHERE userId = :userId AND localDate = :localDate AND deletedAt IS NULL
    """)
    suspend fun getDailySummary(userId: String, localDate: String): DailyConsumptionSummary

    @Query("""
        UPDATE meal_entries
        SET deletedAt = NULL, updatedAt = :updatedAt, syncState = :syncState
        WHERE id = :id
    """)
    suspend fun restoreSoftDelete(id: String, updatedAt: Long, syncState: String)

    @Query("""
        UPDATE meal_entries
        SET deletedAt = :deletedAt, updatedAt = :updatedAt, syncState = :syncState
        WHERE id = :id
    """)
    suspend fun softDelete(id: String, deletedAt: Long, updatedAt: Long, syncState: String)

    @Query("""
        SELECT foodId FROM meal_entries
        WHERE userId = :userId AND foodId IS NOT NULL AND deletedAt IS NULL
        GROUP BY foodId
        ORDER BY MAX(consumedAt) DESC
        LIMIT :limit
    """)
    suspend fun getRecentFoodIds(userId: String, limit: Int = 8): List<String>

    @Query("""
        SELECT foodId, COUNT(*) as cnt FROM meal_entries
        WHERE userId = :userId
          AND foodId IS NOT NULL
          AND deletedAt IS NULL
          AND localDate >= :sinceDate
        GROUP BY foodId
        HAVING cnt >= :minCount
        ORDER BY cnt DESC
        LIMIT :limit
    """)
    suspend fun getFrequentFoodCounts(
        userId: String,
        sinceDate: String,
        minCount: Int = 3,
        limit: Int = 8,
    ): List<com.example.healthcheckin.data.local.model.FoodUsageCount>

    @Query("SELECT COUNT(*) FROM meal_entries WHERE foodId = :foodId AND deletedAt IS NULL")
    suspend fun countByFoodId(foodId: String): Int

    @Query("DELETE FROM meal_entries WHERE userId = :userId")
    suspend fun deleteAllForUser(userId: String)

    @Query("SELECT COUNT(*) FROM meal_entries WHERE userId = :userId AND deletedAt IS NULL")
    suspend fun countActiveForUser(userId: String): Int

    @Query("""
        SELECT * FROM meal_entries
        WHERE userId = :userId AND deletedAt IS NULL
        ORDER BY id ASC
        LIMIT :limit OFFSET :offset
    """)
    suspend fun getActivePageForUser(userId: String, limit: Int, offset: Int): List<MealEntryEntity>
}
