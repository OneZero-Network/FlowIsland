package com.flowisland.android.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import com.flowisland.android.R
import com.flowisland.android.core.activity.ActivityEngine
import com.flowisland.android.core.activity.model.ActivityId
import com.flowisland.android.core.activity.model.ActivityIconId
import com.flowisland.android.core.activity.model.ActivityType
import com.flowisland.android.core.ui.components.ActivityCard
import com.flowisland.android.core.ui.components.EmptyState
import com.flowisland.android.core.ui.components.SectionHeader
import com.flowisland.android.core.ui.components.toImageVector
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(private val activityEngine: ActivityEngine) : ViewModel() {
    val activities = activityEngine.activities

    fun pin(id: String) = activityEngine.pin(ActivityId(id))
    fun unpin(id: String) = activityEngine.unpin(ActivityId(id))
    fun hide(id: String) = activityEngine.hide(ActivityId(id))
    fun stop(id: String) = activityEngine.cancel(ActivityId(id))
}

data class QuickStartEntry(val type: ActivityType, val icon: ActivityIconId, val labelResId: Int, val route: String)

fun quickStartEntries(): List<QuickStartEntry> = listOf(
    QuickStartEntry(ActivityType.TIMER, ActivityIconId.TIMER, R.string.type_timer, "new_timer"),
    QuickStartEntry(ActivityType.POMODORO, ActivityIconId.POMODORO_FOCUS, R.string.type_pomodoro, "new_pomodoro"),
    QuickStartEntry(ActivityType.STOPWATCH, ActivityIconId.STOPWATCH, R.string.type_stopwatch, "new_stopwatch"),
    QuickStartEntry(ActivityType.STUDY, ActivityIconId.STUDY, R.string.type_study, "new_study"),
    QuickStartEntry(ActivityType.COOKING, ActivityIconId.COOKING, R.string.type_cooking, "new_cooking"),
    QuickStartEntry(ActivityType.FITNESS, ActivityIconId.FITNESS_RUN, R.string.type_fitness, "new_fitness"),
    QuickStartEntry(ActivityType.TRIP, ActivityIconId.TRIP, R.string.type_trip, "new_trip"),
    QuickStartEntry(ActivityType.EXPENSE, ActivityIconId.EXPENSE, R.string.type_expense, "new_expense"),
    QuickStartEntry(ActivityType.REMINDER, ActivityIconId.REMINDER, R.string.type_reminder, "new_reminder"),
    QuickStartEntry(ActivityType.DELIVERY, ActivityIconId.DELIVERY, R.string.type_delivery, "new_delivery"),
    QuickStartEntry(ActivityType.FLIGHT, ActivityIconId.FLIGHT, R.string.type_flight, "new_flight"),
    QuickStartEntry(ActivityType.MEDIA, ActivityIconId.MEDIA, R.string.type_media, "media_setup"),
    QuickStartEntry(ActivityType.DOWNLOAD, ActivityIconId.DOWNLOAD, R.string.type_download, "local_exports"),
    QuickStartEntry(ActivityType.AI_TASK, ActivityIconId.AI_TASK, R.string.type_ai, "local_tasks"),
)

@Composable
fun HomeScreen(onOpenActivity: (String) -> Unit, onQuickStart: (String) -> Unit, onSettings: () -> Unit, onHistory: () -> Unit) {
    val viewModel: HomeViewModel = hiltViewModel()
    val activities by viewModel.activities.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(stringResource(R.string.app_name), style = MaterialTheme.typography.titleLarge)
                        Text(stringResource(R.string.app_tagline), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                actions = {
                    IconButton(onClick = onHistory) { Icon(Icons.Filled.History, contentDescription = stringResource(R.string.home_history)) }
                    IconButton(onClick = onSettings) { Icon(Icons.Filled.Settings, contentDescription = stringResource(R.string.settings_title)) }
                },
            )
        },
    ) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp)) {
            item {
                SectionHeader(stringResource(R.string.home_active_now))
            }
            if (activities.isEmpty()) {
                item {
                    EmptyState(
                        title = stringResource(R.string.home_nothing_active_title),
                        body = stringResource(R.string.home_nothing_active_body),
                    )
                }
            } else {
                items(activities, key = { it.id.value }) { activity ->
                    ActivityCard(
                        state = activity,
                        onOpen = { onOpenActivity(activity.id.value) },
                        onPin = { viewModel.pin(activity.id.value) },
                        onUnpin = { viewModel.unpin(activity.id.value) },
                        onHide = { viewModel.hide(activity.id.value) },
                        onStop = { viewModel.stop(activity.id.value) },
                        modifier = Modifier.padding(bottom = 12.dp),
                    )
                }
            }

            item {
                SectionHeader(stringResource(R.string.home_quick_start), modifier = Modifier.padding(top = 20.dp))
            }
            item {
                QuickStartGrid(onQuickStart)
            }
        }
    }
}

@Composable
private fun QuickStartGrid(onQuickStart: (String) -> Unit) {
    val entries = quickStartEntries()
    LazyVerticalGrid(
        columns = GridCells.Fixed(4),
        modifier = Modifier.fillMaxWidth(),
        userScrollEnabled = false,
    ) {
        items(entries) { entry ->
            QuickStartTile(entry, onClick = { onQuickStart(entry.route) })
        }
    }
}

@Composable
private fun QuickStartTile(entry: QuickStartEntry, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(6.dp),
    ) {
        Surface(
            onClick = onClick,
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(vertical = 16.dp).fillMaxWidth()) {
                Icon(entry.icon.toImageVector(), contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            }
        }
        Text(
            stringResource(entry.labelResId),
            style = MaterialTheme.typography.labelSmall,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}
