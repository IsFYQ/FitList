package com.example.healthcheckin.domain.algorithm

import com.example.healthcheckin.util.InventoryExpiryStatus
import kotlin.math.abs
import kotlin.math.min

data class RecommendationCandidate(
    val inventoryItemId: String,
    val inventoryName: String,
    val foodId: String?,
    val foodName: String,
    val remainingAmount: Double,
    val inventoryUnit: String,
    val basisUnit: String,
    val kcalPer100: Double,
    val proteinPer100: Double,
    val carbPer100: Double,
    val fatPer100: Double,
    val expiryStatus: InventoryExpiryStatus,
    val itemScore: Double,
)

data class RecommendationComboItem(
    val candidate: RecommendationCandidate,
    val portionBasis: Double,
    val kcal: Double,
    val proteinG: Double,
    val carbG: Double,
    val fatG: Double,
)

data class RecommendationCombo(
    val items: List<RecommendationComboItem>,
    val totalKcal: Double,
    val comboScore: Double,
)

data class NutritionGap(
    val budgetKcal: Int,
    val consumedKcal: Double,
    val gapKcal: Double,
    val gapProtein: Double,
    val gapCarb: Double,
    val gapFat: Double,
    val targetProtein: Double,
    val targetCarb: Double,
    val targetFat: Double,
)

enum class RecommendationFallback {
    NONE,
    LOW_GAP,
    EMPTY_INVENTORY,
    NO_NUTRITION,
    NO_COMBO,
    ALL_MET,
}

data class RecommendationResult(
    val gap: NutritionGap,
    val combos: List<RecommendationCombo>,
    val alternateCombos: List<RecommendationCombo>,
    val topSingles: List<RecommendationCandidate>,
    val fallback: RecommendationFallback,
    val genericAdvice: String? = null,
    val mostlyExpired: Boolean = false,
)

object NutritionRecommendationEngine {

    private const val EPS = 0.001
    private const val MAX_CANDIDATES = 30
    private const val TOP_FOR_COMBO = 8
    private const val MAX_BASIS = 500.0

    fun compute(
        budgetKcal: Int,
        consumedKcal: Double,
        consumedProtein: Double,
        consumedCarb: Double,
        consumedFat: Double,
        targetProtein: Double,
        targetCarb: Double,
        targetFat: Double,
        candidates: List<RecommendationCandidate>,
    ): RecommendationResult {
        val gap = NutritionGap(
            budgetKcal = budgetKcal,
            consumedKcal = consumedKcal,
            gapKcal = maxOf(budgetKcal - consumedKcal, 0.0),
            gapProtein = maxOf(targetProtein - consumedProtein, 0.0),
            gapCarb = maxOf(targetCarb - consumedCarb, 0.0),
            gapFat = maxOf(targetFat - consumedFat, 0.0),
            targetProtein = targetProtein,
            targetCarb = targetCarb,
            targetFat = targetFat,
        )
        if (gap.gapProtein <= EPS && gap.gapCarb <= EPS && gap.gapFat <= EPS) {
            return RecommendationResult(gap, emptyList(), emptyList(), emptyList(), RecommendationFallback.ALL_MET)
        }
        if (gap.gapKcal < 100) {
            return RecommendationResult(gap, emptyList(), emptyList(), emptyList(), RecommendationFallback.LOW_GAP)
        }
        if (candidates.isEmpty()) {
            return RecommendationResult(
                gap,
                emptyList(),
                emptyList(),
                emptyList(),
                RecommendationFallback.EMPTY_INVENTORY,
                genericAdvice = ReceiptLineParser.genericProteinAdvice(gap.gapProtein),
            )
        }
        val scored = candidates
            .sortedWith(
                compareByDescending<RecommendationCandidate> {
                    if (it.expiryStatus == InventoryExpiryStatus.NEAR_EXPIRY) 0 else 1
                }.thenBy { it.expiryStatus.ordinal }
                    .thenByDescending { it.itemScore },
            )
            .take(MAX_CANDIDATES)
            .map { scoreItem(it, gap) }
            .sortedByDescending { it.itemScore }

        if (scored.isEmpty()) {
            return RecommendationResult(
                gap,
                emptyList(),
                emptyList(),
                emptyList(),
                RecommendationFallback.NO_NUTRITION,
                genericAdvice = ReceiptLineParser.genericProteinAdvice(gap.gapProtein),
            )
        }

        val top8 = scored.take(TOP_FOR_COMBO)
        val allCombos = buildCombos(top8, gap)
        if (allCombos.isEmpty()) {
            return RecommendationResult(
                gap,
                emptyList(),
                emptyList(),
                scored.take(5),
                RecommendationFallback.NO_COMBO,
            )
        }
        val top3 = pickDistinctTop3(allCombos)
        val rest = allCombos.filter { combo -> top3.none { sameCombo(it, combo) } }
        val mostlyExpired = candidates.count { it.expiryStatus == InventoryExpiryStatus.EXPIRED } > candidates.size / 2
        return RecommendationResult(
            gap = gap,
            combos = top3,
            alternateCombos = rest,
            topSingles = scored.take(5),
            fallback = RecommendationFallback.NONE,
            mostlyExpired = mostlyExpired,
        )
    }

