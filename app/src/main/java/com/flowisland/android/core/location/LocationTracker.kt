package com.flowisland.android.core.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Looper
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject
import javax.inject.Singleton

data class TrackPoint(val latitude: Double, val longitude: Double, val timestampMillis: Long, val speedMetersPerSecond: Float)

/**
 * Deliberately built on the plain android.location APIs rather than Google Play
 * Services FusedLocationProvider, so FlowIsland has no dependency on Play
 * Services being installed. Only ever started from a Fitness/Trip screen after
 * the user taps Start, and stopped the instant the session ends or is cancelled
 * -- never runs in the background beyond that window.
 */
@Singleton
class LocationTracker @Inject constructor(@ApplicationContext private val context: Context) {

    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    @SuppressLint("MissingPermission") // Caller is required to check PermissionsManager first.
    fun trackLocation(minIntervalMillis: Long = 3_000L, minDistanceMeters: Float = 5f): Flow<TrackPoint> = callbackFlow {
        val provider = when {
            locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
            locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
            else -> null
        }
        if (provider == null) {
            close(IllegalStateException("No location provider is enabled on this device."))
            return@callbackFlow
        }

        val listener = LocationListener { location: Location ->
            trySend(
                TrackPoint(
                    latitude = location.latitude,
                    longitude = location.longitude,
                    timestampMillis = location.time,
                    speedMetersPerSecond = if (location.hasSpeed()) location.speed else 0f,
                )
            )
        }

        locationManager.requestLocationUpdates(provider, minIntervalMillis, minDistanceMeters, listener, Looper.getMainLooper())

        awaitClose { locationManager.removeUpdates(listener) }
    }

    companion object {
        /** Haversine-based distance in meters -- used to accumulate trip/workout distance from consecutive points. */
        fun distanceMeters(from: TrackPoint, to: TrackPoint): Double {
            val result = FloatArray(1)
            Location.distanceBetween(from.latitude, from.longitude, to.latitude, to.longitude, result)
            return result[0].toDouble()
        }
    }
}
