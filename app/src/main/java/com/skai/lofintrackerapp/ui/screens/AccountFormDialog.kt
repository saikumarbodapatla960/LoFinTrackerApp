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
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountFormDialog(
    accountToEdit: Account? = null,
    onDismiss: () -> Unit,
    onConfirm: suspend (Account) -> String?, // For creating a new account
    onUpdate: (Account) -> Unit, // For updating an existing account
    isCashAccountInitialBalanceEditable: Boolean
) {
    var name by remember { mutableStateOf(accountToEdit?.name ?: "") }
    var initialBalance by remember { mutableStateOf(accountToEdit?.initialBalance?.toString() ?: "0.0") }
    // Lock the type to CASH if it's the initial editable one, otherwise default to SAVINGS
    var selectedType by remember {
        mutableStateOf(
            if (accountToEdit == null && isCashAccountInitialBalanceEditable) AccountType.CASH
            else accountToEdit?.type ?: AccountType.SAVINGS
        )
    }
    var isDropdownExpanded by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()

    // Determine if the form fields should be enabled
    val isEditing = accountToEdit != null
    val isInitialCashSetup = !isEditing && isCashAccountInitialBalanceEditable

    Dialog(onDismissRequest = onDismiss) {
        Card {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = if (isEditing) "Edit Account" else "New Account",
                    style = MaterialTheme.typography.titleLarge
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Account Name") },
                    modifier = Modifier.fillMaxWidth(),
                    // For the one-time cash setup, the name "Cash" is not editable
                    readOnly = isInitialCashSetup
                )

                OutlinedTextField(
                    value = initialBalance,
                    onValueChange = { initialBalance = it },
                    label = { Text("Initial Balance") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    // Balance is only editable when creating a new account (or the special cash setup)
                    enabled = !isEditing
                )
                errorMessage?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }

                // Account Type Dropdown
                ExposedDropdownMenuBox(
                    expanded = isDropdownExpanded && !isEditing && !isInitialCashSetup, // Not expandable when editing or in cash setup
                    onExpandedChange = { isDropdownExpanded = !isDropdownExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedType.name,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Account Type") },
                        trailingIcon = { if (!isEditing && !isInitialCashSetup) ExposedDropdownMenuDefaults.TrailingIcon(expanded = isDropdownExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                        enabled = !isEditing
                    )
                    ExposedDropdownMenu(
                        expanded = isDropdownExpanded && !isEditing && !isInitialCashSetup,
                        onDismissRequest = { isDropdownExpanded = false }
                    ) {
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

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Button(
                        enabled = name.isNotBlank(),
                        onClick = {
                            coroutineScope.launch {
                                val balance = initialBalance.toDoubleOrNull() ?: 0.0
                                if (isEditing) {
                                    onUpdate(accountToEdit!!.copy(name = name))
                                } else {
                                    val newAccount = Account(
                                        name = if(isInitialCashSetup) "Cash" else name,
                                        type = selectedType,
                                        initialBalance = balance,
                                        balance = balance
                                    )
                                    val error = onConfirm(newAccount)
                                    if (error != null) {
                                        errorMessage = error
                                    }
                                }
                            }
                        }
                    ) {
                        Text(if (isEditing) "Update" else "Save")
                    }
                }
            }
        }
    }
}
