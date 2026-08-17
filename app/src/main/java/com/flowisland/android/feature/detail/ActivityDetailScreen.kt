package com.flowisland.android.feature.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flowisland.android.R
import com.flowisland.android.core.activity.ActivityEngine
import com.flowisland.android.core.activity.model.ActivityAction
import com.flowisland.android.core.activity.model.ActivityId
import com.flowisland.android.core.activity.model.ActivityType
import com.flowisland.android.core.activity.model.ActivityUiState
import com.flowisland.android.core.database.ExpenseDao
import com.flowisland.android.core.reminder.ReminderScheduler
import com.flowisland.android.core.time.TimeFormat
import com.flowisland.android.core.ui.components.ExpandedIslandCard
import com.flowisland.android.core.ui.components.ScreenPadding
import com.flowisland.android.feature.cooking.CookingStepAdvancer
import com.flowisland.android.feature.cooking.CookingStepStore
import com.flowisland.android.feature.delivery.DeliveryStatusManager
import com.flowisland.android.feature.fitness.FitnessSessionTracker
import com.flowisland.android.feature.stopwatch.LapStore
import com.flowisland.android.feature.trip.TripSessionTracker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ActivityDetailViewModel @Inject constructor(
    private val activityEngine: ActivityEngine,
    val expenseDao: ExpenseDao,
    val fitnessSessionTracker: FitnessSessionTracker,
    val tripSessionTracker: TripSessionTracker,
    private val reminderScheduler: ReminderScheduler,
    private val cookingStepAdvancer: CookingStepAdvancer,
    private val deliveryStatusManager: DeliveryStatusManager,
) : ViewModel() {

    fun observe(id: String): StateFlow<ActivityUiState?> = activityEngine.observe(ActivityId(id))

    fun handleAction(id: String, action: ActivityAction) {
        val activityId = ActivityId(id)
        val current = activityEngine.get(activityId)
        when (action.kind) {
            ActivityAction.Kind.PAUSE -> {
                if (current?.type == ActivityType.MEDIA) com.flowisland.android.feature.media.MediaSessionListenerService.activeInstance?.handlePlayPause(resume = false)
                else activityEngine.pause(activityId)
            }
            ActivityAction.Kind.RESUME -> {
                if (current?.type == ActivityType.MEDIA) com.flowisland.android.feature.media.MediaSessionListenerService.activeInstance?.handlePlayPause(resume = true)
                else activityEngine.resume(activityId)
            }
            ActivityAction.Kind.CANCEL -> {
                if (current?.type == ActivityType.FITNESS) fitnessSessionTracker.cancel(id)
                if (current?.type == ActivityType.TRIP) tripSessionTracker.cancel(id)
                activityEngine.cancel(activityId)
            }
            ActivityAction.Kind.FINISH -> {
                when (current?.type) {
                    ActivityType.FITNESS -> viewModelScope.launch {
                        fitnessSessionTracker.finish(id, current.payloadId ?: "CUSTOM", current.title)
                        activityEngine.complete(activityId)
                    }
                    ActivityType.TRIP -> viewModelScope.launch {
                        tripSessionTracker.finish(id, current.subtitle)
                        activityEngine.complete(activityId)
                    }
                    ActivityType.EXPENSE -> viewModelScope.launch {
                        current.payloadId?.let { expenseDao.archiveTrip(it) }
                        activityEngine.complete(activityId)
                    }
                    else -> activityEngine.complete(activityId)
                }
            }
            ActivityAction.Kind.ADD_1_MIN -> activityEngine.update(activityId) { it.copy(timer = it.timer?.addDuration(60_000)) }
            ActivityAction.Kind.ADD_5_MIN -> activityEngine.update(activityId) { it.copy(timer = it.timer?.addDuration(300_000)) }
            ActivityAction.Kind.LAP -> current?.timer?.let { LapStore.addLap(id, it.elapsedMillis()) }
            ActivityAction.Kind.DONE -> { activityEngine.complete(activityId); reminderScheduler.markDone(id) }
            ActivityAction.Kind.SNOOZE -> reminderScheduler.snooze(id, minutes = 10)
            ActivityAction.Kind.CUSTOM -> {
                when (current?.type) {
                    ActivityType.COOKING -> cookingStepAdvancer.advanceToNextStep(current)
                    ActivityType.DELIVERY -> deliveryStatusManager.advance(id)
                    else -> Unit
                }
            }
            else -> Unit
        }
        activityEngine.markInteracted(activityId)
    }

    fun markInteracted(id: String) = activityEngine.markInteracted(ActivityId(id))
}

