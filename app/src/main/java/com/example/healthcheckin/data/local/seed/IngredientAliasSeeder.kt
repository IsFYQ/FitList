package com.example.healthcheckin.data.local.seed

import android.content.Context
import com.example.healthcheckin.data.local.dao.IngredientAliasDao
import com.example.healthcheckin.data.local.entity.IngredientAliasEntity
import com.example.healthcheckin.util.Validators
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class IngredientAliasSeeder(
    private val context: Context,
    private val aliasDao: IngredientAliasDao,
) {
    suspend fun seedIfNeeded() {
        if (aliasDao.count() > 0) return
        val entries = context.assets.open("ingredient_aliases.json").bufferedReader().use { reader ->
            Json.parseToJsonElement(reader.readText()).jsonArray.map {
                val obj = it.jsonObject
                IngredientAliasEntity(
                    alias = Validators.normalizeFoodName(obj.getValue("alias").jsonPrimitive.content),
                    ingredientKey = obj.getValue("key").jsonPrimitive.content,
                )
            }
        }
        aliasDao.insertAll(entries)
    }
}
