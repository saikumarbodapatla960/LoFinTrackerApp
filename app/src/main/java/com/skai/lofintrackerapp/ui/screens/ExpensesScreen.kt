package com.skai.lofintrackerapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.skai.lofintrackerapp.data.DEFAULT_EXPENSE_CATEGORIES // FIX: Added Import
import com.skai.lofintrackerapp.data.db.Transaction
import com.skai.lofintrackerapp.ui.common.ReusablePieChart
import com.skai.lofintrackerapp.ui.common.ReusableTotalCard
import com.skai.lofintrackerapp.ui.viewmodel.MainViewModel

@Composable
fun ExpensesScreen(viewModel: MainViewModel) {
    val accounts by viewModel.allAccounts.collectAsStateWithLifecycle()
    val expenseTransactions by viewModel.filteredExpenseTransactions.collectAsStateWithLifecycle()
    val startDate by viewModel.startDate.collectAsStateWithLifecycle()
    val endDate by viewModel.endDate.collectAsStateWithLifecycle()
    val selectedCategories by viewModel.selectedExpenseCategories.collectAsStateWithLifecycle()
    val currency by viewModel.currency.collectAsStateWithLifecycle()
    val allCategories = DEFAULT_EXPENSE_CATEGORIES // FIX: Variable Defined

    val loans by viewModel.allLoans.collectAsStateWithLifecycle()
    val creditCards by viewModel.allCreditCards.collectAsStateWithLifecycle()

    var showTransactionDialog by remember { mutableStateOf(false) }
    var transactionToEdit by remember { mutableStateOf<Transaction?>(null) }

    val expenseByCategory = remember(expenseTransactions) {
        expenseTransactions.groupBy { it.category }.mapValues { (_, txs) -> txs.sumOf { it.amount } }
    }
    val totalExpense = remember(expenseTransactions) { expenseTransactions.sumOf { it.amount } }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        ReusableTotalCard("Total Expense", totalExpense, currency, Color(0xFFFDECEA), Color(0xFFB71C1C))

        // FIX: Using Named Arguments
        FilterControls(
            startDate = startDate,
            endDate = endDate,
            onStartDateSelected = { viewModel.onStartDateChange(it) },
            onEndDateSelected = { viewModel.onEndDateChange(it) },
            viewModel = viewModel,
            allCategories = allCategories,
            selectedCategories = selectedCategories,
            onCategoryToggle = { viewModel.toggleExpenseCategory(it) }
        )

        Card(modifier = Modifier.fillMaxWidth()) {
            if (expenseByCategory.isEmpty()) {
                Text("No expense data for this period.", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(16.dp))
            } else {
                ReusablePieChart(data = expenseByCategory, modifier = Modifier.fillMaxWidth().padding(16.dp))
            }
        }

        Text(text = "Expense Transactions", style = MaterialTheme.typography.titleLarge)
        TransactionList(expenseTransactions, accounts, currency, { transactionToEdit = it; showTransactionDialog = true })
        Spacer(modifier = Modifier.height(16.dp))
    }

    if (showTransactionDialog) {
        TransactionFormDialog(transactionToEdit, accounts, loans, creditCards, { showTransactionDialog = false; transactionToEdit = null }, { if (transactionToEdit == null) viewModel.insertTransaction(it) else viewModel.updateTransaction(transactionToEdit!!, it) }, { viewModel.deleteTransaction(it) })
    }
}