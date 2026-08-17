package com.flowisland.android.core.time

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TimerSpecTest {

    @Test
    fun `remaining time counts down correctly from elapsed realtime`() {
        val spec = TimerSpec(durationMillis = 60_000L, startedAtElapsedRealtime = 0L)
        assertEquals(60_000L, spec.remainingMillis(nowElapsedRealtime = 0L))
        assertEquals(30_000L, spec.remainingMillis(nowElapsedRealtime = 30_000L))
        assertEquals(0L, spec.remainingMillis(nowElapsedRealtime = 60_000L))
        assertEquals(0L, spec.remainingMillis(nowElapsedRealtime = 90_000L)) // never negative
    }

    @Test
    fun `pause freezes remaining time regardless of further elapsed realtime`() {
        val spec = TimerSpec(durationMillis = 60_000L, startedAtElapsedRealtime = 0L)
            .let { it.pause(nowElapsedRealtime = 20_000L) }

        // Time "passes" (screen off, backgrounded) but paused timer must not move.
        assertEquals(40_000L, spec.remainingMillis(nowElapsedRealtime = 20_000L))
        assertEquals(40_000L, spec.remainingMillis(nowElapsedRealtime = 500_000L))
    }

    @Test
    fun `resume after pause correctly excludes the paused duration`() {
        val spec = TimerSpec(durationMillis = 60_000L, startedAtElapsedRealtime = 0L)
            .pause(nowElapsedRealtime = 20_000L)
            .resume(nowElapsedRealtime = 100_000L) // paused for 80s

        // 20s elapsed before pause; resumed at 100s realtime.
        assertEquals(40_000L, spec.remainingMillis(nowElapsedRealtime = 100_000L))
        assertEquals(30_000L, spec.remainingMillis(nowElapsedRealtime = 110_000L))
    }

    @Test
    fun `survives a simulated screen-off gap identically to continuous foreground time`() {
        // Screen off for 5 minutes is just a jump in elapsedRealtime -- no special handling needed.
        val spec = TimerSpec(durationMillis = 600_000L, startedAtElapsedRealtime = 0L)
        assertEquals(300_000L, spec.remainingMillis(nowElapsedRealtime = 300_000L))
    }

    @Test
    fun `stopwatch counts up and never expires`() {
        val spec = TimerSpec(durationMillis = 0L, startedAtElapsedRealtime = 0L, countUp = true)
        assertEquals(45_000L, spec.elapsedMillis(nowElapsedRealtime = 45_000L))
        assertTrue(!spec.isExpired(nowElapsedRealtime = 999_999L))
    }

    @Test
    fun `addDuration extends a countdown without disturbing elapsed time`() {
        val spec = TimerSpec(durationMillis = 60_000L, startedAtElapsedRealtime = 0L).addDuration(60_000L)
        assertEquals(120_000L, spec.remainingMillis(nowElapsedRealtime = 0L))
    }


    @Test
    fun `countdown recovers across a simulated device reboot using wall clock anchor`() {
        val spec = TimerSpec(
            durationMillis = 60_000L,
            startedAtElapsedRealtime = 900_000L,
            startedAtWallClockMillis = 1_000_000L,
        )

        // Before reboot: 20 seconds elapsed. After reboot, elapsedRealtime is
        // smaller than the old anchor, so the implementation must switch to
        // the persisted wall-clock domain.
        assertEquals(40_000L, spec.remainingMillis(920_000L, 1_020_000L))
        assertEquals(10_000L, spec.remainingMillis(20_000L, 1_050_000L))
    }

    @Test
    fun `paused countdown remains paused across a simulated reboot`() {
        val spec = TimerSpec(
            durationMillis = 60_000L,
            startedAtElapsedRealtime = 900_000L,
            startedAtWallClockMillis = 1_000_000L,
        ).pause(920_000L, 1_020_000L)

        assertEquals(40_000L, spec.remainingMillis(20_000L, 1_500_000L))
    }

    @Test
    fun `resume after reboot preserves pre-pause elapsed time`() {
        val spec = TimerSpec(
            durationMillis = 60_000L,
            startedAtElapsedRealtime = 900_000L,
            startedAtWallClockMillis = 1_000_000L,
        ).pause(920_000L, 1_020_000L)
            .resume(30_000L, 1_120_000L)

        assertEquals(30_000L, spec.remainingMillis(40_000L, 1_130_000L))
    }

    @Test
    fun `isExpired is true exactly at and after the deadline`() {
        val spec = TimerSpec(durationMillis = 10_000L, startedAtElapsedRealtime = 0L)
        assertTrue(!spec.isExpired(nowElapsedRealtime = 9_999L))
        assertTrue(spec.isExpired(nowElapsedRealtime = 10_000L))
        assertTrue(spec.isExpired(nowElapsedRealtime = 20_000L))
    }
}
