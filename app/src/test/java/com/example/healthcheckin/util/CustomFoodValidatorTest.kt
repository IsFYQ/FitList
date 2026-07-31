package com.example.healthcheckin.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CustomFoodValidatorTest {

    @Test
    fun validFoodPasses() {
        assertNull(
            CustomFoodValidator.validate(
                name = "燕麦粥",
                kcalPer100 = 100.0,
                proteinPer100 = 3.0,
                carbPer100 = 18.0,
                fatPer100 = 2.0,
                servingGrams = 200.0,
            ),
        )
    }

    @Test
    fun rejectsZeroKcal() {
        assertEquals(
            "custom_food_error_kcal",
            CustomFoodValidator.validate(
                name = "水",
                kcalPer100 = 0.0,
                proteinPer100 = 0.0,
                carbPer100 = 0.0,
                fatPer100 = 0.0,
                servingGrams = null,
            ),
        )
    }

    @Test
    fun rejectsOnlyOnePositiveMacro() {
        assertEquals(
            "custom_food_error_macro_min",
            CustomFoodValidator.validate(
                name = "糖",
                kcalPer100 = 400.0,
                proteinPer100 = 0.0,
                carbPer100 = 100.0,
                fatPer100 = 0.0,
                servingGrams = null,
            ),
        )
    }
}
