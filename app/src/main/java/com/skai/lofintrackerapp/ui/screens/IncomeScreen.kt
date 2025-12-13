// In ...ui.screens/IncomeScreen.kt
package com.skai.lofintrackerapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.* // <-- THIS IMPORT FIXES "mutableStateOf" and "delegated property" errors
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.skai.lofintrackerapp.data.DEFAULT_INCOME_CATEGORIES
import com.skai.lofintrackerapp.data.db.Transaction // <-- Fixes Transaction type error
import com.skai.lofintrackerapp.ui.common.ReusablePieChart
import com.skai.lofintrackerapp.ui.common.ReusableTotalCard
import com.skai.lofintrackerapp.ui.viewmodel.MainViewModel

@Composable
fun IncomeScreen(viewModel: MainViewModel) {
    // --- Data Collection ---
    val accounts by viewModel.allAccounts.collectAsStateWithLifecycle()
    val incomeTransactions by viewModel.filteredIncomeTransactions.collectAsStateWithLifecycle()
    val startDate by viewModel.startDate.collectAsStateWithLifecycle()
    val endDate by viewModel.endDate.collectAsStateWithLifecycle()
    val selectedCategories by viewModel.selectedIncomeCategories.collectAsStateWithLifecycle()
    val allCategories = DEFAULT_INCOME_CATEGORIES

    // Needed for Edit Form (passed to TransactionFormDialog)
    val loans by viewModel.allLoans.collectAsStateWithLifecycle()
    val creditCards by viewModel.allCreditCards.collectAsStateWithLifecycle()

    // --- Local UI State ---
    // These fixed imports allow 'by remember' to work correctly
    var showTransactionDialog by remember { mutableStateOf(false) }
    var transactionToEdit by remember { mutableStateOf<Transaction?>(null) }

    // Process data for the chart
    val incomeByCategory = remember(incomeTransactions) {
        incomeTransactions
            .groupBy { it.category }
            .mapValues { (_, txs) -> txs.sumOf { it.amount } }
    }

    val totalIncome = remember(incomeTransactions) {
        incomeTransactions.sumOf { it.amount }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Total Card
        ReusableTotalCard(
            title = "Total Income",
            totalAmount = totalIncome,
            cardColor = Color(0xFFE8F5E9),
            textColor = Color(0xFF1B5E20)
        )

        // 2. Filters
        FilterControls(
            startDate = startDate,
            endDate = endDate,
            onStartDateSelected = { viewModel.onStartDateChange(it) },
            onEndDateSelected = { viewModel.onEndDateChange(it) },
            viewModel = viewModel,
            allCategories = allCategories,
            selectedCategories = selectedCategories,
            onCategoryToggle = { viewModel.toggleIncomeCategory(it) }
        )

        // 3. Chart
        Card(modifier = Modifier.fillMaxWidth()) {
            if (incomeByCategory.isEmpty()) {
                Text(
                    text = "No income data for this period.",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(16.dp)
                )
            } else {
                ReusablePieChart(
                    data = incomeByCategory,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                )
            }
        }

        // 4. List Header
        Text(
            text = "Income Transactions",
            style = MaterialTheme.typography.titleLarge
        )

        // 5. Transaction List
        TransactionList(
            transactions = incomeTransactions,
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