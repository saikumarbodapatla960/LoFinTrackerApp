package com.skai.lofintrackerapp.ui.screens

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

@Composable
fun BalanceScreen(viewModel: MainViewModel) {
    val accounts by viewModel.allAccounts.collectAsStateWithLifecycle()
    val totalBalance by viewModel.totalBalance.collectAsStateWithLifecycle()
    val currency by viewModel.currency.collectAsStateWithLifecycle()

    var showAddOrEditDialog by remember { mutableStateOf(false) }
    var showUndeletableWarning by remember { mutableStateOf(false) }
    var accountToEdit by remember { mutableStateOf<Account?>(null) }
    var showEditConfirmation by remember { mutableStateOf(false) }
    var showDeleteConfirmation by remember { mutableStateOf<Account?>(null) }

    var showTransactionDialog by remember { mutableStateOf(false) }
    var transactionToEdit by remember { mutableStateOf<Transaction?>(null) }
    val loans by viewModel.allLoans.collectAsStateWithLifecycle()
    val creditCards by viewModel.allCreditCards.collectAsStateWithLifecycle()

    val startDate by viewModel.startDate.collectAsStateWithLifecycle()
    val endDate by viewModel.endDate.collectAsStateWithLifecycle()
    val selectedAccountIds by viewModel.selectedAccountIds.collectAsStateWithLifecycle()
    val filteredTransactions by viewModel.filteredBalanceTransactions.collectAsStateWithLifecycle()
    val filterItems = remember(accounts) { accounts.map { FilterSelectionItem(id = it.id, name = it.name) } }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { accountToEdit = null; showAddOrEditDialog = true }, containerColor = FabGreen) { Icon(Icons.Default.Add, "Add Account") }
        },
        contentWindowInsets = WindowInsets(0.dp)
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp).verticalScroll(rememberScrollState())) {
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

            // FIX: Using Named Arguments to prevent mismatch
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
            Text("Recent Transactions", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(16.dp))

            TransactionList(filteredTransactions, accounts, currency, { transactionToEdit = it; showTransactionDialog = true })
            Spacer(modifier = Modifier.height(80.dp))
        }

        if (showAddOrEditDialog) AccountFormDialog(accountToEdit, { showAddOrEditDialog = false; accountToEdit = null }, { if (accountToEdit == null) viewModel.insertAccount(it) else viewModel.updateAccount(it); showAddOrEditDialog = false; accountToEdit = null })
        if (showEditConfirmation && accountToEdit != null) ConfirmationDialog("Edit Account", "Edit '${accountToEdit!!.name}'?", Icons.Default.Edit, "Edit", { showAddOrEditDialog = true; showEditConfirmation = false }, { showEditConfirmation = false; accountToEdit = null })
        if (showDeleteConfirmation != null) ConfirmationDialog("Delete Account", "Delete '${showDeleteConfirmation!!.name}'?", Icons.Default.Delete, "Delete", { viewModel.deleteAccount(showDeleteConfirmation!!); showDeleteConfirmation = null }, { showDeleteConfirmation = null })
        if (showUndeletableWarning) SimpleAlertDialog("Cannot Delete", "The default 'Cash' account cannot be deleted.", { showUndeletableWarning = false })
        if (showTransactionDialog) TransactionFormDialog(transactionToEdit, accounts, loans, creditCards, { showTransactionDialog = false; transactionToEdit = null }, { if (transactionToEdit == null) viewModel.insertTransaction(it) else viewModel.updateTransaction(transactionToEdit!!, it) }, { viewModel.deleteTransaction(it) })
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