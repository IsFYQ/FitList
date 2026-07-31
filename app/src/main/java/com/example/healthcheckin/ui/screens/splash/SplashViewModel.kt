package com.example.healthcheckin.ui.screens.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.healthcheckin.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _destination = MutableStateFlow(SplashDestination.Loading)
    val destination: StateFlow<SplashDestination> = _destination.asStateFlow()

    init {
        viewModelScope.launch {
            val start = System.currentTimeMillis()
            val session = authRepository.initializeSession()
            val elapsed = System.currentTimeMillis() - start
            if (elapsed < 800) delay(800 - elapsed)

            _destination.value = when {
                session == null -> SplashDestination.Login
                authRepository.needsOnboarding(session.userId) -> SplashDestination.Onboarding
                else -> SplashDestination.Dashboard
            }
        }
    }
}
