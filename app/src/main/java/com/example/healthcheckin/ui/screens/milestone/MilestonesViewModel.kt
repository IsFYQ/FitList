package com.example.healthcheckin.ui.screens.milestone

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.healthcheckin.data.auth.SessionManager
import com.example.healthcheckin.data.local.dao.ProfileDao
import com.example.healthcheckin.domain.analytics.AnalyticsEvents
import com.example.healthcheckin.domain.analytics.AnalyticsTracker
import com.example.healthcheckin.domain.model.MilestoneAchievementEvent
import com.example.healthcheckin.domain.model.MilestoneItem
import com.example.healthcheckin.domain.model.SaveMilestoneRequest
import com.example.healthcheckin.domain.repository.GoalRepository
import com.example.healthcheckin.domain.repository.MilestoneRepository
import com.example.healthcheckin.domain.repository.WeightRepository
import com.example.healthcheckin.util.GoalType
import com.example.healthcheckin.util.P1ValidationConstants
import com.example.healthcheckin.util.Validators
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MilestoneUiState(
    val active: List<MilestoneItem> = emptyList(),
    val achieved: List<MilestoneItem> = emptyList(),
    val showForm: Boolean = false,
    val editingId: String? = null,
    val title: String = "",
    val targetText: String = "",
    val reward: String = "",
    val canCreateMore: Boolean = true,
    val currentWeightKg: Double? = null,
    val goalType: GoalType = GoalType.LOSE,
    val errorMessage: String? = null,
    val menuTarget: MilestoneItem? = null,
    val achievementQueue: List<MilestoneAchievementEvent> = emptyList(),
)

@HiltViewModel
class MilestonesViewModel @Inject constructor(
    private val milestoneRepository: MilestoneRepository,
    private val weightRepository: WeightRepository,
    private val goalRepository: GoalRepository,
    private val profileDao: ProfileDao,
    private val sessionManager: SessionManager,
    private val analyticsTracker: AnalyticsTracker,
) : ViewModel() {
    private val _uiState = MutableStateFlow(MilestoneUiState())
    val uiState: StateFlow<MilestoneUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val userId = sessionManager.getUserId() ?: return@launch
            combine(
                milestoneRepository.observeMilestones(userId),
                weightRepository.observeAllRecords(userId),
                goalRepository.observeActiveGoal(userId),
            ) { milestones, weights, goal ->
                Triple(milestones, weights.firstOrNull()?.weightKg, goal)
            }.collect { (milestones, currentWeight, goal) ->
                val goalType = goal?.goalType?.let { runCatching { GoalType.valueOf(it) }.getOrNull() } ?: GoalType.LOSE
                val enriched = milestones.map { m ->
                    val remaining = currentWeight?.let { cur ->
                        if (goalType == GoalType.GAIN) (m.targetWeightKg - cur).coerceAtLeast(0.0)
                        else (cur - m.targetWeightKg).coerceAtLeast(0.0)
                    }
                    val progress = if (currentWeight == null || m.achievedAt != null) {
                        if (m.achievedAt != null) 1f else 0f
                    } else {
                        val start = profileDao.getActiveByUserId(userId)?.initialWeightKg ?: currentWeight
                        val total = absSafe(start - m.targetWeightKg)
                        if (total <= 0) 1f else (1f - (remaining!! / total).toFloat()).coerceIn(0f, 1f)
                    }
                    m.copy(remainingKg = remaining, progress = progress)
                }
                _uiState.update {
                    it.copy(
                        active = enriched.filter { m -> m.achievedAt == null },
                        achieved = enriched.filter { m -> m.achievedAt != null },
                        canCreateMore = enriched.count { m -> m.achievedAt == null } < P1ValidationConstants.MILESTONE_ACTIVE_MAX,
                        currentWeightKg = currentWeight,
                        goalType = goalType,
                    )
                }
            }
        }
    }

    fun openCreate() = _uiState.update {
        it.copy(showForm = true, editingId = null, title = "", targetText = "", reward = "", errorMessage = null)
    }

    fun openEdit(item: MilestoneItem) = _uiState.update {
        it.copy(
            showForm = true,
            editingId = item.id,
            title = item.title,
            targetText = item.targetWeightKg.toString(),
            reward = item.rewardText.orEmpty(),
            menuTarget = null,
            errorMessage = null,
        )
    }

    fun dismissForm() = _uiState.update { it.copy(showForm = false, editingId = null) }
    fun updateTitle(v: String) = _uiState.update { it.copy(title = v.take(P1ValidationConstants.MILESTONE_TITLE_MAX)) }
    fun updateTarget(v: String) = _uiState.update { it.copy(targetText = Validators.filterDecimalInput(v)) }
    fun updateReward(v: String) = _uiState.update { it.copy(reward = v.take(P1ValidationConstants.MILESTONE_REWARD_MAX)) }
    fun showMenu(item: MilestoneItem) = _uiState.update { it.copy(menuTarget = item) }
    fun dismissMenu() = _uiState.update { it.copy(menuTarget = null) }

    fun save() {
        val state = _uiState.value
        val title = state.title.trim()
        val target = Validators.parseDecimalInput(state.targetText) ?: return
        if (title.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "title") }
            return
        }
        val current = state.currentWeightKg
        if (current != null) {
            val invalid = if (state.goalType == GoalType.GAIN) target <= current else target >= current
            if (invalid) {
                _uiState.update { it.copy(errorMessage = "direction") }
                return
            }
        }
        viewModelScope.launch {
            val userId = sessionManager.getUserId() ?: return@launch
            val request = SaveMilestoneRequest(title, target, state.reward.takeIf { it.isNotBlank() })
            val result = if (state.editingId == null) {
                milestoneRepository.create(userId, request)
            } else {
                milestoneRepository.update(userId, state.editingId, request)
            }
            result.fold(
                onSuccess = {
                    if (state.editingId == null) {
                        analyticsTracker.track(AnalyticsEvents.MILESTONE_CREATED)
                    }
                    _uiState.update { it.copy(showForm = false, editingId = null) }
                },
                onFailure = {
                    _uiState.update {
                        it.copy(errorMessage = if (it.errorMessage == null) "max" else "save_failed")
                    }
                },
            )
        }
    }

    fun delete(item: MilestoneItem) {
        viewModelScope.launch {
            milestoneRepository.delete(item.id)
            analyticsTracker.track(AnalyticsEvents.MILESTONE_DELETED)
            _uiState.update { it.copy(menuTarget = null) }
        }
    }

    fun reset(item: MilestoneItem) {
        viewModelScope.launch {
            milestoneRepository.reset(item.id)
            analyticsTracker.track(AnalyticsEvents.MILESTONE_RESET)
            _uiState.update { it.copy(menuTarget = null) }
        }
    }

    fun enqueueAchievements(events: List<MilestoneAchievementEvent>) {
        if (events.isEmpty()) return
        _uiState.update { it.copy(achievementQueue = it.achievementQueue + events) }
        events.forEach {
            analyticsTracker.track(
                AnalyticsEvents.MILESTONE_ACHIEVED,
                mapOf("days_elapsed" to it.daysElapsed),
            )
        }
    }

    fun dismissCurrentAchievement() {
        _uiState.update { it.copy(achievementQueue = it.achievementQueue.drop(1)) }
    }

    fun markShared(milestoneId: String) {
        viewModelScope.launch {
            milestoneRepository.markShared(milestoneId)
            analyticsTracker.track(AnalyticsEvents.MILESTONE_SHARED)
        }
    }

    fun clearError() = _uiState.update { it.copy(errorMessage = null) }

    private fun absSafe(v: Double) = kotlin.math.abs(v)
}