@Composable
fun ActivityDetailScreen(activityId: String, onBack: () -> Unit, onAddExpense: (String, String) -> Unit = { _, _ -> }) {
    val viewModel: ActivityDetailViewModel = hiltViewModel()
    val state by viewModel.observe(activityId).collectAsState()

    LaunchedEffect(activityId) { viewModel.markInteracted(activityId) }

    Scaffold { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(ScreenPadding)) {
            val currentState = state
            if (currentState == null) {
                Text(stringResource(R.string.home_nothing_active_title), style = MaterialTheme.typography.titleLarge)
                return@Column
            }
            val context = androidx.compose.ui.platform.LocalContext.current
            ExpandedIslandCard(
                state = currentState,
                onAction = { action ->
                    if (action.kind == ActivityAction.Kind.OPEN_NAVIGATION) {
                        com.flowisland.android.feature.trip.openNavigationIntent(context, currentState.payloadId)
                    } else {
                        viewModel.handleAction(activityId, action)
                    }
                },
                onCollapse = onBack,
                extraContent = { TypeSpecificContent(currentState, viewModel, onAddExpense) },
            )
        }
    }
}

@Composable
private fun TypeSpecificContent(state: ActivityUiState, viewModel: ActivityDetailViewModel, onAddExpense: (String, String) -> Unit) {
    when (state.type) {
        ActivityType.STOPWATCH -> StopwatchLaps(state.id.value)
        ActivityType.COOKING -> CookingSteps(state.id.value)
        ActivityType.FITNESS -> FitnessStats(state.id.value, viewModel)
        ActivityType.TRIP -> TripStats(state.id.value, viewModel)
        ActivityType.EXPENSE -> {
            val tripId = state.payloadId
            if (tripId != null) {
                com.flowisland.android.core.ui.components.SecondaryButton(
                    text = stringResource(R.string.expense_add),
                    onClick = { onAddExpense(state.id.value, tripId) },
                    modifier = Modifier.padding(bottom = 16.dp),
                )
            }
        }
        ActivityType.DELIVERY -> Text(state.subtitle.orEmpty(), style = MaterialTheme.typography.bodyMedium)
        ActivityType.FLIGHT -> Text(stringResource(R.string.flight_manual_disclosure), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        ActivityType.REMINDER -> Text(state.subtitle.orEmpty(), style = MaterialTheme.typography.bodyMedium)
        else -> Unit
    }
}

@Composable
private fun StopwatchLaps(activityId: String) {
    val laps = LapStore.lapsFor(activityId)
    if (laps.isEmpty()) return
    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        laps.reversed().forEachIndexed { index, millis ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Lap ${laps.size - index}", style = MaterialTheme.typography.bodyMedium)
                Text(TimeFormat.stopwatch(millis), style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun CookingSteps(activityId: String) {
    val steps = CookingStepStore.stepsFor(activityId)
    if (steps.isEmpty()) return
    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        steps.forEachIndexed { index, step ->
            val isCurrent = index == CookingStepStore.currentIndex(activityId)
            Text(
                "${index + 1}. ${step.name}",
                style = if (isCurrent) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
                color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun FitnessStats(activityId: String, viewModel: ActivityDetailViewModel) {
    val stats by viewModel.fitnessSessionTracker.observe(activityId).collectAsState()
    Row(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        StatColumn("Distance", "%.2f km".format(stats.distanceMeters / 1000.0))
        StatColumn("Pace", stats.paceLabel())
    }
}

@Composable
private fun TripStats(activityId: String, viewModel: ActivityDetailViewModel) {
    val stats by viewModel.tripSessionTracker.observe(activityId).collectAsState()
    Row(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        StatColumn("Distance", "%.1f km".format(stats.distanceMeters / 1000.0))
        StatColumn(stringResource(R.string.trip_average_speed), "%.0f km/h".format(stats.averageSpeedKmh()))
    }
}

@Composable
private fun StatColumn(label: String, value: String) {
    Column {
        Text(value, style = MaterialTheme.typography.titleLarge)
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
