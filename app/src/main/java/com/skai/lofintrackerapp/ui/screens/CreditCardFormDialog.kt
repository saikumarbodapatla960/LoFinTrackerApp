package com.skai.lofintrackerapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.skai.lofintrackerapp.data.db.CreditCard

@Composable
fun CreditCardFormDialog(
    cardToEdit: CreditCard? = null,
    onDismiss: () -> Unit,
    onConfirm: (CreditCard) -> Unit
) {
    var name by remember { mutableStateOf(cardToEdit?.name ?: "") }
    var limit by remember { mutableStateOf(cardToEdit?.limit?.toString() ?: "") }
    var statementDate by remember { mutableStateOf(cardToEdit?.statementDate?.toString() ?: "15") }
    var dueDate by remember { mutableStateOf(cardToEdit?.dueDate?.toString() ?: "28") }

    var statementDateError by remember { mutableStateOf(false) }
    var dueDateError by remember { mutableStateOf(false) }

    fun validateDay(dayStr: String): Boolean {
        val day = dayStr.toIntOrNull() ?: return false
        return day in 1..31
    }

    Dialog(onDismissRequest = onDismiss) {
        Card {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = if (cardToEdit == null) "New Credit Card" else "Edit Credit Card",
                    style = MaterialTheme.typography.titleLarge
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Card Name (e.g., HDFC Regalia)") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = limit,
                    onValueChange = { limit = it },
                    label = { Text("Credit Limit") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = statementDate,
                        onValueChange = { 
                            if (it.length <= 2) {
                                statementDate = it
                                statementDateError = it.isNotEmpty() && !validateDay(it)
                            }
                        },
                        label = { Text("Statement Day") },
                        isError = statementDateError,
                        supportingText = { if (statementDateError) Text("1-31 only") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = dueDate,
                        onValueChange = { 
                            if (it.length <= 2) {
                                dueDate = it
                                dueDateError = it.isNotEmpty() && !validateDay(it)
                            }
                        },
                        label = { Text("Due Day") },
                        isError = dueDateError,
                        supportingText = { if (dueDateError) Text("1-31 only") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Button(
                        onClick = {
                            val finalCard = (cardToEdit ?: CreditCard()).copy(
                                name = name,
                                limit = limit.toDoubleOrNull() ?: 0.0,
                                statementDate = statementDate.toIntOrNull() ?: 15,
                                dueDate = dueDate.toIntOrNull() ?: 28
                            )
                            onConfirm(finalCard)
                        },
                        enabled = name.isNotBlank() && limit.toDoubleOrNull() != null && 
                                  validateDay(statementDate) && validateDay(dueDate)
                    ) {
                        Text(if (cardToEdit == null) "Save" else "Update")
                    }
                }
            }
        }
    }
}