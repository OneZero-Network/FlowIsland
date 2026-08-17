package com.flowisland.android.feature.media

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.flowisland.android.R
import com.flowisland.android.core.permissions.PermissionsManager
import com.flowisland.android.core.ui.components.PrimaryButton
import com.flowisland.android.core.ui.components.ScreenPadding
import com.flowisland.android.core.ui.components.SecondaryButton

@Composable
fun MediaSetupScreen(permissionsManager: PermissionsManager, onDone: () -> Unit) {
    val context = LocalContext.current
    val alreadyEnabled = permissionsManager.hasNotificationListenerAccess()

    Scaffold { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(ScreenPadding), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(stringResource(R.string.media_enable_title), style = MaterialTheme.typography.headlineMedium)
            Text(stringResource(R.string.media_enable_body), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

            if (alreadyEnabled) {
                Text(stringResource(R.string.type_media), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            }

            PrimaryButton(
                text = stringResource(R.string.media_enable_cta),
                onClick = { context.startActivity(permissionsManager.notificationListenerSettingsIntent()) },
            )
            SecondaryButton(text = stringResource(R.string.media_not_now), onClick = onDone)
        }
    }
}
