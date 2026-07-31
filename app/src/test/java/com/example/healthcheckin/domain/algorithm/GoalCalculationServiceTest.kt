package com.example.healthcheckin.domain.algorithm

import com.example.healthcheckin.util.ActivityLevel
import com.example.healthcheckin.util.Sex
import org.junit.Assert.assertEquals
import org.junit.Test

class GoalCalculationServiceTest {

    @Test
    fun example3_femaleLose_fullPipeline() {
        val result = GoalCalculationService.calculate(
            sex = Sex.FEMALE,
            currentWeightKg = 58.0,
            heightCm = 162.0,
            ageYears = 28,
            targetWeightKg = 55.0,
            targetWeeks = 12,
            activityLevel = ActivityLevel.SEDENTARY,
        )
        assertEquals(1292, result.bmr.bmrKcal)
        assertEquals(1550, result.tdeeKcal)
        assertEquals(-275, result.budget.dailyDeltaKcal)
        assertEquals(1275, result.finalBudgetKcal)
    }
}
