package com.example.healthcheckin.domain.model

import com.example.healthcheckin.util.ActivityLevel
import com.example.healthcheckin.util.Sex

data class OnboardingFormData(
    val sex: Sex? = null,
    val birthYearMonth: String = com.example.healthcheckin.util.ValidationConstants.DEFAULT_BIRTH_YEAR_MONTH,
    val heightCm: String = "",
    val currentWeightKg: String = "",
    val targetWeightKg: String = "",
    val targetWeeks: Int = com.example.healthcheckin.util.ValidationConstants.DEFAULT_TARGET_WEEKS,
    val activityLevel: ActivityLevel = ActivityLevel.LIGHT,
)

data class GoalSaveRequest(
    val sex: Sex,
    val birthYearMonth: String,
    val heightCm: Double,
    val currentWeightKg: Double,
    val targetWeightKg: Double,
    val targetWeeks: Int,
    val activityLevel: ActivityLevel,
    val isFirstTime: Boolean,
    val previousCurrentWeightKg: Double? = null,
)

data class GoalSaveResult(
    val goalId: String,
    val promptWeightRecord: Boolean = false,
    val weightToRecord: Double? = null,
)
