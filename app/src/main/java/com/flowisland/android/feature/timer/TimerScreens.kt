package com.flowisland.android.feature.timer

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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

@HiltViewModel
class TimerCreateViewModel @Inject constructor(
    private val activityEngine: ActivityEngine,
) : ViewModel() {

    fun start(label: String, durationMillis: Long, onStarted: (String) -> Unit) {
        val id = ActivityId.new()
        val state = ActivityUiState(
            id = id,
            type = ActivityType.TIMER,
            title = label.ifBlank { "Timer" },
            icon = ActivityIconId.TIMER,
            state = ActivityState.ACTIVE,
            timer = TimerSpec(durationMillis = durationMillis),
            actions = ActivityActions.countdownRunning(),
        )
        activityEngine.upsert(state)
        onStarted(id.value)
    }
}

private val presetMinutes = listOf(1, 5, 10, 15, 25, 45, 60)

@Composable
fun TimerCreateScreen(onStarted: (String) -> Unit, onBack: () -> Unit) {
    val viewModel: TimerCreateViewModel = hiltViewModel()
    var label by remember { mutableStateOf("") }
    var selectedMinutes by remember { mutableIntStateOf(10) }

    Scaffold { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(ScreenPadding), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(stringResource(R.string.timer_new), style = MaterialTheme.typography.headlineMedium)

            OutlinedTextField(
                value = label,
                onValueChange = { label = it },
                label = { Text(stringResource(R.string.timer_label_hint)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )

            Text(stringResource(R.string.timer_duration), style = MaterialTheme.typography.labelLarge)
            MinutePresetChips(selectedMinutes) { selectedMinutes = it }

            PrimaryButton(
                text = stringResource(R.string.action_start),
                onClick = { viewModel.start(label, selectedMinutes * 60_000L, onStarted) },
            )
        }
    }
}

@Composable
fun MinutePresetChips(selected: Int, onSelect: (Int) -> Unit) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
    ) {
        presetMinutes.forEach { minutes ->
            FilterChip(
                selected = selected == minutes,
                onClick = { onSelect(minutes) },
                label = { Text("${minutes}m") },
            )
        }
    }
}
