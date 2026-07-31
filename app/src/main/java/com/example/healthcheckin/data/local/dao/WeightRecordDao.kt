package com.example.healthcheckin.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.healthcheckin.data.local.entity.WeightRecordEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WeightRecordDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: WeightRecordEntity)

    @Update
    suspend fun update(entity: WeightRecordEntity)

    @Query("SELECT * FROM weight_records WHERE userId = :userId AND localDate = :localDate AND deletedAt IS NULL LIMIT 1")
    suspend fun getByDate(userId: String, localDate: String): WeightRecordEntity?

    @Query("""
        SELECT * FROM weight_records
        WHERE userId = :userId AND deletedAt IS NULL
          AND localDate BETWEEN :startDate AND :endDate
        ORDER BY localDate ASC
    """)
    fun observeByDateRange(userId: String, startDate: String, endDate: String): Flow<List<WeightRecordEntity>>

    @Query("""
        SELECT * FROM weight_records
        WHERE userId = :userId AND deletedAt IS NULL
        ORDER BY localDate DESC
        LIMIT 2
    """)
    fun observeLatestTwo(userId: String): Flow<List<WeightRecordEntity>>

    @Query("""
        SELECT * FROM weight_records
        WHERE userId = :userId AND deletedAt IS NULL
        ORDER BY localDate DESC
    """)
    fun observeAll(userId: String): Flow<List<WeightRecordEntity>>

    @Query("SELECT * FROM weight_records WHERE id = :id AND deletedAt IS NULL LIMIT 1")
    suspend fun getById(id: String): WeightRecordEntity?

    @Query("SELECT * FROM weight_records WHERE id = :id LIMIT 1")
    suspend fun getByIdRaw(id: String): WeightRecordEntity?

    @Query("""
        SELECT * FROM weight_records
        WHERE userId = :userId AND deletedAt IS NULL AND localDate < :beforeDate
        ORDER BY localDate DESC
        LIMIT 1
    """)
    suspend fun getPreviousBeforeDate(userId: String, beforeDate: String): WeightRecordEntity?

    @Query("""
        UPDATE weight_records
        SET deletedAt = :deletedAt, updatedAt = :updatedAt, syncState = :syncState
        WHERE id = :id
    """)
    suspend fun softDelete(id: String, deletedAt: Long, updatedAt: Long, syncState: String)

    @Query("DELETE FROM weight_records WHERE userId = :userId")
    suspend fun deleteAllForUser(userId: String)

    @Query("SELECT COUNT(*) FROM weight_records WHERE userId = :userId AND deletedAt IS NULL")
    suspend fun countActiveForUser(userId: String): Int

    @Query("""
        SELECT * FROM weight_records
        WHERE userId = :userId AND deletedAt IS NULL
        ORDER BY id ASC
        LIMIT :limit OFFSET :offset
    """)
    suspend fun getActivePageForUser(userId: String, limit: Int, offset: Int): List<WeightRecordEntity>
}
