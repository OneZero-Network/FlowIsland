package com.flowisland.android.feature.cooking

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import com.flowisland.android.R
import com.flowisland.android.core.activity.ActivityActions
import com.flowisland.android.core.activity.ActivityEngine
import com.flowisland.android.core.activity.model.ActivityAction
import com.flowisland.android.core.activity.model.ActivityIconId
import com.flowisland.android.core.activity.model.ActivityId
import com.flowisland.android.core.activity.model.ActivityState
import com.flowisland.android.core.activity.model.ActivityType
import com.flowisland.android.core.activity.model.ActivityUiState
import com.flowisland.android.core.di.ApplicationScope
import com.flowisland.android.core.time.TimerSpec
import com.flowisland.android.core.ui.components.PrimaryButton
import com.flowisland.android.core.ui.components.ScreenPadding
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

data class CookingStepUi(val name: String, val durationMinutes: Int)

/** In-memory ordered step list + current-step pointer for a running cooking session. */
object CookingStepStore {
    private val stepsByActivity = mutableMapOf<String, List<CookingStepUi>>()
    private val currentIndexByActivity = mutableMapOf<String, Int>()
    var autoAdvance = mutableMapOf<String, Boolean>()

    fun setSteps(activityId: String, steps: List<CookingStepUi>, autoAdvanceEnabled: Boolean) {
        stepsByActivity[activityId] = steps
        currentIndexByActivity[activityId] = 0
        autoAdvance[activityId] = autoAdvanceEnabled
    }

    fun stepsFor(activityId: String): List<CookingStepUi> = stepsByActivity[activityId] ?: emptyList()
    fun currentIndex(activityId: String): Int = currentIndexByActivity[activityId] ?: 0
    fun advance(activityId: String): Boolean {
        val steps = stepsFor(activityId)
        val next = currentIndex(activityId) + 1
        return if (next < steps.size) {
            currentIndexByActivity[activityId] = next
            true
        } else false
    }
    fun clear(activityId: String) {
        stepsByActivity.remove(activityId)
        currentIndexByActivity.remove(activityId)
        autoAdvance.remove(activityId)
    }
}

/** Advances a running cooking activity to its next step automatically when the current step's timer expires and auto-advance is on. */
@Singleton
class CookingStepAdvancer @Inject constructor(
    private val activityEngine: ActivityEngine,
    @ApplicationScope private val scope: CoroutineScope,
) {
    private val advancedThisExpiry = mutableSetOf<String>()

    fun start() {
        scope.launch {
            activityEngine.activities.collect { list ->
                list.filter { it.type == ActivityType.COOKING && it.state == ActivityState.ACTIVE }
                    .forEach { maybeAdvance(it) }
            }
        }
    }

    private fun maybeAdvance(state: ActivityUiState) {
        val timer = state.timer ?: return
        if (!timer.isExpired()) {
            advancedThisExpiry.remove(state.id.value)
            return
        }
        if (state.id.value in advancedThisExpiry) return
        advancedThisExpiry += state.id.value

        val autoAdvance = CookingStepStore.autoAdvance[state.id.value] ?: true
        if (!autoAdvance) return
        advanceToNextStep(state)
    }

    /** Called both by the auto-advance expiry check above and by the manual "Skip step" button. */
    fun advanceToNextStep(state: ActivityUiState) {
        val advanced = CookingStepStore.advance(state.id.value)
        if (!advanced) {
            activityEngine.complete(state.id)
            return
        }
        val steps = CookingStepStore.stepsFor(state.id.value)
        val index = CookingStepStore.currentIndex(state.id.value)
        val step = steps[index]
        activityEngine.update(state.id) {
            it.copy(
                subtitle = context(index, steps.size),
                title = step.name,
                timer = TimerSpec(durationMillis = step.durationMinutes * 60_000L),
            )
        }
    }

    private fun context(index: Int, total: Int) = "Step ${index + 1} of $total"
}

@HiltViewModel
class CookingCreateViewModel @Inject constructor(private val activityEngine: ActivityEngine) : ViewModel() {

    fun start(recipeName: String, steps: List<CookingStepUi>, autoAdvance: Boolean, onStarted: (String) -> Unit) {
        if (steps.isEmpty()) return
        val id = ActivityId.new()
        CookingStepStore.setSteps(id.value, steps, autoAdvance)
        val first = steps.first()
        val state = ActivityUiState(
            id = id,
            type = ActivityType.COOKING,
            title = first.name,
            subtitle = "Step 1 of ${steps.size}",
            icon = ActivityIconId.COOKING,
            state = ActivityState.ACTIVE,
            timer = TimerSpec(durationMillis = first.durationMinutes * 60_000L),
            actions = ActivityActions.countdownRunning(includeAddMinute = false) + ActivityAction("skip", R.string.cooking_skip_step, ActivityAction.Kind.CUSTOM),
        )
        activityEngine.upsert(state)
        onStarted(id.value)
    }
}

@Composable
fun CookingCreateScreen(onStarted: (String) -> Unit, onBack: () -> Unit) {
    val viewModel: CookingCreateViewModel = hiltViewModel()
    var recipeName by remember { mutableStateOf("") }
    var autoAdvance by remember { mutableStateOf(true) }
    val steps = remember { mutableStateListOf<CookingStepUi>() }
    var stepName by remember { mutableStateOf("") }
    var stepMinutes by remember { mutableStateOf("5") }

    Scaffold { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(ScreenPadding), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.cooking_new_recipe), style = MaterialTheme.typography.headlineMedium)
            OutlinedTextField(value = recipeName, onValueChange = { recipeName = it }, label = { Text("Recipe name") }, modifier = Modifier.fillMaxWidth(), singleLine = true)

            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.cooking_auto_advance), modifier = Modifier.weight(1f))
                Switch(checked = autoAdvance, onCheckedChange = { autoAdvance = it })
            }

            LazyColumn(modifier = Modifier.weight(1f, fill = false)) {
                items(steps.size) { index ->
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Text("${index + 1}. ${steps[index].name} (${steps[index].durationMinutes}m)", modifier = Modifier.weight(1f))
                        IconButton(onClick = { steps.removeAt(index) }) { Icon(Icons.Filled.Delete, contentDescription = null) }
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = stepName, onValueChange = { stepName = it }, label = { Text(stringResource(R.string.cooking_step_name_hint)) }, modifier = Modifier.weight(2f))
                OutlinedTextField(value = stepMinutes, onValueChange = { stepMinutes = it.filter(Char::isDigit) }, label = { Text(stringResource(R.string.cooking_step_duration_hint)) }, modifier = Modifier.weight(1f))
            }
            PrimaryButton(
                text = stringResource(R.string.cooking_add_step),
                onClick = {
                    val minutes = stepMinutes.toIntOrNull() ?: return@PrimaryButton
                    if (stepName.isBlank() || minutes <= 0) return@PrimaryButton
                    steps.add(CookingStepUi(stepName, minutes))
                    stepName = ""
                    stepMinutes = "5"
                },
            )

            PrimaryButton(
                text = stringResource(R.string.action_start),
                enabled = steps.isNotEmpty(),
                onClick = { viewModel.start(recipeName.ifBlank { "Recipe" }, steps.toList(), autoAdvance, onStarted) },
            )
        }
    }
}
