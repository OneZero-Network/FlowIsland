package com.flowisland.android.core.activity

import com.flowisland.android.core.activity.model.ActivityPriorityTier
import com.flowisland.android.core.activity.model.ActivityUiState

/**
 * Pure sorting logic, deliberately separate from [ActivityEngine] so it can be
 * unit tested in isolation and reused by the collapsed island (top 2), the
 * notification set, and the Activity Switcher (full ranked list).
 */
object PriorityEngine {

    private val tierOrder = listOf(
        ActivityPriorityTier.URGENT,
        ActivityPriorityTier.PINNED,
        ActivityPriorityTier.NEARING_COMPLETION,
        ActivityPriorityTier.RECENTLY_INTERACTED,
        ActivityPriorityTier.BACKGROUND,
    )

    fun sort(activities: List<ActivityUiState>, now: Long = System.currentTimeMillis()): List<ActivityUiState> {
        return activities.sortedWith(
            compareBy<ActivityUiState> { tierOrder.indexOf(it.priorityTier(now)) }
                .thenByDescending { it.lastInteractedAt }
        )
    }

    /** How many activities the collapsed multi-island row should ever show at once. */
    const val MAX_VISIBLE_COLLAPSED = 2
}
