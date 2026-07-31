package com.example.healthcheckin.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.healthcheckin.data.local.entity.ProfileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProfileDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: ProfileEntity)

    @Update
    suspend fun update(entity: ProfileEntity)

    @Query("SELECT * FROM profiles WHERE id = :id AND deletedAt IS NULL LIMIT 1")
    suspend fun getById(id: String): ProfileEntity?

    @Query("SELECT * FROM profiles WHERE id = :id AND deletedAt IS NULL LIMIT 1")
    fun observeById(id: String): Flow<ProfileEntity?>

    @Query("SELECT * FROM profiles WHERE deletedAt IS NULL LIMIT 1")
    fun observeCurrent(): Flow<ProfileEntity?>

    @Query("UPDATE profiles SET deletedAt = :deletedAt, updatedAt = :updatedAt, syncState = :syncState WHERE id = :id")
    suspend fun softDelete(id: String, deletedAt: Long, updatedAt: Long, syncState: String)

    @Query("SELECT * FROM profiles WHERE id = :id LIMIT 1")
    suspend fun getByIdRaw(id: String): ProfileEntity?

    @Query("DELETE FROM profiles WHERE userId = :userId")
    suspend fun deleteAllForUser(userId: String)

    @Query("SELECT * FROM profiles WHERE userId = :userId AND deletedAt IS NULL LIMIT 1")
    suspend fun getActiveByUserId(userId: String): ProfileEntity?

    @Query("SELECT COUNT(*) FROM profiles WHERE userId = :userId AND deletedAt IS NULL")
    suspend fun countActiveForUser(userId: String): Int
}
