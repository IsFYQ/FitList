package com.example.healthcheckin.ui.screens.settings

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.healthcheckin.BuildConfig
import com.example.healthcheckin.R
import com.example.healthcheckin.ui.theme.HealthCheckInDimens
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onEditGoal: () -> Unit,
    onManageCustomFoods: () -> Unit = {},
    onExportData: () -> Unit = {},
    onNavigateAbout: () -> Unit = {},
    onNavigateDiagnostics: () -> Unit = {},
    onNavigateChangePassword: () -> Unit = {},
    onLogoutComplete: () -> Unit = {},
    onAccountDeleted: () -> Unit = {},
    onRestoreComplete: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var versionTapCount by remember { mutableIntStateOf(0) }

    LaunchedEffect(uiState.messageKey) {
        uiState.messageKey?.let { key ->
            val resId = when (key) {
                "backup_restore_success" -> R.string.backup_restore_success
                "backup_restore_offline" -> R.string.backup_restore_offline
                "delete_account_failed" -> R.string.delete_account_failed
                "delete_account_wrong_password" -> R.string.delete_account_wrong_password
                else -> R.string.backup_restore_failed
            }
            Toast.makeText(context, context.getString(resId), Toast.LENGTH_SHORT).show()
            if (uiState.restoreCompleted) onRestoreComplete()
            viewModel.clearMessage()
        }
    }

    LaunchedEffect(uiState.logoutCompleted) {
        if (uiState.logoutCompleted) {
            viewModel.consumeLogoutCompleted()
            onLogoutComplete()
        }
    }

    LaunchedEffect(uiState.accountDeleted) {
        if (uiState.accountDeleted) {
            Toast.makeText(
                context,
                context.getString(R.string.account_deleted_success),
                Toast.LENGTH_SHORT,
            ).show()
            viewModel.consumeAccountDeleted()
            onAccountDeleted()
        }
    }

    if (uiState.showLogoutDialog) {
        AlertDialog(
            onDismissRequest = viewModel::dismissLogoutDialog,
            title = { Text(stringResource(R.string.settings_logout)) },
            text = {
                Text(
                    if (uiState.backupState.pendingCount > 0) {
                        stringResource(R.string.logout_message_pending, uiState.backupState.pendingCount)
                    } else {
                        stringResource(R.string.logout_message)
                    },
                )
            },
            confirmButton = {
                if (uiState.backupState.pendingCount > 0) {
                    TextButton(onClick = viewModel::backupThenLogout) {
                        Text(stringResource(R.string.logout_backup_first))
                    }
                }
                TextButton(onClick = viewModel::confirmLogout) {
                    Text(stringResource(R.string.settings_logout))
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissLogoutDialog) {
                    Text(stringResource(R.string.common_cancel))
                }
            },
        )
    }

    if (uiState.showDeleteStep1) {
        AlertDialog(
            onDismissRequest = viewModel::dismissDeleteDialogs,
            title = { Text(stringResource(R.string.settings_delete_account)) },
            text = { Text(stringResource(R.string.delete_account_message)) },
            confirmButton = {
                TextButton(onClick = viewModel::proceedDeleteStep2) {
                    Text(stringResource(R.string.common_continue))
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissDeleteDialogs) {
                    Text(stringResource(R.string.common_cancel))
                }
            },
        )
    }

    if (uiState.showDeleteStep2) {
        AlertDialog(
            onDismissRequest = viewModel::dismissDeleteDialogs,
            title = { Text(stringResource(R.string.delete_account_confirm_title)) },
            text = {
                OutlinedTextField(
                    value = uiState.deletePassword,
                    onValueChange = viewModel::updateDeletePassword,
                    label = { Text(stringResource(R.string.delete_account_password_hint)) },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = viewModel::confirmDeleteAccount,
                    enabled = !uiState.isDeletingAccount && uiState.deletePassword.isNotBlank(),
                ) {
                    Text(stringResource(R.string.settings_delete_account))
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissDeleteDialogs) {
                    Text(stringResource(R.string.common_cancel))
                }
            },
        )
    }

    if (uiState.showRestoreStep1) {
        AlertDialog(
            onDismissRequest = viewModel::dismissRestoreDialogs,
            title = { Text(stringResource(R.string.backup_restore_title)) },
            text = {
                Text(
                    if (uiState.backupState.pendingCount > 0) {
                        stringResource(R.string.backup_restore_message_pending, uiState.backupState.pendingCount)
                    } else {
                        stringResource(R.string.backup_restore_message)
                    },
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (uiState.backupState.pendingCount > 0) {
                            viewModel.proceedRestoreStep2()
                        } else {
                            viewModel.confirmRestore()
                        }
                    },
                ) {
                    Text(stringResource(R.string.backup_restore_continue))
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissRestoreDialogs) {
                    Text(stringResource(R.string.common_cancel))
                }
            },
        )
    }

    if (uiState.showRestoreStep2) {
        AlertDialog(
            onDismissRequest = viewModel::dismissRestoreDialogs,
            title = { Text(stringResource(R.string.backup_restore_confirm_title)) },
            text = {
                Text(stringResource(R.string.backup_restore_confirm_message, uiState.backupState.pendingCount))
            },
            confirmButton = {
                TextButton(onClick = viewModel::confirmRestore) {
                    Text(stringResource(R.string.common_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::backupBeforeRestore) {
                    Text(stringResource(R.string.backup_restore_backup_first))
                }
            },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            item { SettingsSectionHeader(stringResource(R.string.settings_section_account)) }
            item {
                ListItem(
                    headlineContent = { Text(uiState.email.ifBlank { "—" }) },
                    supportingContent = {
                        if (!uiState.emailVerified) {
                            UnverifiedBadge()
                        }
                    },
                    trailingContent = {
                        if (!uiState.emailVerified) {
                            TextButton(
                                onClick = viewModel::resendVerificationEmail,
                                enabled = uiState.resendCooldownSeconds == 0 && !uiState.isResendingEmail,
                            ) {
                                Text(
                                    if (uiState.resendCooldownSeconds > 0) {
                                        "${uiState.resendCooldownSeconds}s"
                                    } else {
                                        stringResource(R.string.settings_resend_verification)
                                    },
                                )
                            }
                        }
                    },
                )
            }
            item {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.settings_change_password)) },
                    modifier = Modifier.clickable(onClick = onNavigateChangePassword),
                )
            }
            item {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.settings_logout)) },
                    modifier = Modifier.clickable(onClick = viewModel::requestLogout),
                )
            }
            item {
                ListItem(
                    headlineContent = {
                        Text(
                            stringResource(R.string.settings_delete_account),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    },
                    modifier = Modifier
                        .heightIn(min = HealthCheckInDimens.MinTouchTarget)
                        .clickable(onClick = viewModel::requestDeleteAccount),
                )
            }

            item { SettingsSectionHeader(stringResource(R.string.settings_section_goal)) }
            if (uiState.showAgeUpdatePrompt) {
                item {
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.onboarding_age_update_prompt)) },
                        supportingContent = {
                            Text(
                                text = stringResource(R.string.onboarding_age_update_action),
                                modifier = Modifier.clickable(onClick = onEditGoal),
                            )
                        },
                    )
                }
            }
            item {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.settings_my_goal)) },
                    supportingContent = { uiState.budgetSubtitle?.let { Text(it) } },
                    modifier = Modifier.clickable(onClick = onEditGoal),
                )
            }

            item { SettingsSectionHeader(stringResource(R.string.settings_section_data)) }
            item {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.settings_custom_foods)) },
                    supportingContent = {
                        Text(stringResource(R.string.settings_custom_foods_count, uiState.customFoodCount))
                    },
                    modifier = Modifier.clickable(onClick = onManageCustomFoods),
                )
            }
            item {
                val backup = uiState.backupState
                ListItem(
                    headlineContent = { Text(stringResource(R.string.settings_backup)) },
                    supportingContent = {
                        Text(
                            when {
                                backup.isRunning && backup.progressTotal > 0 ->
                                    stringResource(R.string.backup_in_progress, backup.progressDone, backup.progressTotal)
                                backup.pendingCount == 0 -> stringResource(R.string.backup_all_synced)
                                else -> stringResource(R.string.settings_backup_pending, backup.pendingCount)
                            },
                        )
                        backup.lastBackupAt?.let { at ->
                            Text(stringResource(R.string.backup_last_at, formatRelativeTime(at)))
                        } ?: Text(stringResource(R.string.backup_never))
                    },
                )
            }
            item {
                Button(
                    onClick = viewModel::triggerBackup,
                    enabled = !uiState.backupState.isRunning,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                ) {
                    if (uiState.backupState.isRunning) {
                        CircularProgressIndicator(modifier = Modifier.padding(end = 8.dp))
                    }
                    Text(stringResource(R.string.backup_now))
                }
            }
            item {
                TextButton(
                    onClick = viewModel::requestRestore,
                    enabled = !uiState.isRestoring,
                    modifier = Modifier.padding(horizontal = 16.dp),
                ) {
                    Text(stringResource(R.string.backup_restore_action))
                }
            }
            item {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.settings_export)) },
                    supportingContent = { Text(stringResource(R.string.settings_export_subtitle)) },
                    modifier = Modifier.clickable(onClick = onExportData),
                )
            }
            item {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.settings_diagnostics)) },
                    modifier = Modifier.clickable(onClick = onNavigateDiagnostics),
                )
            }

            item { SettingsSectionHeader(stringResource(R.string.settings_section_preferences)) }
            item {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.settings_analytics)) },
                    supportingContent = { Text(stringResource(R.string.settings_analytics_desc)) },
                    trailingContent = {
                        Switch(
                            checked = uiState.analyticsEnabled,
                            onCheckedChange = viewModel::setAnalyticsEnabled,
                        )
                    },
                )
            }
            item {
                ListItem(headlineContent = { Text(stringResource(R.string.settings_theme)) })
            }
            item {
                ThemeModeSelector(
                    selected = uiState.themeMode,
                    onSelect = viewModel::setThemeMode,
                )
            }

            item { SettingsSectionHeader(stringResource(R.string.settings_section_about)) }
            item {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.settings_about)) },
                    modifier = Modifier.clickable(onClick = onNavigateAbout),
                )
            }
            item {
                ListItem(
                    headlineContent = {
                        Text(stringResource(R.string.about_version_short, BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE))
                    },
                    modifier = Modifier.clickable {
                        if (BuildConfig.DEBUG) {
                            versionTapCount++
                            if (versionTapCount >= 7) {
                                versionTapCount = 0
                                onNavigateDiagnostics()
                            }
                        }
                    },
                )
            }
        }
    }
}

private fun formatRelativeTime(epochMillis: Long): String {
    val diff = System.currentTimeMillis() - epochMillis
    return when {
        diff < 60_000 -> "刚刚"
        diff < 3_600_000 -> "${diff / 60_000} 分钟前"
        diff < 86_400_000 -> "${diff / 3_600_000} 小时前"
        else -> SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(Date(epochMillis))
    }
}
