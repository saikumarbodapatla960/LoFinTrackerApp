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
    val startDate by viewModel.startDate.collectAsStateWithLifecycle()
    val endDate by viewModel.endDate.collectAsStateWithLifecycle()
    val totalFilteredIncome by viewModel.totalFilteredIncome.collectAsStateWithLifecycle(initialValue = 0.0)
    val totalFilteredExpense by viewModel.totalFilteredExpense.collectAsStateWithLifecycle(initialValue = 0.0)
    val transactions by viewModel.filteredTransactions.collectAsStateWithLifecycle(initialValue = emptyList())

    val accounts by viewModel.allAccounts.collectAsStateWithLifecycle(initialValue = emptyList())
    val loans by viewModel.allLoans.collectAsStateWithLifecycle(initialValue = emptyList())
    val creditCards by viewModel.allCreditCards.collectAsStateWithLifecycle(initialValue = emptyList())
    val currency by viewModel.currency.collectAsStateWithLifecycle(initialValue = "INR")

    var showTransactionDialog by remember { mutableStateOf(false) }
    var transactionToEdit by remember { mutableStateOf<Transaction?>(null) }

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
        Spacer(modifier = Modifier.height(16.dp))
        FilterControls(startDate, endDate, { viewModel.onStartDateChange(it) }, { viewModel.onEndDateChange(it) }, viewModel)
        Spacer(modifier = Modifier.height(16.dp))

        Card(modifier = Modifier.fillMaxWidth()) {
            if (totalFilteredIncome == 0.0 && totalFilteredExpense == 0.0) {
                Text("No data for this period.", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(16.dp))
            } else {
                ReusableBarChart(totalFilteredIncome, totalFilteredExpense, Modifier.fillMaxWidth().padding(16.dp))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text("Recent Transactions", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(16.dp))

        TransactionList(
            transactions = transactions,
            accounts = accounts,
            creditCards = creditCards,
            currencyCode = currency,
            onEdit = { transactionToEdit = it; showTransactionDialog = true }
        )
        Spacer(modifier = Modifier.height(16.dp))
    }

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