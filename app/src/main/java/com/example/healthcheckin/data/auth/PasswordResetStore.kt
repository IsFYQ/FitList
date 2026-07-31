package com.example.healthcheckin.data.auth

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

data class PasswordResetSession(
    val accessToken: String,
    val refreshToken: String? = null,
)

@Singleton
class PasswordResetStore @Inject constructor() {
    private val _session = MutableStateFlow<PasswordResetSession?>(null)
    val session: StateFlow<PasswordResetSession?> = _session.asStateFlow()

    private val _recoveryToken = MutableStateFlow<String?>(null)
    val recoveryToken: StateFlow<String?> = _recoveryToken.asStateFlow()

    fun set(session: PasswordResetSession) {
        _session.value = session
        _recoveryToken.value = null
    }

    fun setRecoveryToken(token: String) {
        _recoveryToken.value = token
        _session.value = null
    }

    fun consume(): PasswordResetSession? = _session.value.also { _session.value = null }

    fun consumeRecoveryToken(): String? = _recoveryToken.value.also { _recoveryToken.value = null }

    fun peek(): PasswordResetSession? = _session.value

    fun clear() {
        _session.value = null
        _recoveryToken.value = null
    }
}
