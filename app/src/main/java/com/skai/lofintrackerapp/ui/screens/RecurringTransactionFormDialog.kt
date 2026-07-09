// In ...ui.screens/RecurringTransactionFormDialog.kt
package com.skai.lofintrackerapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.skai.lofintrackerapp.data.db.*
import com.skai.lofintrackerapp.data.DEFAULT_EXPENSE_CATEGORIES
import com.skai.lofintrackerapp.data.DEFAULT_INCOME_CATEGORIES
import com.skai.lofintrackerapp.data.DEFAULT_PAYMENT_MODES
import com.skai.lofintrackerapp.ui.common.FormDropdown
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.Instant
import java.time.ZoneId

// --- DATE HELPER FUNCTIONS ---
private val dbFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
private val displayFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy")
private val dateRegex = Regex("^\\d{2}-\\d{2}-\\d{4}$")

private fun getNextMonthDateMillis(): Long {
    val nextMonth = LocalDate.now().plusMonths(1)
    return nextMonth.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
}

private fun Long.toDisplayDateString(): String {
    return Instant.ofEpochMilli(this).atZone(ZoneId.of("UTC")).toLocalDate().format(displayFormatter)
}

private fun String.toDisplayDateFromDb(): String {
    return try {
        LocalDate.parse(this, dbFormatter).format(displayFormatter)
    } catch (e: Exception) { this }
}

