package com.example.healthcheckin.ui.screens.onboarding

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.healthcheckin.R
import com.example.healthcheckin.ui.screens.onboarding.steps.OnboardingStep1
import com.example.healthcheckin.ui.screens.onboarding.steps.OnboardingStep2
import com.example.healthcheckin.ui.screens.onboarding.steps.OnboardingStep3
import com.example.healthcheckin.ui.screens.onboarding.steps.OnboardingStep4
import com.example.healthcheckin.ui.screens.onboarding.steps.OnboardingStep5

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    isEditMode: Boolean,
    onComplete: (promptWeightRecord: Boolean, weightKg: Double?) -> Unit,
    onBack: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(isEditMode) {
        viewModel.initialize(isEditMode)
    }

    BackHandler {
        if (isEditMode) {
            onBack()
        } else {
            Toast.makeText(context, context.getString(R.string.onboarding_back_blocked), Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    LaunchedEffect(uiState.saveSuccess, uiState.promptWeightRecord) {
        if (uiState.saveSuccess && !uiState.promptWeightRecord) {
            onComplete(false, null)
            viewModel.consumeSaveSuccess()
        }
    }

    if (uiState.saveSuccess && uiState.promptWeightRecord && uiState.weightToRecord != null) {
        AlertDialog(
            onDismissRequest = {
                viewModel.consumeSaveSuccess()
                onComplete(false, null)
            },
            title = { Text(stringResource(R.string.onboarding_record_weight_title)) },
            text = {
                Text(
                    stringResource(
                        R.string.onboarding_record_weight_prompt,
                        uiState.weightToRecord!!,
                    )
                )
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.recordWeightAndComplete {
                        onComplete(false, null)
                    }
                }) {
                    Text(stringResource(R.string.onboarding_record_weight_action))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    viewModel.consumeSaveSuccess()
                    onComplete(false, null)
                }) {
                    Text(stringResource(R.string.common_cancel))
                }
            },
        )
    }

    if (uiState.showWeightDiffConfirm) {
        AlertDialog(
            onDismissRequest = viewModel::dismissWeightDiffConfirm,
            title = { Text(stringResource(R.string.onboarding_weight_diff_title)) },
            text = {
                Text(
                    stringResource(
                        R.string.onboarding_weight_diff_message,
                        uiState.pendingWeightDiffKg?.let { "%.1f".format(it) } ?: "",
                    )
                )
            },
            confirmButton = {
                Button(onClick = viewModel::confirmWeightDiffAndProceed) {
                    Text(stringResource(R.string.common_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissWeightDiffConfirm) {
                    Text(stringResource(R.string.common_cancel))
                }
            },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (isEditMode) {
                            stringResource(R.string.onboarding_edit_title)
                        } else {
                            stringResource(R.string.onboarding_title)
                        }
                    )
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            StepProgressIndicator(
                currentStep = uiState.currentStep,
                totalSteps = uiState.totalSteps,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp),
            ) {
                when (uiState.currentStep) {
                    1 -> OnboardingStep1(
                        sex = uiState.sex,
                        birthYearMonth = uiState.birthYearMonth,
                        sexError = uiState.sexError,
                        birthError = uiState.birthError,
                        onSexSelected = viewModel::onSexSelected,
                        onBirthYearMonthChanged = viewModel::onBirthYearMonthChanged,
                    )
                    2 -> OnboardingStep2(
                        heightCm = uiState.heightCm,
                        currentWeightKg = uiState.currentWeightKg,
                        heightError = uiState.heightError,
                        currentWeightError = uiState.currentWeightError,
                        onHeightChanged = viewModel::onHeightChanged,
                        onCurrentWeightChanged = viewModel::onCurrentWeightChanged,
                    )
                    3 -> OnboardingStep3(
                        targetWeightKg = uiState.targetWeightKg,
                        targetWeeks = uiState.targetWeeks,
                        goalPreview = viewModel.goalPreviewText(),
                        isMaintain = uiState.isMaintainGoal,
                        targetWeightError = uiState.targetWeightError,
                        onTargetWeightChanged = viewModel::onTargetWeightChanged,
                        onTargetWeeksChanged = viewModel::onTargetWeeksChanged,
                    )
                    4 -> OnboardingStep4(
                        selected = uiState.activityLevel,
                        onSelected = viewModel::onActivityLevelSelected,
                    )
                    5 -> uiState.calculation?.let { calc ->
                        OnboardingStep5(
                            calculation = calc,
                            targetWeeks = uiState.targetWeeks,
                            showExpanded = uiState.showCalculationExpanded,
                            onToggleExpanded = viewModel::toggleCalculationExpanded,
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (uiState.currentStep == 5) {
                    Button(
                        onClick = viewModel::saveGoal,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = uiState.canProceed,
                    ) {
                        if (uiState.isSaving) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp))
                        } else {
                            Text(stringResource(R.string.onboarding_start_button))
                        }
                    }
                } else {
                    Button(
                        onClick = viewModel::nextStep,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = uiState.canProceed,
                    ) {
                        Text(stringResource(R.string.onboarding_next))
                    }
                }

                if (uiState.currentStep > 1) {
                    TextButton(
                        onClick = viewModel::previousStep,
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                    ) {
                        Text(stringResource(R.string.onboarding_previous))
                    }
                }
            }
        }
    }
}

@Composable
private fun StepProgressIndicator(
    currentStep: Int,
    totalSteps: Int,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        LinearProgressIndicator(
            progress = { currentStep.toFloat() / totalSteps.toFloat() },
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            repeat(totalSteps) { index ->
                Text(
                    text = "${index + 1}",
                    color = if (index + 1 == currentStep) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    },
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }
    }
}
