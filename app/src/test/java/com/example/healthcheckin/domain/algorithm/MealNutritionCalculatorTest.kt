package com.example.healthcheckin.domain.algorithm

import com.example.healthcheckin.util.MealUnit
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * REQ-005 AC-005-02 / AC-005-03 nutrition calculation checks.
 */
class MealNutritionCalculatorTest {

    @Test
    fun ac005_02_rice150g() {
        val result = MealNutritionCalculator.compute(
            quantity = 150.0,
            unit = MealUnit.G,
            servingGrams = null,
            kcalPer100 = 116.0,
            proteinPer100 = 2.6,
            carbPer100 = 25.9,
            fatPer100 = 0.3,
        )
        assertEquals(174.0, result.kcal, 0.01)
    }

    @Test
    fun ac005_03_milkOneServing() {
        val result = MealNutritionCalculator.compute(
            quantity = 1.0,
            unit = MealUnit.SERVING,
            servingGrams = 250.0,
            kcalPer100 = 66.0,
            proteinPer100 = 3.4,
            carbPer100 = 4.8,
            fatPer100 = 3.6,
        )
        assertEquals(165.0, result.kcal, 0.01)
    }

    @Test
    fun basisAmount_servingUsesServingGrams() {
        val basis = MealNutritionCalculator.basisAmount(2.0, MealUnit.SERVING, 100.0)
        assertEquals(200.0, basis, 0.01)
    }
}
