package com.example.healthcheckin.data.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface SupabaseSyncApi {
    @POST("rest/v1/{table}")
    @Headers("Prefer: resolution=merge-duplicates,return=minimal")
    suspend fun upsertRows(
        @Path("table") table: String,
        @Query("on_conflict") onConflict: String = "id",
        @Body body: String,
    ): Response<Unit>

    @GET("rest/v1/{table}")
    suspend fun fetchRows(
        @Path("table") table: String,
        @Query("user_id") userFilter: String,
        @Query("select") select: String = "*",
        @Query("order") order: String = "created_at.asc",
        @Query("limit") limit: Int = 1000,
        @Query("offset") offset: Int = 0,
    ): String
}
