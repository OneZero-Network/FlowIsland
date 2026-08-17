package com.flowisland.android.core.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.flowisland.android.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

object NotificationChannels {
    const val ACTIVE_ACTIVITIES = "active_activities"
    const val TIMERS = "timers"
    const val REMINDERS = "reminders"
    const val FITNESS = "fitness"
    const val TRIPS = "trips"
}

@Singleton
class NotificationChannelInstaller @Inject constructor(@ApplicationContext private val context: Context) {

    fun installAll() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java)

        val channels = listOf(
            NotificationChannel(
                NotificationChannels.ACTIVE_ACTIVITIES,
                context.getString(R.string.channel_active_activities_name),
                NotificationManager.IMPORTANCE_LOW,
            ).apply { description = context.getString(R.string.channel_active_activities_desc) },
            NotificationChannel(
                NotificationChannels.TIMERS,
                context.getString(R.string.channel_timers_name),
                NotificationManager.IMPORTANCE_LOW,
            ).apply { description = context.getString(R.string.channel_timers_desc) },
            NotificationChannel(
                NotificationChannels.REMINDERS,
                context.getString(R.string.channel_reminders_name),
                NotificationManager.IMPORTANCE_HIGH,
            ).apply { description = context.getString(R.string.channel_reminders_desc) },
            NotificationChannel(
                NotificationChannels.FITNESS,
                context.getString(R.string.channel_fitness_name),
                NotificationManager.IMPORTANCE_LOW,
            ).apply { description = context.getString(R.string.channel_fitness_desc) },
            NotificationChannel(
                NotificationChannels.TRIPS,
                context.getString(R.string.channel_trips_name),
                NotificationManager.IMPORTANCE_LOW,
            ).apply { description = context.getString(R.string.channel_trips_desc) },
        )
        manager.createNotificationChannels(channels)
    }
}
