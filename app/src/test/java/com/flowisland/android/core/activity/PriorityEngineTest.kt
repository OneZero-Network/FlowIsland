package com.flowisland.android.core.activity

import com.flowisland.android.core.activity.model.ActivityIconId
import com.flowisland.android.core.activity.model.ActivityId
import com.flowisland.android.core.activity.model.ActivityState
import com.flowisland.android.core.activity.model.ActivityType
import com.flowisland.android.core.activity.model.ActivityUiState
import com.flowisland.android.core.time.TimerSpec
import org.junit.Assert.assertEquals
import org.junit.Test

class PriorityEngineTest {

    private fun activity(
        id: String,
        pinned: Boolean = false,
        lastInteractedAt: Long = 0L,
        remainingMillis: Long? = null,
        expirationTime: Long? = null,
    ) = ActivityUiState(
        id = ActivityId(id),
        type = ActivityType.TIMER,
        title = id,
        icon = ActivityIconId.TIMER,
        state = ActivityState.ACTIVE,
        timer = remainingMillis?.let { TimerSpec(durationMillis = it, startedAtElapsedRealtime = 0L) },
        pinned = pinned,
        lastInteractedAt = lastInteractedAt,
        expirationTime = expirationTime,
    )

    @Test
    fun `pinned activity outranks an untouched background activity`() {
        val pinned = activity("pinned", pinned = true, lastInteractedAt = 0L)
        val background = activity("background", lastInteractedAt = 0L)

        val sorted = PriorityEngine.sort(
            listOf(background, pinned),
            nowWallClockMillis = 100_000L,
            nowElapsedRealtimeMillis = 100_000L,
        )

        assertEquals("pinned", sorted.first().id.value)
    }

    @Test
    fun `activity nearing completion outranks a pinned activity`() {
        val pinned = activity("pinned", pinned = true, lastInteractedAt = 0L)
        val nearingCompletion = activity("nearly-done", remainingMillis = 5_000L, lastInteractedAt = 0L)

        val sorted = PriorityEngine.sort(
            listOf(pinned, nearingCompletion),
            nowWallClockMillis = 100_000L,
            nowElapsedRealtimeMillis = 0L,
        )

        assertEquals("nearly-done", sorted.first().id.value)
    }

    @Test
    fun `urgent expired-deadline activity outranks everything`() {
        val pinned = activity("pinned", pinned = true, lastInteractedAt = 0L)
        val urgent = activity("urgent", expirationTime = 50L)

        val sorted = PriorityEngine.sort(
            listOf(pinned, urgent),
            nowWallClockMillis = 100L,
            nowElapsedRealtimeMillis = 100L,
        )

        assertEquals("urgent", sorted.first().id.value)
    }

    @Test
    fun `recently interacted outranks stale background within the same tier`() {
        val stale = activity("stale", lastInteractedAt = 0L)
        val recent = activity("recent", lastInteractedAt = 99_000L)

        val sorted = PriorityEngine.sort(
            listOf(stale, recent),
            nowWallClockMillis = 100_000L,
            nowElapsedRealtimeMillis = 100_000L,
        )

        assertEquals("recent", sorted.first().id.value)
    }
}
