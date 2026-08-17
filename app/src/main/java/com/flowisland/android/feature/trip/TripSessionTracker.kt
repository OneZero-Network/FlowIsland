package com.flowisland.android.feature.trip

import android.util.Base64
import com.flowisland.android.core.activity.ActivityEngine
import com.flowisland.android.core.activity.model.ActivityId
import com.flowisland.android.core.activity.model.ActivityState
import com.flowisland.android.core.activity.model.ActivityType
import com.flowisland.android.core.database.TripDao
import com.flowisland.android.core.database.TripEntity
import com.flowisland.android.core.di.ApplicationScope
import com.flowisland.android.core.di.IoDispatcher
import com.flowisland.android.core.location.LocationTracker
import com.flowisland.android.core.location.TrackPoint
import com.flowisland.android.core.permissions.PermissionsManager
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

data class TripStats(
    val distanceMeters: Double = 0.0,
    val startedAt: Long = System.currentTimeMillis(),
    val currentSpeedKmh: Double = 0.0,
) {
    fun averageSpeedKmh(now: Long = System.currentTimeMillis()): Double {
        val elapsedHours = (now - startedAt) / 3_600_000.0
        return if (elapsedHours <= 0) 0.0 else (distanceMeters / 1000.0) / elapsedHours
    }
}

@Singleton
class TripSessionTracker @Inject constructor(
    private val locationTracker: LocationTracker,
    private val activityEngine: ActivityEngine,
    private val tripDao: TripDao,
    private val permissionsManager: PermissionsManager,
    @ApplicationScope private val scope: CoroutineScope,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {
    private val statsFlows = mutableMapOf<String, MutableStateFlow<TripStats>>()
    private val trackingJobs = mutableMapOf<String, Job>()
    private val lastPointByActivity = mutableMapOf<String, TrackPoint>()

    fun observe(activityId: String): StateFlow<TripStats> =
        statsFlows.getOrPut(activityId) { MutableStateFlow(TripStats()) }

    /** Restart location collection for restored trips after a process death. */
    fun recover() {
        scope.launch {
            activityEngine.awaitRestored()
            if (!permissionsManager.hasAnyLocationPermission()) return@launch
            activityEngine.snapshot()
                .filter { it.type == ActivityType.TRIP && it.state == ActivityState.ACTIVE }
                .forEach { state -> start(state.id.value) }
        }
    }

    fun start(activityId: String) {
        val existing = activityEngine.get(ActivityId(activityId))
        val parts = existing?.payloadId?.split("|", limit = 4) ?: emptyList()
        val destination = parts.getOrNull(1)?.let { encoded ->
            runCatching { String(Base64.decode(encoded, Base64.NO_WRAP)) }.getOrElse { encoded }
        }.orEmpty()
        val restoredDistance = parts.getOrNull(2)?.toDoubleOrNull() ?: 0.0
        val restoredStartedAt = parts.getOrNull(3)?.toLongOrNull() ?: existing?.createdAt ?: System.currentTimeMillis()

        val statsFlow = statsFlows.getOrPut(activityId) {
            MutableStateFlow(
                TripStats(
                    distanceMeters = restoredDistance,
                    startedAt = restoredStartedAt,
                )
            )
        }
        if (trackingJobs[activityId]?.isActive == true) return
        lastPointByActivity.remove(activityId)
        trackingJobs[activityId] = scope.launch {
            locationTracker.trackLocation().collect { point ->
                val previous = lastPointByActivity[activityId]
                val added = if (previous != null) LocationTracker.distanceMeters(previous, point) else 0.0
                lastPointByActivity[activityId] = point
                val updated = statsFlow.value.copy(
                    distanceMeters = statsFlow.value.distanceMeters + added,
                    currentSpeedKmh = point.speedMetersPerSecond * 3.6,
                )
                statsFlow.value = updated
                activityEngine.update(ActivityId(activityId)) {
                    it.copy(
                        subtitle = "%.1f km".format(updated.distanceMeters / 1000.0),
                        payloadId = "trip|${Base64.encodeToString(destination.toByteArray(), Base64.NO_WRAP)}|${updated.distanceMeters}|${updated.startedAt}",
                    )
                }
            }
        }
    }

    suspend fun finish(activityId: String, destinationLabel: String?) {
        trackingJobs[activityId]?.cancel()
        trackingJobs.remove(activityId)
        lastPointByActivity.remove(activityId)
        val stats = statsFlows[activityId]?.value ?: restoredStats(activityId)
        val endedAt = System.currentTimeMillis()
        withContext(ioDispatcher) {
            tripDao.insert(
                TripEntity(
                    id = activityId,
                    destinationLabel = destinationLabel,
                    distanceMeters = stats.distanceMeters,
                    durationMillis = (endedAt - stats.startedAt).coerceAtLeast(0L),
                    averageSpeedKmh = stats.averageSpeedKmh(endedAt),
                    startedAt = stats.startedAt,
                    endedAt = endedAt,
                )
            )
        }
        statsFlows.remove(activityId)
    }

    fun cancel(activityId: String) {
        trackingJobs[activityId]?.cancel()
        trackingJobs.remove(activityId)
        lastPointByActivity.remove(activityId)
        statsFlows.remove(activityId)
    }

    private fun restoredStats(activityId: String): TripStats {
        val state = activityEngine.get(ActivityId(activityId))
        val parts = state?.payloadId?.split("|", limit = 4) ?: emptyList()
        return TripStats(
            distanceMeters = parts.getOrNull(2)?.toDoubleOrNull() ?: 0.0,
            startedAt = parts.getOrNull(3)?.toLongOrNull() ?: state?.createdAt ?: System.currentTimeMillis(),
        )
    }
}
