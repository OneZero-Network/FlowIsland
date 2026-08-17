package com.flowisland.android.feature.switcher

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.flowisland.android.R
import com.flowisland.android.core.ui.components.ActivityCard
import com.flowisland.android.core.ui.components.EmptyState
import com.flowisland.android.feature.home.HomeViewModel

@Composable
fun SwitcherScreen(onOpenActivity: (String) -> Unit, onBack: () -> Unit) {
    val viewModel: HomeViewModel = hiltViewModel()
    val activities by viewModel.activities.collectAsState()

    Scaffold(topBar = { TopAppBar(title = { Text(stringResource(R.string.switcher_title)) }) }) { padding ->
        if (activities.isEmpty()) {
            EmptyState(
                title = stringResource(R.string.home_nothing_active_title),
                body = stringResource(R.string.home_nothing_active_body),
                modifier = Modifier.padding(padding),
            )
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp, vertical = 12.dp)) {
                items(activities, key = { it.id.value }) { activity ->
                    ActivityCard(
                        state = activity,
                        onOpen = { onOpenActivity(activity.id.value) },
                        onPin = { viewModel.pin(activity.id.value) },
                        onUnpin = { viewModel.unpin(activity.id.value) },
                        onHide = { viewModel.hide(activity.id.value) },
                        onStop = { viewModel.stop(activity.id.value) },
                        modifier = Modifier.padding(bottom = 12.dp),
                    )
                }
            }
        }
    }
}
