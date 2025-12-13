// In ...ui.screens/CreditCardFormDialog.kt
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
// We don't need FormDropdown here, this file is actually fine.
// The errors were likely phantom errors from the AppDao bug.
// But I will provide the full code just in case.

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
                    label = { Text("Card Name (e.g., Chase Sapphire)") },
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
                        onValueChange = { statementDate = it },
                        label = { Text("Statement Day") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = dueDate,
                        onValueChange = { dueDate = it },
                        label = { Text("Due Day") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
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
                    Button(onClick = {
                        val finalCard = (cardToEdit ?: CreditCard()).copy(
                            name = name,
                            limit = limit.toDoubleOrNull() ?: 0.0,
                            statementDate = statementDate.toIntOrNull() ?: 15,
                            dueDate = dueDate.toIntOrNull() ?: 28
                            // amountOwed is not set here
                        )
                        onConfirm(finalCard)
                    }) {
                        Text(if (cardToEdit == null) "Save" else "Update")
                    }
                }
            }
        }
    }
}