package com.flowisland.android.core.overlay

import android.content.Context
import android.content.Intent
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.flowisland.android.core.activity.ActivityEngine
import com.flowisland.android.core.datastore.SettingsRepository
import com.flowisland.android.core.di.ApplicationScope
import com.flowisland.android.core.permissions.PermissionsManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Owns the lifecycle of the optional floating island.
 *
 * Android 15 tightened the SYSTEM_ALERT_WINDOW foreground-service exemption:
 * when starting from the background, an app must already have a visible overlay.
 * Therefore the controller only starts a new overlay service while the app is
 * foregrounded. Once the service is running it remains visible while the user
 * has enabled the feature and active activities exist.
 */
@Singleton
class OverlayController @Inject constructor(
    @ApplicationContext private val context: Context,
    private val activityEngine: ActivityEngine,
    private val settingsRepository: SettingsRepository,
    private val permissionsManager: PermissionsManager,
    @ApplicationScope private val scope: CoroutineScope,
) : DefaultLifecycleObserver {

    private val appForeground = MutableStateFlow(false)

    fun start() {
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
        appForeground.value = ProcessLifecycleOwner.get().lifecycle.currentState.isAtLeast(
            androidx.lifecycle.Lifecycle.State.STARTED
        )

        scope.launch {
            combine(
                settingsRepository.settings,
                activityEngine.activities,
                appForeground,
            ) { settings, activities, foreground ->
                Triple(settings, activities, foreground)
            }.distinctUntilChanged().collect { (settings, activities, foreground) ->
                val hasVisibleActivities = if (settings.islandDisplayMode == com.flowisland.android.core.datastore.IslandDisplayMode.PINNED_ONLY) {
                    activities.any { it.pinned }
                } else {
                    activities.isNotEmpty()
                }

                val shouldRun = settings.overlayEnabled &&
                    permissionsManager.hasOverlayPermission() &&
                    hasVisibleActivities

                val running = FlowIslandOverlayService.isRunning
                if (shouldRun && !running && foreground) {
                    runCatching {
                        context.startForegroundService(Intent(context, FlowIslandOverlayService::class.java))
                    }
                } else if (!shouldRun && running) {
                    context.stopService(Intent(context, FlowIslandOverlayService::class.java))
                }
            }
        }
    }

    override fun onStart(owner: LifecycleOwner) {
        appForeground.value = true
    }

    override fun onStop(owner: LifecycleOwner) {
        appForeground.value = false
    }
}
