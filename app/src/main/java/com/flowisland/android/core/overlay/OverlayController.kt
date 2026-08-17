package com.flowisland.android.core.overlay

import android.content.Context
import android.content.Intent
import com.flowisland.android.core.activity.ActivityEngine
import com.flowisland.android.core.datastore.SettingsRepository
import com.flowisland.android.core.di.ApplicationScope
import com.flowisland.android.core.permissions.PermissionsManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OverlayController @Inject constructor(
    @ApplicationContext private val context: Context,
    private val activityEngine: ActivityEngine,
    private val settingsRepository: SettingsRepository,
    private val permissionsManager: PermissionsManager,
    @ApplicationScope private val scope: CoroutineScope,
) {
    private var running = false

    fun start() {
        scope.launch {
            combine(settingsRepository.settings, activityEngine.activities) { settings, activities ->
                settings.overlayEnabled && permissionsManager.hasOverlayPermission() && activities.isNotEmpty()
            }.distinctUntilChanged().collect { shouldRun ->
                if (shouldRun && !running) {
                    context.startForegroundService(Intent(context, FlowIslandOverlayService::class.java))
                    running = true
                } else if (!shouldRun && running) {
                    context.stopService(Intent(context, FlowIslandOverlayService::class.java))
                    running = false
                }
            }
        }
    }
}
