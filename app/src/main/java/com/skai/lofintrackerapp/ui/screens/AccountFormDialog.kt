package com.skai.lofintrackerapp.ui.screens

import androidx.compose.foundation.clickable
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
    accountToEdit: Account? = null,
    onDismiss: () -> Unit,
    onConfirm: (Account) -> Unit
) {
    var name by remember { mutableStateOf(accountToEdit?.name ?: "") }
    var initialBalance by remember { mutableStateOf(accountToEdit?.initialBalance?.toString() ?: "") }
    var selectedType by remember { mutableStateOf(accountToEdit?.type ?: AccountType.SAVINGS) }
    var isDropdownExpanded by remember { mutableStateOf(false) }
    val initialBalanceValue = initialBalance.toDoubleOrNull()
    val isBalanceInvalid = accountToEdit == null && (initialBalanceValue == null || initialBalanceValue < 0.0)

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
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    enabled = accountToEdit == null,
                    isError = isBalanceInvalid,
                    supportingText = { if (isBalanceInvalid) Text("Balance cannot be negative.") }
                )

                // --- FIXED DROPDOWN LOGIC ---
                ExposedDropdownMenuBox(
                    expanded = isDropdownExpanded,
                    onExpandedChange = {
                        // Only allow expanding if creating a NEW account
                        if (accountToEdit == null) {
                            isDropdownExpanded = it
                        }
                    }
                ) {
                    OutlinedTextField(
                        value = selectedType.name,
                        onValueChange = {},
                        readOnly = true, // Crucial for dropdowns
                        label = { Text("Account Type") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isDropdownExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(), // This connects the field to the dropdown box
                        enabled = accountToEdit == null,
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                    )

                    ExposedDropdownMenu(
                        expanded = isDropdownExpanded,
                        onDismissRequest = { isDropdownExpanded = false }
                    ) {
                        // Showing all types except CASH (which is usually default/internal)
                        AccountType.entries.filter { it != AccountType.CASH }.forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type.name) },
                                onClick = {
                                    selectedType = type
                                    isDropdownExpanded = false
                                },
                                contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                            )
                        }
                    }
                }
                // --- END FIX ---

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Button(
                        enabled = name.isNotBlank() && (accountToEdit != null || !isBalanceInvalid),
                        onClick = {
                        val balance = initialBalanceValue ?: 0.0

                        val finalAccount = if (accountToEdit != null) {
                            accountToEdit.copy(name = name)
                        } else {
                            Account(
                                name = name,
                                type = selectedType,
                                initialBalance = balance,
                                balance = balance
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
