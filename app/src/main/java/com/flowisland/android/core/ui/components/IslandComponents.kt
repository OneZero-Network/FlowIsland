package com.flowisland.android.core.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flowisland.android.core.activity.model.ActivityAction
import com.flowisland.android.core.activity.model.ActivityUiState

/**
 * The default collapsed state: a small rounded pill, icon + compact time/status.
 * Occupies as little space as practical; never covers more than a corner of the
 * screen. Used by both the in-app preview and the real floating overlay window.
 */
@Composable
fun CollapsedIslandPill(state: ActivityUiState, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp,
        shadowElevation = 4.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
        ) {
            Icon(state.icon.toImageVector(), contentDescription = stringResource(com.flowisland.android.R.string.cd_activity_icon), tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
            val compactText = state.timer?.let {
                if (it.countUp) com.flowisland.android.core.time.TimeFormat.stopwatch(it.elapsedMillis())
                else com.flowisland.android.core.time.TimeFormat.countdown(it.remainingMillis())
            } ?: state.subtitle ?: state.title
            Text(compactText, style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

/** A minimal side-by-side row of up to [com.flowisland.android.core.activity.PriorityEngine.MAX_VISIBLE_COLLAPSED] pills, for when several activities are active at once. */
@Composable
fun CollapsedIslandRow(states: List<ActivityUiState>, onClick: (ActivityUiState) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        states.take(2).forEach { state ->
            CollapsedIslandPill(state = state, onClick = { onClick(state) })
        }
    }
}

@Composable
fun ExpandedIslandCard(
    state: ActivityUiState,
    onAction: (ActivityAction) -> Unit,
    onCollapse: () -> Unit,
    modifier: Modifier = Modifier,
    extraContent: (@Composable () -> Unit)? = null,
) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 4.dp,
        shadowElevation = 8.dp,
        modifier = modifier.fillMaxWidth().animateContentSize(),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(state.icon.toImageVector(), contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                Text(state.title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                TextButton(onClick = onCollapse) { Text(stringResource(com.flowisland.android.R.string.cd_collapse_island), fontSize = 12.sp) }
            }
            state.subtitle?.let {
                Spacer(Modifier.height(2.dp))
                Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Spacer(Modifier.height(16.dp))

            state.timer?.let { timer ->
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    TimerText(timer = timer)
                }
                Spacer(Modifier.height(12.dp))
                LinearProgressIndicator(
                    progress = { timer.progress() },
                    modifier = Modifier.fillMaxWidth().height(6.dp),
                    strokeCap = androidx.compose.ui.graphics.StrokeCap.Round,
                )
                Spacer(Modifier.height(16.dp))
            } ?: state.explicitProgress?.let { progress ->
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth().height(6.dp),
                    strokeCap = androidx.compose.ui.graphics.StrokeCap.Round,
                )
                Spacer(Modifier.height(16.dp))
            }

            extraContent?.invoke()

            if (state.actions.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    state.actions.forEach { action ->
                        IslandActionButton(action = action, modifier = Modifier.weight(1f), onClick = { onAction(action) })
                    }
                }
            }
        }
    }
}

@Composable
private fun IslandActionButton(action: ActivityAction, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val isPrimary = action.kind == ActivityAction.Kind.FINISH || action.kind == ActivityAction.Kind.DONE
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        color = if (isPrimary) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
        modifier = modifier,
    ) {
        Text(
            text = stringResource(action.labelResId),
            style = MaterialTheme.typography.labelLarge,
            color = if (isPrimary) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        )
    }
}
