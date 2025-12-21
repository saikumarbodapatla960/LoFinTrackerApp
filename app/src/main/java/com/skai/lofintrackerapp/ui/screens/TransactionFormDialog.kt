// In ...ui.screens/TransactionFormDialog.kt
package com.skai.lofintrackerapp.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.skai.lofintrackerapp.data.db.Account
import com.skai.lofintrackerapp.data.db.AccountType
import com.skai.lofintrackerapp.data.db.CreditCard
import com.skai.lofintrackerapp.data.db.Loan
import com.skai.lofintrackerapp.data.db.Transaction
import com.skai.lofintrackerapp.data.db.TransactionType
import com.skai.lofintrackerapp.data.DEFAULT_EXPENSE_CATEGORIES
import com.skai.lofintrackerapp.data.DEFAULT_INCOME_CATEGORIES
import com.skai.lofintrackerapp.data.DEFAULT_PAYMENT_MODES
import com.skai.lofintrackerapp.ui.common.ConfirmationDialog
import com.skai.lofintrackerapp.ui.common.FormDropdown
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.launch

// --- DATE HELPER FUNCTIONS ---
private val dbFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
private val displayFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy")
private val dateRegex = Regex("^\\d{2}-\\d{2}-\\d{4}$")

private fun getTodayDateMillis(): Long = LocalDate.now().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

private fun Long.toDisplayDateString(): String {
    return Instant.ofEpochMilli(this).atZone(ZoneId.of("UTC")).toLocalDate().format(displayFormatter)
}

private fun Long.toDbDateString(): String {
    return Instant.ofEpochMilli(this).atZone(ZoneId.of("UTC")).toLocalDate().format(dbFormatter)
}

private fun String.toDisplayDateFromDb(): String {
    return try {
        LocalDate.parse(this, dbFormatter).format(displayFormatter)
    } catch (e: Exception) {
        this
    }
}

private fun validateDate(dateStr: String): Boolean {
    if (!dateStr.matches(dateRegex)) return false
    return try {
        val parsedDate = LocalDate.parse(dateStr, displayFormatter)
        !parsedDate.isAfter(LocalDate.now())
    } catch (e: DateTimeParseException) {
        false
    }
}

