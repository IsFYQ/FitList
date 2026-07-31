package com.example.healthcheckin.ui.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.healthcheckin.BuildConfig
import com.example.healthcheckin.R
import com.example.healthcheckin.ui.util.openExternalLink

private const val FATSECRET_URL = "https://platform.fatsecret.com"
private const val OFF_URL = "https://world.openfoodfacts.org"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    onBack: () -> Unit,
    viewModel: AboutViewModel = hiltViewModel(),
) {
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.about_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
            )
        },
    ) { padding ->
        androidx.compose.foundation.layout.Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineSmall,
            )
            Text(
                text = stringResource(R.string.about_version, BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 4.dp, bottom = 24.dp),
            )

            Text(
                text = stringResource(R.string.about_data_sources_title),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = stringResource(R.string.about_fatsecret_attribution),
                modifier = Modifier.padding(top = 8.dp),
            )
            Text(
                text = "FatSecret Platform API",
                color = MaterialTheme.colorScheme.primary,
                textDecoration = TextDecoration.Underline,
                modifier = Modifier
                    .padding(top = 4.dp)
                    .clickable {
                        viewModel.trackLink("FATSECRET")
                        openExternalLink(context, FATSECRET_URL)
                    },
            )
            Text(
                text = stringResource(R.string.about_off_attribution),
                modifier = Modifier.padding(top = 12.dp),
            )
            Text(
                text = "Open Food Facts",
                color = MaterialTheme.colorScheme.primary,
                textDecoration = TextDecoration.Underline,
                modifier = Modifier
                    .padding(top = 4.dp)
                    .clickable {
                        viewModel.trackLink("OFF")
                        openExternalLink(context, OFF_URL)
                    },
            )
            Text(
                text = stringResource(R.string.about_china_food_table),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 12.dp),
            )

            Text(
                text = stringResource(R.string.about_analytics_title),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 24.dp),
            )
            Text(
                text = stringResource(R.string.settings_analytics_desc),
                modifier = Modifier.padding(top = 8.dp),
            )

            Text(
                text = stringResource(R.string.about_privacy_title),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 24.dp),
            )
            Text(
                text = stringResource(R.string.about_privacy_body),
                modifier = Modifier.padding(top = 8.dp),
            )

            Text(
                text = stringResource(R.string.about_disclaimer),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 24.dp),
            )

            Text(
                text = stringResource(R.string.about_oss_title),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 24.dp, bottom = 8.dp),
            )
            Text(text = stringResource(R.string.about_oss_body))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiagnosticsScreen(
    onBack: () -> Unit,
    viewModel: DiagnosticsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.diagnostics_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
            )
        },
    ) { padding ->
        androidx.compose.foundation.layout.Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            Text(
                text = stringResource(R.string.diagnostics_pending_title),
                style = MaterialTheme.typography.titleMedium,
            )
            if (uiState.pendingByTable.isEmpty()) {
                Text(
                    text = stringResource(R.string.diagnostics_all_synced),
                    modifier = Modifier.padding(top = 8.dp),
                )
            } else {
                uiState.pendingByTable.forEach { item ->
                    Text(
                        text = stringResource(R.string.diagnostics_pending_item, item.label, item.count),
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }

            Text(
                text = stringResource(R.string.diagnostics_errors_title),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 24.dp),
            )
            if (uiState.recentErrors.isEmpty()) {
                Text(
                    text = stringResource(R.string.diagnostics_no_errors),
                    modifier = Modifier.padding(top = 8.dp),
                )
            } else {
                uiState.recentErrors.forEach { error ->
                    Text(
                        text = "${error.tableName} · ${error.errorCode ?: "?"} · retry ${error.retryCount}",
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }

            TextButton(
                onClick = viewModel::retryAll,
                enabled = !uiState.isRetrying,
                modifier = Modifier.padding(top = 16.dp),
            ) {
                Text(stringResource(R.string.diagnostics_retry_all))
            }

            TextButton(
                onClick = {
                    com.example.healthcheckin.ui.util.copyToClipboard(
                        context,
                        "diagnostics",
                        viewModel.buildDiagnosticText(),
                        R.string.diagnostics_copied,
                    )
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.diagnostics_copy))
            }
        }
    }
}
