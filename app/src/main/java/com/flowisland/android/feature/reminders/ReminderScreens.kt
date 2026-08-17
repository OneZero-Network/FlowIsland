package com.flowisland.android.feature.reminders

import android.app.TimePickerDialog
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flowisland.android.R
import com.flowisland.android.core.activity.ActivityActions
import com.flowisland.android.core.activity.ActivityEngine
import com.flowisland.android.core.activity.model.ActivityIconId
import com.flowisland.android.core.activity.model.ActivityId
import com.flowisland.android.core.activity.model.ActivityState
import com.flowisland.android.core.activity.model.ActivityType
import com.flowisland.android.core.activity.model.ActivityUiState
import com.flowisland.android.core.database.ReminderDao
import com.flowisland.android.core.database.ReminderEntity
import com.flowisland.android.core.di.IoDispatcher
import com.flowisland.android.core.reminder.ReminderScheduler
import com.flowisland.android.core.time.TimeFormat
import com.flowisland.android.core.ui.components.PrimaryButton
import com.flowisland.android.core.ui.components.ScreenPadding
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar
import javax.inject.Inject

@HiltViewModel
class ReminderCreateViewModel @Inject constructor(
    private val activityEngine: ActivityEngine,
    private val reminderDao: ReminderDao,
    private val reminderScheduler: ReminderScheduler,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : ViewModel() {

    fun create(title: String, triggerAtMillis: Long, onStarted: (String) -> Unit) {
        if (title.isBlank() || triggerAtMillis <= System.currentTimeMillis()) return
        val id = ActivityId.new()
        viewModelScope.launch {
            val entity = ReminderEntity(id = id.value, title = title, triggerAtMillis = triggerAtMillis, createdAt = System.currentTimeMillis())
            withContext(ioDispatcher) { reminderDao.insert(entity) }
            reminderScheduler.schedule(entity)

            val state = ActivityUiState(
                id = id,
                type = ActivityType.REMINDER,
                title = title,
                subtitle = TimeFormat.relativeMinutes(triggerAtMillis - System.currentTimeMillis()).let { "in $it min" },
                icon = ActivityIconId.REMINDER,
                state = ActivityState.PAUSED, // "scheduled, not yet firing" -- becomes ACTIVE when the alarm actually fires
                actions = ActivityActions.reminderActions(),
                expirationTime = triggerAtMillis,
            )
            activityEngine.upsert(state)
            onStarted(id.value)
        }
    }
}

@Composable
fun ReminderCreateScreen(onStarted: (String) -> Unit, onBack: () -> Unit) {
    val viewModel: ReminderCreateViewModel = hiltViewModel()
    val context = LocalContext.current
    var title by remember { mutableStateOf("") }
    var selectedCalendar by remember {
        mutableStateOf(Calendar.getInstance().apply { add(Calendar.MINUTE, 30) })
    }

    Scaffold { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(ScreenPadding), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(stringResource(R.string.reminder_new), style = MaterialTheme.typography.headlineMedium)
            OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text(stringResource(R.string.reminder_title_hint)) }, modifier = Modifier.fillMaxWidth())

            val timeLabel = String.format("%02d:%02d", selectedCalendar.get(Calendar.HOUR_OF_DAY), selectedCalendar.get(Calendar.MINUTE))
            com.flowisland.android.core.ui.components.SecondaryButton(
                text = "${stringResource(R.string.reminder_time_hint)}: $timeLabel",
                onClick = {
                    TimePickerDialog(
                        context,
                        { _, hour, minute ->
                            val cal = Calendar.getInstance()
                            cal.set(Calendar.HOUR_OF_DAY, hour)
                            cal.set(Calendar.MINUTE, minute)
                            cal.set(Calendar.SECOND, 0)
                            if (cal.timeInMillis <= System.currentTimeMillis()) cal.add(Calendar.DAY_OF_YEAR, 1)
                            selectedCalendar = cal
                        },
                        selectedCalendar.get(Calendar.HOUR_OF_DAY),
                        selectedCalendar.get(Calendar.MINUTE),
                        true,
                    ).show()
                },
            )

            PrimaryButton(text = stringResource(R.string.action_start), onClick = { viewModel.create(title, selectedCalendar.timeInMillis, onStarted) })
        }
    }
}
