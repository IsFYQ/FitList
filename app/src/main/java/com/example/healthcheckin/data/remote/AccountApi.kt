package com.example.healthcheckin.data.remote

import kotlinx.serialization.Serializable
import retrofit2.Response
import retrofit2.http.POST

interface AccountApi {
    @POST("functions/v1/account-delete")
    suspend fun deleteAccount(): Response<AccountDeleteResponse>
}

@Serializable
data class AccountDeleteResponse(val deleted: Boolean = false)
