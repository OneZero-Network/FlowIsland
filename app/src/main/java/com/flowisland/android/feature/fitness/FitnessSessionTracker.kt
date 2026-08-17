package com.flowisland.android.feature.fitness

import com.flowisland.android.core.activity.ActivityEngine
import com.flowisland.android.core.activity.model.ActivityId
import com.flowisland.android.core.activity.model.ActivityState
import com.flowisland.android.core.activity.model.ActivityType
import com.flowisland.android.core.database.FitnessSessionDao
import com.flowisland.android.core.database.FitnessSessionEntity
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

/** Runtime tracker for a fitness activity. Progress is mirrored into the activity
 * payload so a process restart can restore distance and start time. */
data class FitnessStats(
    val distanceMeters: Double = 0.0,
    val startedAt: Long = System.currentTimeMillis(),
    val lastPointAt: Long = System.currentTimeMillis(),
) {
    fun paceLabel(): String {
        if (distanceMeters < 10) return "--:--"
        val elapsedMinutes = (lastPointAt - startedAt) / 60_000.0
        if (elapsedMinutes <= 0) return "--:--"
        val paceMinPerKm = elapsedMinutes / (distanceMeters / 1000.0)
        val minutes = paceMinPerKm.toInt()
        val seconds = ((paceMinPerKm - minutes) * 60).toInt()
        return "%d:%02d /km".format(minutes, seconds)
    }
}

@Singleton
class FitnessSessionTracker @Inject constructor(
    private val locationTracker: LocationTracker,
    private val activityEngine: ActivityEngine,
    private val fitnessSessionDao: FitnessSessionDao,
    private val permissionsManager: PermissionsManager,
    @ApplicationScope private val scope: CoroutineScope,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {
    private val statsFlows = mutableMapOf<String, MutableStateFlow<FitnessStats>>()
    private val trackingJobs = mutableMapOf<String, Job>()
    private val lastPointByActivity = mutableMapOf<String, TrackPoint>()

    fun observe(activityId: String): StateFlow<FitnessStats> =
        statsFlows.getOrPut(activityId) { MutableStateFlow(FitnessStats()) }

    /** Restart location collection for restored fitness activities after a process death. */
    fun recover() {
        scope.launch {
            activityEngine.awaitRestored()
            if (!permissionsManager.hasAnyLocationPermission()) return@launch
            activityEngine.snapshot()
                .filter { it.type == ActivityType.FITNESS && it.state == ActivityState.ACTIVE }
                .forEach { state ->
                    val parts = state.payloadId?.split("|", limit = 4) ?: emptyList()
                    val kind = parts.getOrNull(1) ?: "RUN"
                    start(state.id.value, kind, state.title)
                }
        }
    }

    fun start(activityId: String, kind: String, label: String) {
        val existing = activityEngine.get(ActivityId(activityId))
        val parts = existing?.payloadId?.split("|", limit = 4) ?: emptyList()
        val restoredDistance = parts.getOrNull(2)?.toDoubleOrNull() ?: 0.0
        val restoredStartedAt = parts.getOrNull(3)?.toLongOrNull() ?: existing?.createdAt ?: System.currentTimeMillis()

        val statsFlow = statsFlows.getOrPut(activityId) {
            MutableStateFlow(
                FitnessStats(
                    distanceMeters = restoredDistance,
                    startedAt = restoredStartedAt,
                    lastPointAt = System.currentTimeMillis(),
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
                    lastPointAt = point.timestampMillis,
                )
                statsFlow.value = updated
                activityEngine.update(ActivityId(activityId)) {
                    it.copy(
                        subtitle = "%.2f km".format(updated.distanceMeters / 1000.0),
                        payloadId = "fitness|$kind|${updated.distanceMeters}|${updated.startedAt}",
                    )
                }
            }
        }
    }

    suspend fun finish(activityId: String, kind: String, label: String) {
        trackingJobs[activityId]?.cancel()
        trackingJobs.remove(activityId)
        lastPointByActivity.remove(activityId)
        val stats = statsFlows[activityId]?.value ?: restoredStats(activityId)
        val endedAt = System.currentTimeMillis()
        withContext(ioDispatcher) {
            fitnessSessionDao.insert(
                FitnessSessionEntity(
                    id = activityId,
                    activityKind = kind,
                    label = label,
                    distanceMeters = stats.distanceMeters,
                    durationMillis = (endedAt - stats.startedAt).coerceAtLeast(0L),
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

    private fun restoredStats(activityId: String): FitnessStats {
        val state = activityEngine.get(ActivityId(activityId))
        val parts = state?.payloadId?.split("|", limit = 4) ?: emptyList()
        return FitnessStats(
            distanceMeters = parts.getOrNull(2)?.toDoubleOrNull() ?: 0.0,
            startedAt = parts.getOrNull(3)?.toLongOrNull() ?: state?.createdAt ?: System.currentTimeMillis(),
        )
    }
}
