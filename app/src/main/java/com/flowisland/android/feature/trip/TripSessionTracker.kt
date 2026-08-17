package com.flowisland.android.feature.trip

import com.flowisland.android.core.activity.ActivityEngine
import com.flowisland.android.core.activity.model.ActivityId
import com.flowisland.android.core.database.TripDao
import com.flowisland.android.core.database.TripEntity
import com.flowisland.android.core.di.ApplicationScope
import com.flowisland.android.core.di.IoDispatcher
import com.flowisland.android.core.location.LocationTracker
import com.flowisland.android.core.location.TrackPoint
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
    fun averageSpeedKmh(): Double {
        val elapsedHours = (System.currentTimeMillis() - startedAt) / 3_600_000.0
        return if (elapsedHours <= 0) 0.0 else (distanceMeters / 1000.0) / elapsedHours
    }
}

@Singleton
class TripSessionTracker @Inject constructor(
    private val locationTracker: LocationTracker,
    private val activityEngine: ActivityEngine,
    private val tripDao: TripDao,
    @ApplicationScope private val scope: CoroutineScope,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {
    private val statsFlows = mutableMapOf<String, MutableStateFlow<TripStats>>()
    private val trackingJobs = mutableMapOf<String, Job>()
    private val lastPointByActivity = mutableMapOf<String, TrackPoint>()

    fun observe(activityId: String): StateFlow<TripStats> = statsFlows.getOrPut(activityId) { MutableStateFlow(TripStats()) }

    fun start(activityId: String) {
        val statsFlow = statsFlows.getOrPut(activityId) { MutableStateFlow(TripStats()) }
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
                    it.copy(subtitle = "%.1f km".format(updated.distanceMeters / 1000.0))
                }
            }
        }
    }

    suspend fun finish(activityId: String, destinationLabel: String?) {
        trackingJobs[activityId]?.cancel()
        trackingJobs.remove(activityId)
        lastPointByActivity.remove(activityId)
        val stats = statsFlows[activityId]?.value ?: TripStats()
        withContext(ioDispatcher) {
            tripDao.insert(
                TripEntity(
                    id = activityId,
                    destinationLabel = destinationLabel,
                    distanceMeters = stats.distanceMeters,
                    durationMillis = System.currentTimeMillis() - stats.startedAt,
                    averageSpeedKmh = stats.averageSpeedKmh(),
                    startedAt = stats.startedAt,
                    endedAt = System.currentTimeMillis(),
                )
            )
        }
        statsFlows.remove(activityId)
    }

    fun cancel(activityId: String) {
        trackingJobs[activityId]?.cancel()
        trackingJobs.remove(activityId)
        statsFlows.remove(activityId)
    }
}