// --- DATA CLASS ---
private data class FundingSource(
    val id: String,
    val name: String,
    val type: String, // "ACCOUNT" or "CARD"
    val accountType: AccountType? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionFormDialog(
    transactionToEdit: Transaction? = null,
    accounts: List<Account>,
    loans: List<Loan>,
    creditCards: List<CreditCard>,
    onDismiss: () -> Unit,
    onConfirm: suspend (Transaction) -> String?,
    onDelete: suspend (Transaction) -> Unit = {}
) {
    // --- STATE ---
    var type by remember { mutableStateOf(transactionToEdit?.type ?: TransactionType.EXPENSE) }
    var amount by remember { mutableStateOf(transactionToEdit?.amount?.toString() ?: "") }
    var dateText by remember { mutableStateOf(transactionToEdit?.date?.toDisplayDateFromDb() ?: getTodayDateMillis().toDisplayDateString()) }
    var description by remember { mutableStateOf(transactionToEdit?.description ?: "") }
    var category by remember { mutableStateOf(transactionToEdit?.category ?: "") }
    var paymentMode by remember { mutableStateOf(transactionToEdit?.paymentMode ?: "") }

    var selectedFundingSource by remember { mutableStateOf<FundingSource?>(null) }
    var paymentModes by remember { mutableStateOf<List<String>>(emptyList()) }

    var selectedLoanId by remember { mutableStateOf(transactionToEdit?.loanId) }

    var selectedCardToPayId by remember {
        mutableStateOf(if (transactionToEdit?.category == "Credit Card Payment") transactionToEdit.loanId else null)
    }

    var isAmountError by remember { mutableStateOf(false) }
    var isDateError by remember { mutableStateOf(false) }
    var saveInProgress by remember { mutableStateOf(false) }
    var transactionError by remember { mutableStateOf<String?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    // --- PREPARE ALL SOURCES ---
    val allFundingSources = remember(accounts, creditCards) {
        val accountSources = accounts.map {
            FundingSource(id = "account-${it.id}", name = it.name, type = "ACCOUNT", accountType = it.type)
        }
        val cardSources = if (creditCards.isNotEmpty()) {
            creditCards.map {
                FundingSource(id = "card-${it.id}", name = it.name, type = "CARD")
            }
        } else {
            emptyList()
        }
        accountSources + cardSources
    }

    // --- FILTER VALID SOURCES ---
    // This ensures the dropdown ONLY shows valid options
    val currentFundingOptions = remember(allFundingSources, type, category) {
        if (type == TransactionType.INCOME) {
            // RULE: Income can only go to Accounts
            allFundingSources.filter { it.type == "ACCOUNT" }
        } else {
            // Expense
            if (category == "Loan Repayment" || category == "Credit Card Payment") {
                // RULE: Debt payments must come from Accounts
                allFundingSources.filter { it.type == "ACCOUNT" }
            } else {
                // Normal Expense: Can use Account OR Card
                allFundingSources
            }
        }
    }

    // --- SET INITIAL SELECTION ---
    LaunchedEffect(Unit) {
        if (transactionToEdit != null) {
            if (transactionToEdit.creditCardId != null) {
                selectedFundingSource = allFundingSources.find { it.id == "card-${transactionToEdit.creditCardId}" }
            } else {
                selectedFundingSource = allFundingSources.find { it.id == "account-${transactionToEdit.accountId}" }
            }
        }
    }

    val categories = if (type == TransactionType.INCOME) DEFAULT_INCOME_CATEGORIES else DEFAULT_EXPENSE_CATEGORIES

    // --- RESET LOGIC ---
    // This is the fix: We must clear the selection if it becomes invalid for the new Type/Category
    LaunchedEffect(currentFundingOptions) {
        if (selectedFundingSource != null) {
            // If the currently selected item is NOT in the valid list anymore, clear it
            val isValid = currentFundingOptions.any { it.id == selectedFundingSource?.id }
            if (!isValid) {
                selectedFundingSource = null
                paymentModes = emptyList()
                paymentMode = ""
            }
        }
    }

    // --- PAYMENT MODE LOGIC ---
    LaunchedEffect(selectedFundingSource) {
        val source = selectedFundingSource
        if (source == null) {
            if(transactionToEdit == null) {
                paymentModes = emptyList()
                paymentMode = ""
            }
            return@LaunchedEffect
        }

        when (source.type) {
            "ACCOUNT" -> {
                if (source.accountType == AccountType.CASH) {
                    paymentModes = listOf("Cash")
                    if (transactionToEdit == null || transactionToEdit.paymentMode != "Cash") paymentMode = "Cash"
                } else {
                    paymentModes = DEFAULT_PAYMENT_MODES.filter { it != "Credit Card" && it != "Cash" }
                    if (transactionToEdit == null && paymentMode == "Cash") paymentMode = ""
                }
            }
            "CARD" -> {
                paymentModes = listOf("Credit Card")
                paymentMode = "Credit Card"
            }
        }
    }

    // --- HELPER: Hard Reset on Type Change ---
    fun onTypeChanged(newType: TransactionType) {
        if (type != newType) {
            type = newType
            category = ""
            paymentMode = ""
            selectedFundingSource = null // Explicitly clear the account/card
            selectedLoanId = null
            selectedCardToPayId = null
            transactionError = null
        }
    }

    // --- DATE PICKER DIALOG ---
    if (showDatePicker) {
        val initialMillis = try {
            LocalDate.parse(dateText, displayFormatter).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        } catch (e: Exception) {
            getTodayDateMillis()
        }

        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = initialMillis,
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                    return utcTimeMillis <= getTodayDateMillis()
                }
            }
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val selectedMillis = datePickerState.selectedDateMillis ?: getTodayDateMillis()
                        dateText = selectedMillis.toDisplayDateString()
                        isDateError = false
                        showDatePicker = false
                    }
                ) {
                    Text("OK")
                }
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
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                ) {
                    Text(
                        text = if(transactionToEdit != null) "Edit Transaction" else "New Transaction",
                        style = MaterialTheme.typography.titleLarge
                    )
                    if (transactionToEdit != null) {
                        IconButton(onClick = { showDeleteConfirmation = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red)
                        }
                    }
                }

                if (transactionError != null) {
                    Text(text = transactionError!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                }

                // Tabs (With RESET logic)
                TabRow(selectedTabIndex = type.ordinal) {
                    Tab(
                        selected = type == TransactionType.EXPENSE,
                        onClick = { onTypeChanged(TransactionType.EXPENSE) },
                        text = { Text("Expense") }
                    )
                    Tab(
                        selected = type == TransactionType.INCOME,
                        onClick = { onTypeChanged(TransactionType.INCOME) },
                        text = { Text("Income") }
                    )
                }

                // Date Field
                OutlinedTextField(
                    value = dateText,
                    onValueChange = {
                        dateText = it
                        isDateError = !validateDate(it)
                    },
                    label = { Text("Date") },
                    placeholder = { Text("DD-MM-YYYY") },
                    isError = isDateError,
                    supportingText = { if (isDateError) Text("Invalid date (DD-MM-YYYY)") },
                    trailingIcon = {
                        IconButton(onClick = { showDatePicker = true }) {
                            Icon(Icons.Default.DateRange, contentDescription = "Open Calendar")
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                // Amount Field
                OutlinedTextField(
                    value = amount,
                    onValueChange = {
                        amount = it
                        isAmountError = (it.toDoubleOrNull() ?: 0.0) <= 0.0
                    },
                    label = { Text("Amount") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    isError = isAmountError
                )

                // --- CATEGORY ---
                FormDropdown(
                    label = "Category",
                    options = categories,
                    onOptionSelected = { index -> category = categories[index] },
                    selectedTextValue = category
                )

                // --- ACCOUNT/CARD DROPDOWN ---
                FormDropdown(
                    label = if (type == TransactionType.INCOME) "To Account" else "From",
                    options = currentFundingOptions.map { it.name }, // Use the filtered list
                    onOptionSelected = { index ->
                        selectedFundingSource = currentFundingOptions[index]
                    },
                    selectedTextValue = selectedFundingSource?.name ?: ""
                )

                // Payment Mode
                if (type == TransactionType.EXPENSE) {
                    FormDropdown(
                        label = "Payment Mode",
                        options = paymentModes,
                        onOptionSelected = { index ->
                            paymentMode = paymentModes[index]
                        },
                        selectedTextValue = paymentMode
                    )
                }

                // Conditional Logic
                if (category == "Loan Repayment" && type == TransactionType.EXPENSE) {
                    FormDropdown(
                        label = "Select Loan",
                        options = loans.map { "${it.name} (${it.lender})" },
                        onOptionSelected = { index -> selectedLoanId = loans[index].id },
                        selectedTextValue = loans.find { it.id == selectedLoanId }?.name ?: ""
                    )
                }

                // --- LOGIC FOR CREDIT CARD PAYMENT ---
                if (category == "Credit Card Payment" && type == TransactionType.EXPENSE) {
                    if (creditCards.isEmpty()) {
                        // Show warning if no cards exist
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                        ) {
                            Text(
                                text = "No Credit Cards found. Please add a Credit Card first in the Credit Cards screen.",
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.padding(16.dp),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    } else {
                        FormDropdown(
                            label = "Select Card to Pay",
                            options = creditCards.map { it.name },
                            onOptionSelected = { index -> selectedCardToPayId = creditCards[index].id },
                            selectedTextValue = creditCards.find { it.id == selectedCardToPayId }?.name ?: ""
                        )
                    }
                }

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (Optional)") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }

                    val isSaveEnabled = !isDateError && !isAmountError && amount.isNotBlank() &&
                            selectedFundingSource != null && category.isNotBlank() &&
                            (type == TransactionType.INCOME || paymentMode.isNotBlank()) &&
                            (category == "Loan Repayment" && selectedLoanId != null || category != "Loan Repayment") &&
                            (category == "Credit Card Payment" && selectedCardToPayId != null || category != "Credit Card Payment")

                    Button(
                        onClick = {
                            scope.launch {
                                saveInProgress = true
                                val dbDate = try {
                                    LocalDate.parse(dateText, displayFormatter).format(dbFormatter)
                                } catch (e: Exception) {
                                    getTodayDateMillis().toDbDateString()
                                }

                                val source = selectedFundingSource!!

                                val newTransaction = Transaction(
                                    id = transactionToEdit?.id ?: 0L,
                                    type = type,
                                    amount = amount.toDoubleOrNull() ?: 0.0,
                                    category = category,
                                    accountId = if (source.type == "ACCOUNT") source.id.removePrefix("account-").toLong() else 0L,
                                    creditCardId = if (source.type == "CARD") source.id.removePrefix("card-").toLong() else null,
                                    paymentMode = if (type == TransactionType.EXPENSE) paymentMode else null,
                                    loanId = if (category == "Loan Repayment") selectedLoanId else if (category == "Credit Card Payment") selectedCardToPayId else null,
                                    date = dbDate,
                                    description = description
                                )

                                val error = onConfirm(newTransaction)
                                if (error == null) {
                                    onDismiss()
                                } else {
                                    transactionError = error
                                    saveInProgress = false
                                }
                            }
                        },
                        enabled = isSaveEnabled && !saveInProgress
                    ) {
                        if (saveInProgress) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        } else {
                            Text(if(transactionToEdit != null) "Update" else "Save")
                        }
                    }
                }
            }
        }
    }

    if (showDeleteConfirmation && transactionToEdit != null) {
        ConfirmationDialog(
            title = "Delete Transaction",
            message = "Are you sure you want to delete this transaction?",
            icon = Icons.Default.Delete,
            confirmText = "Delete",
            onDismiss = { showDeleteConfirmation = false },
            onConfirm = {
                scope.launch {
                    onDelete(transactionToEdit)
                    showDeleteConfirmation = false
                    onDismiss()
                }
            }
        )
    }
}