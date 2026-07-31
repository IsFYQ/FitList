package com.example.healthcheckin.domain.algorithm

import com.example.healthcheckin.domain.model.MealNutritionPreview
import com.example.healthcheckin.util.MealUnit
import com.example.healthcheckin.util.PrecisionUtil
import com.example.healthcheckin.util.UnitConverter

object MealNutritionCalculator {

    fun compute(
        quantity: Double,
        unit: MealUnit,
        servingGrams: Double?,
        kcalPer100: Double,
        proteinPer100: Double?,
        carbPer100: Double?,
        fatPer100: Double?,
    ): MealNutritionPreview {
        val basisAmount = UnitConverter.basisAmount(quantity, unit, servingGrams)
        return MealNutritionPreview(
            kcal = PrecisionUtil.roundStorage(UnitConverter.nutritionFromPer100(kcalPer100, basisAmount)),
            proteinG = proteinPer100?.let {
                PrecisionUtil.roundStorage(UnitConverter.nutritionFromPer100(it, basisAmount))
            },
            carbG = carbPer100?.let {
                PrecisionUtil.roundStorage(UnitConverter.nutritionFromPer100(it, basisAmount))
            },
            fatG = fatPer100?.let {
                PrecisionUtil.roundStorage(UnitConverter.nutritionFromPer100(it, basisAmount))
            },
        )
    }

    fun basisAmount(quantity: Double, unit: MealUnit, servingGrams: Double?): Double =
        UnitConverter.basisAmount(quantity, unit, servingGrams)
}
