package com.flowisland.android.feature.media

import android.content.ComponentName
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.flowisland.android.core.activity.ActivityEngine
import com.flowisland.android.core.activity.model.ActivityAction
import com.flowisland.android.core.activity.model.ActivityIconId
import com.flowisland.android.core.activity.model.ActivityId
import com.flowisland.android.core.activity.model.ActivityState
import com.flowisland.android.core.activity.model.ActivityType
import com.flowisland.android.core.activity.model.ActivityUiState
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Opt-in only (disabled in the manifest by default; Settings enables it, which
 * routes the user to the system "Notification access" screen). This is the only
 * public Android API that can see "is anything playing right now" across other
 * apps -- see the deviation note in AndroidManifest.xml and BUILD_REPORT.md.
 */
@AndroidEntryPoint
class MediaSessionListenerService : NotificationListenerService() {

    @Inject lateinit var activityEngine: ActivityEngine

    private val mediaActivityId = ActivityId("media-session-singleton")
    private var activeController: MediaController? = null

    companion object {
        /** Set only while the system has this listener bound; used to route play/pause taps here. */
        var activeInstance: MediaSessionListenerService? = null
            private set
    }

    private val controllerCallback = object : MediaController.Callback() {
        override fun onPlaybackStateChanged(state: PlaybackState?) = refresh()
        override fun onMetadataChanged(metadata: android.media.MediaMetadata?) = refresh()
        override fun onSessionDestroyed() = refresh()
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        activeInstance = this
        refresh()
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        activeInstance = null
        activeController?.unregisterCallback(controllerCallback)
        activeController = null
        activityEngine.dismiss(mediaActivityId)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) = refresh()
    override fun onNotificationRemoved(sbn: StatusBarNotification?) = refresh()

    private fun refresh() {
        val manager = getSystemService(MediaSessionManager::class.java) ?: return
        val sessions = runCatching {
            manager.getActiveSessions(ComponentName(this, MediaSessionListenerService::class.java))
        }.getOrDefault(emptyList())

        val playingSession = sessions.firstOrNull { it.playbackState?.state == PlaybackState.STATE_PLAYING } ?: sessions.firstOrNull()

        if (playingSession == null) {
            activeController?.unregisterCallback(controllerCallback)
            activeController = null
            activityEngine.dismiss(mediaActivityId)
            return
        }

        if (activeController?.sessionToken != playingSession.sessionToken) {
            activeController?.unregisterCallback(controllerCallback)
            activeController = playingSession
            activeController?.registerCallback(controllerCallback)
        }

        val metadata = playingSession.metadata
        val title = metadata?.getString(android.media.MediaMetadata.METADATA_KEY_TITLE) ?: "Playing"
        val artist = metadata?.getString(android.media.MediaMetadata.METADATA_KEY_ARTIST)
        val isPlaying = playingSession.playbackState?.state == PlaybackState.STATE_PLAYING

        val playPauseAction = ActivityAction(
            "play_pause",
            if (isPlaying) com.flowisland.android.R.string.action_pause else com.flowisland.android.R.string.action_resume,
            if (isPlaying) ActivityAction.Kind.PAUSE else ActivityAction.Kind.RESUME,
        )

        activityEngine.upsert(
            ActivityUiState(
                id = mediaActivityId,
                type = ActivityType.MEDIA,
                title = title,
                subtitle = artist,
                icon = ActivityIconId.MEDIA,
                state = if (isPlaying) ActivityState.ACTIVE else ActivityState.PAUSED,
                actions = listOf(playPauseAction),
            )
        )
    }

    fun handlePlayPause(resume: Boolean) {
        val controller = activeController ?: return
        if (resume) controller.transportControls.play() else controller.transportControls.pause()
    }
}
