package com.flowisland.android.feature.pomodoro

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
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

enum class PomodoroPreset(val focusMinutes: Int, val breakMinutes: Int) { TWENTY_FIVE_FIVE(25, 5), FIFTY_TEN(50, 10) }

/**
 * Pomodoro is modeled as a single ActivityEngine entry whose title/subtitle/timer
 * flip between "Focus" and "Break" phases -- one activity, one notification, one
 * island entry, exactly as the spec's single-source-of-truth principle requires
 * (not two separate activities that would double up in the switcher).
 */
@HiltViewModel
class PomodoroCreateViewModel @Inject constructor(
    private val activityEngine: ActivityEngine,
) : ViewModel() {

    fun start(preset: PomodoroPreset, autoStartBreak: Boolean, autoStartFocus: Boolean, onStarted: (String) -> Unit) {
        val id = ActivityId.new()
        val state = ActivityUiState(
            id = id,
            type = ActivityType.POMODORO,
            title = "Focus",
            icon = ActivityIconId.POMODORO_FOCUS,
            state = ActivityState.ACTIVE,
            timer = TimerSpec(durationMillis = preset.focusMinutes * 60_000L),
            actions = ActivityActions.countdownRunning(includeAddMinute = false),
            payloadId = "${preset.focusMinutes}:${preset.breakMinutes}:${if (autoStartBreak) 1 else 0}:${if (autoStartFocus) 1 else 0}:focus",
        )
        activityEngine.upsert(state)
        onStarted(id.value)
    }
}

@Composable
fun PomodoroCreateScreen(onStarted: (String) -> Unit, onBack: () -> Unit) {
    val viewModel: PomodoroCreateViewModel = hiltViewModel()
    var preset by remember { mutableStateOf(PomodoroPreset.TWENTY_FIVE_FIVE) }
    var autoStartBreak by remember { mutableStateOf(true) }
    var autoStartFocus by remember { mutableStateOf(false) }

    Scaffold { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(ScreenPadding), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(stringResource(R.string.type_pomodoro), style = MaterialTheme.typography.headlineMedium)

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = preset == PomodoroPreset.TWENTY_FIVE_FIVE, onClick = { preset = PomodoroPreset.TWENTY_FIVE_FIVE }, label = { Text(stringResource(R.string.pomodoro_preset_25_5)) })
                FilterChip(selected = preset == PomodoroPreset.FIFTY_TEN, onClick = { preset = PomodoroPreset.FIFTY_TEN }, label = { Text(stringResource(R.string.pomodoro_preset_50_10)) })
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.pomodoro_auto_start_break), modifier = Modifier.weight(1f))
                Switch(checked = autoStartBreak, onCheckedChange = { autoStartBreak = it })
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.pomodoro_auto_start_focus), modifier = Modifier.weight(1f))
                Switch(checked = autoStartFocus, onCheckedChange = { autoStartFocus = it })
            }

            PrimaryButton(text = stringResource(R.string.action_start), onClick = { viewModel.start(preset, autoStartBreak, autoStartFocus, onStarted) })
        }
    }
}
