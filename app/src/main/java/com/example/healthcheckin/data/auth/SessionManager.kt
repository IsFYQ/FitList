package com.example.healthcheckin.data.auth

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

data class SessionData(
    val accessToken: String,
    val refreshToken: String,
    val expiresAt: Long,
    val userId: String,
    val email: String,
    val emailVerified: Boolean = false,
)

@Singleton
class SessionManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val prefs: SharedPreferences? by lazy { createEncryptedPrefs() }
    private val memorySession = MutableStateFlow<SessionData?>(null)
    private val loggedInFlow = MutableStateFlow(false)

    val session: StateFlow<SessionData?> = memorySession.asStateFlow()
    val isLoggedIn: StateFlow<Boolean> = loggedInFlow.asStateFlow()

    private fun createEncryptedPrefs(): SharedPreferences? = try {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    } catch (_: Exception) {
        null
    }

    fun saveSession(data: SessionData) {
        memorySession.value = data
        loggedInFlow.value = true
        prefs?.edit()?.apply {
            putString(KEY_ACCESS_TOKEN, data.accessToken)
            putString(KEY_REFRESH_TOKEN, data.refreshToken)
            putLong(KEY_EXPIRES_AT, data.expiresAt)
            putString(KEY_USER_ID, data.userId)
            putString(KEY_EMAIL, data.email)
            putBoolean(KEY_EMAIL_VERIFIED, data.emailVerified)
        }?.apply()
    }

    fun loadSession(): SessionData? {
        val stored = prefs ?: return memorySession.value
        val accessToken = stored.getString(KEY_ACCESS_TOKEN, null) ?: return null
        val refreshToken = stored.getString(KEY_REFRESH_TOKEN, null) ?: return null
        val userId = stored.getString(KEY_USER_ID, null) ?: return null
        val email = stored.getString(KEY_EMAIL, null) ?: return null
        val data = SessionData(
            accessToken = accessToken,
            refreshToken = refreshToken,
            expiresAt = stored.getLong(KEY_EXPIRES_AT, 0L),
            userId = userId,
            email = email,
            emailVerified = stored.getBoolean(KEY_EMAIL_VERIFIED, false),
        )
        memorySession.value = data
        loggedInFlow.value = true
        return data
    }

    fun getAccessToken(): String? =
        memorySession.value?.accessToken ?: prefs?.getString(KEY_ACCESS_TOKEN, null)

    fun getRefreshToken(): String? =
        memorySession.value?.refreshToken ?: prefs?.getString(KEY_REFRESH_TOKEN, null)

    fun getUserId(): String? =
        memorySession.value?.userId ?: prefs?.getString(KEY_USER_ID, null)

    fun needsRefresh(): Boolean {
        val expiresAt = memorySession.value?.expiresAt
            ?: prefs?.getLong(KEY_EXPIRES_AT, 0L)
            ?: return false
        return expiresAt - System.currentTimeMillis() < REFRESH_THRESHOLD_MS
    }

    fun clearSession() {
        memorySession.value = null
        loggedInFlow.value = false
        prefs?.edit()?.clear()?.apply()
    }

    fun isKeystoreAvailable(): Boolean = prefs != null

    companion object {
        private const val PREFS_NAME = "supabase_session"
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_EXPIRES_AT = "expires_at"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_EMAIL = "email"
        private const val KEY_EMAIL_VERIFIED = "email_verified"
        private const val REFRESH_THRESHOLD_MS = 5 * 60 * 1000L
    }
}
