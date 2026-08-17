package com.flowisland.android.feature.study

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import com.flowisland.android.feature.timer.MinutePresetChips
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/** Tracks which running activities are STUDY type and their start time, so a Finish/Cancel can write the right StudySessionEntity. */
object StudySessionRegistry {
    private val startTimes = mutableMapOf<String, Long>()
    private val subjects = mutableMapOf<String, String>()
    private val plannedDurations = mutableMapOf<String, Long>()

    fun register(activityId: String, subject: String, plannedDurationMillis: Long) {
        startTimes[activityId] = System.currentTimeMillis()
        subjects[activityId] = subject
        plannedDurations[activityId] = plannedDurationMillis
    }

    fun consume(activityId: String): Triple<String, Long, Long>? {
        val start = startTimes.remove(activityId) ?: return null
        val subject = subjects.remove(activityId) ?: "Study"
        val planned = plannedDurations.remove(activityId) ?: 0L
        return Triple(subject, start, planned)
    }
}

@HiltViewModel
class StudyCreateViewModel @Inject constructor(private val activityEngine: ActivityEngine) : ViewModel() {

    fun start(subject: String, minutes: Int, onStarted: (String) -> Unit) {
        val id = ActivityId.new()
        val durationMillis = minutes * 60_000L
        StudySessionRegistry.register(id.value, subject.ifBlank { "Study" }, durationMillis)
        val state = ActivityUiState(
            id = id,
            type = ActivityType.STUDY,
            title = subject.ifBlank { "Study" },
            icon = ActivityIconId.STUDY,
            state = ActivityState.ACTIVE,
            timer = TimerSpec(durationMillis = durationMillis),
            actions = ActivityActions.simpleFinishCancel(),
        )
        activityEngine.upsert(state)
        onStarted(id.value)
    }
}

@Composable
fun StudyCreateScreen(onStarted: (String) -> Unit, onBack: () -> Unit) {
    val viewModel: StudyCreateViewModel = hiltViewModel()
    var subject by remember { mutableStateOf("") }
    var minutes by remember { mutableIntStateOf(25) }

    Scaffold { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(ScreenPadding), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(stringResource(R.string.study_new_session), style = MaterialTheme.typography.headlineMedium)
            OutlinedTextField(value = subject, onValueChange = { subject = it }, label = { Text(stringResource(R.string.study_subject_hint)) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            MinutePresetChips(minutes) { minutes = it }
            PrimaryButton(text = stringResource(R.string.action_start), onClick = { viewModel.start(subject, minutes, onStarted) })
        }
    }
}
