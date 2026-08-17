package com.flowisland.android.feature.delivery

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flowisland.android.R
import com.flowisland.android.core.activity.ActivityEngine
import com.flowisland.android.core.activity.model.ActivityAction
import com.flowisland.android.core.activity.model.ActivityIconId
import com.flowisland.android.core.activity.model.ActivityId
import com.flowisland.android.core.activity.model.ActivityState
import com.flowisland.android.core.activity.model.ActivityType
import com.flowisland.android.core.activity.model.ActivityUiState
import com.flowisland.android.core.database.DeliveryDao
import com.flowisland.android.core.database.DeliveryEntity
import com.flowisland.android.core.di.IoDispatcher
import com.flowisland.android.core.ui.components.PrimaryButton
import com.flowisland.android.core.ui.components.ScreenPadding
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

enum class DeliveryStatus(val labelResId: Int) {
    ORDERED(R.string.delivery_status_ordered),
    SHIPPED(R.string.delivery_status_shipped),
    OUT_FOR_DELIVERY(R.string.delivery_status_out_for_delivery),
    DELIVERED(R.string.delivery_status_delivered);

    fun next(): DeliveryStatus? = entries.getOrNull(ordinal + 1)
}

@HiltViewModel
class DeliveryCreateViewModel @Inject constructor(
    private val activityEngine: ActivityEngine,
    private val deliveryDao: DeliveryDao,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : ViewModel() {

    fun start(name: String, orderNumber: String, onStarted: (String) -> Unit) {
        if (name.isBlank()) return
        val id = ActivityId.new()
        viewModelScope.launch {
            withContext(ioDispatcher) {
                deliveryDao.insert(
                    DeliveryEntity(id = id.value, name = name, orderNumber = orderNumber.ifBlank { null }, etaMillis = null, status = DeliveryStatus.ORDERED.name, createdAt = System.currentTimeMillis(), updatedAt = System.currentTimeMillis())
                )
            }
            val state = ActivityUiState(
                id = id,
                type = ActivityType.DELIVERY,
                title = name,
                subtitle = "Ordered",
                icon = ActivityIconId.DELIVERY,
                state = ActivityState.ACTIVE,
                actions = listOf(ActivityAction("advance", R.string.delivery_update_status, ActivityAction.Kind.CUSTOM)),
                payloadId = id.value,
            )
            activityEngine.upsert(state)
            onStarted(id.value)
        }
    }
}

@Composable
fun DeliveryCreateScreen(onStarted: (String) -> Unit, onBack: () -> Unit) {
    val viewModel: DeliveryCreateViewModel = hiltViewModel()
    var name by remember { mutableStateOf("") }
    var orderNumber by remember { mutableStateOf("") }

    Scaffold { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(ScreenPadding), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(stringResource(R.string.delivery_new), style = MaterialTheme.typography.headlineMedium)
            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text(stringResource(R.string.delivery_name_hint)) }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = orderNumber, onValueChange = { orderNumber = it }, label = { Text(stringResource(R.string.delivery_order_number_hint)) }, modifier = Modifier.fillMaxWidth())
            PrimaryButton(text = stringResource(R.string.action_start), onClick = { viewModel.start(name, orderNumber, onStarted) })
        }
    }
}
