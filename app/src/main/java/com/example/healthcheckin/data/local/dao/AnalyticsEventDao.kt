package com.example.healthcheckin.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.healthcheckin.data.local.entity.AnalyticsEventEntity

@Dao
interface AnalyticsEventDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: AnalyticsEventEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<AnalyticsEventEntity>)

    @Query("SELECT COUNT(*) FROM analytics_events")
    suspend fun countAll(): Int

    @Query("DELETE FROM analytics_events WHERE syncState = 'SYNCED' AND eventAt < :cutoff")
    suspend fun deleteSyncedOlderThan(cutoff: Long)

    @Query("""
        DELETE FROM analytics_events WHERE id IN (
            SELECT id FROM analytics_events WHERE syncState = 'SYNCED'
            ORDER BY eventAt ASC LIMIT :count
        )
    """)
    suspend fun deleteOldestSynced(count: Int)

    @Query("""
        UPDATE analytics_events
        SET userId = :userId, updatedAt = :updatedAt
        WHERE userId IS NULL AND sessionId = :sessionId
    """)
    suspend fun backfillUserId(sessionId: String, userId: String, updatedAt: Long)

    @Query("""
        UPDATE analytics_events
        SET syncState = :syncState, updatedAt = :updatedAt
        WHERE id IN (:ids)
    """)
    suspend fun markSyncState(ids: List<String>, syncState: String, updatedAt: Long)

    @Query("SELECT * FROM analytics_events WHERE syncState != 'SYNCED' ORDER BY createdAt ASC LIMIT :limit")
    suspend fun getUnsynced(limit: Int = 200): List<AnalyticsEventEntity>

    @Query("UPDATE analytics_events SET syncState = :syncState, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateSyncState(id: String, syncState: String, updatedAt: Long)

    @Query("SELECT * FROM analytics_events WHERE id = :id LIMIT 1")
    suspend fun getByIdRaw(id: String): AnalyticsEventEntity?

    @Query("DELETE FROM analytics_events WHERE userId = :userId")
    suspend fun deleteAllForUser(userId: String)
}
