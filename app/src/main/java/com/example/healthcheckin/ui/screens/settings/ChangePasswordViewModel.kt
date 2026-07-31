package com.example.healthcheckin.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.healthcheckin.domain.repository.AuthRepository
import com.example.healthcheckin.util.Validators
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChangePasswordUiState(
    val currentPassword: String = "",
    val newPassword: String = "",
    val confirmPassword: String = "",
    val currentPasswordError: String? = null,
    val newPasswordError: String? = null,
    val confirmPasswordError: String? = null,
    val isSaving: Boolean = false,
    val success: Boolean = false,
    val errorMessage: String? = null,
)

@HiltViewModel
class ChangePasswordViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChangePasswordUiState())
    val uiState: StateFlow<ChangePasswordUiState> = _uiState.asStateFlow()

    fun updateCurrentPassword(value: String) {
        _uiState.update { it.copy(currentPassword = value, currentPasswordError = null) }
    }

    fun updateNewPassword(value: String) {
        _uiState.update { it.copy(newPassword = value, newPasswordError = null) }
    }

    fun updateConfirmPassword(value: String) {
        _uiState.update { it.copy(confirmPassword = value, confirmPasswordError = null) }
    }

    fun save(onSuccess: () -> Unit) {
        val state = _uiState.value
        if (state.isSaving) return

        val passwordResult = Validators.validatePassword(state.newPassword)
        val confirmResult = Validators.validateConfirmPassword(state.newPassword, state.confirmPassword)
        if (state.currentPassword.isBlank()) {
            _uiState.update { it.copy(currentPasswordError = "required") }
            return
        }
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
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }
            authRepository.changePassword(state.currentPassword, state.newPassword).fold(
                onSuccess = {
                    _uiState.update { it.copy(isSaving = false, success = true) }
                    onSuccess()
                },
                onFailure = {
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            currentPasswordError = "wrong",
                            errorMessage = "change_password_failed",
                        )
                    }
                },
            )
        }
    }
}
