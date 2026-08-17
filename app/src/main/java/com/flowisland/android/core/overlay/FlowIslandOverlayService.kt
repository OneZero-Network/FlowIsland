package com.flowisland.android.core.overlay

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ServiceLifecycleDispatcher
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.flowisland.android.MainActivity
import com.flowisland.android.R
import com.flowisland.android.core.activity.ActivityEngine
import com.flowisland.android.core.activity.PriorityEngine
import com.flowisland.android.core.activity.model.ActivityAction
import com.flowisland.android.core.activity.model.ActivityId
import com.flowisland.android.core.datastore.IslandDisplayMode
import com.flowisland.android.core.datastore.SettingsRepository
import com.flowisland.android.core.notification.NotificationChannels
import com.flowisland.android.core.ui.components.CollapsedIslandRow
import com.flowisland.android.core.ui.components.ExpandedIslandCard
import com.flowisland.android.core.ui.theme.FlowIslandTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

/**
 * Hosts the collapsed/expanded island as a real WindowManager overlay. Only ever
 * started by [OverlayController] after the user has both (a) explicitly enabled
 * "floating island" in Settings and (b) granted SYSTEM_ALERT_WINDOW. A
 * foreground service of type "specialUse" (declared + justified in the
 * manifest) because the overlay is a genuinely ongoing, user-visible surface
 * the user asked for -- it is stopped the instant the setting is disabled, the
 * permission is revoked, or there is nothing left to show.
 */
@AndroidEntryPoint
class FlowIslandOverlayService : Service(), LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {

    @Inject lateinit var activityEngine: ActivityEngine
    @Inject lateinit var settingsRepository: SettingsRepository

    private val dispatcher = ServiceLifecycleDispatcher(this)
    override val lifecycle: Lifecycle get() = dispatcher.lifecycle
    override val viewModelStore = ViewModelStore()
    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry

    private var windowManager: WindowManager? = null
    private var composeView: ComposeView? = null
    private var expandedId = mutableStateOf<ActivityId?>(null)

    override fun onCreate() {
        dispatcher.onServicePreSuperOnCreate()
        savedStateRegistryController.performRestore(null)
        super.onCreate()

        // Add the visible TYPE_APPLICATION_OVERLAY window before promoting the
        // service. Android 15 requires a visible overlay for the SYSTEM_ALERT_WINDOW
        // background-start exemption; OverlayController also only starts this service
        // while the app is foregrounded.
        addOverlayView()
        startForeground(NOTIFICATION_ID, buildForegroundNotification())
        isRunning = true

        combine(activityEngine.activities, settingsRepository.settings) { activities, settings ->
            val visible = if (settings.islandDisplayMode == IslandDisplayMode.PINNED_ONLY) activities.filter { it.pinned } else activities
            visible.take(PriorityEngine.MAX_VISIBLE_COLLAPSED)
        }.onEach { _ ->
            // OverlayController owns the stop decision. Keeping the service alive
            // for this short coordination window prevents a stale controller state
            // from blocking a later restart.
        }.launchIn(lifecycleScope)
    }

    private fun addOverlayView() {
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY else @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            y = 24
        }

        val view = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@FlowIslandOverlayService)
            setViewTreeViewModelStoreOwner(this@FlowIslandOverlayService)
            setViewTreeSavedStateRegistryOwner(this@FlowIslandOverlayService)
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                FlowIslandTheme {
                    OverlayContent()
                }
            }
        }
        composeView = view
        windowManager?.addView(view, params)
    }

    @androidx.compose.runtime.Composable
    private fun OverlayContent() {
        val activities by activityEngine.activities.collectAsState()
        val expanded = expandedId.value
        val expandedState = activities.find { it.id == expanded }

        androidx.compose.foundation.layout.Box(modifier = androidx.compose.ui.Modifier.padding(top = 8.dp)) {
            if (expandedState != null) {
                ExpandedIslandCard(
                    state = expandedState,
                    onAction = { action -> handleAction(expandedState.id, action) },
                    onCollapse = { expandedId.value = null },
                    modifier = androidx.compose.ui.Modifier.padding(horizontal = 16.dp),
                )
            } else {
                CollapsedIslandRow(states = activities.take(PriorityEngine.MAX_VISIBLE_COLLAPSED)) { tapped ->
                    expandedId.value = tapped.id
                    activityEngine.markInteracted(tapped.id)
                }
            }
        }
    }

    private fun handleAction(id: ActivityId, action: ActivityAction) {
        when (action.kind) {
            ActivityAction.Kind.PAUSE -> activityEngine.pause(id)
            ActivityAction.Kind.RESUME -> activityEngine.resume(id)
            ActivityAction.Kind.FINISH -> { activityEngine.complete(id); expandedId.value = null }
            ActivityAction.Kind.CANCEL -> { activityEngine.cancel(id); expandedId.value = null }
            ActivityAction.Kind.ADD_1_MIN -> activityEngine.update(id) { it.copy(timer = it.timer?.addDuration(60_000)) }
            ActivityAction.Kind.ADD_5_MIN -> activityEngine.update(id) { it.copy(timer = it.timer?.addDuration(300_000)) }
            else -> Unit // Actions needing full app context (lap list, navigation chooser) open the app instead.
        }
    }

    private fun buildForegroundNotification(): Notification {
        val openIntent = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(this, NotificationChannels.ACTIVE_ACTIVITIES)
            .setSmallIcon(R.drawable.ic_notification_flowisland)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(getString(R.string.settings_enable_island))
            .setOngoing(true)
            .setContentIntent(openIntent)
            .build()
    }

    override fun onBind(intent: Intent?): IBinder? {
        dispatcher.onServicePreSuperOnBind()
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        dispatcher.onServicePreSuperOnStart()
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        isRunning = false
        composeView?.let { runCatching { windowManager?.removeView(it) } }
        dispatcher.onServicePreSuperOnDestroy()
        super.onDestroy()
    }

    companion object {
        @Volatile
        var isRunning: Boolean = false
            private set

        private const val NOTIFICATION_ID = 9000
    }
}
