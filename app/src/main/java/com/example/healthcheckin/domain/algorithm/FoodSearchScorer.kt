package com.example.healthcheckin.domain.algorithm

import com.example.healthcheckin.domain.model.FoodSearchItem
import com.example.healthcheckin.util.FoodSource
import com.example.healthcheckin.util.Validators
import java.text.Collator
import java.util.Locale
import kotlin.math.min

object FoodSearchScorer {

    private val collator: Collator = Collator.getInstance(Locale.CHINA).apply {
        strength = Collator.PRIMARY
    }

    fun score(query: String, item: FoodSearchItem, useCount30d: Int = 0): Double {
        val queryNorm = Validators.normalizeFoodName(query)
        val nameNorm = Validators.normalizeFoodName(item.name)
        val nameMatch = nameMatch(queryNorm, nameNorm)
        val sourceWeight = sourceWeight(item.source)
        val personalRecency = min(useCount30d / 5.0, 1.0)
        val dataCompleteness = dataCompleteness(item)
        return 0.45 * nameMatch +
            0.25 * sourceWeight +
            0.20 * personalRecency +
            0.10 * dataCompleteness
    }

    fun nameMatch(queryNormalized: String, nameNormalized: String): Double {
        if (queryNormalized.isEmpty() || nameNormalized.isEmpty()) return 0.0
        if (nameNormalized == queryNormalized) return 1.0
        if (nameNormalized.startsWith(queryNormalized)) return 0.80
        if (nameNormalized.contains(queryNormalized)) return 0.60
        val tokens = tokenize(queryNormalized)
        if (tokens.isEmpty()) return 0.0
        val hitCount = tokens.count { nameNormalized.contains(it) }
        if (hitCount == tokens.size) return 0.50
        if (hitCount > 0) return 0.30 * (hitCount.toDouble() / tokens.size)
        return 0.0
    }

    fun sourceWeight(source: FoodSource): Double = when (source) {
        FoodSource.CUSTOM -> 1.0
        FoodSource.PUBLIC -> 0.85
        FoodSource.FATSECRET -> 0.70
        FoodSource.OFF -> 0.40
    }

    fun dataCompleteness(item: FoodSearchItem): Double {
        val fields = listOf(
            item.kcalPer100,
            item.proteinPer100,
            item.carbPer100,
            item.fatPer100,
        )
        val present = fields.count { it != null }
        var base = when (present) {
            4 -> 1.0
            3 -> 0.60
            else -> 0.20
        }
        if (item.dataIncomplete) base *= 0.5
        return base
    }

    fun sortComparator(query: String): Comparator<FoodSearchItem> {
        return compareByDescending<FoodSearchItem> { it.score }
            .thenByDescending { it.lastUsedAt ?: 0L }
            .thenBy(collator) { it.name }
    }

    fun withScore(query: String, item: FoodSearchItem, useCount30d: Int = 0): FoodSearchItem =
        item.copy(score = score(query, item, useCount30d))

    private fun tokenize(query: String): List<String> {
        if (query.contains(' ')) {
            return query.split(' ').filter { it.isNotEmpty() }
        }
        return query.map { it.toString() }.filter { it.isNotBlank() }
    }
}

object FoodSearchMerger {

    fun dedupeKey(name: String, brand: String?): String =
        Validators.normalizeFoodName(name) + "|" + Validators.normalizeFoodName(brand.orEmpty())

    fun dedupeKey(item: FoodSearchItem): String = dedupeKey(item.name, item.brand)

    fun mergeByScore(query: String, items: List<FoodSearchItem>): List<FoodSearchItem> {
        val byKey = linkedMapOf<String, FoodSearchItem>()
        items.forEach { item ->
            val key = dedupeKey(item)
            val existing = byKey[key]
            if (existing == null || item.score > existing.score) {
                byKey[key] = item
            }
        }
        return byKey.values.sortedWith(FoodSearchScorer.sortComparator(query))
    }

    /** F-04: append remote items to tail without reordering existing rows. */
    fun appendRemote(existing: List<FoodSearchItem>, remote: List<FoodSearchItem>): List<FoodSearchItem> {
        val existingKeys = existing.map { dedupeKey(it) }.toSet()
        val toAppend = remote.filter { dedupeKey(it) !in existingKeys }
        return existing + toAppend
    }
}
