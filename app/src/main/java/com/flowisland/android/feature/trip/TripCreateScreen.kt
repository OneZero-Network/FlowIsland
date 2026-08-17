package com.flowisland.android.feature.trip

import android.Manifest
import android.content.Intent
import android.util.Base64
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import com.flowisland.android.R
import com.flowisland.android.core.activity.ActivityActions
import com.flowisland.android.core.activity.ActivityEngine
import com.flowisland.android.core.activity.model.ActivityAction
import com.flowisland.android.core.activity.model.ActivityIconId
import com.flowisland.android.core.activity.model.ActivityId
import com.flowisland.android.core.activity.model.ActivityState
import com.flowisland.android.core.activity.model.ActivityType
import com.flowisland.android.core.activity.model.ActivityUiState
import com.flowisland.android.core.permissions.PermissionsManager
import com.flowisland.android.core.time.TimerSpec
import com.flowisland.android.core.ui.components.PrimaryButton
import com.flowisland.android.core.ui.components.ScreenPadding
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class TripCreateViewModel @Inject constructor(
    private val activityEngine: ActivityEngine,
    val permissionsManager: PermissionsManager,
    private val tracker: TripSessionTracker,
) : ViewModel() {

    fun start(destination: String, onStarted: (String) -> Unit) {
        val id = ActivityId.new()
        val state = ActivityUiState(
            id = id,
            type = ActivityType.TRIP,
            title = destination.ifBlank { "Trip" },
            subtitle = "0.0 km",
            icon = ActivityIconId.TRIP,
            state = ActivityState.ACTIVE,
            timer = TimerSpec(durationMillis = 0L, countUp = true),
            actions = listOf(
                ActivityAction("navigate", R.string.trip_open_navigation, ActivityAction.Kind.OPEN_NAVIGATION),
            ) + ActivityActions.simpleFinishCancel(),
            payloadId = "trip|${Base64.encodeToString(destination.ifBlank { "Trip" }.toByteArray(), Base64.NO_WRAP)}|0.0|${System.currentTimeMillis()}",
        )
        activityEngine.upsert(state)
        tracker.start(id.value)
        onStarted(id.value)
    }
}

@Composable
fun TripCreateScreen(onStarted: (String) -> Unit, onBack: () -> Unit) {
    val viewModel: TripCreateViewModel = hiltViewModel()
    val context = LocalContext.current
    var destination by remember { mutableStateOf("") }
    var showRationale by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results ->
        if (results.values.any { it }) viewModel.start(destination, onStarted)
    }

    Scaffold { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(ScreenPadding), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(stringResource(R.string.trip_new), style = MaterialTheme.typography.headlineMedium)
            OutlinedTextField(
                value = destination,
                onValueChange = { destination = it },
                label = { Text(stringResource(R.string.trip_destination_hint)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            if (showRationale) {
                Text(stringResource(R.string.trip_location_rationale_body), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            PrimaryButton(
                text = stringResource(R.string.action_start),
                onClick = {
                    if (viewModel.permissionsManager.hasAnyLocationPermission()) {
                        viewModel.start(destination, onStarted)
                    } else {
                        showRationale = true
                        permissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
                    }
                },
            )
        }
    }
}

/** Launches whatever navigation app is installed via a normal, standard geo: intent -- never a bespoke routing implementation. */
fun openNavigationIntent(context: android.content.Context, destinationLabel: String?) {
    val uri = if (destinationLabel.isNullOrBlank()) Uri.parse("geo:0,0") else Uri.parse("geo:0,0?q=${Uri.encode(destinationLabel)}")
    val intent = Intent(Intent.ACTION_VIEW, uri)
    if (intent.resolveActivity(context.packageManager) != null) context.startActivity(intent)
}
