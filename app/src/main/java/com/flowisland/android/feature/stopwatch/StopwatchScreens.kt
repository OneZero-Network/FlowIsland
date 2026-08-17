package com.flowisland.android.feature.stopwatch

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import com.flowisland.android.R
import com.flowisland.android.core.activity.ActivityActions
import com.flowisland.android.core.activity.ActivityEngine
import com.flowisland.android.core.activity.model.ActivityIconId
import com.flowisland.android.core.activity.model.ActivityId
import com.flowisland.android.core.activity.model.ActivityState
import com.flowisland.android.core.activity.model.ActivityType
import com.flowisland.android.core.activity.model.ActivityUiState
import com.flowisland.android.core.time.TimerSpec
import com.flowisland.android.core.ui.components.PrimaryButton
import com.flowisland.android.core.ui.components.ScreenPadding
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/** In-memory lap store, keyed by activity id -- laps are a live-session detail, not a persisted history record. */
object LapStore {
    private val laps = mutableMapOf<String, MutableList<Long>>()

    fun addLap(activityId: String, elapsedMillis: Long) {
        laps.getOrPut(activityId) { mutableListOf() }.add(elapsedMillis)
    }

    fun lapsFor(activityId: String): List<Long> = laps[activityId]?.toList() ?: emptyList()

    fun clear(activityId: String) {
        laps.remove(activityId)
    }
}

@HiltViewModel
class StopwatchCreateViewModel @Inject constructor(
    private val activityEngine: ActivityEngine,
) : ViewModel() {

    fun start(onStarted: (String) -> Unit) {
        val id = ActivityId.new()
        val state = ActivityUiState(
            id = id,
            type = ActivityType.STOPWATCH,
            title = "Stopwatch",
            icon = ActivityIconId.STOPWATCH,
            state = ActivityState.ACTIVE,
            timer = TimerSpec(durationMillis = 0L, countUp = true),
            actions = ActivityActions.stopwatchRunning(),
        )
        activityEngine.upsert(state)
        onStarted(id.value)
    }
}

@Composable
fun StopwatchCreateScreen(onStarted: (String) -> Unit, onBack: () -> Unit) {
    val viewModel: StopwatchCreateViewModel = hiltViewModel()
    Scaffold { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(ScreenPadding), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(stringResource(R.string.stopwatch_title), style = MaterialTheme.typography.headlineMedium)
            PrimaryButton(text = stringResource(R.string.action_start), onClick = { viewModel.start(onStarted) })
        }
    }
}
