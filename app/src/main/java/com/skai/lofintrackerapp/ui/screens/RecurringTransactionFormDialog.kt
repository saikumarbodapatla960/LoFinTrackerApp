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
import com.skai.lofintrackerapp.data.db.Account
import com.skai.lofintrackerapp.data.db.ScheduledTransaction
import com.skai.lofintrackerapp.data.db.TransactionType
import com.skai.lofintrackerapp.data.DEFAULT_EXPENSE_CATEGORIES
import com.skai.lofintrackerapp.data.DEFAULT_INCOME_CATEGORIES
import com.skai.lofintrackerapp.ui.common.FormDropdown
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.Instant
import java.time.ZoneId

// --- DATE HELPER FUNCTIONS (Same as TransactionForm) ---
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
    } catch (e: Exception) { this }
}

private fun validateDate(dateStr: String): Boolean {
    if (!dateStr.matches(dateRegex)) return false
    return try {
        // For scheduled payments, future dates ARE allowed (and encouraged!)
        LocalDate.parse(dateStr, displayFormatter)
        true
    } catch (e: DateTimeParseException) {
        false
    }
}
// --- END DATE HELPERS ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecurringTransactionFormDialog(
    itemToEdit: ScheduledTransaction? = null,
    accounts: List<Account>,
    onDismiss: () -> Unit,
    onConfirm: (ScheduledTransaction) -> Unit
) {
    var type by remember { mutableStateOf(itemToEdit?.type ?: TransactionType.EXPENSE) }
    var amount by remember { mutableStateOf(itemToEdit?.amount?.toString() ?: "") }
    var description by remember { mutableStateOf(itemToEdit?.description ?: "") }
    var category by remember { mutableStateOf(itemToEdit?.category ?: "") }
    var selectedAccountId by remember { mutableStateOf<Long?>(itemToEdit?.accountId) }
    var frequency by remember { mutableStateOf(itemToEdit?.frequency ?: "Monthly") }

    // --- DATE STATE ---
    // Default to today if adding new, or format existing DB date if editing
    var dateText by remember {
        mutableStateOf(itemToEdit?.nextDueDate?.toDisplayDateFromDb() ?: getTodayDateMillis().toDisplayDateString())
    }
    var isDateError by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }

    val categories = if (type == TransactionType.INCOME) DEFAULT_INCOME_CATEGORIES else DEFAULT_EXPENSE_CATEGORIES
    // --- ADDED "Daily" ---
    val frequencies = listOf("Daily", "Weekly", "Monthly", "Yearly")
    val scrollState = rememberScrollState()

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
                // Allow ALL dates (past, present, future) for scheduling
                override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                    return true
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
                    label = { Text("Description (e.g. Netflix, Salary)") },
                    modifier = Modifier.fillMaxWidth()
                )

                // --- DATE FIELD ---
                OutlinedTextField(
                    value = dateText,
                    onValueChange = {
                        dateText = it
                        isDateError = !validateDate(it)
                    },
                    label = { Text("Next Due Date") },
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

                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Amount (0 for variable)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                FormDropdown(
                    label = "Category",
                    options = categories,
                    onOptionSelected = { category = categories[it] },
                    selectedTextValue = category
                )

                FormDropdown(
                    label = "Frequency",
                    options = frequencies,
                    onOptionSelected = { frequency = frequencies[it] },
                    selectedTextValue = frequency
                )

                FormDropdown(
                    label = "Default Account",
                    options = accounts.map { it.name },
                    onOptionSelected = { selectedAccountId = accounts[it].id },
                    selectedTextValue = accounts.find { it.id == selectedAccountId }?.name ?: ""
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Button(
                        onClick = {
                            // Convert UI date (DD-MM-YYYY) to DB date (YYYY-MM-DD)
                            val dbDate = try {
                                LocalDate.parse(dateText, displayFormatter).format(dbFormatter)
                            } catch (e: Exception) {
                                getTodayDateMillis().toDbDateString()
                            }

                            val newItem = ScheduledTransaction(
                                id = itemToEdit?.id ?: 0L,
                                type = type,
                                amount = amount.toDoubleOrNull() ?: 0.0,
                                category = category,
                                accountId = selectedAccountId ?: 0L,
                                paymentMode = null,
                                description = description,
                                frequency = frequency,
                                nextDueDate = dbDate // <-- Using the selected date
                            )
                            onConfirm(newItem)
                            onDismiss()
                        },
                        enabled = description.isNotBlank() && selectedAccountId != null && !isDateError
                    ) { Text("Save") }
                }
            }
        }
    }
}