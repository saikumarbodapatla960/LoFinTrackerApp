package com.skai.lofintrackerapp.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.skai.lofintrackerapp.data.db.Account
import com.skai.lofintrackerapp.data.db.AccountType
import com.skai.lofintrackerapp.data.db.Transaction
import com.skai.lofintrackerapp.ui.common.ConfirmationDialog
import com.skai.lofintrackerapp.ui.common.ReusableTotalCard
import com.skai.lofintrackerapp.ui.common.formatCurrency
import com.skai.lofintrackerapp.ui.theme.FabGreen
import com.skai.lofintrackerapp.ui.viewmodel.MainViewModel
import kotlinx.coroutines.launch

@Composable
fun BalanceScreen(viewModel: MainViewModel) {
    val accounts by viewModel.allAccounts.collectAsStateWithLifecycle(initialValue = emptyList())
    val totalBalance by viewModel.totalBalance.collectAsStateWithLifecycle(initialValue = 0.0)
    val currency by viewModel.currency.collectAsStateWithLifecycle(initialValue = "INR")
    val isSortDesc by viewModel.sortDescending.collectAsStateWithLifecycle(initialValue = true)
    val isCashAccountInitialBalanceEditable by viewModel.isCashAccountInitialBalanceEditable.collectAsStateWithLifecycle(initialValue = false)

    var showAddOrEditDialog by remember { mutableStateOf(false) }
    var showUndeletableWarning by remember { mutableStateOf(false) }
    var accountToEdit by remember { mutableStateOf<Account?>(null) }
    var showEditConfirmation by remember { mutableStateOf(false) }
    var showDeleteConfirmation by remember { mutableStateOf<Account?>(null) }

    var showTransactionDialog by remember { mutableStateOf(false) }
    var transactionToEdit by remember { mutableStateOf<Transaction?>(null) }
    val loans by viewModel.allLoans.collectAsStateWithLifecycle(initialValue = emptyList())
    val creditCards by viewModel.allCreditCards.collectAsStateWithLifecycle(initialValue = emptyList())

    val startDate by viewModel.startDate.collectAsStateWithLifecycle()
    val endDate by viewModel.endDate.collectAsStateWithLifecycle()
    val selectedAccountIds by viewModel.selectedAccountIds.collectAsStateWithLifecycle(initialValue = emptySet())
    val filteredTransactions by viewModel.filteredBalanceTransactions.collectAsStateWithLifecycle(initialValue = emptyList())
    val filterItems = remember(accounts) { accounts.map { FilterSelectionItem(id = it.id, name = it.name) } }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        floatingActionButton = {
            FloatingActionButton(onClick = { accountToEdit = null; showAddOrEditDialog = true }, containerColor = FabGreen) { Icon(Icons.Default.Add, "Add Account") }
        },
        contentWindowInsets = WindowInsets(0.dp)
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues).padding(horizontal = 16.dp).verticalScroll(rememberScrollState())) {
            Spacer(modifier = Modifier.height(16.dp))
            ReusableTotalCard("Total Balance", totalBalance, currency, Color(0xFFE3F2FD), Color(0xFF2196F3))
            Spacer(modifier = Modifier.height(16.dp))
            Text("Your Accounts", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(16.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                accounts.forEach { account ->
                    AccountRow(account, currency, { accountToEdit = account; showEditConfirmation = true }, { if (account.type == AccountType.CASH) showUndeletableWarning = true else showDeleteConfirmation = account })
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Recent Transactions", style = MaterialTheme.typography.titleLarge)
                IconButton(onClick = { viewModel.toggleSortOrder() }) {
                    Icon(
                        imageVector = Icons.Default.AccessTime,
                        contentDescription = if (isSortDesc) "Newest added first" else "Oldest added first",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))

            TransactionList(
                transactions = filteredTransactions,
                accounts = accounts,
                creditCards = creditCards,
                currencyCode = currency,
                onEdit = { transaction ->
                    transactionToEdit = transaction
                    showTransactionDialog = true
                }
            )
            Spacer(modifier = Modifier.height(80.dp)) // Spacer at bottom for FAB
        }

        if (showAddOrEditDialog) {
            AccountFormDialog(
                accountToEdit = accountToEdit,
                onDismiss = { showAddOrEditDialog = false; accountToEdit = null },
                onConfirm = { newAccount ->
                    val error = viewModel.insertAccount(newAccount)
                    if (error == null) {
                        showAddOrEditDialog = false
                        accountToEdit = null
                    }
                    error // Return error message to the dialog
                },
                onUpdate = { updatedAccount ->
                    viewModel.updateAccount(updatedAccount)
                    showAddOrEditDialog = false
                    accountToEdit = null
                },
                isCashAccountInitialBalanceEditable = isCashAccountInitialBalanceEditable,
                accountsExist = accounts.isNotEmpty()
            )
        }
        if (showEditConfirmation && accountToEdit != null) ConfirmationDialog("Edit Account", "Edit '${accountToEdit!!.name}'?", Icons.Default.Edit, "Edit", { showAddOrEditDialog = true; showEditConfirmation = false }, { showEditConfirmation = false; accountToEdit = null })
        if (showDeleteConfirmation != null) ConfirmationDialog("Delete Account", "Delete '${showDeleteConfirmation!!.name}'?", Icons.Default.Delete, "Delete", { viewModel.deleteAccount(showDeleteConfirmation!!); showDeleteConfirmation = null }, { showDeleteConfirmation = null })
        if (showUndeletableWarning) SimpleAlertDialog("Cannot Delete", "The default 'Cash' account cannot be deleted.", { showUndeletableWarning = false })
        if (showTransactionDialog) {
            TransactionFormDialog(
                transactionToEdit = transactionToEdit,
                accounts = accounts,
                loans = loans,
                creditCards = creditCards,
                onDismiss = { showTransactionDialog = false; transactionToEdit = null },
                onConfirm = { if (transactionToEdit == null) viewModel.insertTransaction(it) else viewModel.updateTransaction(transactionToEdit!!, it) },
                onDelete = { viewModel.deleteTransaction(it) },
                onAddScheduled = { viewModel.insertScheduledTransaction(it) }
            )
        }
    }
}

@Composable
private fun AccountRow(account: Account, currency: String, onEdit: (Account) -> Unit, onDelete: (Account) -> Unit) {
    val formattedBalance = formatCurrency(account.balance, currency)
    Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(2.dp)) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(account.name, style = MaterialTheme.typography.titleMedium)
                Text(account.type.name, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(formattedBalance, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
            IconButton(onClick = { onEdit(account) }) { Icon(Icons.Default.Edit, "Edit") }
            IconButton(onClick = { onDelete(account) }) { Icon(Icons.Default.Delete, "Delete", tint = Color.Red) }
        }
    }
}
