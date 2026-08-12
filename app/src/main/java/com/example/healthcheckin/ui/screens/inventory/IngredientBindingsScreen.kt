package com.example.healthcheckin.ui.screens.inventory

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.example.healthcheckin.R
import com.example.healthcheckin.data.auth.SessionManager
import com.example.healthcheckin.domain.analytics.AnalyticsEvents
import com.example.healthcheckin.domain.analytics.AnalyticsTracker
import com.example.healthcheckin.domain.model.IngredientBindingItem
import com.example.healthcheckin.domain.repository.IngredientBindingRepository
import com.example.healthcheckin.ui.components.AppEmptyState
import com.example.healthcheckin.ui.theme.HealthCheckInDimens
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class IngredientBindingsViewModel @Inject constructor(
    private val repository: IngredientBindingRepository,
    private val sessionManager: SessionManager,
    private val analyticsTracker: AnalyticsTracker,
) : ViewModel() {
    private val userId = MutableStateFlow<String?>(null)

    init {
        viewModelScope.launch { userId.value = sessionManager.getUserId() }
    }

    val bindings = userId.flatMapLatest { id ->
        if (id == null) flowOf(emptyList()) else repository.observeBindings(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun unbind(item: IngredientBindingItem) {
        viewModelScope.launch {
            repository.unbind(item.id)
            analyticsTracker.track(AnalyticsEvents.INGREDIENT_BINDING_REMOVED)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IngredientBindingsScreen(
    onBack: () -> Unit,
    viewModel: IngredientBindingsViewModel = hiltViewModel(),
) {
    val bindings by viewModel.bindings.collectAsStateWithLifecycle()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.binding_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
            )
        },
    ) { padding ->
        if (bindings.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                AppEmptyState(title = stringResource(R.string.binding_empty))
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(HealthCheckInDimens.PagePadding),
                verticalArrangement = Arrangement.spacedBy(HealthCheckInDimens.Space2),
            ) {
                items(bindings, key = { it.id }) { item ->
                    val dismissState = rememberSwipeToDismissBoxState(
                        confirmValueChange = {
                            if (it == SwipeToDismissBoxValue.EndToStart) {
                                viewModel.unbind(item)
                                true
                            } else {
                                false
                            }
                        },
                    )
                    SwipeToDismissBox(
                        state = dismissState,
                        backgroundContent = {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                horizontalArrangement = Arrangement.End,
                            ) {
                                Text(stringResource(R.string.binding_unbind))
                            }
                        },
                    ) {
                        Text(
                            stringResource(R.string.binding_pair, item.foodName, item.inventoryName),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                        )
                    }
                }
            }
        }
    }
}
