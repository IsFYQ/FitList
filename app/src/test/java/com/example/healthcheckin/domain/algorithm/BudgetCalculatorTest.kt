package com.example.healthcheckin.domain.algorithm

import com.example.healthcheckin.util.ActivityLevel
import com.example.healthcheckin.util.Sex
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * TC-ALG-01: PRD §5.3 example validation table.
 */
class BudgetCalculatorTest {

    @Test
    fun example1_maleLoseLight() {
        val bmr = BmrCalculator.calculate(Sex.MALE, 80.0, 175.0, 30)
        assertEquals(1749, bmr.bmrKcal)

        val tdee = TdeeCalculator.calculate(bmr.bmrKcal, ActivityLevel.LIGHT)
        assertEquals(2405, tdee)

        val budget = BudgetCalculator.calculate(
            sex = Sex.MALE,
            tdeeKcal = tdee,
            currentWeightKg = 80.0,
            targetWeightKg = 70.0,
            targetWeeks = 20,
        )
        assertEquals(-550, budget.dailyDeltaKcal)
        assertEquals(1855, budget.budgetKcal)
        assertEquals(20, budget.estWeeks)
    }

    @Test
    fun example2_maleLoseDoubleClamp() {
        val bmr = BmrCalculator.calculate(Sex.MALE, 80.0, 175.0, 30)
        val tdee = TdeeCalculator.calculate(bmr.bmrKcal, ActivityLevel.LIGHT)
        val budget = BudgetCalculator.calculate(
            sex = Sex.MALE,
            tdeeKcal = tdee,
            currentWeightKg = 80.0,
            targetWeightKg = 70.0,
            targetWeeks = 4,
        )
        assertEquals(-1000, budget.dailyDeltaKcal)
        assertEquals(1500, budget.budgetKcal)
        assertEquals(true, budget.clamped)
        assertEquals(13, budget.estWeeks)
    }

    @Test
    fun example3_femaleLoseSedentary() {
        val bmr = BmrCalculator.calculate(Sex.FEMALE, 58.0, 162.0, 28)
        assertEquals(1292, bmr.bmrKcal)

        val tdee = TdeeCalculator.calculate(bmr.bmrKcal, ActivityLevel.SEDENTARY)
        assertEquals(1550, tdee)

        val budget = BudgetCalculator.calculate(
            sex = Sex.FEMALE,
            tdeeKcal = tdee,
            currentWeightKg = 58.0,
            targetWeightKg = 55.0,
            targetWeeks = 12,
        )
        assertEquals(-275, budget.dailyDeltaKcal)
        assertEquals(1275, budget.budgetKcal)
        assertEquals(false, budget.clamped)
        assertEquals(12, budget.estWeeks)
    }

    @Test
    fun example4_femaleGainModerate() {
        val bmr = BmrCalculator.calculate(Sex.FEMALE, 52.0, 162.0, 28)
        assertEquals(1232, bmr.bmrKcal)

        val tdee = TdeeCalculator.calculate(bmr.bmrKcal, ActivityLevel.MODERATE)
        assertEquals(1910, tdee)

        val budget = BudgetCalculator.calculate(
            sex = Sex.FEMALE,
            tdeeKcal = tdee,
            currentWeightKg = 52.0,
            targetWeightKg = 55.0,
            targetWeeks = 12,
        )
        assertEquals(275, budget.dailyDeltaKcal)
        assertEquals(2185, budget.budgetKcal)
        assertEquals(false, budget.clamped)
    }

    @Test
    fun example5_maleMaintain() {
        val bmr = BmrCalculator.calculate(Sex.MALE, 70.0, 170.0, 45)
        assertEquals(1543, bmr.bmrKcal)

        val tdee = TdeeCalculator.calculate(bmr.bmrKcal, ActivityLevel.MODERATE)
        assertEquals(2392, tdee)

        val budget = BudgetCalculator.calculate(
            sex = Sex.MALE,
            tdeeKcal = tdee,
            currentWeightKg = 70.0,
            targetWeightKg = 70.0,
            targetWeeks = 12,
        )
        assertEquals(0, budget.dailyDeltaKcal)
        assertEquals(2392, budget.budgetKcal)
        assertNull(budget.estWeeks)
    }
}
