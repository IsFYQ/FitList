package com.example.healthcheckin.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.healthcheckin.data.local.entity.IngredientAliasEntity

@Dao
interface IngredientAliasDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<IngredientAliasEntity>)

    @Query("SELECT * FROM ingredient_aliases WHERE alias = :alias LIMIT 1")
    suspend fun findByAlias(alias: String): IngredientAliasEntity?

    @Query("SELECT COUNT(*) FROM ingredient_aliases")
    suspend fun count(): Int

    @Query("SELECT * FROM ingredient_aliases")
    suspend fun getAll(): List<IngredientAliasEntity>
}
