package com.flowisland.android.core.compatibility

import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

data class DeviceCapabilities(
    val manufacturer: String,
    val model: String,
    val sdkInt: Int,
    val supportsLiveUpdates: Boolean,
    val supportsPromotedNotifications: Boolean,
    val canDrawOverlays: Boolean,
    val isBatteryOptimizationIgnored: Boolean,
    val isKnownRestrictiveOem: Boolean,
)

/**
 * Central point of OEM/version awareness. Nothing else in the app should branch
 * on Build.MANUFACTURER or Build.VERSION.SDK_INT directly -- route the decision
 * through here so fallbacks stay consistent and testable.
 *
 * "Restrictive OEM" list covers manufacturers with historically aggressive
 * background/overlay restrictions (Xiaomi/MIUI, OnePlus/OxygenOS deep
 * optimizations, some Motorola builds). This does not block any functionality --
 * it only informs the UI to proactively suggest whitelisting steps instead of
 * silently failing when a timer doesn't fire on time.
 */
@Singleton
class DeviceCompatibilityManager @Inject constructor(@ApplicationContext private val context: Context) {

    private val restrictiveManufacturers = setOf("xiaomi", "oneplus", "oppo", "vivo", "realme")

    fun currentCapabilities(): DeviceCapabilities {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager

        return DeviceCapabilities(
            manufacturer = Build.MANUFACTURER,
            model = Build.MODEL,
            sdkInt = Build.VERSION.SDK_INT,
            supportsLiveUpdates = Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA,
            supportsPromotedNotifications = Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA &&
                runCatching { notificationManager.canPostPromotedNotifications() }.getOrDefault(false),
            canDrawOverlays = Settings.canDrawOverlays(context),
            isBatteryOptimizationIgnored = powerManager.isIgnoringBatteryOptimizations(context.packageName),
            isKnownRestrictiveOem = Build.MANUFACTURER.lowercase() in restrictiveManufacturers,
        )
    }
}
