package com.example.healthcheckin.ui.screens.milestone

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.healthcheckin.R
import com.example.healthcheckin.domain.model.MilestoneAchievementEvent
import com.example.healthcheckin.domain.model.MilestoneItem
import com.example.healthcheckin.ui.components.AppEmptyState
import com.example.healthcheckin.ui.theme.HealthCheckInDimens
import com.example.healthcheckin.util.DateTimeUtil
import com.example.healthcheckin.util.GoalType
import com.example.healthcheckin.util.PrecisionUtil
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MilestonesScreen(
    onBack: () -> Unit,
    viewModel: MilestonesViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { key ->
            val msg = when (key) {
                "direction" -> {
                    val w = uiState.currentWeightKg ?: 0.0
                    if (uiState.goalType == GoalType.GAIN) {
                        context.getString(R.string.milestone_direction_error_gain, w)
                    } else {
                        context.getString(R.string.milestone_direction_error, w)
                    }
                }
                "max" -> context.getString(R.string.milestone_max_toast)
                "title" -> context.getString(R.string.milestone_name)
                else -> context.getString(R.string.milestone_save_failed)
            }
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            viewModel.clearError()
        }
    }

    uiState.menuTarget?.let { target ->
        AlertDialog(
            onDismissRequest = viewModel::dismissMenu,
            title = { Text(target.title) },
            text = {
                Column {
                    TextButton(onClick = { viewModel.openEdit(target) }) { Text(stringResource(R.string.milestone_edit)) }
                    if (target.achievedAt != null) {
                        TextButton(onClick = { viewModel.reset(target) }) { Text(stringResource(R.string.milestone_reset)) }
                    }
                    TextButton(onClick = { viewModel.delete(target) }) { Text(stringResource(R.string.milestone_delete)) }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = viewModel::dismissMenu) { Text(stringResource(R.string.common_cancel)) }
            },
        )
    }

    if (uiState.showForm) {
        AlertDialog(
            onDismissRequest = viewModel::dismissForm,
            title = { Text(stringResource(R.string.milestone_create)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = uiState.title,
                        onValueChange = viewModel::updateTitle,
                        label = { Text(stringResource(R.string.milestone_name)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = uiState.targetText,
                        onValueChange = viewModel::updateTarget,
                        label = { Text(stringResource(R.string.milestone_target_weight)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = uiState.reward,
                        onValueChange = viewModel::updateReward,
                        label = { Text(stringResource(R.string.milestone_reward)) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = viewModel::save) { Text(stringResource(R.string.common_save)) }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissForm) { Text(stringResource(R.string.common_cancel)) }
            },
        )
    }

    uiState.achievementQueue.firstOrNull()?.let { event ->
        AchievementDialog(
            event = event,
            onClose = viewModel::dismissCurrentAchievement,
            onShare = {
                val text = context.getString(
                    R.string.milestone_share_text,
                    event.title,
                    event.achievedWeightKg,
                    event.daysElapsed,
                )
                context.startActivity(
                    Intent.createChooser(
                        Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, text)
                        },
                        context.getString(R.string.milestone_share),
                    ),
                )
                viewModel.markShared(event.milestoneId)
            },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.milestone_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (uiState.canCreateMore) viewModel.openCreate()
                    else Toast.makeText(context, context.getString(R.string.milestone_max_toast), Toast.LENGTH_SHORT).show()
                },
            ) { Icon(Icons.Default.Add, contentDescription = null) }
        },
    ) { padding ->
        if (uiState.active.isEmpty() && uiState.achieved.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                AppEmptyState(title = stringResource(R.string.milestone_empty))
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(HealthCheckInDimens.PagePadding),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (uiState.active.isNotEmpty()) {
                    item { Text(stringResource(R.string.milestone_active), fontWeight = FontWeight.Bold) }
                    items(uiState.active, key = { it.id }) { item ->
                        MilestoneRow(item, onLongClick = { viewModel.showMenu(item) }, onShare = null)
                    }
                }
                if (uiState.achieved.isNotEmpty()) {
                    item { Text(stringResource(R.string.milestone_achieved), fontWeight = FontWeight.Bold) }
                    items(uiState.achieved, key = { it.id }) { item ->
                        MilestoneRow(
                            item,
                            onLongClick = { viewModel.showMenu(item) },
                            onShare = {
                                val text = context.getString(
                                    R.string.milestone_share_text,
                                    item.title,
                                    item.achievedWeightKg ?: item.targetWeightKg,
                                    item.daysElapsed ?: 0,
                                )
                                context.startActivity(
                                    Intent.createChooser(
                                        Intent(Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(Intent.EXTRA_TEXT, text)
                                        },
                                        context.getString(R.string.milestone_share),
                                    ),
                                )
                                viewModel.markShared(item.id)
                            },
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MilestoneRow(
    item: MilestoneItem,
    onLongClick: () -> Unit,
    onShare: (() -> Unit)?,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = {}, onLongClick = onLongClick)
            .padding(vertical = 8.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column(modifier = Modifier.weight(1f)) {
                Text(item.title, style = MaterialTheme.typography.titleMedium)
                Text(stringResource(R.string.milestone_target_label, item.targetWeightKg))
                item.rewardText?.takeIf { it.isNotBlank() }?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (onShare != null) {
                IconButton(onClick = onShare) { Icon(Icons.Default.Share, contentDescription = null) }
            }
        }
        if (item.achievedAt == null) {
            item.remainingKg?.let {
                Text(stringResource(R.string.milestone_remaining, it))
                Spacer(modifier = Modifier.height(4.dp))
                LinearProgressIndicator(progress = { item.progress }, modifier = Modifier.fillMaxWidth())
            }
        } else {
            val date = item.achievedAt?.let { DateTimeUtil.toLocalDateString(it) }.orEmpty()
            Text(stringResource(R.string.milestone_achieved_at, date, item.daysElapsed ?: 0))
        }
    }
}

@Composable
fun AchievementDialog(
    event: MilestoneAchievementEvent,
    onClose: () -> Unit,
    onShare: () -> Unit,
) {
    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.8f))
                .clickable { onClose() },
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp)
                    .background(MaterialTheme.colorScheme.surface, MaterialTheme.shapes.large)
                    .padding(24.dp)
                    .clickable(enabled = false) {},
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(
                    Icons.Default.EmojiEvents,
                    contentDescription = null,
                    tint = Color(0xFFFFC107),
                    modifier = Modifier.height(72.dp),
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(event.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(stringResource(R.string.milestone_achieved_weight, event.achievedWeightKg))
                Text(stringResource(R.string.milestone_days_elapsed, event.daysElapsed))
                event.rewardText?.takeIf { it.isNotBlank() }?.let {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(it)
                }
                Spacer(modifier = Modifier.height(16.dp))
                TextButton(onClick = onShare) { Text(stringResource(R.string.milestone_share)) }
                TextButton(onClick = onClose) { Text(stringResource(R.string.milestone_close)) }
            }
        }
    }
}

/** Host helper for weight screen achievement queue with 500ms gap. */
@Composable
fun AchievementQueueHost(
    queue: List<MilestoneAchievementEvent>,
    onDismiss: () -> Unit,
    onShare: (MilestoneAchievementEvent) -> Unit,
) {
    val current = queue.firstOrNull() ?: return
    var ready by remember { mutableStateOf(true) }
    LaunchedEffect(current.milestoneId) {
        ready = false
        delay(50)
        ready = true
    }
    if (ready) {
        val context = LocalContext.current
        AchievementDialog(
            event = current,
            onClose = {
                onDismiss()
            },
            onShare = {
                val text = context.getString(
                    R.string.milestone_share_text,
                    current.title,
                    current.achievedWeightKg,
                    current.daysElapsed,
                )
                context.startActivity(
                    Intent.createChooser(
                        Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, text)
                        },
                        context.getString(R.string.milestone_share),
                    ),
                )
                onShare(current)
            },
        )
    }
}
