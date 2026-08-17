package com.flowisland.android.feature.fitness

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.flowisland.android.core.permissions.PermissionsManager
import com.flowisland.android.core.time.TimerSpec
import com.flowisland.android.core.ui.components.PrimaryButton
import com.flowisland.android.core.ui.components.ScreenPadding
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

enum class FitnessKind(val icon: ActivityIconId, val labelResId: Int) {
    WALK(ActivityIconId.FITNESS_WALK, R.string.fitness_walking),
    RUN(ActivityIconId.FITNESS_RUN, R.string.fitness_running),
    CYCLE(ActivityIconId.FITNESS_CYCLE, R.string.fitness_cycling),
    CUSTOM(ActivityIconId.FITNESS_RUN, R.string.fitness_custom),
}

@HiltViewModel
class FitnessCreateViewModel @Inject constructor(
    private val activityEngine: ActivityEngine,
    val permissionsManager: PermissionsManager,
    private val tracker: FitnessSessionTracker,
) : ViewModel() {

    fun start(kind: FitnessKind, onStarted: (String) -> Unit) {
        val id = ActivityId.new()
        val label = "Workout"
        val state = ActivityUiState(
            id = id,
            type = ActivityType.FITNESS,
            title = label,
            subtitle = "0.00 km",
            icon = kind.icon,
            state = ActivityState.ACTIVE,
            timer = TimerSpec(durationMillis = 0L, countUp = true),
            actions = ActivityActions.simpleFinishCancel(),
            payloadId = "fitness|${kind.name}|0.0|${System.currentTimeMillis()}",
        )
        activityEngine.upsert(state)
        tracker.start(id.value, kind.name, label)
        onStarted(id.value)
    }
}

@Composable
fun FitnessCreateScreen(onStarted: (String) -> Unit, onBack: () -> Unit) {
    val viewModel: FitnessCreateViewModel = hiltViewModel()
    var kind by remember { mutableStateOf(FitnessKind.RUN) }
    var showRationale by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results ->
        if (results.values.any { it }) viewModel.start(kind, onStarted)
    }

    Scaffold { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(ScreenPadding), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(stringResource(R.string.fitness_new_workout), style = MaterialTheme.typography.headlineMedium)

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FitnessKind.entries.forEach { candidate ->
                    FilterChip(selected = kind == candidate, onClick = { kind = candidate }, label = { Text(stringResource(candidate.labelResId)) })
                }
            }

            if (showRationale) {
                Text(stringResource(R.string.fitness_location_rationale_body), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            PrimaryButton(
                text = stringResource(R.string.action_start),
                onClick = {
                    if (viewModel.permissionsManager.hasAnyLocationPermission()) {
                        viewModel.start(kind, onStarted)
                    } else {
                        showRationale = true
                        permissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
                    }
                },
            )
        }
    }
}
