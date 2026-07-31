package com.example.healthcheckin.util

import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.abs

object UnitConverter {

    fun kgToG(kg: Double): Double = kg * 1000.0

    fun gToKg(g: Double): Double = g / 1000.0

    fun lToMl(l: Double): Double = l * 1000.0

    fun mlToL(ml: Double): Double = ml / 1000.0

    fun basisAmount(quantity: Double, unit: MealUnit, servingGrams: Double?): Double = when (unit) {
        MealUnit.G, MealUnit.ML -> quantity
        MealUnit.SERVING -> {
            require(servingGrams != null && servingGrams > 0) {
                "serving_grams required for SERVING unit"
            }
            quantity * servingGrams
        }
    }

    fun nutritionFromPer100(per100: Double, basisAmount: Double): Double =
        per100 * basisAmount / 100.0

    fun perServingToPer100(perServing: Double, servingGrams: Double): Double? {
        if (servingGrams <= 0.0) return null
        return perServing * 100.0 / servingGrams
    }

    fun inventoryToBasisAmount(
        quantity: Double,
        unit: String,
        pieceGrams: Double?,
    ): Double = when (unit) {
        "G" -> quantity
        "KG" -> kgToG(quantity)
        "ML" -> quantity
        "L" -> lToMl(quantity)
        "PIECE" -> {
            require(pieceGrams != null && pieceGrams > 0) {
                "piece_grams required for PIECE unit"
            }
            quantity * pieceGrams
        }
        else -> throw IllegalArgumentException("Unknown inventory unit: $unit")
    }
}

object PrecisionUtil {

    private val HALF_UP = RoundingMode.HALF_UP

    fun roundStorage(value: Double): Double =
        BigDecimal.valueOf(value).setScale(2, HALF_UP).toDouble()

    fun roundCaloriesDisplay(value: Double): Int =
        BigDecimal.valueOf(value).setScale(0, HALF_UP).toInt()

    fun roundMacroDisplay(value: Double): Double =
        BigDecimal.valueOf(value).setScale(1, HALF_UP).toDouble()

    fun roundWeightDisplay(value: Double): Double =
        BigDecimal.valueOf(value).setScale(1, HALF_UP).toDouble()

    fun roundInt(value: Double): Int =
        BigDecimal.valueOf(value).setScale(0, HALF_UP).toInt()

    fun formatCaloriesWithSeparator(value: Int): String {
        val absValue = abs(value)
        val formatted = absValue.toString()
            .reversed()
            .chunked(3)
            .joinToString(",")
            .reversed()
        return if (value < 0) "-$formatted" else formatted
    }

    fun formatPercent(ratio: Double): String {
        val percent = roundInt(ratio * 100.0)
        return "$percent%"
    }

    fun aggregateCalories(values: List<Double>): Int {
        val sum = values.fold(0.0) { acc, v ->
            acc + roundStorage(v)
        }
        return roundCaloriesDisplay(sum)
    }

    fun aggregateMacros(values: List<Double>): Double {
        val sum = values.fold(0.0) { acc, v ->
            acc + roundStorage(v)
        }
        return roundMacroDisplay(sum)
    }
}
