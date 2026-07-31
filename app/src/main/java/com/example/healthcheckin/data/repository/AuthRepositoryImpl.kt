package com.example.healthcheckin.data.repository

import com.example.healthcheckin.data.auth.SessionData
import com.example.healthcheckin.data.auth.SessionManager
import com.example.healthcheckin.data.local.dao.AppSettingDao
import com.example.healthcheckin.data.local.dao.ProfileDao
import com.example.healthcheckin.data.local.dao.SyncQueueDao
import com.example.healthcheckin.data.local.entity.AppSettingEntity
import com.example.healthcheckin.data.local.entity.ProfileEntity
import com.example.healthcheckin.data.local.entity.SyncQueueEntity
import com.example.healthcheckin.data.local.HealthDatabase
import com.example.healthcheckin.data.remote.AccountApi
import com.example.healthcheckin.data.remote.AuthApi
import com.example.healthcheckin.data.remote.VerifyRecoveryRequest
import com.example.healthcheckin.data.remote.RecoverRequest
import com.example.healthcheckin.data.remote.RefreshTokenRequest
import com.example.healthcheckin.data.remote.ResendRequest
import com.example.healthcheckin.data.remote.SignInRequest
import com.example.healthcheckin.data.remote.SignUpRequest
import com.example.healthcheckin.data.remote.UpdateUserRequest
import com.example.healthcheckin.domain.analytics.AnalyticsEvents
import com.example.healthcheckin.domain.analytics.AnalyticsTracker
import com.example.healthcheckin.domain.auth.AuthErrorMapper
import com.example.healthcheckin.domain.auth.AuthException
import com.example.healthcheckin.domain.repository.AuthRepository
import com.example.healthcheckin.domain.repository.AuthState
import com.example.healthcheckin.util.DateTimeUtil
import com.example.healthcheckin.util.SyncState
import com.example.healthcheckin.util.UuidV7
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val authApi: AuthApi,
    private val sessionManager: SessionManager,
    private val profileDao: ProfileDao,
    private val syncQueueDao: SyncQueueDao,
    private val appSettingDao: AppSettingDao,
    private val deviceId: String,
    private val json: Json,
    private val analyticsTracker: AnalyticsTracker,
    private val database: HealthDatabase,
    private val accountApi: AccountApi,
) : AuthRepository {

    override suspend fun signUp(email: String, password: String): Result<SessionData> {
        return try {
            val signUpResponse = authApi.signUp(body = SignUpRequest(email, password))
            val authResponse = if (signUpResponse.accessToken != null) {
                signUpResponse
            } else {
                try {
                    authApi.signInWithPassword(body = SignInRequest(email, password))
                } catch (signInError: Exception) {
                    saveLastLoginEmail(email)
                    throw AuthException.NeedsLogin(email).apply { initCause(signInError) }
                }
            }
            val session = authResponse.toSessionData(email)
            sessionManager.saveSession(session)
            createProfileIfNeeded(session)
            saveLastLoginEmail(email)
            analyticsTracker.onUserLoggedIn(session.userId)
            analyticsTracker.track(AnalyticsEvents.SIGN_UP_SUCCEEDED)
            analyticsTracker.track(AnalyticsEvents.SIGN_IN_SUCCEEDED, mapOf("is_auto" to false))
            Result.success(session)
        } catch (error: AuthException) {
            Result.failure(error)
        } catch (error: Exception) {
            Result.failure(AuthErrorMapper.fromThrowable(error))
        }
    }

    override suspend fun signIn(email: String, password: String): Result<SessionData> = runCatching {
        val response = authApi.signInWithPassword(body = SignInRequest(email, password))
        val session = response.toSessionData(email)
        sessionManager.saveSession(session)
        createProfileIfNeeded(session)
        saveLastLoginEmail(email)
        analyticsTracker.onUserLoggedIn(session.userId)
        analyticsTracker.track(AnalyticsEvents.SIGN_IN_SUCCEEDED, mapOf("is_auto" to false))
        session
    }

    override suspend fun refreshToken(): Result<Unit> = runCatching {
        val refreshToken = sessionManager.getRefreshToken()
            ?: throw IllegalStateException("No refresh token")
        val response = authApi.refreshToken(body = RefreshTokenRequest(refreshToken))
        val accessToken = response.accessToken ?: throw IllegalStateException("No access token")
        val current = sessionManager.session.value
            ?: throw IllegalStateException("No session")
        sessionManager.saveSession(
            current.copy(
                accessToken = accessToken,
                refreshToken = response.refreshToken ?: refreshToken,
                expiresAt = System.currentTimeMillis() + (response.expiresIn ?: 3600L) * 1000,
            )
        )
    }

    override suspend fun signOut() {
        sessionManager.clearSession()
    }

    override suspend fun sendPasswordResetEmail(email: String): Result<Unit> {
        return try {
            authApi.recoverPassword(
                body = RecoverRequest(
                    email = email,
                    redirectTo = "healthcheckin://reset-password",
                ),
            ).close()
            analyticsTracker.track(AnalyticsEvents.PASSWORD_RESET_REQUESTED)
            Result.success(Unit)
        } catch (error: Exception) {
            Result.failure(AuthErrorMapper.fromThrowable(error))
        }
    }

    override suspend fun verifyRecoveryToken(token: String): Result<com.example.healthcheckin.data.auth.PasswordResetSession> =
        runCatching {
            val response = authApi.verifyRecovery(body = VerifyRecoveryRequest(tokenHash = token))
            val accessToken = response.accessToken ?: throw IllegalStateException("missing_token")
            com.example.healthcheckin.data.auth.PasswordResetSession(
                accessToken = accessToken,
                refreshToken = response.refreshToken,
            )
        }

    override suspend fun completePasswordReset(accessToken: String, newPassword: String): Result<Unit> =
        runCatching {
            authApi.updateUser(
                authorization = "Bearer $accessToken",
                body = UpdateUserRequest(password = newPassword),
            )
            runCatching { authApi.signOutGlobal(authorization = "Bearer $accessToken").close() }
            sessionManager.clearSession()
            analyticsTracker.track(AnalyticsEvents.PASSWORD_RESET_COMPLETED)
        }

    override suspend fun resendVerificationEmail(): Result<Unit> = runCatching {
        val session = sessionManager.session.value ?: throw IllegalStateException("Not logged in")
        authApi.resendVerification(
            authorization = "Bearer ${session.accessToken}",
            body = ResendRequest(email = session.email),
        ).close()
    }

    override suspend fun changePassword(currentPassword: String, newPassword: String): Result<Unit> =
        runCatching {
            val session = sessionManager.session.value ?: throw IllegalStateException("Not logged in")
            authApi.signInWithPassword(
                body = SignInRequest(email = session.email, password = currentPassword),
            )
            authApi.updateUser(
                authorization = "Bearer ${session.accessToken}",
                body = UpdateUserRequest(password = newPassword),
            )
            analyticsTracker.track(AnalyticsEvents.PASSWORD_CHANGED)
        }

    override suspend fun deleteAccount(password: String): Result<Unit> {
        val session = sessionManager.session.value
            ?: return Result.failure(IllegalStateException("Not logged in"))
        val passwordOk = runCatching {
            authApi.signInWithPassword(
                body = SignInRequest(email = session.email, password = password),
            )
        }.isSuccess
        if (!passwordOk) {
            return Result.failure(IllegalStateException("wrong_password"))
        }
        return runCatching {
            val response = accountApi.deleteAccount()
            if (!response.isSuccessful) {
                throw IllegalStateException("account_delete_failed")
            }
            database.clearAllTables()
            syncQueueDao.deleteAll()
            sessionManager.clearSession()
            analyticsTracker.track(AnalyticsEvents.ACCOUNT_DELETED)
        }
    }

    override fun observeAuthState(): Flow<AuthState> =
        sessionManager.isLoggedIn.map { loggedIn ->
            if (!loggedIn) {
                AuthState.Unauthenticated
            } else {
                val session = sessionManager.session.value
                if (session == null) {
                    AuthState.Unauthenticated
                } else {
                    AuthState.Authenticated(
                        userId = session.userId,
                        email = session.email,
                        emailVerified = session.emailVerified,
                    )
                }
            }
        }

    override suspend fun initializeSession(): SessionData? {
        val session = sessionManager.loadSession() ?: return null
        if (sessionManager.needsRefresh()) {
            runCatching { refreshToken() }
        }
        val active = sessionManager.session.value ?: return null
        analyticsTracker.onUserLoggedIn(active.userId)
        analyticsTracker.track(
            AnalyticsEvents.SIGN_IN_SUCCEEDED,
            mapOf("is_auto" to true),
        )
        return active
    }

    override suspend fun needsOnboarding(userId: String): Boolean {
        val profile = profileDao.getById(userId)
        return profile?.onboardingCompletedAt == null
    }

    private suspend fun createProfileIfNeeded(session: SessionData) {
        val existing = profileDao.getById(session.userId)
        if (existing != null) return
        val now = DateTimeUtil.nowEpochMillis()
        profileDao.insert(
            ProfileEntity(
                id = session.userId,
                userId = session.userId,
                email = session.email,
                registeredLocalDate = DateTimeUtil.todayLocalDateString(),
                deviceId = deviceId,
                createdAt = now,
                updatedAt = now,
                syncState = SyncState.PENDING,
            )
        )
        syncQueueDao.insert(
            SyncQueueEntity(
                id = UuidV7.generate(),
                tableName = "profiles",
                rowId = session.userId,
                operation = "UPSERT",
                createdAt = now,
                updatedAt = now,
            )
        )
    }

    private suspend fun saveLastLoginEmail(email: String) {
        appSettingDao.upsert(
            AppSettingEntity(
                key = "last_login_email",
                valueJson = "\"$email\"",
                updatedAt = DateTimeUtil.nowEpochMillis(),
            )
        )
    }

    private fun com.example.healthcheckin.data.remote.AuthResponse.toSessionData(
        fallbackEmail: String,
    ): SessionData {
        val accessToken = accessToken ?: throw IllegalStateException("Missing access token")
        val refreshToken = refreshToken ?: throw IllegalStateException("Missing refresh token")
        val user = user ?: throw IllegalStateException("Missing user")
        return SessionData(
            accessToken = accessToken,
            refreshToken = refreshToken,
            expiresAt = System.currentTimeMillis() + (expiresIn ?: 3600L) * 1000,
            userId = user.id,
            email = user.email ?: fallbackEmail,
            emailVerified = user.emailConfirmedAt != null,
        )
    }
}
