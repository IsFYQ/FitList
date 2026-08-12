package com.example.healthcheckin.domain.repository

import com.example.healthcheckin.domain.model.BodyChartRange
import com.example.healthcheckin.domain.model.BodyMeasurementItem
import com.example.healthcheckin.domain.model.BodyMetricSummary
import com.example.healthcheckin.domain.model.SaveBodyMeasurementRequest
import com.example.healthcheckin.util.BodyMetric
import kotlinx.coroutines.flow.Flow

interface BodyMeasurementRepository {
    fun observeSummaries(userId: String): Flow<List<BodyMetricSummary>>
    fun observeMetricHistory(userId: String, metric: BodyMetric, range: BodyChartRange): Flow<List<BodyMeasurementItem>>
    suspend fun save(userId: String, request: SaveBodyMeasurementRequest, overwrite: Boolean): Result<BodyMeasurementItem>
    suspend fun delete(recordId: String): Result<Unit>
    suspend fun getByMetricDate(userId: String, metric: BodyMetric, localDate: String): BodyMeasurementItem?
}
