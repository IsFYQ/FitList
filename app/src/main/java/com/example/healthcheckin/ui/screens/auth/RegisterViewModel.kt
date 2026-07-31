package com.example.healthcheckin.ui.screens.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.healthcheckin.domain.analytics.AnalyticsEvents
import com.example.healthcheckin.domain.analytics.AnalyticsTracker
import com.example.healthcheckin.domain.auth.AuthException
import com.example.healthcheckin.domain.repository.AuthRepository
import com.example.healthcheckin.util.ValidationError
import com.example.healthcheckin.util.Validators
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RegisterUiState(
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val emailError: String? = null,
    val passwordError: String? = null,
    val confirmPasswordError: String? = null,
    val isLoading: Boolean = false,
    val canSubmit: Boolean = false,
    val errorMessage: String? = null,
    val registerSuccess: Boolean = false,
    val registerNeedsLoginEmail: String? = null,
)

@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val analyticsTracker: AnalyticsTracker,
) : ViewModel() {

    private val _uiState = MutableStateFlow(RegisterUiState())
    val uiState: StateFlow<RegisterUiState> = _uiState.asStateFlow()

    fun onEmailChange(value: String) {
        _uiState.update { it.copy(email = value, emailError = null) }
        validate()
    }

    fun onPasswordChange(value: String) {
        _uiState.update { it.copy(password = value, passwordError = null) }
        validate()
    }

    fun onConfirmPasswordChange(value: String) {
        _uiState.update { it.copy(confirmPassword = value, confirmPasswordError = null) }
        validate()
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun consumeSuccess() {
        _uiState.update { it.copy(registerSuccess = false) }
    }

    fun consumeNeedsLogin() {
        _uiState.update { it.copy(registerNeedsLoginEmail = null) }
    }

    fun register() {
        val state = _uiState.value
        if (!state.canSubmit || state.isLoading) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            authRepository.signUp(state.email, state.password).fold(
                onSuccess = {
                    _uiState.update { it.copy(isLoading = false, registerSuccess = true) }
                },
                onFailure = { error ->
                    handleRegisterFailure(error)
                },
            )
        }
    }

    private fun handleRegisterFailure(error: Throwable) {
        when (error) {
            is AuthException.NeedsLogin -> {
                analyticsTracker.track(
                    AnalyticsEvents.SIGN_UP_FAILED,
                    mapOf("error_code" to "needs_login"),
                )
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        registerNeedsLoginEmail = error.email,
                    )
                }
            }
            is AuthException.EmailAlreadyRegistered -> {
                analyticsTracker.track(
                    AnalyticsEvents.SIGN_UP_FAILED,
                    mapOf("error_code" to "email_already_registered"),
                )
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "该邮箱已注册，可直接登录或重置密码",
                        emailError = "该邮箱已注册，可直接登录或重置密码",
                    )
                }
            }
            is AuthException.NetworkUnavailable -> {
                analyticsTracker.track(
                    AnalyticsEvents.SIGN_UP_FAILED,
                    mapOf("error_code" to "network_unavailable"),
                )
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "网络似乎没有连接，请检查后重试",
                    )
                }
            }
            is AuthException.Timeout -> {
                analyticsTracker.track(
                    AnalyticsEvents.SIGN_UP_FAILED,
                    mapOf("error_code" to "timeout"),
                )
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "网络响应超时，请重试",
                    )
                }
            }
            else -> {
                analyticsTracker.track(
                    AnalyticsEvents.SIGN_UP_FAILED,
                    mapOf("error_code" to "server_error"),
                )
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "服务暂时不可用，请稍后重试",
                    )
                }
            }
        }
    }

    private fun validate() {
        val email = Validators.validateEmail(_uiState.value.email)
        val password = Validators.validatePassword(_uiState.value.password)
        val confirm = Validators.validateConfirmPassword(
            _uiState.value.password,
            _uiState.value.confirmPassword,
        )

        _uiState.update {
            it.copy(
                canSubmit = email.isValid && password.isValid && confirm.isValid,
                emailError = mapError(email, ValidationError.EMAIL_INVALID, "请输入正确的邮箱地址"),
                passwordError = mapError(password, ValidationError.PASSWORD_INVALID, "密码需8-64个字符，且同时包含字母和数字"),
                confirmPasswordError = mapError(confirm, ValidationError.PASSWORD_MISMATCH, "两次输入的密码不一致"),
            )
        }
    }

    private fun mapError(
        result: com.example.healthcheckin.util.ValidationResult,
        expected: ValidationError,
        message: String,
    ): String? {
        if (result is com.example.healthcheckin.util.ValidationResult.Error && result.error == expected) {
            return message
        }
        return null
    }
}
