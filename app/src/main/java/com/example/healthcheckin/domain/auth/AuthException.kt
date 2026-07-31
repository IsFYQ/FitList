package com.example.healthcheckin.domain.auth

sealed class AuthException(
    message: String? = null,
    cause: Throwable? = null,
) : Exception(message, cause) {
    data object EmailAlreadyRegistered : AuthException()

    data class NeedsLogin(val email: String) : AuthException()

    data object NetworkUnavailable : AuthException()

    data object Timeout : AuthException()

    data object ServerError : AuthException()

    data object InvalidCredentials : AuthException()

    data object SendFailed : AuthException()
}
