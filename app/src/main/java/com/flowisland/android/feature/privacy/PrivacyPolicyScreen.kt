package com.flowisland.android.feature.privacy

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.flowisland.android.R

@Composable
fun PrivacyPolicyScreen(onBack: () -> Unit) {
    Scaffold(topBar = { TopAppBar(title = { Text(stringResource(R.string.privacy_policy_title)) }) }) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp, vertical = 16.dp).verticalScroll(rememberScrollState()),
        ) {
            PolicySection(
                "No account, no server",
                "FlowIsland has no login, no user account, and no backend server. Every activity, timer, trip, expense and reminder you create is stored only in this app's private storage on this device, using Android's Room database and DataStore preferences.",
            )
            PolicySection(
                "What each permission is for",
                "• Notifications -- to show your ongoing activities and let you control them without opening the app.\n" +
                    "• Display over other apps (optional) -- only if you enable the floating island, to show a small pill above other apps. You can decline this and use notifications instead; nothing else changes.\n" +
                    "• Location (optional) -- requested only when you start a Fitness or Trip activity, to calculate distance and pace. Location access stops the moment that activity ends.\n" +
                    "• Notification access (optional) -- requested only if you enable Media tracking in Settings. This is the only public Android API that lets an app see what's currently playing across other apps; FlowIsland uses it solely to show playback controls and does not read or store notification content for anything else.\n" +
                    "• Alarms & reminders / exact alarm scheduling -- used to ring Reminders and update Flight status at the times you specify.",
            )
            PolicySection(
                "No tracking",
                "FlowIsland contains no analytics SDK and no advertising SDK. Nothing you do in the app is sent anywhere -- there is nowhere for it to go, since the app has no server.",
            )
            PolicySection(
                "Deleting your data",
                "Settings > Delete all local data permanently removes every FlowIsland activity, timer, session, trip and expense from this device. This cannot be undone. Uninstalling the app has the same effect.",
            )
        }
    }
}

@Composable
private fun PolicySection(title: String, body: String) {
    Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 16.dp, bottom = 6.dp))
    Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}
