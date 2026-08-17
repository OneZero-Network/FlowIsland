package com.flowisland.android.core.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Watch
import androidx.compose.ui.graphics.vector.ImageVector
import com.flowisland.android.core.activity.model.ActivityIconId

fun ActivityIconId.toImageVector(): ImageVector = when (this) {
    ActivityIconId.TIMER -> Icons.Filled.Timer
    ActivityIconId.POMODORO_FOCUS -> Icons.Filled.MenuBook
    ActivityIconId.POMODORO_BREAK -> Icons.Filled.Coffee
    ActivityIconId.STOPWATCH -> Icons.Filled.Watch
    ActivityIconId.MEDIA -> Icons.Filled.MusicNote
    ActivityIconId.STUDY -> Icons.Filled.MenuBook
    ActivityIconId.COOKING -> Icons.Filled.RestaurantMenu
    ActivityIconId.FITNESS_WALK -> Icons.Filled.DirectionsWalk
    ActivityIconId.FITNESS_RUN -> Icons.Filled.DirectionsRun
    ActivityIconId.FITNESS_CYCLE -> Icons.Filled.DirectionsBike
    ActivityIconId.TRIP -> Icons.Filled.Route
    ActivityIconId.EXPENSE -> Icons.Filled.Payments
    ActivityIconId.DOWNLOAD -> Icons.Filled.Download
    ActivityIconId.AI_TASK -> Icons.Filled.SmartToy
    ActivityIconId.REMINDER -> Icons.Filled.Notifications
    ActivityIconId.DELIVERY -> Icons.Filled.LocalShipping
    ActivityIconId.FLIGHT -> Icons.Filled.Flight
    ActivityIconId.CHECK -> Icons.Filled.Check
}
