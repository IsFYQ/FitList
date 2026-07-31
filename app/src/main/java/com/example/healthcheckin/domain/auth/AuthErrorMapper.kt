package com.example.healthcheckin.domain.auth

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import retrofit2.HttpException
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

object AuthErrorMapper {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    fun fromThrowable(throwable: Throwable): AuthException = when (throwable) {
        is AuthException -> throwable
        is HttpException -> fromHttpException(throwable)
        is SocketTimeoutException -> AuthException.Timeout
        is UnknownHostException -> AuthException.NetworkUnavailable
        is IOException -> AuthException.NetworkUnavailable
        else -> AuthException.ServerError
    }

    fun fromHttpException(exception: HttpException): AuthException {
        val body = exception.response()?.errorBody()?.string().orEmpty()
        val payload = runCatching { json.decodeFromString<SupabaseAuthError>(body) }.getOrNull()

        val errorCode = payload?.errorCode?.lowercase()
            ?: payload?.code?.lowercase()
            ?: payload?.error?.lowercase()
            ?: payload?.msg?.lowercase()

        return when {
            exception.code() == 422 &&
                (errorCode == "user_already_exists" || body.contains("already registered", ignoreCase = true)) -> {
                AuthException.EmailAlreadyRegistered
            }
            exception.code() == 400 &&
                (errorCode == "invalid_grant" || body.contains("invalid login credentials", ignoreCase = true)) -> {
                AuthException.InvalidCredentials
            }
            exception.code() in 500..599 -> AuthException.ServerError
            exception.code() == 408 || body.contains("timeout", ignoreCase = true) -> AuthException.Timeout
            else -> AuthException.ServerError
        }
    }

    @Serializable
    private data class SupabaseAuthError(
        val code: String? = null,
        @SerialName("error_code") val errorCode: String? = null,
        val error: String? = null,
        val msg: String? = null,
    )
}
