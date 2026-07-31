package com.example.healthcheckin.domain.algorithm

import com.example.healthcheckin.util.PrecisionUtil
import com.example.healthcheckin.util.Sex
import com.example.healthcheckin.util.ValidationConstants

data class BmrResult(
    val bmrKcal: Int,
    val bmrClamped: Boolean,
)

object BmrCalculator {

    fun calculate(
        sex: Sex,
        weightKg: Double,
        heightCm: Double,
        ageYears: Int,
    ): BmrResult {
        val raw = when (sex) {
            Sex.MALE -> 10.0 * weightKg + 6.25 * heightCm - 5.0 * ageYears + 5.0
            Sex.FEMALE -> 10.0 * weightKg + 6.25 * heightCm - 5.0 * ageYears - 161.0
        }
        val rounded = PrecisionUtil.roundInt(raw)
        val clamped = rounded < ValidationConstants.BMR_MIN_KCAL
        return BmrResult(
            bmrKcal = if (clamped) ValidationConstants.BMR_MIN_KCAL else rounded,
            bmrClamped = clamped,
        )
    }
}
