package com.flowisland.android.feature.pomodoro

import com.flowisland.android.core.activity.ActivityActions
import com.flowisland.android.core.activity.ActivityEngine
import com.flowisland.android.core.activity.model.ActivityIconId
import com.flowisland.android.core.activity.model.ActivityState
import com.flowisland.android.core.activity.model.ActivityType
import com.flowisland.android.core.activity.model.ActivityUiState
import com.flowisland.android.core.di.ApplicationScope
import com.flowisland.android.core.time.TimerSpec
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PomodoroCycleManager @Inject constructor(
    private val activityEngine: ActivityEngine,
    @ApplicationScope private val scope: CoroutineScope,
) {
    private val advancedThisExpiry = mutableSetOf<String>()

    fun start() {
        scope.launch {
            activityEngine.activities.collect { list ->
                list.filter { it.type == ActivityType.POMODORO && it.state == ActivityState.ACTIVE }
                    .forEach { pomodoro -> maybeAdvance(pomodoro) }
            }
        }
    }

    private fun maybeAdvance(state: ActivityUiState) {
        val timer = state.timer ?: return
        if (!timer.isExpired()) {
            advancedThisExpiry.remove(state.id.value)
            return
        }
        if (state.id.value in advancedThisExpiry) return
        advancedThisExpiry += state.id.value

        val parts = state.payloadId?.split(":") ?: return
        if (parts.size != 5) return
        val focusMinutes = parts[0].toIntOrNull() ?: return
        val breakMinutes = parts[1].toIntOrNull() ?: return
        val autoStartBreak = parts[2] == "1"
        val autoStartFocus = parts[3] == "1"
        val currentPhase = parts[4]

        val goingToBreak = currentPhase == "focus"
        val autoStart = if (goingToBreak) autoStartBreak else autoStartFocus
        val nextPhaseLabel = if (goingToBreak) "break" else "focus"
        val nextDurationMillis = (if (goingToBreak) breakMinutes else focusMinutes) * 60_000L
        val newPayload = "$focusMinutes:$breakMinutes:${parts[2]}:${parts[3]}:$nextPhaseLabel"

        activityEngine.update(state.id) {
            it.copy(
                title = if (goingToBreak) "Break" else "Focus",
                icon = if (goingToBreak) ActivityIconId.POMODORO_BREAK else ActivityIconId.POMODORO_FOCUS,
                state = if (autoStart) ActivityState.ACTIVE else ActivityState.PAUSED,
                timer = TimerSpec(durationMillis = nextDurationMillis).let { spec -> if (autoStart) spec else spec.pause() },
                actions = if (autoStart) ActivityActions.countdownRunning(includeAddMinute = false) else ActivityActions.countdownPaused(),
                payloadId = newPayload,
            )
        }
    }
}
