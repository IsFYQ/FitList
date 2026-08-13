package com.example.healthcheckin.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.healthcheckin.data.local.entity.ExerciseRecordEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExerciseRecordDao {
    @Insert
    suspend fun insert(entity: ExerciseRecordEntity)

    @Update
    suspend fun update(entity: ExerciseRecordEntity)

    @Query(
        """
        SELECT * FROM exercise_records
        WHERE userId = :userId AND deletedAt IS NULL
        ORDER BY localDate DESC, createdAt DESC
        """,
    )
    fun observeAll(userId: String): Flow<List<ExerciseRecordEntity>>

    @Query("SELECT * FROM exercise_records WHERE id = :id AND deletedAt IS NULL LIMIT 1")
    suspend fun getById(id: String): ExerciseRecordEntity?

    @Query(
        """
        SELECT * FROM exercise_records
        WHERE userId = :userId AND deletedAt IS NULL
        ORDER BY localDate DESC, createdAt DESC
        """,
    )
    suspend fun getAll(userId: String): List<ExerciseRecordEntity>

    @Query(
        """
        UPDATE exercise_records
        SET deletedAt = :deletedAt, updatedAt = :updatedAt, syncState = :syncState
        WHERE id = :id
        """,
    )
    suspend fun softDelete(id: String, deletedAt: Long, updatedAt: Long, syncState: String)

    @Query("SELECT COUNT(*) FROM exercise_records WHERE userId = :userId AND deletedAt IS NULL")
    suspend fun countActiveForUser(userId: String): Int

    @Query(
        """
        SELECT * FROM exercise_records
        WHERE userId = :userId AND deletedAt IS NULL
        ORDER BY id ASC
        LIMIT :limit OFFSET :offset
        """,
    )
    suspend fun getActivePageForUser(userId: String, limit: Int, offset: Int): List<ExerciseRecordEntity>
}
