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

@Composable
fun LoansScreen(viewModel: MainViewModel) {
    val allLoans by viewModel.allLoans.collectAsStateWithLifecycle(initialValue = emptyList())
    val activeLoans = remember(allLoans) { allLoans.filter { !it.isClosed } }
    val closedLoans = remember(allLoans) { allLoans.filter { it.isClosed } }
    
    val accounts by viewModel.allAccounts.collectAsStateWithLifecycle(initialValue = emptyList())
    val totalLoans by viewModel.totalLoans.collectAsStateWithLifecycle(initialValue = 0.0)
    val creditCards by viewModel.allCreditCards.collectAsStateWithLifecycle(initialValue = emptyList())
    val currency by viewModel.currency.collectAsStateWithLifecycle(initialValue = "INR")
    val isSortDesc by viewModel.sortDescending.collectAsStateWithLifecycle(initialValue = true)

    var showAddOrEditDialog by remember { mutableStateOf(false) }
    var loanToEdit by remember { mutableStateOf<Loan?>(null) }
    var showEditConfirmation by remember { mutableStateOf(false) }
    var showDeleteConfirmation by remember { mutableStateOf<Loan?>(null) }
    var showTransactionDialog by remember { mutableStateOf(false) }
    var transactionToEdit by remember { mutableStateOf<Transaction?>(null) }

    val startDate by viewModel.startDate.collectAsStateWithLifecycle()
    val endDate by viewModel.endDate.collectAsStateWithLifecycle()
    val selectedLoanIds by viewModel.selectedLoanIds.collectAsStateWithLifecycle(initialValue = emptySet())
    val filteredTransactions by viewModel.filteredLoanTransactions.collectAsStateWithLifecycle(initialValue = emptyList())

    val filterItems = remember(allLoans) {
        allLoans.map { FilterSelectionItem(id = it.id, name = "${it.name} (${it.lender})") }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        floatingActionButton = {
            FloatingActionButton(onClick = { loanToEdit = null; showAddOrEditDialog = true }, containerColor = FabGreen) {
                Icon(Icons.Default.Add, "Add Loan")
            }
        },
        contentWindowInsets = WindowInsets(0.dp) // FIX: Remove double padding
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues).padding(horizontal = 16.dp).verticalScroll(rememberScrollState())) {
            Spacer(modifier = Modifier.height(16.dp))
            ReusableTotalCard("Total Outstanding Loans", totalLoans, currency, Color(0xFFFFF8E1), Color(0xFFFF9800))
            Spacer(modifier = Modifier.height(16.dp))

            if (activeLoans.isNotEmpty()) {
                Text("Active Loans", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    activeLoans.forEach { loan ->
                        LoanRow(loan, currency, { loanToEdit = loan; showEditConfirmation = true }, { showDeleteConfirmation = loan })
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            if (closedLoans.isNotEmpty()) {
                Text("Closed Loans", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.secondary)
                Spacer(modifier = Modifier.height(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    closedLoans.forEach { loan ->
                        LoanRow(loan, currency, { loanToEdit = loan; showEditConfirmation = true }, { showDeleteConfirmation = loan }, isClosed = true)
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Recent Loan Transactions", style = MaterialTheme.typography.titleLarge)
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
                onEdit = { transactionToEdit = it; showTransactionDialog = true }
            )
            Spacer(modifier = Modifier.height(80.dp))
        }

        if (showAddOrEditDialog) { LoanFormDialog(loanToEdit, accounts, { showAddOrEditDialog = false; loanToEdit = null }, { loan, acc -> if(loanToEdit == null && acc != null) viewModel.insertLoanAndPayout(loan, acc) else viewModel.updateLoan(loan); showAddOrEditDialog = false; loanToEdit = null }) }
        if (showEditConfirmation && loanToEdit != null) { ConfirmationDialog("Edit Loan", "Edit '${loanToEdit!!.name}'?", Icons.Default.Edit, "Edit", { showAddOrEditDialog = true; showEditConfirmation = false }, { showEditConfirmation = false; loanToEdit = null }) }
        if (showDeleteConfirmation != null) { ConfirmationDialog("Delete Loan", "Delete '${showDeleteConfirmation!!.name}'?", Icons.Default.Delete, "Delete", { viewModel.deleteLoan(showDeleteConfirmation!!); showDeleteConfirmation = null }, { showDeleteConfirmation = null }) }
        if (showTransactionDialog) { 
            TransactionFormDialog(
                transactionToEdit = transactionToEdit, 
                accounts = accounts, 
                loans = allLoans, 
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
private fun LoanRow(
    loan: Loan, 
    currency: String, 
    onEdit: (Loan) -> Unit, 
    onDelete: (Loan) -> Unit,
    isClosed: Boolean = false
) {
    val formattedRemaining = formatCurrency(loan.remainingAmount, currency)
    val formattedInitial = formatCurrency(loan.initialAmount, currency)
    val formattedInterest = formatCurrency(loan.totalInterestPaid, currency)
    
    Card(
        elevation = CardDefaults.cardElevation(2.dp),
        colors = if (isClosed) CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)) 
                 else CardDefaults.cardColors()
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(loan.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("from ${loan.lender}\nInitial: $formattedInitial", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (loan.totalInterestPaid > 0) {
                    Text("Interest Paid: $formattedInterest", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                }
                if (isClosed) {
                    Text("STATUS: CLOSED", style = MaterialTheme.typography.labelSmall, color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold)
                }
            }
            if (!isClosed) {
                Text(formattedRemaining, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
            }
            IconButton(onClick = { onEdit(loan) }) { Icon(Icons.Default.Edit, "Edit") }
            IconButton(onClick = { onDelete(loan) }) { Icon(Icons.Default.Delete, "Delete", tint = Color.Red) }
        }
    }
}
