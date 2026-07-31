package com.example.healthcheckin.domain.repository

import com.example.healthcheckin.data.auth.SessionData
import kotlinx.coroutines.flow.Flow

sealed class AuthState {
    data object Unauthenticated : AuthState()
    data class Authenticated(
        val userId: String,
        val email: String,
        val emailVerified: Boolean,
    ) : AuthState()
}

interface AuthRepository {
    suspend fun signUp(email: String, password: String): Result<SessionData>
    suspend fun signIn(email: String, password: String): Result<SessionData>
    suspend fun refreshToken(): Result<Unit>
    suspend fun signOut()
    suspend fun sendPasswordResetEmail(email: String): Result<Unit>
    suspend fun verifyRecoveryToken(token: String): Result<com.example.healthcheckin.data.auth.PasswordResetSession>
    suspend fun completePasswordReset(accessToken: String, newPassword: String): Result<Unit>
    suspend fun resendVerificationEmail(): Result<Unit>
    suspend fun changePassword(currentPassword: String, newPassword: String): Result<Unit>
    suspend fun deleteAccount(password: String): Result<Unit>
    fun observeAuthState(): Flow<AuthState>
    suspend fun initializeSession(): SessionData?
    suspend fun needsOnboarding(userId: String): Boolean
}
