package com.flowisland.android.feature.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flowisland.android.R
import com.flowisland.android.core.datastore.SettingsRepository
import com.flowisland.android.core.ui.components.PrimaryButton
import com.flowisland.android.core.ui.components.ScreenPadding
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

private data class OnboardingPage(val titleResId: Int, val bodyResId: Int)

private val pages = listOf(
    OnboardingPage(R.string.onboarding_1_title, R.string.onboarding_1_body),
    OnboardingPage(R.string.onboarding_2_title, R.string.onboarding_2_body),
    OnboardingPage(R.string.onboarding_3_title, R.string.onboarding_3_body),
)

@HiltViewModel
class OnboardingViewModel @Inject constructor(private val settingsRepository: SettingsRepository) : ViewModel() {
    fun complete(onDone: () -> Unit) {
        viewModelScope.launch {
            settingsRepository.setOnboardingCompleted(true)
            onDone()
        }
    }
}

@Composable
fun OnboardingScreen(onDone: () -> Unit) {
    val viewModel: OnboardingViewModel = hiltViewModel()
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScopeCompat()

    Scaffold { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(ScreenPadding)) {
            Row(modifier = Modifier.fillMaxWidth()) {
                TextButton(onClick = { viewModel.complete(onDone) }, modifier = Modifier.align(Alignment.Top)) {
                    Text(stringResource(R.string.onboarding_skip))
                }
            }

            HorizontalPager(state = pagerState, modifier = Modifier.weight(1f)) { page ->
                Column(
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(stringResource(pages[page].titleResId), style = MaterialTheme.typography.headlineLarge, textAlign = TextAlign.Center)
                    Text(
                        stringResource(pages[page].bodyResId),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 16.dp),
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                pages.indices.forEach { index ->
                    Text(if (index == pagerState.currentPage) "●" else "○", modifier = Modifier.padding(4.dp), color = MaterialTheme.colorScheme.primary)
                }
            }

            if (pagerState.currentPage == pages.lastIndex) {
                PrimaryButton(text = stringResource(R.string.onboarding_get_started), onClick = { viewModel.complete(onDone) })
            } else {
                PrimaryButton(text = "Next", onClick = { scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) } })
            }
        }
    }
}

@Composable
private fun rememberCoroutineScopeCompat() = androidx.compose.runtime.rememberCoroutineScope()
