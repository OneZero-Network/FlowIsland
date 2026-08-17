package com.flowisland.android.core.activity.model

import android.os.SystemClock
import com.flowisland.android.core.time.TimerSpec
import java.util.UUID

@JvmInline
value class ActivityId(val value: String) {
    companion object {
        fun new(): ActivityId = ActivityId(UUID.randomUUID().toString())
    }
}

enum class ActivityType {
    TIMER, POMODORO, STOPWATCH, MEDIA, STUDY, COOKING, FITNESS, TRIP,
    EXPENSE, DOWNLOAD, AI_TASK, REMINDER, DELIVERY, FLIGHT,
}

enum class ActivityState {
    CREATED, ACTIVE, PAUSED, COMPLETED, FAILED, CANCELLED, EXPIRED;

    val isTerminal: Boolean get() = this == COMPLETED || this == FAILED || this == CANCELLED || this == EXPIRED
    val isOngoing: Boolean get() = this == ACTIVE || this == PAUSED
}

/** UI-agnostic icon identifier -- mapped to an ImageVector only at the presentation layer. */
enum class ActivityIconId {
    TIMER, POMODORO_FOCUS, POMODORO_BREAK, STOPWATCH, MEDIA, STUDY, COOKING,
    FITNESS_WALK, FITNESS_RUN, FITNESS_CYCLE, TRIP, EXPENSE, DOWNLOAD, AI_TASK,
    REMINDER, DELIVERY, FLIGHT, CHECK,
}

enum class ActivityPriorityTier {
    URGENT, PINNED, NEARING_COMPLETION, RECENTLY_INTERACTED, BACKGROUND,
}

/** A single user-facing action surfaced on the expanded island / notification. */
data class ActivityAction(
    val id: String,
    val labelResId: Int,
    val kind: Kind,
) {
    enum class Kind { PAUSE, RESUME, FINISH, CANCEL, ADD_1_MIN, ADD_5_MIN, LAP, SNOOZE, DONE, CUSTOM, VIEW_RESULT, OPEN_NAVIGATION }
}

/**
 * The single immutable snapshot the entire app is built around. The exact same
 * instance drives the home screen, the Activity Switcher, the Android notification,
 * the Android 16 Live Update, and the optional floating overlay -- never a
 * separately-derived copy.
 */
data class ActivityUiState(
    val id: ActivityId,
    val type: ActivityType,
    val title: String,
    val subtitle: String? = null,
    val icon: ActivityIconId,
    val state: ActivityState,
    val timer: TimerSpec? = null,
    /** 0f..1f, independent of [timer] for activities whose progress isn't time-based (download, AI task). */
    val explicitProgress: Float? = null,
    val actions: List<ActivityAction> = emptyList(),
    val pinned: Boolean = false,
    val hidden: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val lastInteractedAt: Long = System.currentTimeMillis(),
    /** Free-form payload key the owning feature can use to look up its own DB row. */
    val payloadId: String? = null,
    /** Optional wall-clock deadline for non-timer activities (flight boarding, reminder). */
    val expirationTime: Long? = null,
) {
    fun priorityTier(
        nowWallClockMillis: Long = System.currentTimeMillis(),
        nowElapsedRealtimeMillis: Long = SystemClock.elapsedRealtime(),
    ): ActivityPriorityTier = when {
        expirationTime != null && expirationTime <= nowWallClockMillis && state.isOngoing -> ActivityPriorityTier.URGENT
        state == ActivityState.ACTIVE && timer != null && !timer.countUp &&
            timer.remainingMillis(nowElapsedRealtimeMillis) in 0..30_000 -> ActivityPriorityTier.NEARING_COMPLETION
        pinned -> ActivityPriorityTier.PINNED
        nowWallClockMillis - lastInteractedAt < 15_000 -> ActivityPriorityTier.RECENTLY_INTERACTED
        else -> ActivityPriorityTier.BACKGROUND
    }
}
