package com.flowisland.android.core.ui.components

import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import com.flowisland.android.core.time.TimeFormat
import com.flowisland.android.core.time.TimerSpec
import kotlinx.coroutines.delay

/**
 * Recomputes remaining/elapsed time from [TimerSpec] once a second while this
 * composable is actually on screen -- NOT a background poll. When the composable
 * leaves composition (screen off, navigated away), the LaunchedEffect is
 * cancelled automatically and nothing keeps ticking. The Android notification
 * itself never depends on this -- it uses the system chronometer, which needs no
 * app-side ticking at all.
 */
@Composable
fun TimerText(
    timer: TimerSpec,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.displayLarge,
) {
    var now by remember(timer) { mutableLongStateOf(android.os.SystemClock.elapsedRealtime()) }

    LaunchedEffect(timer, timer.isPaused) {
        if (timer.isPaused) return@LaunchedEffect
        while (true) {
            now = android.os.SystemClock.elapsedRealtime()
            delay(if (timer.countUp) 33L else 1000L) // stopwatch shows centiseconds -> refresh faster
        }
    }

    val text = if (timer.countUp) TimeFormat.stopwatch(timer.elapsedMillis(now)) else TimeFormat.countdown(timer.remainingMillis(now))

    Text(
        text = text,
        style = style.copy(fontFamily = com.flowisland.android.core.ui.theme.TimerNumeralStyle.fontFamily, fontFeatureSettings = "tnum"),
        color = LocalContentColor.current,
        modifier = modifier,
    )
}
