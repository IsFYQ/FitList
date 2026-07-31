package com.example.healthcheckin.data.remote

import com.example.healthcheckin.BuildConfig
import com.example.healthcheckin.data.auth.SessionManager
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import okhttp3.Interceptor
import okhttp3.Response
import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query

interface AuthApi {
    @POST("auth/v1/signup")
    suspend fun signUp(
        @Header("apikey") apiKey: String = BuildConfig.SUPABASE_ANON_KEY,
        @Body body: SignUpRequest,
    ): AuthResponse

    @POST("auth/v1/token")
    suspend fun signInWithPassword(
        @Header("apikey") apiKey: String = BuildConfig.SUPABASE_ANON_KEY,
        @Query("grant_type") grantType: String = "password",
        @Body body: SignInRequest,
    ): AuthResponse

    @POST("auth/v1/token")
    suspend fun refreshToken(
        @Header("apikey") apiKey: String = BuildConfig.SUPABASE_ANON_KEY,
        @Query("grant_type") grantType: String = "refresh_token",
        @Body body: RefreshTokenRequest,
    ): AuthResponse

    @GET("auth/v1/user")
    suspend fun getUser(
        @Header("apikey") apiKey: String = BuildConfig.SUPABASE_ANON_KEY,
        @Header("Authorization") authorization: String,
    ): UserResponse

    @POST("auth/v1/recover")
    suspend fun recoverPassword(
        @Header("apikey") apiKey: String = BuildConfig.SUPABASE_ANON_KEY,
        @Body body: RecoverRequest,
    ): ResponseBody

    @POST("auth/v1/verify")
    suspend fun verifyRecovery(
        @Header("apikey") apiKey: String = BuildConfig.SUPABASE_ANON_KEY,
        @Body body: VerifyRecoveryRequest,
    ): AuthResponse

    @POST("auth/v1/logout")
    suspend fun signOutGlobal(
        @Header("apikey") apiKey: String = BuildConfig.SUPABASE_ANON_KEY,
        @Header("Authorization") authorization: String,
    ): ResponseBody

    @POST("auth/v1/resend")
    suspend fun resendVerification(
        @Header("apikey") apiKey: String = BuildConfig.SUPABASE_ANON_KEY,
        @Header("Authorization") authorization: String,
        @Body body: ResendRequest,
    ): ResponseBody

    @retrofit2.http.PUT("auth/v1/user")
    suspend fun updateUser(
        @Header("apikey") apiKey: String = BuildConfig.SUPABASE_ANON_KEY,
        @Header("Authorization") authorization: String,
        @Body body: UpdateUserRequest,
    ): UserResponse
}

@Serializable
data class SignUpRequest(val email: String, val password: String)

@Serializable
data class SignInRequest(val email: String, val password: String)

@Serializable
data class RefreshTokenRequest(
    @SerialName("refresh_token") val refreshToken: String,
)

@Serializable
data class RecoverRequest(
    val email: String,
    @SerialName("redirect_to") val redirectTo: String? = null,
)

@Serializable
data class VerifyRecoveryRequest(
    val type: String = "recovery",
    @SerialName("token_hash") val tokenHash: String,
)

@Serializable
data class ResendRequest(val type: String = "signup", val email: String)

@Serializable
data class UpdateUserRequest(val password: String)

@Serializable
data class AuthResponse(
    @SerialName("access_token") val accessToken: String? = null,
    @SerialName("refresh_token") val refreshToken: String? = null,
    @SerialName("expires_in") val expiresIn: Long? = null,
    val user: UserResponse? = null,
)

@Serializable
data class UserResponse(
    val id: String,
    val email: String? = null,
    @SerialName("email_confirmed_at") val emailConfirmedAt: String? = null,
)

class AuthInterceptor(
    private val sessionManager: SessionManager,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val builder = request.newBuilder()
            .header("apikey", BuildConfig.SUPABASE_ANON_KEY)

        if (shouldAttachSessionToken(request.url.encodedPath, request.url.queryParameter("grant_type"))) {
            sessionManager.getAccessToken()?.let { token ->
                builder.header("Authorization", "Bearer $token")
            }
        }

        return chain.proceed(builder.build())
    }

    private fun shouldAttachSessionToken(path: String, grantType: String?): Boolean {
        if (path.endsWith("/auth/v1/signup")) return false
        if (path.endsWith("/auth/v1/recover")) return false
        if (path.endsWith("/auth/v1/verify")) return false
        if (path.contains("/auth/v1/token") && grantType != null) return false
        return true
    }
}
