package com.example.healthcheckin.data.local.seed

import android.content.Context
import com.example.healthcheckin.data.local.entity.PublicFoodEntity
import com.example.healthcheckin.util.Validators
import java.io.BufferedReader
import java.io.InputStreamReader

object PublicFoodCatalogLoader {
    private const val FOODS_ASSET = "nutridata_foods.csv"
    private const val DISHES_ASSET = "nutridata_dishes_id2_per100.csv"
    private const val DEFAULT_SERVING_GRAMS = 100.0
    private const val ID_PREFIX_FOOD = "pub-food-"
    private const val ID_PREFIX_DISH = "pub-dish-"
    private const val FOOD_TAIL_COLUMNS = 4
    private const val DISH_TAIL_COLUMNS = 5

    fun load(context: Context): List<PublicFoodEntity> {
        val now = System.currentTimeMillis()
        val foods = loadFoods(context, now)
        val dishes = loadDishes(context, now)
        return foods + dishes
    }

    private fun loadFoods(context: Context, now: Long): List<PublicFoodEntity> =
        readCsvRows(context, FOODS_ASSET, FOOD_TAIL_COLUMNS).mapIndexedNotNull { index, parts ->
            if (parts.size != FOOD_TAIL_COLUMNS + 1) return@mapIndexedNotNull null
            val name = parts[0].trim()
            if (name.isEmpty() || name == "食物名称") return@mapIndexedNotNull null
            val kcal = parts[1].trim().toDoubleOrNull() ?: return@mapIndexedNotNull null
            toEntity(
                id = "$ID_PREFIX_FOOD$index",
                name = name,
                kcalPer100 = kcal,
                proteinPer100 = parts[2].trim().toDoubleOrNull(),
                fatPer100 = parts[3].trim().toDoubleOrNull(),
                carbPer100 = parts[4].trim().toDoubleOrNull(),
                servingGrams = DEFAULT_SERVING_GRAMS,
                now = now,
            )
        }

    private fun loadDishes(context: Context, now: Long): List<PublicFoodEntity> =
        readCsvRows(context, DISHES_ASSET, DISH_TAIL_COLUMNS).mapIndexedNotNull { index, parts ->
            if (parts.size != DISH_TAIL_COLUMNS + 1) return@mapIndexedNotNull null
            val name = parts[0].trim()
            if (name.isEmpty() || name == "食物名称") return@mapIndexedNotNull null
            val kcal = parts[1].trim().toDoubleOrNull() ?: return@mapIndexedNotNull null
            val servingGrams = parts[5].trim().toDoubleOrNull() ?: return@mapIndexedNotNull null
            toEntity(
                id = "$ID_PREFIX_DISH$index",
                name = name,
                kcalPer100 = kcal,
                proteinPer100 = parts[2].trim().toDoubleOrNull(),
                fatPer100 = parts[3].trim().toDoubleOrNull(),
                carbPer100 = parts[4].trim().toDoubleOrNull(),
                servingGrams = servingGrams,
                now = now,
            )
        }

    private fun toEntity(
        id: String,
        name: String,
        kcalPer100: Double,
        proteinPer100: Double?,
        fatPer100: Double?,
        carbPer100: Double?,
        servingGrams: Double,
        now: Long,
    ) = PublicFoodEntity(
        id = id,
        source = "PUBLIC",
        name = name,
        nameNormalized = Validators.normalizeFoodName(name),
        basisUnit = "G",
        kcalPer100 = kcalPer100,
        proteinPer100 = proteinPer100,
        carbPer100 = carbPer100,
        fatPer100 = fatPer100,
        servingGrams = servingGrams,
        createdAt = now,
        updatedAt = now,
    )

    private fun readCsvRows(context: Context, assetName: String, tailColumns: Int): List<List<String>> {
        context.assets.open(assetName).use { input ->
            BufferedReader(InputStreamReader(input, Charsets.UTF_8)).use { reader ->
                val rows = mutableListOf<List<String>>()
                reader.forEachLine { line ->
                    if (line.isBlank()) return@forEachLine
                    rows += parseCsvLine(line.removePrefix("\uFEFF"), tailColumns)
                }
                return rows
            }
        }
    }

    /** Name may contain commas; numeric columns are always the last [tailColumns] fields. */
    private fun parseCsvLine(line: String, tailColumns: Int): List<String> {
        val parts = line.split(',')
        if (parts.size <= tailColumns) return parts
        val tail = parts.takeLast(tailColumns)
        val name = parts.dropLast(tailColumns).joinToString(",").trim()
        return listOf(name) + tail
    }
}
