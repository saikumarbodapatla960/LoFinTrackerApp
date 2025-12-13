// In ...ui/screens/AccountFormDialog.kt
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
import com.skai.lofintrackerapp.data.db.AccountType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountFormDialog(
    accountToEdit: Account? = null, // Null for "Add", non-null for "Edit"
    onDismiss: () -> Unit,
    onConfirm: (Account) -> Unit
) {
    // Set initial state from the account being edited, or empty
    var name by remember { mutableStateOf(accountToEdit?.name ?: "") }
    var initialBalance by remember { mutableStateOf(accountToEdit?.initialBalance?.toString() ?: "") }
    var selectedType by remember { mutableStateOf(accountToEdit?.type ?: AccountType.SAVINGS) }
    var isDropdownExpanded by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = if (accountToEdit == null) "New Account" else "Edit Account",
                    style = MaterialTheme.typography.titleLarge
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Account Name") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = initialBalance,
                    onValueChange = { initialBalance = it },
                    label = { Text("Initial Balance") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    // Disable editing initial balance after creation
                    enabled = accountToEdit == null
                )

                ExposedDropdownMenuBox(
                    expanded = isDropdownExpanded,
                    onExpandedChange = { isDropdownExpanded = !it }
                ) {
                    OutlinedTextField(
                        value = selectedType.name,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Account Type") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isDropdownExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                        enabled = accountToEdit == null // Disable type change on edit
                    )
                    ExposedDropdownMenu(
                        expanded = isDropdownExpanded,
                        onDismissRequest = { isDropdownExpanded = false }
                    ) {
                        // Filter out "CASH" as it's a default, non-creatable type
                        AccountType.entries.filter { it != AccountType.CASH }.forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type.name) },
                                onClick = {
                                    selectedType = type
                                    isDropdownExpanded = false
                                }
                            )
                        }
                    }
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
                        val balance = initialBalance.toDoubleOrNull() ?: 0.0

                        val finalAccount = if (accountToEdit != null) {
                            // If editing, only update the name
                            accountToEdit.copy(name = name)
                        } else {
                            // If new, create a full Account object
                            Account(
                                name = name,
                                type = selectedType,
                                initialBalance = balance,
                                balance = balance // New account balance starts at initial
                            )
                        }
                        onConfirm(finalAccount)
                    }) {
                        Text(if (accountToEdit == null) "Save" else "Update")
                    }
                }
            }
        }
    }
}