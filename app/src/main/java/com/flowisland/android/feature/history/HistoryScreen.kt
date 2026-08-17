package com.flowisland.android.feature.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import com.flowisland.android.R
import com.flowisland.android.core.database.ActivityHistoryDao
import com.flowisland.android.core.database.ActivityHistoryEntity
import com.flowisland.android.core.time.TimeFormat
import com.flowisland.android.core.ui.components.EmptyState
import dagger.hilt.android.lifecycle.HiltViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(activityHistoryDao: ActivityHistoryDao) : ViewModel() {
    val recent = activityHistoryDao.observeRecent()
}

@Composable
fun HistoryScreen(onBack: () -> Unit) {
    val viewModel: HistoryViewModel = hiltViewModel()
    val entries by viewModel.recent.collectAsState(initial = emptyList())

    Scaffold(topBar = { TopAppBar(title = { Text(stringResource(R.string.home_history)) }) }) { padding ->
        if (entries.isEmpty()) {
            EmptyState(
                title = stringResource(R.string.home_nothing_active_title),
                body = stringResource(R.string.home_nothing_active_body),
                modifier = Modifier.padding(padding),
            )
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp, vertical = 12.dp)) {
                items(entries, key = { it.id }) { entry -> HistoryRow(entry) }
            }
        }
    }
}

@Composable
private fun HistoryRow(entry: ActivityHistoryEntity) {
    val dateFormat = remember(entry.endedAt) { SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()) }
    Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surface, tonalElevation = 1.dp, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text(entry.title, style = MaterialTheme.typography.titleMedium)
                Text(dateFormat.format(Date(entry.endedAt)), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(
                if (entry.completed) "✓ ${TimeFormat.countdown(entry.durationMillis)}" else "✕",
                style = MaterialTheme.typography.bodyMedium,
                color = if (entry.completed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
            )
        }
    }
}
