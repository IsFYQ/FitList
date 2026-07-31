package com.example.healthcheckin.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.healthcheckin.data.local.entity.DailyBudgetEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyBudgetDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: DailyBudgetEntity)

    @Query("SELECT * FROM daily_budgets WHERE userId = :userId AND localDate = :localDate AND deletedAt IS NULL LIMIT 1")
    fun observeByDate(userId: String, localDate: String): Flow<DailyBudgetEntity?>

    @Query("SELECT * FROM daily_budgets WHERE userId = :userId AND localDate = :localDate AND deletedAt IS NULL LIMIT 1")
    suspend fun getByDate(userId: String, localDate: String): DailyBudgetEntity?

    @Query("SELECT * FROM daily_budgets WHERE id = :id LIMIT 1")
    suspend fun getByIdRaw(id: String): DailyBudgetEntity?

    @Query("DELETE FROM daily_budgets WHERE userId = :userId")
    suspend fun deleteAllForUser(userId: String)

    @Query("SELECT COUNT(*) FROM daily_budgets WHERE userId = :userId AND deletedAt IS NULL")
    suspend fun countActiveForUser(userId: String): Int

    @Query("""
        SELECT * FROM daily_budgets
        WHERE userId = :userId AND deletedAt IS NULL
        ORDER BY id ASC
        LIMIT :limit OFFSET :offset
    """)
    suspend fun getActivePageForUser(userId: String, limit: Int, offset: Int): List<DailyBudgetEntity>
}
