package com.example.healthcheckin.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.healthcheckin.data.local.entity.PublicFoodEntity

@Dao
interface PublicFoodDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<PublicFoodEntity>)

    @Query("""
        SELECT * FROM public_foods
        WHERE nameNormalized LIKE '%' || :query || '%'
        ORDER BY name ASC
        LIMIT :limit
    """)
    suspend fun searchByName(query: String, limit: Int = 20): List<PublicFoodEntity>

    @Query("SELECT * FROM public_foods WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): PublicFoodEntity?

    @Query("SELECT COUNT(*) FROM public_foods")
    suspend fun count(): Int
}
