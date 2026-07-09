package com.skai.lofintrackerapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.skai.lofintrackerapp.data.db.Account
import com.skai.lofintrackerapp.data.db.Loan
import com.skai.lofintrackerapp.ui.common.FormDropdown
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

private val dbFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
private val displayFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoanFormDialog(
    loanToEdit: Loan? = null,
    accounts: List<Account>,
    onDismiss: () -> Unit,
    onConfirm: (Loan, Long?) -> Unit
) {
    var name by remember { mutableStateOf(loanToEdit?.name ?: "") }
    var lender by remember { mutableStateOf(loanToEdit?.lender ?: "") }
    var initialAmount by remember { mutableStateOf(loanToEdit?.initialAmount?.toString() ?: "") }
    var selectedAccountId by remember { mutableStateOf<Long?>(null) }
    
    // Date State
    var dateText by remember { 
        mutableStateOf(loanToEdit?.date?.let { 
            LocalDate.parse(it, dbFormatter).format(displayFormatter) 
        } ?: LocalDate.now().format(displayFormatter)) 
    }
    var showDatePicker by remember { mutableStateOf(false) }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = LocalDate.parse(dateText, displayFormatter)
                .atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        dateText = Instant.ofEpochMilli(it).atZone(ZoneId.of("UTC")).toLocalDate().format(displayFormatter)
                    }
                    showDatePicker = false
                }) { Text("OK") }
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
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = if (loanToEdit == null) "New Loan" else "Edit Loan",
                    style = MaterialTheme.typography.titleLarge
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Loan Name (e.g., Car Loan)") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = lender,
                    onValueChange = { lender = it },
                    label = { Text("Lender (e.g., City Bank)") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = dateText,
                    onValueChange = { },
                    label = { Text("Date") },
                    readOnly = true,
                    trailingIcon = {
                        IconButton(onClick = { showDatePicker = true }) {
                            Icon(Icons.Default.DateRange, contentDescription = "Select Date")
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = initialAmount,
                    onValueChange = { initialAmount = it },
                    label = { Text("Initial Amount") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    enabled = loanToEdit == null
                )

                if (loanToEdit == null) {
                    FormDropdown(
                        label = "Deposit to Account",
                        options = accounts.map { it.name },
                        onOptionSelected = { index ->
                            selectedAccountId = accounts[index].id
                        },
                        selectedTextValue = accounts.find { it.id == selectedAccountId }?.name ?: ""
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }

                    val isSaveEnabled = name.isNotBlank() && lender.isNotBlank() && 
                            (loanToEdit != null || (selectedAccountId != null && initialAmount.toDoubleOrNull() != null))

                    Button(
                        onClick = {
                            val amount = initialAmount.toDoubleOrNull() ?: 0.0
                            val dbDate = LocalDate.parse(dateText, displayFormatter).format(dbFormatter)

                            val finalLoan = if (loanToEdit != null) {
                                loanToEdit.copy(name = name, lender = lender, date = dbDate)
                            } else {
                                Loan(
                                    name = name,
                                    lender = lender,
                                    initialAmount = amount,
                                    remainingAmount = amount,
                                    date = dbDate
                                )
                            }
                            onConfirm(finalLoan, selectedAccountId)
                        },
                        enabled = isSaveEnabled
                    ) {
                        Text(if (loanToEdit == null) "Save" else "Update")
                    }
                }
            }
        }
    }
}