    private fun scoreItem(item: RecommendationCandidate, gap: NutritionGap): RecommendationCandidate {
        val sum = gap.gapProtein + gap.gapCarb + gap.gapFat + EPS
        val wProtein = gap.gapProtein / sum
        val wCarb = gap.gapCarb / sum
        val wFat = gap.gapFat / sum
        val nutrientScore =
            wProtein * min(item.proteinPer100 / 30.0, 1.0) +
                wCarb * min(item.carbPer100 / 50.0, 1.0) +
                wFat * min(item.fatPer100 / 20.0, 1.0)
        val expiryBonus = if (item.expiryStatus == InventoryExpiryStatus.NEAR_EXPIRY) 0.15 else 0.0
        return item.copy(itemScore = 0.85 * nutrientScore + expiryBonus)
    }

    private fun nutrientsForPortion(item: RecommendationCandidate, portion: Double): RecommendationComboItem {
        val factor = portion / 100.0
        return RecommendationComboItem(
            candidate = item,
            portionBasis = portion,
            kcal = item.kcalPer100 * factor,
            proteinG = item.proteinPer100 * factor,
            carbG = item.carbPer100 * factor,
            fatG = item.fatPer100 * factor,
        )
    }

    private fun buildCombos(candidates: List<RecommendationCandidate>, gap: NutritionGap): List<RecommendationCombo> {
        val combos = mutableListOf<RecommendationCombo>()
        for (size in 1..3) {
            combine(candidates, size).forEach { group ->
                buildPortions(group, gap)?.let { combos.add(it) }
            }
        }
        return combos.sortedByDescending { it.comboScore }
    }

    private fun buildPortions(group: List<RecommendationCandidate>, gap: NutritionGap): RecommendationCombo? {
        val items = group.map { nutrientsForPortion(it, 100.0) }.toMutableList()
        val maxPortion = group.map { min(it.remainingAmount, MAX_BASIS) }
        var guard = 0
        while (guard++ < 200) {
            val totalKcal = items.sumOf { it.kcal }
            if (totalKcal in gap.gapKcal * 0.8..gap.gapKcal * 1.0) {
                return scoreCombo(items, gap)
            }
            if (totalKcal > gap.gapKcal * 1.0) break
            val idx = items.indices.maxByOrNull { group[it].itemScore } ?: break
            val next = items[idx].portionBasis + 50.0
            if (next > maxPortion[idx]) break
            items[idx] = nutrientsForPortion(group[idx], next)
        }
        return null
    }

    private fun scoreCombo(items: List<RecommendationComboItem>, gap: NutritionGap): RecommendationCombo {
        val totalKcal = items.sumOf { it.kcal }
        val protein = items.sumOf { it.proteinG }
        val carb = items.sumOf { it.carbG }
        val fat = items.sumOf { it.fatG }
        val gapSum = gap.gapProtein + gap.gapCarb + gap.gapFat + EPS
        val fillRate = (
            min(protein, gap.gapProtein) +
                min(carb, gap.gapCarb) +
                min(fat, gap.gapFat)
            ) / gapSum
        val kcalScore = 1.0 - abs(totalKcal - gap.gapKcal) / maxOf(gap.gapKcal, 1.0)
        val expiryRatio = items.count { it.candidate.expiryStatus == InventoryExpiryStatus.NEAR_EXPIRY }.toDouble() /
            items.size.coerceAtLeast(1)
        val comboScore = 0.50 * kcalScore + 0.35 * fillRate + 0.15 * expiryRatio
        return RecommendationCombo(items, totalKcal, comboScore)
    }

    private fun pickDistinctTop3(all: List<RecommendationCombo>): List<RecommendationCombo> {
        val picked = mutableListOf<RecommendationCombo>()
        for (combo in all) {
            if (picked.any { overlap(it, combo) }) continue
            picked.add(combo)
            if (picked.size == 3) break
        }
        return picked
    }

    private fun overlap(a: RecommendationCombo, b: RecommendationCombo): Boolean {
        val idsA = a.items.map { it.candidate.inventoryItemId }.toSet()
        val idsB = b.items.map { it.candidate.inventoryItemId }.toSet()
        return idsA == idsB
    }

    private fun sameCombo(a: RecommendationCombo, b: RecommendationCombo): Boolean =
        a.items.map { it.candidate.inventoryItemId to it.portionBasis } ==
            b.items.map { it.candidate.inventoryItemId to it.portionBasis }

    private fun <T> combine(items: List<T>, size: Int): List<List<T>> {
        if (size <= 0 || items.size < size) return emptyList()
        val result = mutableListOf<List<T>>()
        fun backtrack(start: Int, current: MutableList<T>) {
            if (current.size == size) {
                result.add(current.toList())
                return
            }
            for (i in start..items.lastIndex) {
                current.add(items[i])
                backtrack(i + 1, current)
                current.removeAt(current.lastIndex)
            }
        }
        backtrack(0, mutableListOf())
        return result
    }
}
