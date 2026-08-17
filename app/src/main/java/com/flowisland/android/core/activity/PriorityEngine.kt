package com.flowisland.android.core.activity

import com.flowisland.android.core.activity.model.ActivityPriorityTier
import com.flowisland.android.core.activity.model.ActivityUiState
import android.os.SystemClock

/**
 * Pure sorting logic, deliberately separate from [ActivityEngine] so it can be
 * unit tested in isolation and reused by the collapsed island (top 2), the
 * notification set, and the Activity Switcher (full ranked list).
 */
object PriorityEngine {

    private val tierOrder = listOf(
        ActivityPriorityTier.URGENT,
        ActivityPriorityTier.NEARING_COMPLETION,
        ActivityPriorityTier.PINNED,
        ActivityPriorityTier.RECENTLY_INTERACTED,
        ActivityPriorityTier.BACKGROUND,
    )

    /**
     * Sort activities using the correct clock for each kind of time data.
     * Wall-clock time is used for absolute deadlines; elapsed realtime is used
     * for monotonic timers. Keeping the two domains separate prevents a wall-clock
     * epoch timestamp from being accidentally passed into TimerSpec.remainingMillis().
     */
    fun sort(
        activities: List<ActivityUiState>,
        nowWallClockMillis: Long = System.currentTimeMillis(),
        nowElapsedRealtimeMillis: Long = SystemClock.elapsedRealtime(),
    ): List<ActivityUiState> {
        return activities.sortedWith(
            compareBy<ActivityUiState> {
                tierOrder.indexOf(
                    it.priorityTier(
                        nowWallClockMillis = nowWallClockMillis,
                        nowElapsedRealtimeMillis = nowElapsedRealtimeMillis,
                    )
                )
            }.thenByDescending { it.lastInteractedAt }
        )
    }

    /** How many activities the collapsed multi-island row should ever show at once. */
    const val MAX_VISIBLE_COLLAPSED = 2
}
