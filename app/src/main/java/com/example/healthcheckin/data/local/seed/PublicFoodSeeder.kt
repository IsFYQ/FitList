package com.example.healthcheckin.data.local.seed

import com.example.healthcheckin.data.local.dao.PublicFoodDao
import com.example.healthcheckin.data.local.entity.PublicFoodEntity
import com.example.healthcheckin.util.Validators
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PublicFoodSeeder @Inject constructor(
    private val publicFoodDao: PublicFoodDao,
) {
    suspend fun seedIfEmpty() {
        if (publicFoodDao.count() > 0) return
        publicFoodDao.insertAll(PublicFoodCatalog.build())
    }
}

object PublicFoodCatalog {
    private data class FoodSeed(
        val id: String,
        val name: String,
        val basisUnit: String = "G",
        val kcalPer100: Double,
        val proteinPer100: Double? = null,
        val carbPer100: Double? = null,
        val fatPer100: Double? = null,
        val servingGrams: Double? = null,
    )

    fun build(): List<PublicFoodEntity> {
        val now = 1_700_000_000_000L
        return entries.map { seed ->
            PublicFoodEntity(
                id = seed.id,
                source = "PUBLIC",
                name = seed.name,
                nameNormalized = Validators.normalizeFoodName(seed.name),
                basisUnit = seed.basisUnit,
                kcalPer100 = seed.kcalPer100,
                proteinPer100 = seed.proteinPer100,
                carbPer100 = seed.carbPer100,
                fatPer100 = seed.fatPer100,
                servingGrams = seed.servingGrams,
                createdAt = now,
                updatedAt = now,
            )
        }
    }

