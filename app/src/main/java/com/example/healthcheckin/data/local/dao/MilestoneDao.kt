package com.example.healthcheckin.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.healthcheckin.data.local.entity.MilestoneEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MilestoneDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: MilestoneEntity)

    @Update
    suspend fun update(entity: MilestoneEntity)

    @Query(
        """
        SELECT * FROM milestones
        WHERE userId = :userId AND deletedAt IS NULL
        ORDER BY
          CASE WHEN achievedAt IS NULL THEN 0 ELSE 1 END ASC,
          CASE WHEN achievedAt IS NULL THEN ABS(targetWeightKg) ELSE 0 END ASC,
          achievedAt DESC
        """,
    )
    fun observeAll(userId: String): Flow<List<MilestoneEntity>>

    @Query(
        """
        SELECT * FROM milestones
        WHERE userId = :userId AND deletedAt IS NULL AND achievedAt IS NULL
        """,
    )
    suspend fun getActive(userId: String): List<MilestoneEntity>

    @Query("SELECT COUNT(*) FROM milestones WHERE userId = :userId AND deletedAt IS NULL AND achievedAt IS NULL")
    suspend fun countActive(userId: String): Int

    @Query("SELECT * FROM milestones WHERE id = :id AND deletedAt IS NULL LIMIT 1")
    suspend fun getById(id: String): MilestoneEntity?

    @Query("SELECT * FROM milestones WHERE id = :id LIMIT 1")
    suspend fun getByIdRaw(id: String): MilestoneEntity?

    @Query(
        """
        UPDATE milestones
        SET deletedAt = :deletedAt, updatedAt = :updatedAt, syncState = :syncState
        WHERE id = :id
        """,
    )
    suspend fun softDelete(id: String, deletedAt: Long, updatedAt: Long, syncState: String)

    @Query("DELETE FROM milestones WHERE userId = :userId")
    suspend fun deleteAllForUser(userId: String)

    @Query("SELECT COUNT(*) FROM milestones WHERE userId = :userId AND deletedAt IS NULL")
    suspend fun countActiveForUser(userId: String): Int

    @Query(
        """
        SELECT * FROM milestones
        WHERE userId = :userId AND deletedAt IS NULL
        ORDER BY id ASC
        LIMIT :limit OFFSET :offset
        """,
    )
    suspend fun getActivePageForUser(userId: String, limit: Int, offset: Int): List<MilestoneEntity>
}
