package com.flowisland.android.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flowisland.android.R
import com.flowisland.android.core.activity.model.ActivityType
import com.flowisland.android.core.datastore.AnimationIntensity
import com.flowisland.android.core.datastore.AutoCollapseDuration
import com.flowisland.android.core.datastore.FlowIslandSettings
import com.flowisland.android.core.datastore.IslandDisplayMode
import com.flowisland.android.core.datastore.IslandSize
import com.flowisland.android.core.datastore.SettingsRepository
import com.flowisland.android.core.database.AppDatabase
import com.flowisland.android.core.permissions.PermissionsManager
import com.flowisland.android.core.ui.components.SectionHeader
import com.flowisland.android.core.ui.theme.ThemeMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    val settingsRepository: SettingsRepository,
    private val database: AppDatabase,
    val permissionsManager: PermissionsManager,
) : ViewModel() {
    val settings = settingsRepository.settings

    fun deleteAllData() {
        viewModelScope.launch {
            database.clearAllTables()
            settingsRepository.clearAll()
            settingsRepository.setOnboardingCompleted(true) // stay past onboarding after a data wipe
        }
    }
}

@Composable
fun SettingsScreen(onBack: () -> Unit, onPrivacyPolicy: () -> Unit) {
    val viewModel: SettingsViewModel = hiltViewModel()
    val settings by viewModel.settings.collectAsState(initial = FlowIslandSettings())
    val context = LocalContext.current
    var showDeleteConfirm by remember { mutableStateOf(false) }
    val scope = androidx.compose.runtime.rememberCoroutineScope()

    Scaffold(topBar = { TopAppBar(title = { Text(stringResource(R.string.settings_title)) }) }) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp)) {
            item { SectionHeader(stringResource(R.string.settings_section_flowisland), Modifier.padding(top = 16.dp)) }
            item {
                SwitchRow(stringResource(R.string.settings_enable_island), settings.overlayEnabled) { enabled ->
                    if (enabled && !viewModel.permissionsManager.hasOverlayPermission()) {
                        context.startActivity(viewModel.permissionsManager.overlayPermissionIntent())
                    }
                    scope.launch { viewModel.settingsRepository.setOverlayEnabled(enabled) }
                }
            }
            item {
                ChipRow(stringResource(R.string.settings_island_size), IslandSize.entries.map { it to it.name.lowercase() }, settings.islandSize) {
                    scope.launch { viewModel.settingsRepository.setIslandSize(it) }
                }
            }
            item {
                ChipRow(
                    stringResource(R.string.settings_island_animation),
                    listOf(
                        AnimationIntensity.SUBTLE to stringResource(R.string.settings_animation_subtle),
                        AnimationIntensity.STANDARD to stringResource(R.string.settings_animation_standard),
                        AnimationIntensity.REDUCED_MOTION to stringResource(R.string.settings_animation_reduced),
                    ),
                    settings.animationIntensity,
                ) { scope.launch { viewModel.settingsRepository.setAnimationIntensity(it) } }
            }
            item {
                ChipRow(
                    stringResource(R.string.settings_island_display),
                    listOf(
                        IslandDisplayMode.ALL to stringResource(R.string.settings_display_all),
                        IslandDisplayMode.PINNED_ONLY to stringResource(R.string.settings_display_pinned),
                    ),
                    settings.islandDisplayMode,
                ) { scope.launch { viewModel.settingsRepository.setIslandDisplayMode(it) } }
            }
            item {
                ChipRow(
                    stringResource(R.string.settings_auto_collapse),
                    listOf(
                        AutoCollapseDuration.FIVE to stringResource(R.string.settings_auto_collapse_5s),
                        AutoCollapseDuration.TEN to stringResource(R.string.settings_auto_collapse_10s),
                        AutoCollapseDuration.THIRTY to stringResource(R.string.settings_auto_collapse_30s),
                        AutoCollapseDuration.NEVER to stringResource(R.string.settings_auto_collapse_never),
                    ),
                    settings.autoCollapse,
                ) { scope.launch { viewModel.settingsRepository.setAutoCollapse(it) } }
            }

            item { SectionHeader(stringResource(R.string.settings_section_activities), Modifier.padding(top = 24.dp)) }
            items(ActivityType.entries.toList()) { type ->
                SwitchRow(type.name.lowercase().replaceFirstChar(Char::uppercase), type !in settings.disabledActivityTypes) { enabled ->
                    scope.launch { viewModel.settingsRepository.setActivityTypeEnabled(type, enabled) }
                }
            }

            item { SectionHeader(stringResource(R.string.settings_section_appearance), Modifier.padding(top = 24.dp)) }
            item {
                ChipRow(
                    "",
                    listOf(
                        ThemeMode.SYSTEM to stringResource(R.string.settings_theme_system),
                        ThemeMode.LIGHT to stringResource(R.string.settings_theme_light),
                        ThemeMode.DARK to stringResource(R.string.settings_theme_dark),
                    ),
                    settings.themeMode,
                ) { scope.launch { viewModel.settingsRepository.setThemeMode(it) } }
            }

            item { SectionHeader(stringResource(R.string.settings_section_privacy), Modifier.padding(top = 24.dp)) }
            item {
                TextButton(onClick = onPrivacyPolicy) { Text(stringResource(R.string.settings_privacy_policy)) }
            }
            item {
                TextButton(onClick = { showDeleteConfirm = true }) { Text(stringResource(R.string.settings_delete_data), color = MaterialTheme.colorScheme.error) }
            }

            item { SectionHeader(stringResource(R.string.settings_section_about), Modifier.padding(top = 24.dp)) }
            item {
                val versionName = remember {
                    runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull() ?: "1.0.0"
                }
                Text("${stringResource(R.string.settings_version)}: $versionName", modifier = Modifier.padding(vertical = 8.dp))
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(stringResource(R.string.settings_delete_data_confirm_title)) },
            text = { Text(stringResource(R.string.settings_delete_data_confirm_body)) },
            confirmButton = {
                TextButton(onClick = { viewModel.deleteAllData(); showDeleteConfirm = false }) {
                    Text(stringResource(R.string.settings_delete_data), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { showDeleteConfirm = false }) { Text(stringResource(R.string.action_cancel)) } },
        )
    }
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Text(label, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun <T> ChipRow(label: String, options: List<Pair<T, String>>, selected: T, onSelect: (T) -> Unit) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        if (label.isNotBlank()) Text(label, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(bottom = 4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEach { (value, text) ->
                FilterChip(selected = value == selected, onClick = { onSelect(value) }, label = { Text(text) })
            }
        }
    }
}
