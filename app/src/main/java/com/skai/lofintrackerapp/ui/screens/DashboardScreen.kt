// In ...ui/screens/DashboardScreen.kt
package com.skai.lofintrackerapp.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import com.skai.lofintrackerapp.ui.navigation.Screen
import com.skai.lofintrackerapp.ui.viewmodel.MainViewModel
import java.text.NumberFormat
import java.util.Locale

@Composable
fun DashboardScreen(
    viewModel: MainViewModel,
    navController: NavController
) {
    // --- Data Collection ---
    val totalBalance by viewModel.totalBalance.collectAsStateWithLifecycle()
    val totalLoans by viewModel.totalLoans.collectAsStateWithLifecycle()
    val startDate by viewModel.startDate.collectAsStateWithLifecycle()
    val endDate by viewModel.endDate.collectAsStateWithLifecycle()
    val totalFilteredIncome by viewModel.totalFilteredIncome.collectAsStateWithLifecycle()
    val totalFilteredExpense by viewModel.totalFilteredExpense.collectAsStateWithLifecycle()
    val transactions by viewModel.filteredTransactions.collectAsStateWithLifecycle()
    val accounts by viewModel.allAccounts.collectAsStateWithLifecycle()

    // Needed for Edit Form
    val loans by viewModel.allLoans.collectAsStateWithLifecycle()
    val creditCards by viewModel.allCreditCards.collectAsStateWithLifecycle()

    // --- Local UI State ---
    var showTransactionDialog by remember { mutableStateOf(false) }
    var transactionToEdit by remember { mutableStateOf<Transaction?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp) // Standard screen padding
            .verticalScroll(rememberScrollState())
    ) {
        // --- NEW CARD LAYOUT (No extra top gap) ---

        // Row 1: Balance & Loans
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            SummaryCard(
                title = "Total Balance",
                amount = totalBalance,
                icon = Icons.Default.AccountBalance,
                color = Color(0xFF2196F3),
                onClick = { navController.navigate(Screen.Balance.route) },
                modifier = Modifier.weight(1f)
            )
            SummaryCard(
                title = "Total Loans",
                amount = totalLoans,
                icon = Icons.Default.CreditCard,
                color = Color(0xFFFF9800),
                onClick = { navController.navigate(Screen.Loans.route) },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Row 2: Income & Expense
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            SummaryCard(
                title = "Total Income",
                amount = totalFilteredIncome,
                icon = Icons.Default.ArrowUpward,
                color = Color(0xFF4CAF50),
                onClick = { navController.navigate(Screen.Income.route) },
                modifier = Modifier.weight(1f)
            )
            SummaryCard(
                title = "Total Expense",
                amount = totalFilteredExpense,
                icon = Icons.Default.ArrowDownward,
                color = Color(0xFFF44336),
                onClick = { navController.navigate(Screen.Expenses.route) },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- Filter Controls (No Categories) ---
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

        // --- "Get Started" Message ---
        if (accounts.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Go to the Balance page to add your first account!",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // --- Recent Transactions ---
        Text(
            text = "Recent Transactions",
            style = MaterialTheme.typography.titleLarge
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (transactions.isEmpty()) {
            Text(
                text = "No transactions for this period. Click '+' to add one!",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            TransactionList(
                transactions = transactions,
                accounts = accounts,
                onEdit = { transaction ->
                    transactionToEdit = transaction
                    showTransactionDialog = true
                }
            )
        }

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

@Composable
private fun SummaryCard(
    title: String,
    amount: Double,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val formattedAmount = NumberFormat.getCurrencyInstance(Locale("en", "IN")).format(amount)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.align(Alignment.CenterStart)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = formattedAmount,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    ),
                    color = color
                )
            }
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = color,
                modifier = Modifier
                    .size(32.dp)
                    .align(Alignment.TopEnd)
            )
        }
    }
}