    // 参考《中国食物成分表》常见值，每 100g
    private val entries = listOf(
        FoodSeed("pub-rice-cooked", "米饭（熟）", kcalPer100 = 116.0, proteinPer100 = 2.6, carbPer100 = 25.9, fatPer100 = 0.3, servingGrams = 150.0),
        FoodSeed("pub-steamed-bun", "馒头", kcalPer100 = 221.0, proteinPer100 = 7.0, carbPer100 = 47.0, fatPer100 = 1.1, servingGrams = 80.0),
        FoodSeed("pub-noodles-wheat", "面条（熟）", kcalPer100 = 110.0, proteinPer100 = 4.0, carbPer100 = 22.0, fatPer100 = 0.5, servingGrams = 200.0),
        FoodSeed("pub-dumpling", "饺子", kcalPer100 = 198.0, proteinPer100 = 7.5, carbPer100 = 24.0, fatPer100 = 8.0, servingGrams = 120.0),
        FoodSeed("pub-congee", "白粥", kcalPer100 = 46.0, proteinPer100 = 1.1, carbPer100 = 9.9, fatPer100 = 0.3, servingGrams = 250.0),
        FoodSeed("pub-fried-rice", "蛋炒饭", kcalPer100 = 188.0, proteinPer100 = 5.6, carbPer100 = 28.0, fatPer100 = 6.0, servingGrams = 200.0),
        FoodSeed("pub-tomato-egg", "番茄炒蛋", kcalPer100 = 86.0, proteinPer100 = 5.8, carbPer100 = 4.5, fatPer100 = 5.5, servingGrams = 150.0),
        FoodSeed("pub-kungpao-chicken", "宫保鸡丁", kcalPer100 = 160.0, proteinPer100 = 14.0, carbPer100 = 8.0, fatPer100 = 8.5, servingGrams = 150.0),
        FoodSeed("pub-braised-pork", "红烧肉", kcalPer100 = 395.0, proteinPer100 = 9.0, carbPer100 = 5.0, fatPer100 = 37.0, servingGrams = 100.0),
        FoodSeed("pub-pepper-pork", "青椒肉丝", kcalPer100 = 145.0, proteinPer100 = 12.0, carbPer100 = 6.0, fatPer100 = 8.0, servingGrams = 150.0),
        FoodSeed("pub-mapo-tofu", "麻婆豆腐", kcalPer100 = 120.0, proteinPer100 = 8.0, carbPer100 = 5.0, fatPer100 = 7.5, servingGrams = 150.0),
        FoodSeed("pub-fish-fragrant-pork", "鱼香肉丝", kcalPer100 = 155.0, proteinPer100 = 11.0, carbPer100 = 10.0, fatPer100 = 8.0, servingGrams = 150.0),
        FoodSeed("pub-sweet-sour-ribs", "糖醋排骨", kcalPer100 = 280.0, proteinPer100 = 15.0, carbPer100 = 18.0, fatPer100 = 16.0, servingGrams = 120.0),
        FoodSeed("pub-stir-fry-vegetables", "清炒时蔬", kcalPer100 = 45.0, proteinPer100 = 2.5, carbPer100 = 5.0, fatPer100 = 2.0, servingGrams = 150.0),
        FoodSeed("pub-cucumber-salad", "拌黄瓜", kcalPer100 = 25.0, proteinPer100 = 1.0, carbPer100 = 3.5, fatPer100 = 1.0, servingGrams = 100.0),
        FoodSeed("pub-soy-milk", "豆浆", basisUnit = "ML", kcalPer100 = 31.0, proteinPer100 = 3.0, carbPer100 = 1.8, fatPer100 = 1.6, servingGrams = 250.0),
        FoodSeed("pub-youtiao", "油条", kcalPer100 = 386.0, proteinPer100 = 6.9, carbPer100 = 51.0, fatPer100 = 17.0, servingGrams = 50.0),
        FoodSeed("pub-baozi", "包子", kcalPer100 = 227.0, proteinPer100 = 7.3, carbPer100 = 36.0, fatPer100 = 5.5, servingGrams = 80.0),
        FoodSeed("pub-apple", "苹果", kcalPer100 = 52.0, proteinPer100 = 0.3, carbPer100 = 13.8, fatPer100 = 0.2, servingGrams = 180.0),
        FoodSeed("pub-banana", "香蕉", kcalPer100 = 89.0, proteinPer100 = 1.1, carbPer100 = 22.8, fatPer100 = 0.3, servingGrams = 120.0),
        FoodSeed("pub-orange", "橙子", kcalPer100 = 47.0, proteinPer100 = 0.9, carbPer100 = 11.8, fatPer100 = 0.1, servingGrams = 150.0),
        FoodSeed("pub-egg-boiled", "水煮蛋", kcalPer100 = 144.0, proteinPer100 = 13.0, carbPer100 = 1.0, fatPer100 = 9.0, servingGrams = 50.0),
        FoodSeed("pub-milk", "牛奶", basisUnit = "ML", kcalPer100 = 54.0, proteinPer100 = 3.0, carbPer100 = 4.8, fatPer100 = 3.2, servingGrams = 250.0),
        FoodSeed("pub-yogurt", "酸奶", basisUnit = "ML", kcalPer100 = 72.0, proteinPer100 = 2.5, carbPer100 = 9.3, fatPer100 = 2.7, servingGrams = 200.0),
        FoodSeed("pub-cola", "可乐", basisUnit = "ML", kcalPer100 = 42.0, proteinPer100 = 0.0, carbPer100 = 10.6, fatPer100 = 0.0, servingGrams = 330.0),
        FoodSeed("pub-chicken-breast", "鸡胸肉（熟）", kcalPer100 = 165.0, proteinPer100 = 31.0, carbPer100 = 0.0, fatPer100 = 3.6, servingGrams = 100.0),
        FoodSeed("pub-beef", "牛肉（瘦）", kcalPer100 = 125.0, proteinPer100 = 20.0, carbPer100 = 0.0, fatPer100 = 4.2, servingGrams = 100.0),
        FoodSeed("pub-pork-lean", "猪瘦肉", kcalPer100 = 143.0, proteinPer100 = 20.3, carbPer100 = 0.0, fatPer100 = 6.2, servingGrams = 100.0),
        FoodSeed("pub-tofu", "北豆腐", kcalPer100 = 81.0, proteinPer100 = 8.1, carbPer100 = 2.0, fatPer100 = 4.8, servingGrams = 150.0),
        FoodSeed("pub-broccoli", "西兰花", kcalPer100 = 34.0, proteinPer100 = 2.8, carbPer100 = 6.6, fatPer100 = 0.4, servingGrams = 150.0),
        FoodSeed("pub-spinach", "菠菜", kcalPer100 = 23.0, proteinPer100 = 2.6, carbPer100 = 3.6, fatPer100 = 0.3, servingGrams = 150.0),
        FoodSeed("pub-potato", "土豆", kcalPer100 = 77.0, proteinPer100 = 2.0, carbPer100 = 17.0, fatPer100 = 0.1, servingGrams = 150.0),
        FoodSeed("pub-sweet-potato", "红薯", kcalPer100 = 86.0, proteinPer100 = 1.6, carbPer100 = 20.0, fatPer100 = 0.1, servingGrams = 150.0),
        FoodSeed("pub-corn", "玉米（熟）", kcalPer100 = 96.0, proteinPer100 = 3.4, carbPer100 = 21.0, fatPer100 = 1.2, servingGrams = 100.0),
        FoodSeed("pub-bread", "全麦面包", kcalPer100 = 246.0, proteinPer100 = 9.0, carbPer100 = 45.0, fatPer100 = 3.5, servingGrams = 40.0),
        FoodSeed("pub-oatmeal", "燕麦片", kcalPer100 = 367.0, proteinPer100 = 15.0, carbPer100 = 66.0, fatPer100 = 6.7, servingGrams = 40.0),
        FoodSeed("pub-peanut", "花生", kcalPer100 = 563.0, proteinPer100 = 24.8, carbPer100 = 21.7, fatPer100 = 44.3, servingGrams = 15.0),
        FoodSeed("pub-almond", "杏仁", kcalPer100 = 578.0, proteinPer100 = 21.2, carbPer100 = 21.6, fatPer100 = 49.9, servingGrams = 15.0),
        FoodSeed("pub-salmon", "三文鱼", kcalPer100 = 208.0, proteinPer100 = 20.0, carbPer100 = 0.0, fatPer100 = 13.0, servingGrams = 100.0),
        FoodSeed("pub-shrimp", "虾仁", kcalPer100 = 93.0, proteinPer100 = 18.6, carbPer100 = 1.5, fatPer100 = 1.0, servingGrams = 100.0),
        FoodSeed("pub-cod", "鳕鱼", kcalPer100 = 82.0, proteinPer100 = 17.8, carbPer100 = 0.0, fatPer100 = 0.7, servingGrams = 100.0),
        FoodSeed("pub-hotpot-beef", "火锅牛肉卷", kcalPer100 = 250.0, proteinPer100 = 18.0, carbPer100 = 0.0, fatPer100 = 19.0, servingGrams = 100.0),
        FoodSeed("pub-lamb-skewer", "烤羊肉串", kcalPer100 = 280.0, proteinPer100 = 18.0, carbPer100 = 2.0, fatPer100 = 22.0, servingGrams = 80.0),
        FoodSeed("pub-fried-chicken", "炸鸡块", kcalPer100 = 290.0, proteinPer100 = 18.0, carbPer100 = 12.0, fatPer100 = 19.0, servingGrams = 100.0),
        FoodSeed("pub-french-fries", "薯条", kcalPer100 = 312.0, proteinPer100 = 3.4, carbPer100 = 41.0, fatPer100 = 15.0, servingGrams = 100.0),
        FoodSeed("pub-latte", "拿铁咖啡", basisUnit = "ML", kcalPer100 = 43.0, proteinPer100 = 2.0, carbPer100 = 4.0, fatPer100 = 2.0, servingGrams = 350.0),
        FoodSeed("pub-green-tea", "绿茶（无糖）", basisUnit = "ML", kcalPer100 = 1.0, proteinPer100 = 0.0, carbPer100 = 0.2, fatPer100 = 0.0, servingGrams = 500.0),
        FoodSeed("pub-beer", "啤酒", basisUnit = "ML", kcalPer100 = 43.0, proteinPer100 = 0.5, carbPer100 = 3.6, fatPer100 = 0.0, servingGrams = 500.0),
        FoodSeed("pub-watermelon", "西瓜", kcalPer100 = 30.0, proteinPer100 = 0.6, carbPer100 = 7.6, fatPer100 = 0.2, servingGrams = 200.0),
        FoodSeed("pub-grape", "葡萄", kcalPer100 = 69.0, proteinPer100 = 0.7, carbPer100 = 18.0, fatPer100 = 0.2, servingGrams = 150.0),
        FoodSeed("pub-pear", "梨", kcalPer100 = 44.0, proteinPer100 = 0.4, carbPer100 = 11.0, fatPer100 = 0.1, servingGrams = 180.0),
    )
}
