package com.flowisland.android.feature.flight

import android.app.TimePickerDialog
import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import com.flowisland.android.core.activity.ActivityEngine
import com.flowisland.android.core.activity.model.ActivityIconId
import com.flowisland.android.core.activity.model.ActivityId
import com.flowisland.android.core.activity.model.ActivityState
import com.flowisland.android.core.activity.model.ActivityType
import com.flowisland.android.core.activity.model.ActivityUiState
import com.flowisland.android.core.database.FlightDao
import com.flowisland.android.core.database.FlightEntity
import com.flowisland.android.core.di.ApplicationScope
import com.flowisland.android.core.di.IoDispatcher
import com.flowisland.android.core.ui.components.PrimaryButton
import com.flowisland.android.core.ui.components.ScreenPadding
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

@HiltViewModel
class FlightCreateViewModel @Inject constructor(
    private val activityEngine: ActivityEngine,
    private val flightDao: FlightDao,
    private val flightStatusManager: FlightStatusManager,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : ViewModel() {

    fun start(flightNumber: String, airline: String, from: String, to: String, boardingAt: Long, departureAt: Long, onStarted: (String) -> Unit) {
        if (flightNumber.isBlank() || from.isBlank() || to.isBlank()) return
        val id = ActivityId.new()
        viewModelScope.launch {
            withContext(ioDispatcher) {
                flightDao.insert(
                    FlightEntity(id.value, flightNumber, airline.ifBlank { null }, from, to, boardingAt, departureAt, "UPCOMING", System.currentTimeMillis())
                )
            }
            flightStatusManager.scheduleCheckpoints(id.value, boardingAt, departureAt)
            val state = ActivityUiState(
                id = id,
                type = ActivityType.FLIGHT,
                title = flightNumber,
                subtitle = "$from → $to",
                icon = ActivityIconId.FLIGHT,
                state = ActivityState.ACTIVE,
                actions = emptyList(),
                expirationTime = boardingAt,
                payloadId = id.value,
            )
            activityEngine.upsert(state)
            onStarted(id.value)
        }
    }
}

/**
 * Advances a Flight activity's status label (Upcoming -> Check-in -> Boarding ->
 * Departing -> Completed) purely from the manually-entered timestamps -- never
 * from any live source. Uses three scheduled AlarmManager checkpoints per flight
 * rather than polling, so an idle flight countdown costs zero background wakeups
 * between those points.
 */
@Singleton
class FlightStatusManager @Inject constructor(
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: android.content.Context,
    private val activityEngine: ActivityEngine,
    private val flightDao: FlightDao,
    @ApplicationScope private val scope: CoroutineScope,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {
    private val alarmManager = context.getSystemService(android.content.Context.ALARM_SERVICE) as android.app.AlarmManager

    fun scheduleCheckpoints(flightId: String, boardingAt: Long, departureAt: Long) {
        schedule(flightId, "CHECKIN", boardingAt - 60 * 60_000L)
        schedule(flightId, "BOARDING", boardingAt)
        schedule(flightId, "COMPLETED", departureAt + 10 * 60_000L)
    }

    private fun schedule(flightId: String, label: String, atMillis: Long) {
        if (atMillis <= System.currentTimeMillis()) return
        val intent = android.content.Intent(context, FlightStatusReceiver::class.java).apply {
            putExtra(FlightStatusReceiver.EXTRA_FLIGHT_ID, flightId)
            putExtra(FlightStatusReceiver.EXTRA_STATUS, label)
        }
        val pendingIntent = android.app.PendingIntent.getBroadcast(
            context, (flightId + label).hashCode(), intent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE,
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            alarmManager.set(android.app.AlarmManager.RTC_WAKEUP, atMillis, pendingIntent)
        } else {
            alarmManager.setExactAndAllowWhileIdle(android.app.AlarmManager.RTC_WAKEUP, atMillis, pendingIntent)
        }
    }

    fun applyStatus(flightId: String, status: String) {
        scope.launch {
            withContext(ioDispatcher) { flightDao.updateStatus(flightId, status) }
            val niceLabel = status.lowercase().replaceFirstChar(Char::uppercase)
            activityEngine.update(ActivityId(flightId)) { it.copy(subtitle = niceLabel) }
            if (status == "COMPLETED") activityEngine.complete(ActivityId(flightId))
        }
    }
}

@AndroidEntryPoint
class FlightStatusReceiver : android.content.BroadcastReceiver() {

    @Inject lateinit var flightStatusManager: FlightStatusManager

    companion object {
        const val EXTRA_FLIGHT_ID = "extra_flight_id"
        const val EXTRA_STATUS = "extra_status"
    }

    override fun onReceive(context: android.content.Context, intent: android.content.Intent) {
        val flightId = intent.getStringExtra(EXTRA_FLIGHT_ID) ?: return
        val status = intent.getStringExtra(EXTRA_STATUS) ?: return
        val pendingResult = goAsync()
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Default).launch {
            try {
                flightStatusManager.applyStatus(flightId, status)
            } finally {
                pendingResult.finish()
            }
        }
    }
}

@Composable
fun FlightCreateScreen(onStarted: (String) -> Unit, onBack: () -> Unit) {
    val viewModel: FlightCreateViewModel = hiltViewModel()
    val context = LocalContext.current
    var flightNumber by remember { mutableStateOf("") }
    var airline by remember { mutableStateOf("") }
    var from by remember { mutableStateOf("") }
    var to by remember { mutableStateOf("") }
    var boardingCal by remember { mutableStateOf(Calendar.getInstance().apply { add(Calendar.HOUR, 2) }) }
    var departureCal by remember { mutableStateOf(Calendar.getInstance().apply { add(Calendar.HOUR, 3) }) }

    fun pickTime(initial: Calendar, onPicked: (Calendar) -> Unit) {
        TimePickerDialog(context, { _, h, m ->
            val cal = Calendar.getInstance()
            cal.timeInMillis = initial.timeInMillis
            cal.set(Calendar.HOUR_OF_DAY, h)
            cal.set(Calendar.MINUTE, m)
            onPicked(cal)
        }, initial.get(Calendar.HOUR_OF_DAY), initial.get(Calendar.MINUTE), true).show()
    }

    Scaffold { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(ScreenPadding), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.flight_new), style = MaterialTheme.typography.headlineMedium)
            OutlinedTextField(value = flightNumber, onValueChange = { flightNumber = it }, label = { Text(stringResource(R.string.flight_number_hint)) }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = airline, onValueChange = { airline = it }, label = { Text(stringResource(R.string.flight_airline_hint)) }, modifier = Modifier.fillMaxWidth())
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = from, onValueChange = { from = it }, label = { Text(stringResource(R.string.flight_from_hint)) }, modifier = Modifier.weight(1f))
                OutlinedTextField(value = to, onValueChange = { to = it }, label = { Text(stringResource(R.string.flight_to_hint)) }, modifier = Modifier.weight(1f))
            }
            val boardingLabel = String.format("%02d:%02d", boardingCal.get(Calendar.HOUR_OF_DAY), boardingCal.get(Calendar.MINUTE))
            val departureLabel = String.format("%02d:%02d", departureCal.get(Calendar.HOUR_OF_DAY), departureCal.get(Calendar.MINUTE))
            com.flowisland.android.core.ui.components.SecondaryButton(text = "${stringResource(R.string.flight_boarding_time_hint)}: $boardingLabel", onClick = { pickTime(boardingCal) { boardingCal = it } })
            com.flowisland.android.core.ui.components.SecondaryButton(text = "${stringResource(R.string.flight_departure_time_hint)}: $departureLabel", onClick = { pickTime(departureCal) { departureCal = it } })
            Text(stringResource(R.string.flight_manual_disclosure), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            PrimaryButton(
                text = stringResource(R.string.action_start),
                onClick = { viewModel.start(flightNumber, airline, from, to, boardingCal.timeInMillis, departureCal.timeInMillis, onStarted) },
            )
        }
    }
}
