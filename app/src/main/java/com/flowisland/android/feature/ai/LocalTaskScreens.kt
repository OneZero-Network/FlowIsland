package com.flowisland.android.feature.ai

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
import androidx.lifecycle.viewModelScope
import com.flowisland.android.R
import com.flowisland.android.core.activity.ActivityActions
import com.flowisland.android.core.activity.ActivityEngine
import com.flowisland.android.core.activity.model.ActivityIconId
import com.flowisland.android.core.activity.model.ActivityId
import com.flowisland.android.core.activity.model.ActivityState
import com.flowisland.android.core.activity.model.ActivityType
import com.flowisland.android.core.activity.model.ActivityUiState
import com.flowisland.android.core.database.AppDatabase
import com.flowisland.android.core.di.IoDispatcher
import com.flowisland.android.core.ui.components.PrimaryButton
import com.flowisland.android.core.ui.components.ScreenPadding
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Generic interface any future local (or later, remote) processing task can
 * drive by calling [update] with real values -- this is the seam the spec asks
 * V1 to leave in place without requiring an actual AI backend to exist yet.
 */
interface LocalTaskUpdater {
    fun update(taskId: ActivityId, step: String, progress: Float)
    fun complete(taskId: ActivityId)
}

@HiltViewModel
class LocalTaskViewModel @Inject constructor(
    private val activityEngine: ActivityEngine,
    private val database: AppDatabase,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : ViewModel(), LocalTaskUpdater {

    override fun update(taskId: ActivityId, step: String, progress: Float) {
        activityEngine.update(taskId) { it.copy(subtitle = step, explicitProgress = progress) }
    }

    override fun complete(taskId: ActivityId) = activityEngine.complete(taskId)

    /** Real, genuinely local work: VACUUM (compact) then REINDEX the on-device
     * database. Two real, discrete operations -- progress reflects actual
     * completed steps, not a simulated delay. */
    fun optimizeDatabase(onStarted: (String) -> Unit) {
        val id = ActivityId.new()
        val state = ActivityUiState(
            id = id,
            type = ActivityType.AI_TASK,
            title = "Local Task",
            subtitle = "Compacting storage",
            icon = ActivityIconId.AI_TASK,
            state = ActivityState.ACTIVE,
            explicitProgress = 0f,
            actions = ActivityActions.cancelOnly(),
        )
        activityEngine.upsert(state)
        onStarted(id.value)

        viewModelScope.launch {
            withContext(ioDispatcher) {
                update(id, "Compacting storage", 0.1f)
                database.openHelper.writableDatabase.execSQL("VACUUM")
                update(id, "Rebuilding indexes", 0.6f)
                database.openHelper.writableDatabase.execSQL("REINDEX")
                update(id, "Rebuilding indexes", 1f)
            }
            complete(id)
        }
    }
}

@Composable
fun LocalTasksScreen(onStarted: (String) -> Unit, onBack: () -> Unit) {
    val viewModel: LocalTaskViewModel = hiltViewModel()
    Scaffold { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(ScreenPadding), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(stringResource(R.string.type_ai), style = MaterialTheme.typography.headlineMedium)
            PrimaryButton(text = stringResource(R.string.ai_optimize_database), onClick = { viewModel.optimizeDatabase(onStarted) })
        }
    }
}
