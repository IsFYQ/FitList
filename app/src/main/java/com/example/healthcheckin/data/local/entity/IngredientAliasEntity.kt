package com.example.healthcheckin.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "ingredient_aliases",
    indices = [
        Index(value = ["ingredientKey"]),
    ],
)
data class IngredientAliasEntity(
    @PrimaryKey val alias: String,
    val ingredientKey: String,
)
