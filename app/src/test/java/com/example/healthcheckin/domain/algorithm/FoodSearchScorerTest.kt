package com.example.healthcheckin.domain.algorithm

import com.example.healthcheckin.domain.model.FoodSearchItem
import com.example.healthcheckin.util.BasisUnit
import com.example.healthcheckin.util.FoodSource
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * TC-ALG-02 / AC-006-04: FatSecret exact match beats custom partial match (no hard pin).
 */
class FoodSearchScorerTest {

    @Test
    fun ac006_04_fatsecretExactBeatsCustomPartial() {
        val fatsecretOats = item(
            name = "燕麦粥",
            source = FoodSource.FATSECRET,
        )
        val customOats = item(
            name = "我的燕麦粥",
            source = FoodSource.CUSTOM,
        )
        val fatsecretScore = FoodSearchScorer.score("燕麦粥", fatsecretOats)
        val customScore = FoodSearchScorer.score("燕麦粥", customOats)
        assertTrue(fatsecretScore > customScore)
    }

    @Test
    fun nameMatch_exactAndContains() {
        assertTrue(FoodSearchScorer.nameMatch("燕麦粥", "燕麦粥") == 1.0)
        assertTrue(FoodSearchScorer.nameMatch("燕麦", "燕麦粥") == 0.80)
        assertTrue(FoodSearchScorer.nameMatch("麦粥", "燕麦粥") == 0.60)
    }

    @Test
    fun merge_appendDoesNotReorderExisting() {
        val existing = listOf(
            item(name = "A", source = FoodSource.CUSTOM).copy(score = 0.9),
            item(name = "B", source = FoodSource.CUSTOM).copy(score = 0.8),
        )
        val remote = listOf(
            item(name = "C", source = FoodSource.FATSECRET).copy(score = 0.95),
        )
        val merged = FoodSearchMerger.appendRemote(existing, remote)
        assertTrue(merged[0].name == "A")
        assertTrue(merged[1].name == "B")
        assertTrue(merged[2].name == "C")
    }

    private fun item(name: String, source: FoodSource) = FoodSearchItem(
        foodId = null,
        publicFoodId = null,
        externalId = "ext",
        name = name,
        brand = null,
        kcalPer100 = 100.0,
        proteinPer100 = 1.0,
        carbPer100 = 10.0,
        fatPer100 = 1.0,
        basisUnit = BasisUnit.G,
        servingName = null,
        servingGrams = null,
        source = source,
        dataIncomplete = false,
        nutritionWarning = false,
        lastUsedAt = null,
        lastQuantity = null,
        lastUnit = null,
    )
}
