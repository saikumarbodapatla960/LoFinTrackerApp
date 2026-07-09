package com.skai.lofintrackerapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.skai.lofintrackerapp.data.DEFAULT_EXPENSE_CATEGORIES
import com.skai.lofintrackerapp.data.db.Transaction
import com.skai.lofintrackerapp.ui.common.ReusablePieChart
import com.skai.lofintrackerapp.ui.common.ReusableTotalCard
import com.skai.lofintrackerapp.ui.viewmodel.MainViewModel

@Composable
fun ExpensesScreen(viewModel: MainViewModel) {
    val accounts by viewModel.allAccounts.collectAsStateWithLifecycle(initialValue = emptyList())
    val expenseTransactions by viewModel.filteredExpenseTransactions.collectAsStateWithLifecycle(initialValue = emptyList())
    val startDate by viewModel.startDate.collectAsStateWithLifecycle()
    val endDate by viewModel.endDate.collectAsStateWithLifecycle()
    val selectedCategories by viewModel.selectedExpenseCategories.collectAsStateWithLifecycle(initialValue = emptySet())
    val currency by viewModel.currency.collectAsStateWithLifecycle(initialValue = "INR")
    val isSortDesc by viewModel.sortDescending.collectAsStateWithLifecycle(initialValue = true)
    val allCategories = DEFAULT_EXPENSE_CATEGORIES

    val loans by viewModel.allLoans.collectAsStateWithLifecycle(initialValue = emptyList())
    val creditCards by viewModel.allCreditCards.collectAsStateWithLifecycle(initialValue = emptyList())

    var showTransactionDialog by remember { mutableStateOf(false) }
    var transactionToEdit by remember { mutableStateOf<Transaction?>(null) }

    val expenseByCategory = remember(expenseTransactions) {
        expenseTransactions.groupBy { it.category }.mapValues { (_, txs) -> txs.sumOf { it.amount } }
    }
    val totalExpense = remember(expenseTransactions) { expenseTransactions.sumOf { it.amount } }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        ReusableTotalCard("Total Expense", totalExpense, currency, Color(0xFFFDECEA), Color(0xFFB71C1C))

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

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "Expense Transactions", style = MaterialTheme.typography.titleLarge)
            IconButton(onClick = { viewModel.toggleSortOrder() }) {
                Icon(
                    imageVector = Icons.Default.AccessTime,
                    contentDescription = if (isSortDesc) "Newest added first" else "Oldest added first",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
        
        TransactionList(
            transactions = expenseTransactions,
            accounts = accounts,
            creditCards = creditCards,
            currencyCode = currency,
            onEdit = { transaction ->
                transactionToEdit = transaction
                showTransactionDialog = true
            }
        )
        Spacer(modifier = Modifier.height(16.dp))
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
            onConfirm = { if (transactionToEdit == null) viewModel.insertTransaction(it) else viewModel.updateTransaction(transactionToEdit!!, it) },
            onDelete = { viewModel.deleteTransaction(it) },
            onAddScheduled = { viewModel.insertScheduledTransaction(it) }
        )
    }
}
