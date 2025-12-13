// In ...ui.screens/RecurringPaymentsScreen.kt
package com.skai.lofintrackerapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EventRepeat
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight // <-- ADDED IMPORT
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.skai.lofintrackerapp.data.db.ScheduledTransaction
import com.skai.lofintrackerapp.data.db.Transaction
import com.skai.lofintrackerapp.data.db.TransactionType
import com.skai.lofintrackerapp.ui.theme.FabGreen
import com.skai.lofintrackerapp.ui.viewmodel.MainViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun RecurringPaymentsScreen(viewModel: MainViewModel) {
    val scheduledItems by viewModel.allScheduledTransactions.collectAsStateWithLifecycle()
    val accounts by viewModel.allAccounts.collectAsStateWithLifecycle()
    // Also need these for the Transaction form (Pay Now)
    val loans by viewModel.allLoans.collectAsStateWithLifecycle()
    val creditCards by viewModel.allCreditCards.collectAsStateWithLifecycle()

    var showAddDialog by remember { mutableStateOf(false) }
    var itemToEdit by remember { mutableStateOf<ScheduledTransaction?>(null) }

    // For "Paying" a scheduled item
    var showPayDialog by remember { mutableStateOf(false) }
    var transactionToPreFill by remember { mutableStateOf<Transaction?>(null) }
    var itemBeingPaid by remember { mutableStateOf<ScheduledTransaction?>(null) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }, containerColor = FabGreen) {
                Icon(Icons.Default.Add, "Add Scheduled")
            }
        },
        // --- FIX: REMOVE EXTRA TOP PADDING ---
        contentWindowInsets = WindowInsets(0.dp)
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp).fillMaxSize()) {
            Text("Scheduled Payments", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(16.dp))

            if (scheduledItems.isEmpty()) {
                Text("No recurring payments set up.", style = MaterialTheme.typography.bodyLarge)
            }

            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(scheduledItems) { item ->
                    ScheduledItemRow(
                        item = item,
                        onEdit = { itemToEdit = item; showAddDialog = true },
                        onDelete = { viewModel.deleteScheduledTransaction(item) },
                        onPay = {
                            val preFilled = Transaction(
                                type = item.type,
                                amount = item.amount,
                                category = item.category,
                                accountId = item.accountId,
                                paymentMode = null,
                                creditCardId = null,
                                loanId = null,
                                date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")),
                                description = item.description
                            )
                            transactionToPreFill = preFilled
                            itemBeingPaid = item
                            showPayDialog = true
                        }
                    )
                }
            }
        }
    }

    // Add/Edit Dialog
    if (showAddDialog) {
        RecurringTransactionFormDialog(
            itemToEdit = itemToEdit,
            accounts = accounts,
            onDismiss = { showAddDialog = false; itemToEdit = null },
            onConfirm = {
                if (itemToEdit == null) viewModel.insertScheduledTransaction(it)
                else viewModel.updateScheduledTransaction(it)
            }
        )
    }

    // "Pay Now" Dialog
    if (showPayDialog && transactionToPreFill != null) {
        TransactionFormDialog(
            transactionToEdit = transactionToPreFill,
            accounts = accounts,
            loans = loans,
            creditCards = creditCards,
            onDismiss = { showPayDialog = false },
            onConfirm = { tx ->
                // 1. Save the real transaction
                viewModel.insertTransaction(tx)
                // 2. Advance the due date
                // (Simple logic: add 1 month. You can make this smarter based on frequency later)
                val currentDue = LocalDate.parse(itemBeingPaid!!.nextDueDate)
                val nextDate = currentDue.plusMonths(1).toString()
                viewModel.updateScheduledTransaction(itemBeingPaid!!.copy(nextDueDate = nextDate))

                showPayDialog = false
                null
            }
        )
    }
}

@Composable
fun ScheduledItemRow(
    item: ScheduledTransaction,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onPay: () -> Unit
) {
    Card {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(item.description, style = MaterialTheme.typography.titleMedium)
                Text("${item.frequency} - Due: ${item.nextDueDate}", style = MaterialTheme.typography.bodySmall)
                Text(
                    if (item.amount > 0.0) "₹${item.amount}" else "Variable Amount",
                    color = if(item.type == TransactionType.INCOME) Color.Green else Color.Red,
                    fontWeight = FontWeight.Bold
                )
            }
            Row {
                IconButton(onClick = onPay) {
                    Icon(Icons.Default.CheckCircle, "Pay Now", tint = FabGreen)
                }
                IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, "Edit") }
                IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, "Delete", tint = Color.Red) }
            }
        }
    }
}