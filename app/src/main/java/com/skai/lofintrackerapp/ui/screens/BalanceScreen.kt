// In ...ui/screens/BalanceScreen.kt
package com.skai.lofintrackerapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.skai.lofintrackerapp.data.db.Account
import com.skai.lofintrackerapp.data.db.AccountType
import com.skai.lofintrackerapp.data.db.Transaction
import com.skai.lofintrackerapp.ui.common.ConfirmationDialog
import com.skai.lofintrackerapp.ui.common.ReusableTotalCard
import com.skai.lofintrackerapp.ui.theme.FabGreen
import com.skai.lofintrackerapp.ui.viewmodel.MainViewModel
import java.text.NumberFormat
import java.util.Locale

@Composable
fun BalanceScreen(viewModel: MainViewModel) {

    // --- State Management ---
    val accounts by viewModel.allAccounts.collectAsStateWithLifecycle()
    val totalBalance by viewModel.totalBalance.collectAsStateWithLifecycle()

    // Dialog States
    var showAddOrEditDialog by remember { mutableStateOf(false) }
    var showUndeletableWarning by remember { mutableStateOf(false) }
    var accountToEdit by remember { mutableStateOf<Account?>(null) }
    var showEditConfirmation by remember { mutableStateOf(false) }
    var showDeleteConfirmation by remember { mutableStateOf<Account?>(null) }

    // Transaction Edit States
    var showTransactionDialog by remember { mutableStateOf(false) }
    var transactionToEdit by remember { mutableStateOf<Transaction?>(null) }

    // Filter States
    val startDate by viewModel.startDate.collectAsStateWithLifecycle()
    val endDate by viewModel.endDate.collectAsStateWithLifecycle()
    val selectedAccountIds by viewModel.selectedAccountIds.collectAsStateWithLifecycle()
    val filteredTransactions by viewModel.filteredBalanceTransactions.collectAsStateWithLifecycle()

    // Other Data for Transaction Form
    val loans by viewModel.allLoans.collectAsStateWithLifecycle()
    val creditCards by viewModel.allCreditCards.collectAsStateWithLifecycle()

    val filterItems = remember(accounts) {
        accounts.map { FilterSelectionItem(id = it.id, name = it.name) }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    accountToEdit = null
                    showAddOrEditDialog = true
                },
                containerColor = FabGreen
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Account")
            }
        },
        // --- FIX: REMOVE EXTRA TOP PADDING ---
        contentWindowInsets = WindowInsets(0.dp)
    ) { paddingValues ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {

            // 1. Total Balance Card
            // Manually add top padding here
            ReusableTotalCard(
                title = "Total Balance",
                totalAmount = totalBalance,
                cardColor = Color(0xFFE3F2FD),
                textColor = Color(0xFF2196F3),
                modifier = Modifier.padding(top = 16.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(text = "Your Accounts", style = MaterialTheme.typography.titleLarge)

            Spacer(modifier = Modifier.height(16.dp))

            // 2. Accounts List
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                accounts.forEach { account ->
                    AccountRow(
                        account = account,
                        onEdit = {
                            accountToEdit = account
                            showEditConfirmation = true
                        },
                        onDelete = {
                            if (account.type == AccountType.CASH) {
                                showUndeletableWarning = true
                            } else {
                                showDeleteConfirmation = account
                            }
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 3. Filter Controls
            FilterControls(
                startDate = startDate,
                endDate = endDate,
                onStartDateSelected = { viewModel.onStartDateChange(it) },
                onEndDateSelected = { viewModel.onEndDateChange(it) },
                viewModel = viewModel,
                filterTitle = "Accounts",
                allItems = filterItems,
                selectedIds = selectedAccountIds,
                onItemToggle = { viewModel.toggleAccountId(it) }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 4. Transaction List
            Text(text = "Recent Transactions", style = MaterialTheme.typography.titleLarge)

            Spacer(modifier = Modifier.height(16.dp))

            TransactionList(
                transactions = filteredTransactions,
                accounts = accounts,
                onEdit = { transaction ->
                    transactionToEdit = transaction
                    showTransactionDialog = true
                }
            )

            Spacer(modifier = Modifier.height(16.dp))
        }

        // --- DIALOG HANDLERS ---

        if (showAddOrEditDialog) {
            AccountFormDialog(
                accountToEdit = accountToEdit,
                onDismiss = {
                    showAddOrEditDialog = false
                    accountToEdit = null
                },
                onConfirm = { account ->
                    if (accountToEdit == null) {
                        viewModel.insertAccount(account)
                    } else {
                        viewModel.updateAccount(account)
                    }
                    showAddOrEditDialog = false
                    accountToEdit = null
                }
            )
        }

        if (showEditConfirmation && accountToEdit != null) {
            ConfirmationDialog(
                title = "Edit Account",
                message = "Are you sure you want to edit '${accountToEdit!!.name}'?",
                icon = Icons.Default.Edit,
                confirmText = "Edit",
                onDismiss = {
                    showEditConfirmation = false
                    accountToEdit = null
                },
                onConfirm = {
                    showAddOrEditDialog = true
                    showEditConfirmation = false
                }
            )
        }

        if (showDeleteConfirmation != null) {
            ConfirmationDialog(
                title = "Delete Account",
                message = "Are you sure you want to delete '${showDeleteConfirmation!!.name}'? This cannot be undone.",
                icon = Icons.Default.Delete,
                confirmText = "Delete",
                onDismiss = { showDeleteConfirmation = null },
                onConfirm = {
                    viewModel.deleteAccount(showDeleteConfirmation!!)
                    showDeleteConfirmation = null
                }
            )
        }

        if (showUndeletableWarning) {
            SimpleAlertDialog(
                title = "Cannot Delete",
                message = "The default 'Cash' account cannot be deleted.",
                onDismiss = { showUndeletableWarning = false }
            )
        }

        if (showTransactionDialog) {
            TransactionFormDialog(
                transactionToEdit = transactionToEdit,
                accounts = accounts,
                loans = loans,
                creditCards = creditCards,
                onDismiss = {
                    showTransactionDialog = false
                    transactionToEdit = null
                },
                onConfirm = { transaction ->
                    if (transactionToEdit == null) {
                        viewModel.insertTransaction(transaction)
                    } else {
                        viewModel.updateTransaction(transactionToEdit!!, transaction)
                    }
                },
                onDelete = { transaction ->
                    viewModel.deleteTransaction(transaction)
                }
            )
        }
    }
}

@Composable
private fun AccountRow(
    account: Account,
    onEdit: (Account) -> Unit,
    onDelete: (Account) -> Unit
) {
    val formattedBalance = NumberFormat.getCurrencyInstance(Locale("en", "IN")).format(account.balance)

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = account.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = account.type.name,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = formattedBalance,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold
            )
            IconButton(onClick = { onEdit(account) }) {
                Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = { onDelete(account) }) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red)
            }
        }
    }
}