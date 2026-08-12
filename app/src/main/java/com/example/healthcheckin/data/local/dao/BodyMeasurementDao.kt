package com.example.healthcheckin.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.healthcheckin.data.local.entity.BodyMeasurementEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BodyMeasurementDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: BodyMeasurementEntity)

    @Update
    suspend fun update(entity: BodyMeasurementEntity)

    @Query(
        """
        SELECT * FROM body_measurements
        WHERE userId = :userId AND metric = :metric AND localDate = :localDate AND deletedAt IS NULL
        LIMIT 1
        """,
    )
    suspend fun getByMetricDate(userId: String, metric: String, localDate: String): BodyMeasurementEntity?

    @Query(
        """
        SELECT * FROM body_measurements
        WHERE userId = :userId AND metric = :metric AND deletedAt IS NULL
        ORDER BY localDate DESC
        """,
    )
    fun observeByMetric(userId: String, metric: String): Flow<List<BodyMeasurementEntity>>

    @Query(
        """
        SELECT * FROM body_measurements
        WHERE userId = :userId AND metric = :metric AND deletedAt IS NULL
          AND localDate BETWEEN :startDate AND :endDate
        ORDER BY localDate ASC
        """,
    )
    fun observeByMetricRange(
        userId: String,
        metric: String,
        startDate: String,
        endDate: String,
    ): Flow<List<BodyMeasurementEntity>>

    @Query(
        """
        SELECT * FROM body_measurements
        WHERE userId = :userId AND deletedAt IS NULL
        ORDER BY metric ASC, localDate DESC
        """,
    )
    fun observeAll(userId: String): Flow<List<BodyMeasurementEntity>>

    @Query("SELECT * FROM body_measurements WHERE id = :id AND deletedAt IS NULL LIMIT 1")
    suspend fun getById(id: String): BodyMeasurementEntity?

    @Query("SELECT * FROM body_measurements WHERE id = :id LIMIT 1")
    suspend fun getByIdRaw(id: String): BodyMeasurementEntity?

    @Query(
        """
        SELECT * FROM body_measurements
        WHERE userId = :userId AND metric = :metric AND deletedAt IS NULL AND localDate < :beforeDate
        ORDER BY localDate DESC
        LIMIT 1
        """,
    )
    suspend fun getPreviousBeforeDate(
        userId: String,
        metric: String,
        beforeDate: String,
    ): BodyMeasurementEntity?

    @Query(
        """
        UPDATE body_measurements
        SET deletedAt = :deletedAt, updatedAt = :updatedAt, syncState = :syncState
        WHERE id = :id
        """,
    )
    suspend fun softDelete(id: String, deletedAt: Long, updatedAt: Long, syncState: String)

    @Query("DELETE FROM body_measurements WHERE userId = :userId")
    suspend fun deleteAllForUser(userId: String)

    @Query("SELECT COUNT(*) FROM body_measurements WHERE userId = :userId AND deletedAt IS NULL")
    suspend fun countActiveForUser(userId: String): Int

    @Query(
        """
        SELECT * FROM body_measurements
        WHERE userId = :userId AND deletedAt IS NULL
        ORDER BY id ASC
        LIMIT :limit OFFSET :offset
        """,
    )
    suspend fun getActivePageForUser(userId: String, limit: Int, offset: Int): List<BodyMeasurementEntity>
}
