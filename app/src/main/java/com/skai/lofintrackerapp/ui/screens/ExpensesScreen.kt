// In ...ui/screens/ExpensesScreen.kt
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
import com.skai.lofintrackerapp.data.DEFAULT_EXPENSE_CATEGORIES
import com.skai.lofintrackerapp.data.db.Transaction
import com.skai.lofintrackerapp.ui.common.ReusablePieChart
import com.skai.lofintrackerapp.ui.common.ReusableTotalCard
import com.skai.lofintrackerapp.ui.viewmodel.MainViewModel

@Composable
fun ExpensesScreen(viewModel: MainViewModel) {
    // --- Data Collection ---
    val accounts by viewModel.allAccounts.collectAsStateWithLifecycle()
    val expenseTransactions by viewModel.filteredExpenseTransactions.collectAsStateWithLifecycle()
    val startDate by viewModel.startDate.collectAsStateWithLifecycle()
    val endDate by viewModel.endDate.collectAsStateWithLifecycle()
    val selectedCategories by viewModel.selectedExpenseCategories.collectAsStateWithLifecycle()
    val allCategories = DEFAULT_EXPENSE_CATEGORIES

    // Needed for Edit Form
    val loans by viewModel.allLoans.collectAsStateWithLifecycle()
    val creditCards by viewModel.allCreditCards.collectAsStateWithLifecycle()

    // --- Local UI State ---
    var showTransactionDialog by remember { mutableStateOf(false) }
    var transactionToEdit by remember { mutableStateOf<Transaction?>(null) }

    // Process data for the chart
    val expenseByCategory = remember(expenseTransactions) {
        expenseTransactions
            .groupBy { it.category }
            .mapValues { (_, txs) -> txs.sumOf { it.amount } }
    }

    val totalExpense = remember(expenseTransactions) {
        expenseTransactions.sumOf { it.amount }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // 1. Total Card
        ReusableTotalCard(
            title = "Total Expense",
            totalAmount = totalExpense,
            cardColor = Color(0xFFFDECEA), // Light Red
            textColor = Color(0xFFB71C1C)  // Dark Red
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 2. Filters
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

        Spacer(modifier = Modifier.height(16.dp))

        // 3. Chart
        Card(modifier = Modifier.fillMaxWidth()) {
            if (expenseByCategory.isEmpty()) {
                Text(
                    text = "No expense data for this period.",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(16.dp)
                )
            } else {
                ReusablePieChart(
                    data = expenseByCategory,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 4. List
        Text(
            text = "Expense Transactions",
            style = MaterialTheme.typography.titleLarge
        )

        Spacer(modifier = Modifier.height(16.dp))

        TransactionList(
            transactions = expenseTransactions,
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