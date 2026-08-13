package com.example.healthcheckin.domain.algorithm

import com.example.healthcheckin.util.InventoryCategory
import com.example.healthcheckin.util.InventoryUnit
import com.example.healthcheckin.util.Validators
import kotlin.math.ceil

data class ParsedReceiptLine(
    val name: String,
    val quantity: Double,
    val unit: InventoryUnit,
    val unitPrice: Double?,
    val category: InventoryCategory,
    val rawText: String,
    val pattern: String,
    val needsReview: Boolean = false,
)

data class ReceiptParseResult(
    val lines: List<ParsedReceiptLine>,
    val candidateCount: Int,
    val parseRate: Double,
)

object ReceiptLineParser {

    private val excludeRegex = Regex(
        """^(合计|总计|应收|实收|找零|抹零|优惠|折扣|会员|积分|收银员|流水号|门店|电话|地址|谢谢|欢迎|税号|发票)""",
    )
    private val dateRegex = Regex("""^\d{4}[-/年]\d{1,2}[-/月]\d{1,2}""")
    private val blankRegex = Regex("""^[\s\W]*$""")
    private val promoPrefix = Regex("""^(特价|促销|新品|进口|精选|优质)""")
    private val specSuffix = Regex("""\d+(\.\d+)?\s*(g|kg|ml|L|克|千克|毫升|升)$""", RegexOption.IGNORE_CASE)

    private val p1 = Regex(
        """^(?<name>.+?)\s+(?<qty>\d+(\.\d+)?)\s*(?<unit>kg|g|KG|G|ml|ML|L|升|克|千克|毫升|个|袋|盒|瓶|包|把|斤)\s*(?<price>\d+(\.\d{1,2})?)?$""",
    )
    private val p2 = Regex(
        """^(?<name>.+?)\s+(?<qty>\d+(\.\d+)?)\s*(?<unit>kg|g|KG|G|ml|ML|L|升|克|千克|毫升|个|袋|盒|瓶|包|把|斤)$""",
    )
    private val p3 = Regex("""^(?<name>.+?)\s+(?<price>\d+\.\d{2})$""")
    private val p4 = Regex("""^(?<name>[\u4e00-\u9fa5A-Za-z]{2,})$""")

    fun parse(rawLines: List<String>): ReceiptParseResult {
        val candidates = rawLines.map { it.trim() }.filter { line ->
            line.isNotBlank() &&
                !excludeRegex.containsMatchIn(line) &&
                !dateRegex.containsMatchIn(line) &&
                !blankRegex.matches(line)
        }
        var p1p2Hits = 0
        val parsed = candidates.mapNotNull { line ->
            val match = matchLine(line) ?: return@mapNotNull null
            if (match.pattern == "P1" || match.pattern == "P2") p1p2Hits++
            match
        }
        val rate = if (candidates.isEmpty()) 0.0 else p1p2Hits.toDouble() / candidates.size
        return ReceiptParseResult(parsed, candidates.size, rate)
    }

    private fun matchLine(line: String): ParsedReceiptLine? {
        p1.matchEntire(line)?.let { return buildFromQty(it, "P1") }
        p2.matchEntire(line)?.let { return buildFromQty(it, "P2") }
        p3.matchEntire(line)?.let { m ->
            val name = cleanName(m.groups["name"]!!.value.trim())
            return ParsedReceiptLine(
                name = name,
                quantity = 1.0,
                unit = InventoryUnit.PIECE,
                unitPrice = m.groups["price"]!!.value.toDoubleOrNull(),
                category = guessCategory(name),
                rawText = line,
                pattern = "P3",
            )
        }
        p4.matchEntire(line)?.let { m ->
            val name = cleanName(m.groups["name"]!!.value.trim())
            return ParsedReceiptLine(
                name = name,
                quantity = 1.0,
                unit = InventoryUnit.PIECE,
                unitPrice = null,
                category = guessCategory(name),
                rawText = line,
                pattern = "P4",
            )
        }
        return null
    }

    private fun buildFromQty(m: MatchResult, pattern: String): ParsedReceiptLine {
        val rawName = m.groups["name"]!!.value.trim()
        val name = cleanName(rawName)
        var qty = m.groups["qty"]!!.value.toDoubleOrNull() ?: 1.0
        val unitToken = m.groups["unit"]!!.value
        val (unit, normalizedQty) = normalizeUnit(unitToken, qty)
        val price = m.groups["price"]?.value?.toDoubleOrNull()
        val needsReview = normalizedQty <= 0
        val finalQty = if (needsReview) 1.0 else normalizedQty
        return ParsedReceiptLine(
            name = name,
            quantity = finalQty,
            unit = unit,
            unitPrice = price,
            category = guessCategory(name),
            rawText = m.value,
            pattern = pattern,
            needsReview = needsReview,
        )
    }

    fun cleanName(raw: String): String {
        var name = raw.trim()
        name = promoPrefix.replace(name, "")
        name = specSuffix.replace(name, "").trim()
        return name.ifBlank { raw.trim() }
    }

    fun normalizeUnit(token: String, qty: Double): Pair<InventoryUnit, Double> = when (token.lowercase()) {
        "斤" -> InventoryUnit.G to qty * 500.0
        "g", "克" -> InventoryUnit.G to qty
        "kg", "千克" -> InventoryUnit.KG to qty
        "ml", "毫升" -> InventoryUnit.ML to qty
        "l", "升" -> InventoryUnit.L to qty
        else -> InventoryUnit.PIECE to qty
    }

    fun guessCategory(name: String): InventoryCategory {
        val n = Validators.normalizeFoodName(name)
        return when {
            listOf("鸡", "猪", "牛", "羊", "肉", "鱼", "虾", "蟹").any { n.contains(it) } -> InventoryCategory.MEAT
            listOf("菜", "瓜", "豆", "菇", "葱", "蒜", "芹", "菠", "白", "生").any { n.contains(it) } -> InventoryCategory.VEGETABLE
            listOf("米", "面", "粉", "饭", "馒头", "包").any { n.contains(it) } -> InventoryCategory.STAPLE
            listOf("奶", "乳", "酸奶", "芝士", "奶酪").any { n.contains(it) } -> InventoryCategory.DAIRY
            listOf("油", "盐", "酱", "醋", "糖", "料").any { n.contains(it) } -> InventoryCategory.SEASONING
            else -> InventoryCategory.OTHER
        }
    }

    fun genericProteinAdvice(gapProtein: Double): String {
        if (gapProtein <= 0) return ""
        val chickenGrams = (gapProtein / 31.0 * 100.0).toInt()
        val eggs = ceil(gapProtein / 6.5).toInt()
        return "蛋白质还差 ${gapProtein.toInt()} g，约等于 ${chickenGrams} g 鸡胸肉 或 ${eggs} 个鸡蛋"
    }
}
