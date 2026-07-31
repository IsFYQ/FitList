package com.example.healthcheckin.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.healthcheckin.data.local.entity.GoalEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GoalDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: GoalEntity)

    @Update
    suspend fun update(entity: GoalEntity)

    @Query("SELECT * FROM goals WHERE userId = :userId AND isActive = 1 AND deletedAt IS NULL LIMIT 1")
    fun observeActiveGoal(userId: String): Flow<GoalEntity?>

    @Query("SELECT * FROM goals WHERE userId = :userId AND isActive = 1 AND deletedAt IS NULL LIMIT 1")
    suspend fun getActiveGoal(userId: String): GoalEntity?

    @Query("""
        SELECT * FROM goals
        WHERE userId = :userId AND deletedAt IS NULL AND effectiveFrom <= :localDate
        ORDER BY effectiveFrom DESC
        LIMIT 1
    """)
    suspend fun getGoalEffectiveOnDate(userId: String, localDate: String): GoalEntity?

    @Query("""
        SELECT * FROM goals
        WHERE userId = :userId AND deletedAt IS NULL
        ORDER BY effectiveFrom ASC
        LIMIT 1
    """)
    suspend fun getEarliestGoal(userId: String): GoalEntity?

    @Query("UPDATE goals SET isActive = 0, updatedAt = :updatedAt WHERE userId = :userId AND isActive = 1")
    suspend fun deactivateAll(userId: String, updatedAt: Long)

    @Query("SELECT * FROM goals WHERE userId = :userId AND deletedAt IS NULL ORDER BY createdAt DESC")
    fun observeByUser(userId: String): Flow<List<GoalEntity>>

    @Query("SELECT * FROM goals WHERE id = :id LIMIT 1")
    suspend fun getByIdRaw(id: String): GoalEntity?

    @Query("DELETE FROM goals WHERE userId = :userId")
    suspend fun deleteAllForUser(userId: String)

    @Query("SELECT COUNT(*) FROM goals WHERE userId = :userId AND deletedAt IS NULL")
    suspend fun countActiveForUser(userId: String): Int

    @Query("""
        SELECT * FROM goals
        WHERE userId = :userId AND deletedAt IS NULL
        ORDER BY id ASC
        LIMIT :limit OFFSET :offset
    """)
    suspend fun getActivePageForUser(userId: String, limit: Int, offset: Int): List<GoalEntity>
}
