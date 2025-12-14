// In ...ui/screens/DashboardScreen.kt
package com.skai.lofintrackerapp.ui.screens

import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.skai.lofintrackerapp.data.db.Transaction
import com.skai.lofintrackerapp.ui.common.formatCurrency
import com.skai.lofintrackerapp.ui.navigation.Screen
import com.skai.lofintrackerapp.ui.viewmodel.MainViewModel

@Composable
fun DashboardScreen(viewModel: MainViewModel, navController: NavController) {
    val totalBalance by viewModel.totalBalance.collectAsStateWithLifecycle()
    val totalLoans by viewModel.totalLoans.collectAsStateWithLifecycle()
    val totalFilteredIncome by viewModel.totalFilteredIncome.collectAsStateWithLifecycle()
    val totalFilteredExpense by viewModel.totalFilteredExpense.collectAsStateWithLifecycle()
    val transactions by viewModel.filteredTransactions.collectAsStateWithLifecycle()
    val accounts by viewModel.allAccounts.collectAsStateWithLifecycle()
    val loans by viewModel.allLoans.collectAsStateWithLifecycle()
    val creditCards by viewModel.allCreditCards.collectAsStateWithLifecycle()
    val currency by viewModel.currency.collectAsStateWithLifecycle() // Currency

    val startDate by viewModel.startDate.collectAsStateWithLifecycle()
    val endDate by viewModel.endDate.collectAsStateWithLifecycle()

    var showTransactionDialog by remember { mutableStateOf(false) }
    var transactionToEdit by remember { mutableStateOf<Transaction?>(null) }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            SummaryCard("Total Balance", totalBalance, currency, Icons.Default.AccountBalance, Color(0xFF2196F3), { navController.navigate(Screen.Balance.route) }, Modifier.weight(1f))
            SummaryCard("Total Loans", totalLoans, currency, Icons.Default.CreditCard, Color(0xFFFF9800), { navController.navigate(Screen.Loans.route) }, Modifier.weight(1f))
        }
        Spacer(modifier = Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            SummaryCard("Total Income", totalFilteredIncome, currency, Icons.Default.ArrowUpward, Color(0xFF4CAF50), { navController.navigate(Screen.Income.route) }, Modifier.weight(1f))
            SummaryCard("Total Expense", totalFilteredExpense, currency, Icons.Default.ArrowDownward, Color(0xFFF44336), { navController.navigate(Screen.Expenses.route) }, Modifier.weight(1f))
        }

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

        if (accounts.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp), contentAlignment = Alignment.Center) {
                Text("Go to the Balance page to add your first account!", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        Text("Recent Transactions", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(16.dp))

        if (transactions.isEmpty()) {
            Text("No transactions for this period. Click '+' to add one!", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            TransactionList(
                transactions = transactions,
                accounts = accounts,
                currencyCode = currency, // Pass Currency
                onEdit = { transaction ->
                    transactionToEdit = transaction
                    showTransactionDialog = true
                }
            )
        }
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
            onDelete = { viewModel.deleteTransaction(it) }
        )
    }
}

@Composable
private fun SummaryCard(
    title: String,
    amount: Double,
    currencyCode: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val formattedAmount = formatCurrency(amount, currencyCode)

    Card(
        modifier = modifier.fillMaxWidth().clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.align(Alignment.CenterStart)) {
                Text(text = title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(text = formattedAmount, style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold, fontSize = 20.sp), color = color)
            }
            Icon(imageVector = icon, contentDescription = title, tint = color, modifier = Modifier.size(32.dp).align(Alignment.TopEnd))
        }
    }
}