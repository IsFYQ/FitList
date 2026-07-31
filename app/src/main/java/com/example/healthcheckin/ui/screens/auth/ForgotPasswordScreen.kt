package com.example.healthcheckin.ui.screens.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.healthcheckin.R

@Composable
fun ForgotPasswordScreen(
    onBack: () -> Unit,
    viewModel: ForgotPasswordViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(text = stringResource(R.string.auth_forgot_title))
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = stringResource(R.string.auth_forgot_description))

            Spacer(modifier = Modifier.height(24.dp))

            if (uiState.sent) {
                Text(text = stringResource(R.string.auth_forgot_success, uiState.email.trim()))
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.auth_forgot_back_login))
                }
                TextButton(
                    onClick = viewModel::sendResetEmail,
                    enabled = uiState.resendCooldownSeconds == 0 && !uiState.isSending,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        if (uiState.resendCooldownSeconds > 0) {
                            stringResource(R.string.auth_forgot_resend, uiState.resendCooldownSeconds)
                        } else {
                            stringResource(R.string.auth_forgot_send)
                        },
                    )
                }
            } else {
                OutlinedTextField(
                    value = uiState.email,
                    onValueChange = viewModel::updateEmail,
                    label = { Text(stringResource(R.string.auth_email_hint)) },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    singleLine = true,
                    isError = uiState.emailError != null,
                    supportingText = uiState.emailError?.let {
                        { Text(stringResource(R.string.auth_error_email_invalid)) }
                    },
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = viewModel::sendResetEmail,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = uiState.email.isNotBlank() && !uiState.isSending,
                ) {
                    if (uiState.isSending) {
                        CircularProgressIndicator(modifier = Modifier.padding(end = 8.dp))
                    }
                    Text(stringResource(R.string.auth_forgot_send))
                }
                TextButton(onClick = onBack) {
                    Text(stringResource(R.string.auth_forgot_back_login))
                }
            }
        }
    }
}

@Composable
fun ResetPasswordScreen(
    onBackToLogin: () -> Unit,
    onNavigateForgotPassword: () -> Unit,
    recoveryToken: String? = null,
    viewModel: ResetPasswordViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    androidx.compose.runtime.LaunchedEffect(recoveryToken) {
        recoveryToken?.let(viewModel::verifyToken)
    }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            when {
                uiState.linkInvalid -> {
                    Text(text = stringResource(R.string.auth_reset_link_invalid))
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = onNavigateForgotPassword, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.auth_forgot_title))
                    }
                    TextButton(onClick = onBackToLogin) {
                        Text(stringResource(R.string.auth_forgot_back_login))
                    }
                }
                else -> {
                    Text(text = stringResource(R.string.auth_reset_title))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = stringResource(R.string.auth_reset_description))
                    Spacer(modifier = Modifier.height(24.dp))
                    OutlinedTextField(
                        value = uiState.newPassword,
                        onValueChange = viewModel::updateNewPassword,
                        label = { Text(stringResource(R.string.auth_password_hint)) },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier.fillMaxWidth(),
                        isError = uiState.newPasswordError != null,
                        supportingText = uiState.newPasswordError?.let {
                            { Text(stringResource(R.string.auth_error_password_invalid)) }
                        },
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = uiState.confirmPassword,
                        onValueChange = viewModel::updateConfirmPassword,
                        label = { Text(stringResource(R.string.auth_confirm_password_hint)) },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier.fillMaxWidth(),
                        isError = uiState.confirmPasswordError != null,
                        supportingText = uiState.confirmPasswordError?.let {
                            { Text(stringResource(R.string.auth_error_password_mismatch)) }
                        },
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = { viewModel.submit(onSuccess = onBackToLogin) },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !uiState.isSaving,
                    ) {
                        if (uiState.isSaving) {
                            CircularProgressIndicator(modifier = Modifier.padding(end = 8.dp))
                        }
                        Text(stringResource(R.string.auth_reset_confirm))
                    }
                }
            }
        }
    }
}
