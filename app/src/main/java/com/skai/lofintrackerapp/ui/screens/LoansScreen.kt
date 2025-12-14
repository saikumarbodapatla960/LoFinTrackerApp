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
import com.skai.lofintrackerapp.data.db.Loan
import com.skai.lofintrackerapp.data.db.Transaction
import com.skai.lofintrackerapp.ui.common.ConfirmationDialog
import com.skai.lofintrackerapp.ui.common.ReusableTotalCard
import com.skai.lofintrackerapp.ui.common.formatCurrency
import com.skai.lofintrackerapp.ui.theme.FabGreen
import com.skai.lofintrackerapp.ui.viewmodel.MainViewModel
// --- IMPORT THIS ---
import com.skai.lofintrackerapp.ui.screens.FilterSelectionItem

@Composable
fun LoansScreen(viewModel: MainViewModel) {
    val loans by viewModel.allLoans.collectAsStateWithLifecycle()
    val accounts by viewModel.allAccounts.collectAsStateWithLifecycle()
    val totalLoans by viewModel.totalLoans.collectAsStateWithLifecycle()
    val creditCards by viewModel.allCreditCards.collectAsStateWithLifecycle()
    val currency by viewModel.currency.collectAsStateWithLifecycle()

    var showAddOrEditDialog by remember { mutableStateOf(false) }
    var loanToEdit by remember { mutableStateOf<Loan?>(null) }
    var showEditConfirmation by remember { mutableStateOf(false) }
    var showDeleteConfirmation by remember { mutableStateOf<Loan?>(null) }
    var showTransactionDialog by remember { mutableStateOf(false) }
    var transactionToEdit by remember { mutableStateOf<Transaction?>(null) }

    val startDate by viewModel.startDate.collectAsStateWithLifecycle()
    val endDate by viewModel.endDate.collectAsStateWithLifecycle()
    val selectedLoanIds by viewModel.selectedLoanIds.collectAsStateWithLifecycle()
    val filteredTransactions by viewModel.filteredLoanTransactions.collectAsStateWithLifecycle()

    // Map loans to filter items (ID + Name)
    val filterItems = remember(loans) {
        loans.map { FilterSelectionItem(id = it.id, name = "${it.name} (${it.lender})") }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { loanToEdit = null; showAddOrEditDialog = true }, containerColor = FabGreen) {
                Icon(Icons.Default.Add, "Add Loan")
            }
        },
        contentWindowInsets = WindowInsets(0.dp)
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp).verticalScroll(rememberScrollState())) {
            Spacer(modifier = Modifier.height(16.dp))
            ReusableTotalCard("Total Outstanding Loans", totalLoans, currency, Color(0xFFFFF8E1), Color(0xFFFF9800))
            Spacer(modifier = Modifier.height(16.dp))

            Text("Your Loans", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(16.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                loans.forEach { loan ->
                    LoanRow(loan, currency, { loanToEdit = loan; showEditConfirmation = true }, { showDeleteConfirmation = loan })
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // This now works because FilterControls (Step 1) accepts 'allItems'
            FilterControls(
                startDate = startDate,
                endDate = endDate,
                onStartDateSelected = { viewModel.onStartDateChange(it) },
                onEndDateSelected = { viewModel.onEndDateChange(it) },
                viewModel = viewModel,
                filterTitle = "Loans",
                allItems = filterItems,
                selectedIds = selectedLoanIds,
                onItemToggle = { viewModel.toggleLoanId(it) }
            )

            Spacer(modifier = Modifier.height(16.dp))
            Text("Recent Loan Transactions", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(16.dp))

            TransactionList(filteredTransactions, accounts, currency, { transactionToEdit = it; showTransactionDialog = true })
            Spacer(modifier = Modifier.height(80.dp))
        }

        // Dialogs
        if (showAddOrEditDialog) { LoanFormDialog(loanToEdit, accounts, { showAddOrEditDialog = false; loanToEdit = null }, { loan, acc -> if(loanToEdit == null && acc != null) viewModel.insertLoanAndPayout(loan, acc) else viewModel.updateLoan(loan); showAddOrEditDialog = false; loanToEdit = null }) }
        if (showEditConfirmation && loanToEdit != null) { ConfirmationDialog("Edit Loan", "Edit '${loanToEdit!!.name}'?", Icons.Default.Edit, "Edit", { showAddOrEditDialog = true; showEditConfirmation = false }, { showEditConfirmation = false; loanToEdit = null }) }
        if (showDeleteConfirmation != null) { ConfirmationDialog("Delete Loan", "Delete '${showDeleteConfirmation!!.name}'?", Icons.Default.Delete, "Delete", { viewModel.deleteLoan(showDeleteConfirmation!!); showDeleteConfirmation = null }, { showDeleteConfirmation = null }) }
        if (showTransactionDialog) { TransactionFormDialog(transactionToEdit, accounts, loans, creditCards, { showTransactionDialog = false; transactionToEdit = null }, { if (transactionToEdit == null) viewModel.insertTransaction(it) else viewModel.updateTransaction(transactionToEdit!!, it) }, { viewModel.deleteTransaction(it) }) }
    }
}

@Composable
private fun LoanRow(loan: Loan, currency: String, onEdit: (Loan) -> Unit, onDelete: (Loan) -> Unit) {
    val formattedRemaining = formatCurrency(loan.remainingAmount, currency)
    val formattedInitial = formatCurrency(loan.initialAmount, currency)
    Card(elevation = CardDefaults.cardElevation(2.dp)) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(loan.name, style = MaterialTheme.typography.titleMedium)
                Text("from ${loan.lender}\nInitial: $formattedInitial", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(formattedRemaining, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
            IconButton(onClick = { onEdit(loan) }) { Icon(Icons.Default.Edit, "Edit") }
            IconButton(onClick = { onDelete(loan) }) { Icon(Icons.Default.Delete, "Delete", tint = Color.Red) }
        }
    }
}