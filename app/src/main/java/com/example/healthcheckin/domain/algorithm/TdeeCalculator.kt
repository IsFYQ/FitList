package com.example.healthcheckin.domain.algorithm

import com.example.healthcheckin.util.ActivityLevel
import com.example.healthcheckin.util.PrecisionUtil

object TdeeCalculator {

    fun calculate(bmrKcal: Int, activityLevel: ActivityLevel): Int =
        PrecisionUtil.roundInt(bmrKcal * activityLevel.pal)
}
