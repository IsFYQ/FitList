package com.example.healthcheckin.domain.algorithm

import com.example.healthcheckin.util.DateTimeUtil
import com.example.healthcheckin.util.InventoryExpiryStatus
import com.example.healthcheckin.util.P1ValidationConstants
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.abs

object InventoryExpiryEvaluator {
    data class ExpiryInfo(
        val status: InventoryExpiryStatus,
        val daysStored: Int,
        val daysLeft: Int?,
        val label: String?,
    )

    fun evaluate(purchaseDate: String, expiryDate: String?, today: String = DateTimeUtil.todayLocalDateString()): ExpiryInfo {
        val todayDate = DateTimeUtil.parseLocalDateOrNull(today) ?: DateTimeUtil.todayLocalDate()
        val purchase = DateTimeUtil.parseLocalDateOrNull(purchaseDate) ?: todayDate
        val daysStored = ChronoUnit.DAYS.between(purchase, todayDate).toInt().coerceAtLeast(0)

        val expiry = expiryDate?.let { DateTimeUtil.parseLocalDateOrNull(it) }
        if (expiry == null) {
            return ExpiryInfo(
                status = InventoryExpiryStatus.NORMAL,
                daysStored = daysStored,
                daysLeft = null,
                label = null,
            )
        }

        val daysLeft = ChronoUnit.DAYS.between(todayDate, expiry).toInt()
        return when {
            daysLeft < 0 -> ExpiryInfo(
                status = InventoryExpiryStatus.EXPIRED,
                daysStored = daysStored,
                daysLeft = daysLeft,
                label = "已过期${abs(daysLeft)} 天",
            )
            daysLeft == 0 -> ExpiryInfo(
                status = InventoryExpiryStatus.NEAR_EXPIRY,
                daysStored = daysStored,
                daysLeft = 0,
                label = "今天到期",
            )
            daysLeft <= P1ValidationConstants.NEAR_EXPIRY_DAYS -> ExpiryInfo(
                status = InventoryExpiryStatus.NEAR_EXPIRY,
                daysStored = daysStored,
                daysLeft = daysLeft,
                label = "${daysLeft} 天后过期",
            )
            else -> ExpiryInfo(
                status = InventoryExpiryStatus.NORMAL,
                daysStored = daysStored,
                daysLeft = daysLeft,
                label = null,
            )
        }
    }
}

object MilestoneEvaluator {
    data class Achievement(
        val milestoneId: String,
        val title: String,
        val achievedWeightKg: Double,
        val daysElapsed: Int,
        val rewardText: String?,
        val targetWeightKg: Double,
    )

    fun evaluate(
        weightKg: Double,
        localDate: String,
        nowEpochMillis: Long,
        isGain: Boolean,
        activeMilestones: List<MilestoneCandidate>,
        initialWeightKg: Double?,
    ): List<Achievement> {
        val hits = activeMilestones.filter { m ->
            if (isGain) weightKg >= m.targetWeightKg else weightKg <= m.targetWeightKg
        }
        if (hits.isEmpty()) return emptyList()

        val start = initialWeightKg ?: weightKg
        val ordered = hits.sortedBy { abs(it.targetWeightKg - start) }
        return ordered.map { m ->
            val createdDate = DateTimeUtil.toLocalDateString(m.createdAt)
            val days = ChronoUnit.DAYS.between(LocalDate.parse(createdDate), LocalDate.parse(localDate)).toInt().coerceAtLeast(0)
            Achievement(
                milestoneId = m.id,
                title = m.title,
                achievedWeightKg = weightKg,
                daysElapsed = days,
                rewardText = m.rewardText,
                targetWeightKg = m.targetWeightKg,
            )
        }.also {
            // stamp time used by caller
            @Suppress("UNUSED_EXPRESSION")
            nowEpochMillis
        }
    }

    data class MilestoneCandidate(
        val id: String,
        val title: String,
        val targetWeightKg: Double,
        val rewardText: String?,
        val createdAt: Long,
    )
}

object InventoryUnitConverter {
    private val knownInventoryUnits = setOf("G", "KG", "ML", "L", "PIECE")

    /**
     * Meal foods are recorded in g or ml; inventory may use kg / L / 个.
     * g and ml are treated as 1:1 so logging is never blocked by mixed units.
     */
    fun dimensionsCompatible(foodBasisUnit: String, inventoryUnit: String): Boolean {
        if (foodBasisUnit != "G" && foodBasisUnit != "ML") return false
        return inventoryUnit in knownInventoryUnits
    }

    fun canConvert(unit: String, pieceGrams: Double?): Boolean = when (unit) {
        "G", "KG", "ML", "L" -> true
        "PIECE" -> pieceGrams != null && pieceGrams > 0
        else -> false
    }

    fun toBasis(quantity: Double, unit: String, pieceGrams: Double?): Double =
        com.example.healthcheckin.util.UnitConverter.inventoryToBasisAmount(quantity, unit, pieceGrams)

    fun fromBasis(basisAmount: Double, unit: String, pieceGrams: Double?): Double? {
        val factor = when (unit) {
            "G", "ML" -> 1.0
            "KG", "L" -> 1000.0
            "PIECE" -> {
                if (pieceGrams == null || pieceGrams <= 0) return null
                pieceGrams
            }
            else -> return null
        }
        return basisAmount / factor
    }
}
