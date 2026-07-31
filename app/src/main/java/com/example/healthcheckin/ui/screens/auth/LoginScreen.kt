package com.example.healthcheckin.ui.screens.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.healthcheckin.R

@Composable
fun LoginScreen(
    onNavigateRegister: () -> Unit,
    onNavigateForgotPassword: () -> Unit,
    onLoginSuccess: (needsOnboarding: Boolean) -> Unit,
    prefillEmail: String? = null,
    viewModel: LoginViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val emailFocus = remember { FocusRequester() }

    LaunchedEffect(prefillEmail) {
        if (!prefillEmail.isNullOrBlank()) {
            viewModel.onEmailChange(prefillEmail)
        }
    }

    LaunchedEffect(Unit) { emailFocus.requestFocus() }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    LaunchedEffect(uiState.loginSuccess) {
        if (uiState.loginSuccess) {
            onLoginSuccess(uiState.needsOnboarding)
            viewModel.consumeSuccess()
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(text = stringResource(R.string.auth_login_title))

            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(
                value = uiState.email,
                onValueChange = viewModel::onEmailChange,
                label = { Text(stringResource(R.string.auth_email_hint)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(emailFocus),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next,
                ),
                isError = uiState.emailError != null,
                supportingText = uiState.emailError?.let { { Text(it) } },
                singleLine = true,
                enabled = !uiState.isLoading,
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = uiState.password,
                onValueChange = viewModel::onPasswordChange,
                label = { Text(stringResource(R.string.auth_password_hint)) },
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = if (uiState.passwordVisible) {
                    VisualTransformation.None
                } else {
                    PasswordVisualTransformation()
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(onDone = { viewModel.login() }),
                isError = uiState.passwordError != null,
                supportingText = uiState.passwordError?.let { { Text(it) } },
                singleLine = true,
                enabled = !uiState.isLoading,
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = viewModel::login,
                modifier = Modifier.fillMaxWidth(),
                enabled = uiState.canSubmit && !uiState.isLoading,
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.height(16.dp))
                } else {
                    Text(
                        text = if (uiState.lockSecondsRemaining > 0) {
                            stringResource(R.string.auth_error_login_locked, uiState.lockSecondsRemaining)
                        } else {
                            stringResource(R.string.auth_login_button)
                        }
                    )
                }
            }

            TextButton(onClick = onNavigateForgotPassword) {
                Text(stringResource(R.string.auth_forgot_password))
            }

            TextButton(onClick = onNavigateRegister) {
                Text(stringResource(R.string.auth_no_account))
            }
        }
    }
}
