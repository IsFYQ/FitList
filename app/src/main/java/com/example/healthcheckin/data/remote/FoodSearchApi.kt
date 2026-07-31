package com.example.healthcheckin.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.GET
import retrofit2.http.Query

interface FoodSearchApi {
    @GET("functions/v1/food-search")
    suspend fun search(
        @Query("q") query: String,
        @Query("page") page: Int = 0,
        @Query("page_size") pageSize: Int = 20,
        @Query("barcode") barcode: String? = null,
    ): FoodSearchResponseDto
}

@Serializable
data class FoodSearchResponseDto(
    val query: String,
    @SerialName("quota_remaining") val quotaRemaining: Int? = null,
    val sources: FoodSearchSourcesDto? = null,
    val items: List<FoodSearchRemoteItemDto> = emptyList(),
    @SerialName("error_code") val errorCode: String? = null,
    val message: String? = null,
)

@Serializable
data class FoodSearchSourcesDto(
    val fatsecret: FoodSearchSourceStatusDto? = null,
    val off: FoodSearchSourceStatusDto? = null,
)

@Serializable
data class FoodSearchSourceStatusDto(
    val status: String,
    val count: Int = 0,
)

@Serializable
data class FoodSearchRemoteItemDto(
    val source: String,
    @SerialName("external_id") val externalId: String,
    val name: String,
    val brand: String? = null,
    @SerialName("basis_unit") val basisUnit: String = "G",
    @SerialName("kcal_per_100") val kcalPer100: Double,
    @SerialName("protein_per_100") val proteinPer100: Double? = null,
    @SerialName("carb_per_100") val carbPer100: Double? = null,
    @SerialName("fat_per_100") val fatPer100: Double? = null,
    @SerialName("serving_name") val servingName: String? = null,
    @SerialName("serving_grams") val servingGrams: Double? = null,
    @SerialName("data_incomplete") val dataIncomplete: Boolean = false,
    val barcode: String? = null,
)

@Serializable
data class FoodSearchCachePayloadDto(
    val items: List<FoodSearchRemoteItemDto>,
    @SerialName("quota_remaining") val quotaRemaining: Int? = null,
)
