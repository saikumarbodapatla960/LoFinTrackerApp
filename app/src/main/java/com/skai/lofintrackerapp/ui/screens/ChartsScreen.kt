// In ...ui/screens/ChartsScreen.kt
package com.skai.lofintrackerapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.skai.lofintrackerapp.data.db.Transaction
import com.skai.lofintrackerapp.ui.common.ReusableBarChart
import com.skai.lofintrackerapp.ui.viewmodel.MainViewModel

@Composable
fun ChartsScreen(viewModel: MainViewModel) {
    // --- Collect Filtered Data ---
    val startDate by viewModel.startDate.collectAsStateWithLifecycle()
    val endDate by viewModel.endDate.collectAsStateWithLifecycle()
    val totalFilteredIncome by viewModel.totalFilteredIncome.collectAsStateWithLifecycle()
    val totalFilteredExpense by viewModel.totalFilteredExpense.collectAsStateWithLifecycle()

    // --- For List & Edit ---
    val accounts by viewModel.allAccounts.collectAsStateWithLifecycle()
    val loans by viewModel.allLoans.collectAsStateWithLifecycle()
    val creditCards by viewModel.allCreditCards.collectAsStateWithLifecycle()
    val transactions by viewModel.filteredTransactions.collectAsStateWithLifecycle()

    // --- Local UI State ---
    var showTransactionDialog by remember { mutableStateOf(false) }
    var transactionToEdit by remember { mutableStateOf<Transaction?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp) // <-- Layout Fix
    ) {
        // Manually add top padding here (since Scaffold isn't wrapping this directly in AppShell for content,
        // but usually AppShell's Scaffold handles the top bar.
        // If ChartsScreen is inside AppShell's padding, we might need a top spacer.
        // Based on previous screens, let's add it for consistency.)
        Spacer(modifier = Modifier.height(16.dp))

        FilterControls(
            startDate = startDate,
            endDate = endDate,
            onStartDateSelected = { viewModel.onStartDateChange(it) },
            onEndDateSelected = { viewModel.onEndDateChange(it) },
            viewModel = viewModel,
            allCategories = null,
            selectedCategories = null,
            onCategoryToggle = null
        )

        Spacer(modifier = Modifier.height(16.dp))

        Card(modifier = Modifier.fillMaxWidth()) {
            if (totalFilteredIncome == 0.0 && totalFilteredExpense == 0.0) {
                Text(
                    text = "No income or expense data for this period.",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(16.dp)
                )
            } else {
                ReusableBarChart(
                    income = totalFilteredIncome,
                    expense = totalFilteredExpense,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Recent Transactions",
            style = MaterialTheme.typography.titleLarge
        )

        Spacer(modifier = Modifier.height(16.dp))

        TransactionList(
            transactions = transactions,
            accounts = accounts,
            onEdit = { transaction ->
                transactionToEdit = transaction
                showTransactionDialog = true
            }
        )

        Spacer(modifier = Modifier.height(16.dp))
    }

    // --- Edit Dialog ---
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