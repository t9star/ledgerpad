package jp.tpp.t9s.ledgerpad.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import jp.tpp.t9s.ledgerpad.AppContainer
import jp.tpp.t9s.ledgerpad.R
import jp.tpp.t9s.ledgerpad.data.CashType
import jp.tpp.t9s.ledgerpad.data.TxnType
import jp.tpp.t9s.ledgerpad.ui.components.NumericKeypad
import jp.tpp.t9s.ledgerpad.ui.theme.GreenCredit
import jp.tpp.t9s.ledgerpad.ui.theme.RedDebit
import jp.tpp.t9s.ledgerpad.util.Calc
import jp.tpp.t9s.ledgerpad.util.Dates
import jp.tpp.t9s.ledgerpad.util.Money
import kotlinx.coroutines.launch

sealed class EntryMode {
    data class CustomerTxn(val customerId: Long, val type: TxnType) : EntryMode()
    data class Cashbook(val type: CashType) : EntryMode()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AmountEntryScreen(
    mode: EntryMode,
    container: AppContainer,
    onDone: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currency by container.prefs.currency.collectAsState()
    val scope = rememberCoroutineScope()

    var expression by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var selectedDateMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showEditChipsDialog by remember { mutableStateOf(false) }

    val isExpenseOrGave = when (mode) {
        is EntryMode.CustomerTxn -> mode.type == TxnType.GAVE
        is EntryMode.Cashbook -> mode.type == CashType.OUT
    }

    val actionColor = if (isExpenseOrGave) RedDebit else GreenCredit
    val titleText = when (mode) {
        is EntryMode.CustomerTxn -> if (mode.type == TxnType.GAVE) stringResource(R.string.title_gave_credit) else stringResource(R.string.title_got_payment)
        is EntryMode.Cashbook -> if (mode.type == CashType.IN) stringResource(R.string.title_cash_in_sale) else stringResource(R.string.title_cash_out_expense)
    }

    val currencyInfo = remember(currency) { Money.getInfo(currency) }

    // Evaluated amount
    val evaluatedAmount = remember(expression) {
        if (expression.isBlank()) 0.0 else (Calc.evaluate(expression) ?: 0.0)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(titleText, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = actionColor,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Amount Display Banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(actionColor.copy(alpha = 0.1f))
                    .padding(vertical = 18.dp, horizontal = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = currencyInfo.symbol + " " + if (expression.isEmpty()) "0" else expression,
                        fontSize = 34.sp,
                        fontWeight = FontWeight.Bold,
                        color = actionColor,
                        textAlign = TextAlign.Center
                    )
                    if (expression.contains("+") || expression.contains("-") || expression.contains("×") || expression.contains("÷")) {
                        Text(
                            text = "= ${currencyInfo.symbol}${String.format("%.2f", evaluatedAmount)}",
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Note field & Date button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    placeholder = { Text(stringResource(R.string.note_item_details_hint)) },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                Spacer(Modifier.width(8.dp))
                OutlinedButton(onClick = { showDatePicker = true }) {
                    Icon(Icons.Default.CalendarMonth, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text(Dates.formatShortDate(selectedDateMillis))
                }
            }

            Spacer(Modifier.weight(1f))

            // Quick Amount Chips
            val customQuickAmountsStr by container.prefs.customQuickAmounts.collectAsState()
            val quickChips = remember(currency, customQuickAmountsStr) {
                container.prefs.getQuickAmounts(currency)
            }

            androidx.compose.foundation.lazy.LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                items(quickChips.size) { index ->
                    val chipVal = quickChips[index]
                    androidx.compose.material3.AssistChip(
                        onClick = {
                            if (expression.isBlank() || expression == "0") {
                                expression = chipVal.toString()
                            } else {
                                // Add as + amount or replace
                                expression = if (expression.endsWith("+") || expression.endsWith("-") ||
                                    expression.endsWith("×") || expression.endsWith("÷")
                                ) {
                                    expression + chipVal
                                } else {
                                    "$expression+$chipVal"
                                }
                            }
                        },
                        label = {
                            Text(
                                text = "+$chipVal",
                                fontWeight = FontWeight.SemiBold,
                                color = actionColor
                            )
                        }
                    )
                }
                item {
                    IconButton(
                        onClick = { showEditChipsDialog = true },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = stringResource(R.string.edit_quick_amounts),
                            tint = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }

            // Calculator Keypad
            NumericKeypad(
                onDigitOrOp = { key ->
                    if (expression.length < 25) {
                        expression += key
                    }
                },
                onClear = { expression = "" },
                onBackspace = {
                    if (expression.isNotEmpty()) {
                        expression = expression.dropLast(1)
                    }
                }
            )

            // Submit Button
            Button(
                onClick = {
                    val finalAmount = evaluatedAmount
                    if (finalAmount > 0.0) {
                        val minor = (finalAmount * 100).toLong()
                        scope.launch {
                            when (mode) {
                                is EntryMode.CustomerTxn -> {
                                    container.repository.addTxn(
                                        customerId = mode.customerId,
                                        type = mode.type,
                                        amountMinor = minor,
                                        note = note,
                                        occurredAt = selectedDateMillis
                                    )
                                }
                                is EntryMode.Cashbook -> {
                                    container.repository.addCash(
                                        type = mode.type,
                                        amountMinor = minor,
                                        note = note,
                                        occurredAt = selectedDateMillis
                                    )
                                }
                            }
                            onDone()
                        }
                    }
                },
                enabled = evaluatedAmount > 0.0,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .height(54.dp),
                colors = ButtonDefaults.buttonColors(containerColor = actionColor),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(
                    text = stringResource(R.string.save_entry),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = selectedDateMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    selectedDateMillis = datePickerState.selectedDateMillis ?: selectedDateMillis
                    showDatePicker = false
                }) {
                    Text(stringResource(R.string.save))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // Edit Quick Amounts Dialog
    if (showEditChipsDialog) {
        val currentList = container.prefs.getQuickAmounts(currency)
        var textInput by remember { mutableStateOf(currentList.joinToString(", ")) }

        AlertDialog(
            onDismissRequest = { showEditChipsDialog = false },
            title = { Text(stringResource(R.string.edit_quick_amounts)) },
            text = {
                Column {
                    Text(
                        text = stringResource(R.string.edit_quick_amounts_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = textInput,
                        onValueChange = { textInput = it },
                        placeholder = { Text(stringResource(R.string.quick_amounts_hint)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    val parsed = textInput.split(",")
                        .mapNotNull { it.trim().toLongOrNull() }
                        .filter { it > 0 }
                    if (parsed.isNotEmpty()) {
                        container.prefs.setQuickAmounts(parsed)
                    }
                    showEditChipsDialog = false
                }) {
                    Text(stringResource(R.string.save))
                }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = {
                        container.prefs.resetQuickAmounts()
                        showEditChipsDialog = false
                    }) {
                        Text(stringResource(R.string.reset_default))
                    }
                    TextButton(onClick = { showEditChipsDialog = false }) {
                        Text(stringResource(R.string.cancel))
                    }
                }
            }
        )
    }
}
