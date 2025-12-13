// In ...ui/screens/LoansScreen.kt
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
import com.skai.lofintrackerapp.data.db.Loan
import com.skai.lofintrackerapp.data.db.Transaction
import com.skai.lofintrackerapp.ui.common.ConfirmationDialog
import com.skai.lofintrackerapp.ui.common.ReusableTotalCard
import com.skai.lofintrackerapp.ui.theme.FabGreen
import com.skai.lofintrackerapp.ui.viewmodel.MainViewModel
import java.text.NumberFormat
import java.util.Locale

@Composable
fun LoansScreen(viewModel: MainViewModel) {

    // --- State Management ---
    val loans by viewModel.allLoans.collectAsStateWithLifecycle()
    val accounts by viewModel.allAccounts.collectAsStateWithLifecycle()
    val creditCards by viewModel.allCreditCards.collectAsStateWithLifecycle()
    val totalLoans by viewModel.totalLoans.collectAsStateWithLifecycle()

    // Dialog States
    var showAddOrEditDialog by remember { mutableStateOf(false) }
    var loanToEdit by remember { mutableStateOf<Loan?>(null) }
    var showEditConfirmation by remember { mutableStateOf(false) }
    var showDeleteConfirmation by remember { mutableStateOf<Loan?>(null) }

    // Transaction Edit States
    var showTransactionDialog by remember { mutableStateOf(false) }
    var transactionToEdit by remember { mutableStateOf<Transaction?>(null) }

    // Filter States
    val startDate by viewModel.startDate.collectAsStateWithLifecycle()
    val endDate by viewModel.endDate.collectAsStateWithLifecycle()
    val selectedLoanIds by viewModel.selectedLoanIds.collectAsStateWithLifecycle()
    val filteredTransactions by viewModel.filteredLoanTransactions.collectAsStateWithLifecycle()

    val filterItems = remember(loans) {
        loans.map { FilterSelectionItem(id = it.id, name = "${it.name} (${it.lender})") }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    loanToEdit = null
                    showAddOrEditDialog = true
                },
                containerColor = FabGreen
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Loan")
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

            // 1. Total Loans Card
            // Manually add top padding here
            ReusableTotalCard(
                title = "Total Outstanding Loans",
                totalAmount = totalLoans,
                cardColor = Color(0xFFFFF8E1),
                textColor = Color(0xFFFF9800),
                modifier = Modifier.padding(top = 16.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(text = "Your Loans", style = MaterialTheme.typography.titleLarge)

            Spacer(modifier = Modifier.height(16.dp))

            // 2. Loans List
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                loans.forEach { loan ->
                    LoanRow(
                        loan = loan,
                        onEdit = {
                            loanToEdit = loan
                            showEditConfirmation = true
                        },
                        onDelete = {
                            showDeleteConfirmation = loan
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
                filterTitle = "Loans",
                allItems = filterItems,
                selectedIds = selectedLoanIds,
                onItemToggle = { viewModel.toggleLoanId(it) }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 4. Transaction List
            Text(text = "Recent Loan Transactions", style = MaterialTheme.typography.titleLarge)

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
            LoanFormDialog(
                loanToEdit = loanToEdit,
                accounts = accounts,
                onDismiss = {
                    showAddOrEditDialog = false
                    loanToEdit = null
                },
                onConfirm = { loan, accountId ->
                    if (loanToEdit == null && accountId != null) {
                        viewModel.insertLoanAndPayout(loan, accountId)
                    } else {
                        viewModel.updateLoan(loan)
                    }
                    showAddOrEditDialog = false
                    loanToEdit = null
                }
            )
        }

        if (showEditConfirmation && loanToEdit != null) {
            ConfirmationDialog(
                title = "Edit Loan",
                message = "Are you sure you want to edit '${loanToEdit!!.name}'?",
                icon = Icons.Default.Edit,
                confirmText = "Edit",
                onDismiss = {
                    showEditConfirmation = false
                    loanToEdit = null
                },
                onConfirm = {
                    showAddOrEditDialog = true
                    showEditConfirmation = false
                }
            )
        }

        if (showDeleteConfirmation != null) {
            ConfirmationDialog(
                title = "Delete Loan",
                message = "Are you sure you want to delete '${showDeleteConfirmation!!.name}'? This cannot be undone.",
                icon = Icons.Default.Delete,
                confirmText = "Delete",
                onDismiss = { showDeleteConfirmation = null },
                onConfirm = {
                    viewModel.deleteLoan(showDeleteConfirmation!!)
                    showDeleteConfirmation = null
                }
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

// ... (LoanRow function is unchanged and correct)
@Composable
private fun LoanRow(loan: Loan, onEdit: (Loan) -> Unit, onDelete: (Loan) -> Unit) {
    val formattedRemaining = NumberFormat.getCurrencyInstance(Locale("en", "IN")).format(loan.remainingAmount)
    val formattedInitial = NumberFormat.getCurrencyInstance(Locale("en", "IN")).format(loan.initialAmount)

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
                Text(text = loan.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "from ${loan.lender}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Initial: $formattedInitial",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = formattedRemaining,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold
            )
            IconButton(onClick = { onEdit(loan) }) {
                Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = { onDelete(loan) }) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red)
            }
        }
    }
}