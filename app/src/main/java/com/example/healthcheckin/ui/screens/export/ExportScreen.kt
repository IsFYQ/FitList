package com.example.healthcheckin.ui.screens.export

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.healthcheckin.R
import com.example.healthcheckin.domain.model.ExportFormat
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportScreen(
    onBack: () -> Unit,
    viewModel: ExportViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.shareEvents.collect { file ->
            shareExportFile(context, file)
        }
    }

    LaunchedEffect(uiState.toastMessage) {
        when (uiState.toastMessage) {
            "cancelled" -> {
                Toast.makeText(context, context.getString(R.string.export_cancelled), Toast.LENGTH_SHORT).show()
                viewModel.clearToast()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.export_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack, enabled = !uiState.isExporting) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            Text(
                text = stringResource(R.string.export_description),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(vertical = 8.dp),
            )

            Text(
                text = stringResource(R.string.export_format_label),
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
            )

            ExportFormatOption(
                label = stringResource(R.string.export_format_both),
                selected = uiState.format == ExportFormat.BOTH,
                enabled = !uiState.isExporting,
                onSelect = { viewModel.setFormat(ExportFormat.BOTH) },
            )
            ExportFormatOption(
                label = stringResource(R.string.export_format_json),
                selected = uiState.format == ExportFormat.JSON,
                enabled = !uiState.isExporting,
                onSelect = { viewModel.setFormat(ExportFormat.JSON) },
            )
            ExportFormatOption(
                label = stringResource(R.string.export_format_csv),
                selected = uiState.format == ExportFormat.CSV,
                enabled = !uiState.isExporting,
                onSelect = { viewModel.setFormat(ExportFormat.CSV) },
            )

            if (uiState.isExporting) {
                Spacer(modifier = Modifier.height(24.dp))
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(
                        R.string.export_in_progress,
                        uiState.progress.message.ifEmpty { "…" },
                    ),
                    style = MaterialTheme.typography.bodySmall,
                )
                TextButton(
                    onClick = viewModel::cancelExport,
                    modifier = Modifier.align(Alignment.End),
                ) {
                    Text(stringResource(R.string.common_cancel))
                }
            }

            uiState.errorMessage?.let { code ->
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = errorMessageForCode(context, code),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            uiState.savedPath?.let { path ->
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = stringResource(R.string.export_saved_to, path),
                    style = MaterialTheme.typography.bodySmall,
                )
                if (uiState.isEmptyData) {
                    Text(
                        text = stringResource(R.string.export_empty_data),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(
                    onClick = viewModel::shareAgain,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.export_share_again))
                }
                TextButton(
                    onClick = { copyPath(context, path) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.export_copy_path))
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = viewModel::startExport,
                enabled = !uiState.isExporting,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (uiState.isExporting) {
                    CircularProgressIndicator(
                        modifier = Modifier.padding(end = 8.dp),
                        strokeWidth = 2.dp,
                    )
                }
                Text(
                    if (uiState.savedPath == null) {
                        stringResource(R.string.export_start)
                    } else {
                        stringResource(R.string.export_start_again)
                    },
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun ExportFormatOption(
    label: String,
    selected: Boolean,
    enabled: Boolean,
    onSelect: () -> Unit,
) {
    androidx.compose.foundation.layout.Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
    ) {
        RadioButton(
            selected = selected,
            onClick = onSelect,
            enabled = enabled,
        )
        Text(
            text = label,
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}

private fun errorMessageForCode(context: Context, code: String): String = when (code) {
    "E6011" -> context.getString(R.string.export_error_storage)
    else -> context.getString(R.string.export_error_failed)
}

private fun shareExportFile(context: Context, file: File) {
    val uri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        file,
    )
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "application/zip"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        clipData = ClipData.newUri(context.contentResolver, file.name, uri)
    }
    val chooser = Intent.createChooser(intent, context.getString(R.string.export_share_title))
    if (chooser.resolveActivity(context.packageManager) != null) {
        context.startActivity(chooser)
    } else {
        Toast.makeText(
            context,
            context.getString(R.string.export_no_share_target, file.absolutePath),
            Toast.LENGTH_LONG,
        ).show()
    }
}

private fun copyPath(context: Context, path: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("export_path", path))
    Toast.makeText(context, context.getString(R.string.export_path_copied), Toast.LENGTH_SHORT).show()
}
