package com.flowisland.android.core.time

import android.os.SystemClock
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Timestamp-based timer state. Deliberately does NOT hold a running "seconds left"
 * counter -- everything is derived on demand from [SystemClock.elapsedRealtime],
 * which keeps advancing through screen-off, doze and app-background (unlike
 * uptimeMillis) and is immune to the user manually changing the wall clock or a
 * timezone/DST shift (unlike System.currentTimeMillis()).
 *
 * [wallClockTargetMillis] is derived separately, only when needed to seed a
 * Notification's chronometer (which is wall-clock based) -- see [wallClockTargetOrStartMillis].
 */
data class TimerSpec(
    val durationMillis: Long,
    val startedAtElapsedRealtime: Long = SystemClock.elapsedRealtime(),
    val accumulatedPausedMillis: Long = 0L,
    val pausedAtElapsedRealtime: Long? = null,
    val countUp: Boolean = false,
) {
    val isPaused: Boolean get() = pausedAtElapsedRealtime != null

    /** Total elapsed "live" milliseconds since start, excluding paused time. */
    fun elapsedMillis(nowElapsedRealtime: Long = SystemClock.elapsedRealtime()): Long {
        val effectiveNow = pausedAtElapsedRealtime ?: nowElapsedRealtime
        return (effectiveNow - startedAtElapsedRealtime - accumulatedPausedMillis).coerceAtLeast(0L)
    }

    /** Remaining milliseconds for a countdown; always 0 for a count-up (stopwatch) spec. */
    fun remainingMillis(nowElapsedRealtime: Long = SystemClock.elapsedRealtime()): Long {
        if (countUp) return 0L
        return (durationMillis - elapsedMillis(nowElapsedRealtime)).coerceAtLeast(0L)
    }

    fun isExpired(nowElapsedRealtime: Long = SystemClock.elapsedRealtime()): Boolean =
        !countUp && remainingMillis(nowElapsedRealtime) <= 0L

    fun progress(nowElapsedRealtime: Long = SystemClock.elapsedRealtime()): Float {
        if (durationMillis <= 0L) return 0f
        return (elapsedMillis(nowElapsedRealtime).toFloat() / durationMillis.toFloat()).coerceIn(0f, 1f)
    }

    fun pause(nowElapsedRealtime: Long = SystemClock.elapsedRealtime()): TimerSpec =
        if (isPaused) this else copy(pausedAtElapsedRealtime = nowElapsedRealtime)

    fun resume(nowElapsedRealtime: Long = SystemClock.elapsedRealtime()): TimerSpec {
        val pausedAt = pausedAtElapsedRealtime ?: return this
        val extraPause = nowElapsedRealtime - pausedAt
        return copy(accumulatedPausedMillis = accumulatedPausedMillis + extraPause, pausedAtElapsedRealtime = null)
    }

    fun addDuration(deltaMillis: Long): TimerSpec = copy(durationMillis = (durationMillis + deltaMillis).coerceAtLeast(0L))

    /**
     * Wall-clock epoch millis to feed a Notification's setWhen()/setUsesChronometer(),
     * recomputed fresh from the current elapsed-realtime-based truth every time a
     * notification is (re)built, so it self-corrects even if the notification sat
     * unrefreshed for a while.
     */
    fun wallClockTargetOrStartMillis(nowWallClock: Long = System.currentTimeMillis(), nowElapsedRealtime: Long = SystemClock.elapsedRealtime()): Long {
        return if (countUp) {
            nowWallClock - elapsedMillis(nowElapsedRealtime)
        } else {
            nowWallClock + remainingMillis(nowElapsedRealtime)
        }
    }
}

object TimeFormat {

    /** mm:ss, or hh:mm:ss once an hour is crossed. */
    fun countdown(millis: Long): String {
        val totalSeconds = TimeUnit.MILLISECONDS.toSeconds(millis.coerceAtLeast(0L))
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return if (hours > 0) {
            String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format(Locale.US, "%02d:%02d", minutes, seconds)
        }
    }

    /** Stopwatch-style with centiseconds, e.g. 18:42.31 */
    fun stopwatch(millis: Long): String {
        val totalCentis = millis / 10
        val minutes = totalCentis / 6000
        val seconds = (totalCentis / 100) % 60
        val centis = totalCentis % 100
        return String.format(Locale.US, "%02d:%02d.%02d", minutes, seconds, centis)
    }

    /** "in 24 minutes" style relative label for reminders/flights. */
    fun relativeMinutes(millis: Long): Long = TimeUnit.MILLISECONDS.toMinutes(millis.coerceAtLeast(0L))
}
