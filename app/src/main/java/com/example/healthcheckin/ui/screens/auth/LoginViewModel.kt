package com.example.healthcheckin.ui.screens.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.healthcheckin.data.local.dao.AppSettingDao
import com.example.healthcheckin.domain.analytics.AnalyticsEvents
import com.example.healthcheckin.domain.analytics.AnalyticsTracker
import com.example.healthcheckin.domain.repository.AuthRepository
import com.example.healthcheckin.util.ValidationConstants
import com.example.healthcheckin.util.ValidationError
import com.example.healthcheckin.util.Validators
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val passwordVisible: Boolean = false,
    val emailError: String? = null,
    val passwordError: String? = null,
    val isLoading: Boolean = false,
    val canSubmit: Boolean = false,
    val errorMessage: String? = null,
    val loginSuccess: Boolean = false,
    val needsOnboarding: Boolean = false,
    val lockSecondsRemaining: Int = 0,
    val failCount: Int = 0,
)

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val appSettingDao: AppSettingDao,
    private val analyticsTracker: AnalyticsTracker,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val lastEmail = appSettingDao.get("last_login_email")?.valueJson
                ?.trim('"')
            if (!lastEmail.isNullOrBlank()) {
                _uiState.update { it.copy(email = lastEmail) }
                validate()
            }
        }
    }

    fun onEmailChange(value: String) {
        _uiState.update { it.copy(email = value, emailError = null) }
        validate()
    }

    fun onPasswordChange(value: String) {
        _uiState.update { it.copy(password = value, passwordError = null) }
        validate()
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun consumeSuccess() {
        _uiState.update { it.copy(loginSuccess = false) }
    }

    fun login() {
        val state = _uiState.value
        if (!state.canSubmit || state.isLoading || state.lockSecondsRemaining > 0) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val result = authRepository.signIn(state.email, state.password)
            result.fold(
                onSuccess = { session ->
                    val needsOnboarding = authRepository.needsOnboarding(session.userId)
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            loginSuccess = true,
                            needsOnboarding = needsOnboarding,
                            failCount = 0,
                        )
                    }
                },
                onFailure = {
                    val newFailCount = state.failCount + 1
                    analyticsTracker.track(
                        AnalyticsEvents.SIGN_IN_FAILED,
                        mapOf(
                            "error_code" to "invalid_credentials",
                            "fail_count" to newFailCount,
                        ),
                    )
                    if (newFailCount >= ValidationConstants.LOGIN_FAIL_LOCK_COUNT) {
                        startLockCountdown()
                    }
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            failCount = newFailCount,
                            password = "",
                            passwordError = "邮箱或密码不正确",
                            errorMessage = "邮箱或密码不正确",
                        )
                    }
                }
            )
        }
    }

    private fun validate() {
        val emailResult = Validators.validateEmail(_uiState.value.email)
        val passwordResult = Validators.validatePassword(_uiState.value.password)
        _uiState.update {
            it.copy(
                canSubmit = emailResult.isValid && passwordResult.isValid,
                emailError = if (emailResult is com.example.healthcheckin.util.ValidationResult.Error &&
                    emailResult.error == ValidationError.EMAIL_INVALID &&
                    it.email.isNotBlank()
                ) {
                    "请输入正确的邮箱地址"
                } else {
                    null
                },
            )
        }
    }

    private fun startLockCountdown() {
        viewModelScope.launch {
            var remaining = ValidationConstants.LOGIN_LOCK_SECONDS
            while (remaining > 0) {
                _uiState.update { it.copy(lockSecondsRemaining = remaining) }
                delay(1000)
                remaining--
            }
            _uiState.update { it.copy(lockSecondsRemaining = 0, failCount = 0) }
        }
    }
}
