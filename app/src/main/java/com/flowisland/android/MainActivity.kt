package com.flowisland.android

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.navigation.compose.rememberNavController
import com.flowisland.android.core.datastore.SettingsRepository
import com.flowisland.android.core.permissions.PermissionsManager
import com.flowisland.android.core.ui.theme.FlowIslandTheme
import com.flowisland.android.navigation.FlowIslandNavGraph
import com.flowisland.android.navigation.Screen
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class MainActivityViewModel @Inject constructor(
    val settingsRepository: SettingsRepository,
    val permissionsManager: PermissionsManager,
) : ViewModel()

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    companion object {
        const val EXTRA_OPEN_ACTIVITY_ID = "extra_open_activity_id"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        val deepLinkActivityId = intent.getStringExtra(EXTRA_OPEN_ACTIVITY_ID)

        setContent {
            val viewModel: MainActivityViewModel = hiltViewModel()
            val settings by viewModel.settingsRepository.settings.collectAsState(initial = null)

            // Wait for the first real settings read before deciding onboarding vs
            // home, so a fresh install never flashes the wrong start screen.
            val resolvedSettings = settings ?: return@setContent

            FlowIslandTheme(themeMode = resolvedSettings.themeMode) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val navController = rememberNavController()
                    val startDestination = if (resolvedSettings.onboardingCompleted) Screen.Home.route else Screen.Onboarding.route

                    val notificationPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}

                    LaunchedEffect(resolvedSettings.onboardingCompleted) {
                        if (resolvedSettings.onboardingCompleted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            if (!viewModel.permissionsManager.hasNotificationPermission()) {
                                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                        }
                    }

                    LaunchedEffect(deepLinkActivityId) {
                        deepLinkActivityId?.let { id ->
                            navController.navigate(Screen.Detail.createRoute(id))
                        }
                    }

                    FlowIslandNavGraph(navController = navController, startDestination = startDestination, permissionsManager = viewModel.permissionsManager)
                }
            }
        }
    }
}
