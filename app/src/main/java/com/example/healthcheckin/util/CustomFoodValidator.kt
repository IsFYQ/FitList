package com.example.healthcheckin.util

object CustomFoodValidator {
    const val KCAL_MIN = 0.0
    const val KCAL_MAX = 900.0
    const val MACRO_MIN = 0.0
    const val MACRO_MAX = 100.0

    /**
     * Returns a string resource key for validation error, or null if valid.
     */
    fun validate(
        name: String,
        kcalPer100: Double?,
        proteinPer100: Double?,
        carbPer100: Double?,
        fatPer100: Double?,
        servingGrams: Double?,
    ): String? {
        val nameResult = Validators.validateFoodName(name)
        if (!nameResult.isValid) return "custom_food_error_name"

        if (kcalPer100 == null || kcalPer100 <= KCAL_MIN || kcalPer100 > KCAL_MAX) {
            return "custom_food_error_kcal"
        }

        val macros = listOfNotNull(proteinPer100, carbPer100, fatPer100)
        if (macros.any { it < MACRO_MIN || it > MACRO_MAX }) {
            return "custom_food_error_macro"
        }

        val positiveCount = listOf(proteinPer100 ?: 0.0, carbPer100 ?: 0.0, fatPer100 ?: 0.0)
            .count { it > 0.0 }
        if (positiveCount < 2) {
            return "custom_food_error_macro_min"
        }

        if (servingGrams != null && servingGrams <= 0.0) {
            return "custom_food_error_serving"
        }

        return null
    }
}
