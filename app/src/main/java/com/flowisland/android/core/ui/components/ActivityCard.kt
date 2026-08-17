package com.flowisland.android.core.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.flowisland.android.R
import com.flowisland.android.core.activity.model.ActivityUiState
import com.flowisland.android.core.time.TimeFormat

@Composable
fun ActivityCard(
    state: ActivityUiState,
    onOpen: () -> Unit,
    onPin: () -> Unit,
    onUnpin: () -> Unit,
    onHide: () -> Unit,
    onStop: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var menuOpen by remember { mutableStateOf(false) }

    Surface(
        onClick = onOpen,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(state.icon.toImageVector(), contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                    Text(state.title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    val subtitle = state.timer?.let {
                        if (it.countUp) TimeFormat.stopwatch(it.elapsedMillis()) else TimeFormat.countdown(it.remainingMillis())
                    } ?: state.subtitle
                    subtitle?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                }
                if (state.pinned) {
                    Icon(Icons.Filled.PushPin, contentDescription = stringResource(R.string.action_pin), tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                }
                IconButton(onClick = { menuOpen = true }) {
                    Icon(Icons.Filled.MoreVert, contentDescription = null)
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(text = { Text(stringResource(if (state.pinned) R.string.action_unpin else R.string.action_pin)) }, onClick = { menuOpen = false; if (state.pinned) onUnpin() else onPin() })
                    DropdownMenuItem(text = { Text(stringResource(R.string.action_hide)) }, onClick = { menuOpen = false; onHide() })
                    DropdownMenuItem(text = { Text(stringResource(R.string.action_open)) }, onClick = { menuOpen = false; onOpen() })
                    DropdownMenuItem(text = { Text(stringResource(R.string.action_stop)) }, onClick = { menuOpen = false; onStop() })
                }
            }

            val progress = state.timer?.progress() ?: state.explicitProgress
            if (progress != null) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                    strokeCap = androidx.compose.ui.graphics.StrokeCap.Round,
                )
            }
        }
    }
}