private fun validateDate(dateStr: String): Boolean {
    if (!dateStr.matches(dateRegex)) return false
    return try {
        LocalDate.parse(dateStr, displayFormatter)
        true
    } catch (e: DateTimeParseException) {
        false
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecurringTransactionFormDialog(
    itemToEdit: ScheduledTransaction? = null,
    accounts: List<Account>,
    loans: List<Loan>,
    creditCards: List<CreditCard>,
    onDismiss: () -> Unit,
    onConfirm: (ScheduledTransaction) -> Unit
) {
    var type by remember { mutableStateOf(itemToEdit?.type ?: TransactionType.EXPENSE) }
    var amount by remember { mutableStateOf(itemToEdit?.amount?.toString() ?: "") }
    var description by remember { mutableStateOf(itemToEdit?.description ?: "") }
    var category by remember { mutableStateOf(itemToEdit?.category ?: "") }
    
    // Funding Source
    var selectedAccountId by remember { mutableStateOf<Long?>(itemToEdit?.accountId) }
    var selectedCreditCardId by remember {
        mutableStateOf(if (itemToEdit?.category == "Credit Card Payment") null else itemToEdit?.creditCardId)
    }
    
    // Target (Loan/Credit Card being paid)
    var selectedTargetId by remember {
        mutableStateOf(if (itemToEdit?.category == "Credit Card Payment") itemToEdit.creditCardId else itemToEdit?.loanId)
    }
    
    var paymentMode by remember { mutableStateOf(itemToEdit?.paymentMode ?: "") }
    var frequency by remember { mutableStateOf(itemToEdit?.frequency ?: "Monthly") }

    var dateText by remember {
        mutableStateOf(itemToEdit?.nextDueDate?.toDisplayDateFromDb() ?: getNextMonthDateMillis().toDisplayDateString())
    }
    var isDateError by remember { mutableStateOf(false) }
    var isAmountError by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }

    val categories = if (type == TransactionType.INCOME) DEFAULT_INCOME_CATEGORIES else DEFAULT_EXPENSE_CATEGORIES
    val frequencies = listOf("Daily", "Weekly", "Monthly", "Yearly")
    val scrollState = rememberScrollState()

    // Reset logic for category when type changes
    LaunchedEffect(type) {
        if (category.isNotEmpty() && category !in categories) {
            category = ""
        }
    }

    if (showDatePicker) {
        val initialMillis = try {
            LocalDate.parse(dateText, displayFormatter).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        } catch (e: Exception) {
            getNextMonthDateMillis()
        }

        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val selectedMillis = datePickerState.selectedDateMillis ?: initialMillis
                        dateText = selectedMillis.toDisplayDateString()
                        isDateError = false
                        showDatePicker = false
                    }
                ) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(if(itemToEdit == null) "New Scheduled Payment" else "Edit Scheduled Payment", style = MaterialTheme.typography.titleLarge)

                TabRow(selectedTabIndex = type.ordinal) {
                    Tab(
                        selected = type == TransactionType.EXPENSE,
                        onClick = { type = TransactionType.EXPENSE },
                        text = { Text("Expense") }
                    )
                    Tab(
                        selected = type == TransactionType.INCOME,
                        onClick = { type = TransactionType.INCOME },
                        text = { Text("Income") }
                    )
                }

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = dateText,
                    onValueChange = {
                        dateText = it
                        isDateError = !validateDate(it)
                    },
                    label = { Text("Next Due Date") },
                    placeholder = { Text("DD-MM-YYYY") },
                    isError = isDateError,
                    trailingIcon = {
                        IconButton(onClick = { showDatePicker = true }) {
                            Icon(Icons.Default.DateRange, contentDescription = "Open Calendar")
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = amount,
                    onValueChange = {
                        amount = it
                        isAmountError = (it.toDoubleOrNull() ?: 0.0) <= 0.0
                    },
                    label = { Text("Amount") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    isError = isAmountError,
                    supportingText = { if (isAmountError) Text("Amount must be greater than zero.") }
                )

                FormDropdown(
                    label = "Category",
                    options = categories,
                    onOptionSelected = { 
                        category = categories[it]
                        if (category == "Credit Card Payment" || category == "Loan Repayment") {
                            selectedTargetId = null
                            if (category == "Credit Card Payment") selectedCreditCardId = null
                        }
                    },
                    selectedTextValue = category
                )

                // Funding Source selection (Request 4: Credit Cards as default method)
                val fundingSources = accounts.map { "Account: ${it.name}" } + creditCards.map { "Card: ${it.name}" }
                val selectedSourceText = when {
                    selectedAccountId != null -> "Account: ${accounts.find { it.id == selectedAccountId }?.name ?: ""}"
                    selectedCreditCardId != null -> "Card: ${creditCards.find { it.id == selectedCreditCardId }?.name ?: ""}"
                    else -> ""
                }
                FormDropdown(
                    label = "Pay From / To",
                    options = fundingSources,
                    onOptionSelected = { index ->
                        if (index < accounts.size) {
                            selectedAccountId = accounts[index].id
                            selectedCreditCardId = null
                            paymentMode = "" // Reset or set default
                        } else {
                            selectedAccountId = null
                            selectedCreditCardId = creditCards[index - accounts.size].id
                            paymentMode = "Credit Card"
                        }
                    },
                    selectedTextValue = selectedSourceText
                )

                // If Credit Card Payment is selected, show card to pay and auto-set date (Request 3)
                if (category == "Credit Card Payment" && type == TransactionType.EXPENSE) {
                    FormDropdown(
                        label = "Select Card to Pay",
                        options = creditCards.map { it.name },
                        onOptionSelected = { index ->
                            val card = creditCards[index]
                            selectedTargetId = card.id
                            // Auto-set Next Due Date based on Card (Request 3)
                            val today = LocalDate.now()
                            var targetDate = today.withDayOfMonth(1).plusMonths(1)
                            try {
                                targetDate = targetDate.withDayOfMonth(card.dueDate)
                            } catch (e: Exception) {
                                // Request 6: Handle short months (e.g. 31st -> 1st of next month)
                                targetDate = targetDate.plusMonths(1).withDayOfMonth(1)
                            }
                            dateText = targetDate.format(displayFormatter)
                        },
                        selectedTextValue = creditCards.find { it.id == selectedTargetId }?.name ?: ""
                    )
                }

                if (category == "Loan Repayment" && type == TransactionType.EXPENSE) {
                    FormDropdown(
                        label = "Select Loan",
                        options = loans.map { "${it.name} (${it.lender})" },
                        onOptionSelected = { selectedTargetId = loans[it].id },
                        selectedTextValue = loans.find { it.id == selectedTargetId }?.name ?: ""
                    )
                }

                FormDropdown(
                    label = "Payment Mode",
                    options = DEFAULT_PAYMENT_MODES,
                    onOptionSelected = { paymentMode = DEFAULT_PAYMENT_MODES[it] },
                    selectedTextValue = paymentMode
                )

                FormDropdown(
                    label = "Frequency",
                    options = frequencies,
                    onOptionSelected = { frequency = frequencies[it] },
                    selectedTextValue = frequency
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Button(
                        onClick = {
                            val dbDate = try {
                                LocalDate.parse(dateText, displayFormatter).format(dbFormatter)
                            } catch (e: Exception) {
                                LocalDate.now().plusMonths(1).format(dbFormatter)
                            }

                            val newItem = ScheduledTransaction(
                                id = itemToEdit?.id ?: 0L,
                                type = type,
                                amount = amount.toDoubleOrNull() ?: 0.0,
                                category = category,
                                accountId = selectedAccountId,
                                creditCardId = if (category == "Credit Card Payment") selectedTargetId else selectedCreditCardId,
                                loanId = if (category == "Loan Repayment") selectedTargetId else null,
                                paymentMode = paymentMode,
                                description = description,
                                frequency = frequency,
                                nextDueDate = dbDate
                            )
                            onConfirm(newItem)
                            onDismiss()
                        },
                        enabled = description.isNotBlank() &&
                                amount.toDoubleOrNull()?.let { it > 0.0 } == true &&
                                (selectedAccountId != null || selectedCreditCardId != null || (category == "Credit Card Payment" && selectedTargetId != null)) &&
                                !isDateError
                    ) { Text("Save") }
                }
            }
        }
    }
}
