package com.example.healthcheckin.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.healthcheckin.data.local.entity.AppSettingEntity
import com.example.healthcheckin.data.local.entity.BackupStateEntity
import com.example.healthcheckin.data.local.entity.FoodSearchCacheEntity
import com.example.healthcheckin.data.local.entity.SyncQueueEntity
import com.example.healthcheckin.data.local.model.TablePendingCount
import kotlinx.coroutines.flow.Flow

@Dao
interface FoodSearchCacheDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: FoodSearchCacheEntity)

    @Query("SELECT * FROM food_search_cache WHERE queryNormalized = :query LIMIT 1")
    suspend fun getByQuery(query: String): FoodSearchCacheEntity?

    @Query("DELETE FROM food_search_cache WHERE expiresAt < :now")
    suspend fun deleteExpired(now: Long)

    @Query("DELETE FROM food_search_cache")
    suspend fun deleteAll()
}

@Dao
interface SyncQueueDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: SyncQueueEntity)

    @Query("SELECT * FROM sync_queue ORDER BY createdAt ASC LIMIT :limit")
    suspend fun getPending(limit: Int = 200): List<SyncQueueEntity>

    @Query("""
        SELECT * FROM sync_queue
        WHERE tableName = :tableName
          AND (nextRetryAt IS NULL OR nextRetryAt <= :now)
        ORDER BY createdAt ASC
        LIMIT :limit
    """)
    suspend fun getPendingForTable(tableName: String, now: Long, limit: Int = 200): List<SyncQueueEntity>

    @Query("SELECT COUNT(*) FROM sync_queue WHERE tableName != 'analytics_events'")
    fun observePendingCount(): Flow<Int>

    @Query("""
        SELECT tableName, COUNT(*) as cnt FROM sync_queue
        GROUP BY tableName
    """)
    suspend fun countPendingByTable(): List<TablePendingCount>

    @Query("""
        UPDATE sync_queue
        SET retryCount = :retryCount,
            nextRetryAt = :nextRetryAt,
            lastErrorCode = :errorCode,
            updatedAt = :updatedAt
        WHERE id = :id
    """)
    suspend fun updateRetry(
        id: String,
        retryCount: Int,
        nextRetryAt: Long?,
        errorCode: String?,
        updatedAt: Long,
    )

    @Query("UPDATE sync_queue SET retryCount = 0, nextRetryAt = NULL, lastErrorCode = NULL, updatedAt = :updatedAt")
    suspend fun resetAllRetries(updatedAt: Long)

    @Query("DELETE FROM sync_queue WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM sync_queue")
    suspend fun deleteAll()

    @Query("""
        SELECT * FROM sync_queue
        WHERE lastErrorCode IS NOT NULL
        ORDER BY updatedAt DESC
        LIMIT :limit
    """)
    suspend fun getRecentErrors(limit: Int = 20): List<SyncQueueEntity>
}

@Dao
interface AppSettingDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: AppSettingEntity)

    @Query("SELECT * FROM app_settings WHERE key = :key LIMIT 1")
    suspend fun get(key: String): AppSettingEntity?

    @Query("SELECT * FROM app_settings WHERE key = :key LIMIT 1")
    fun observe(key: String): Flow<AppSettingEntity?>
}

@Dao
interface BackupStateDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: BackupStateEntity)

    @Query("SELECT * FROM backup_state WHERE tableName = :tableName AND rowId = :rowId LIMIT 1")
    suspend fun get(tableName: String, rowId: String): BackupStateEntity?
}
