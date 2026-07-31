package com.example.healthcheckin.domain.algorithm

import com.example.healthcheckin.util.CalorieState
import com.example.healthcheckin.util.MealSlot
import java.time.LocalTime

object MealSlotInferencer {

    fun infer(localTime: LocalTime): MealSlot {
        val minutes = localTime.hour * 60 + localTime.minute
        return when {
            minutes in (4 * 60) until (10 * 60 + 30) -> MealSlot.BREAKFAST
            minutes in (10 * 60 + 30) until (14 * 60 + 30) -> MealSlot.LUNCH
            minutes in (14 * 60 + 30) until (17 * 60) -> MealSlot.SNACK
            minutes in (17 * 60) until (21 * 60 + 30) -> MealSlot.DINNER
            else -> MealSlot.SNACK
        }
    }
}

object CalorieStateCalculator {

    fun calculate(budget: Int, consumed: Int): Pair<CalorieState, Int> {
        val remaining = budget - consumed
        val ratio = if (budget > 0) remaining.toDouble() / budget else 0.0
        val state = when {
            remaining < 0 -> CalorieState.OVER
            ratio <= 0.15 -> CalorieState.WARN
            else -> CalorieState.NORMAL
        }
        return state to remaining
    }
}
