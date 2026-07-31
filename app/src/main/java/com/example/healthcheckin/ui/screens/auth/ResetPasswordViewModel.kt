package com.example.healthcheckin.ui.screens.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.healthcheckin.data.auth.PasswordResetSession
import com.example.healthcheckin.data.auth.PasswordResetStore
import com.example.healthcheckin.domain.repository.AuthRepository
import com.example.healthcheckin.util.Validators
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ResetPasswordUiState(
    val newPassword: String = "",
    val confirmPassword: String = "",
    val newPasswordError: String? = null,
    val confirmPasswordError: String? = null,
    val isSaving: Boolean = false,
    val linkInvalid: Boolean = false,
    val completed: Boolean = false,
)

@HiltViewModel
class ResetPasswordViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val passwordResetStore: PasswordResetStore,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ResetPasswordUiState())
    val uiState: StateFlow<ResetPasswordUiState> = _uiState.asStateFlow()

    private var session: PasswordResetSession? = null

    init {
        session = passwordResetStore.peek()
        if (session == null) {
            _uiState.update { it.copy(linkInvalid = true) }
        }
    }

    fun verifyToken(token: String) {
        viewModelScope.launch {
            authRepository.verifyRecoveryToken(token).fold(
                onSuccess = { verified ->
                    session = verified
                    passwordResetStore.set(verified)
                    _uiState.update { it.copy(linkInvalid = false) }
                },
                onFailure = {
                    _uiState.update { it.copy(linkInvalid = true) }
                },
            )
        }
    }

    fun updateNewPassword(value: String) {
        _uiState.update { it.copy(newPassword = value, newPasswordError = null) }
    }

    fun updateConfirmPassword(value: String) {
        _uiState.update { it.copy(confirmPassword = value, confirmPasswordError = null) }
    }

    fun submit(onSuccess: () -> Unit) {
        val currentSession = session
        if (currentSession == null) {
            _uiState.update { it.copy(linkInvalid = true) }
            return
        }
        val state = _uiState.value
        if (state.isSaving) return

        val passwordResult = Validators.validatePassword(state.newPassword)
        val confirmResult = Validators.validateConfirmPassword(state.newPassword, state.confirmPassword)
        if (!passwordResult.isValid || !confirmResult.isValid) {
            _uiState.update {
                it.copy(
                    newPasswordError = if (!passwordResult.isValid) "invalid" else null,
                    confirmPasswordError = if (!confirmResult.isValid) "mismatch" else null,
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            authRepository.completePasswordReset(currentSession.accessToken, state.newPassword).fold(
                onSuccess = {
                    passwordResetStore.clear()
                    _uiState.update { it.copy(isSaving = false, completed = true) }
                    onSuccess()
                },
                onFailure = {
                    _uiState.update { it.copy(isSaving = false, linkInvalid = true) }
                },
            )
        }
    }
}
