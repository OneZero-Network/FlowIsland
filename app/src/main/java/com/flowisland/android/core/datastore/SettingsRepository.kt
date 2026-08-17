package com.flowisland.android.core.datastore

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.flowisland.android.core.activity.model.ActivityType
import com.flowisland.android.core.ui.theme.ThemeMode
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "flowisland_settings")

enum class IslandSize { SMALL, DEFAULT, LARGE }
enum class AnimationIntensity { SUBTLE, STANDARD, REDUCED_MOTION }
enum class IslandDisplayMode { ALL, PINNED_ONLY }
enum class AutoCollapseDuration(val seconds: Int) { FIVE(5), TEN(10), THIRTY(30), NEVER(-1) }

data class FlowIslandSettings(
    val onboardingCompleted: Boolean = false,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val overlayEnabled: Boolean = false,
    val islandSize: IslandSize = IslandSize.DEFAULT,
    val animationIntensity: AnimationIntensity = AnimationIntensity.STANDARD,
    val islandDisplayMode: IslandDisplayMode = IslandDisplayMode.ALL,
    val autoCollapse: AutoCollapseDuration = AutoCollapseDuration.TEN,
    val mediaTrackingEnabled: Boolean = false,
    val disabledActivityTypes: Set<ActivityType> = emptySet(),
)

@Singleton
class SettingsRepository @Inject constructor(@ApplicationContext private val context: Context) {

    private object Keys {
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val OVERLAY_ENABLED = booleanPreferencesKey("overlay_enabled")
        val ISLAND_SIZE = stringPreferencesKey("island_size")
        val ANIMATION_INTENSITY = stringPreferencesKey("animation_intensity")
        val ISLAND_DISPLAY_MODE = stringPreferencesKey("island_display_mode")
        val AUTO_COLLAPSE_SECONDS = intPreferencesKey("auto_collapse_seconds")
        val MEDIA_TRACKING_ENABLED = booleanPreferencesKey("media_tracking_enabled")
        val DISABLED_ACTIVITY_TYPES = stringPreferencesKey("disabled_activity_types")
    }

    val settings: Flow<FlowIslandSettings> = context.dataStore.data.map { prefs ->
        FlowIslandSettings(
            onboardingCompleted = prefs[Keys.ONBOARDING_COMPLETED] ?: false,
            themeMode = prefs[Keys.THEME_MODE]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() } ?: ThemeMode.SYSTEM,
            overlayEnabled = prefs[Keys.OVERLAY_ENABLED] ?: false,
            islandSize = prefs[Keys.ISLAND_SIZE]?.let { runCatching { IslandSize.valueOf(it) }.getOrNull() } ?: IslandSize.DEFAULT,
            animationIntensity = prefs[Keys.ANIMATION_INTENSITY]?.let { runCatching { AnimationIntensity.valueOf(it) }.getOrNull() } ?: AnimationIntensity.STANDARD,
            islandDisplayMode = prefs[Keys.ISLAND_DISPLAY_MODE]?.let { runCatching { IslandDisplayMode.valueOf(it) }.getOrNull() } ?: IslandDisplayMode.ALL,
            autoCollapse = prefs[Keys.AUTO_COLLAPSE_SECONDS]?.let { secs -> AutoCollapseDuration.entries.find { it.seconds == secs } } ?: AutoCollapseDuration.TEN,
            mediaTrackingEnabled = prefs[Keys.MEDIA_TRACKING_ENABLED] ?: false,
            disabledActivityTypes = prefs[Keys.DISABLED_ACTIVITY_TYPES]
                ?.split(",")
                ?.filter { it.isNotBlank() }
                ?.mapNotNull { runCatching { ActivityType.valueOf(it) }.getOrNull() }
                ?.toSet() ?: emptySet(),
        )
    }

    suspend fun setOnboardingCompleted(completed: Boolean) = context.dataStore.edit { it[Keys.ONBOARDING_COMPLETED] = completed }
    suspend fun setThemeMode(mode: ThemeMode) = context.dataStore.edit { it[Keys.THEME_MODE] = mode.name }
    suspend fun setOverlayEnabled(enabled: Boolean) = context.dataStore.edit { it[Keys.OVERLAY_ENABLED] = enabled }
    suspend fun setIslandSize(size: IslandSize) = context.dataStore.edit { it[Keys.ISLAND_SIZE] = size.name }
    suspend fun setAnimationIntensity(intensity: AnimationIntensity) = context.dataStore.edit { it[Keys.ANIMATION_INTENSITY] = intensity.name }
    suspend fun setIslandDisplayMode(mode: IslandDisplayMode) = context.dataStore.edit { it[Keys.ISLAND_DISPLAY_MODE] = mode.name }
    suspend fun setAutoCollapse(duration: AutoCollapseDuration) = context.dataStore.edit { it[Keys.AUTO_COLLAPSE_SECONDS] = duration.seconds }
    suspend fun setMediaTrackingEnabled(enabled: Boolean) = context.dataStore.edit { it[Keys.MEDIA_TRACKING_ENABLED] = enabled }

    suspend fun setActivityTypeEnabled(type: ActivityType, enabled: Boolean) {
        context.dataStore.edit { prefs ->
            val current = prefs[Keys.DISABLED_ACTIVITY_TYPES]?.split(",")?.filter { it.isNotBlank() }?.toMutableSet() ?: mutableSetOf()
            if (enabled) current.remove(type.name) else current.add(type.name)
            prefs[Keys.DISABLED_ACTIVITY_TYPES] = current.joinToString(",")
        }
    }

    suspend fun clearAll() = context.dataStore.edit { it.clear() }
}
