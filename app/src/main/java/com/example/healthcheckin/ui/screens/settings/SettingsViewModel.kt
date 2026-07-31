package com.example.healthcheckin.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.healthcheckin.data.auth.SessionManager
import com.example.healthcheckin.data.local.dao.FoodDao
import com.example.healthcheckin.data.preferences.ThemePreferences
import com.example.healthcheckin.domain.analytics.AnalyticsEvents
import com.example.healthcheckin.domain.analytics.AnalyticsTracker
import com.example.healthcheckin.domain.model.BackupState
import com.example.healthcheckin.domain.model.ThemeMode
import com.example.healthcheckin.domain.repository.AuthRepository
import com.example.healthcheckin.domain.repository.BackupRepository
import com.example.healthcheckin.domain.repository.GoalRepository
import com.example.healthcheckin.util.PrecisionUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val email: String = "",
    val emailVerified: Boolean = true,
    val resendCooldownSeconds: Int = 0,
    val isResendingEmail: Boolean = false,
    val budgetSubtitle: String? = null,
    val showAgeUpdatePrompt: Boolean = false,
    val customFoodCount: Int = 0,
    val backupState: BackupState = BackupState(),
    val showRestoreStep1: Boolean = false,
    val showRestoreStep2: Boolean = false,
    val isRestoring: Boolean = false,
    val restoreCompleted: Boolean = false,
    val messageKey: String? = null,
    val analyticsEnabled: Boolean = true,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val showLogoutDialog: Boolean = false,
    val showDeleteStep1: Boolean = false,
    val showDeleteStep2: Boolean = false,
    val deletePassword: String = "",
    val isDeletingAccount: Boolean = false,
    val logoutCompleted: Boolean = false,
    val accountDeleted: Boolean = false,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val goalRepository: GoalRepository,
    private val backupRepository: BackupRepository,
    private val authRepository: AuthRepository,
    private val sessionManager: SessionManager,
    private val foodDao: FoodDao,
    private val themePreferences: ThemePreferences,
    private val analyticsTracker: AnalyticsTracker,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    val backupState: StateFlow<BackupState> = backupRepository.observeBackupState()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BackupState())

    private var resendJob: Job? = null

    init {
        analyticsTracker.track(AnalyticsEvents.SETTINGS_OPENED)
        val session = sessionManager.session.value
        _uiState.update {
            it.copy(
                email = session?.email.orEmpty(),
                emailVerified = session?.emailVerified ?: true,
            )
        }

        viewModelScope.launch {
            analyticsTracker.isEnabled().collect { enabled ->
                _uiState.update { it.copy(analyticsEnabled = enabled) }
            }
        }

        viewModelScope.launch {
            themePreferences.themeMode.collect { mode ->
                _uiState.update { it.copy(themeMode = mode) }
            }
        }

        viewModelScope.launch {
            val userId = sessionManager.getUserId() ?: return@launch
            val goal = goalRepository.getActiveGoal(userId)
            val showAgePrompt = goalRepository.shouldPromptAgeUpdate(userId)
            _uiState.update {
                it.copy(
                    budgetSubtitle = goal?.let { g ->
                        "${PrecisionUtil.formatCaloriesWithSeparator(g.budgetKcal)}大卡/天"
                    },
                    showAgeUpdatePrompt = showAgePrompt,
                )
            }
            backupRepository.scheduleBackup()
            foodDao.observeCustomFoodCount(userId).collect { count ->
                _uiState.update { it.copy(customFoodCount = count) }
            }
        }

        viewModelScope.launch {
            backupState.collect { state ->
                _uiState.update { it.copy(backupState = state) }
            }
        }
    }

    fun setAnalyticsEnabled(enabled: Boolean) {
        viewModelScope.launch { analyticsTracker.setEnabled(enabled) }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { themePreferences.setThemeMode(mode) }
    }

    fun resendVerificationEmail() {
        if (_uiState.value.resendCooldownSeconds > 0 || _uiState.value.isResendingEmail) return
        viewModelScope.launch {
            _uiState.update { it.copy(isResendingEmail = true) }
            authRepository.resendVerificationEmail()
                .onSuccess { startResendCooldown() }
            _uiState.update { it.copy(isResendingEmail = false) }
        }
    }

    fun requestLogout() {
        _uiState.update { it.copy(showLogoutDialog = true) }
    }

    fun dismissLogoutDialog() {
        _uiState.update { it.copy(showLogoutDialog = false) }
    }

    fun confirmLogout() {
        viewModelScope.launch {
            analyticsTracker.track(
                AnalyticsEvents.SIGN_OUT,
                mapOf("pending_backup_count" to _uiState.value.backupState.pendingCount),
            )
            authRepository.signOut()
            _uiState.update { it.copy(showLogoutDialog = false, logoutCompleted = true) }
        }
    }

    fun backupThenLogout() {
        dismissLogoutDialog()
        triggerBackup()
        viewModelScope.launch {
            analyticsTracker.track(
                AnalyticsEvents.SIGN_OUT,
                mapOf("pending_backup_count" to _uiState.value.backupState.pendingCount),
            )
            authRepository.signOut()
            _uiState.update { it.copy(logoutCompleted = true) }
        }
    }

    fun requestDeleteAccount() {
        _uiState.update { it.copy(showDeleteStep1 = true) }
    }

    fun dismissDeleteDialogs() {
        _uiState.update {
            it.copy(showDeleteStep1 = false, showDeleteStep2 = false, deletePassword = "")
        }
    }

    fun proceedDeleteStep2() {
        _uiState.update { it.copy(showDeleteStep1 = false, showDeleteStep2 = true) }
    }

    fun updateDeletePassword(value: String) {
        _uiState.update { it.copy(deletePassword = value) }
    }

    fun confirmDeleteAccount() {
        val password = _uiState.value.deletePassword
        if (password.isBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isDeletingAccount = true) }
            authRepository.deleteAccount(password).fold(
                onSuccess = {
                    _uiState.update {
                        it.copy(
                            isDeletingAccount = false,
                            showDeleteStep2 = false,
                            accountDeleted = true,
                        )
                    }
                },
                onFailure = { error ->
                    val key = if (error.message == "wrong_password") {
                        "delete_account_wrong_password"
                    } else {
                        "delete_account_failed"
                    }
                    _uiState.update {
                        it.copy(
                            isDeletingAccount = false,
                            messageKey = key,
                            showDeleteStep2 = error.message != "wrong_password",
                        )
                    }
                },
            )
        }
    }

    fun triggerBackup() {
        viewModelScope.launch { backupRepository.triggerBackup(force = true) }
    }

    fun requestRestore() {
        _uiState.update { it.copy(showRestoreStep1 = true) }
    }

    fun dismissRestoreDialogs() {
        _uiState.update { it.copy(showRestoreStep1 = false, showRestoreStep2 = false) }
    }

    fun proceedRestoreStep2() {
        _uiState.update { it.copy(showRestoreStep1 = false, showRestoreStep2 = true) }
    }

    fun confirmRestore() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRestoring = true, showRestoreStep2 = false) }
            val result = backupRepository.restoreFromCloud()
            _uiState.update {
                it.copy(
                    isRestoring = false,
                    restoreCompleted = result.success,
                    messageKey = when {
                        result.success -> "backup_restore_success"
                        result.errorMessage == "offline" -> "backup_restore_offline"
                        else -> "backup_restore_failed"
                    },
                )
            }
        }
    }

    fun backupBeforeRestore() {
        dismissRestoreDialogs()
        triggerBackup()
    }

    fun clearMessage() {
        _uiState.update { it.copy(messageKey = null, restoreCompleted = false) }
    }

    fun consumeLogoutCompleted() {
        _uiState.update { it.copy(logoutCompleted = false) }
    }

    fun consumeAccountDeleted() {
        _uiState.update { it.copy(accountDeleted = false) }
    }

    private fun startResendCooldown() {
        resendJob?.cancel()
        resendJob = viewModelScope.launch {
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
