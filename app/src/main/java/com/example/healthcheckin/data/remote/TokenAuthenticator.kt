package com.example.healthcheckin.data.remote

import com.example.healthcheckin.data.auth.SessionData
import com.example.healthcheckin.data.auth.SessionManager
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

@Singleton
class TokenAuthenticator @Inject constructor(
    private val sessionManager: SessionManager,
    private val authApiProvider: Provider<AuthApi>,
) : Authenticator {

    override fun authenticate(route: Route?, response: Response): Request? {
        if (responseCount(response) >= 2) return null

        val refreshToken = sessionManager.getRefreshToken() ?: return null

        return runBlocking {
            try {
                val authResponse = authApiProvider.get().refreshToken(
                    body = RefreshTokenRequest(refreshToken)
                )
                val accessToken = authResponse.accessToken ?: return@runBlocking null
                val newRefresh = authResponse.refreshToken ?: refreshToken
                val expiresIn = authResponse.expiresIn ?: 3600L
                val userId = sessionManager.getUserId() ?: return@runBlocking null
                val email = sessionManager.session.value?.email ?: return@runBlocking null

                sessionManager.saveSession(
                    SessionData(
                        accessToken = accessToken,
                        refreshToken = newRefresh,
                        expiresAt = System.currentTimeMillis() + expiresIn * 1000,
                        userId = userId,
                        email = email,
                        emailVerified = sessionManager.session.value?.emailVerified ?: false,
                    )
                )

                response.request.newBuilder()
                    .header("Authorization", "Bearer $accessToken")
                    .build()
            } catch (_: Exception) {
                null
            }
        }
    }

    private fun responseCount(response: Response): Int {
        var count = 1
        var prior = response.priorResponse
        while (prior != null) {
            count++
            prior = prior.priorResponse
        }
        return count
    }
}
