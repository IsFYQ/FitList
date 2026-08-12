package com.example.healthcheckin.data.local.seed

import android.content.Context
import com.example.healthcheckin.data.local.dao.AppSettingDao
import com.example.healthcheckin.data.local.dao.PublicFoodDao
import com.example.healthcheckin.data.local.entity.AppSettingEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PublicFoodSeeder @Inject constructor(
    @ApplicationContext private val context: Context,
    private val publicFoodDao: PublicFoodDao,
    private val appSettingDao: AppSettingDao,
) {
    suspend fun seedIfNeeded() {
        val currentVersion = appSettingDao.get(CATALOG_VERSION_KEY)?.valueJson?.trim('"')
        if (currentVersion == CATALOG_VERSION && publicFoodDao.count() > 0) return

        val catalog = PublicFoodCatalogLoader.load(context)
        publicFoodDao.deleteAll()
        catalog.chunked(INSERT_BATCH_SIZE).forEach { batch ->
            publicFoodDao.insertAll(batch)
        }
        appSettingDao.upsert(
            AppSettingEntity(
                key = CATALOG_VERSION_KEY,
                valueJson = "\"$CATALOG_VERSION\"",
                updatedAt = System.currentTimeMillis(),
            )
        )
    }

    companion object {
        private const val CATALOG_VERSION_KEY = "public_food_catalog_version"
        private const val CATALOG_VERSION = "nutridata-v1"
        private const val INSERT_BATCH_SIZE = 500
    }
}
