package com.flowisland.android.feature.download

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
import com.flowisland.android.core.database.ActivityHistoryDao
import com.flowisland.android.core.di.IoDispatcher
import com.flowisland.android.core.ui.components.PrimaryButton
import com.flowisland.android.core.ui.components.ScreenPadding
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileWriter
import javax.inject.Inject

@HiltViewModel
class ExportViewModel @Inject constructor(
    @ApplicationContext private val context: android.content.Context,
    private val activityEngine: ActivityEngine,
    private val activityHistoryDao: ActivityHistoryDao,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : ViewModel() {

    fun exportHistory(onStarted: (String) -> Unit) {
        val id = ActivityId.new()
        val state = ActivityUiState(
            id = id,
            type = ActivityType.DOWNLOAD,
            title = "export_history.csv",
            icon = ActivityIconId.DOWNLOAD,
            state = ActivityState.ACTIVE,
            explicitProgress = 0f,
            actions = ActivityActions.cancelOnly(),
        )
        activityEngine.upsert(state)
        onStarted(id.value)

        viewModelScope.launch {
            withContext(ioDispatcher) {
                // Point-in-time snapshot -- an export operates on the data as it
                // exists right now, not a live subscription.
                val snapshot = activityHistoryDao.observeRecent().first()
                val file = File(context.getExternalFilesDir(null), "flowisland_export_${System.currentTimeMillis()}.csv")
                FileWriter(file).use { writer ->
                    writer.appendLine("type,title,started_at,ended_at,duration_ms,completed")
                    if (snapshot.isEmpty()) {
                        activityEngine.setProgress(id, 1f)
                    } else {
                        snapshot.forEachIndexed { index, entry ->
                            writer.appendLine("${entry.type},\"${entry.title.replace("\"", "'")}\",${entry.startedAt},${entry.endedAt},${entry.durationMillis},${entry.completed}")
                            activityEngine.setProgress(id, (index + 1f) / snapshot.size)
                        }
                    }
                }
            }
            activityEngine.complete(id)
        }
    }
}

@Composable
fun LocalExportsScreen(onStarted: (String) -> Unit, onBack: () -> Unit) {
    val viewModel: ExportViewModel = hiltViewModel()
    Scaffold { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(ScreenPadding), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(stringResource(R.string.type_download), style = MaterialTheme.typography.headlineMedium)
            PrimaryButton(text = stringResource(R.string.download_export_history), onClick = { viewModel.exportHistory(onStarted) })
        }
    }
}
