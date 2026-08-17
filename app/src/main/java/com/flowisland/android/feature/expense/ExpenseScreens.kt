package com.flowisland.android.feature.expense

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import com.flowisland.android.core.activity.ActivityActions
import com.flowisland.android.core.activity.ActivityEngine
import com.flowisland.android.core.activity.model.ActivityIconId
import com.flowisland.android.core.activity.model.ActivityId
import com.flowisland.android.core.activity.model.ActivityState
import com.flowisland.android.core.activity.model.ActivityType
import com.flowisland.android.core.activity.model.ActivityUiState
import com.flowisland.android.core.database.ExpenseDao
import com.flowisland.android.core.database.ExpenseEntity
import com.flowisland.android.core.database.ExpenseTripEntity
import com.flowisland.android.core.di.IoDispatcher
import com.flowisland.android.core.ui.components.PrimaryButton
import com.flowisland.android.core.ui.components.ScreenPadding
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Currency
import java.util.UUID
import javax.inject.Inject

fun formatCurrency(amount: Double, currencyCode: String): String = try {
    val symbol = Currency.getInstance(currencyCode).symbol
    "$symbol%.2f".format(amount)
} catch (e: IllegalArgumentException) {
    "%.2f %s".format(amount, currencyCode)
}

val expenseCategories = listOf(
    R.string.expense_category_food, R.string.expense_category_transport, R.string.expense_category_hotel,
    R.string.expense_category_shopping, R.string.expense_category_entertainment, R.string.expense_category_other,
)

@HiltViewModel
class ExpenseCreateViewModel @Inject constructor(
    private val activityEngine: ActivityEngine,
    private val expenseDao: ExpenseDao,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : ViewModel() {

    fun startTrip(name: String, currencyCode: String, onStarted: (String) -> Unit) {
        val tripId = UUID.randomUUID().toString()
        viewModelScope.launch {
            withContext(ioDispatcher) {
                expenseDao.insertTrip(ExpenseTripEntity(id = tripId, name = name.ifBlank { "Trip" }, currencyCode = currencyCode, createdAt = System.currentTimeMillis()))
            }
            val id = ActivityId.new()
            val state = ActivityUiState(
                id = id,
                type = ActivityType.EXPENSE,
                title = name.ifBlank { "Trip" },
                subtitle = formatCurrency(0.0, currencyCode),
                icon = ActivityIconId.EXPENSE,
                state = ActivityState.ACTIVE,
                actions = ActivityActions.simpleFinishCancel(),
                payloadId = tripId,
            )
            activityEngine.upsert(state)
            onStarted(id.value)
        }
    }
}

@Composable
fun ExpenseCreateScreen(onStarted: (String) -> Unit, onBack: () -> Unit) {
    val viewModel: ExpenseCreateViewModel = hiltViewModel()
    var name by remember { mutableStateOf("") }
    var currency by remember { mutableStateOf("INR") }

    Scaffold { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(ScreenPadding), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(stringResource(R.string.expense_new_trip), style = MaterialTheme.typography.headlineMedium)
            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Trip name") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("INR", "USD", "EUR", "GBP").forEach { code ->
                    FilterChip(selected = currency == code, onClick = { currency = code }, label = { Text(code) })
                }
            }
            PrimaryButton(text = stringResource(R.string.action_start), onClick = { viewModel.startTrip(name, currency, onStarted) })
        }
    }
}

@HiltViewModel
class AddExpenseViewModel @Inject constructor(
    private val activityEngine: ActivityEngine,
    private val expenseDao: ExpenseDao,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : ViewModel() {

    fun tripExpenses(tripId: String) = expenseDao.observeTrip(tripId)

    fun addExpense(activityId: String, tripId: String, currencyCode: String, amount: Double, category: String, note: String?) {
        viewModelScope.launch {
            withContext(ioDispatcher) {
                expenseDao.insertExpense(
                    ExpenseEntity(id = UUID.randomUUID().toString(), tripId = tripId, amount = amount, category = category, note = note, date = System.currentTimeMillis())
                )
            }
            // Update the running total shown on the island/notification immediately using the just-added amount,
            // rather than waiting for the next Flow emission -- keeps the UI feeling instant.
            activityEngine.update(ActivityId(activityId)) { current ->
                val currentTotal = current.subtitle?.let { parseTrailingAmount(it) } ?: 0.0
                current.copy(subtitle = formatCurrency(currentTotal + amount, currencyCode))
            }
        }
    }

    private fun parseTrailingAmount(text: String): Double =
        text.filter { it.isDigit() || it == '.' }.toDoubleOrNull() ?: 0.0
}

@Composable
fun AddExpenseScreen(activityId: String, tripId: String, currencyCode: String, onBack: () -> Unit) {
    val viewModel: AddExpenseViewModel = hiltViewModel()
    val tripWithExpenses by viewModel.tripExpenses(tripId).collectAsState(initial = null)
    var amount by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(R.string.expense_category_food) }
    var note by remember { mutableStateOf("") }

    Scaffold { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(ScreenPadding), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.expense_add), style = MaterialTheme.typography.headlineMedium)

            LazyColumn(modifier = Modifier.weight(1f, fill = false)) {
                tripWithExpenses?.expenses?.let { expenses ->
                    items(expenses.size) { index ->
                        val expense = expenses[index]
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(expense.category, style = MaterialTheme.typography.bodyMedium)
                            Text(formatCurrency(expense.amount, currencyCode), style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
            HorizontalDivider()
            val total = tripWithExpenses?.expenses?.sumOf { it.amount } ?: 0.0
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(stringResource(R.string.expense_total), style = MaterialTheme.typography.titleMedium)
                Text(formatCurrency(total, currencyCode), style = MaterialTheme.typography.titleMedium)
            }

            OutlinedTextField(value = amount, onValueChange = { amount = it.filter { c -> c.isDigit() || c == '.' } }, label = { Text(stringResource(R.string.expense_amount_hint)) }, modifier = Modifier.fillMaxWidth())
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                expenseCategories.forEach { catRes ->
                    FilterChip(selected = category == catRes, onClick = { category = catRes }, label = { Text(stringResource(catRes)) })
                }
            }
            OutlinedTextField(value = note, onValueChange = { note = it }, label = { Text(stringResource(R.string.expense_note_hint)) }, modifier = Modifier.fillMaxWidth())

            PrimaryButton(
                text = stringResource(R.string.expense_add),
                onClick = {
                    val amountValue = amount.toDoubleOrNull() ?: return@PrimaryButton
                    viewModel.addExpense(activityId, tripId, currencyCode, amountValue, categoryLabel(category), note.ifBlank { null })
                    amount = ""
                    note = ""
                },
            )
        }
    }
}

private fun categoryLabel(resId: Int): String = when (resId) {
    R.string.expense_category_food -> "Food"
    R.string.expense_category_transport -> "Transport"
    R.string.expense_category_hotel -> "Hotel"
    R.string.expense_category_shopping -> "Shopping"
    R.string.expense_category_entertainment -> "Entertainment"
    else -> "Other"
}
