package com.example.healthcheckin.ui.screens.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.healthcheckin.domain.auth.AuthException
import com.example.healthcheckin.domain.repository.AuthRepository
import com.example.healthcheckin.util.Validators
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ForgotPasswordUiState(
    val email: String = "",
    val emailError: String? = null,
    val sent: Boolean = false,
    val isSending: Boolean = false,
    val resendCooldownSeconds: Int = 0,
    val errorMessage: String? = null,
)

@HiltViewModel
class ForgotPasswordViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ForgotPasswordUiState())
    val uiState: StateFlow<ForgotPasswordUiState> = _uiState.asStateFlow()

    private var cooldownJob: Job? = null

    fun updateEmail(value: String) {
        _uiState.update { it.copy(email = value, emailError = null, errorMessage = null) }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun sendResetEmail() {
        val email = _uiState.value.email.trim()
        if (!Validators.validateEmail(email).isValid) {
            _uiState.update { it.copy(emailError = "invalid") }
            return
        }
        if (_uiState.value.isSending || _uiState.value.resendCooldownSeconds > 0) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSending = true, errorMessage = null) }
            authRepository.sendPasswordResetEmail(email).fold(
                onSuccess = {
                    _uiState.update { it.copy(isSending = false, sent = true) }
                    startCooldown()
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isSending = false,
                            errorMessage = mapSendError(error),
                        )
                    }
                },
            )
        }
    }

    private fun mapSendError(error: Throwable): String = when (error) {
        is AuthException.NetworkUnavailable -> "发送失败，请检查网络后重试"
        is AuthException.Timeout -> "网络响应超时，请重试"
        else -> "发送失败，请稍后重试"
    }

    private fun startCooldown() {
        cooldownJob?.cancel()
        cooldownJob = viewModelScope.launch {
            var remaining = 60
            while (remaining > 0) {
                _uiState.update { it.copy(resendCooldownSeconds = remaining) }
                delay(1000)
                remaining--
            }
            _uiState.update { it.copy(resendCooldownSeconds = 0) }
        }
    }
}
