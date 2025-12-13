// In ...ui.screens/LoanFormDialog.kt
package com.skai.lofintrackerapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.skai.lofintrackerapp.data.db.Account
import com.skai.lofintrackerapp.data.db.Loan
import com.skai.lofintrackerapp.ui.common.FormDropdown // <-- ADDED THIS IMPORT
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
                    value = initialAmount,
                    onValueChange = { initialAmount = it },
                    label = { Text("Initial Amount") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    enabled = loanToEdit == null
                )

                if (loanToEdit == null) {
                    // This line will now work
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

                    val isSaveEnabled = loanToEdit != null || (selectedAccountId != null && initialAmount.isNotBlank() && name.isNotBlank() && lender.isNotBlank())

                    Button(
                        onClick = {
                            val amount = initialAmount.toDoubleOrNull() ?: 0.0

                            val finalLoan = if (loanToEdit != null) {
                                loanToEdit.copy(name = name, lender = lender)
                            } else {
                                val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                                Loan(
                                    name = name,
                                    lender = lender,
                                    initialAmount = amount,
                                    remainingAmount = amount,
                                    date = currentDate
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