package com.example.healthcheckin.domain.algorithm

import com.example.healthcheckin.util.GoalType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WeightProgressCalculatorTest {

    @Test
    fun progressPercent_forLoseGoal() {
        val pct = WeightProgressCalculator.progressPercent(
            goalType = GoalType.LOSE,
            initialKg = 70.0,
            targetKg = 60.0,
            latestKg = 65.0,
        )
        assertEquals(50, pct)
    }

    @Test
    fun progressPercent_zeroWhenRegressed() {
        val pct = WeightProgressCalculator.progressPercent(
            goalType = GoalType.LOSE,
            initialKg = 70.0,
            targetKg = 60.0,
            latestKg = 72.0,
        )
        assertEquals(0, pct)
    }

    @Test
    fun progressPercent_nullForMaintain() {
        assertNull(
            WeightProgressCalculator.progressPercent(
                goalType = GoalType.MAINTAIN,
                initialKg = 70.0,
                targetKg = 70.0,
                latestKg = 70.0,
            ),
        )
    }

    @Test
    fun deltaFromPrevious() {
        assertEquals(1.5, WeightProgressCalculator.deltaFromPrevious(68.5, 67.0)!!, 0.01)
        assertNull(WeightProgressCalculator.deltaFromPrevious(68.5, null))
    }
}
